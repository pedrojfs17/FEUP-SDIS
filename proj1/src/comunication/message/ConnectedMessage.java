package comunication.message;

import peer.Peer;
import utils.Utils;

import java.nio.charset.StandardCharsets;

public class ConnectedMessage extends Message {
    protected String ip;
    protected int port;

    public ConnectedMessage(Peer peer, String[] header, byte[] connectionDetails) {
        super(peer);

        this.version = Double.parseDouble(header[0]);
        this.senderId = Integer.parseInt(header[2]);

        String[] connection = new String(connectionDetails, StandardCharsets.UTF_8).trim().split(":");
        this.ip = connection[0];
        this.port = Integer.parseInt(connection[1]);
    }

    @Override
    public void handleMessage() {
        try {
            peer.resendDeleteMessages(this.ip, this.port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] buildMessage(String version, int peerId, byte[] connectionDetails) {
        byte[] header = (version + " CONNECTED " + peerId + " " + Utils.CRLF + Utils.CRLF).getBytes();

        byte[] msg = new byte[header.length + connectionDetails.length];

        System.arraycopy(header, 0, msg, 0, header.length);
        System.arraycopy(connectionDetails, 0, msg, header.length, connectionDetails.length);

        return msg;
    }
}
