import java.io.IOException;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException{
        if (args.length < 4 || args.length > 5) {
            System.out.println("Usage: java Client <mcast_addr> <mcast_port> <oper> <opnd> *");
            return;
        }

        String command = buildCommand(args);

        if (command.equals("")) {
            System.out.println("Invalid Command");
            return;
        }

        int port = Integer.parseInt(args[1]);

        InetAddress multicastAddress = InetAddress.getByName(args[0]);
        InetSocketAddress group = new InetSocketAddress(multicastAddress, port);

        MulticastSocket socket = new MulticastSocket(port);
        NetworkInterface netIf = NetworkInterface.getByName("bge0");
        socket.joinGroup(group, netIf);

        byte[] responseBytes =  new byte[256]; 
        DatagramPacket inPacket = new DatagramPacket(responseBytes, responseBytes.length);
        socket.receive(inPacket);

        socket.leaveGroup(group, netIf);
        socket.close();

        String[] response = new String(inPacket.getData()).trim().split(" ");
        
        System.out.println("multicast: " + multicastAddress + " " + port + " : " + response[0] + " " + response[1]);

        int servicePort = Integer.parseInt(response[1]);

        byte[] msgBytes = command.getBytes();

        InetAddress serviceAddr = InetAddress.getByName(response[0]);
        
        DatagramSocket serviceSocket = new DatagramSocket();
        DatagramPacket outPacket = new DatagramPacket(msgBytes, msgBytes.length, serviceAddr, servicePort);

        serviceSocket.send(outPacket);

        //receive the response (blocking)
        responseBytes =  new byte[256]; // empty the receive buffer
        inPacket = new DatagramPacket(responseBytes, responseBytes.length);
        serviceSocket.receive(inPacket);

        String received = new String(inPacket.getData());
        System.out.println(command + " *:: " + received);

        serviceSocket.close();
    }

    private static String buildCommand(String[] args) {
        String[] ops = new String[args.length - 2];
        System.arraycopy(args, 2, ops, 0, args.length - 2);

        if (ops[0].equalsIgnoreCase("lookup") && ops.length == 2)
            return String.join(" ", ops[0], ops[1]);
        else if (ops[0].equalsIgnoreCase("register") && ops.length == 3)
            return String.join(" ", ops[0], ops[1], ops[2]);

        return "";
    }
}