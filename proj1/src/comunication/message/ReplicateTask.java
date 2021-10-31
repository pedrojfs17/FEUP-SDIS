package comunication.message;

import peer.Peer;

public class ReplicateTask implements Runnable {

    Peer peer;
    String fileId;
    int chunkNo;

    public ReplicateTask(Peer peer, String fileId, int chunkNo) {
        this.peer = peer;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
    }

    @Override
    public void run() {
        peer.replicateChunk(fileId, chunkNo);
    }
}
