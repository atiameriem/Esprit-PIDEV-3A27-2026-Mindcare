package services;

import okhttp3.*;
import org.json.*;
import java.util.List;
import java.util.Map;

/**
 * GroqService — Service unifié pour appeler l'API Groq
 * Utilisé par : AIInterviewController, WordGeneratorService
 */
public class GroqServiceF {

    private static final String API_KEY = "";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Modèles disponibles
    public static final String MODEL_PUISSANT = "llama-3.3-70b-versatile"; // Génération cours
    public static final String MODEL_RAPIDE = "llama-3.1-8b-instant"; // Traduction rapide
    public static final String MODEL_LONG = "mixtral-8x7b-32768"; // Longs documents

    private static final OkHttpClient client = new OkHttpClient();

    // ─────────────────────────────────────────────
    // 1. Appel simple (prompt → réponse)
    // ─────────────────────────────────────────────
    public static String appeler(String prompt) {
        return appeler(null, prompt, MODEL_PUISSANT);
    }

    public static String appeler(String systemPrompt, String userPrompt) {
        return appeler(systemPrompt, userPrompt, MODEL_PUISSANT);
    }

    public static String appeler(String systemPrompt, String userPrompt, String model) {
        try {
            JSONArray messages = new JSONArray();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.put(new JSONObject()
                        .put("role", "system")
                        .put("content", systemPrompt));
            }

            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", userPrompt));

            return envoyerRequete(messages, model);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ─────────────────────────────────────────────
    // 2. Appel avec historique complet (chat interview)
    // ─────────────────────────────────────────────
    public static String appelerAvecHistorique(
            List<Map<String, String>> historique,
            String model,
            String systemPrompt) {
        try {
            JSONArray messages = new JSONArray();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.put(new JSONObject()
                        .put("role", "system")
                        .put("content", systemPrompt));
            }

            for (Map<String, String> msg : historique) {
                messages.put(new JSONObject()
                        .put("role", msg.get("role"))
                        .put("content", msg.get("content")));
            }

            return envoyerRequete(messages, model);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ─────────────────────────────────────────────
    // Méthode interne — Envoi HTTP avec OkHttp
    // ─────────────────────────────────────────────
    private static String envoyerRequete(JSONArray messages, String model) throws Exception {
        JSONObject bodyJson = new JSONObject()
                .put("model", model != null ? model : MODEL_PUISSANT)
                .put("messages", messages)
                .put("max_tokens", 2000)
                .put("temperature", 0.7);

        RequestBody body = RequestBody.create(
                bodyJson.toString(),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);

            if (json.has("error")) {
                System.err.println("❌ Groq API Error: " +
                        json.getJSONObject("error").getString("message"));
                return null;
            }

            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        }
    }
}
