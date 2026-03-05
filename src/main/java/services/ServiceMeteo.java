package services;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import utils.EnvConfig;

/**
 * ServiceMeteo — APIs OpenWeatherMap GRATUITES
 * Fix : appelAPI() lève une IOException au lieu de retourner null
 * → les try/catch dans SuivieQuizController capturent vraiment l'erreur
 */
public class ServiceMeteo {

    private static final String API_KEY     = EnvConfig.get("OPENWEATHERMAP_API_KEY");
    private static final String URL_CURRENT = "https://api.openweathermap.org/data/2.5/weather";
    private static final String URL_FORECAST= "https://api.openweathermap.org/data/2.5/forecast";
    private static final int    CONNECT_TO  = 10_000;
    private static final int    READ_TO     = 15_000;

    // ════════════════════════════════════════════════════════════
    //  Icône → Emoji
    // ════════════════════════════════════════════════════════════
    public static String iconeVersEmoji(String code) {
        if (code == null || code.length() < 2) return "🌡️";
        String cat = code.substring(0, 2); boolean nuit = code.endsWith("n");
        return switch (cat) {
            case "01" -> nuit ? "🌙" : "☀️";
            case "02" -> nuit ? "🌙" : "🌤️";
            case "03" -> "🌥️";
            case "04" -> "☁️";
            case "09" -> "🌧️";
            case "10" -> nuit ? "🌧️" : "🌦️";
            case "11" -> "⛈️";
            case "13" -> "❄️";
            case "50" -> "🌫️";
            default   -> "🌡️";
        };
    }

    // ════════════════════════════════════════════════════════════
    //  MÉTÉO ACTUELLE
    // ════════════════════════════════════════════════════════════
    public static String getMeteo(double lat, double lon) throws Exception {
        String url  = URL_CURRENT + "?lat=" + lat + "&lon=" + lon
                + "&units=metric&lang=fr&appid=" + API_KEY;
        String json = appelAPI(url); // ✅ lance Exception si erreur

        double temp     = parseDouble(json, "\"temp\"");
        double ressenti = parseDouble(json, "\"feels_like\"");
        int    humidite = (int) parseDouble(json, "\"humidity\"");
        double ventKmh  = parseDouble(json, "\"speed\"") * 3.6;
        int    visib    = (int) parseDoubleOpt(json, "\"visibility\"", -1);
        String desc     = capitaliser(parseString(json, "\"description\""));
        String icone    = parseString(json, "\"icon\"");
        String emoji    = iconeVersEmoji(icone);

        StringBuilder r = new StringBuilder();
        r.append(emoji).append(" ").append(desc).append("\n")
                .append("🌡️ Température : ").append(arrondir(temp)).append("°C")
                .append("  (ressenti ").append(arrondir(ressenti)).append("°C)\n")
                .append("💧 Humidité    : ").append(humidite).append("%\n")
                .append("💨 Vent        : ").append(arrondir(ventKmh)).append(" km/h");
        if (visib >= 0)
            r.append("\n👁️ Visibilité  : ")
                    .append(visib >= 1000 ? arrondir(visib / 1000.0) + " km" : visib + " m");
        return r.toString();
    }

    // ════════════════════════════════════════════════════════════
    //  DONNÉES BRUTES — [temp, ressenti, humidite, ventKmh]
    // ════════════════════════════════════════════════════════════
    public static double[] getDonneesBrutes(double lat, double lon) throws Exception {
        String url  = URL_CURRENT + "?lat=" + lat + "&lon=" + lon
                + "&units=metric&lang=fr&appid=" + API_KEY;
        String json = appelAPI(url);
        return new double[]{
                parseDouble(json, "\"temp\""),
                parseDouble(json, "\"feels_like\""),
                parseDouble(json, "\"humidity\""),
                parseDouble(json, "\"speed\"") * 3.6
        };
    }

    // ════════════════════════════════════════════════════════════
    //  ICÔNE OWM
    // ════════════════════════════════════════════════════════════
    public static String getIcone(double lat, double lon) throws Exception {
        String url  = URL_CURRENT + "?lat=" + lat + "&lon=" + lon
                + "&units=metric&appid=" + API_KEY;
        String json = appelAPI(url);
        return parseString(json, "\"icon\"");
    }

    // ════════════════════════════════════════════════════════════
    //  CONSEIL SANTÉ
    // ════════════════════════════════════════════════════════════
    public static String getConseilSante(double lat, double lon) {
        try {
            String url  = URL_CURRENT + "?lat=" + lat + "&lon=" + lon
                    + "&units=metric&lang=fr&appid=" + API_KEY;
            String json = appelAPI(url);
            double temp = parseDouble(json, "\"temp\"");
            String ico  = parseString(json, "\"icon\"");
            String em   = iconeVersEmoji(ico);
            if      (temp < 0)   return em + " Il gèle ! Couvrez-vous bien.";
            else if (temp < 10)  return em + " Il fait froid. Portez une veste chaude.";
            else if (temp >= 35) return em + " Chaleur extrême. Hydratez-vous toutes les 30 min.";
            else if (temp >= 28) return em + " Grosse chaleur. Hydratez-vous et protégez-vous du soleil.";
            else if (ico.startsWith("09") || ico.startsWith("10")) return em + " Pluie prévue. Prenez un parapluie.";
            else if (ico.startsWith("11")) return em + " Orage. Restez à l'intérieur si possible.";
            else if (ico.startsWith("13")) return em + " Neige ! Soyez prudent sur la route.";
            else if (ico.startsWith("50")) return em + " Brouillard. Visibilité réduite.";
            else if (temp >= 18) return em + " Temps agréable. Une promenade fera du bien !";
            else return em + " Temps variable. Habillez-vous en couches.";
        } catch (Exception e) {
            return "🌤️ Conseil météo indisponible.";
        }
    }

