package services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ServiceGemini {

    private static final String API_KEY = ""; // gsk_...
    private static final String API_URL = "";

    public String genererConseil(int scoreBienEtre, int scoreStress,
                                 int scoreHumeur, int nbSessions) {
        try {
            String prompt = construirePrompt(scoreBienEtre, scoreStress, scoreHumeur, nbSessions);
            String reponse = appellerGroq(prompt); // ✅ appellerGroq pas appellerGemini
            return reponse != null ? reponse : conseilParDefaut(scoreStress);
        } catch (Exception e) {
            System.err.println("❌ Erreur Groq : " + e.getMessage());
            return conseilParDefaut(scoreStress);
        }
    }/////

    private String construirePrompt(int be, int st, int hu, int sessions) {
        String niveauBE = be >= 70 ? "bon"            : be >= 40 ? "moyen"   : "faible";
        String niveauST = st >= 70 ? "faible (calme)" : st >= 40 ? "modéré"  : "élevé";
        String niveauHU = hu >= 70 ? "bonne"          : hu >= 40 ? "moyenne" : "mauvaise";

        return "Tu es un assistant psychologique bienveillant pour l'application MindCare. "
                + "Un utilisateur a : Bien-etre " + be + "/100 (" + niveauBE + "), "
                + "Stress " + st + "/100 (" + niveauST + "), "
                + "Humeur " + hu + "/100 (" + niveauHU + "), "
                + sessions + " sessions completees. "
                + "Genere UN seul conseil personnalise en 2 phrases maximum. "
                + "Reponds uniquement en français. Sans titre ni liste.";
    }

    // ✅ Format Groq/OpenAI — complètement différent de Gemini
    private String appellerGroq(String prompt) throws Exception {
        String promptEscape = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");

        // ✅ Corps au format OpenAI
        String corps = "{"
                + "\"model\":\"llama-3.3-70b-versatile\","  // ← changer ici
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + promptEscape + "\"}],"
                + "\"max_tokens\":120,"
                + "\"temperature\":0.75"
                + "}";

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY); // ✅ Header Groq
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
            return null;
        }
    }

    // ✅ Format réponse Groq : choices[0].message.content
    private String extraireTexte(String json) {
        try {
            int debut = json.indexOf("\"content\":\"") + 11;
            int fin = debut;
            while (fin < json.length()) {
                if (json.charAt(fin) == '"' && json.charAt(fin - 1) != '\\') break;
                fin++;
            }
            return json.substring(debut, fin)
                    .replace("\\n", " ")
                    .replace("\\t", " ")
                    .trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String conseilParDefaut(int scoreStress) {
        if (scoreStress < 40) {
            return "😮‍💨 Ton niveau de stress est élevé. Essaie la respiration 4-7-8 : inspire 4s, retiens 7s, expire 8s.";
        } else if (scoreStress < 70) {
            return "🌿 Tu gères bien ton stress. Continue avec de courtes pauses de pleine conscience.";
        } else {
            return "✅ Excellent équilibre ! 5 minutes de cohérence cardiaque chaque matin renforceront ton bien-être.";
        }
    }
    // ════════════════════════════════════════════════════════
// Génère 3 conseils structurés via Groq
// Format retourné : "emoji|Titre|Description"
// ════════════════════════════════════════════════════════
    public List<String[]> genererTroisConseils(int be, int st, int hu) {
        try {
            String prompt = "Tu es un assistant bien-être pour MindCare. "
                    + "Un utilisateur a : Bien-etre " + be + "/100, Stress " + st + "/100, Humeur " + hu + "/100. "
                    + "Genere exactement 3 conseils pratiques adaptes a son profil. "
                    + "Reponds UNIQUEMENT dans ce format exact (3 lignes, rien d'autre) : "
                    + "EMOJI|Titre court|Description courte en une phrase. "
                    + "Exemple : 🧘|Méditation|5 minutes le matin pour calmer l'esprit. "
                    + "En français. 3 lignes uniquement.";

            String reponse = appellerGroq(prompt);
            return parserTroisConseils(reponse);

        } catch (Exception e) {
            return conseilsParDefaut();
        }
    }

    private List<String[]> parserTroisConseils(String reponse) {
        List<String[]> liste = new ArrayList<>();
        if (reponse == null) return conseilsParDefaut();

        String[] lignes = reponse.split("\n");
        for (String ligne : lignes) {
            ligne = ligne.trim();
            if (ligne.isEmpty()) continue;
            String[] parts = ligne.split("\\|");
            if (parts.length >= 3) {
                liste.add(new String[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
            }
            if (liste.size() == 3) break;
        }
        // Compléter si l'IA n'a pas retourné 3 conseils
        while (liste.size() < 3) {
            List<String[]> def = conseilsParDefaut();
            liste.add(def.get(liste.size()));
        }
        return liste;
    }

    private List<String[]> conseilsParDefaut() {
        List<String[]> liste = new ArrayList<>();
        liste.add(new String[]{"🫀", "Cohérence cardiaque",   "5 min le matin pour stabiliser ton bien-être."});
        liste.add(new String[]{"😮‍💨", "Respiration 4-7-8",    "Réduit le stress en moins de 2 minutes."});
        liste.add(new String[]{"🌙", "Qualité du sommeil",    "Objectif : 7h avant minuit pour améliorer l'humeur."});
        return liste;
    }
}