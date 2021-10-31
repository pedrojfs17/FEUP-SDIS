package backupservice.comunication.message.chord;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class NewNodeMessage extends Message {
    private final int id;
    private final ChordKey succ;

    public NewNodeMessage(Peer peer, ChordKey sender, String body) {
        super(peer, sender, null);
        String[] elements = body.split(" ");

        id = Integer.parseInt(elements[0]);
        succ = ChordKey.fromString(elements[1]);
    }

    @Override
    public void handleMessage() {
        peer.setID(id);
        peer.getChordController().initFingerTable(succ);
        peer.getChordController().setPeriodicTasks();
    }

    public static byte[] buildMessage(ChordKey key, ChordKey succ, int id) {
        return (key.getString() + " NEW-NODE" + Utils.CRLF + Utils.CRLF + id + " " + succ.getString()).getBytes();
    }
}
