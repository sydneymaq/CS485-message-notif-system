package client;

// Java and networking classes used for the TCP connection.
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Sends one notification request from the JavaFX application to TCPServer.
 */
public class TCPClient {

    // The server runs on the same computer as the JavaFX application.
    private static final String HOST = "127.0.0.1";

    // TCPClient and TCPServer must use the same port number.
    private static final int PORT = 65432;

    // Stop a connection attempt after five seconds.
    private static final int CONNECTION_TIMEOUT = 5000;

    // Wait up to thirty seconds for the server and API response.
    private static final int READ_TIMEOUT = 30000;

    /**
     * Formats and sends a notification request, then returns the server reply.
     */
    public String sendMessage(
            String phoneNumber,
            String message,
            String deliveryMethod
    ) {
        // try-with-resources automatically closes the socket after this request.
        try (Socket socket = new Socket()) {

            // Connect to the configured host and port using the timeout limit.
            socket.connect(
                new InetSocketAddress(HOST, PORT),
                CONNECTION_TIMEOUT
            );

            // Apply a timeout while waiting for the server's response.
            socket.setSoTimeout(READ_TIMEOUT);

            try (
                // PrintWriter sends the formatted request to the server.
                PrintWriter output = new PrintWriter(
                    socket.getOutputStream(),
                    true // Automatically flush each println call.
                );

                // BufferedReader receives one text response from the server.
                BufferedReader input = new BufferedReader(
                    new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.UTF_8
                    )
                )
            ) {
                /*
                 * Base64 converts the message into a safe single-line value.
                 * This prevents line breaks and pipe-like special content from
                 * interfering with the custom TCP request format.
                 */
                String encodedMessage = Base64.getEncoder().encodeToString(
                    message.getBytes(StandardCharsets.UTF_8)
                );

                /*
                 * Custom request protocol:
                 * DELIVERY_METHOD|PHONE_NUMBER|BASE64_MESSAGE
                 */
                String request =
                        deliveryMethod + "|" +
                        phoneNumber + "|" +
                        encodedMessage;

                // Send the complete request as one line.
                output.println(request);

                // Block until TCPServer sends back success or error text.
                String response = input.readLine();

                // Protect the GUI from receiving a null value.
                if (response == null) {
                    return "ERROR: The server returned no response.";
                }

                return response;
            }

        } catch (java.net.ConnectException e) {
            // This normally occurs when TCPServer has not been started.
            return "ERROR: The server is not running.";

        } catch (java.net.SocketTimeoutException e) {
            // The server or Vonage request exceeded the allowed wait time.
            return "ERROR: The server response timed out.";

        } catch (Exception e) {
            // Return any other socket or stream error to the interface.
            return "ERROR: " + e.getMessage();
        }
    }
}
