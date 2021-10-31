package comunication.message;

import peer.Peer;
import utils.Utils;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RemovedMessage extends Message {
    public RemovedMessage(Peer peer, String[] header) {
        super(peer);

        this.version = Double.parseDouble(header[0]);
        this.senderId = Integer.parseInt(header[2]);
        this.fileId = header[3];
        this.chunkNo = Integer.parseInt(header[4]);
    }

    @Override
    public void handleMessage() {
        if (!peer.hasChunk(fileId, chunkNo)) return;
        if (!peer.decreaseReplicationDegree(fileId, chunkNo, senderId)) {
            Random random = new Random();
            try {
                peer.addToReplicateQueue(this.fileId + "_" + this.chunkNo);
                peer.getScheduledExecutorReceiverService().schedule(new ReplicateTask(peer, fileId, chunkNo), random.nextInt(401), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                System.err.println("Error replicating chunk! Reason: " + e.getMessage());
            }
        }
    }

    public static byte[] buildMessage(String version, int peerId, String fileId, int chunkNo) {
        return (version + " REMOVED "
                + peerId + " "
                + fileId + " "
                + chunkNo + " " + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
