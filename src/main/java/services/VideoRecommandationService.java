package services;

import com.google.gson.JsonArray;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class VideoRecommandationService {

    private static final String COHERE_API_KEY = "HIDDEN_KEY";

    public static String getMotsCles(String diagnostic, String notes) throws Exception {
        OkHttpClient client = new OkHttpClient();

        String prompt = String.format(
                "Patient avec : %s\nNotes : %s\n\nDonne UNIQUEMENT 2-3 mots-clés de recherche YouTube en français pour cet état (méditation ou relaxation). Réponds uniquement avec les mots-clés, pas de ponctuation superflue.",
                diagnostic, notes);

        JSONObject json = new JSONObject();
        json.put("model", "command"); // Utilisation du modèle standard car command-r est obsolète
        json.put("message", prompt);
        json.put("temperature", 0.3);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url("https://api.cohere.com/v1/chat")
                .header("Authorization", "Bearer " + COHERE_API_KEY)
                .header("Accept", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                // On essaie d'extraire le message d'erreur du JSON de Cohere
                String errorMsg = response.message();
                try {
                    JSONObject errObj = new JSONObject(responseBody);
                    if (errObj.has("message"))
                        errorMsg = errObj.getString("message");
                } catch (Exception e) {
                    /* fallback au message par défaut */ }

                throw new IOException("Erreur Cohere " + response.code() + ": " + errorMsg);
            }

            JSONObject obj = new JSONObject(responseBody);
            // Dans l'API Chat v1, la réponse est dans le champ "text"
            if (obj.has("text")) {
                return obj.getString("text").trim();
            } else {
                throw new IOException("Format de réponse Cohere inconnu : " + responseBody);
            }
        }
    }

    public static JsonArray getVideosPersonnalisees(String diagnostic, String notes) throws Exception {
        String motsCles = getMotsCles(diagnostic, notes);
        System.out.println("AI Mots-clés générés : " + motsCles);
        return YouTubeService.search(motsCles);
    }
}
