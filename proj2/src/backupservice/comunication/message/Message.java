package backupservice.comunication.message;

import backupservice.comunication.chord.*;
import backupservice.comunication.message.backup.*;
import backupservice.comunication.message.chord.*;
import backupservice.comunication.ssl.SSLConnection;
import backupservice.peer.Peer;
import backupservice.utils.Utils;

import java.util.Arrays;

public abstract class Message {
    protected Peer peer;
    protected final ChordKey sender;
    protected String fileId;
    protected SSLConnection connection;

    protected byte[] body;

    public Message(Peer peer, ChordKey sender, SSLConnection connection) {
        this.peer = peer;
        this.sender = sender;
        this.connection = connection;
    }

    public abstract void handleMessage();

    public static Message parseMessage(Peer peer, byte[] msg, int msgSize, SSLConnection connection) throws Exception {
        int splitIndex = Utils.findByte(msg, Utils.CRLF + Utils.CRLF);
        byte[] headerArray = Arrays.copyOfRange(msg, 0, splitIndex);
        String headerString = new String(headerArray);
        String[] header = headerString.split(" ");

        Message message;

        ChordKey sender = ChordKey.fromString(header[0]);

        if (header[1].equalsIgnoreCase("PUTFILE"))
            message = new PutFileMessage(peer, sender, header[2], header[3], Arrays.copyOfRange(msg, splitIndex + 4, msgSize), connection);
        else if (header[1].equalsIgnoreCase("READING-BACKUP"))
            message = new ReadingMessage(peer, sender, header[2], true, connection);
        else if (header[1].equalsIgnoreCase("READING-RESTORE"))
            message = new ReadingMessage(peer, sender, header[2], false, connection);
        else if (header[1].equalsIgnoreCase("STORED"))
            message = new StoredMessage(peer, sender, header[2], header[3], connection);
        else if (header[1].equalsIgnoreCase("DELETE"))
            message = new DeleteMessage(peer, sender, header[2]);
        else if (header[1].equalsIgnoreCase("GETFILE"))
            message = new GetFileMessage(peer, sender, header[2], connection);
        else if (header[1].equalsIgnoreCase("FILE"))
            message = new FileMessage(peer, sender, header[2], Arrays.copyOfRange(msg, splitIndex + 4, msgSize), connection);
        else if (header[1].equalsIgnoreCase("RECEIVED"))
            message = new ReceivedMessage(peer, sender, header[2], connection);
        else if (header[1].equalsIgnoreCase("REMOVED"))
            message = new RemovedMessage(peer, sender, header[2], header[3]);
        else if (header[1].equalsIgnoreCase("NO-SPACE"))
            message = new NoSpaceMessage(peer, sender, header[2], header[3], connection);
        // CHORD MESSAGES
        else if (header[1].equalsIgnoreCase("JOIN"))
            message = new JoinMessage(peer, sender, connection);
        else if (header[1].equalsIgnoreCase("NEW-NODE"))
            message = new NewNodeMessage(peer, sender, new String(Arrays.copyOfRange(msg, splitIndex + 4, msgSize)).trim());
        else if (header[1].equalsIgnoreCase("LOOKUP-SUCC"))
            message = new LookupSuccMessage(peer, sender, new String(Arrays.copyOfRange(msg, splitIndex + 4, msgSize)).trim(), connection);
        else if (header[1].equalsIgnoreCase("SUCCESSOR"))
            message = new SuccessorMessage(peer, sender, new String(Arrays.copyOfRange(msg, splitIndex + 4, msgSize)).trim());
        else if (header[1].equalsIgnoreCase("LOOKUP-PRED"))
            message = new LookupPredMessage(peer, sender, connection);
        else if (header[1].equalsIgnoreCase("PREDECESSOR"))
            message = new PredecessorMessage(peer, sender, new String(Arrays.copyOfRange(msg, splitIndex + 4, msgSize)).trim());
        else if (header[1].equalsIgnoreCase("NOTIFY"))
            message = new NotifyMessage(peer, sender, connection);
        else if (header[1].equalsIgnoreCase("REPLACE-SUCC"))
            message = new ReplaceSuccMessage(peer, sender, new String(Arrays.copyOfRange(msg, splitIndex + 4, msgSize)).trim());
        else if (header[1].equalsIgnoreCase("REPLACE-PRED"))
            message = new ReplacePredMessage(peer, sender, new String(Arrays.copyOfRange(msg, splitIndex + 4, msgSize)).trim());
        else
            throw new Exception("Invalid Message");

        System.out.println("[" + header[1] + "] " + header[0] + " | BODY: " + new String(Arrays.copyOfRange(msg, splitIndex + 4, msgSize)));

        return message;
    }

    public ChordKey getChordKey() {
        return sender;
    }
}
