package backupservice.comunication.message.backup;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class RemovedMessage extends Message {
    private final int fileKey;

    public RemovedMessage(Peer peer, ChordKey sender, String fileId, String fileKey) {
        super(peer, sender, null);
        this.fileId = fileId;
        this.fileKey = Integer.parseInt(fileKey);
    }

    @Override
    public void handleMessage() {
        if (peer.isFileOwner(fileId)) {
            peer.decreaseReplicationDegree(fileId, fileKey);
        }
    }

    public static byte[] buildMessage(ChordKey key, String fileId, int fileKey) {
        return (key.getString() +" REMOVED "
                + fileId + " "
                + fileKey + " " + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
