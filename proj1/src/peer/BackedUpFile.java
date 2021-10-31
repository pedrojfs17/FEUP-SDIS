package peer;

import utils.Utils;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class BackedUpFile implements Serializable {
    private final String path;
    private final String hashKey;
    private int replicationDegree;
    private final ConcurrentHashMap<Integer, Chunk> chunks = new ConcurrentHashMap<>();

    BackedUpFile(String path, String hashKey, int replicationDegree) {
        this.path = path;
        this.hashKey = hashKey;
        this.replicationDegree = replicationDegree;
    }

    BackedUpFile(String hashKey, int replicationDegree) {
        this(null, hashKey, replicationDegree);
    }

    public void updateReplicationDegree(int replicationDegree) {
        this.replicationDegree = replicationDegree;
        chunks.forEach((integer, chunk) -> chunk.updateDesiredReplicationDegree(replicationDegree));
    }

    public void addChunk(Chunk chunk) {
        this.chunks.put(chunk.getId(), chunk);
    }

    public Chunk getChunkByID(int id) {
        return chunks.get(id);
    }

    public void removeChunk(Integer chunkNo) {
        this.chunks.remove(chunkNo);
    }

    public int getNumberOfChunks() {
        return this.chunks.size();
    }

    public ConcurrentHashMap<Integer, Chunk> getChunks() {
        return chunks;
    }

    public String chunksToString() {
        StringBuilder status = new StringBuilder();
        chunks.forEach((key, value) -> status.append(value.toString()));
        return status.toString();
    }

    @Override
    public String toString() {
        String status = "-> " + Utils.getFileName(path) +
                "\n\n   Path: " + path +
                "\n   File Hash: " + hashKey +
                "\n   Desired Replication Degree: " + replicationDegree +
                "\n\n   Chunks:\n";

        status += chunksToString() + "\n";
        return status;
    }
}
