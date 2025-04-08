package it.polimi.ingsw.server;

import it.polimi.ingsw.server.rmi.RemoteService;
import it.polimi.ingsw.server.rmi.RMIServer;
import it.polimi.ingsw.server.socket.ServerApplication;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes both the Socket and RMI servers concurrently.
 */
public class CombinedServerExecutor {

    private static final Logger LOGGER = Logger.getLogger(CombinedServerExecutor.class.getName());
    private static final int RMI_PORT = 1099; // Default RMI port used in ServerMain
    private static final String RMI_SERVICE_NAME = "GalaxyTruckerService"; // Service name from ServerMain

    public static void main(String[] args) {
        LOGGER.info("Starting both servers...");

        // 1. Start the Socket Server
        ServerApplication socketServer = new ServerApplication();
        LOGGER.info("Starting Socket Server...");
        socketServer.startServer();
        LOGGER.info("Socket Server start sequence initiated (runs in background).");


        // 2. Start the RMI Server
        LOGGER.info("Attempting to start RMI Server...");
        try {
            // Create an instance of the RMI service implementation
            RemoteService rmiService = new RMIServer();
            LOGGER.info("Instance of RMI RemoteService created.");

            // Create or get the RMI registry
            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            LOGGER.info("RMI registry created/located on port: " + RMI_PORT);

            // Bind the RMI service implementation to the registry
            registry.rebind(RMI_SERVICE_NAME, rmiService);
            LOGGER.info("RMI Remote service '" + RMI_SERVICE_NAME + "' registered.");
            LOGGER.info("RMI Server setup complete and waiting for clients.");

        } catch (Exception e) {
            // Catch potential exceptions during RMI setup
            LOGGER.log(Level.SEVERE, "FATAL ERROR during RMI server setup: " + e.getMessage(), e);
            socketServer.stopServer();
            LOGGER.severe("Exiting due to RMI server setup failure.");
            System.exit(1); // Exit if RMI setup fails
        }

        LOGGER.info("Both Socket and RMI servers are now running (or attempting to run).");
        // The main thread will now wait
    }
}