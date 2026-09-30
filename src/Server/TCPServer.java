package server;

// Classes used to receive TCP data and display server-side information.
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Local TCP server for the Message Notification System.
 *
 * The server receives requests from TCPClient, validates and decodes them,
 * sends valid WhatsApp requests through VonageService, and returns a result.
 */
public class TCPServer {

    // This must match the port configured in TCPClient.
    private static final int PORT = 65432;

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
         * delivery method, phone number, and encoded message.
         */
        String[] parts = request.split("\\|", 3);

        // All three fields are required.
        if (parts.length != 3) {
            return "ERROR: Invalid request format.";
        }

        // Extract and clean each request field.
        String deliveryMethod = parts[0].trim();
        String phoneNumber = parts[1].trim();
        String encodedMessage = parts[2].trim();

        // Reject unimplemented delivery channels.
        if (!deliveryMethod.equalsIgnoreCase("WHATSAPP")) {
            return "ERROR: Only WhatsApp is available.";
        }

        // Revalidate the phone number instead of trusting client validation.
        if (!phoneNumber.matches("[+]?[0-9() -]{7,20}")) {
            return "ERROR: Invalid phone number.";
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
        displayNotification(phoneNumber, deliveryMethod, message);

        // Send through Vonage and return the result to TCPClient.
        return sendThroughVonage(phoneNumber, message);
    }

    /** Prints a readable copy of the accepted request for demonstration. */
    private static void displayNotification(
            String phoneNumber,
            String deliveryMethod,
            String message
    ) {
        System.out.println();
        System.out.println("INCOMING NOTIFICATION");
        System.out.println("----------------------------------------");
        System.out.println("Received: " + LocalDateTime.now());
        System.out.println("Recipient: " + phoneNumber);
        System.out.println("Method: " + deliveryMethod);
        System.out.println();
        System.out.println("Message:");
        System.out.println(message);
        System.out.println("----------------------------------------");
    }

    /** Calls VonageService and converts its result into a client response. */
    private static String sendThroughVonage(
            String phoneNumber,
            String message
    ) {
        try {
            // The constructor also verifies the required environment variables.
            VonageService service = new VonageService();

            // Submit the message and receive a structured delivery result.
            VonageService.DeliveryResult result = service.sendMessage(
                phoneNumber,
                message
            );

            // Log details that are useful during testing and demonstrations.
            System.out.println();
            System.out.println(
                "[VONAGE] HTTP status: " + result.getStatusCode()
            );
            System.out.println("[VONAGE] Result: " + result.getMessage());
            System.out.println(
                "[VONAGE] Provider response: " + result.getProviderResponse()
            );

            // Translate a successful API result into GUI-friendly text.
            if (result.isSuccessful()) {
                return "SUCCESS: WhatsApp message accepted for " +
                       phoneNumber + ".";
            }

            // Include the HTTP status when the provider rejects the request.
            return "ERROR: " + result.getMessage() +
                   " HTTP status " + result.getStatusCode() + ".";

        } catch (IllegalStateException e) {
            // Missing Vonage environment variables are configuration errors.
            System.err.println(
                "[VONAGE] Configuration error: " + e.getMessage()
            );
            return "ERROR: Server configuration is incomplete. " +
                   e.getMessage();
        }
    }
}
