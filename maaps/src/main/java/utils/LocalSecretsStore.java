package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Stockage local (user.home) pour enregistrer des infos runtime (ex: grantId Nylas)
 * sans modifier les ressources du projet.
 */
public final class LocalSecretsStore {

    private static final String DIR = ".mindcare";
    private static final String FILE = "secrets.properties";

    private LocalSecretsStore() {}

    private static Path filePath() {
        return Paths.get(System.getProperty("user.home"), DIR, FILE);
    }

    public static synchronized Properties read() {
        Properties p = new Properties();
        Path fp = filePath();
        if (!Files.exists(fp)) return p;
        try (InputStream is = Files.newInputStream(fp)) {
            p.load(is);
        } catch (IOException ignored) {}
        return p;
    }

    public static synchronized void write(Properties p) throws IOException {
        Path fp = filePath();
        Files.createDirectories(fp.getParent());
        try (OutputStream os = Files.newOutputStream(fp)) {
            p.store(os, "MindCare local secrets");
        }
    }

    public static synchronized String get(String key) {
        return read().getProperty(key);
    }

    public static synchronized void put(String key, String value) throws IOException {
        Properties p = read();
        if (value == null) p.remove(key);
        else p.setProperty(key, value);
        write(p);
    }
}
