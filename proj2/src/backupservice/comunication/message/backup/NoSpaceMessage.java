package backupservice.comunication.message.backup;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.comunication.ssl.SSLConnection;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class NoSpaceMessage extends Message {
    private final int fileKey;

    public NoSpaceMessage(Peer peer, ChordKey sender, String fileId, String fileKey, SSLConnection connection) {
        super(peer, sender, connection);
        this.fileId = fileId;
        this.fileKey = Integer.parseInt(fileKey);
    }

    @Override
    public void handleMessage() {
        if (peer.isFileOwner(fileId)) {
            peer.decreaseReplicationDegree(fileId, fileKey);
        }

        try {
            peer.closeClientConnection(connection);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error closing connection!");
        }
    }

    public static byte[] buildMessage(ChordKey key, String fileId, int fileKey) {
        return (key.getString() +" NO-SPACE "
                + fileId + " "
                + fileKey + " " + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
