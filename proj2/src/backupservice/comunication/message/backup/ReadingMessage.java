package backupservice.comunication.message.backup;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.comunication.ssl.SSLConnection;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class ReadingMessage extends Message {
    private final boolean backup;

    public ReadingMessage(Peer peer, ChordKey sender, String fileId, boolean backup, SSLConnection connection) {
        super(peer, sender, connection);
        this.fileId = fileId;
        this.backup = backup;
    }

    @Override
    public void handleMessage() {
        try {
            peer.sendFile(fileId, connection, backup);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ERROR] Error sending file!");
        }
    }

    public static byte[] buildMessage(ChordKey key, String fileId, boolean backup)  {
        if (backup)
            return (key.getString() + " READING-BACKUP " + fileId + " " + Utils.CRLF + Utils.CRLF).getBytes();
        else
            return (key.getString() + " READING-RESTORE " + fileId + " " + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
