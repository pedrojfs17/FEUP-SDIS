package backupservice.comunication.message.backup;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class DeleteMessage extends Message {
    public DeleteMessage(Peer peer, ChordKey sender, String fileId) {
        super(peer, sender, null);
        this.fileId = fileId;
    }
    @Override
    public void handleMessage() {
        peer.deleteChunks(this.fileId);
    }

    public static byte[] buildMessage(ChordKey key, String fileId) {
        return (key.getString() + " DELETE " + fileId + " " + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
