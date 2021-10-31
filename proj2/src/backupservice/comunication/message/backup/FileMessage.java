package backupservice.comunication.message.backup;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.comunication.ssl.SSLConnection;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class FileMessage extends Message {
    private final int fileSize;

    public FileMessage(Peer peer, ChordKey sender, String fileId, byte[] msg, SSLConnection connection) {
        super(peer, sender, connection);
        this.fileId = fileId;
        this.fileSize = Integer.parseInt(new String(msg));
    }

    @Override
    public void handleMessage() {
        try {
            if (peer.isFileOwner(fileId))
                peer.handleFile(fileId, fileSize, connection);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static byte[] buildMessage(ChordKey key, String fileId, byte[] body) {
        byte[] header = (key.getString() + " FILE "
                + fileId + " "
                + Utils.CRLF + Utils.CRLF).getBytes();

        byte[] msg = new byte[header.length + body.length];

        System.arraycopy(header, 0, msg, 0, header.length);
        System.arraycopy(body, 0, msg, header.length, body.length);

        return msg;
    }
}
