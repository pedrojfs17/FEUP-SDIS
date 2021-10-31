package comunication.transmitter;

import comunication.MulticastListener;
import peer.Chunk;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ChunkMessageSender extends MultipleMessageSender {
    protected Chunk chunk;
    protected Semaphore lock;
    protected Set<Integer> restoredList;
    protected Integer chunkID;

    public ChunkMessageSender(MulticastListener channel, ScheduledExecutorService executorService, int timeOut, byte[] msg, Chunk chunk, Semaphore lock, Set<Integer> restoredList, Integer chunkID) {
        super(channel, executorService, timeOut, msg, 5);
        this.channel = channel;
        this.chunk = chunk;
        this.msg = msg;
        this.lock = lock;
        this.restoredList = restoredList;
        this.chunkID = chunkID;
    }

    public ChunkMessageSender(MulticastListener channel, ScheduledExecutorService executorService, int timeOut, byte[] msg, Chunk chunk) {
        this(channel, executorService, timeOut, msg, chunk, null,null,null);
    }

    public ChunkMessageSender(MulticastListener channel, ScheduledExecutorService executorService, int timeOut, byte[] msg, Set<Integer> restoredList, Integer chunkID) {
        this(channel, executorService, timeOut, msg, null, null,restoredList,chunkID);
    }

    @Override
    public void run() {
        if (chunk != null) {
            if (chunk.hasDesiredReplicationDegree()) {
                if (lock != null) lock.release();
                return;
            }
        }
        else if (this.restoredList.contains(chunkID)) return;

        if (timeOut >= 96000) {
            if (lock != null) lock.release();
            //System.out.println("Couldn't store chunk: " + chunk.getId());
            return;
        }

        channel.sendMessage(msg);
        executorService.schedule(new ChunkMessageSender(channel, executorService, timeOut * 2, msg, chunk, lock, restoredList, chunkID), timeOut, TimeUnit.MILLISECONDS);
    }
}
