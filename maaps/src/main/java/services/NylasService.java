package services;

import utils.LocalSecretsStore;
import utils.NylasConfig;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Intégration Nylas (squelette) :
 * - Lance l'OAuth Hosted Auth (ou Connect) dans le navigateur
 * - Récupère le code via localhost callback
 * - Échange le code contre un grant_id
 *
 * NB: Les endpoints exacts peuvent dépendre de la config Nylas (v3). Ce code vise un flux standard.
 */
public class NylasService {

    private static final String STORE_GRANT_ID = "nylas.grant.id";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String getStoredGrantId() {
        return LocalSecretsStore.get(STORE_GRANT_ID);
    }

    public void clearStoredGrantId() throws IOException {
        LocalSecretsStore.put(STORE_GRANT_ID, null);
    }

    /**
     * Lance l'OAuth Nylas dans le navigateur et retourne un grant_id.
     */
    public CompletableFuture<String> connectAndGetGrantId() {
        String clientId = NylasConfig.clientId();
        String apiKey = NylasConfig.apiKey();
        if (clientId.isBlank() || apiKey.isBlank()) {
            CompletableFuture<String> f = new CompletableFuture<>();
            f.completeExceptionally(new IllegalStateException("Veuillez renseigner nylas.client.id et nylas.api.key dans config.properties"));
            return f;
        }

        URI redirect = URI.create(NylasConfig.redirectUri());
        int port = redirect.getPort() == -1 ? 80 : redirect.getPort();
        String path = redirect.getPath();
        if (path == null || path.isBlank()) path = "/callback";

        String state = UUID.randomUUID().toString();

        // Démarrer serveur callback
        NylasOAuthCallbackServer cb = new NylasOAuthCallbackServer();
        CompletableFuture<Map<String, String>> cbFuture;
        try {
            cbFuture = cb.start(port, path);
        } catch (IOException e) {
            CompletableFuture<String> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }

        // Ouvrir navigateur
        String authUrl = buildAuthUrl(state);
        openBrowser(authUrl);

        // Quand callback arrive: échange code -> grant_id
        return cbFuture.thenCompose(params -> {
            String code = params.get("code");
            String returnedState = params.get("state");
            if (code == null || code.isBlank()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Callback reçu sans code. Paramètres: " + params));
            }
            if (returnedState != null && !returnedState.isBlank() && !returnedState.equals(state)) {
                return CompletableFuture.failedFuture(new IllegalStateException("State OAuth invalide"));
            }
            return exchangeCodeForGrantId(code).thenApply(grantId -> {
                try {
                    LocalSecretsStore.put(STORE_GRANT_ID, grantId);
                } catch (IOException ignored) {}
                return grantId;
            });
        });
    }

    /**
     * Construit l'URL d'authentification.
     * IMPORTANT: selon votre configuration Nylas, vous pouvez préférer l'URL "Hosted Authentication" fournie par le dashboard.
     */
    public String buildAuthUrl(String state) {
        String base = NylasConfig.baseUrl();
        String clientId = NylasConfig.clientId();
        String redirectUri = NylasConfig.redirectUri();
        String provider = NylasConfig.provider();

        // Scope minimal calendrier (adapter selon besoins)
        String scope = "calendar";

        return base + "/v3/connect/auth?" +
                "client_id=" + enc(clientId) +
                "&redirect_uri=" + enc(redirectUri) +
                "&response_type=code" +
                "&access_type=offline" +
                "&provider=" + enc(provider) +
                "&scope=" + enc(scope) +
                "&state=" + enc(state);
    }

    /**
     * Échange le code OAuth contre un grant_id.
     */
    public CompletableFuture<String> exchangeCodeForGrantId(String code) {
        String base = NylasConfig.baseUrl();
        String apiKey = NylasConfig.apiKey();
        String clientId = NylasConfig.clientId();
        String redirectUri = NylasConfig.redirectUri();

        String json = "{" +
                "\"grant_type\":\"authorization_code\"," +
                "\"code\":" + q(code) + "," +
                "\"client_id\":" + q(clientId) + "," +
                "\"redirect_uri\":" + q(redirectUri) +
                "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/v3/connect/token"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        throw new RuntimeException("Nylas token exchange failed: " + resp.statusCode() + "\n" + resp.body());
                    }
                    String grantId = extractJsonString(resp.body(), "grant_id");
                    if (grantId == null) {
                        // Certains retours utilisent "grant" {"id": ...}
                        grantId = extractJsonString(resp.body(), "id");
                    }
                    if (grantId == null) {
                        throw new RuntimeException("Impossible d'extraire grant_id. Réponse: " + resp.body());
                    }
                    return grantId;
                });
    }

    /**
     * Crée un événement calendrier via Nylas.
     * calendarId: souvent "primary" (Google) mais peut varier selon provider.
     */
    public CompletableFuture<Void> createEvent(String grantId, String calendarId,
                                              String title, String description,
                                              String startIso, String endIso,
                                              String reservationId) {
        String base = NylasConfig.baseUrl();
        String apiKey = NylasConfig.apiKey();

        String json = "{" +
                "\"title\":" + q(title) + "," +
                "\"description\":" + q(description) + "," +
                "\"when\":{" +
                "\"start_time\":" + q(startIso) + "," +
                "\"end_time\":" + q(endIso) +
                "}," +
                "\"metadata\":{" +
                "\"reservationId\":" + q(reservationId) +
                "}" +
                "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/v3/grants/" + enc(grantId) + "/events?calendar_id=" + enc(calendarId)))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        throw new RuntimeException("Nylas create event failed: " + resp.statusCode() + "\n" + resp.body());
                    }
                    return null;
                });
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {}
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String q(String s) {
        String v = s == null ? "" : s;
        v = v.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + v + "\"";
    }

    // Parser JSON minimaliste: récupère la valeur string d'une clé "key":"value"
    private static String extractJsonString(String json, String key) {
        if (json == null) return null;
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        return json.substring(firstQuote + 1, secondQuote);
    }
}
