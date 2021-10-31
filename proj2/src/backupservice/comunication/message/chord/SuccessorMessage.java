package backupservice.comunication.message.chord;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class SuccessorMessage extends Message {
    private final ChordKey successor;

    public SuccessorMessage(Peer peer, ChordKey sender, String successor) {
        super(peer, sender, null);
        this.successor = successor.equals("EMPTY") ? null : ChordKey.fromString(successor);
    }

    @Override
    public void handleMessage() {}

    public static byte[] buildMessage(ChordKey key, ChordKey successorKey) {
        return (key.getString() + " SUCCESSOR " + Utils.CRLF + Utils.CRLF
                + (successorKey != null ? successorKey.getString() : "EMPTY")).getBytes();
    }

    public ChordKey getSuccessor() {
        return successor;
    }
}
