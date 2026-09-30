package server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WhatsAppService {

    private final String accessToken;
    private final String phoneNumberId;
    private final String apiVersion;

    private final HttpClient httpClient;

    public WhatsAppService() {

        accessToken =
                System.getenv("WHATSAPP_ACCESS_TOKEN");

        phoneNumberId =
                System.getenv("WHATSAPP_PHONE_NUMBER_ID");

        apiVersion =
                System.getenv("WHATSAPP_API_VERSION");

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        validateConfiguration();
    }

    private void validateConfiguration() {

        if (accessToken == null ||
                accessToken.isBlank()) {

            throw new IllegalStateException(
                "WHATSAPP_ACCESS_TOKEN is missing."
            );
        }

        if (phoneNumberId == null ||
                phoneNumberId.isBlank()) {

            throw new IllegalStateException(
                "WHATSAPP_PHONE_NUMBER_ID is missing."
            );
        }

        if (apiVersion == null ||
                apiVersion.isBlank()) {

            throw new IllegalStateException(
                "WHATSAPP_API_VERSION is missing."
            );
        }
    }

    /*
     * Use this method first.
     *
     * It sends Meta's built-in hello_world template.
     * This is the safest way to test the API connection.
     */
    public DeliveryResult sendTestTemplate(
            String recipientPhoneNumber
    ) {

        String cleanPhoneNumber =
                cleanPhoneNumber(recipientPhoneNumber);

        String jsonBody = """
                {
                  "messaging_product": "whatsapp",
                  "recipient_type": "individual",
                  "to": "%s",
                  "type": "template",
                  "template": {
                    "name": "hello_world",
                    "language": {
                      "code": "en_US"
                    }
                  }
                }
                """.formatted(cleanPhoneNumber);

        return sendApiRequest(jsonBody);
    }

    /*
     * Use this method for custom messages.
     *
     * Meta may only allow custom text while the
     * recipient has an active conversation window.
     */
    public DeliveryResult sendMessage(
            String recipientPhoneNumber,
            String message
    ) {

        String cleanPhoneNumber =
                cleanPhoneNumber(recipientPhoneNumber);

        String jsonBody = """
                {
                  "messaging_product": "whatsapp",
                  "recipient_type": "individual",
                  "to": "%s",
                  "type": "text",
                  "text": {
                    "preview_url": false,
                    "body": "%s"
                  }
                }
                """.formatted(
                    escapeJson(cleanPhoneNumber),
                    escapeJson(message)
                );

        return sendApiRequest(jsonBody);
    }

    private DeliveryResult sendApiRequest(
            String jsonBody
    ) {

        try {
            String endpoint =
                    "https://graph.facebook.com/" +
                    apiVersion + "/" +
                    phoneNumberId +
                    "/messages";

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .timeout(
                                Duration.ofSeconds(30)
                            )
                            .header(
                                "Authorization",
                                "Bearer " + accessToken
                            )
                            .header(
                                "Content-Type",
                                "application/json"
                            )
                            .POST(
                                HttpRequest
                                    .BodyPublishers
                                    .ofString(jsonBody)
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
                    "WhatsApp accepted the message.",
                    statusCode,
                    responseBody
                );
            }

            return new DeliveryResult(
                false,
                "WhatsApp rejected the message.",
                statusCode,
                responseBody
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return new DeliveryResult(
                false,
                "The API request was interrupted.",
                0,
                e.getMessage()
            );

        } catch (Exception e) {

            return new DeliveryResult(
                false,
                "Could not contact WhatsApp: " +
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