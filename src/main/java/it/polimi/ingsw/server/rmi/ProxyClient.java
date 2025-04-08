package it.polimi.ingsw.server.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ProxyClient extends Remote {
    void showMessage(String message) throws RemoteException;
}
