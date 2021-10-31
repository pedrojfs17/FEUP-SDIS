package backupservice.comunication.ssl;

import backupservice.comunication.message.Message;
import backupservice.comunication.message.backup.GetFileMessage;
import backupservice.comunication.message.backup.PutFileMessage;
import backupservice.peer.Peer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;

public class SSLServer extends SSLPeer {
    protected Selector selector;
    private final SSLContext sslContext;
    private boolean active;
    private final InetSocketAddress address;

    public SSLServer(Peer peer, SSLContext sslContext, InetSocketAddress address) throws Exception{
        this.peer = peer;
        this.sslContext = sslContext;

        selector = SelectorProvider.provider().openSelector();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(address);
        serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        this.address = new InetSocketAddress(serverSocketChannel.socket().getInetAddress(), serverSocketChannel.socket().getLocalPort());

        active = true;

        scheduledExecutorReceiverService = Executors.newScheduledThreadPool(THREAD_NUMBER / 4);
    }

    public void run() {
        while (active) {
            Iterator<SelectionKey> selectedKeys = selectKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (!key.isValid())
                    continue;
                if (key.isAcceptable()) {
                    try {
                        startConnection(key);
                    } catch (Exception e) {
                        System.err.println("[ERROR] Could not start connection! Reason: " + e.getMessage());
                    }
                }
                else if (key.isReadable()) {
                    Message message = null;
                    try {
                        message = this.read((SSLConnection) key.attachment());
                    } catch (Exception e) {
                        System.err.println("[ERROR] Server could not read message! Reason: " + e.getMessage());
                    }
                    if (message != null)
                        this.handleKey(message, key);
                }
            }
        }
    }

    public void handleKey(Message message, SelectionKey key) {
        if (message instanceof PutFileMessage || message instanceof GetFileMessage) key.cancel();
        scheduledExecutorReceiverService.submit(() -> {
            try {
                message.handleMessage();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Could not parse message. Reason: " + e.getMessage());
            }
        });
    }

    private Set<SelectionKey> selectKeys() {
        try {
            selector.select();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[ERROR] Exception thrown when selecting keys! Reason: " + e.getMessage());
        }
        return selector.selectedKeys();
    }

    private void startConnection(SelectionKey key) throws Exception {
        //System.out.println("server starting connection");
        /*
         * After you have created the SSLEngine, call the various set* methods to configure all aspects of the connection that is about to occur
         * (for example, setEnabledProtocols(), setEnabledCipherSuites(), setUseClientMode(), and setWantClientAuth()).
         * You can also configure the connection with the SSLParameters class, which enables you to set multiple settings in a single method call.
         */
        SSLEngine engine = sslContext.createSSLEngine();
        engine.setUseClientMode(false);

        /*
         * Once you have configured the connection and the buffers, call the beginHandshake() method,
         * which moves the SSLEngine into the initial handshaking state.
         */
        engine.beginHandshake();

        /*
         * Create the transport mechanism that the connection will use with, for example, the SocketChannel or Socket classes.
         */
        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        socketChannel.configureBlocking(false);

        SSLConnection connection = new SSLConnection(socketChannel, engine);

        if (handshake(connection)) {
            socketChannel.register(selector, SelectionKey.OP_READ, connection);
        } else {
            socketChannel.close();
        }
    }

    /*
     * Once the handshaking has completed, application data can now start flowing.
     * Call the wrap() method to take the bytes from the outbound application buffer, encrypt and protect them,
     * and then place them in the network buffer for transport to the peer.
     * Likewise, call the unwrap() method to decrypt and unprotect inbound network data.
     * The resulting application data is placed in the inbound application data buffer.
     */
    public Message read(SSLConnection connection) throws Exception {
        SSLEngine sslEngine = connection.getEngine();
        SocketChannel channel = connection.getChannel();
        ByteBuffer inNetBuffer = connection.getInNetBuffer();
        ByteBuffer inAppBuffer = connection.getInAppBuffer();

        inNetBuffer.clear();
        int bytesRead = channel.read(inNetBuffer);

        byte[] receivedMessage = new byte[0];

        if (bytesRead > 0) {
            inNetBuffer.flip();
            while (inNetBuffer.hasRemaining()) {
                inAppBuffer.clear();
                SSLEngineResult result = sslEngine.unwrap(inNetBuffer, inAppBuffer);
                switch (result.getStatus()) {
                    case OK:
                        inAppBuffer.flip();

                        byte[] tmp = new byte[receivedMessage.length + inAppBuffer.remaining()];
                        System.arraycopy(receivedMessage, 0, tmp, 0, receivedMessage.length);
                        System.arraycopy(inAppBuffer.array(), 0, tmp, receivedMessage.length, inAppBuffer.remaining());
                        receivedMessage = tmp;
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
            }

        } else if (bytesRead < 0) {
            handleEOS(connection);
        }

        try {
            return Message.parseMessage(peer, receivedMessage, receivedMessage.length, connection);
        } catch (Exception e) {
            return null;
        }
    }

    public void stop() {
        active = false;
        executor.shutdown();
        selector.wakeup();
    }

    public InetSocketAddress getBoundAddress() {
        return this.address;
    }

}
