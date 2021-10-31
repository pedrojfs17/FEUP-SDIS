package backupservice.comunication.message.chord;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class PredecessorMessage extends Message {
    private final ChordKey predecessor;

    public PredecessorMessage(Peer peer, ChordKey sender, String predecessor) {
        super(peer, sender, null);
        this.predecessor = predecessor.equals("EMPTY") ? null : ChordKey.fromString(predecessor);
    }

    @Override
    public void handleMessage() {}

    public static byte[] buildMessage(ChordKey key, ChordKey predecessor) {
        return (key.getString() + " PREDECESSOR" + Utils.CRLF + Utils.CRLF
                + (predecessor != null ? predecessor.getString() : "EMPTY")).getBytes();
    }

    public ChordKey getPredecessor() {
        return predecessor;
    }
}
