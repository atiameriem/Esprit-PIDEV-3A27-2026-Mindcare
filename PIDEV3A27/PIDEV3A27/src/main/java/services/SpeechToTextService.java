package services;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;
/**
 * Speech-to-Text offline (Vosk).
 *
 * Utilisation:
 * - start(text -> ...)  // callback sur texte reconnu
 * - stop()
 */
public class SpeechToTextService {

    private volatile boolean running;
    private Thread worker;

    // Dernier résultat final complet retourné par Vosk.
    private String lastFinalText = "";

    public boolean isRunning() {
        return running;
    }

    public void start(Consumer<String> onText) {
        if (running) return;
        running = true;
        lastFinalText = "";

        worker = new Thread(() -> {
            Path modelDir = resolveModelDir();

            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            try (Model model = new Model(modelDir.toString())) {

                TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
                mic.open(format);
                mic.start();

                try (Recognizer rec = new Recognizer(model, 16000.0f)) {
                    byte[] buffer = new byte[4096];

                    while (running) {
                        int n = mic.read(buffer, 0, buffer.length);
                        if (n <= 0) continue;

                        boolean isFinal = rec.acceptWaveForm(buffer, n);
                        if (!isFinal) continue; // ignore partialResult

                        // Uniquement les résultats finaux
                        String json = rec.getResult();
                        String full = extractValue(json, "text");
                        if (full == null) continue;

                        full = fixUtf8IfBroken(full).trim();
                        if (full.isBlank()) continue;

                        // Anti-duplication: Vosk peut répéter / renvoyer l'historique.
                        String delta = computeDelta(lastFinalText, full);
                        lastFinalText = full;
                        delta = fixUtf8IfBroken(delta);
                        if (!delta.isBlank()) onText.accept(delta);
                    }

                } finally {
                    mic.stop();
                    mic.close();
                }

            } catch (Exception e) {
                onText.accept("[STT ERROR] " + e.getMessage());
            }
        }, "vosk-stt");

        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running = false;
    }

    // =========================================================
    // Alias — API utilisée par ReclamationController (module officiel)
    // Délègue vers les méthodes existantes du module Forum.
    // =========================================================

    /** Alias de {@link #isRunning()} — compatibilité ReclamationController. */
    public boolean isRecording() {
        return isRunning();
    }

    /**
     * Alias de {@link #start(java.util.function.Consumer)} avec gestion d'erreur séparée.
     * Compatibilité ReclamationController (module officiel).
     */
    public void startListening(java.util.function.Consumer<String> onResult,
                               java.util.function.Consumer<String> onError) {
        start(text -> {
            if (text != null && text.startsWith("[STT ERROR]")) {
                if (onError != null) onError.accept(text);
            } else {
                if (onResult != null) onResult.accept(text);
            }
        });
    }

    /** Alias de {@link #stop()} — compatibilité ReclamationController. */
    public void stopListening() {
        stop();
    }

    private static String computeDelta(String prevFull, String newFull) {
        if (prevFull == null) prevFull = "";
        if (newFull == null) return "";
        prevFull = prevFull.trim();
        newFull = newFull.trim();
        if (newFull.isEmpty()) return "";

        if (prevFull.isEmpty()) return newFull;
        if (newFull.equals(prevFull)) return "";

        // Cas simple: le nouveau commence par l'ancien
        if (newFull.startsWith(prevFull)) {
            return newFull.substring(prevFull.length()).trim();
        }

        // Déduplication par mots (longest common prefix sur tokens)
        String[] a = prevFull.split("\\s+");
        String[] b = newFull.split("\\s+");
        int i = 0;
        while (i < a.length && i < b.length && a[i].equalsIgnoreCase(b[i])) i++;
        if (i >= b.length) return "";

        StringBuilder sb = new StringBuilder();
        for (int j = i; j < b.length; j++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(b[j]);
        }
        return sb.toString().trim();
    }

    private Path resolveModelDir() {
        // 1) chemin dev (recommandé)
        Path p1 = Path.of("src/main/resources/vosk/vosk-model-small-fr-0.22");
        if (Files.exists(p1.resolve("conf").resolve("model.conf"))) return p1;

        // 2) alternative: dossier à côté du projet
        Path p2 = Path.of("vosk-model-small-fr-0.22");
        if (Files.exists(p2.resolve("conf").resolve("model.conf"))) return p2;

        throw new IllegalStateException(
                "Modèle Vosk introuvable. Place le dossier 'vosk-model-small-fr-0.22' dans " +
                        "src/main/resources/vosk/ (ou à la racine du projet)."
        );
    }

    private String extractValue(String json, String key) {
        // parsing JSON minimal (sans dépendance)
        if (json == null) return null;
        String k = "\"" + key + "\"";
        int i = json.indexOf(k);
        if (i < 0) return null;
        int colon = json.indexOf(':', i);
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }
    private static String fixUtf8IfBroken(String s) {
        if (s == null) return null;

        // Heuristique : si on voit Ã / â etc, c'est souvent un UTF-8 lu comme ISO-8859-1
        if (s.contains("Ã") || s.contains("â") || s.contains("€") || s.contains("™")) {
            return new String(s.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        }
        return s;
    }
}
