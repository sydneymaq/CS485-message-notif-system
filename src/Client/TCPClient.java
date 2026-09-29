package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TCPClient {

    private final String host;
    private final int port;

    public TCPClient() {
        this.host = "127.0.0.1";
        this.port = 65432;
    }

    public TCPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String sendMessage(
            String phoneNumber,
            String message,
            String deliveryMethod
    ) {
        try (
            Socket socket = new Socket(host, port);
            PrintWriter output =
                    new PrintWriter(socket.getOutputStream(), true);
            BufferedReader input =
                    new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                    )
        ) {
            /*
             * Encode the message so special characters, spaces,
             * and line breaks do not interfere with the separator.
             */
            String encodedMessage = Base64.getEncoder().encodeToString(
                message.getBytes(StandardCharsets.UTF_8)
            );

            /*
             * Request format:
             * METHOD|PHONE_NUMBER|BASE64_MESSAGE
             */
            String request =
                    deliveryMethod + "|" +
                    phoneNumber + "|" +
                    encodedMessage;

            output.println(request);

            // Wait for the server's response.
            String response = input.readLine();

            if (response == null) {
                return "ERROR: The server closed the connection.";
            }

            return response;

        } catch (Exception e) {
            return "ERROR: Could not connect to the server. "
                    + e.getMessage();
        }
    }
}