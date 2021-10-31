package backupservice.peer;

import backupservice.comunication.ssl.SSLConnection;
import backupservice.comunication.ssl.SSLPeer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;

public class StorageManager implements Serializable {
    // Disk Space
    private int maxDiskSpace;
    private int usedDiskSpace;

    private final String ROOTDIR;
    private static final String CHUNKDIR = "/chunks/";
    private static final String FILESDIR = "/files/";

    // Peer maximum storage size -> 100Mb
    public static final int PEER_STORAGE_SIZE = 100000000;

    StorageManager(String accessPoint) {
        this.maxDiskSpace = PEER_STORAGE_SIZE;
        this.usedDiskSpace = 0;
        this.ROOTDIR = "/storage/" + accessPoint;

        try {
            String folderPath = System.getProperty("user.dir") + ROOTDIR;
            Path path = Paths.get(folderPath);
            Files.createDirectories(path);
            System.out.println("Main Directory is created!");

            Path chunksPath = Paths.get(folderPath + CHUNKDIR);
            Files.createDirectories(chunksPath);
            Path filesPath = Paths.get(folderPath + FILESDIR);
            Files.createDirectories(filesPath);

            System.out.println("Chunks and Files Directories are created!");
        } catch (IOException e) {
            System.err.println("Failed to create directories!" + e.getMessage());
        }
    }

    public String getFILESDIR() {
        return System.getProperty("user.dir") + ROOTDIR + FILESDIR;
    }

    public String getCHUNKDIR() {
        return System.getProperty("user.dir") + ROOTDIR + CHUNKDIR;
    }

    private synchronized void incrementUsedSpace(int space) {
        this.usedDiskSpace += space;
    }

    private synchronized void decrementUsedSpace(int space) {
        this.usedDiskSpace -= space;
    }

    public void reserveFileSpace(int fileSize) throws Exception {
        if (this.usedDiskSpace + fileSize > this.maxDiskSpace) {
            int neededSize = this.usedDiskSpace + fileSize - this.maxDiskSpace;
            throw new Exception("Not enough disk space to store chunk. Needed: " + neededSize);
        }

        this.incrementUsedSpace(fileSize);
    }

    public void saveBackedUpFile(String fileId, int fileSize, SSLPeer peer, SSLConnection connection) throws Exception {
        Path path = Paths.get(getCHUNKDIR() + fileId);
        FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        peer.receiveFile(connection, channel, fileSize);
    }

    public void saveRestoredFile(String fileId, int fileSize, SSLPeer peer, SSLConnection connection) throws Exception {
        Path path = Paths.get(getFILESDIR() + fileId);
        FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        System.out.println("[RESTORE] GOING TO RECEIVE FILE NOW");
        peer.receiveFile(connection, channel, fileSize);
    }

    public void deleteFile(String path) {
        File chunkFile = new File(getCHUNKDIR() + path);

        decrementUsedSpace((int) chunkFile.length());

        if (chunkFile.delete())
            System.out.println("Deleted file: " + path);
        else
            System.out.println("Failed to delete file: " + path);
    }

    public byte[] loadFile(String fileName) throws IOException {
        Path path = Paths.get(getCHUNKDIR() + fileName);
        return Files.readAllBytes(path);
    }

    public void setDiskSize(int diskSpace) {
        this.maxDiskSpace = diskSpace * 1000;
    }

    public int getAvailableSpace() {
        return maxDiskSpace - usedDiskSpace;
    }

    public String toString() {
        DecimalFormat df = new DecimalFormat("#.###");
        return "Peer Storage\n"
                + String.format("-> Total Disk Space: %sKB\n", df.format(this.maxDiskSpace / 1000.0))
                + String.format("-> Used Disk Space: %sKB\n", df.format(this.usedDiskSpace / 1000.0))
                + String.format("-> Available Disk Space: %sKB", df.format((this.maxDiskSpace - this.usedDiskSpace) / 1000.0));
    }
}
