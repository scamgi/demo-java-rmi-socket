package it.polimi.ingsw.server.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RMIServer extends UnicastRemoteObject implements RemoteService {
    private static final Logger logger = Logger.getLogger(RMIServer.class.getName());
    private final List<ProxyClient> clients = Collections.synchronizedList(new ArrayList<>());

    public RMIServer() throws RemoteException {
        super();
    }

    @Override
    public String sendMessage(String message) throws RemoteException {
        // TODO: to implement in the future with a json
        // this is the string that will be sent to the client
        return "test " + message;
    }

    @Override
    public void registerClient(ProxyClient client) throws RemoteException {
        if (client != null && !clients.contains(client)) {
            clients.add(client);
            logger.info("Registered new client.");
            client.receiveMessage("You have been registered.");
        }
    }

    @Override
    public void broadcastMessage(String message) throws RemoteException {
        for (ProxyClient client : clients) {
            try {
                client.receiveMessage(message);
            } catch (RemoteException e) {
                logger.warning("Error while sending message to the client, it's probably disconnected: " + e.getMessage());
            }
        }
    }

    @Override
    public void broadcastMessage(String message, ProxyClient sender) throws RemoteException {
        for (ProxyClient proxyClient : clients) {
            try {
                if (!proxyClient.equals(sender))
                    proxyClient.receiveMessage(message);
            }
            catch (RemoteException e) {
                logger.warning("Error while sending message to the client, it's probably disconnected: " + e.getMessage());
            }
        }
    }

    @Override
    public void removeClient(ProxyClient client) throws RemoteException {
        if (client != null && clients.remove(client)) {
            logger.info("Client disconnected.");
            broadcastMessage("A client was disconnected.");
        }
    }
}