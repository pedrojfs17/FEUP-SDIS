package comunication.message;

import peer.Peer;

import java.io.IOException;


public class SendChunkTask implements Runnable {
    Peer peer;
    String fileId;
    int chunkNo;
    double msgVersion;

    public SendChunkTask(Peer peer, String fileId, int chunkNo, double msgVersion) {
        this.peer = peer;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.msgVersion = msgVersion;
    }

    @Override
    public void run() {
        try {
            peer.sendChunk(this.fileId, this.chunkNo, msgVersion);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Couldn't send chunk");
        }
    }

}
