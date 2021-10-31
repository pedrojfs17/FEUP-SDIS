package comunication.message;

import peer.Peer;
import utils.Utils;

public class StoredMessage extends Message {
    public StoredMessage(Peer peer, String[] header) {
        super(peer);

        this.version = Double.parseDouble(header[0]);
        this.senderId = Integer.parseInt(header[2]);
        this.fileId = header[3];
        this.chunkNo = Integer.parseInt(header[4]);
    }

    @Override
    public void handleMessage() {
        if (peer.hasChunk(fileId, chunkNo) || peer.isFileOwner(fileId))
            peer.increaseReplicationDegree(this.fileId, this.chunkNo, this.senderId);
    }

    public static byte[] buildMessage(String version, int peerId, String fileId, int chunkNo) {
        return (version + " STORED "
                + peerId + " "
                + fileId + " "
                + chunkNo + " " + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
