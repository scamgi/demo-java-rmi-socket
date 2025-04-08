package it.polimi.ingsw.server.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerMain {
    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());
    private static final int RMI_PORT = 1099;
    private static final String SERVICE = "GalaxyTruckerService";

    public static void main(String[] args) {
        try {
            RemoteService serverService = new RMIServer();
            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            registry.rebind(SERVICE, serverService);
            LOGGER.info("Server RMI waiting for clients.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during the creation of the RMI service: ", e);
            System.exit(1);
        }
    }
}