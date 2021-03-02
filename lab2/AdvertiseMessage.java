import java.io.IOException;
import java.time.*;
import java.net.*;
import java.util.*;

public class AdvertiseMessage extends TimerTask {
    private String mcastAddr, mcastPort, srvcAddr, srvcPort;
    private DatagramSocket socket;
    private DatagramPacket packet;

    public AdvertiseMessage(String mcastAddr, String mcastPort, String srvcAddr, String srvcPort) throws IOException, SocketException, UnknownHostException {
        this.mcastAddr = mcastAddr;
        this.mcastPort = mcastPort;
        this.srvcAddr = srvcAddr;
        this.srvcPort = srvcPort;

        this.socket = new MulticastSocket(Integer.parseInt(mcastPort));

        InetAddress group = InetAddress.getByName(mcastAddr);

        String message = srvcAddr + " " + srvcPort;

        byte[] buf = message.getBytes();
        this.packet = new DatagramPacket(buf, buf.length, group, Integer.parseInt(mcastPort));
    }

    @Override
    public void run() {
        try {
            this.socket.send(this.packet);
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.out.println("multicast: " + this.mcastAddr + " " + this.mcastPort + " : " + this.srvcAddr + " " + this.srvcPort);
    }
}
