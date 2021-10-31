package backupservice.comunication.message.chord;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class ReplaceSuccMessage extends Message {
    private final ChordKey newKey;

    public ReplaceSuccMessage(Peer peer, ChordKey sender, String newKey) {
        super(peer, sender,null);
        this.newKey = ChordKey.fromString(newKey);
    }

    @Override
    public void handleMessage() {
        peer.getChordController().replacePeer(sender,newKey);
    }

    public static byte[] buildMessage(ChordKey key, ChordKey newSucc) {
        return (key.getString() + " REPLACE-SUCC" + Utils.CRLF + Utils.CRLF + newSucc.getString()).getBytes();
    }
}
