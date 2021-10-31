package backupservice.comunication.ssl;

import javax.net.ssl.SSLEngine;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SSLConnection {
    private final SocketChannel channel;
    private final SSLEngine engine;
    protected ByteBuffer outAppBuffer;
    protected ByteBuffer outNetBuffer;
    protected ByteBuffer inAppBuffer;
    protected ByteBuffer inNetBuffer;
    private final boolean blocking;

    public SSLConnection(SocketChannel channel, SSLEngine engine) {
        this(channel, engine, false);
    }

    public SSLConnection(SocketChannel channel, SSLEngine engine, boolean blocking) {
        this.channel = channel;
        this.engine = engine;
        outAppBuffer = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
        outNetBuffer = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        inAppBuffer = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
        inNetBuffer = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        this.blocking = blocking;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public SSLEngine getEngine() {
        return engine;
    }

    public ByteBuffer getOutAppBuffer() {
        return outAppBuffer;
    }

    public ByteBuffer getInAppBuffer() {
        return inAppBuffer;
    }

    public void setInAppBuffer(ByteBuffer inAppBuffer) {
        this.inAppBuffer = inAppBuffer;
    }

    public ByteBuffer getInNetBuffer() {
        return inNetBuffer;
    }

    public void setInNetBuffer(ByteBuffer inNetBuffer) {
        this.inNetBuffer = inNetBuffer;
    }

    public ByteBuffer getOutNetBuffer() {
        return outNetBuffer;
    }

    public void setOutNetBuffer(ByteBuffer outNetBuffer) {
        this.outNetBuffer = outNetBuffer;
    }

    public boolean isBlocking() {
        return blocking;
    }
}
