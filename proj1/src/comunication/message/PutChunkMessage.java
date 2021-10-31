package comunication.message;

import peer.Peer;
import utils.Utils;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class PutChunkMessage extends Message {
    PutChunkMessage(Peer peer, String[] header, byte[] body) {
        super(peer);

        this.version = Double.parseDouble(header[0]);
        this.senderId = Integer.parseInt(header[2]);
        this.fileId = header[3];
        this.chunkNo = Integer.parseInt(header[4]);
        this.replicationDegree = Integer.parseInt(header[5]);

        this.body = body;
    }

    @Override
    public void handleMessage() {
        if (peer.isFileOwner(fileId)) return;
        Random random = new Random();
        try {
            peer.updateReplicateQueue(this.fileId + "_" + this.chunkNo);
            boolean alreadyHasChunk = peer.putChunkHandler(this.fileId, this.chunkNo, this.replicationDegree, body);
            peer.getScheduledExecutorReceiverService().schedule(new StoredChunkTask(peer, fileId, chunkNo, body, alreadyHasChunk), random.nextInt(401), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Did not save chunk! Reason: " + e.getMessage());
        }
    }

    public static byte[] buildMessage(String version, int peerId, String fileId, int chunkNo, int replication, byte[] body, int bodySize) {
        byte[] header = (version + " PUTCHUNK "
                + peerId + " "
                + fileId + " "
                + chunkNo + " "
                + replication + " " + Utils.CRLF + Utils.CRLF).getBytes();

        byte[] msg = new byte[header.length + bodySize];

        System.arraycopy(header, 0, msg, 0, header.length);
        System.arraycopy(body, 0, msg, header.length, bodySize);

        return msg;
    }
}
