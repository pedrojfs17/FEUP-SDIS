package backupservice.peer;

import backupservice.comunication.*;
import backupservice.comunication.chord.*;
import backupservice.comunication.message.*;
import backupservice.comunication.message.backup.*;
import backupservice.comunication.ssl.*;
import backupservice.utils.BiMap;
import backupservice.utils.Utils;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;

public class Peer implements RMI, Serializable {
    private final StorageManager storageManager;
    private static final int THREAD_NUMBER = 32;
    private final InetSocketAddress address;
    private final ChordKey key;
    private final boolean bootPeer;

    // Thread Pool
    private final ScheduledExecutorService scheduledExecutorReceiverService;

    // State
    private final BiMap<String, String> fileHashes;
    private final ConcurrentHashMap<String, BackedUpFile> myFiles;
    private final ConcurrentHashMap<String, BackedUpFile> backedUpFiles;

    private final SSLClient client;
    private final SSLServer server;

    private final ChordController chordController;

    public Peer(String accessPoint, String hostName, int hostPort, boolean bootPeer) throws Exception {
        this.bootPeer = bootPeer;

        this.storageManager = new StorageManager(accessPoint);

        this.fileHashes = new BiMap<>();
        this.myFiles = new ConcurrentHashMap<>();
        this.backedUpFiles = new ConcurrentHashMap<>();

        this.scheduledExecutorReceiverService = Executors.newScheduledThreadPool(THREAD_NUMBER);

        RMI peerStub = (RMI) UnicastRemoteObject.exportObject(this, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.rebind(accessPoint, peerStub);

        InetSocketAddress tempAddress = this.bootPeer ? new InetSocketAddress(hostName, hostPort) : new InetSocketAddress("localhost", 0);

        SSLContext sslContext = SSLPeer.initializeSSLContext("../../server.keystore", "../../truststore", "sdis25", "sdis25", "sdis25");
        this.server = new SSLServer(this, sslContext, tempAddress);
        this.client = new SSLClient(this, sslContext);

        Thread serverRunnable = new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Server down!");
            }
        });
        serverRunnable.start();

        this.address = server.getBoundAddress();
        this.key = ChordKey.getKeyFromAddress(this.address);

        chordController = new ChordController(this);
        chordController.join(new InetSocketAddress(hostName, hostPort));
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java backupservice.peer.Peer <access_point> <host_name> <host_port> [-b]");
            return;
        }

        try {
            boolean bootPeer = args.length == 4;
            Peer peer = new Peer(args[0], args[1], Integer.parseInt(args[2]), bootPeer);
            System.out.println("Peer Initialized");
            peer.createLeaveProcess();
        } catch (Exception e) {
            System.err.println("Peer exception: " + e);
            e.printStackTrace();
        }
    }

    public void sendServerReply(SSLConnection connection, byte[] message) throws Exception {
        this.server.write(connection, message);
    }

    private void createLeaveProcess() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            chordController.leave();
        }));
    }

    @Override
    public String backup(String filePath, int replicationDegree) throws RemoteException {
        if (!chordController.isAssignedKey()) return "Peer has not joined CHORD ring yet!";
        try {
            int nKeys = this.backupFile(filePath, replicationDegree);
            if (nKeys < replicationDegree)
                return "Could not achieve desired replication. File \"" + Utils.getFileName(filePath) + "\" saved in " + nKeys + " peers";
            return "Backed up " + Utils.getFileName(filePath) + " with replication: " + nKeys;
        } catch (Exception e) {
            e.printStackTrace();
            return "Backup Failed: " + e;
        }
    }

    @Override
    public String restore(String filePath) throws RemoteException {
        if (!chordController.isAssignedKey()) return "Peer has not joined CHORD ring yet!";
        try {
            this.restoreFile(filePath);
            return "Restored file " + filePath;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    public String delete(String filePath) throws RemoteException {
        if (!chordController.isAssignedKey()) return "Peer has not joined CHORD ring yet!";
        this.deleteFile(filePath);
        return "Deleted file " + filePath;
    }

    @Override
    public String reclaim(int diskSpace) throws RemoteException {
        if (!chordController.isAssignedKey()) return "Peer has not joined CHORD ring yet!";
        try {
            if (diskSpace >= 0) {
                this.reclaimSpace(diskSpace);
                return "Peer has now " + diskSpace + "KB of usable disk space.";
            } else throw new Exception("Invalid disk space parameter.");
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    public String state() throws RemoteException {
        if (!chordController.isAssignedKey()) return "Peer has not joined CHORD ring yet!";
        return this.printState();
    }

    public boolean checkConnection(InetSocketAddress address) {
        Random rand = new Random();
        try {
            Thread.sleep(rand.nextInt(1000));
            SSLConnection connection = client.connectToServer(address.getHostString(), address.getPort(), true);
            Thread.sleep(100);
            client.stop(connection);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void sendMessage(InetSocketAddress address, byte[] msg) {
        Random rand = new Random();
        try {
            Thread.sleep(rand.nextInt(1000));
            SSLConnection connection = client.connectToServer(address.getHostString(), address.getPort(), true);
            client.write(connection, msg);
            Thread.sleep(100);
            client.stop(connection);
        } catch (Exception e) {
            System.err.println("[ERROR] Could not send message: " + e.getMessage());
        }
    }

    public Message sendMessageWithReply(InetSocketAddress address, byte[] msg) {
        Message message = null;
        Random rand = new Random();
        try {
            Thread.sleep(rand.nextInt(1000));
            SSLConnection connection = client.connectToServer(address.getHostString(), address.getPort(), true);
            client.write(connection, msg);
            byte[] response = client.read(connection);
            if (response != null) {
                message = Message.parseMessage(this, response, response.length, connection);
                message.handleMessage();
            }
            client.stop(connection);
        } catch (Exception e) {
            System.err.println("[ERROR] Could not send message and wait for a reply: " + e.getMessage());
        }
        return message;
    }

    private int backupFile(String filePath, int replicationDegree) throws Exception {
        String fileHashKey = Utils.bytesToHexString(Utils.hashKey(filePath));

        if (fileHashes.get(filePath) != null && !fileHashes.get(filePath).equals(fileHashKey))
            throw new Exception("Previous version of this file has already been stored!\nPlease delete it before backing up the new version!");

        Set<ChordKey> ignoreList = new HashSet<>();
        ignoreList.add(this.key);
        Map<Integer, ChordKey> fileKeys = chordController.generateRandomKeys(replicationDegree, ignoreList);

        fileHashes.put(filePath, fileHashKey);
        myFiles.put(fileHashKey, new BackedUpFile(filePath, fileHashKey, new HashSet<>(fileKeys.keySet())));

        FileInputStream fileStream = new FileInputStream(System.getProperty("user.dir") + "/" + filePath);
        myFiles.get(fileHashKey).setSize(fileStream.available());

        byte[] putFileBody = (this.getKey().getString() + " " + fileStream.available()).getBytes();

        Random rand = new Random();
        for (Map.Entry<Integer, ChordKey> entry : fileKeys.entrySet()) {
            Integer key = entry.getKey();
            ChordKey target = entry.getValue();

            Thread.sleep(rand.nextInt(1000));
            SSLConnection connection = client.connectToServer(target.getAddress().getHostString(), target.getAddress().getPort(), true);
            client.write(connection, PutFileMessage.buildMessage(this.key, fileHashKey, key, putFileBody, putFileBody.length));
            byte[] response = client.read(connection);
            if (response != null) {
                Message message = Message.parseMessage(this, response, response.length, connection);
                message.handleMessage();
            }
        }

        return myFiles.get(fileHashKey).getActualReplicationDegree();
    }

    public void waitForServerResponse(SSLConnection connection, boolean stop) {
        scheduledExecutorReceiverService.submit(() -> {
            try {
                byte[] response = client.read(connection);
                if (response != null) {
                    Message message = Message.parseMessage(this, response, response.length, connection);
                    message.handleMessage();
                }
                if (stop) client.stop(connection);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error reading message: " + e.getMessage());
            }
        });
    }

    public void waitForClientResponse(SSLConnection connection) {
        scheduledExecutorReceiverService.submit(() -> {
            try {
                Message message;
                do {
                    message = server.read(connection);
                } while (message == null);
                message.handleMessage();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error reading message: " + e.getMessage());
            }
        });
    }

    public void sendFile(String fileId, SSLConnection connection, boolean backup) throws Exception {
        if (!backup) {
            server.write(connection, storageManager.loadFile(fileId));
            waitForClientResponse(connection);
            return;
        }

        String filePath = fileHashes.getKey(fileId);

        if (filePath == null) {
            client.write(connection, storageManager.loadFile(fileId));
            waitForServerResponse(connection, false);
            return;
        }

        FileInputStream fileStream = new FileInputStream(System.getProperty("user.dir") + "/" + filePath);
        byte[] chunkBuffer = new byte[fileStream.available()];
        int rc = fileStream.read(chunkBuffer);

        System.out.println("Read file with " + rc + " bytes!");

        client.write(connection, chunkBuffer);
        waitForServerResponse(connection, false);
    }

    public void putFileHandler(String fileId, int fileKey, int fileSize, ChordKey owner, SSLConnection connection) throws Exception {
        BackedUpFile chunkFile = backedUpFiles.get(fileId);

        if (chunkFile == null) {
            chunkFile = new BackedUpFile(fileId, fileKey, owner);
            this.backedUpFiles.put(fileId, chunkFile);
            this.saveFile(fileId, fileKey, fileSize, connection);
        }
        else {
            this.server.write(connection, ReceivedMessage.buildMessage(this.key, fileId));
        }

        sendMessage(owner.getAddress(), StoredMessage.buildMessage(this.key, fileId, fileKey));
    }

    public void saveFile(String fileId, int fileKey, int fileSize, SSLConnection connection) throws Exception {
        try {
            this.storageManager.reserveFileSpace(fileSize);
        } catch (Exception e) {
            this.server.write(connection, NoSpaceMessage.buildMessage(this.key, fileId, fileKey));
            System.out.println("[NO SPACE AVAILABLE] Sent No Space message!");
            this.backedUpFiles.remove(fileId);
            return;
        }

        this.backedUpFiles.get(fileId).setSize(fileSize);

        System.out.println("[SAVE FILE PROCESS] Reserved space!");
        this.server.write(connection, ReadingMessage.buildMessage(this.key, fileId, true));
        System.out.println("[SAVE FILE PROCESS] Sent Reading message!");
        this.storageManager.saveBackedUpFile(fileId, fileSize, server, connection);
        System.out.println("[SAVE FILE PROCESS] Saved file!");
        this.server.write(connection, ReceivedMessage.buildMessage(this.key, fileId));
        System.out.println("[SAVE FILE PROCESS] Sent Received message!");
    }

    public void restoreFile(String fileName) throws Exception {
        String fileHash = fileHashes.get(fileName);

        if (fileHash == null) throw new Exception("File has not been backed up by this peer!");

        BackedUpFile file = this.myFiles.get(fileHash);

        Set<Integer> fileKeys = file.getKeys();

        Random rand = new Random();
        for (Integer fileKey : fileKeys) {
            ChordKey target = chordController.lookup(new ChordKey(fileKey));
            if (target == null) continue;

            Thread.sleep(rand.nextInt(1000));
            SSLConnection connection = client.connectToServer(target.getAddress().getHostString(), target.getAddress().getPort(), true);
            client.write(connection, GetFileMessage.buildMessage(this.key, fileHash));

            try {
                byte[] response = client.read(connection);
                Message message = Message.parseMessage(this, response, response.length, connection);
                if (message instanceof FileMessage) {
                    message.handleMessage();
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error reading message: " + e.getMessage());
            }
        }
    }

    public void handleGetFile(String fileId, SSLConnection connection) throws Exception {
        BackedUpFile file = backedUpFiles.get(fileId);

        if (file == null) {
            client.stop(connection);
            return;
        }

        server.write(connection, FileMessage.buildMessage(this.key, fileId, String.valueOf(file.getSize()).getBytes()));
        waitForClientResponse(connection);
    }

    public void handleFile(String fileId, int fileSize, SSLConnection connection) throws Exception {
        String filePath = "r_" + Utils.getFileName(this.fileHashes.getKey(fileId));

        this.client.write(connection, ReadingMessage.buildMessage(this.key, fileId, false));
        System.out.println("[RESTORE FILE PROCESS] Sent Reading message!");
        this.storageManager.saveRestoredFile(filePath, fileSize, client, connection);
        System.out.println("[RESTORE FILE PROCESS] Saved file!");
        this.client.write(connection, ReceivedMessage.buildMessage(this.key, fileId));
        System.out.println("[RESTORE FILE PROCESS] Sent Received message!");
    }


    public boolean isFileOwner(String fileID) {
        return this.myFiles.containsKey(fileID);
    }

    public void increaseReplicationDegree(String fileID, int fileKey) {
        this.myFiles.get(fileID).increaseReplicationStatus(fileKey);
    }

    public void decreaseReplicationDegree(String fileId, int fileKey) {
        this.myFiles.get(fileId).decreaseReplicationStatus(fileKey);
    }

    private void deleteFile(String filePath) {
        String fileId = this.fileHashes.get(filePath);
        BackedUpFile file = myFiles.get(fileId);

        Set<Integer> fileKeys = file.getKeys();

        for (Integer fileKey : fileKeys) {
            ChordKey target = chordController.lookup(new ChordKey(fileKey));
            if (target == null) continue;
            sendMessage(target.getAddress(), DeleteMessage.buildMessage(this.key, fileId));
        }

        this.myFiles.remove(fileId);
        this.fileHashes.remove(filePath);
        System.out.println("Removed file: " + fileId + " from registry");
    }

    public void deleteChunks(String fileID) {
        if (this.backedUpFiles.containsKey(fileID)) {
            this.storageManager.deleteFile(fileID);
            this.backedUpFiles.remove(fileID);
        }
    }

    public boolean hasFile(String fileId) {
        BackedUpFile file = this.backedUpFiles.get(fileId);
        return file != null;
    }

    public void reclaimSpace(int diskSpace) throws Exception {
        this.storageManager.setDiskSize(diskSpace);
        while (this.storageManager.getAvailableSpace() < 0) {
            this.reclaimFile();
        }
    }

    private void reclaimFile() throws Exception {
        for (String fileHash : this.backedUpFiles.keySet()) {
            this.sendRemoved(fileHash);
            System.out.println("[RECLAIM] Sent Removed!");
            int newPeers = this.replicateFile(fileHash);
            System.out.println("[RECLAIM] Replicated File! " + newPeers + " more peer have this file!");
            this.backedUpFiles.remove(fileHash);
            storageManager.deleteFile(fileHash);
            System.out.println("[RECLAIM] Removed File!");
            return;
        }
    }

    public void sendRemoved(String fileId) {
        BackedUpFile file = backedUpFiles.get(fileId);
        sendMessage(file.getFileOwner().getAddress(), RemovedMessage.buildMessage(this.key, fileId, file.getFileKey()));
    }

    public int replicateFile(String fileId) throws Exception {
        BackedUpFile file = this.backedUpFiles.get(fileId);

        Set<ChordKey> ignoreList = new HashSet<>();
        ignoreList.add(this.key);
        ignoreList.add(file.getFileOwner());
        Map<Integer, ChordKey> fileKeys = chordController.generateRandomKeys(1, ignoreList);

        byte[] putFileBody = (file.getFileOwner().getString() + " " + file.getSize()).getBytes();
        int newPeers = 0;

        Random rand = new Random();
        for (Map.Entry<Integer, ChordKey> entry : fileKeys.entrySet()) {
            Integer key = entry.getKey();
            ChordKey target = entry.getValue();

            Thread.sleep(rand.nextInt(1000));
            SSLConnection connection = client.connectToServer(target.getAddress().getHostString(), target.getAddress().getPort(), true);
            client.write(connection, PutFileMessage.buildMessage(this.key, fileId, key, putFileBody, putFileBody.length));
            byte[] response = client.read(connection);
            if (response != null) {
                Message message = Message.parseMessage(this, response, response.length, connection);
                message.handleMessage();
                if (message instanceof ReadingMessage || message instanceof ReceivedMessage) newPeers++;
            }
        }

        return newPeers;
    }

    public void sendFileToPeer(String fileId, ChordKey peer) throws Exception {
        BackedUpFile file = this.backedUpFiles.get(fileId);

        byte[] putFileBody = (file.getFileOwner().getString() + " " + file.getSize()).getBytes();

        Thread.sleep(new Random().nextInt(1000));
        SSLConnection connection = client.connectToServer(peer.getAddress().getHostString(), peer.getAddress().getPort(), true);
        client.write(connection, PutFileMessage.buildMessage(this.key, fileId, file.getFileKey(), putFileBody, putFileBody.length));
        byte[] response = client.read(connection);
        if (response != null) {
            Message message = Message.parseMessage(this, response, response.length, connection);
            message.handleMessage();
            if (message instanceof ReadingMessage || message instanceof ReceivedMessage) return;
        }
        throw new Exception("Did not receive Reading message from peer!");
    }

    public void stabilizeFileKeys(ChordKey predecessor) {
        for (Map.Entry<String, BackedUpFile> entry : backedUpFiles.entrySet()) {
            String fileHash = entry.getKey();
            BackedUpFile file = entry.getValue();

            boolean myKey = new ChordKey(file.getFileKey()).between(predecessor, this.getKey(), false);
            if (!myKey) {
                try {
                    sendFileToPeer(fileHash, predecessor);
                    this.backedUpFiles.remove(fileHash);
                    storageManager.deleteFile(fileHash);
                    System.out.println("[FILE MOVED] Removed File from storage because it is now in predecessor peer!");
                } catch (Exception e) {
                    System.err.println("[ERROR] Could not send to my predecessor its keys! Reason: " + e.getMessage());
                }
            }
        }
    }

    private String printState() {
        StringBuilder state = new StringBuilder();
        state.append("Peer Info: ").append(getKey()).append("\nParent: ").append(chordController.getPre());
        state.append("\n-----\nPeer Finger Table:\n").append(chordController.writeFingerTable());
        if (this.myFiles.size() == 0) state.append("\n-----\nNo Owned Files\n");
        else {
            state.append("\n-----\nOwned Files:\n");
            this.myFiles.forEach((key, value) -> state.append(value.toString()));
        }

        if (this.backedUpFiles.size() == 0) state.append("-----\nNo Saved Files\n");
        else {
            state.append("-----\nBacked Up Files:\n");
            this.backedUpFiles.forEach((key, value) -> state.append(value.toString()));
        }

        state.append("-----\n").append(this.storageManager.toString());

        return state.toString();
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public ChordKey getKey() {
        return key;
    }

    public boolean isBootPeer() {
        return bootPeer;
    }

    public void setID(int id) {
        this.key.setId(id);
        this.chordController.setAssignedKey(true);
    }

    public ChordController getChordController() {
        return chordController;
    }

    public void closeClientConnection(SSLConnection connection) throws Exception {
        this.client.stop(connection);
    }
}
