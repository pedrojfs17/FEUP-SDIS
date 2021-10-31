package backupservice.comunication.ssl;

import backupservice.peer.Peer;

import javax.net.ssl.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class SSLClient extends SSLPeer {
    private final SSLContext context;

    public SSLClient(Peer peer, SSLContext sslContext) {
        this.peer = peer;
        this.context = sslContext;
    }

    public SSLConnection connectToServer(String address, int port, boolean blocking) throws Exception {
        SSLConnection connection = new SSLConnection(SocketChannel.open(), context.createSSLEngine(address, port), blocking);
        SocketChannel channel = connection.getChannel();
        connection.getEngine().setUseClientMode(true);

        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(address, port));
        while (!channel.finishConnect()) {
            // do something until connect completed
        }
        connection.getEngine().beginHandshake();
        handshake(connection);
        return connection;
    }

    public byte[] read(SSLConnection connection) throws Exception  {
        SSLEngine sslEngine = connection.getEngine();
        SocketChannel channel = connection.getChannel();
        ByteBuffer inNetBuffer = connection.getInNetBuffer();
        ByteBuffer inAppBuffer = connection.getInAppBuffer();
        boolean blocking = connection.isBlocking();

        int numTries = 0;
        int wait = 50;
        do {
            inNetBuffer.clear();
            inAppBuffer.clear();
            int bytesRead = channel.read(inNetBuffer);

            if (bytesRead > 0) {
                inNetBuffer.flip();

                SSLEngineResult result = sslEngine.unwrap(inNetBuffer, inAppBuffer);

                switch (result.getStatus()) {
                    case OK:
                        inNetBuffer.compact();
                        blocking = false;
                        break;
                    case BUFFER_OVERFLOW:
                        inAppBuffer = extendBuffer(inAppBuffer, sslEngine.getSession().getApplicationBufferSize());
                        break;
                    case BUFFER_UNDERFLOW:
                        inNetBuffer = handleUnderflow(inNetBuffer, sslEngine.getSession().getPacketBufferSize());
                        break;
                    case CLOSED:
                        close(connection);
                        return null;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }

                if (inAppBuffer.hasRemaining()) {
                    inAppBuffer.flip();
                    byte[] buffer;
                    int size = inAppBuffer.remaining();
                    if (inAppBuffer.hasArray()) {
                        buffer = Arrays.copyOfRange(inAppBuffer.array(),
                                inAppBuffer.arrayOffset() + inAppBuffer.position(),
                                size);
                    } else {
                        buffer = new byte[inAppBuffer.remaining()];
                        inAppBuffer.duplicate().get(buffer);
                    }
                    return buffer;
                }

            } else if (bytesRead < 0) {
                handleEOS(connection);
                return null;
            }
            Thread.sleep(wait);
            numTries++;
        } while (blocking && numTries < 100);

        if (blocking) {
            stop(connection);
            throw new Exception("Read time out!");
        }

        return null;
    }

    public void stop(SSLConnection connection) throws Exception {
        close(connection);
    }
}
