package services;

import models.PostDraft;
import utils.ApiConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Chatbot via Groq (OpenAI-compatible).
 * Endpoint: https://api.groq.com/openai/v1/chat/completions
 *
 * Docs Groq: base URL https://api.groq.com/openai/v1 :contentReference[oaicite:0]{index=0}
 */
public class HuggingFaceChatService {

    private static final URI CHAT_URI = URI.create("https://api.groq.com/openai/v1/chat/completions");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public PostDraft draftPost(String discussion) throws Exception {
        if (discussion == null) discussion = "";

        String apiKey = ApiConfig.groqApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY manquant. Ajoute-le dans ApiConfig.");
        }

        String model = ApiConfig.groqModel();
        if (model == null || model.isBlank()) model = "llama3-8b-8192"; // modèle Groq classique :contentReference[oaicite:1]{index=1}

        String system = "Tu es un assistant pour une application de santé mentale. "
                + "Aide l'utilisateur à rédiger un post clair, respectueux et non médical. "
                + "Retourne STRICTEMENT ce format:\n"
                + "TITLE: <titre court>\n"
                + "CONTENT: <contenu structuré en 1-2 paragraphes>";

        String user = "Discussion de l'utilisateur:\n" + discussion;

        String payload = "{"
                + "\"model\":" + json(model) + ","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":" + json(system) + "},"
                + "{\"role\":\"user\",\"content\":" + json(user) + "}"
                + "],"
                + "\"temperature\":0.7"
                + "}";

        String content = callChat(apiKey.trim(), payload);
        if (content == null || content.isBlank()) {
            return new PostDraft("Mon post", discussion);
        }

        String title = extractLineValue(content, "TITLE:");
        String body = extractLineValue(content, "CONTENT:");
        if (title == null || title.isBlank()) title = "Mon post";
        if (body == null || body.isBlank()) body = content;

        return new PostDraft(title.trim(), body.trim());
    }

    public String chatReply(String conversationHistorique, String userMessage) throws Exception {
        if (conversationHistorique == null) conversationHistorique = "";
        if (userMessage == null) userMessage = "";

        String apiKey = ApiConfig.groqApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY manquant. Ajoute-le dans ApiConfig.");
        }

        String model = ApiConfig.groqModel();
        if (model == null || model.isBlank()) model = "llama3-8b-8192";

        String system = "Tu es un chatbot bienveillant pour une application de santé mentale. "
                + "Réponds en français, de façon courte, claire et respectueuse. "
                + "Ne donne pas de conseils médicaux. Pose des questions et reformule.";

        String user = "Historique:\n" + conversationHistorique + "\n\n"
                + "Utilisateur: " + userMessage;

        String payload = "{"
                + "\"model\":" + json(model) + ","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":" + json(system) + "},"
                + "{\"role\":\"user\",\"content\":" + json(user) + "}"
                + "],"
                + "\"temperature\":0.7"
                + "}";

        String content = callChat(apiKey.trim(), payload);
        return content == null ? "" : content.strip();
    }

    private String callChat(String apiKey, String payload) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(CHAT_URI)
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("Groq error " + res.statusCode() + ": " + res.body());
        }
        return extractChoiceContent(res.body());
    }

    // ---- parsing minimal OpenAI format: choices[0].message.content ----

    private static String extractChoiceContent(String json) {
        if (json == null) return null;

        int choices = json.indexOf("\"choices\"");
        if (choices < 0) return null;

        int msg = json.indexOf("\"message\"", choices);
        if (msg < 0) return null;

        int contentKey = json.indexOf("\"content\"", msg);
        if (contentKey < 0) return null;

        int colon = json.indexOf(':', contentKey);
        if (colon < 0) return null;

        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;

        int q2 = q1 + 1;
        while (q2 < json.length()) {
            char c = json.charAt(q2);
            if (c == '"' && json.charAt(q2 - 1) != '\\') break;
            q2++;
        }
        if (q2 >= json.length()) return null;

        String raw = json.substring(q1 + 1, q2);
        return unescapeJson(raw);
    }

    private static String extractLineValue(String text, String prefix) {
        if (text == null) return null;
        int i = text.indexOf(prefix);
        if (i < 0) return null;
        int start = i + prefix.length();
        int end = text.indexOf('\n', start);
        String line = (end < 0) ? text.substring(start) : text.substring(start, end);
        return line.trim();
    }

    private static String json(String s) {
        if (s == null) return "\"\"";
        String esc = s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + esc + "\"";
    }

    private static String unescapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}