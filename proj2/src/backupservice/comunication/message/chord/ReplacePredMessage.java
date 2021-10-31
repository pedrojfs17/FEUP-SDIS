package backupservice.comunication.message.chord;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class ReplacePredMessage extends Message {
    private final ChordKey newKey;

    public ReplacePredMessage(Peer peer, ChordKey sender, String newKey) {
        super(peer, sender, null);
        this.newKey = ChordKey.fromString(newKey);
    }

    @Override
    public void handleMessage() {
        peer.getChordController().replacePred(sender, newKey);
    }

    public static byte[] buildMessage(ChordKey key, ChordKey newPred) {
        return (key.getString() + " REPLACE-PRED" + Utils.CRLF + Utils.CRLF + newPred.getString()).getBytes();
    }
}
