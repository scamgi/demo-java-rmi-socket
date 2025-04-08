package it.polimi.ingsw.server.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ServerApplication server;
    private PrintWriter out;
    private BufferedReader in;
    private final String clientAddress;

    public ClientHandler(Socket socket, ServerApplication server) {
        this.clientSocket = socket;
        this.server = server;
        this.clientAddress = socket.getInetAddress().toString();
    }

    public String getClientAddress() {
        return clientAddress;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            server.log("Handler started for " + clientAddress);
            out.println("Welcome to the Server!");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                server.log("Received from " + clientAddress + ": " + inputLine);
                server.broadcastMessage(clientAddress + ": " + inputLine, this);
            }
        } catch (IOException e) {
            if (!clientSocket.isClosed()) {
                server.log("Error handling client " + clientAddress + ": " + e.getMessage());
            }
        } finally {
            closeConnection();
            server.removeClient(this);
        }
    }

    public void sendMessage(String message) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(message);
        }
    }

    public void closeConnection() {
        server.log("Closing connection for " + clientAddress);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            server.log("Error closing resources for " + clientAddress + ": " + e.getMessage());
        }
    }
}