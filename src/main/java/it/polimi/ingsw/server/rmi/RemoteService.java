package it.polimi.ingsw.server.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteService extends Remote {
    String getMessage(String clientName) throws RemoteException;
    void registerClient(ClientCallback client) throws RemoteException;
    void broadcastMessage(String message) throws RemoteException;
    void removeClient(ClientCallback client) throws RemoteException;
}
