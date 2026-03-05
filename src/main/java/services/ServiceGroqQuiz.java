package services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import utils.EnvConfig;

public class ServiceGroqQuiz {

    // ✅ Coller ta clé ici — format : gsk_xxxxx
    private static final String API_KEY = EnvConfig.get("GROQ_API_KEY");
    private static final String API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    // ══════════════════════════════════════════════════════════════
    // Conseil principal
    // ══════════════════════════════════════════════════════════════
    public String genererConseil(int scoreBienEtre, int scoreStress,
                                 int scoreHumeur, int nbSessions) {
        try {
            String prompt = construirePrompt(
                    scoreBienEtre, scoreStress, scoreHumeur, nbSessions);
            String reponse = appellerGroq(prompt);
            return reponse != null
                    ? reponse : conseilParDefaut(scoreStress);
        } catch (Exception e) {
            System.err.println("❌ Erreur Groq : " + e.getMessage());
            return conseilParDefaut(scoreStress);
        }
    }

    private String construirePrompt(int be, int st, int hu,
                                    int sessions) {
        String niveauBE = be >= 70 ? "bon"
                : be >= 40 ? "moyen" : "faible";
        String niveauST = st >= 70 ? "faible (calme)"
                : st >= 40 ? "modere" : "eleve";
        String niveauHU = hu >= 70 ? "bonne"
                : hu >= 40 ? "moyenne" : "mauvaise";

        return "Tu es un assistant psychologique bienveillant "
                + "pour MindCare. "
                + "Un utilisateur a : Bien-etre " + be
                + "/100 (" + niveauBE + "), "
                + "Stress " + st + "/100 (" + niveauST + "), "
                + "Humeur " + hu + "/100 (" + niveauHU + "), "
                + sessions + " sessions completees. "
                + "Genere UN seul conseil personnalise en 2 phrases "
                + "maximum. Reponds uniquement en francais. "
                + "Sans titre ni liste.";
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ PUBLIC — utilisable depuis n'importe quel controller
    // ══════════════════════════════════════════════════════════════
    public String appellerGroq(String prompt) throws Exception {
        String promptEscape = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");

        String corps = "{"
                + "\"model\":\"llama-3.3-70b-versatile\","
                + "\"messages\":[{\"role\":\"user\","
                + "\"content\":\"" + promptEscape + "\"}],"
                + "\"max_tokens\":1024,"    // ✅ FIX : 200 → 1024
                + "\"temperature\":0.7"
                + "}";

        URL url = new URL(API_URL);
        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",
                "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization",
                "Bearer " + API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(corps.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status == 200) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null)
                    sb.append(line);
            }
            return extraireTexte(sb.toString());
        } else {
            StringBuilder err = new StringBuilder();
            InputStream es = conn.getErrorStream();
            if (es != null) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(es,
                                StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null)
                        err.append(line);
                }
            }
            System.err.println("❌ Groq HTTP "
                    + status + " : " + err);
            return null;
        }
    }

    private String extraireTexte(String json) {
        try {
            int debut = json.indexOf("\"content\":\"") + 11;
            int fin   = debut;
            while (fin < json.length()) {
                if (json.charAt(fin) == '"'
                        && json.charAt(fin - 1) != '\\') break;
                fin++;
            }
            return json.substring(debut, fin)
                    .replace("\\n", "\n")
                    .replace("\\t", " ")
                    .trim();
        } catch (Exception e) { return null; }
    }

    private String conseilParDefaut(int scoreStress) {
        if (scoreStress < 40)
            return "Ton niveau de stress est eleve. "
                    + "Essaie la respiration 4-7-8 : "
                    + "inspire 4s, retiens 7s, expire 8s.";
        if (scoreStress < 70)
            return "Tu geres bien ton stress. "
                    + "Continue avec de courtes pauses "
                    + "de pleine conscience.";
        return "Excellent equilibre ! "
                + "5 minutes de coherence cardiaque "
                + "chaque matin renforceront ton bien-etre.";
    }

    // ══════════════════════════════════════════════════════════════
    // 3 conseils structurés
    // ══════════════════════════════════════════════════════════════
    public List<String[]> genererTroisConseils(int be, int st,
                                               int hu) {
        try {
            String prompt = "Tu es un assistant bien-etre pour MindCare. "
                    + "Un utilisateur a : Bien-etre " + be
                    + "/100, Stress " + st
                    + "/100, Humeur " + hu + "/100. "
                    + "Genere exactement 3 conseils pratiques. "
                    + "Format exact (3 lignes) : "
                    + "EMOJI|Titre court|Description courte. "
                    + "En francais. 3 lignes uniquement.";
            String reponse = appellerGroq(prompt);
            return parserTroisConseils(reponse);
        } catch (Exception e) {
            System.err.println("❌ 3 conseils : " + e.getMessage());
            return conseilsParDefaut();
        }
    }

    private List<String[]> parserTroisConseils(String reponse) {
        List<String[]> liste = new ArrayList<>();
        if (reponse == null) return conseilsParDefaut();
        for (String ligne : reponse.split("\n")) {
            ligne = ligne.trim();
            if (ligne.isEmpty()) continue;
            String[] parts = ligne.split("\\|");
            if (parts.length >= 3)
                liste.add(new String[]{
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim()});
            if (liste.size() == 3) break;
        }
        List<String[]> def = conseilsParDefaut();
        while (liste.size() < 3)
            liste.add(def.get(liste.size()));
        return liste;
    }

    private List<String[]> conseilsParDefaut() {
        List<String[]> l = new ArrayList<>();
        l.add(new String[]{"🫀", "Coherence cardiaque",
                "5 min le matin pour stabiliser ton bien-etre."});
        l.add(new String[]{"😮", "Respiration 4-7-8",
                "Reduit le stress en moins de 2 minutes."});
        l.add(new String[]{"🌙", "Qualite du sommeil",
                "7h avant minuit pour ameliorer l humeur."});
        return l;
    }

    // ══════════════════════════════════════════════════════════════
    // Recommandation test + raison
    // ══════════════════════════════════════════════════════════════
    public String recommanderProchainTest(int scoreBE,
                                          int scoreST,
                                          int scoreHU) {
        int min = Math.min(scoreBE, Math.min(scoreST, scoreHU));
        if (min == scoreST) return "Test de Stress";
        if (min == scoreBE) return "Test de Bien-etre";
        return "Test d Humeur";
    }

    public String getRaisonRecommandation(int scoreBE,
                                          int scoreST,
                                          int scoreHU) {
        int min = Math.min(scoreBE, Math.min(scoreST, scoreHU));
        if (min == scoreST)
            return "Votre niveau de stress est le plus critique.";
        if (min == scoreBE)
            return "Votre bien-etre necessite une attention particuliere.";
        return "Votre humeur montre des signes qui meritent un suivi.";
    }

    // ══════════════════════════════════════════════════════════════
    // 🔹 Analyse de l'émotion à partir d'un texte
    // ══════════════════════════════════════════════════════════════
    public String analyserEmotion(String texte) {
        if (texte == null || texte.isEmpty()) return "Non détectée";

        String prompt = "Analyse l'émotion principale du texte suivant "
                + "et renvoie un mot ou emoji décrivant l'état émotionnel : "
                + texte + " Répond uniquement par 1 mot ou un emoji, en français.";

        String emotion = null;
        try {
            emotion = appellerGroq(prompt);
        } catch (Exception e) {
            System.err.println("❌ Erreur analyse émotion : " + e.getMessage());
        }

        if (emotion == null || emotion.trim().isEmpty()) {
            texte = texte.toLowerCase();
            if (texte.contains("heureux") || texte.contains("bien") || texte.contains("positif"))
                emotion = "😊 Heureux / positif";
            else if (texte.contains("triste") || texte.contains("déprimé") || texte.contains("bas"))
                emotion = "😢 Triste / déprimé";
            else if (texte.contains("stressé") || texte.contains("angoissé"))
                emotion = "😰 Stressé / anxieux";
            else if (texte.contains("colère") || texte.contains("irrité"))
                emotion = "😡 En colère / irrité";
            else
                emotion = "😶 Indéterminé";
        }

        return emotion;
    }
    /**
     * Envoie un prompt libre à Groq et retourne la réponse texte.
     * Utilisé par SuivieQuizController pour le conseil météo × psy.
     */
    public String envoyerPromptLibre(String prompt) {
        try {
            String body = "{"
                    + "\"model\":\"llama3-8b-8192\","
                    + "\"messages\":[{\"role\":\"user\",\"content\":"
                    + jsonEscape(prompt) + "}],"
                    + "\"max_tokens\":200,\"temperature\":0.7}";

            URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setConnectTimeout(12000); c.setReadTimeout(20000);
            c.setRequestProperty("Content-Type","application/json");
            c.setRequestProperty("Authorization","Bearer " + API_KEY);
            c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

            if (c.getResponseCode() != 200) return null;
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line; while ((line = br.readLine()) != null) sb.append(line);

            String json = sb.toString();
            int ci = json.indexOf("\"content\"");
            if (ci < 0) return null;
            int d = json.indexOf("\"", ci + 10) + 1;
            int f = d;
            while (f < json.length()) {
                if (json.charAt(f) == '"' && json.charAt(f-1) != '\\') break;
                f++;
            }
            return json.substring(d, f)
                    .replace("\\n","\n").replace("\\\"","\"").trim();
        } catch (Exception e) {
            System.err.println("❌ envoyerPromptLibre : " + e.getMessage());
            return null;
        }
    }

    private String jsonEscape(String s) {
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","\\r") + "\"";
    }

}