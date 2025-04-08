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

    private static final String DEFAULT_SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

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

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        serverAddressField = new TextField(DEFAULT_SERVER_ADDRESS);
        portField = new TextField(String.valueOf(SERVER_PORT));
        portField.setPrefWidth(60);
        connectButton = new Button("Connect");
        connectButton.setOnAction(_ -> toggleConnection());
        HBox connectionBox = new HBox(5, new Label("Server:"), serverAddressField, new Label("Port:"), portField, connectButton);
        root.setTop(connectionBox);

        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        root.setCenter(messageArea);

        inputField = new TextField();
        inputField.setPromptText("Enter message...");
        inputField.setOnAction(_ -> sendMessage());
        sendButton = new Button("Send");
        sendButton.setOnAction(_ -> sendMessage());
        sendButton.setDisable(true);
        inputField.setDisable(true);
        HBox inputBox = new HBox(5, inputField, sendButton);
        HBox.setHgrow(inputField, javafx.scene.layout.Priority.ALWAYS);
        root.setBottom(inputBox);
        Scene scene = new Scene(root, 500, 400);
        primaryStage.setTitle("JavaFX Client");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(_ -> disconnect());
        primaryStage.show();
    }

    private void toggleConnection() {
        if (connected) {
            disconnect();
        } else {
            connect();
        }
    }

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

    private void disconnect() {
        log("Disconnecting...");
        connected = false;

        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {

            System.err.println("Error closing client resources: " + e.getMessage());
        } finally {

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

        new Thread(() -> {
            try {
                String serverMessage;

                while (connected && (serverMessage = in.readLine()) != null) {
                    final String msg = serverMessage;
                    Platform.runLater(() -> log("Server: " + msg));
                }
            } catch (IOException e) {
                if (connected) {
                    Platform.runLater(() -> log("Connection lost: " + e.getMessage()));
                }
            } finally {

                if (connected) {
                    Platform.runLater(this::disconnect);
                }
            }
        }).start();
    }

    private void sendMessage() {
        if (!connected || out == null) {
            log("Not connected to the server.");
            return;
        }
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            log("Me: " + message);
            inputField.clear();
        }
    }

    private void log(String message) {

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