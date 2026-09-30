package client;

// JavaFX imports used to create the desktop interface and background task.
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Main JavaFX application for the Message Notification System.
 *
 * This class builds the GUI, validates user input, calls TCPClient on a
 * background thread, and displays success or error feedback to the user.
 */
public class Main extends Application {

    // Form controls that are accessed by multiple methods in this class.
    private TextField phoneNumberField;
    private TextArea messageArea;

    // Delivery method controls. SMS is displayed but not implemented yet.
    private RadioButton smsButton;
    private RadioButton whatsappButton;

    // Buttons used to submit or reset the form.
    private Button sendButton;
    private Button clearButton;

    // Labels used to report program status and the current message length.
    private Label statusLabel;
    private Label characterCountLabel;

    // Reuse one TCPClient object whenever the user sends a message.
    private final TCPClient tcpClient = new TCPClient();

    /**
     * JavaFX calls start() after the application launches.
     * This method constructs and displays the entire interface.
     */
    @Override
    public void start(Stage stage) {

        // Create the application's heading.
        Label titleLabel = new Label("Message Notification System");
        titleLabel.getStyleClass().add("title-label");

        // Add a short explanation below the heading.
        Label descriptionLabel = new Label(
            "Send an important notification to a registered recipient."
        );
        descriptionLabel.getStyleClass().add("description-label");

        // Stack and center the two heading labels.
        VBox headingBox = new VBox(5, titleLabel, descriptionLabel);
        headingBox.setAlignment(Pos.CENTER);

        // Build the phone-number portion of the form.
        Label phoneLabel = new Label("Recipient Phone Number");
        phoneNumberField = new TextField();
        phoneNumberField.setPromptText("Example: +1 671 555 1234");
        phoneNumberField.getStyleClass().add("input-field");

        // Build the message portion of the form.
        Label messageLabel = new Label("Notification Message");
        messageArea = new TextArea();
        messageArea.setPromptText("Enter the notification message...");
        messageArea.setWrapText(true);      // Wrap long text inside the box.
        messageArea.setPrefRowCount(7);     // Give the box a comfortable height.

        // Initially, the message contains zero characters.
        characterCountLabel = new Label("0 characters");
        characterCountLabel.getStyleClass().add("character-count");

        // Update the character counter every time the message changes.
        messageArea.textProperty().addListener(
            (observable, oldText, newText) -> characterCountLabel.setText(
                newText.length() + " characters"
            )
        );

        // Keep the message area and its counter together.
        VBox messageBox = new VBox(5, messageArea, characterCountLabel);

        // Create mutually exclusive delivery-method choices.
        Label methodLabel = new Label("Delivery Method");
        ToggleGroup deliveryGroup = new ToggleGroup();
        smsButton = new RadioButton("SMS — Coming Soon");
        whatsappButton = new RadioButton("WhatsApp");
        smsButton.setToggleGroup(deliveryGroup);
        whatsappButton.setToggleGroup(deliveryGroup);

        // SMS is visible for the planned design but has no connected API yet.
        smsButton.setDisable(true);

        // WhatsApp is the current and default delivery method.
        whatsappButton.setSelected(true);

        // Display the delivery options next to one another.
        HBox deliveryBox = new HBox(25, smsButton, whatsappButton);
        deliveryBox.setAlignment(Pos.CENTER_LEFT);

        // Create and style the form buttons.
        sendButton = new Button("Send Message");
        clearButton = new Button("Clear");
        sendButton.getStyleClass().add("send-button");
        clearButton.getStyleClass().add("clear-button");

        // Connect each button to the method that handles its action.
        sendButton.setOnAction(event -> handleSendMessage());
        clearButton.setOnAction(event -> clearForm());

        // Place the form buttons on one row and align them to the right.
        HBox buttonBox = new HBox(12, clearButton, sendButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        // Display the application's initial state.
        statusLabel = new Label("Status: Ready");
        statusLabel.getStyleClass().add("status-ready");

        // Assemble every form control in display order.
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

        // Combine the heading and form into the root layout.
        VBox root = new VBox(25, headingBox, form);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);

        // Create the window's scene at its starting size.
        Scene scene = new Scene(root, 600, 620);

        // Load the external stylesheet from the compiled resources folder.
        String cssFile = getClass()
                .getResource("/resources/javafx.css")
                .toExternalForm();
        scene.getStylesheets().add(cssFile);

        // Configure and display the JavaFX window.
        stage.setTitle("Message Notification System");
        stage.setScene(scene);
        stage.setMinWidth(550);
        stage.setMinHeight(600);
        stage.show();
    }

