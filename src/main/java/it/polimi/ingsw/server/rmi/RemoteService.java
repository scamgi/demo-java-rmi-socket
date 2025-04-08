package it.polimi.ingsw.server.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteService extends Remote {
    void registerClient(ProxyClient client) throws RemoteException;
    void removeClient(ProxyClient client) throws RemoteException;
    String sendMessage(String clientName) throws RemoteException;
    void broadcastMessage(String message) throws RemoteException;
    void broadcastMessage(String message, ProxyClient sender) throws RemoteException;
}
