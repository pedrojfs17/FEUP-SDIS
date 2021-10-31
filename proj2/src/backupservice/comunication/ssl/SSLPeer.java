package backupservice.comunication.ssl;

import backupservice.peer.Peer;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class SSLPeer {
    protected Peer peer;
    protected ScheduledExecutorService scheduledExecutorReceiverService;
    protected static final int THREAD_NUMBER = 32;

    protected ExecutorService executor = Executors.newSingleThreadExecutor();

    /*
     * Call the wrap() and unwrap() methods to perform the initial handshaking.
     * You'll need to call these methods several times before application data can be consumed, produced, and properly protected by later wrap()/unwrap() calls.
     */
    protected boolean handshake(SSLConnection connection) throws Exception {
        SSLEngineResult result;
        SSLEngine engine = connection.getEngine();
        SocketChannel socketChannel = connection.getChannel();

        HandshakeStatus handshakeStatus = engine.getHandshakeStatus();

        while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED &&
                handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            switch (handshakeStatus) {
                /*
                 * The wrap() method produces a TLS ClientHello message, then places it in the outbound network buffer.
                 * The application must correctly send the bytes of this message to the peer.
                 */
                case NEED_WRAP:
                    connection.getOutAppBuffer().clear();
                    try {
                        result = engine.wrap(connection.getOutAppBuffer(), connection.getOutNetBuffer());
                        handshakeStatus = result.getHandshakeStatus();
                    } catch (SSLException sslException) {
                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    switch (result.getStatus()) {
                        case OK:
                            connection.getOutNetBuffer().flip();
                            while (connection.getOutNetBuffer().hasRemaining()) {
                                socketChannel.write(connection.getOutNetBuffer());
                            }
                            break;
                        case BUFFER_OVERFLOW:
                            connection.setOutNetBuffer(extendBuffer(connection.getOutNetBuffer(), engine.getSession().getPacketBufferSize()));
                            break;
                        case CLOSED:
                            try {
                                connection.getOutNetBuffer().flip();
                                while (connection.getOutNetBuffer().hasRemaining()) {
                                    socketChannel.write(connection.getOutNetBuffer());
                                }
                                connection.getInNetBuffer().clear();
                            } catch (Exception e) {
                                handshakeStatus = engine.getHandshakeStatus();
                            }
                            break;
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                    break;
                /*
                 * The SSLEngine must now process the peer's response (such as the ServerHello, Certificate, and ServerHelloDone messages) to drive the handshake forward.
                 * The application obtains the response bytes from the network transport and places them in the inbound network buffer.
                 * The SSLEngine processes these bytes using the unwrap() method.
                 */
                case NEED_UNWRAP:
                    if (socketChannel.read(connection.getInNetBuffer()) < 0) {
                        if (engine.isInboundDone() && engine.isOutboundDone()) {
                            return false;
                        }
                        engine.closeInbound();
                        engine.closeOutbound();

                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    connection.getInNetBuffer().flip();
                    try {
                        result = engine.unwrap(connection.getInNetBuffer(), connection.getInAppBuffer());
                        connection.getInNetBuffer().compact();
                        handshakeStatus = result.getHandshakeStatus();
                    } catch (SSLException sslException) {
                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    switch (result.getStatus()) {
                        case OK:
                            break;
                        case BUFFER_OVERFLOW:
                            connection.setInAppBuffer(extendBuffer(connection.getInAppBuffer(), engine.getSession().getApplicationBufferSize()));
                            break;
                        case BUFFER_UNDERFLOW:
                            connection.setInNetBuffer(handleUnderflow(connection.getInNetBuffer(), engine.getSession().getPacketBufferSize()));
                            break;
                        case CLOSED:
                            if (engine.isOutboundDone())
                                return false;
                            else {
                                engine.closeOutbound();
                                handshakeStatus = engine.getHandshakeStatus();
                                break;
                            }
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                    break;
                case NEED_TASK:
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        executor.submit(task);
                    }
                    handshakeStatus = engine.getHandshakeStatus();
                    break;
                default:
                    break;
            }
        }
        Thread.sleep(250);
        return true;
    }

    protected ByteBuffer extendBuffer(ByteBuffer buffer, int capacity) {
        if (capacity > buffer.capacity()) {
            buffer = ByteBuffer.allocate(capacity);
        } else {
            buffer = ByteBuffer.allocate(buffer.capacity() * 2);
        }
        return buffer;
    }

    protected ByteBuffer handleUnderflow(ByteBuffer buffer, int capacity) {
        if (capacity < buffer.limit())
            return buffer;

        ByteBuffer newBuffer = extendBuffer(buffer, capacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;

    }

    public void write(SSLConnection connection, byte[] message) throws Exception {
        SSLEngine engine = connection.getEngine();
        SocketChannel socketChannel = connection.getChannel();
        ByteBuffer outAppBuffer = connection.getOutAppBuffer();
        ByteBuffer outNetBuffer = connection.getOutNetBuffer();
        outAppBuffer.clear();

        while (true) {
            try {
                outAppBuffer.put(message);
                break;
            }
            catch (BufferOverflowException e) {
                outAppBuffer = extendBuffer(outAppBuffer, engine.getSession().getApplicationBufferSize());
            }
        }

        outAppBuffer.flip();
        while (outAppBuffer.hasRemaining()) {
            outNetBuffer.clear();
            SSLEngineResult result = engine.wrap(outAppBuffer, outNetBuffer);
            switch (result.getStatus()) {
                case OK:
                    outNetBuffer.flip();
                    while (outNetBuffer.hasRemaining()) {
                        socketChannel.write(outNetBuffer);
                    }
                    break;
                case BUFFER_OVERFLOW:
                    outNetBuffer = extendBuffer(outNetBuffer, engine.getSession().getPacketBufferSize());
                    break;
                case CLOSED:
                    close(connection);
                    return;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }

        }
    }

    protected void close(SSLConnection connection) throws Exception {
        connection.getEngine().closeOutbound();
        handshake(connection);
        connection.getChannel().close();
    }

    protected void handleEOS(SSLConnection connection) throws Exception {
        connection.getEngine().closeInbound();
        close(connection);
    }


    public static SSLContext initializeSSLContext(String keystoreFile, String truststoreFile, String keystorePass, String truststorePass, String keyPass) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        KeyStore trustStore = KeyStore.getInstance("JKS");

        keyStore.load(new FileInputStream(keystoreFile), keystorePass.toCharArray());
        trustStore.load(new FileInputStream(truststoreFile), truststorePass.toCharArray());

        KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmFactory.init(keyStore, keyPass.toCharArray());

        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmFactory.init(trustStore);

        SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
        sslCtx.init(kmFactory.getKeyManagers(), tmFactory.getTrustManagers(), null);
        return sslCtx;
    }

    public void receiveFile(SSLConnection connection, FileChannel fileChannel, int fileSize) throws Exception {
        int bytesWritten = 0;

        while (bytesWritten != fileSize) {
            bytesWritten += receiveFileChunk(connection, fileChannel);
            System.out.println("Written " + bytesWritten + " bytes to file!");
        }

        connection.getChannel().configureBlocking(false);
    }

    public int receiveFileChunk(SSLConnection connection, FileChannel fileChannel) throws Exception {
        SSLEngine engine = connection.getEngine();
        SocketChannel channel = connection.getChannel();
        channel.configureBlocking(true);
        ByteBuffer inNetBuffer = ByteBuffer.allocate(16413);
        ByteBuffer inAppBuffer = connection.getInAppBuffer();

        channel.socket().setSoTimeout(1000);
        ReadableByteChannel byteChannel = Channels.newChannel(channel.socket().getInputStream());

        int bytesRead = 0;
        do {
            int read;
            try {
                read = byteChannel.read(inNetBuffer);
            } catch (Exception e) {
                break;
            }
            bytesRead += read;
        } while (bytesRead % 16413 != 0);

        int bytesWritten = 0;

        if (bytesRead > 0) {
            inNetBuffer.flip();
            while (inNetBuffer.hasRemaining()) {
                inAppBuffer.clear();
                SSLEngineResult result = engine.unwrap(inNetBuffer, inAppBuffer);
                switch (result.getStatus()) {
                    case OK:
                        inAppBuffer.flip();
                        bytesWritten += fileChannel.write(inAppBuffer);
                        break;
                    case BUFFER_OVERFLOW:
                        inAppBuffer = extendBuffer(inAppBuffer, engine.getSession().getApplicationBufferSize());
                        break;
                    case BUFFER_UNDERFLOW:
                        inNetBuffer = handleUnderflow(inNetBuffer, engine.getSession().getPacketBufferSize());
                        break;
                    case CLOSED:
                        close(connection);
                        return -1;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
            }

        } else if (bytesRead < 0) {
            handleEOS(connection);
        }

        return bytesWritten;
    }
}

