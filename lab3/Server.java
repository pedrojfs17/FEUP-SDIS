import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.util.*;

public class Server implements RMI {

    private Map<String, String> table = new HashMap<>();
    
    public Server() {}

    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Usage: java Server <remote_object_name>");
            return;
        }

        try {
            Server obj = new Server();
            RMI stub = (RMI) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(args[0], stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public String request(String request) {
        String[] command = request.trim().split(" ");
        String response = "-1";

        if (command[0].equalsIgnoreCase("REGISTER")) {
            response = "" + register(command[1], command[2]);
        } else if (command[0].equalsIgnoreCase("LOOKUP")) {
            String ip = lookup(command[1]);
            if (ip != null)
                response = command[1] + " " + ip;
        }

        System.out.println(request + " :: " + response);
            
        return response;
    }

    public String lookup(String name) {
        return table.get(name);
    }

    public int register(String name, String ip) {
        String value = table.put(name, ip);

        if (value == null)
            return table.size();
        
        return -1;
    }
}