    /** Collects the form data and begins the message-sending process. */
    private void handleSendMessage() {

        // Read the user's values and remove surrounding whitespace.
        String phoneNumber = phoneNumberField.getText().trim();
        String message = messageArea.getText().trim();

        // WhatsApp is currently the only enabled option.
        String deliveryMethod = "WHATSAPP";

        // Validate locally before opening a network connection.
        String validationError = validateInput(phoneNumber, message);
        if (validationError != null) {
            showError(validationError);
            return; // Do not continue if the input is invalid.
        }

        // Disable repeated submissions and show progress to the user.
        setSendingState();

        /*
         * Perform the network request on a background thread. Running it on
         * the JavaFX application thread would freeze the interface while the
         * program waited for the server and Vonage API.
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

        // This handler runs on the JavaFX thread after a normal response.
        sendTask.setOnSucceeded(event -> {
            String response = sendTask.getValue();
            sendButton.setDisable(false);

            // Use the response prefix to decide which feedback to display.
            if (response.startsWith("SUCCESS")) {
                showSuccess(response);
            } else {
                showError(response);
            }
        });

        // This handler runs if the background task throws an exception.
        sendTask.setOnFailed(event -> {
            sendButton.setDisable(false);
            Throwable error = sendTask.getException();
            showError(
                "The notification could not be sent: " + error.getMessage()
            );
        });

        // Start the task on a daemon thread so it cannot block application exit.
        Thread sendThread = new Thread(sendTask);
        sendThread.setDaemon(true);
        sendThread.start();
    }

    /** Returns an error message, or null when both values are valid. */
    private String validateInput(String phoneNumber, String message) {

        // A recipient is required.
        if (phoneNumber.isBlank()) {
            return "Please enter a phone number.";
        }

        // Permit common phone formatting while limiting the overall length.
        if (!phoneNumber.matches("[+]?[0-9() -]{7,20}")) {
            return "Please enter a valid phone number.";
        }

        // A notification cannot be empty.
        if (message.isBlank()) {
            return "Please enter a notification message.";
        }

        // Apply a smaller interface limit than WhatsApp's provider limit.
        if (message.length() > 1000) {
            return "The message cannot exceed 1,000 characters.";
        }

        return null; // No validation problem was found.
    }

    /** Updates the interface while a message is being processed. */
    private void setSendingState() {
        sendButton.setDisable(true);
        statusLabel.setText("Status: Sending notification...");
        replaceStatusStyle("status-sending");
    }

    /** Displays successful delivery feedback. */
    private void showSuccess(String message) {
        statusLabel.setText("Status: " + message);
        replaceStatusStyle("status-success");
        showAlert(Alert.AlertType.INFORMATION, "Message Sent", message);
    }

    /** Displays error feedback and re-enables the Send button. */
    private void showError(String message) {
        sendButton.setDisable(false);
        statusLabel.setText("Status: " + message);
        replaceStatusStyle("status-error");
        showAlert(Alert.AlertType.ERROR, "Message Error", message);
    }

    /** Replaces the previous status color with the requested CSS class. */
    private void replaceStatusStyle(String styleClass) {
        statusLabel.getStyleClass().removeAll(
            "status-ready",
            "status-sending",
            "status-success",
            "status-error"
        );
        statusLabel.getStyleClass().add(styleClass);
    }

    /** Creates a modal JavaFX alert for an important result. */
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

    /** Restores the form to its initial state. */
    private void clearForm() {
        phoneNumberField.clear();
        messageArea.clear();
        whatsappButton.setSelected(true);
        statusLabel.setText("Status: Ready");
        replaceStatusStyle("status-ready");
        phoneNumberField.requestFocus();
    }

    /** Standard Java entry point; JavaFX continues execution in start(). */
    public static void main(String[] args) {
        launch(args);
    }
}
