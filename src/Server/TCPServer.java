package server;

// Classes used to receive TCP data and display server-side information.
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Local TCP server for the Message Notification System.
 *
 * The server receives requests from TCPClient, validates and decodes them,
 * sends valid WhatsApp requests through VonageService, and returns a result.
 */
public class TCPServer {

    // This must match the port configured in TCPClient.
    private static final int PORT = 65432;

    // Largest number of recipients accepted in one request (matches client).
    private static final int MAX_RECIPIENTS = 20;

    /** Starts the server and continuously waits for clients. */
    public static void main(String[] args) {

        // Print a readable server heading in the terminal.
        System.out.println("========================================");
        System.out.println("     MESSAGE NOTIFICATION SERVER");
        System.out.println("========================================");

        // Open the listening socket and close it automatically on shutdown.
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Listening on port " + PORT + "...");

            // Keep accepting clients until the server is manually stopped.
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            }

        } catch (Exception e) {
            // A fatal socket error stops the main server loop.
            System.err.println("[SERVER] Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Reads one client request, processes it, and returns one response. */
    private static void handleClient(Socket clientSocket) {

        System.out.println();
        System.out.println(
            "[SERVER] Client connected: " +
            clientSocket.getRemoteSocketAddress()
        );

        try (
            // Assigning clientSocket here ensures that it is closed afterward.
            Socket socket = clientSocket;

            // Read UTF-8 text sent by TCPClient.
            BufferedReader input = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );

            // Return success or error text to TCPClient.
            PrintWriter output = new PrintWriter(
                socket.getOutputStream(),
                true
            )
        ) {
            // TCPClient sends exactly one request line per connection.
            String request = input.readLine();

            // Validate, decode, and deliver the request.
            String response = processRequest(request);

            // Send the final result back to the JavaFX application.
            output.println(response);

        } catch (Exception e) {
            // Report a problem that affects only the current client.
            System.err.println("[SERVER] Client error: " + e.getMessage());
        }

        System.out.println("[SERVER] Client disconnected.");
        System.out.println("[SERVER] Waiting for another client...");
    }

    /** Validates the custom request and forwards valid data to Vonage. */
    private static String processRequest(String request) {

        // Reject a connection that did not send usable data.
        if (request == null || request.isBlank()) {
            return "ERROR: Empty request received.";
        }

        /*
         * Split into no more than three fields:
         * delivery method, phone number list, and encoded message.
         * The phone field may hold several numbers separated by commas.
         */
        String[] parts = request.split("\\|", 3);

        // All three fields are required.
        if (parts.length != 3) {
            return "ERROR: Invalid request format.";
        }

        // Extract and clean each request field.
        String deliveryMethod = parts[0].trim();
        String phoneField = parts[1].trim();
        String encodedMessage = parts[2].trim();

        // Reject unimplemented delivery channels.
        if (!deliveryMethod.equalsIgnoreCase("WHATSAPP")) {
            return "ERROR: Only WhatsApp is available.";
        }

        // Split the list, revalidate every number, and drop duplicates.
        List<String> recipients = new ArrayList<>();
        Set<String> seenDigits = new HashSet<>();

        for (String entry : phoneField.split("[,;]")) {
            String number = entry.trim();
            if (number.isEmpty()) {
                continue;
            }
            if (!number.matches("[+]?[0-9() -]{7,20}")) {
                return "ERROR: Invalid phone number: " + number + ".";
            }
            String digits = number.replaceAll("[^0-9]", "");
            if (seenDigits.add(digits)) {
                recipients.add(number);
            }
        }

        if (recipients.isEmpty()) {
            return "ERROR: No phone numbers provided.";
        }

        if (recipients.size() > MAX_RECIPIENTS) {
            return "ERROR: At most " + MAX_RECIPIENTS +
                   " recipients are allowed per request.";
        }

        String message;

        try {
            // Decode the Base64 message back into its original UTF-8 text.
            byte[] decodedBytes = Base64.getDecoder().decode(encodedMessage);
            message = new String(decodedBytes, StandardCharsets.UTF_8).trim();

        } catch (IllegalArgumentException e) {
            // Base64.getDecoder throws this exception for malformed data.
            return "ERROR: Invalid message encoding.";
        }

        // Check the decoded value before calling the external API.
        if (message.isBlank()) {
            return "ERROR: Message cannot be empty.";
        }

        // Apply the provider's WhatsApp text-length limit.
        if (message.length() > 4096) {
            return "ERROR: WhatsApp messages cannot exceed 4,096 characters.";
        }

        // Log the validated notification in the server terminal.
        displayNotification(recipients, deliveryMethod, message);

        // Send to every recipient and return one combined result to TCPClient.
        return sendToAllRecipients(recipients, message);
    }

    /** Prints a readable copy of the accepted request for demonstration. */
    private static void displayNotification(
            List<String> recipients,
            String deliveryMethod,
            String message
    ) {
        System.out.println();
        System.out.println("INCOMING NOTIFICATION");
        System.out.println("----------------------------------------");
        System.out.println("Received: " + LocalDateTime.now());
        System.out.println("Recipients (" + recipients.size() + "): " +
                           String.join(", ", recipients));
        System.out.println("Method: " + deliveryMethod);
        System.out.println();
        System.out.println("Message:");
        System.out.println(message);
        System.out.println("----------------------------------------");
    }

    /**
     * Sends the message to each recipient through VonageService and builds one
     * single-line response summarizing every result.
     *
     * The Vonage Messages API accepts one recipient per request, so this loops
     * and keeps going even if an individual send fails.
     */
    private static String sendToAllRecipients(
            List<String> recipients,
            String message
    ) {
        VonageService service;

        try {
            // The constructor also verifies the required environment variables.
            service = new VonageService();

        } catch (IllegalStateException e) {
            // Missing Vonage environment variables are configuration errors.
            System.err.println(
                "[VONAGE] Configuration error: " + e.getMessage()
            );
            return "ERROR: Server configuration is incomplete. " +
                   e.getMessage();
        }

        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (String phoneNumber : recipients) {

            // Submit the message and receive a structured delivery result.
            VonageService.DeliveryResult result = service.sendMessage(
                phoneNumber,
                message
            );

            // Log details that are useful during testing and demonstrations.
            System.out.println();
            System.out.println("[VONAGE] Recipient: " + phoneNumber);
            System.out.println(
                "[VONAGE] HTTP status: " + result.getStatusCode()
            );
            System.out.println("[VONAGE] Result: " + result.getMessage());
            System.out.println(
                "[VONAGE] Provider response: " + result.getProviderResponse()
            );

            if (result.isSuccessful()) {
                succeeded.add(phoneNumber);
            } else {
                failed.add(phoneNumber + " (HTTP " +
                           result.getStatusCode() + ")");
            }
        }

        int total = recipients.size();

        // Every recipient was accepted.
        if (failed.isEmpty()) {
            return "SUCCESS: WhatsApp message accepted for " + total +
                   " recipient(s): " + String.join(", ", succeeded) + ".";
        }

        // Nobody received the message.
        if (succeeded.isEmpty()) {
            return "ERROR: Message failed for all " + total +
                   " recipient(s): " + String.join(", ", failed) + ".";
        }

        // Mixed outcome: tell the user exactly who was missed.
        return "PARTIAL: Sent to " + succeeded.size() + " of " + total +
               ". Failed: " + String.join(", ", failed) + ".";
    }
}