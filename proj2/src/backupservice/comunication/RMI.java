package backupservice.comunication;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMI extends Remote {
    String backup(String filePath, int replicationDegree) throws RemoteException;

    String restore(String filePath) throws RemoteException;

    String delete(String filePath) throws RemoteException;

    String reclaim(int diskSpace) throws RemoteException;

    String state() throws RemoteException;
}
