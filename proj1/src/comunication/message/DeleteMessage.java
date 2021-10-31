package comunication.message;

import peer.Peer;
import utils.Utils;

public class DeleteMessage extends Message {
    public DeleteMessage(Peer peer, String[] header) {
        super(peer);

        this.version = Double.parseDouble(header[0]);
        this.senderId = Integer.parseInt(header[2]);
        this.fileId = header[3];
    }

    @Override
    public void handleMessage() {
        peer.deleteChunks(this.fileId);
    }

    public static byte[] buildMessage(String version, int peerId, String fileId) {
        return (version + " DELETE "
                + peerId + " "
                + fileId + " " + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
