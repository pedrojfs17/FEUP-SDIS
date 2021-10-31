package backupservice.comunication.message.chord;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.comunication.ssl.SSLConnection;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class LookupSuccMessage extends Message {
    private final String lookupKey;

    public LookupSuccMessage(Peer peer, ChordKey sender, String lookupKey, SSLConnection connection) {
        super(peer, sender, connection);
        this.lookupKey = lookupKey;
    }

    @Override
    public void handleMessage() {
        try{
            ChordKey successor = peer.getChordController().lookup(new ChordKey(Integer.parseInt(lookupKey)));
            peer.sendServerReply(connection, SuccessorMessage.buildMessage(peer.getKey(), successor));
        } catch (Exception e) {
            System.out.println("Couldn't send successor msg: " + e.getMessage());
        }
    }

    public static byte[] buildMessage(ChordKey key, ChordKey search) {
        return (key.getString() + " LOOKUP-SUCC" + Utils.CRLF + Utils.CRLF + search.getKey()).getBytes();
    }
}
