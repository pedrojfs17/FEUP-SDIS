package backupservice.peer;

import backupservice.comunication.chord.ChordKey;
import backupservice.utils.Utils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class BackedUpFile implements Serializable {
    private final String path;
    private final String hashKey;
    private final Integer fileKey;
    private final ChordKey fileOwner;
    private final Set<Integer> keys;
    private int size;

    private final boolean owner;

    BackedUpFile(String path, String hashKey, Set<Integer> keys) {
        this.path = path;
        this.hashKey = hashKey;
        this.fileKey = null;
        this.fileOwner = null;
        this.keys = keys;
        this.owner = true;
    }

    BackedUpFile(String hashKey, int fileKey, ChordKey fileOwner) {
        this.path = null;
        this.hashKey = hashKey;
        this.fileKey = fileKey;
        this.fileOwner = fileOwner;
        this.keys = new HashSet<>();
        this.owner = false;
    }

    public void decreaseReplicationStatus(int fileKey) {
        this.keys.remove(fileKey);
    }

    public void increaseReplicationStatus(int fileKey) {
        this.keys.add(fileKey);
    }

    public int getActualReplicationDegree() {
        return this.keys.size();
    }

    public void setSize(int size) { this.size=size;}

    public int getSize() {
        return size;
    }

    public Set<Integer> getKeys() {
        return keys;
    }

    @Override
    public String toString() {
        if (this.owner) {
            return "-> " + Utils.getFileName(path) + "\n\n" +
                    "   Path: " + path + "\n" +
                    "   Size: " + size + "B\n" +
                    "   File Hash: " + hashKey + "\n" +
                    "   Replication Degree: " + this.keys.size() + "\n" +
                    "   File Keys: " + this.keys + "\n";
        }
        else {
            return "-> " + hashKey + "\n\n" +
                    "   Size: " + size + "B\n" +
                    "   File Key: " + fileKey + "\n" +
                    "   File Owner: " + fileOwner + "\n";
        }
    }

    public ChordKey getFileOwner() {
        return fileOwner;
    }

    public int getFileKey() {
        return fileKey;
    }
}
