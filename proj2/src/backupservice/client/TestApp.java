package backupservice.client;

import backupservice.comunication.RMI;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    private static RMI homePeer;

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 4) {
            System.out.println("Usage: java backupservice.client.TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }

        TestApp app = new TestApp();

        try {
            Registry registry = LocateRegistry.getRegistry();
            homePeer = (RMI) registry.lookup(args[0]);

            String operation = args[1];

            if (operation.equalsIgnoreCase("BACKUP"))
                app.backup(args[2], Integer.parseInt(args[3]));
            else if (operation.equalsIgnoreCase("RESTORE"))
                app.restore(args[2]);
            else if (operation.equalsIgnoreCase("DELETE"))
                app.delete(args[2]);
            else if (operation.equalsIgnoreCase("RECLAIM"))
                app.reclaim(Integer.parseInt(args[2]));
            else if (operation.equalsIgnoreCase("STATE"))
                app.state();
            else
                System.out.println("Invalid Operation!");

        } catch (Exception e) {
            System.err.println("Client exception: " + e);
            e.printStackTrace();
        }

    }

    public void backup(String filePath, int replicationDegree) throws RemoteException {
        String response = homePeer.backup(filePath, replicationDegree);
        System.out.println(response);
    }

    public void restore(String filePath) throws RemoteException {
        String response = homePeer.restore(filePath);
        System.out.println(response);
    }

    public void delete(String filePath) throws RemoteException {
        String response = homePeer.delete(filePath);
        System.out.println(response);
    }

    public void reclaim(int diskSpace) throws RemoteException {
        String response = homePeer.reclaim(diskSpace);
        System.out.println(response);
    }

    public void state() throws RemoteException {
        String response = homePeer.state();
        System.out.println(response);
    }
}
