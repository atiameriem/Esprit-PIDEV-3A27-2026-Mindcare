package services;

import utils.ApiConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TranslateService {

    // ✅ Ignore SSL (dev) pour éviter PKIX
    private final HttpClient http = HttpClient.newBuilder()
            .sslContext(insecureSslContext())
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String translate(String text, String sourceLang, String targetLang) {
        try {
            return translateOrThrow(text, sourceLang, targetLang);
        } catch (Exception ex) {
            // ✅ Ne jamais remonter une exception "vide"
            String msg = (ex.getMessage() == null || ex.getMessage().isBlank())
                    ? ex.getClass().getSimpleName()
                    : ex.getMessage();
            throw new IllegalStateException("Translate failed: " + msg, ex);
        }
    }

    private String translateOrThrow(String text, String sourceLang, String targetLang) throws Exception {
        if (text == null || text.isBlank()) return "";
        if (targetLang == null || targetLang.isBlank()) return text;

        String src = (sourceLang == null || sourceLang.isBlank()) ? "fr" : sourceLang.trim(); // ✅ pas "auto"
        String tgt = targetLang.trim();
        if (src.equalsIgnoreCase(tgt)) return text;

        String key = text + "|" + src.toLowerCase() + "|" + tgt.toLowerCase();
        String cached = cache.get(key);
        if (cached != null) return cached;

        // ✅ Instance fiable (évite libretranslate.com)
        String base = ApiConfig.libreTranslateUrl();
        if (base == null || base.isBlank()) base = "https://translate.argosopentech.com";

        String url = base.endsWith("/") ? base + "translate" : base + "/translate";
        String apiKey = ApiConfig.libreTranslateApiKey();

        String payload = "{"
                + "\"q\":" + json(text) + ","
                + "\"source\":" + json(src) + ","
                + "\"target\":" + json(tgt) + ","
                + "\"format\":\"text\""
                + (apiKey == null || apiKey.isBlank() ? "" : ",\"api_key\":" + json(apiKey))
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String body = res.body();
        if (body != null && body.trim().startsWith("<!DOCTYPE html")) {
            throw new IllegalStateException("Endpoint non compatible (HTML) @ " + base);
        }

        if (res.statusCode() >= 300) {
            // ✅ message clair
            throw new IllegalStateException("LibreTranslate error " + res.statusCode() + " @ " + base + ": " + body);
        }

        String translated = extractJsonString(body, "translatedText");
        String out = (translated == null || translated.isBlank()) ? text : translated;

        cache.put(key, out);
        return out;
    }

    // ---------------- SSL: ignore certs (DEV ONLY) ----------------
    private static SSLContext insecureSslContext() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new SecureRandom());
            return sc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------- JSON helpers ----------------
    private static String json(String s) {
        if (s == null) return "\"\"";
        String esc = s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + esc + "\"";
    }

    private static String extractJsonString(String json, String key) {
        if (json == null) return null;
        String k = "\"" + key + "\"";
        int i = json.indexOf(k);
        if (i < 0) return null;
        int colon = json.indexOf(':', i);
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
        return raw.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}