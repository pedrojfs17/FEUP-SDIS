import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {
    private static Map<String, String> table = new HashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java Server <portname>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        DatagramSocket socket = new DatagramSocket(port);

        String str = "";

        while(!str.equals("STOP SERVER")) {
            byte[] buf = new byte[1024];  
            DatagramPacket packet = new DatagramPacket(buf, 1024);  
            socket.receive(packet);  
            str = new String(packet.getData(), 0, packet.getLength());  
    
            System.out.println("Server: " + str);
    
            String response = handleCommand(str);

            packet.setData(response.getBytes());
            socket.send(packet);
        }

        socket.close();
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
