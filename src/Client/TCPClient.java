package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPClient {

    private String host = "127.0.0.1";
    private int port = 65432;

    public String sendMessage(String message) {

        try (
            Socket socket = new Socket(host, port);
            PrintWriter output =
                new PrintWriter(socket.getOutputStream(), true);
            BufferedReader input =
                new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
                )
        ) {

            output.println(message);

            return input.readLine();

        } catch (Exception e) {

            return "ERROR: " + e.getMessage();
        }
    }
}