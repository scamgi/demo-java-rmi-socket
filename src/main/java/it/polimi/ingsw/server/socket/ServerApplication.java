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

    private static final int PORT = 12345;
    private static final int MAX_CLIENTS = 4;

    private static final java.util.logging.Logger LOGGER = Logger.getLogger(ServerApplication.class.getName());
    private ServerSocket serverSocket;
    private ExecutorService clientExecutorService;
    private final List<ClientHandler> connectedClients = new ArrayList<>();
    private volatile boolean serverRunning = false;

    void log(String message) {
        LOGGER.info(message);
    }

    public void startServer() {
        log("Starting server on port " + PORT + "...");
        clientExecutorService = Executors.newFixedThreadPool(MAX_CLIENTS);
        serverRunning = true;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                log("Server started successfully.");

                while (serverRunning && !serverSocket.isClosed()) {
                    try {

                        Socket clientSocket = serverSocket.accept();
                        log("Client connected: " + clientSocket.getInetAddress());

                        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                        synchronized (connectedClients) {
                            connectedClients.add(clientHandler);
                        }
                        clientExecutorService.submit(clientHandler);

                    } catch (IOException e) {
                        if (serverRunning) {
                            log("Error accepting client connection: " + e.getMessage());
                        } else {
                            log("Server socket closed.");
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

    public void stopServer() {
        log("Stopping server...");
        serverRunning = false;

        synchronized (connectedClients) {
            for (ClientHandler handler : connectedClients) {
                handler.closeConnection();
            }
            connectedClients.clear();
        }

        if (clientExecutorService != null && !clientExecutorService.isShutdown()) {
            clientExecutorService.shutdown();
            try {

                if (!clientExecutorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    clientExecutorService.shutdownNow();
                }
            } catch (InterruptedException ie) {
                clientExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log("Error closing server socket: " + e.getMessage());
            }
        }
        log("Server stopped.");
    }

    protected void removeClient(ClientHandler clientHandler) {
        synchronized (connectedClients) {
            connectedClients.remove(clientHandler);
        }
        log("Client disconnected: " + clientHandler.getClientAddress());
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        log("Broadcasting: " + message);
        synchronized (connectedClients) {
            for (ClientHandler handler : connectedClients) {
                if (handler != sender) {
                    handler.sendMessage(message);
                }
            }
        }
    }

    private void cleanupServerResources() {
        stopServer();
        log("Server resources cleaned up.");
    }

    public static void main(String[] args) {
        ServerApplication application = new ServerApplication();

        application.startServer();

        application.log("Server process exiting.");
    }
}