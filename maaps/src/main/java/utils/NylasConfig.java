package utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * Centralise la configuration Nylas.
 * Les valeurs sont lues depuis: src/main/resources/config.properties
 */
public final class NylasConfig {

    private static final Properties P = new Properties();

    static {
        try (InputStream is = NylasConfig.class.getResourceAsStream("/config.properties")) {
            if (is == null) {
                throw new IllegalStateException("config.properties introuvable dans resources");
            }
            P.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger config.properties", e);
        }
    }

    private NylasConfig() {}

    public static String apiKey() {
        return P.getProperty("nylas.api.key", "").trim();
    }

    public static String clientId() {
        return P.getProperty("nylas.client.id", "").trim();
    }

    public static String baseUrl() {
        return P.getProperty("nylas.base.url", "https://api.us.nylas.com").trim();
    }

    public static String redirectUri() {
        return P.getProperty("nylas.redirect.uri", "http://localhost:7777/callback").trim();
    }

    public static String provider() {
        return P.getProperty("nylas.provider", "google").trim();
    }
}
