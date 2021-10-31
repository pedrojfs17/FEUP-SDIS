package backupservice.comunication.message.chord;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.comunication.ssl.SSLConnection;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

public class NotifyMessage extends Message {

    public NotifyMessage(Peer peer, ChordKey sender, SSLConnection connection) {
        super(peer, sender, connection);
    }

    @Override
    public void handleMessage() {
        if (this.peer.getChordController().getPre() == null || sender.between(this.peer.getChordController().getPre(), this.peer.getKey(), true))
            this.peer.getChordController().notified(sender);
    }

    public static byte[] buildMessage(ChordKey key) {
        return (key.getString() + " NOTIFY" + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
