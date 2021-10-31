package comunication.message;

import peer.Peer;

public class StoredChunkTask implements Runnable {
    Peer peer;
    String fileId;
    int chunkNo;
    byte[] body;
    boolean alreadyHasChunk;

    public StoredChunkTask(Peer peer, String fileId, int chunkNo, byte[] body, boolean alreadyHasChunk) {
        this.peer = peer;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.body = body;
        this.alreadyHasChunk = alreadyHasChunk;
    }

    @Override
    public void run() {
        try {
            if (!this.alreadyHasChunk)
                peer.saveChunk(this.fileId, this.chunkNo, body);
            peer.sendStore(this.fileId, this.chunkNo);
        } catch (Exception e) {
            System.err.println("Did not save chunk! Reason: " + e.getMessage());
        }

    }
}
