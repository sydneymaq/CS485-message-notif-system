// receives message requests

package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class TCPServer {

    private static final int PORT = 65432;

    public static void main(String[] args) {

        System.out.println("==============================================");
        System.out.println("       MESSAGE NOTIFICATION SERVER");
        System.out.println("==============================================");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println(
                "[SERVER] Listening on port " + PORT + "..."
            );

            /*
             * The server remains running after a client disconnects.
             * It returns to accept() and waits for another client.
             */
            while (true) {
                try {
                    handleClient(serverSocket.accept());
                } catch (Exception e) {
                    System.err.println(
                        "[SERVER] Client processing error: "
                        + e.getMessage()
                    );
                }
            }

        } catch (Exception e) {
            System.err.println(
                "[SERVER] Could not start: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {

        System.out.println();
        System.out.println(
            "[SERVER] Client connected: "
            + clientSocket.getRemoteSocketAddress()
        );

        try (
            Socket socket = clientSocket;
            BufferedReader input =
                    new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                    );
            PrintWriter output =
                    new PrintWriter(socket.getOutputStream(), true)
        ) {
            String request = input.readLine();

            if (request == null || request.isBlank()) {
                output.println(
                    "ERROR: The server received an empty request."
                );

                return;
            }

            String response = processRequest(request);

            output.println(response);

        } catch (Exception e) {
            System.err.println(
                "[SERVER] Connection error: " + e.getMessage()
            );
        }

        System.out.println("[SERVER] Client disconnected.");
        System.out.println("[SERVER] Waiting for another client...");
    }

    private static String processRequest(String request) {

        /*
         * The limit of 3 prevents additional separators from
         * creating unexpected fields.
         */
        String[] parts = request.split("\\|", 3);

        if (parts.length != 3) {
            return "ERROR: Invalid request format.";
        }

        String deliveryMethod = parts[0].trim();
        String phoneNumber = parts[1].trim();
        String encodedMessage = parts[2].trim();

        if (!isValidDeliveryMethod(deliveryMethod)) {
            return "ERROR: Delivery method must be SMS or WHATSAPP.";
        }

        if (!isValidPhoneNumber(phoneNumber)) {
            return "ERROR: The phone number is invalid.";
        }

        String message;

        try {
            byte[] decodedBytes =
                    Base64.getDecoder().decode(encodedMessage);

            message = new String(
                decodedBytes,
                StandardCharsets.UTF_8
            ).trim();

        } catch (IllegalArgumentException e) {
            return "ERROR: The message could not be decoded.";
        }

        if (message.isEmpty()) {
            return "ERROR: The message cannot be empty.";
        }

        displayNotification(
            phoneNumber,
            deliveryMethod,
            message
        );

        /*
         * This is where a real SMS or WhatsApp API method
         * will eventually be called.
         */
        boolean sentSuccessfully = simulateDelivery(
            phoneNumber,
            deliveryMethod,
            message
        );

        if (!sentSuccessfully) {
            return "ERROR: The notification could not be delivered.";
        }

        return "SUCCESS: " + deliveryMethod +
               " notification processed for " + phoneNumber + ".";
    }

    private static boolean isValidDeliveryMethod(String method) {
        return method.equalsIgnoreCase("SMS")
                || method.equalsIgnoreCase("WHATSAPP");
    }

    private static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("[+]?[0-9() -]{7,20}");
    }

    private static void displayNotification(
            String phoneNumber,
            String deliveryMethod,
            String message
    ) {
        String time = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        System.out.println();
        System.out.println("INCOMING NOTIFICATION");
        System.out.println("----------------------------------------------");
        System.out.println("Received:  " + time);
        System.out.println("Recipient: " + phoneNumber);
        System.out.println("Method:    " + deliveryMethod);
        System.out.println();
        System.out.println("Message:");
        System.out.println(message);
        System.out.println("----------------------------------------------");
    }

    private static boolean simulateDelivery(
            String phoneNumber,
            String deliveryMethod,
            String message
    ) {
        System.out.println(
            "[SERVER] Simulating " + deliveryMethod +
            " delivery to " + phoneNumber + "..."
        );

        System.out.println(
            "[SERVER] Notification processed successfully."
        );

        return true;
    }
}