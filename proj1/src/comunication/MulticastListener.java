package comunication;

import peer.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastListener extends Thread {
    private final MulticastSocket socket;
    private final InetAddress group;
    private final int port;

    private final Peer peer;

    public MulticastListener(Peer peer, String ip, int port) throws IOException {
        this.peer = peer;
        this.port = port;
        this.socket = new MulticastSocket(port);
        this.socket.setTimeToLive(1);
        this.group = InetAddress.getByName(ip);
        this.socket.joinGroup(this.group);
    }

    @Override
    public void run() {
        while (true) {
            byte[] buf = new byte[Peer.CHUNK_SIZE + 1000];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            try {
                this.socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Couldn't read packet from socket");
            }

            this.peer.receivedMessage(packet.getData(), packet.getLength());
        }
    }

    public void sendMessage(byte[] msg) {
        DatagramPacket packet = new DatagramPacket(msg, msg.length, this.group, this.port);

        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Couldn't send message to socket");
        }
    }
}