    // ════════════════════════════════════════════════════════════
    //  PRÉVISIONS HORAIRES
    // ════════════════════════════════════════════════════════════
    public static String[] getPrevisionHoraire(double lat, double lon) {
        try {
            String url  = URL_FORECAST + "?lat=" + lat + "&lon=" + lon
                    + "&units=metric&lang=fr&appid=" + API_KEY;
            String json = appelAPI(url);
            String[] blocs = extraireTableau(json, "\"list\"", 7);
            int nb = Math.min(6, blocs.length - 1);
            if (nb <= 0) return new String[]{"❌ Aucune prévision"};
            String[] res = new String[nb];
            for (int i = 1; i <= nb; i++) {
                long ts   = (long) parseDouble(blocs[i], "\"dt\"");
                double temp = parseDouble(blocs[i], "\"temp\"");
                int  pop  = (int)(parseDoubleOpt(blocs[i], "\"pop\"", 0) * 100);
                String em = iconeVersEmoji(parseString(blocs[i], "\"icon\""));
                int heure = java.time.Instant.ofEpochSecond(ts)
                        .atZone(java.time.ZoneId.systemDefault()).getHour();
                res[i-1] = String.format("%02dh %s %s°C  🌂%d%%", heure, em, arrondir(temp), pop);
            }
            return res;
        } catch (Exception e) { return new String[]{"❌ " + e.getMessage()}; }
    }

    // ════════════════════════════════════════════════════════════
    //  HTTP — ✅ lève IOException au lieu de retourner null
    // ════════════════════════════════════════════════════════════
    private static String appelAPI(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TO);
        conn.setReadTimeout(READ_TO);
        conn.setRequestProperty("User-Agent", "MindCare/1.0");

        int code = conn.getResponseCode();
        if (code == 200) {
            return lireFlux(conn.getInputStream());
        }

        // ✅ Lève une exception avec le message HTTP → capturée par le try/catch
        String errBody = lireFlux(conn.getErrorStream());
        throw new IOException("HTTP " + code + " : " + errBody);
    }

    private static String lireFlux(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════
    //  PARSING JSON MANUEL
    // ════════════════════════════════════════════════════════════
    public static String extraireBloc(String json, String cle) {
        int ki = json.indexOf(cle); if (ki < 0) return "";
        int debut = json.indexOf("{", ki + cle.length());
        int debutArr = json.indexOf("[", ki + cle.length());
        if (debutArr >= 0 && (debut < 0 || debutArr < debut)) debut = json.indexOf("{", debutArr);
        if (debut < 0) return "";
        int niveau = 0, i = debut;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '{') niveau++;
            else if (c == '}') { niveau--; if (niveau == 0) return json.substring(debut, i+1); }
            i++;
        }
        return "";
    }

    public static String[] extraireTableau(String json, String cle, int maxItems) {
        int ki = json.indexOf(cle); if (ki < 0) return new String[0];
        int debut = json.indexOf("[", ki + cle.length()); if (debut < 0) return new String[0];
        java.util.List<String> blocs = new java.util.ArrayList<>();
        int i = debut + 1;
        while (i < json.length() && blocs.size() < maxItems) {
            while (i < json.length() && json.charAt(i) != '{' && json.charAt(i) != ']') i++;
            if (i >= json.length() || json.charAt(i) == ']') break;
            int niveau = 0, d = i;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '{') niveau++;
                else if (c == '}') { niveau--; if (niveau == 0) { blocs.add(json.substring(d, i+1)); i++; break; } }
                i++;
            }
        }
        return blocs.toArray(new String[0]);
    }

    public static double parseDouble(String bloc, String cle) {
        int ki = bloc.indexOf(cle);
        if (ki < 0) throw new IllegalArgumentException("Clé manquante : " + cle);
        int d = ki + cle.length();
        while (d < bloc.length() && !Character.isDigit(bloc.charAt(d)) && bloc.charAt(d) != '-') d++;
        int f = d; if (f < bloc.length() && bloc.charAt(f) == '-') f++;
        while (f < bloc.length() && (Character.isDigit(bloc.charAt(f)) || bloc.charAt(f) == '.')) f++;
        return Double.parseDouble(bloc.substring(d, f));
    }

    public static double parseDoubleOpt(String bloc, String cle, double def) {
        try { return parseDouble(bloc, cle); } catch (Exception e) { return def; }
    }

    public static String parseString(String bloc, String cle) {
        int ki = bloc.indexOf(cle); if (ki < 0) return "";
        int d = bloc.indexOf("\"", ki + cle.length() + 1); if (d < 0) return "";
        d++; int f = bloc.indexOf("\"", d); if (f < 0) return "";
        return bloc.substring(d, f);
    }

    private static String arrondir(double val) {
        String s = String.format("%.1f", val);
        return s.endsWith(".0") ? s.substring(0, s.length()-2) : s;
    }

    private static String capitaliser(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}