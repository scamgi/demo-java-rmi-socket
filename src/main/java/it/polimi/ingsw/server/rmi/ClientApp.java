package it.polimi.ingsw.server.rmi;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientApp extends Application implements ProxyClient {

    private static final Logger LOGGER = Logger.getLogger(ClientApp.class.getName());
    private static final String SERVER_HOST = "localhost"; // or the IP of the server
    private static final int RMI_PORT = 1099;
    private static final String SERVICE_NAME = "GalaxyTruckerService";

    private RemoteService serverService;
    private TextArea outputArea;
    private ProxyClient clientStub;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX RMI Client");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        root.setCenter(outputArea);
        VBox controls = new VBox(10);
        controls.setPadding(new Insets(10, 0, 0, 0));
        TextField nameField = new TextField("ClientFX");
        nameField.setPromptText("Enter your name");
        Button connectButton = new Button("Connect to Server");
        connectButton.setOnAction(_ -> connectToServer(nameField.getText()));
        Button callMethodButton = new Button("Call Remote Method (getMessage)");
        callMethodButton.setOnAction(_ -> callRemoteMethod(nameField.getText()));
        callMethodButton.setDisable(true);
        Button broadcastButton = new Button("Send Broadcast");
        TextField broadcastField = new TextField();
        broadcastField.setPromptText("Message to send to everyone");
        broadcastButton.setOnAction(_ -> sendBroadcast(broadcastField.getText()));
        broadcastButton.setDisable(true);
        controls.getChildren().addAll(nameField, connectButton, callMethodButton, broadcastField, broadcastButton);
        root.setBottom(controls);
        primaryStage.setOnCloseRequest(_ -> {
            disconnectFromServer();
            Platform.exit();
            System.exit(0);
        });
        Scene scene = new Scene(root, 450, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void connectToServer(String clientName) {
        if (serverService != null) {
            logOutput("You are already connected.");
            return;
        }

        try {
            clientStub = (ProxyClient) UnicastRemoteObject.exportObject(this, 0);
            String registryUrl = "rmi://" + SERVER_HOST + ":" + RMI_PORT + "/" + SERVICE_NAME;
            logOutput("Attempting to connect to: " + registryUrl);
            serverService = (RemoteService) Naming.lookup(registryUrl);
            logOutput("RMI Lookup successful.");
            serverService.registerClient(clientStub);
            logOutput("Callback registration sent to the server.");
            logOutput("Connected to RMI server as " + clientName);
            enableControls(true);

        } catch (Exception e) {
            logOutput("RMI connection error: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "RMI Lookup/Register Error", e);
            disconnectFromServer();
        }
    }

    private void callRemoteMethod(String clientName) {
        if (serverService == null) {
            logOutput("You are not connected to the server.");
            return;
        }
        try {
            String response = serverService.sendMessage(clientName);
            logOutput("Response from server: " + response);
        } catch (RemoteException e) {
            logOutput("Error during remote call: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "RMI Call Error", e);
            handleDisconnection();
        }
    }

    private void sendBroadcast(String message) {
        if (serverService == null || message == null || message.trim().isEmpty()) {
            logOutput("Not connected or empty message.");
            return;
        }
        try {
            serverService.broadcastMessage(message);
            logOutput("Broadcast message sent.");
        } catch (RemoteException e) {
            logOutput("Error sending broadcast: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "RMI Broadcast Error", e);
            handleDisconnection();
        }
    }

    @Override
    public void receiveMessage(String message) throws RemoteException {
        logOutput("Message from Server (Callback): " + message);
    }

    private void disconnectFromServer() {
        if (serverService != null && clientStub != null) {
            try {
                serverService.removeClient(clientStub);
            } catch (RemoteException e) {
                LOGGER.warning("Error unregistering client: " + e.getMessage());
            }
        }

        if (clientStub != null) {
            try {
                UnicastRemoteObject.unexportObject(this, true); // Force unexport
                logOutput("Client callback unexported.");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during client unexport", e);
            }
        }
        serverService = null;
        clientStub = null;
        enableControls(false);
        logOutput("Disconnected.");
    }

    private void handleDisconnection() {
        logOutput("!! Communication error with the server. It might have disconnected.");
        disconnectFromServer();
    }

    private void logOutput(String message) {
        if (Platform.isFxApplicationThread()) {
            outputArea.appendText(message + "\n");
        } else {
            Platform.runLater(() -> outputArea.appendText(message + "\n"));
        }
        LOGGER.info(message);
    }

    private void enableControls(boolean enable) {
        BorderPane root = (BorderPane) outputArea.getScene().getRoot();
        VBox controls = (VBox) root.getBottom();
        controls.getChildren().get(1).setDisable(enable); // Connect Button
        controls.getChildren().get(2).setDisable(!enable); // Call Method Button
        controls.getChildren().get(4).setDisable(!enable); // Broadcast Button
    }

    public static void main(String[] args) {
        launch(args);
    }
}