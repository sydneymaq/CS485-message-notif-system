//Starts JavaFX and creates GUI
// testing 
package client;

import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final TCPClient client = new TCPClient();

    public static void main(String[] args) {

        boolean running = true;

        printHeader();

        while (running) {
            printMenu();

            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    createNotification();
                    break;

                case "2":
                    showSystemInformation();
                    break;

                case "3":
                    running = false;
                    System.out.println("\nClosing the program...");
                    break;

                default:
                    System.out.println(
                        "\nInvalid selection. Please enter 1, 2, or 3."
                    );
            }
        }

        scanner.close();

        System.out.println("Program closed.");
    }
    ///GUI display
    private static void printHeader() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println("       MESSAGE NOTIFICATION SYSTEM");
        System.out.println("==============================================");
        System.out.println("Client address: 127.0.0.1");
        System.out.println("Server port: 65432");
    }
    // main menu options that come up after starting Client.main
    private static void printMenu() {
        System.out.println();
        System.out.println("----------------------------------------------");
        System.out.println("MAIN MENU");
        System.out.println("----------------------------------------------");
        System.out.println("1. Send a notification");
        System.out.println("2. View system information");
        System.out.println("3. Exit");
        System.out.println("----------------------------------------------");
    }

    private static void createNotification() {

        System.out.println();
        System.out.println("CREATE NOTIFICATION");
        System.out.println("----------------------------------------------");

        String phoneNumber = getPhoneNumber();
        String deliveryMethod = getDeliveryMethod();
        String message = getMessage();

        printConfirmation(
            phoneNumber,
            deliveryMethod,
            message
        );

        System.out.print("\nSend this notification? (Y/N): ");
        String confirmation = scanner.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("Y")) {
            System.out.println("Notification cancelled.");
            return;
        }

        System.out.println("\nConnecting to the server...");

        String response = client.sendMessage(
            phoneNumber,
            message,
            deliveryMethod
        );

        System.out.println();
        System.out.println("SERVER RESPONSE");
        System.out.println("----------------------------------------------");
        System.out.println(response);
        System.out.println("----------------------------------------------");
    }

    private static String getPhoneNumber() {

        while (true) {
            System.out.print(
                "Enter the recipient's phone number: "
            );

            String phoneNumber = scanner.nextLine().trim();

            /*
             * Allows numbers, spaces, parentheses, hyphens,
             * and an optional plus sign.
             */
            if (phoneNumber.matches("[+]?[0-9() -]{7,20}")) {
                return phoneNumber;
            }

            System.out.println(
                "Invalid phone number. Please enter at least " +
                "seven digits."
            );
        }
    }

    private static String getDeliveryMethod() {

        while (true) {
            System.out.println();
            System.out.println("Delivery method:");
            System.out.println("1. SMS");
            System.out.println("2. WhatsApp");
            System.out.print("Select a delivery method: ");

            String selection = scanner.nextLine().trim();

            if (selection.equals("1")) {
                return "SMS";
            }

            if (selection.equals("2")) {
                return "WHATSAPP";
            }

            System.out.println(
                "Invalid selection. Please enter 1 or 2."
            );
        }
    }

    private static String getMessage() {

        while (true) {
            System.out.println();
            System.out.println(
                "Enter the notification message."
            );
            System.out.println(
                "Press Enter on an empty line when finished:"
            );

            StringBuilder messageBuilder = new StringBuilder();

            while (true) {
                String line = scanner.nextLine();

                if (line.isBlank()) {
                    break;
                }

                if (messageBuilder.length() > 0) {
                    messageBuilder.append(System.lineSeparator());
                }

                messageBuilder.append(line);
            }

            String message = messageBuilder.toString().trim();

            if (!message.isEmpty()) {
                return message;
            }

            System.out.println(
                "The message cannot be empty."
            );
        }
    }

    private static void printConfirmation(
            String phoneNumber,
            String deliveryMethod,
            String message
    ) {
        System.out.println();
        System.out.println("NOTIFICATION SUMMARY");
        System.out.println("----------------------------------------------");
        System.out.println("Recipient: " + phoneNumber);
        System.out.println("Method:    " + deliveryMethod);
        System.out.println();
        System.out.println("Message:");
        System.out.println(message);
        System.out.println("----------------------------------------------");
    }

    private static void showSystemInformation() {
        System.out.println();
        System.out.println("SYSTEM INFORMATION");
        System.out.println("----------------------------------------------");
        System.out.println(
            "The CLI collects a phone number, delivery method, " +
            "and notification message."
        );
        System.out.println(
            "TCPClient sends the request to TCPServer."
        );
        System.out.println(
            "TCPServer validates and processes the request."
        );
        System.out.println(
            "Real phone delivery is currently simulated."
        );
        System.out.println("----------------------------------------------");
    }
}