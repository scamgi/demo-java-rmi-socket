package it.polimi.ingsw.server.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ServerApplication {

    /**
     * Server port
     */
    private static final int PORT = 12345;
    private static final int MAX_CLIENTS = 4;

    // object to log actions
    private static final java.util.logging.Logger LOGGER = Logger.getLogger(ServerApplication.class.getName());
    private ServerSocket serverSocket;
    private ExecutorService clientExecutorService; // To handle multiple clients
    private final List<ClientHandler> connectedClients = new ArrayList<>();
    private volatile boolean serverRunning = false; // Flag to control the server loop

    /**
     * Writes a message on the console
     * @param message Message to be written in the UI
     */
    void log(String message) {
        LOGGER.info(message);
    }

    /**
     * Starts the server
     */
    public void startServer() {
        log("Starting server on port " + PORT + "...");
        clientExecutorService = Executors.newFixedThreadPool(MAX_CLIENTS); // Thread pool for clients
        serverRunning = true;

        // starts the server on a new thread to avoid blocking everything.
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                log("Server started successfully.");

                while (serverRunning && !serverSocket.isClosed()) {
                    try {
                        // Wait for a client connection (blocking call)
                        Socket clientSocket = serverSocket.accept();
                        log("Client connected: " + clientSocket.getInetAddress());

                        // Create a handler for the new client and submit to executor
                        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                        synchronized (connectedClients) {
                            connectedClients.add(clientHandler);
                        }
                        clientExecutorService.submit(clientHandler);

                    } catch (IOException e) {
                        if (serverRunning) {
                            log("Error accepting client connection: " + e.getMessage());
                        } else {
                            log("Server socket closed."); // Expected when stopping
                        }
                    }
                }
            } catch (IOException e) {
                log("Could not start server on port " + PORT + ": " + e.getMessage());
            } finally {
                cleanupServerResources();
            }
        }).start();
    }

    /**
     * Stops the server
     */
    public void stopServer() {
        log("Stopping server...");
        serverRunning = false; // Signal the server loop to stop

        // Close client connections gracefully
        synchronized (connectedClients) {
            for (ClientHandler handler : connectedClients) {
                handler.closeConnection();
            }
            connectedClients.clear();
        }

        // Shutdown the client handler thread pool
        if (clientExecutorService != null && !clientExecutorService.isShutdown()) {
            clientExecutorService.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!clientExecutorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    clientExecutorService.shutdownNow(); // Cancel currently executing tasks
                }
            } catch (InterruptedException ie) {
                clientExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Close the server socket (this will also interrupt the accept() call)
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log("Error closing server socket: " + e.getMessage());
            }
        }
        log("Server stopped.");
    }

    /**
     * Method to be called by ClientHandler when a client disconnects
     * @param clientHandler Client to be removed
     */
    protected void removeClient(ClientHandler clientHandler) {
        synchronized (connectedClients) {
            connectedClients.remove(clientHandler);
        }
        log("Client disconnected: " + clientHandler.getClientAddress());
    }

    /**
     * Broadcasts a message to all clients
     * @param message The message to send
     * @param sender The client who is sending the message
     */
    public void broadcastMessage(String message, ClientHandler sender) {
        log("Broadcasting: " + message);
        synchronized (connectedClients) {
            for (ClientHandler handler : connectedClients) {
                if (handler != sender) { // Don't send back to the original sender
                    handler.sendMessage(message);
                }
            }
        }
    }

    /**
     * Cleans the resources by stopping the server
     */
    private void cleanupServerResources() {
        stopServer(); // Call stop logic again to be sure
        log("Server resources cleaned up.");
    }

    /**
     * Runs the server
     * @param args Arguments to pass to the server
     */
    public static void main(String[] args) {
        ServerApplication application = new ServerApplication();

        // this will start the server
        application.startServer();

        // when you reach this line, it means the server stopped
        application.log("Server process exiting.");
    }
}
