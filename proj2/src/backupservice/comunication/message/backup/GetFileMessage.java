package backupservice.comunication.message.backup;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.comunication.ssl.SSLConnection;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class GetFileMessage extends Message {

    public GetFileMessage(Peer peer, ChordKey sender, String fileId, SSLConnection connection) {
        super(peer, sender, connection);
        this.fileId = fileId;
    }

    @Override
    public void handleMessage() {
        try {
            if (peer.hasFile(this.fileId)) {
                peer.handleGetFile(fileId, connection);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Did not send file! Reason: " + e.getMessage());
        }
    }

    public static byte[] buildMessage(ChordKey key, String fileId) {
        return (key.getString() + " GETFILE "
                + fileId + " "
                + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
