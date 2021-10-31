package comunication.transmitter;

import comunication.MulticastListener;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MultipleMessageSender extends MessageSender {
    protected int timeOut;
    protected int reps;

    public MultipleMessageSender(MulticastListener channel, ScheduledExecutorService executorService, int timeOut, byte[] msg, int numTries) {
        super(channel, executorService, msg);
        this.reps = numTries;
        this.timeOut = timeOut;
    }

    @Override
    public void run() {
        channel.sendMessage(msg);
        if (reps > 1)
            executorService.schedule(new MultipleMessageSender(channel, executorService, timeOut * 2, msg, reps - 1), timeOut, TimeUnit.MILLISECONDS);
    }
}
