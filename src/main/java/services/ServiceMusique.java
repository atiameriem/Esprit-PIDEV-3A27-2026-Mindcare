package services;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ServiceMusique {

    // ✅ COLLER TES CLÉS ICI
    private static final String FREESOUND_KEY = "";
    private static final String GROQ_KEY      = "";


    // ══════════════════════════════════════════════════════════════
    // Classe Piste
    // ══════════════════════════════════════════════════════════════
    public static class Piste {
        public String nom;
        public String url;
        public String duree;
        public String emoji;

        public Piste(String nom, String url,
                     String duree, String emoji) {
            this.nom   = nom;
            this.url   = url;
            this.duree = duree;
            this.emoji = emoji;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Classe Params
    // ══════════════════════════════════════════════════════════════
    public static class MusiqueParams {
        public int    bpm;
        public String tags;
        public String message;
        public String ambiance;

        public MusiqueParams(int bpm, String tags,
                             String message, String ambiance) {
            this.bpm      = bpm;
            this.tags     = tags;
            this.message  = message;
            this.ambiance = ambiance;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ Groq analyse les scores et calcule les params musicaux
    // ══════════════════════════════════════════════════════════════
    public MusiqueParams calculerParams(int scoreBE, int scoreST,
                                        int scoreHU, String nomPatient) {
        try {
            String prompt =
                    "Tu es musicothérapeute expert. "
                            + "Patient " + nomPatient + " : "
                            + "Bien-être=" + scoreBE + "%, "
                            + "Stress=" + scoreST + "%, "
                            + "Humeur=" + scoreHU + "%. "
                            + "Réponds UNIQUEMENT en JSON sans markdown : "
                            + "{\"bpm\":60,"
                            + "\"tags\":\"ocean waves relaxing\","
                            + "\"ambiance\":\"relaxation profonde\","
                            + "\"message\":\"message motivant max 8 mots\"}. "
                            + "Règles BPM : stress<40 ou humeur<40 → bpm 40-55, "
                            + "stress 40-70 → bpm 55-75, "
                            + "stress>70 → bpm 75-95. "
                            + "Tags en anglais — choisir parmi : "
                            + "ocean waves, forest birds, rain nature, "
                            + "piano soft, ambient calm, classical relaxing. "
                            + "Message en français max 8 mots.";

            String reponse = appelerGroq(prompt);
            System.out.println("🤖 Groq params: " + reponse);
            return parseParams(reponse, scoreST, scoreHU);

        } catch (Exception e) {
            System.err.println("❌ Groq params : " + e.getMessage());
            return paramsParDefaut(scoreST, scoreHU);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ Freesound — cherche pistes selon ambiance
    // ══════════════════════════════════════════════════════════════
    public List<Piste> chercherPistes(MusiqueParams params) {
        List<Piste> pistes = new ArrayList<>();
        try {
            String[] mots = params.tags.split(" ");
            String queryStr = mots.length >= 2
                    ? mots[0] + " " + mots[1] : mots[0];

            String query = URLEncoder.encode(
                    queryStr,
                    StandardCharsets.UTF_8.toString());

            String urlStr =
                    "https://freesound.org/apiv2/search/text/"
                            + "?query=" + query
                            + "&fields=name,previews,duration"
                            + "&filter=duration:[30+TO+300]"
                            + "&page_size=6"
                            + "&sort=rating_desc";

            System.out.println("🌐 Freesound query: " + queryStr);

            URL url = new URL(urlStr);
            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization",
                    "Token " + FREESOUND_KEY);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            System.out.println("🌐 HTTP: " + code);

            if (code == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(
                                conn.getInputStream(),
                                StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null)
                        sb.append(line);
                }
                String json = sb.toString();
                System.out.println("📦 JSON: "
                        + json.substring(0,
                        Math.min(80, json.length())));

                if (json.startsWith("{")) {
                    pistes = parsePistes(json);
                    System.out.println("🎵 "
                            + pistes.size() + " pistes trouvées !");
                } else {
                    System.err.println("❌ HTML — clé invalide");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Freesound : " + e.getMessage());
        }

        if (pistes.isEmpty()) {
            System.out.println("⚠️ Fallback Mixkit");
            pistes = pistesParDefaut(params);
        }
        return pistes;
    }

    // ══════════════════════════════════════════════════════════════
    // Parse JSON Freesound
    // ══════════════════════════════════════════════════════════════
    private List<Piste> parsePistes(String json) {
        List<Piste> pistes = new ArrayList<>();
        try {
            String[] emojis = {"🎵", "🌊", "🌿", "🎹", "🧘", "🌙"};
            int idx = 0;
            int pos = 0;

            while (idx < 6) {
                int nameStart = json.indexOf("\"name\":\"", pos);
                if (nameStart == -1) break;
                nameStart += 8;
                int nameEnd = json.indexOf("\"", nameStart);
                String nom = json.substring(nameStart, nameEnd);

                int urlStart = json.indexOf(
                        "\"preview-hq-mp3\":\"", pos);
                if (urlStart == -1)
                    urlStart = json.indexOf(
                            "\"preview-lq-mp3\":\"", pos);
                if (urlStart == -1) {
                    pos = nameEnd + 1;
                    continue;
                }
                urlStart += 18; // ✅ https:// complet
                int urlEnd = json.indexOf("\"", urlStart);
                String url = json.substring(urlStart, urlEnd);

                int durStart = json.indexOf(
                        "\"duration\":", pos) + 11;
                int durEnd = json.indexOf(",", durStart);
                if (durEnd == -1)
                    durEnd = json.indexOf("}", durStart);
                int dureeInt = 0;
                try {
                    dureeInt = (int) Double.parseDouble(
                            json.substring(durStart, durEnd).trim());
                } catch (Exception ignored) {}
                String duree = (dureeInt / 60) + "m"
                        + String.format("%02d", dureeInt % 60) + "s";

                String nomCourt = nom.length() > 28
                        ? nom.substring(0, 28) + "…" : nom;

                pistes.add(new Piste(nomCourt, url, duree,
                        emojis[idx % emojis.length]));
                idx++;
                pos = urlEnd + 1;
            }
        } catch (Exception e) {
            System.err.println("❌ Parse: " + e.getMessage());
        }
        return pistes;
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ Appel Groq
    // ══════════════════════════════════════════════════════════════
    private String appelerGroq(String prompt) throws Exception {
        URL url = new URL(
                "https://api.groq.com/openai/v1/chat/completions");
        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",
                "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization",
                "Bearer " + GROQ_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);

        String promptEscape = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");

        String corps = "{\"model\":\"llama-3.3-70b-versatile\","
                + "\"messages\":[{\"role\":\"user\","
                + "\"content\":\"" + promptEscape + "\"}],"
                + "\"max_tokens\":150,\"temperature\":0.3}";

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
            String json  = sb.toString();
            int    debut = json.indexOf("\"content\":\"") + 11;
            int    fin   = debut;
            while (fin < json.length()) {
                if (json.charAt(fin) == '"'
                        && json.charAt(fin - 1) != '\\') break;
                fin++;
            }
            return json.substring(debut, fin)
                    .replace("\\n", " ").trim();
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

    // ══════════════════════════════════════════════════════════════
    // Parse params JSON
    // ══════════════════════════════════════════════════════════════
    private MusiqueParams parseParams(String json,
                                      int scoreST, int scoreHU) {
        try {
            if (json == null)
                return paramsParDefaut(scoreST, scoreHU);
            json = json.replace("```json", "")
                    .replace("```", "").trim();
            int debut = json.indexOf("{");
            int fin   = json.lastIndexOf("}");
            if (debut != -1 && fin != -1)
                json = json.substring(debut, fin + 1);

            int    bpm      = Integer.parseInt(
                    extraireStr(json, "bpm"));
            String tags     = extraireStr(json, "tags");
            String message  = extraireStr(json, "message");
            String ambiance = extraireStr(json, "ambiance");
            return new MusiqueParams(bpm, tags, message, ambiance);
        } catch (Exception e) {
            return paramsParDefaut(scoreST, scoreHU);
        }
    }

    private MusiqueParams paramsParDefaut(int scoreST, int scoreHU) {
        if (scoreST < 40 || scoreHU < 40)
            return new MusiqueParams(50,
                    "ocean waves relaxing",
                    "Détendez-vous profondément 🧘",
                    "Relaxation profonde");
        else if (scoreST < 70)
            return new MusiqueParams(65,
                    "forest birds peaceful",
                    "Respirez et lâchez prise 🌿",
                    "Détente douce");
        else
            return new MusiqueParams(85,
                    "piano soft instrumental",
                    "Retrouvez votre énergie ! ⚡",
                    "Énergie positive");
    }

    // ══════════════════════════════════════════════════════════════
    // Pistes Mixkit si Freesound indisponible
    // ══════════════════════════════════════════════════════════════
    private List<Piste> pistesParDefaut(MusiqueParams params) {
        List<Piste> fallback = new ArrayList<>();
        if (params.bpm <= 65) {
            fallback.add(new Piste("Vagues océan",
                    "https://assets.mixkit.co/music/preview/mixkit-dreaming-big-31.mp3",
                    "2m30s", "🌊"));
            fallback.add(new Piste("Forêt apaisante",
                    "https://assets.mixkit.co/music/preview/mixkit-serene-view-443.mp3",
                    "2m00s", "🌿"));
            fallback.add(new Piste("Méditation douce",
                    "https://assets.mixkit.co/music/preview/mixkit-sleepy-cat-135.mp3",
                    "2m15s", "🧘"));
            fallback.add(new Piste("Piano zen",
                    "https://assets.mixkit.co/music/preview/mixkit-piano-reflections-22.mp3",
                    "2m00s", "🎹"));
            fallback.add(new Piste("Nuit étoilée",
                    "https://assets.mixkit.co/music/preview/mixkit-starring-at-the-night-sky-426.mp3",
                    "2m30s", "🌙"));
            fallback.add(new Piste("Calme intérieur",
                    "https://assets.mixkit.co/music/preview/mixkit-just-chillin-30.mp3",
                    "2m00s", "☮️"));
        } else {
            fallback.add(new Piste("Énergie positive",
                    "https://assets.mixkit.co/music/preview/mixkit-sun-and-his-daughter-580.mp3",
                    "2m00s", "⚡"));
            fallback.add(new Piste("Matin lumineux",
                    "https://assets.mixkit.co/music/preview/mixkit-morning-routine-14.mp3",
                    "2m15s", "☀️"));
            fallback.add(new Piste("Piano motivant",
                    "https://assets.mixkit.co/music/preview/mixkit-upbeat-funny-short-153.mp3",
                    "1m45s", "🎹"));
            fallback.add(new Piste("Douce joie",
                    "https://assets.mixkit.co/music/preview/mixkit-happy-times-154.mp3",
                    "2m00s", "😊"));
            fallback.add(new Piste("Espoir",
                    "https://assets.mixkit.co/music/preview/mixkit-feeling-happy-5.mp3",
                    "2m30s", "🌈"));
            fallback.add(new Piste("Confiance",
                    "https://assets.mixkit.co/music/preview/mixkit-a-very-happy-christmas-897.mp3",
                    "2m00s", "💪"));
        }
        return fallback;
    }

    // ══════════════════════════════════════════════════════════════
    // Helper
    // ══════════════════════════════════════════════════════════════
    private String extraireStr(String json, String cle) {
        try {
            String pattern = "\"" + cle + "\":";
            int idx = json.indexOf(pattern) + pattern.length();
            if (idx < pattern.length()) return "";
            if (json.charAt(idx) == '"') {
                idx++;
                int fin = json.indexOf("\"", idx);
                return json.substring(idx, fin);
            } else {
                int fin = json.indexOf(",", idx);
                if (fin == -1) fin = json.indexOf("}", idx);
                return json.substring(idx, fin).trim();
            }
        } catch (Exception e) { return ""; }
    }
}