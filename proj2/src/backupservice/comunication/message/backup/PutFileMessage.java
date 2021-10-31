package backupservice.comunication.message.backup;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.comunication.ssl.SSLConnection;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class PutFileMessage extends Message {
    private final int fileKey;
    private final ChordKey owner;
    private final int fileSize;

    public PutFileMessage(Peer peer, ChordKey sender, String fileId, String fileKey, byte[] msg, SSLConnection connection) {
        super(peer, sender, connection);
        this.fileId = fileId;
        this.fileKey = Integer.parseInt(fileKey);
        this.body = msg;

        String[] body = new String(msg).split(" ");
        this.owner = ChordKey.fromString(body[0]);
        this.fileSize = Integer.parseInt(body[1]);
    }

    @Override
    public void handleMessage() {
        if (peer.isFileOwner(fileId)) return;

        try {
            peer.putFileHandler(fileId, fileKey, fileSize, owner, connection);
        } catch (Exception e) {
            System.err.println("[ERROR] Did not save file!");
        }
    }

    public static byte[] buildMessage(ChordKey key, String fileId, int fileKey, byte[] body, int bodySize)  {
        byte[] header = (key.getString() + " PUTFILE "
                + fileId + " "
                + fileKey + " " + Utils.CRLF + Utils.CRLF).getBytes();

        byte[] msg = new byte[header.length + bodySize];

        System.arraycopy(header, 0, msg, 0, header.length);
        System.arraycopy(body, 0, msg, header.length, bodySize);

        return msg;
    }
}
