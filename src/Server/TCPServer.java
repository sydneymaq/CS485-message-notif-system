// receives message requests

package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
    public static void main(String[] args) {
        int port = 65432;

        // 1. Create a ServerSocket listening on the specified port
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVER] Listening on port " + port + "...");

            // 2. Accept an incoming client connection (blocks until a client connects)
            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("[SERVER] Connected to client: " + clientSocket.getRemoteSocketAddress());

                // 3. Set up input and output streams for communication
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true); // true enables auto-flush

                String clientMessage;
                // 4. Read incoming messages line-by-line
                while ((clientMessage = input.readLine()) != null) {
                    System.out.println("[SERVER] Received from client: " + clientMessage);
                    
                    // 5. Echo the message back to the client
                    output.println(clientMessage);
                }
            }
            System.out.println("[SERVER] Client disconnected. Shutting down.");
            
        } catch (Exception e) {
            System.err.println("[SERVER] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
