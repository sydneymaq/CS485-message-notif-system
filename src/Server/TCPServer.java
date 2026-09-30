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
            "[SERVER] Starting notification server..."
        );

        try (
            ServerSocket serverSocket =
                    new ServerSocket(PORT)
        ) {
            System.out.println(
                "[SERVER] Listening on port " + PORT
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
        System.out.println(
            "[SERVER] Client connected: " +
            clientSocket.getRemoteSocketAddress()
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
    }

    private static String processRequest(
            String request
    ) {
        if (request == null || request.isBlank()) {
            return "ERROR: Empty request received.";
        }

        String[] parts = request.split("\\|", 3);

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
            return "ERROR: Only WhatsApp is currently available.";
        }

        if (!phoneNumber.matches(
                "[+]?[0-9() -]{7,20}"
        )) {
            return "ERROR: Invalid phone number.";
        }

        String message;

        try {
            byte[] decodedMessage =
                    Base64.getDecoder()
                            .decode(encodedMessage);

            message = new String(
                decodedMessage,
                StandardCharsets.UTF_8
            ).trim();

        } catch (IllegalArgumentException e) {
            return "ERROR: Invalid message encoding.";
        }

        if (message.isBlank()) {
            return "ERROR: Message cannot be empty.";
        }

        System.out.println();
        System.out.println("NEW NOTIFICATION");
        System.out.println("--------------------------------");
        System.out.println(
            "Time: " + LocalDateTime.now()
        );
        System.out.println(
            "Method: " + deliveryMethod
        );
        System.out.println(
            "Recipient: " + phoneNumber
        );
        System.out.println("Message:");
        System.out.println(message);
        System.out.println("--------------------------------");

        return sendThroughWhatsApp(
            phoneNumber,
            message
        );
    }

    private static String sendThroughWhatsApp(
            String phoneNumber,
            String message
    ) {
        try {
            WhatsAppService service =
                    new WhatsAppService();

           /* WhatsAppService.DeliveryResult result =
                    service.sendMessage(
                        phoneNumber,
                        message
                    );
            */
           WhatsAppService.DeliveryResult result =
                 service.sendTestTemplate(
                     phoneNumber
                );
            System.out.println(
                "[WHATSAPP] Status code: " +
                result.getStatusCode()
            );

            System.out.println(
                "[WHATSAPP] Provider response: " +
                result.getProviderResponse()
            );

            if (result.isSuccessful()) {
                return "SUCCESS: WhatsApp message accepted for "
                        + phoneNumber + ".";
            }

            return "ERROR: " + result.getMessage() + " Provider response: "+ result.getProviderResponse();

        } catch (IllegalStateException e) {
            return "ERROR: Server API configuration is missing. "
                    + e.getMessage();
        }
    }
}