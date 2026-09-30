package server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class VonageService {

    private final String apiKey;
    private final String apiSecret;
    private final String sandboxNumber;
    private final String sandboxUrl;

    private final HttpClient httpClient;

    public VonageService() {

        apiKey = System.getenv("VONAGE_API_KEY");

        apiSecret =
                System.getenv("VONAGE_API_SECRET");

        sandboxNumber =
                System.getenv("VONAGE_SANDBOX_NUMBER");

        sandboxUrl =
                System.getenv("VONAGE_SANDBOX_URL");

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        validateConfiguration();
    }

    private void validateConfiguration() {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "VONAGE_API_KEY is missing."
            );
        }

        if (apiSecret == null ||
                apiSecret.isBlank()) {

            throw new IllegalStateException(
                "VONAGE_API_SECRET is missing."
            );
        }

        if (sandboxNumber == null ||
                sandboxNumber.isBlank()) {

            throw new IllegalStateException(
                "VONAGE_SANDBOX_NUMBER is missing."
            );
        }

        if (sandboxUrl == null ||
                sandboxUrl.isBlank()) {

            throw new IllegalStateException(
                "VONAGE_SANDBOX_URL is missing."
            );
        }
    }

    public DeliveryResult sendMessage(
            String recipientPhoneNumber,
            String message
    ) {

        try {
            String cleanRecipient =
                    cleanPhoneNumber(
                        recipientPhoneNumber
                    );

            String cleanSender =
                    cleanPhoneNumber(
                        sandboxNumber
                    );

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

            String credentials =
                    apiKey + ":" + apiSecret;

            String basicAuthentication =
                    Base64.getEncoder()
                            .encodeToString(
                                credentials.getBytes(
                                    StandardCharsets.UTF_8
                                )
                            );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                URI.create(sandboxUrl)
                            )
                            .timeout(
                                Duration.ofSeconds(30)
                            )
                            .header(
                                "Authorization",
                                "Basic " +
                                basicAuthentication
                            )
                            .header(
                                "Content-Type",
                                "application/json"
                            )
                            .header(
                                "Accept",
                                "application/json"
                            )
                            .POST(
                                HttpRequest
                                    .BodyPublishers
                                    .ofString(
                                        jsonBody,
                                        StandardCharsets.UTF_8
                                    )
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                        request,
                        HttpResponse
                            .BodyHandlers
                            .ofString()
                    );

            int statusCode =
                    response.statusCode();

            String responseBody =
                    response.body();

            if (statusCode >= 200 &&
                    statusCode < 300) {

                return new DeliveryResult(
                    true,
                    "Vonage accepted the WhatsApp message.",
                    statusCode,
                    responseBody
                );
            }

            return new DeliveryResult(
                false,
                "Vonage rejected the WhatsApp message.",
                statusCode,
                responseBody
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return new DeliveryResult(
                false,
                "The Vonage request was interrupted.",
                0,
                e.getMessage()
            );

        } catch (Exception e) {

            return new DeliveryResult(
                false,
                "Could not contact Vonage: " +
                e.getMessage(),
                0,
                e.toString()
            );
        }
    }

    private String cleanPhoneNumber(
            String phoneNumber
    ) {

        /*
         * Converts:
         * +1 671 555 1234
         *
         * Into:
         * 16715551234
         */
        return phoneNumber.replaceAll(
            "[^0-9]",
            ""
        );
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    public static class DeliveryResult {

        private final boolean successful;
        private final String message;
        private final int statusCode;
        private final String providerResponse;

        public DeliveryResult(
                boolean successful,
                String message,
                int statusCode,
                String providerResponse
        ) {
            this.successful = successful;
            this.message = message;
            this.statusCode = statusCode;
            this.providerResponse =
                    providerResponse;
        }

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