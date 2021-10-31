package backupservice.comunication.message.chord;

import backupservice.comunication.chord.ChordKey;
import backupservice.comunication.message.Message;
import backupservice.comunication.ssl.SSLConnection;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

import java.net.InetSocketAddress;

public class JoinMessage extends Message {

    public JoinMessage(Peer peer, ChordKey sender, SSLConnection connection) {
        super(peer, sender, connection);
    }

    @Override
    public void handleMessage() {
        InetSocketAddress newNodeAddress = sender.getAddress();
        ChordKey key = peer.getChordController().createRingKey(newNodeAddress);
        try {
            peer.sendServerReply(connection, NewNodeMessage.buildMessage(peer.getKey(),peer.getChordController().findSucc(key), key.getKey()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] buildMessage(InetSocketAddress address) {
        return (address.getHostString() + ":" + address.getPort() + ":-1" + " JOIN" + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
