package services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service pour interagir avec l'API Google Gemini.
 * Permet d'analyser les statistiques de l'utilisateur et de donner des
 * suggestions
 * personnalisées.
 */
public class GeminiService {

    // REMPLACER PAR VOTRE CLÉ API GEMINI
    private static final String API_KEY = "";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
            + API_KEY;

    public String getAiInsights(int totalTime, double avgProgression) {
        if ("".equals(API_KEY)) {
            return "Note : Configurez votre clé API Gemini dans GeminiService.java pour recevoir des suggestions personnalisées basées sur votre temps passé ("
                    + totalTime + " min).";
        }

        try {
            URL url = java.net.URI.create(API_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String prompt = "En tant qu'assistant bien-être MindCare, analyse ces stats d'un utilisateur : " +
                    "Temps total passé sur les modules : " + totalTime + " minutes. " +
                    "Progression moyenne : " + (avgProgression * 100) + "%. " +
                    "Donne une suggestion courte (2 phrases max) pour la suite de son parcours.";

            String jsonPayload = "{" +
                    "\"contents\": [{" +
                    "  \"parts\":[{" +
                    "    \"text\": \"" + prompt + "\"" +
                    "  }]" +
                    "}]" +
                    "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }

                // Extraction rudimentaire du texte de la réponse JSON de Gemini
                String res = response.toString();
                if (res.contains("\"text\": \"")) {
                    int start = res.indexOf("\"text\": \"") + 9;
                    int end = res.indexOf("\"", start);
                    return res.substring(start, end).replace("\\n", "\n");
                }
                return "Analyse terminée. Continuez vos efforts !";
            } else {
                return "Suggestion : Gardez une routine régulière pour maximiser les bienfaits de votre formation.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "L'IA MindCare vous encourage à continuer votre parcours de bien-être.";
        }
    }
}
