package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Main extends Application {

    private TextField phoneNumberField;
    private TextArea messageArea;

    private RadioButton smsButton;
    private RadioButton whatsappButton;

    private Button sendButton;
    private Button clearButton;

    private Label statusLabel;
    private Label characterCountLabel;

    private final TCPClient tcpClient = new TCPClient();

    @Override
    public void start(Stage stage) {

        Label titleLabel =
                new Label("Message Notification System");

        titleLabel.getStyleClass().add("title-label");

        Label descriptionLabel =
                new Label(
                    "Send an important notification to a registered recipient."
                );

        descriptionLabel.getStyleClass().add(
            "description-label"
        );

        VBox headingBox = new VBox(
            5,
            titleLabel,
            descriptionLabel
        );

        headingBox.setAlignment(Pos.CENTER);

        Label phoneLabel =
                new Label("Recipient Phone Number");

        phoneNumberField = new TextField();

        phoneNumberField.setPromptText(
            "Example: +1 671 555 1234"
        );

        phoneNumberField.getStyleClass().add(
            "input-field"
        );

        Label messageLabel =
                new Label("Notification Message");

        messageArea = new TextArea();

        messageArea.setPromptText(
            "Enter the notification message..."
        );

        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(7);

        characterCountLabel = new Label("0 characters");

        characterCountLabel.getStyleClass().add(
            "character-count"
        );

        messageArea.textProperty().addListener(
            (observable, oldText, newText) -> {
                characterCountLabel.setText(
                    newText.length() + " characters"
                );
            }
        );

        VBox messageBox = new VBox(
            5,
            messageArea,
            characterCountLabel
        );

        Label methodLabel =
                new Label("Delivery Method");

        ToggleGroup deliveryGroup = new ToggleGroup();

        smsButton = new RadioButton("SMS — Coming Soon");
        whatsappButton = new RadioButton("WhatsApp");

        smsButton.setToggleGroup(deliveryGroup);
        whatsappButton.setToggleGroup(deliveryGroup);

        /*
         * SMS is shown to match the sample interface,
         * but it is disabled because an SMS API has not
         * been connected.
         */
        smsButton.setDisable(true);

        whatsappButton.setSelected(true);

        HBox deliveryBox = new HBox(
            25,
            smsButton,
            whatsappButton
        );

        deliveryBox.setAlignment(Pos.CENTER_LEFT);

        sendButton = new Button("Send Message");
        clearButton = new Button("Clear");

        sendButton.getStyleClass().add("send-button");
        clearButton.getStyleClass().add("clear-button");

        sendButton.setOnAction(
            event -> handleSendMessage()
        );

        clearButton.setOnAction(
            event -> clearForm()
        );

        HBox buttonBox = new HBox(
            12,
            clearButton,
            sendButton
        );

        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        statusLabel = new Label("Status: Ready");

        statusLabel.getStyleClass().add("status-ready");

        VBox form = new VBox(
            10,
            phoneLabel,
            phoneNumberField,
            messageLabel,
            messageBox,
            methodLabel,
            deliveryBox,
            buttonBox,
            statusLabel
        );

        form.getStyleClass().add("form-container");

        VBox root = new VBox(
            25,
            headingBox,
            form
        );

        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(root, 600, 620);

        String cssFile = getClass()
                .getResource("/resources/javafx.css")
                .toExternalForm();

        scene.getStylesheets().add(cssFile);

        stage.setTitle(
            "Message Notification System"
        );

        stage.setScene(scene);
        stage.setMinWidth(550);
        stage.setMinHeight(600);
        stage.show();
    }

    private void handleSendMessage() {

        String phoneNumber =
                phoneNumberField.getText().trim();

        String message =
                messageArea.getText().trim();

        String deliveryMethod = "WHATSAPP";

        String validationError =
                validateInput(phoneNumber, message);

        if (validationError != null) {
            showError(validationError);
            return;
        }

        setSendingState();

        /*
         * The network request runs on a background thread.
         * Without this Task, JavaFX may freeze while waiting
         * for the server or WhatsApp API.
         */
        Task<String> sendTask = new Task<>() {

            @Override
            protected String call() {
                return tcpClient.sendMessage(
                    phoneNumber,
                    message,
                    deliveryMethod
                );
            }
        };

        sendTask.setOnSucceeded(event -> {

            String response = sendTask.getValue();

            sendButton.setDisable(false);

            if (response.startsWith("SUCCESS")) {
                showSuccess(response);
            } else {
                showError(response);
            }
        });

        sendTask.setOnFailed(event -> {

            sendButton.setDisable(false);

            Throwable error =
                    sendTask.getException();

            showError(
                "The notification could not be sent: " +
                error.getMessage()
            );
        });

        Thread sendThread = new Thread(sendTask);

        sendThread.setDaemon(true);
        sendThread.start();
    }

    private String validateInput(
            String phoneNumber,
            String message
    ) {
        if (phoneNumber.isBlank()) {
            return "Please enter a phone number.";
        }

        if (!phoneNumber.matches(
                "[+]?[0-9() -]{7,20}"
        )) {
            return "Please enter a valid phone number.";
        }

        if (message.isBlank()) {
            return "Please enter a notification message.";
        }

        if (message.length() > 1000) {
            return "The message cannot exceed 1,000 characters.";
        }

        return null;
    }

    private void setSendingState() {

        sendButton.setDisable(true);

        statusLabel.setText(
            "Status: Sending notification..."
        );

        replaceStatusStyle("status-sending");
    }

    private void showSuccess(String message) {

        statusLabel.setText(
            "Status: " + message
        );

        replaceStatusStyle("status-success");

        showAlert(
            Alert.AlertType.INFORMATION,
            "Message Sent",
            message
        );
    }

    private void showError(String message) {

        sendButton.setDisable(false);

        statusLabel.setText(
            "Status: " + message
        );

        replaceStatusStyle("status-error");

        showAlert(
            Alert.AlertType.ERROR,
            "Message Error",
            message
        );
    }

    private void replaceStatusStyle(
            String styleClass
    ) {
        statusLabel.getStyleClass().removeAll(
            "status-ready",
            "status-sending",
            "status-success",
            "status-error"
        );

        statusLabel.getStyleClass().add(
            styleClass
        );
    }

    private void showAlert(
            Alert.AlertType alertType,
            String title,
            String message
    ) {
        Alert alert = new Alert(alertType);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private void clearForm() {

        phoneNumberField.clear();
        messageArea.clear();

        whatsappButton.setSelected(true);

        statusLabel.setText("Status: Ready");

        replaceStatusStyle("status-ready");

        phoneNumberField.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}