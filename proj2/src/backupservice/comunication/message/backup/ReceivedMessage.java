package backupservice.comunication.message.backup;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.comunication.ssl.SSLConnection;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class ReceivedMessage extends Message {
    public ReceivedMessage(Peer peer, ChordKey sender, String fileId, SSLConnection connection) {
        super(peer, sender, connection);
        this.fileId = fileId;
    }

    @Override
    public void handleMessage() {
        try {
            peer.closeClientConnection(connection);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error closing connection!");
        }
    }

    public static byte[] buildMessage(ChordKey key, String fileId)  {
        return (key.getString() + " RECEIVED " + fileId + " " + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
