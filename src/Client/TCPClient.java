package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TCPClient {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 65432;

    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 30000;

    public String sendMessage(
            String phoneNumber,
            String message,
            String deliveryMethod
    ) {
        try (Socket socket = new Socket()) {

            socket.connect(
                new InetSocketAddress(HOST, PORT),
                CONNECTION_TIMEOUT
            );

            socket.setSoTimeout(READ_TIMEOUT);

            try (
                PrintWriter output =
                        new PrintWriter(
                            socket.getOutputStream(),
                            true
                        );

                BufferedReader input =
                        new BufferedReader(
                            new InputStreamReader(
                                socket.getInputStream(),
                                StandardCharsets.UTF_8
                            )
                        )
            ) {
                String encodedMessage =
                        Base64.getEncoder()
                                .encodeToString(
                                    message.getBytes(
                                        StandardCharsets.UTF_8
                                    )
                                );

                String request =
                        deliveryMethod + "|" +
                        phoneNumber + "|" +
                        encodedMessage;

                output.println(request);

                String response = input.readLine();

                if (response == null) {
                    return "ERROR: The server returned no response.";
                }

                return response;
            }

        } catch (java.net.ConnectException e) {
            return "ERROR: The server is not running.";

        } catch (java.net.SocketTimeoutException e) {
            return "ERROR: The server response timed out.";

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}