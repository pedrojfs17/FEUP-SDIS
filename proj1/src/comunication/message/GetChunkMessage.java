package comunication.message;

import peer.Peer;
import utils.Utils;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GetChunkMessage extends Message {
    public GetChunkMessage(Peer peer, String[] header) {
        super(peer);

        this.version = Double.parseDouble(header[0]);
        this.senderId = Integer.parseInt(header[2]);
        this.fileId = header[3];
        this.chunkNo = Integer.parseInt(header[4]);
    }

    @Override
    public void handleMessage() {
        Random random = new Random();
        try {
            if (!peer.hasChunk(this.fileId, this.chunkNo)) return;
            peer.addToRestoreQueue(this.fileId + "_" + this.chunkNo);
            peer.getScheduledExecutorReceiverService().schedule(new SendChunkTask(peer, fileId, chunkNo, version), random.nextInt(401), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Did not send chunk! Reason: " + e.getMessage());
        }
    }

    public static byte[] buildMessage(String version, int peerId, String fileId, int chunkNo) {
        return (version + " GETCHUNK "
                + peerId + " "
                + fileId + " "
                + chunkNo + " " + Utils.CRLF + Utils.CRLF).getBytes();
    }
}
