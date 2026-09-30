package server;

// Java's built-in HTTP client classes are used to contact the Vonage API.
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Handles the external Vonage Messages Sandbox connection.
 *
 * TCPServer supplies a recipient and message. This class cleans the values,
 * creates the JSON payload, authenticates the request, submits it over HTTPS,
 * and returns a DeliveryResult that TCPServer can interpret.
 */
public class VonageService {

    // Values are loaded from environment variables instead of hard-coded.
    private final String apiKey;
    private final String apiSecret;
    private final String sandboxNumber;
    private final String sandboxUrl;

    // Reusable client for outbound HTTPS requests.
    private final HttpClient httpClient;

    /** Loads configuration and creates the HTTP client. */
    public VonageService() {

        // Public account identifier used for Basic Authentication.
        apiKey = System.getenv("VONAGE_API_KEY");

        // Private secret paired with the API key.
        apiSecret = System.getenv("VONAGE_API_SECRET");

        // WhatsApp sender number assigned by the Vonage sandbox.
        sandboxNumber = System.getenv("VONAGE_SANDBOX_NUMBER");

        // Sandbox Messages API endpoint supplied by Vonage.
        sandboxUrl = System.getenv("VONAGE_SANDBOX_URL");

        // Stop connection attempts that cannot be established within 15 seconds.
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        // Fail early with a clear message when configuration is incomplete.
        validateConfiguration();
    }

    /** Ensures every required environment variable is available. */
    private void validateConfiguration() {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("VONAGE_API_KEY is missing.");
        }

        if (apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("VONAGE_API_SECRET is missing.");
        }

        if (sandboxNumber == null || sandboxNumber.isBlank()) {
            throw new IllegalStateException(
                "VONAGE_SANDBOX_NUMBER is missing."
            );
        }

        if (sandboxUrl == null || sandboxUrl.isBlank()) {
            throw new IllegalStateException("VONAGE_SANDBOX_URL is missing.");
        }
    }

    /** Sends one WhatsApp text message through the Vonage sandbox. */
    public DeliveryResult sendMessage(
            String recipientPhoneNumber,
            String message
    ) {

        try {
            // Vonage expects phone numbers as digits without display formatting.
            String cleanRecipient = cleanPhoneNumber(recipientPhoneNumber);
            String cleanSender = cleanPhoneNumber(sandboxNumber);

            /*
             * Build the JSON object required by the Messages API.
             * escapeJson protects quotes, slashes, tabs, and line breaks that
             * may appear in the user's notification.
             */
            String jsonBody = """
                    {
                      "to": "%s",
                      "from": "%s",
                      "channel": "whatsapp",
                      "message_type": "text",
                      "text": "%s"
                    }
                    """.formatted(
                        escapeJson(cleanRecipient),
                        escapeJson(cleanSender),
                        escapeJson(message)
                    );

            // HTTP Basic Authentication begins with "API_KEY:API_SECRET".
            String credentials = apiKey + ":" + apiSecret;

            // Encode the credential pair as required by the Authorization header.
            String basicAuthentication = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
            );

            /*
             * Construct an authenticated HTTPS POST request containing the
             * JSON message. The API response must also be returned as JSON.
             */
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(sandboxUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header(
                        "Authorization",
                        "Basic " + basicAuthentication
                    )
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            jsonBody,
                            StandardCharsets.UTF_8
                        )
                    )
                    .build();

            // Send the request synchronously and collect the response body.
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            // Store the values needed by TCPServer for logging and feedback.
            int statusCode = response.statusCode();
            String responseBody = response.body();

            // Any status from 200 through 299 represents API acceptance.
            if (statusCode >= 200 && statusCode < 300) {
                return new DeliveryResult(
                    true,
                    "Vonage accepted the WhatsApp message.",
                    statusCode,
                    responseBody
                );
            }

            // Preserve Vonage's response when the provider rejects the request.
            return new DeliveryResult(
                false,
                "Vonage rejected the WhatsApp message.",
                statusCode,
                responseBody
            );

        } catch (InterruptedException e) {
            // Restore the interrupt flag instead of silently discarding it.
            Thread.currentThread().interrupt();

            return new DeliveryResult(
                false,
                "The Vonage request was interrupted.",
                0, // No HTTP response was received.
                e.getMessage()
            );

        } catch (Exception e) {
            // Handle network, URI, encoding, or other unexpected problems.
            return new DeliveryResult(
                false,
                "Could not contact Vonage: " + e.getMessage(),
                0, // A zero status indicates failure before an HTTP response.
                e.toString()
            );
        }
    }

    /** Removes all characters except digits from a phone number. */
    private String cleanPhoneNumber(String phoneNumber) {
        // Example: "+1 671 555 1234" becomes "16715551234".
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    /** Escapes user text before inserting it into a JSON string value. */
    private String escapeJson(String value) {

        // Convert null into an empty string to avoid a NullPointerException.
        if (value == null) {
            return "";
        }

        // Escape characters that have special meaning inside JSON strings.
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    /** Immutable object containing the result of one delivery attempt. */
    public static class DeliveryResult {

        // True when Vonage returned a 2xx HTTP status.
        private final boolean successful;

        // Human-readable explanation for TCPServer and the JavaFX interface.
        private final String message;

        // HTTP status returned by Vonage, or zero before a response was received.
        private final int statusCode;

        // Original provider response, retained for logging and debugging.
        private final String providerResponse;

        /** Initializes every value in the delivery result. */
        public DeliveryResult(
                boolean successful,
                String message,
                int statusCode,
                String providerResponse
        ) {
            this.successful = successful;
            this.message = message;
            this.statusCode = statusCode;
            this.providerResponse = providerResponse;
        }

        // Getter methods allow TCPServer to read the private result fields.
        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getProviderResponse() {
            return providerResponse;
        }
    }
}
