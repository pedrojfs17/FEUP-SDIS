package peer;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Chunk implements Serializable {
    private final Set<Integer> actualReplicationDegree;
    private final int id;
    private final int size;
    private int replicationDegree;
    private final String fileId;

    Chunk(int id, int size, int replicationDegree, String fileId) {
        this.id = id;
        this.size = size;
        this.replicationDegree = replicationDegree;
        this.fileId = fileId;
        this.actualReplicationDegree = ConcurrentHashMap.newKeySet();
    }

    public void updateDesiredReplicationDegree(int replicationDegree) {
        this.replicationDegree = replicationDegree;
    }

    public void increaseReplicationStatus(int peerID) {
        this.actualReplicationDegree.add(peerID);
    }

    public void decreaseReplicationStatus(int peerID) {
        this.actualReplicationDegree.remove(peerID);
    }

    public int getId() {
        return id;
    }

    public boolean hasDesiredReplicationDegree() {
        return this.actualReplicationDegree.size() >= this.replicationDegree;
    }

    public int getActualReplicationDegree() {
        return actualReplicationDegree.size();
    }

    public int getDesiredReplicationDegree() {
        return this.replicationDegree;
    }

    public String getFileId() {
        return fileId;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#.###");
        return "    -> Chunk ID: " + fileId + "\n" +
                "       Chunk Size: " + df.format(this.size / 1000.0) + "KB\n" +
                "       Replication Degree (Perceived/Desired): " + actualReplicationDegree.size() + "/" + replicationDegree + "\n";
    }
}

