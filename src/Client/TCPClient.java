//connects GUI to server

package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPClient {
    public static void main(String[] args) {
        String host = "127.0.0.1"; // Localhost
        int port = 65432;

        System.out.println("[CLIENT] Connecting to server at " + host + ":" + port + "...");

        // 1. Establish connection to the server
        try (Socket socket = new Socket(host, port)) {
            
            // 2. Set up input and output streams
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 3. Prepare and send a message (must include a newline character for readLine())
            String message = "Hello, Java Server! This is the Client.";
            output.println(message);
            System.out.println("[CLIENT] Sent message: " + message);

            // 4. Read the echo response from the server
            String response = input.readLine();
            System.out.println("[CLIENT] Received echo back: " + response);

        } catch (Exception e) {
            System.err.println("[CLIENT] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
