package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public class TCPServer {

    private static final int PORT = 65432;

    public static void main(String[] args) {

        System.out.println(
            "========================================"
        );

        System.out.println(
            "     MESSAGE NOTIFICATION SERVER"
        );

        System.out.println(
            "========================================"
        );

        try (
            ServerSocket serverSocket =
                    new ServerSocket(PORT)
        ) {
            System.out.println(
                "[SERVER] Listening on port " +
                PORT + "..."
            );

            while (true) {

                Socket clientSocket =
                        serverSocket.accept();

                handleClient(clientSocket);
            }

        } catch (Exception e) {

            System.err.println(
                "[SERVER] Fatal error: " +
                e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private static void handleClient(
            Socket clientSocket
    ) {

        System.out.println();

        System.out.println(
            "[SERVER] Client connected: " +
            clientSocket
                .getRemoteSocketAddress()
        );

        try (
            Socket socket = clientSocket;

            BufferedReader input =
                    new BufferedReader(
                        new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8
                        )
                    );

            PrintWriter output =
                    new PrintWriter(
                        socket.getOutputStream(),
                        true
                    )
        ) {
            String request = input.readLine();

            String response =
                    processRequest(request);

            output.println(response);

        } catch (Exception e) {

            System.err.println(
                "[SERVER] Client error: " +
                e.getMessage()
            );
        }

        System.out.println(
            "[SERVER] Client disconnected."
        );

        System.out.println(
            "[SERVER] Waiting for another client..."
        );
    }

    private static String processRequest(
            String request
    ) {

        if (request == null ||
                request.isBlank()) {

            return "ERROR: Empty request received.";
        }

        String[] parts =
                request.split("\\|", 3);

        if (parts.length != 3) {
            return "ERROR: Invalid request format.";
        }

        String deliveryMethod =
                parts[0].trim();

        String phoneNumber =
                parts[1].trim();

        String encodedMessage =
                parts[2].trim();

        if (!deliveryMethod.equalsIgnoreCase(
                "WHATSAPP"
        )) {
            return "ERROR: Only WhatsApp is available.";
        }

        if (!phoneNumber.matches(
                "[+]?[0-9() -]{7,20}"
        )) {
            return "ERROR: Invalid phone number.";
        }

        String message;

        try {
            byte[] decodedBytes =
                    Base64.getDecoder()
                            .decode(
                                encodedMessage
                            );

            message = new String(
                decodedBytes,
                StandardCharsets.UTF_8
            ).trim();

        } catch (IllegalArgumentException e) {

            return "ERROR: Invalid message encoding.";
        }

        if (message.isBlank()) {
            return "ERROR: Message cannot be empty.";
        }

        if (message.length() > 4096) {
            return "ERROR: WhatsApp messages cannot " +
                   "exceed 4,096 characters.";
        }

        displayNotification(
            phoneNumber,
            deliveryMethod,
            message
        );

        return sendThroughVonage(
            phoneNumber,
            message
        );
    }

    private static void displayNotification(
            String phoneNumber,
            String deliveryMethod,
            String message
    ) {

        System.out.println();

        System.out.println(
            "INCOMING NOTIFICATION"
        );

        System.out.println(
            "----------------------------------------"
        );

        System.out.println(
            "Received: " +
            LocalDateTime.now()
        );

        System.out.println(
            "Recipient: " +
            phoneNumber
        );

        System.out.println(
            "Method: " +
            deliveryMethod
        );

        System.out.println();

        System.out.println("Message:");

        System.out.println(message);

        System.out.println(
            "----------------------------------------"
        );
    }

    private static String sendThroughVonage(
            String phoneNumber,
            String message
    ) {

        try {
            VonageService service =
                    new VonageService();

            VonageService.DeliveryResult result =
                    service.sendMessage(
                        phoneNumber,
                        message
                    );

            System.out.println();

            System.out.println(
                "[VONAGE] HTTP status: " +
                result.getStatusCode()
            );

            System.out.println(
                "[VONAGE] Result: " +
                result.getMessage()
            );

            System.out.println(
                "[VONAGE] Provider response: " +
                result.getProviderResponse()
            );

            if (result.isSuccessful()) {

                return "SUCCESS: WhatsApp message " +
                       "accepted for " +
                       phoneNumber + ".";
            }

            return "ERROR: " +
                    result.getMessage() +
                    " HTTP status " +
                    result.getStatusCode() +
                    ".";

        } catch (IllegalStateException e) {

            System.err.println(
                "[VONAGE] Configuration error: " +
                e.getMessage()
            );

            return "ERROR: Server configuration " +
                   "is incomplete. " +
                   e.getMessage();
        }
    }
}