package backupservice.comunication.message.chord;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.comunication.ssl.SSLConnection;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class LookupPredMessage extends Message {
    public LookupPredMessage(Peer peer, ChordKey sender, SSLConnection connection) {
        super(peer, sender, connection);
    }

    @Override
    public void handleMessage() {
        try {
            ChordKey predecessor = peer.getChordController().getPre();
            peer.sendServerReply(connection, PredecessorMessage.buildMessage(peer.getKey(), predecessor));
        } catch (Exception e) {
            System.out.println("Couldn't send msg: " + e.getMessage());
        }
    }

    public static byte[] buildMessage(ChordKey key) {
        return (key.getString() + " LOOKUP-PRED" + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
