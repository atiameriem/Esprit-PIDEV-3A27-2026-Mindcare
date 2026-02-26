package services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * ══════════════════════════════════════════════════════════════
 *  EmotionAnalyzer
 *  Analyse le texte via HuggingFace Inference API
 *  Modèle : j-hartmann/emotion-english-distilroberta-base
 *  Retourne l'émotion dominante parmi :
 *    anger / disgust / fear / joy / neutral / sadness / surprise
 * ══════════════════════════════════════════════════════════════
 */
public class ServiceEmotionAnalyzer {

    // ✅ Clé HuggingFace — https://huggingface.co/settings/tokens
    private static final String HF_TOKEN =
            "";

    // Modèle émotion 7 classes — SamLowe (plus stable que j-hartmann)
    private static final String HF_URL =
            "https://api-inference.huggingface.co/models/"
                    + "SamLowe/roberta-base-go_emotions";

    // Timeout généreux — modèle peut se réveiller (cold start)
    private static final int TIMEOUT_MS = 20_000;

    /**
     * Analyse l'émotion dominante à partir d'une phrase contexte.
     *
     * @param texteContexte  phrase décrivant l'état du patient
     * @return émotion en français : "Joie", "Tristesse", etc.
     */
    public String analyserEmotion(String texteContexte) {
        try {
            String json = appellerHuggingFace(texteContexte);
            if (json == null) return emotionParDefaut(texteContexte);

            String labelBrut = extraireTopLabel(json);
            return traduitreEmotion(labelBrut);

        } catch (Exception e) {
            System.err.println("❌ EmotionAnalyzer : " + e.getMessage());
            return emotionParDefaut(texteContexte);
        }
    }

    /**
     * Retourne aussi le score de confiance (0.0 → 1.0).
     */
    public double analyserScore(String texteContexte) {
        try {
            String json = appellerHuggingFace(texteContexte);
            if (json == null) return 0.5;
            return extraireTopScore(json);
        } catch (Exception e) {
            return 0.5;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Appel HTTP HuggingFace
    // ══════════════════════════════════════════════════════════════
    private String appellerHuggingFace(String texte) throws Exception {
        String escaped = texte
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");

        String corps = "{\"inputs\":\"" + escaped + "\"}";

        URL url = new URL(HF_URL);
        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",
                "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization",
                "Bearer " + HF_TOKEN);
        conn.setDoOutput(true);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(corps.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        System.out.println("🧠 HuggingFace status=" + status);

        if (status == 200) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null)
                    sb.append(line);
            }
            System.out.println("🧠 HF réponse : " + sb);
            return sb.toString();

        } else if (status == 503) {
            // Cold start HuggingFace — réessayer après 3s
            System.out.println("⏳ HF modèle en veille, attente...");
            Thread.sleep(3000);
            return appellerHuggingFace(texte);
        }

        System.err.println("❌ HF HTTP " + status);
        return null;
    }

    // ══════════════════════════════════════════════════════════════
    // Extraction du label dominant
    // Format retourné : [[{"label":"joy","score":0.95},...]]
    // ══════════════════════════════════════════════════════════════
    private String extraireTopLabel(String json) {
        try {
            // Chercher le premier "label" dans la réponse
            int idx = json.indexOf("\"label\":\"");
            if (idx < 0) return "neutral";
            int debut = idx + 9;
            int fin   = json.indexOf("\"", debut);
            return json.substring(debut, fin).toLowerCase().trim();
        } catch (Exception e) {
            return "neutral";
        }
    }

    private double extraireTopScore(String json) {
        try {
            int idx = json.indexOf("\"score\":");
            if (idx < 0) return 0.5;
            int debut = idx + 8;
            int fin   = json.indexOf(",", debut);
            if (fin < 0) fin = json.indexOf("}", debut);
            return Double.parseDouble(
                    json.substring(debut, fin).trim());
        } catch (Exception e) {
            return 0.5;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Traduction anglais → français + emoji
    // ══════════════════════════════════════════════════════════════
    public String traduitreEmotion(String labelEn) {
        if (labelEn == null) return "😐 Neutre";
        return switch (labelEn.toLowerCase()) {
            case "joy", "amusement", "excitement", "optimism",
                 "gratitude", "love", "admiration", "approval",
                 "caring", "pride"                -> "😊 Joie";
            case "sadness", "grief", "disappointment",
                 "remorse"                        -> "😢 Tristesse";
            case "anger", "annoyance", "disapproval"
                    -> "😠 Colère";
            case "fear", "nervousness"            -> "😨 Anxiété";
            case "surprise", "realization",
                 "curiosity", "confusion"         -> "😲 Surprise";
            case "disgust"                        -> "🤢 Dégoût";
            case "neutral"                        -> "😐 Neutre";
            case "embarrassment"                  -> "😳 Gêne";
            case "relief"                         -> "😌 Soulagement";
            default                               -> "😐 " + labelEn;
        };
    }

    // ══════════════════════════════════════════════════════════════
    // Fallback basé sur le texte si HF échoue
    // ══════════════════════════════════════════════════════════════
    private String emotionParDefaut(String texte) {
        String t = texte.toLowerCase();
        if (t.contains("stress") || t.contains("anxiet"))
            return "😨 Anxiété";
        if (t.contains("bien") || t.contains("excel"))
            return "😊 Joie";
        if (t.contains("faible") || t.contains("difficil"))
            return "😢 Tristesse";
        return "😐 Neutre";
    }

    /**
     * Construit une phrase contexte pour l'analyse
     * à partir des scores et du type de quiz.
     */
    public static String construireTexteContexte(
            String titreQuiz, int pct) {

        String base = "I feel ";

        if (titreQuiz.toLowerCase().contains("stress")) {
            if (pct < 40)
                return base + "very stressed and overwhelmed lately.";
            if (pct < 70)
                return base + "somewhat stressed but managing.";
            return base + "calm and in control of my stress.";

        } else if (titreQuiz.toLowerCase().contains("humeur")) {
            if (pct < 40)
                return base + "sad and low in mood these days.";
            if (pct < 70)
                return base + "okay but not great emotionally.";
            return base + "happy and in a good mood.";

        } else {
            if (pct < 40)
                return base + "unwell and struggling with daily life.";
            if (pct < 70)
                return base + "average, not great but getting by.";
            return base + "great and positive about my wellbeing.";
        }
    }
}