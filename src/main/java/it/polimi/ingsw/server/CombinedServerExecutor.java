package it.polimi.ingsw.server;

import it.polimi.ingsw.server.rmi.RemoteService;
import it.polimi.ingsw.server.rmi.RMIServer;
import it.polimi.ingsw.server.socket.ServerApplication;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CombinedServerExecutor {

    private static final Logger LOGGER = Logger.getLogger(CombinedServerExecutor.class.getName());
    private static final int RMI_PORT = 1099;
    private static final String RMI_SERVICE_NAME = "GalaxyTruckerService";

    public static void main(String[] args) {
        LOGGER.info("Starting both servers...");

        ServerApplication socketServer = new ServerApplication();
        LOGGER.info("Starting Socket Server...");
        socketServer.startServer();
        LOGGER.info("Socket Server start sequence initiated (runs in background).");

        LOGGER.info("Attempting to start RMI Server...");
        try {

            RemoteService rmiService = new RMIServer();
            LOGGER.info("Instance of RMI RemoteService created.");

            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            LOGGER.info("RMI registry created/located on port: " + RMI_PORT);

            registry.rebind(RMI_SERVICE_NAME, rmiService);
            LOGGER.info("RMI Remote service '" + RMI_SERVICE_NAME + "' registered.");
            LOGGER.info("RMI Server setup complete and waiting for clients.");

        } catch (Exception e) {

            LOGGER.log(Level.SEVERE, "FATAL ERROR during RMI server setup: " + e.getMessage(), e);
            socketServer.stopServer();
            LOGGER.severe("Exiting due to RMI server setup failure.");
            System.exit(1);
        }

        LOGGER.info("Both Socket and RMI servers are now running (or attempting to run).");

    }
}