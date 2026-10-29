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
 * Handles the external Vonage Messages API connection for both delivery
 * channels: the WhatsApp sandbox and SMS.
 *
 * TCPServer supplies a recipient and message. This class cleans the values,
 * creates the JSON payload, authenticates the request, submits it over HTTPS,
 * and returns a DeliveryResult that TCPServer can interpret.
 */
public class VonageService {

    // Production Messages API endpoint, used for SMS unless overridden.
    private static final String DEFAULT_SMS_URL =
            "https://api.nexmo.com/v1/messages";

    // Default SMS sender name (same value as the Vonage dashboard example).
    private static final String DEFAULT_SMS_SENDER = "Vonage APIs";

    // Which channel this instance sends on: "whatsapp" or "sms".
    private final String channel;

    // Values are loaded from environment variables instead of hard-coded.
    private final String apiKey;
    private final String apiSecret;

    // "From" value: the sandbox number (WhatsApp) or SMS sender ID / number.
    private final String sender;

    // Messages API endpoint for the chosen channel.
    private final String apiUrl;

    // Reusable client for outbound HTTPS requests.
    private final HttpClient httpClient;

    /**
     * Loads configuration for the requested delivery method ("SMS" or
     * "WHATSAPP") and creates the HTTP client.
     */
    public VonageService(String deliveryMethod) {

        // Public account identifier used for Basic Authentication.
        apiKey = System.getenv("VONAGE_API_KEY");

        // Private secret paired with the API key.
        apiSecret = System.getenv("VONAGE_API_SECRET");

        if ("SMS".equalsIgnoreCase(deliveryMethod)) {
            channel = "sms";

            // Optional overrides; sensible defaults are used when unset.
            apiUrl = envOrDefault("VONAGE_SMS_URL", DEFAULT_SMS_URL);
            sender = envOrDefault("VONAGE_SMS_FROM", DEFAULT_SMS_SENDER);

        } else {
            channel = "whatsapp";

            // Sandbox Messages API endpoint supplied by Vonage.
            apiUrl = System.getenv("VONAGE_SANDBOX_URL");

            // WhatsApp sender number assigned by the Vonage sandbox.
            sender = System.getenv("VONAGE_SANDBOX_NUMBER");
        }

        // Stop connection attempts that cannot be established within 15 seconds.
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        // Fail early with a clear message when configuration is incomplete.
        validateConfiguration();
    }

    /** Returns an environment variable, or a fallback when it is unset/blank. */
    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    /** Ensures every required environment variable is available. */
    private void validateConfiguration() {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("VONAGE_API_KEY is missing.");
        }

        if (apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("VONAGE_API_SECRET is missing.");
        }

        // Only the WhatsApp sandbox needs these two variables.
        if (channel.equals("whatsapp")) {
            if (sender == null || sender.isBlank()) {
                throw new IllegalStateException(
                    "VONAGE_SANDBOX_NUMBER is missing."
                );
            }

            if (apiUrl == null || apiUrl.isBlank()) {
                throw new IllegalStateException(
                    "VONAGE_SANDBOX_URL is missing."
                );
            }
        }
    }

    /** Readable channel name used in result messages. */
    private String channelLabel() {
        return channel.equals("sms") ? "SMS" : "WhatsApp";
    }

    /** Sends one text message on this service's channel (SMS or WhatsApp). */
    public DeliveryResult sendMessage(
            String recipientPhoneNumber,
            String message
    ) {

        try {
            // Vonage expects phone numbers as digits without display formatting.
            String cleanRecipient = normalizeRecipient(recipientPhoneNumber);
            String cleanSender = formatSender();

            /*
             * Build the JSON object required by the Messages API. The same
             * shape is used for SMS and WhatsApp; only "channel" differs.
             * escapeJson protects quotes, slashes, tabs, and line breaks that
             * may appear in the user's notification.
             */
            String jsonBody = """
                    {
                      "to": "%s",
                      "from": "%s",
                      "channel": "%s",
                      "message_type": "text",
                      "text": "%s"
                    }
                    """.formatted(
                        escapeJson(cleanRecipient),
                        escapeJson(cleanSender),
                        channel,
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
                    .uri(URI.create(apiUrl))
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
                    "Vonage accepted the " + channelLabel() + " message.",
                    statusCode,
                    responseBody
                );
            }

            // Preserve Vonage's response when the provider rejects the request.
            return new DeliveryResult(
                false,
                "Vonage rejected the " + channelLabel() + " message.",
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

    /**
     * Formats the "from" value. Numbers are reduced to digits; an
     * alphanumeric SMS sender ID such as "Vonage APIs" is sent unchanged.
     */
    private String formatSender() {
        if (sender.matches("[+0-9 ()-]+")) {
            return cleanPhoneNumber(sender);
        }
        return sender;
    }

    /**
     * Cleans a recipient number and adds the US/Guam country code (1) when
     * only 10 digits were given, e.g. "671 489 1960" -> "16714891960".
     */
    private String normalizeRecipient(String phoneNumber) {
        String digits = cleanPhoneNumber(phoneNumber);
        return digits.length() == 10 ? "1" + digits : digits;
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