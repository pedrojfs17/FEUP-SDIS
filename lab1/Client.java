import java.io.IOException;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException{
        if (args.length < 4 || args.length > 5) {
            System.out.println("Usage: java Client.java <host> <port> <oper> <opnd1> [<opnd2>]");
            return;
        }

        String command = buildCommand(args);

        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(2000);

        byte[] sbuf = command.getBytes();

        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);

        DatagramPacket packet = new DatagramPacket(sbuf, sbuf.length, address, port);
        socket.send(packet);

        //get response
        byte[] rbuf = new byte[sbuf.length];
        packet = new DatagramPacket(rbuf, rbuf.length);
        socket.receive(packet);

        // display response
        String received = new String(packet.getData());
        System.out.println("Client: " + command + " : " + received);
        socket.close();
    }

    private static String buildCommand(String[] args) {
        String[] ops = new String[args.length - 2];
        System.arraycopy(args, 2, ops, 0, args.length - 2);

        if (ops.length == 2)
            return String.join(" ", ops[0], ops[1]);
        else if (ops.length == 3)
            return String.join(" ", ops[0], ops[1], ops[2]);

        return "";
    }
}