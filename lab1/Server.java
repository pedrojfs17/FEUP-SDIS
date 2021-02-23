import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {
    private static Map<String, String> table = new HashMap<>();

    private static DatagramSocket socket;
    private static DatagramPacket packet;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java Server.java <portname>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        Server.socket = new DatagramSocket(port);

        while(true) {
            String str = receivePacket();
            String response = handleCommand(str);

            Server.packet.setData(response.getBytes());
            Server.socket.send(Server.packet);
        }
    }

    private static String receivePacket() throws IOException {
        byte[] buf = new byte[1024];  
        Server.packet = new DatagramPacket(buf, 1024);  
        Server.socket.receive(Server.packet);  
        String data = new String(Server.packet.getData(), 0, Server.packet.getLength());
        
        System.out.println("Server: " + data);  

        return data;
    }

	private static String handleCommand(String str) {
        String[] command = str.split(" ");
        int result;

        if (command[0].equals("REGISTER")) {
            result = register(command[1], command[2]);
            return "" + result + "\n" + command[1] + " " + command[2];
        } else if (command[0].equals("LOOKUP")) {
            String ip = lookup(command[1]);
            if (ip != null)
                return "" + table.size() + "\n" + command[1] + " " + ip;
        }
            
        return "-1";
	}

	private static int register(String name, String ip) {
        String value = table.put(name, ip);

        if (value == null)
            return table.size();
        
        return -1;
    }

    private static String lookup(String name) {
        return table.get(name);
    }
}
