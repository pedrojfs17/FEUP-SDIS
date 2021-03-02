import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {
    private static Map<String, InetAddress> table = new HashMap<>();

    private static DatagramSocket socket;
    private static DatagramPacket packet;

    public static void main(String[] args) throws IOException, UnknownHostException {
        if (args.length != 3) {
            System.out.println("Usage: java Server <srvc_port> <mcast_addr> <mcast_port>");
            return;
        }

        String localhost = InetAddress.getLocalHost().getHostAddress();

        new Timer().scheduleAtFixedRate(new AdvertiseMessage(args[1], args[2], localhost, args[0]), 0, 1000);

        int servicePort = Integer.parseInt(args[0]);

        Server.socket = new DatagramSocket(servicePort);

        while(true) {
            String str = receivePacket();
            String response = handleCommand(str);
            System.out.println(str + " *:: " + response);  

            InetAddress returnAddress = Server.packet.getAddress();
            int returnPort = Server.packet.getPort();

            byte[] buf = response.getBytes();
            Server.packet = new DatagramPacket(buf, buf.length, returnAddress, returnPort);
            Server.socket.send(Server.packet);
        }
    }

    private static String receivePacket() throws IOException {
        byte[] buf = new byte[1024];  
        Server.packet = new DatagramPacket(buf, 1024);  
        Server.socket.receive(Server.packet);  
        String data = new String(Server.packet.getData(), 0, Server.packet.getLength());

        return data;
    }

	private static String handleCommand(String str) throws UnknownHostException {
        String[] command = str.split(" ");

        if (command[0].equalsIgnoreCase("register")) {
            return "" + register(command[1], command[2]);
        } else if (command[0].equalsIgnoreCase("lookup")) {
            InetAddress ip = lookup(command[1]);
            if (ip != null)
                return command[1] + " " + ip;
        }
            
        return "-1";
	}

	private static int register(String name, String ip) throws UnknownHostException {
        InetAddress value = table.put(name, InetAddress.getByName(ip));

        if (value == null)
            return table.size();
        
        return -1;
    }

    private static InetAddress lookup(String name) {
        return table.get(name);
    }
}
