package peer;

import comunication.MessageReceiver;
import comunication.MulticastListener;
import comunication.RMI;
import comunication.message.*;
import comunication.transmitter.ChunkMessageSender;
import comunication.transmitter.MessageSender;
import comunication.transmitter.MultipleMessageSender;
import utils.BiMap;
import utils.Utils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class Peer implements RMI, Serializable {
    private transient String version;
    private final String accessPoint;
    private final int identifier;
    private final StorageManager storageManager;
    public static int CHUNK_SIZE = 64000;
    private static final int THREAD_NUMBER = 32;

    // Multicast Sockets
    private transient MulticastListener mc;
    private transient MulticastListener mdb;
    private transient MulticastListener mdr;

    // Thread Pool
    private transient ScheduledExecutorService scheduledExecutorSenderService;
    private transient ScheduledExecutorService scheduledExecutorReceiverService;

    // State
    private final BiMap<String, String> fileHashes;
    private final ConcurrentHashMap<String, BackedUpFile> backupHash;
    private final ConcurrentHashMap<String, BackedUpFile> chunkHash;
    private final ConcurrentHashMap<String, Boolean> restoredHash;
    private final ConcurrentHashMap<String, Boolean> replicatedHash;
    private final Set<String> deletedFiles;
    private final ConcurrentHashMap<String, Set<Integer>> restoredChunks;

    private transient Semaphore lock;

    Peer(String[] args) {
        this.version = args[0];
        this.identifier = Integer.parseInt(args[1]);
        this.accessPoint = args[2];
        this.storageManager = new StorageManager(identifier);

        this.fileHashes = new BiMap<>();
        this.backupHash = new ConcurrentHashMap<>();
        this.chunkHash = new ConcurrentHashMap<>();
        this.restoredHash = new ConcurrentHashMap<>();
        this.replicatedHash = new ConcurrentHashMap<>();
        this.deletedFiles = ConcurrentHashMap.newKeySet();
        this.restoredChunks = new ConcurrentHashMap<>();

        try {
            this.createSockets(args);
        } catch (Exception e) {
            System.err.println("Couldn't create Multicast Sockets");
        }

        this.createSaveStateProcess();
    }

    public static void main(String[] args) {
        if (args.length != 9) {
            System.out.println("Usage: java peer.Peer <protocol_version> <identifier> <access_point> <MC_IP> <MC_PORT> <MDB_IP> <MDB_PORT> <MDR_IP> <MDR_PORT>");
            return;
        }

        try {
            Peer peer;
            boolean firstConnection = false;

            if (!Utils.checkForSavedState(args)) {
                peer = new Peer(args);
                firstConnection = true;
            } else {
                System.out.println("loaded");
                peer = Utils.getSavedState(args);
                peer.version = args[0];
                peer.createSockets(args);
                peer.createSaveStateProcess();
            }

            RMI peerStub = (RMI) UnicastRemoteObject.exportObject(peer, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(args[2], peerStub);

            System.out.println("Peer ready");

            if (!firstConnection && peer.isEnhanced()) peer.sendConnectedMessage();
        } catch (Exception e) {
            System.err.println("Peer exception: " + e);
            e.printStackTrace();
        }
    }

    private void createSaveStateProcess() {
        ScheduledExecutorService scheduledExecutorSaveService = Executors.newScheduledThreadPool(1);

        Runnable savingProcess = () -> {
            try {
                FileOutputStream fileOutputStream
                        = new FileOutputStream(System.getProperty("user.dir") + "/../../storage/peer" + identifier + "/" + accessPoint);
                ObjectOutputStream objectOutputStream
                        = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(this);
                objectOutputStream.flush();
                objectOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Saved peer state");
        };

        Runtime.getRuntime().addShutdownHook(new Thread(savingProcess));
        scheduledExecutorSaveService.scheduleWithFixedDelay(savingProcess, 1, 1, TimeUnit.MINUTES);
    }

    private void createSockets(String[] args) throws IOException {
        // Multicast Control Channel
        this.mc = new MulticastListener(this, args[3], Integer.parseInt(args[4]));
        Thread mc = new Thread(this.mc);
        mc.start();

        // Multicast Data Backup Channel
        this.mdb = new MulticastListener(this, args[5], Integer.parseInt(args[6]));
        Thread mdb = new Thread(this.mdb);
        mdb.start();

        // Multicast Data Restore Channel
        this.mdr = new MulticastListener(this, args[7], Integer.parseInt(args[8]));
        Thread mdr = new Thread(this.mdr);
        mdr.start();

        //Create ThreadPool for processing messages
        this.scheduledExecutorSenderService =
                Executors.newScheduledThreadPool(THREAD_NUMBER / 4);

        this.scheduledExecutorReceiverService =
                Executors.newScheduledThreadPool(THREAD_NUMBER * 2);

        this.lock = new Semaphore(THREAD_NUMBER);
    }

    @Override
    public String backup(String filePath, int replicationDegree) throws RemoteException {
        try {
            this.readChunks(filePath, replicationDegree);
            return "Backed up " + Utils.getFileName(filePath) + " with replication: " + replicationDegree;
        } catch (Exception e) {
            return "Backup Failed: " + e.getMessage();
        }
    }

    @Override
    public String restore(String filePath) throws RemoteException {
        try {
            this.restoreFile(filePath);
            return "Restored file " + filePath;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    public String delete(String filePath) throws RemoteException {
        try {
            this.deleteFile(filePath);
            return "Deleted file " + filePath;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    public String reclaim(int diskSpace) throws RemoteException {
        try {
            if (diskSpace >= 0) {
                this.reclaimSpace(diskSpace);
                return "Peer " + identifier + " has now " + diskSpace + "KB of usable disk space.";
            } else throw new Exception("Invalid disk space parameter.");
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    public String state() throws RemoteException {
        return this.printState();
    }

    public int getIdentifier() {
        return this.identifier;
    }

    public ScheduledExecutorService getScheduledExecutorReceiverService() {
        return scheduledExecutorReceiverService;
    }

    public boolean isEnhanced() {
        return version.equalsIgnoreCase("2.0");
    }

    public void receivedMessage(byte[] message, int messageSize) {
        scheduledExecutorReceiverService.submit(new MessageReceiver(message, messageSize, this));
    }

    private void sendConnectedMessage() throws IOException {
        ServerSocket tcpSocket = new ServerSocket(0);

        byte[] connectionDetails = (tcpSocket.getInetAddress().getHostAddress() + ":" + tcpSocket.getLocalPort()).getBytes(StandardCharsets.UTF_8);

        this.scheduledExecutorSenderService.submit(new MessageSender(
                this.mc,
                scheduledExecutorSenderService,
                ConnectedMessage.buildMessage(this.version, this.identifier, connectionDetails)
        ));

        tcpSocket.setSoTimeout(1000);
        tcpSocket.setReceiveBufferSize(64);

        try {
            while (true) {
                Socket clientSocket = tcpSocket.accept();
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                byte[] msg = in.readAllBytes();

                clientSocket.close();
                in.close();

                this.receivedMessage(msg, msg.length);
            }
        } catch (Exception e) {
            System.out.println("Timed out connection, closing TCP");
        }

        tcpSocket.close();
    }

    private void readChunks(String filePath, int replicationDegree) throws Exception {
        String fileHashKey = Utils.bytesToHexString(Utils.hashKey(filePath));

        if (fileHashes.get(filePath) != null && !fileHashes.get(filePath).equals(fileHashKey))
            throw new Exception("Previous version of this file has already been stored!\nPlease delete it before backing up the new version!");

        fileHashes.put(filePath, fileHashKey);
        backupHash.put(fileHashKey, new BackedUpFile(filePath, fileHashKey, replicationDegree));

        byte[] chunkBuffer = new byte[CHUNK_SIZE];
        FileInputStream fileStream = new FileInputStream(System.getProperty("user.dir") + "/" + filePath);
        boolean multipleSize = fileStream.available() % CHUNK_SIZE == 0;

        lock.acquire();
        int rc = fileStream.read(chunkBuffer);
        int id = 0;

        while (rc != -1) {
            Chunk chunk = new Chunk(id, rc, replicationDegree, fileHashKey + "_" + id);
            backupHash.get(fileHashKey).addChunk(chunk);

            this.scheduledExecutorSenderService.submit(new ChunkMessageSender(
                    this.mdb,
                    scheduledExecutorSenderService,
                    1000,
                    PutChunkMessage.buildMessage(this.version, this.identifier, fileHashKey, id, replicationDegree, chunkBuffer, rc),
                    chunk,
                    this.lock,
                    null,
                    null
            ));

            lock.acquire();
            rc = fileStream.read(chunkBuffer);
            id++;
        }

        if (multipleSize) {
            Chunk chunk = new Chunk(id, 0, replicationDegree, fileHashKey + "_" + id);
            backupHash.get(fileHashKey).addChunk(chunk);

            this.scheduledExecutorSenderService.submit(new ChunkMessageSender(
                    this.mdb,
                    scheduledExecutorSenderService,
                    1000,
                    PutChunkMessage.buildMessage(this.version, this.identifier, fileHashKey, id, replicationDegree, new byte[0], 0),
                    chunk
            ));
        }
    }

    public boolean putChunkHandler(String fileId, int chunkNo, int replicationDegree, byte[] chunk) {
        BackedUpFile chunkFile = chunkHash.get(fileId);

        if (chunkFile == null) {
            chunkFile = new BackedUpFile(fileId, replicationDegree);
            this.chunkHash.put(fileId, chunkFile);
        }
        else {
            chunkFile.updateReplicationDegree(replicationDegree);
        }

        if (chunkFile.getChunkByID(chunkNo) != null) {
            return true;
        }

        Chunk newChunk = new Chunk(chunkNo, chunk.length, replicationDegree, fileId + "_" + chunkNo);
        chunkHash.get(fileId).addChunk(newChunk);

        return false;
    }

    public void saveChunk(String fileId, int chunkNo, byte[] chunkBody) throws Exception {
        Chunk chunk = chunkHash.get(fileId).getChunkByID(chunkNo);

        if (this.isEnhanced() && chunk.hasDesiredReplicationDegree()) {
            chunkHash.get(fileId).removeChunk(chunkNo);
            if (this.chunkHash.get(fileId).getNumberOfChunks() == 0)
                this.chunkHash.remove(fileId);
            throw new Exception("Chunk already has desired replication degree.");
        }

        try {
            this.storageManager.saveChunk(fileId + "_" + chunkNo, chunkBody);
        } catch (Exception e) {
            chunkHash.get(fileId).removeChunk(chunkNo);
            int neededSize = Integer.parseInt(e.getMessage().split(": ")[1]);

            while (neededSize > 0) {
                int size = this.reclaimRedundantSpace();

                if (size < 0) {
                    if (this.chunkHash.get(fileId).getNumberOfChunks() == 0)
                        this.chunkHash.remove(fileId);
                    throw new Exception("Not enough disk space to store chunk.");
                }

                neededSize -= size;
            }

            this.storageManager.saveChunk(fileId + "_" + chunkNo, chunkBody);
            this.chunkHash.get(fileId).addChunk(chunk);
        }

        chunk.increaseReplicationStatus(this.identifier);
    }

    public void restoreFile(String fileName) throws Exception {
        String fileHash = fileHashes.get(fileName);

        if (fileHash == null) throw new Exception("File has not been backed up by this peer!");

        System.out.println("Going to restore file " + fileName);
        ConcurrentHashMap<Integer, Chunk> toRestore = this.backupHash.get(fileHash).getChunks();

        this.restoredChunks.put(fileHash, ConcurrentHashMap.newKeySet());

        toRestore.forEach((integer, chunk) ->
            this.scheduledExecutorSenderService.submit(new ChunkMessageSender(
                    this.mc,
                    scheduledExecutorSenderService,
                    1000,
                    GetChunkMessage.buildMessage(this.version, this.identifier, fileHash, integer),
                    this.restoredChunks.get(fileHash),
                    integer
            ))
        );
    }

    public void sendStore(String fileId, int chunkNo) {
        this.scheduledExecutorSenderService.submit(new MessageSender(
                this.mc,
                scheduledExecutorSenderService,
                StoredMessage.buildMessage(this.version, this.identifier, fileId, chunkNo)
        ));
    }

    public boolean isFileOwner(String fileID) {
        return this.backupHash.containsKey(fileID);
    }

    public void increaseReplicationDegree(String fileID, int chunkID, int peerIdentifier) {
        if (this.isFileOwner(fileID)) {
            this.backupHash.get(fileID).getChunkByID(chunkID).increaseReplicationStatus(peerIdentifier);
        } else {
            this.chunkHash.get(fileID).getChunkByID(chunkID).increaseReplicationStatus(peerIdentifier);
        }
    }

    public boolean decreaseReplicationDegree(String fileID, int chunkID, int peerIdentifier) {
        if (this.isFileOwner(fileID)) {
            this.backupHash.get(fileID).getChunkByID(chunkID).decreaseReplicationStatus(peerIdentifier);
            return this.backupHash.get(fileID).getChunkByID(chunkID).hasDesiredReplicationDegree();
        } else {
            this.chunkHash.get(fileID).getChunkByID(chunkID).decreaseReplicationStatus(peerIdentifier);
            return this.chunkHash.get(fileID).getChunkByID(chunkID).hasDesiredReplicationDegree();
        }
    }

    private void deleteFile(String filePath) {
        String fileID = this.fileHashes.get(filePath);
        this.backupHash.remove(fileID);
        this.deletedFiles.add(fileID);
        this.fileHashes.remove(filePath);
        System.out.println("Removed file: " + fileID + " from registry");

        this.scheduledExecutorSenderService.submit(new MultipleMessageSender(
                this.mc,
                scheduledExecutorSenderService,
                1000,
                DeleteMessage.buildMessage(this.version, identifier, fileID),
                3
        ));
    }

    public void deleteChunks(String fileID) {
        if (this.chunkHash.containsKey(fileID)) {
            ConcurrentHashMap<Integer, Chunk> chunks = this.chunkHash.remove(fileID).getChunks();
            chunks.forEach(((integer, chunk) -> this.storageManager.deleteFile(chunk.getFileId())));
        }
    }

    public boolean hasChunk(String fileId, int chunkNo) {
        BackedUpFile file = this.chunkHash.get(fileId);
        return file != null && file.getChunkByID(chunkNo) != null;
    }

    public void addToRestoreQueue(String chunkID) {
        this.restoredHash.put(chunkID, false);
    }

    public void addToReplicateQueue(String chunkID) {
        this.replicatedHash.put(chunkID, false);
    }

    public void updateRestoreQueue(String s) {
        this.restoredHash.put(s, true);
    }

    public void updateReplicateQueue(String s) {
        this.replicatedHash.put(s, true);
    }

    public void sendChunk(String fileId, int chunkNo, double senderVersion) throws IOException {
        if (this.restoredHash.remove(fileId + "_" + chunkNo)) return;

        byte[] chunkContent;

        try {
            chunkContent = this.storageManager.loadFile(fileId + "_" + chunkNo);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Couldn't read chunk");
            return;
        }

        if (this.isEnhanced() && senderVersion == 2.0) {
            sendChunkWithTCP(fileId, chunkNo, chunkContent);
        } else {
            MessageSender msg = new MessageSender(
                    this.mdr,
                    scheduledExecutorSenderService,
                    ChunkMessage.buildMessage(this.version, this.identifier, fileId, chunkNo, chunkContent)
            );
            msg.run();
        }
    }

    public void sendChunkWithTCP(String fileId, int chunkNo, byte[] chunkContent) throws IOException {
        ServerSocket tcpSocket = new ServerSocket(0);

        byte[] connectionDetails = (tcpSocket.getInetAddress().getHostAddress() + ":" + tcpSocket.getLocalPort()).getBytes(StandardCharsets.UTF_8);

        MessageSender msg = new MessageSender(
                this.mdr,
                scheduledExecutorSenderService,
                ChunkMessage.buildMessage(this.version, this.identifier, fileId, chunkNo, connectionDetails)
        );
        msg.run();

        Socket clientSocket = tcpSocket.accept();
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        out.write(chunkContent);

        out.close();
        clientSocket.close();
        tcpSocket.close();
    }

    public void composeFile(String fileID, int chunkID, byte[] body, double msgVersion) throws IOException {
        if (this.restoredChunks.get(fileID).contains(chunkID)) return;

        String filePath = "r_" + Utils.getFileName(this.fileHashes.getKey(fileID));

        byte[] chunkContent;

        if (this.isEnhanced() && msgVersion == 2.0) {
            String[] tcpDetails = new String(body).split(":");
            Socket clientSocket = new Socket(tcpDetails[0], Integer.parseInt(tcpDetails[1]));
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            chunkContent = in.readAllBytes();
            in.close();
            clientSocket.close();
        } else {
            chunkContent = body;
        }

        try {
            this.storageManager.writeToFile(filePath, chunkContent, chunkID * CHUNK_SIZE);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Couldn't write to File");
        }

        this.restoredChunks.get(fileID).add(chunkID);
        if (this.restoredChunks.get(fileID).size() == this.backupHash.get(fileID).getNumberOfChunks())
            System.out.println("All chunks restored");
    }

    private void reclaimSpace(int diskSpace) {
        boolean redundancy = true;
        this.storageManager.setDiskSize(diskSpace);
        while (this.storageManager.getAvailableSpace() < 0) {
            if (redundancy && this.reclaimRedundantSpace() < 0) redundancy = false;
            else this.reclaimChunk();
        }
    }

    private void reclaimChunk() {
        for (String fileHash : this.chunkHash.keySet()) {
            BackedUpFile file = this.chunkHash.get(fileHash);
            ConcurrentHashMap<Integer, Chunk> chunks = file.getChunks();

            for (Integer chunkNo : chunks.keySet()) {
                Chunk chunk = chunks.get(chunkNo);
                storageManager.deleteFile(chunk.getFileId());
                this.chunkHash.get(fileHash).removeChunk(chunkNo);
                if (this.chunkHash.get(fileHash).getNumberOfChunks() == 0)
                    this.chunkHash.remove(fileHash);
                this.sendRemoved(fileHash, chunkNo);
                return;
            }
        }
    }

    private int reclaimRedundantSpace() {
        for (String fileHash : this.chunkHash.keySet()) {
            BackedUpFile file = this.chunkHash.get(fileHash);
            ConcurrentHashMap<Integer, Chunk> chunks = file.getChunks();

            for (Integer chunkNo : chunks.keySet()) {
                Chunk chunk = chunks.get(chunkNo);

                if (chunk.getActualReplicationDegree() > chunk.getDesiredReplicationDegree()) {
                    storageManager.deleteFile(chunk.getFileId());
                    this.chunkHash.get(fileHash).removeChunk(chunkNo);
                    if (this.chunkHash.get(fileHash).getNumberOfChunks() == 0)
                        this.chunkHash.remove(fileHash);
                    this.sendRemoved(fileHash, chunkNo);

                    return chunk.getSize();
                }
            }
        }

        return -1;
    }

    public void sendRemoved(String fileId, int chunkNo) {
        this.scheduledExecutorSenderService.submit(new MessageSender(
                this.mc,
                this.scheduledExecutorSenderService,
                RemovedMessage.buildMessage(this.version, this.identifier, fileId, chunkNo)
        ));
    }

    public void replicateChunk(String fileId, int chunkNo) {
        if (this.replicatedHash.remove(fileId + "_" + chunkNo)) return;

        byte[] chunkContent;

        try {
            chunkContent = this.storageManager.loadFile(fileId + "_" + chunkNo);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Couldn't read chunk");
            return;
        }

        Chunk chunk = this.chunkHash.get(fileId).getChunkByID(chunkNo);

        this.scheduledExecutorSenderService.submit(new ChunkMessageSender(
                this.mdb,
                scheduledExecutorSenderService,
                1000,
                PutChunkMessage.buildMessage(this.version, this.identifier, fileId, chunkNo, chunk.getDesiredReplicationDegree(), chunkContent, chunkContent.length),
                chunk
        ));

        this.scheduledExecutorSenderService.schedule(new MessageSender(
                this.mc,
                scheduledExecutorSenderService,
                StoredMessage.buildMessage(this.version, this.identifier, fileId, chunkNo)
        ), 200, TimeUnit.MILLISECONDS);
    }

    public void resendDeleteMessages(String ip, int port) throws IOException {
        for (String fileId : this.deletedFiles) {
            Socket clientSocket = new Socket(ip, port);
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            out.write(DeleteMessage.buildMessage(this.version, this.identifier, fileId));
            out.close();
            clientSocket.close();
        }
    }

    private String printState() {
        StringBuilder state = new StringBuilder();

        state.append("Identifier: ").append(this.identifier)
                .append("\nAccess Point: ").append(this.accessPoint)
                .append("\nVersion: ").append(this.version);

        if (this.backupHash.size() == 0) state.append("\n-----\nNo Backed Up Files\n");
        else {
            state.append("\n-----\nBacked Up Files:\n");
            this.backupHash.forEach((key, value) -> state.append(value.toString()));
        }

        if (this.chunkHash.size() == 0) state.append("-----\nNo Saved Chunks\n");
        else {
            state.append("-----\nSaved Chunks:\n");
            for (Map.Entry<String, BackedUpFile> chunk : this.chunkHash.entrySet()) {
                state.append(chunk.getValue().chunksToString());
            }
        }

        state.append("-----\n").append(this.storageManager.toString());

        return state.toString();
    }
}
