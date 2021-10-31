package comunication.message;

import peer.Peer;
import utils.Utils;

import java.util.Arrays;

public abstract class Message {
    protected Peer peer;

    protected double version;
    protected int senderId;
    protected String fileId;
    protected int chunkNo;
    protected int replicationDegree;

    protected byte[] body;

    public Message(Peer peer) {
        this.peer = peer;
    }

    public abstract void handleMessage();

    public static Message parseMessage(Peer peer, byte[] msg, int msgSize) throws Exception {
        int splitIndex = Utils.findByte(msg, Utils.CRLF + Utils.CRLF);

        byte[] headerArray = Arrays.copyOfRange(msg, 0, splitIndex);
        String[] header = new String(headerArray).split(" ");

        if (Integer.parseInt(header[2]) == peer.getIdentifier())
            return null;

        Message message;

        if (header[1].equalsIgnoreCase("PUTCHUNK"))
            message = new PutChunkMessage(peer, header, Arrays.copyOfRange(msg, splitIndex + 4, msgSize));
        else if (header[1].equalsIgnoreCase("STORED"))
            message = new StoredMessage(peer, header);
        else if (header[1].equalsIgnoreCase("DELETE"))
            message = new DeleteMessage(peer, header);
        else if (header[1].equalsIgnoreCase("GETCHUNK"))
            message = new GetChunkMessage(peer, header);
        else if (header[1].equalsIgnoreCase("CHUNK"))
            message = new ChunkMessage(peer, header, Arrays.copyOfRange(msg, splitIndex + 4, msgSize));
        else if (header[1].equalsIgnoreCase("REMOVED"))
            message = new RemovedMessage(peer, header);
        else if (header[1].equalsIgnoreCase("CONNECTED"))
            if (peer.isEnhanced()) message = new ConnectedMessage(peer, header, Arrays.copyOfRange(msg, splitIndex + 4, msgSize));
            else return null;
        else
            throw new Exception("Invalid Message");

        System.out.println(String.join(" ", header));

        return message;
    }
}
