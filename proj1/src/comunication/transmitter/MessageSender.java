package comunication.transmitter;

import comunication.MulticastListener;

import java.util.concurrent.ScheduledExecutorService;

public class MessageSender implements Runnable {
    protected MulticastListener channel;
    protected byte[] msg;
    protected ScheduledExecutorService executorService;

    public MessageSender(MulticastListener channel, ScheduledExecutorService executorService, byte[] msg) {
        this.channel = channel;
        this.msg = msg;
        this.executorService = executorService;
    }

    @Override
    public void run() {
        channel.sendMessage(msg);
    }
}
