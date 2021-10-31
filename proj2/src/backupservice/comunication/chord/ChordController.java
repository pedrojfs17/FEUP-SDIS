package backupservice.comunication.chord;

import backupservice.comunication.chord.task.CheckPredecessorTask;
import backupservice.comunication.chord.task.FixFingerTableTask;
import backupservice.comunication.chord.task.StabilizeTask;
import backupservice.comunication.message.chord.*;
import backupservice.peer.Peer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChordController implements Chord {

    private ChordKey predecessor;
    private ChordKey[] fingerTable;
    private final Peer peer;
    private int next = 1;
    private boolean assignedKey;
    private ScheduledExecutorService scheduledNodeService;

    public ChordController(Peer peer) {
        this.peer = peer;
        this.predecessor = null;
        this.assignedKey = false;
    }

    @Override
    public ChordKey getSucc() {
        return this.fingerTable[0];
    }

    @Override
    public ChordKey getPre() {
        return this.predecessor;
    }

    @Override
    public ChordKey lookup(ChordKey key) {
        return findSucc(key);
    }

    @Override
    public void replacePeer(ChordKey key, ChordKey newKey) {
        if(newKey.equals(predecessor)) this.setPre(null);
        int i = 0;
        for (ChordKey finger : fingerTable) {
            if (finger.equals(key)) {
                fingerTable[i] =  key.equals(newKey)? getKey() : newKey;
            }
            i++;
        }
    }

    @Override
    public ChordKey getFinger(int i) {
        return fingerTable[i - 1];
    }

    @Override
    public void setFinger(int i, ChordKey key) {
        fingerTable[i - 1] = key;
    }

    @Override
    public void setPre(ChordKey key) {
        this.predecessor = key;
    }

    @Override
    public void create() {
        setPeriodicTasks();
        initFingerTable(peer.getKey());
        predecessor = peer.getKey();
    }

    public void initFingerTable(ChordKey bootPeer) {
        this.fingerTable = new ChordKey[ChordKey.M_BIT];
        for (int i = 1; i <= ChordKey.M_BIT; i++) {
            setFinger(i, bootPeer);
        }
    }

    public void setPeriodicTasks() {
        Random rand = new Random();
        scheduledNodeService = Executors.newScheduledThreadPool(3);
        scheduledNodeService.scheduleWithFixedDelay(new StabilizeTask(this), rand.nextInt(10) + 10, rand.nextInt(10) + 10, TimeUnit.SECONDS);
        scheduledNodeService.scheduleWithFixedDelay(new CheckPredecessorTask(this), rand.nextInt(10) + 15, rand.nextInt(10) + 10, TimeUnit.SECONDS);
        scheduledNodeService.scheduleWithFixedDelay(new FixFingerTableTask(this), rand.nextInt(10) + 5, rand.nextInt(10) + 5, TimeUnit.SECONDS);
    }

    @Override
    public void leave() {
        scheduledNodeService.shutdown();

        try {
            peer.reclaimSpace(0);
        } catch (Exception e) {
            System.err.println("Couldn't resend all files: " + e.getMessage());
        }

        try {
            Thread.sleep(new Random().nextInt(1000));
        } catch (InterruptedException ignored) {}

        this.sendReplacePred(getSucc(), getPre());
        this.sendSucc(getPre(), getSucc());
        System.out.println("[LEAVE] See ya!");
    }

    @Override
    public void join(InetSocketAddress bootAddress) {
        //if this is the boot peer, do not join network
        if (this.peer.isBootPeer()) {
            this.create();
            this.setAssignedKey(true);
            return;
        }

        peer.sendMessageWithReply(bootAddress, JoinMessage.buildMessage(peer.getAddress()));
    }

    public void setAssignedKey(boolean isAssigned) {
        this.assignedKey = isAssigned;
    }

    public boolean isAssignedKey() {
        return this.assignedKey;
    }

    @Override
    public void notify(ChordKey node, ChordKey potentialPredecessor) {
        if (node.equals(potentialPredecessor))
            return;

        peer.sendMessage(node.getAddress(), NotifyMessage.buildMessage(potentialPredecessor));
        System.out.println("[NOTIFY] Notified!");
    }

    private void sendSucc(ChordKey pre, ChordKey succ) {
        if (pre.equals(getKey()))
            return;

        peer.sendMessage(pre.getAddress(), ReplaceSuccMessage.buildMessage(getKey(), succ));
        System.out.println("[LEAVE] Sent replacement!");
    }

    private void sendReplacePred(ChordKey peer, ChordKey newPredecessor) {
        if (peer.equals(getKey()))
            return;

        this.peer.sendMessage(peer.getAddress(), ReplacePredMessage.buildMessage(getKey(), newPredecessor));
        System.out.println("[LEAVE] Sent predecessor replacement!");
    }

    public void replacePred(ChordKey old, ChordKey newPred) {
        if (this.predecessor.equals(old))
            this.predecessor = newPred;
    }

    @Override
    public void notified(ChordKey predecessor) {
        this.predecessor = predecessor;
        this.peer.stabilizeFileKeys(predecessor);
    }

    @Override
    public ChordKey findSucc(ChordKey key) {
        ChordKey succ = getSucc();
        if (key.between(peer.getKey(), succ, false))
            return succ;

        ChordKey pre = getClosestPre(key);
        if (pre.equals(peer.getKey()))
            return succ;

        SuccessorMessage successorMessage = (SuccessorMessage) peer.sendMessageWithReply(pre.getAddress(), LookupSuccMessage.buildMessage(peer.getKey(), key));
        return successorMessage != null ? successorMessage.getSuccessor() : null;
    }

    @Override
    public ChordKey getClosestPre(ChordKey key) {
        for (int i = fingerTable.length - 1; i > 0; i--) {
            ChordKey finger = getFinger(i);
            if (finger.between(peer.getKey(), key, true)) {
                return finger;
            }
        }
        return peer.getKey();
    }

    public ChordKey getKey() {
        return peer.getKey();
    }

    public boolean checkPre() {
        if (predecessor == null) return false;
        return peer.checkConnection(predecessor.getAddress());
    }

    public int getNextFinger() {
        return next;
    }

    public void incrementFinger() {
        next += 1;
        if (next > ChordKey.M_BIT) next = 1;
    }

    public ChordKey requestPreFromSucc() {
        ChordKey succ = getSucc();
        if (succ == getKey()) return getPre();
        PredecessorMessage predecessor = (PredecessorMessage) peer.sendMessageWithReply(succ.getAddress(), LookupPredMessage.buildMessage(peer.getKey()));
        return predecessor != null ? predecessor.getPredecessor() : null;
    }

    public void printFingerTable() {
        System.out.println("My Key - " + this.getKey());
        System.out.println("Predecessor - " + this.predecessor);
        for (int i = 0; i < fingerTable.length; i++) {
            System.out.println(i + " - " + fingerTable[i]);
        }
    }

    public String writeFingerTable() {
        StringBuilder ret = new StringBuilder("My Key - " + this.getKey() + "\nPredecessor - " + this.predecessor);
        for (int i = 0; i < fingerTable.length; i++) {
            ret.append("\n").append(i).append(" - ").append(fingerTable[i]);
        }
        return ret.toString();
    }

    public ChordKey createRingKey(InetSocketAddress newNodeAddress) {
        ChordKey key = ChordKey.getKeyFromAddress(newNodeAddress);
        ChordKey successor = findSucc(key);
        while (successor.getKey() == key.getKey()) {
            key.incrementKey();
            successor = findSucc(key);
        }
        return key;
    }

    public Map<Integer, ChordKey> generateRandomKeys(int neededKeys, Set<ChordKey> ignoreList) {
        Map<Integer, ChordKey> fileKeys = new HashMap<>();
        int[] keys = new Random().ints(0, ChordKey.MAX).distinct().limit(neededKeys * 3L).toArray();

        for (int key : keys) {
            ChordKey peer = lookup(new ChordKey(key));
            if (peer != null && !ignoreList.contains(peer)) {
                ignoreList.add(peer);
                fileKeys.put(key, peer);
            }
        }

        return fileKeys;
    }
}
