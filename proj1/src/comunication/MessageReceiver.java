package comunication;

import comunication.message.Message;
import peer.Peer;

public class MessageReceiver implements Runnable {
    private final byte[] msg;
    private final int packetSize;
    private final Peer peer;

    public MessageReceiver(byte[] msg, int packetSize, Peer peer) {
        this.msg = msg;
        this.packetSize = packetSize;
        this.peer = peer;
    }

    @Override
    public void run() {
        try {
            Message message = Message.parseMessage(this.peer, this.msg, this.packetSize);
            if (message != null)
                message.handleMessage();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not parse message. Reason: " + e.getMessage());
        }
    }
}
