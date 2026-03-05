package utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Charge les variables d'environnement depuis le fichier .env
 */
public class EnvConfig {
    private static final Map<String, String> vars = new HashMap<>();

    static {
        charger();
    }

    private static void charger() {
        String[] chemins = {".env", "../.env", System.getProperty("user.dir") + "/.env"};
        for (String chemin : chemins) {
            Path p = Path.of(chemin);
            if (Files.exists(p)) {
                try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    String ligne;
                    while ((ligne = br.readLine()) != null) {
                        ligne = ligne.trim();
                        if (ligne.isEmpty() || ligne.startsWith("#")) continue;
                        int eq = ligne.indexOf('=');
                        if (eq > 0) {
                            vars.put(ligne.substring(0, eq).trim(), ligne.substring(eq + 1).trim().replace("\"", "").replace("'", ""));
                        }
                    }
                    return;
                } catch (IOException e) { }
            }
        }
    }

    public static String get(String cle) {
        String val = vars.get(cle);
        if (val != null && !val.isEmpty()) return val;
        val = System.getenv(cle);
        return val != null ? val : "";
    }
}
