package it.polimi.ingsw.server.socket;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientApplication extends Application {

    private static final String DEFAULT_SERVER_ADDRESS = "localhost"; // Or server's IP
    private static final int SERVER_PORT = 12345; // Must match server port

    private TextArea messageArea;
    private TextField inputField;
    private Button sendButton;
    private Button connectButton;
    private TextField serverAddressField;
    private TextField portField;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean connected = false;

    @Override
    public void start(Stage primaryStage) {

        // Window stuff
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        // Top: Connection controls
        serverAddressField = new TextField(DEFAULT_SERVER_ADDRESS);
        portField = new TextField(String.valueOf(SERVER_PORT));
        portField.setPrefWidth(60);
        connectButton = new Button("Connect");
        connectButton.setOnAction(_ -> toggleConnection());
        HBox connectionBox = new HBox(5, new Label("Server:"), serverAddressField, new Label("Port:"), portField, connectButton);
        root.setTop(connectionBox);
        // Center: Message display area
        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        root.setCenter(messageArea);
        // Bottom: Input field and send button
        inputField = new TextField();
        inputField.setPromptText("Enter message...");
        inputField.setOnAction(_ -> sendMessage()); // Allow sending with Enter key
        sendButton = new Button("Send");
        sendButton.setOnAction(_ -> sendMessage());
        sendButton.setDisable(true); // Disabled until connected
        inputField.setDisable(true); // Disabled until connected
        HBox inputBox = new HBox(5, inputField, sendButton);
        HBox.setHgrow(inputField, javafx.scene.layout.Priority.ALWAYS); // Make input field grow
        root.setBottom(inputBox);
        Scene scene = new Scene(root, 500, 400);
        primaryStage.setTitle("JavaFX Client");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(_ -> disconnect()); // Disconnect on close
        primaryStage.show();
    }

    /**
     * Connects or disconnects the client.
     */
    private void toggleConnection() {
        if (connected) {
            disconnect();
        } else {
            connect();
        }
    }

    /**
     * Connects the client
     */
    private void connect() {
        String serverAddress = serverAddressField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            log("Invalid port number.");
            return;
        }

        log("Attempting to connect to " + serverAddress + ":" + port + "...");

        // Network connection must happen in a background thread, to avoid blocking the UI
        new Thread(() -> {
            try {
                socket = new Socket(serverAddress, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;

                Platform.runLater(() -> {
                    log("Connected successfully.");
                    connectButton.setText("Disconnect");
                    sendButton.setDisable(false);
                    inputField.setDisable(false);
                    serverAddressField.setDisable(true);
                    portField.setDisable(true);
                });

                // Start a new thread to continuously listen for server messages
                startListening();

            } catch (UnknownHostException e) {
                Platform.runLater(() -> log("Error: Unknown host " + serverAddress));
                resetConnectionState();
            } catch (IOException e) {
                Platform.runLater(() -> log("Error connecting to server: " + e.getMessage()));
                resetConnectionState();
            }
        }).start();
    }

    /**
     * Disconnects the application
     */
    private void disconnect() {
        log("Disconnecting...");
        connected = false; // Signal listener thread to stop

        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Log quietly or ignore, as we are disconnecting anyway
            System.err.println("Error closing client resources: " + e.getMessage());
        } finally {
            // Reset UI elements on the JavaFX thread
            Platform.runLater(this::resetConnectionState);
            log("Disconnected.");
        }
    }

    private void resetConnectionState() {
        connected = false;
        socket = null;
        in = null;
        out = null;
        connectButton.setText("Connect");
        sendButton.setDisable(true);
        inputField.setDisable(true);
        serverAddressField.setDisable(false);
        portField.setDisable(false);
    }


    private void startListening() {
        // Listener runs in its own thread
        new Thread(() -> {
            try {
                String serverMessage;
                // Keep listening as long as we are 'connected' and the input stream is valid
                while (connected && (serverMessage = in.readLine()) != null) {
                    final String msg = serverMessage; // Final variable for lambda
                    Platform.runLater(() -> log("Server: " + msg));
                }
            } catch (IOException e) {
                if (connected) { // Only show error if we didn't intentionally disconnect
                    Platform.runLater(() -> log("Connection lost: " + e.getMessage()));
                }
            } finally {
                // If the loop ends (server disconnected or error), ensure we are fully disconnected
                if (connected) {
                    Platform.runLater(this::disconnect);
                }
            }
        }).start();
    }

    /**
     * Sends a message to the server
     */
    private void sendMessage() {
        if (!connected || out == null) {
            log("Not connected to the server.");
            return;
        }
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message); // Send message to server
            log("Me: " + message); // Log locally
            inputField.clear();
        }
    }

    private void log(String message) {
        // Ensure UI updates happen on the JavaFX Application Thread
        if (Platform.isFxApplicationThread()) {
            messageArea.appendText(message + "\n");
        } else {
            Platform.runLater(() -> messageArea.appendText(message + "\n"));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}