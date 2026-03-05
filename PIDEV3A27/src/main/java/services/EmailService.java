package services;

import utils.ApiConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Envoi email via Mailjet Send API v3.1.
 *
 * Auth: Basic Auth (API Key : Secret Key).
 * Endpoint: POST https://api.mailjet.com/v3.1/send
 */
public class EmailService {

    private static final URI SEND_URI = URI.create("https://api.mailjet.com/v3.1/send");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void sendEmailToUser(String toEmail, String subject, String bodyText) throws Exception {
        String apiKey = ApiConfig.mailjetApiKey();
        String secret = ApiConfig.mailjetSecretKey();
        if (isBlank(apiKey) || isBlank(secret)) {
            throw new IllegalStateException("MAILJET_API_KEY / MAILJET_SECRET_KEY manquantes.");
        }

        if (isBlank(toEmail)) {
            throw new IllegalArgumentException("Email destinataire vide.");
        }

        String fromEmail = ApiConfig.mailjetSenderEmail();
        String fromName = ApiConfig.mailjetSenderName();
        if (isBlank(fromEmail)) {
            throw new IllegalStateException("MAILJET_SENDER_EMAIL manquant.");
        }

        String payload = buildPayload(fromEmail, fromName, toEmail, subject, bodyText);

        String basic = Base64.getEncoder().encodeToString((apiKey + ":" + secret).getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(SEND_URI)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + basic)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("Mailjet error " + res.statusCode() + ": " + res.body());
        }
    }

    private static String buildPayload(String fromEmail, String fromName, String toEmail, String subject, String bodyText) {
        return "{" +
                "\"Messages\":[{" +
                "\"From\":{\"Email\":" + json(fromEmail) + ",\"Name\":" + json(nullToEmpty(fromName)) + "}," +
                "\"To\":[{\"Email\":" + json(toEmail) + "}]," +
                "\"Subject\":" + json(nullToEmpty(subject)) + "," +
                "\"TextPart\":" + json(nullToEmpty(bodyText)) +
                "}]" +
                "}";
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String json(String s) {
        if (s == null) return "\"\"";
        String esc = s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
        return "\"" + esc + "\"";
    }
}
