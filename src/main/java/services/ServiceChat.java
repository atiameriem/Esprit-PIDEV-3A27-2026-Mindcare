package services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ServiceChat {

    private static final String API_KEY = ""; // même clé que ServiceGemini
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";





    public String envoyerMessage(List<String> historiqueMessages, String question) {
        try {
            StringBuilder messages = new StringBuilder();

            // Contexte système
            messages.append("{\"role\":\"system\",\"content\":\"Tu es un assistant psychologique expert pour MindCare. "
                    + "Ton objectif est de mener un court bilan de bien-être en 3 ou 4 questions. "
                    + "1. Pose une question à la fois sur l'humeur, le stress ou le sommeil. "
                    + "2. Sois très empathique et encourageant. "
                    + "3. Une fois que tu as assez d'infos (après 3-4 échanges), donne un 'BILAN FINAL' clair avec : "
                    + "un score de bien-être estimé (0-100), une analyse courte et un conseil pratique. "
                    + "Réponds toujours en français de manière chaleureuse.\"}");

            // Historique alternée user/assistant
            boolean isUser = true;
            for (String msg : historiqueMessages) {
                String role = isUser ? "user" : "assistant";
                messages.append(",{\"role\":\"").append(role)
                        .append("\",\"content\":\"").append(escaper(msg)).append("\"}");
                isUser = !isUser;
            }

            // Nouvelle question
            messages.append(",{\"role\":\"user\",\"content\":\"").append(escaper(question)).append("\"}");

            String corps = "{"
                    + "\"model\":\"llama-3.3-70b-versatile\","
                    + "\"messages\":[" + messages + "],"
                    + "\"max_tokens\":300,"
                    + "\"temperature\":0.7"
                    + "}";

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(corps.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                return extraireTexte(sb.toString());
            } else {
                StringBuilder err = new StringBuilder();
                InputStream es = conn.getErrorStream();
                if (es != null) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(es, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) err.append(line);
                    }
                }
                System.err.println("❌ Groq HTTP " + status + " : " + err);
                return "Désolé, je n'ai pas pu répondre. Réessaie dans un instant.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur de connexion : " + e.getMessage();
        }
    }

    private String extraireTexte(String json) {
        try {
            int debut = json.indexOf("\"content\":\"") + 11;
            int fin = debut;
            while (fin < json.length()) {
                if (json.charAt(fin) == '"' && json.charAt(fin - 1) != '\\') break;
                fin++;
            }
            return json.substring(debut, fin)
                    .replace("\\n", "\n")
                    .replace("\\t", " ")
                    .trim();
        } catch (Exception e) {
            return "Je n'ai pas compris la réponse.";
        }
    }

    private String escaper(String texte) {
        return texte
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}