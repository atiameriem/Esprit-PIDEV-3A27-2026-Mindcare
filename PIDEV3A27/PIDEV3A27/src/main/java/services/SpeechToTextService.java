package services;

import javafx.application.Platform;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.File;
import java.util.function.Consumer;

public class SpeechToTextService {

    private static Model model; // ✅ chargé une seule fois
    private boolean isRecording = false;
    private TargetDataLine microphone;
    private Thread recognitionThread;

    public SpeechToTextService() {
        loadModel();
    }

    private void loadModel() {
        try {
            if (model == null) {
                String path1 = java.nio.file.Paths
                        .get("src", "main", "resources", "vosk_resources", "vosk-model-small-fr-0.22").toAbsolutePath()
                        .toString();
                String path2 = java.nio.file.Paths.get("PIDEV3A27", "PIDEV3A27", "src", "main", "resources",
                        "vosk_resources", "vosk-model-small-fr-0.22").toAbsolutePath().toString();
                String path3 = "C:\\Users\\eyabd\\OneDrive\\Bureau\\service_user_rec\\PIDEV3A27\\PIDEV3A27\\src\\main\\resources\\vosk_resources\\vosk-model-small-fr-0.22";

                String modelPath = null;
                if (new File(path1).exists()) {
                    modelPath = path1;
                } else if (new File(path2).exists()) {
                    modelPath = path2;
                } else if (new File(path3).exists()) {
                    modelPath = path3;
                }

                if (modelPath == null || !new File(modelPath).exists()) {
                    System.err.println("❌ VOSK: Modèle introuvable. Chemins testés :");
                    System.err.println("1: " + path1);
                    System.err.println("2: " + path2);
                    System.err.println("3: " + path3);
                    return; // On ne lance pas d'exception fatale ici, on quitte juste
                }

                model = new Model(modelPath);
                System.out.println("✅ Modèle chargé une seule fois");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startListening(Consumer<String> onResult, Consumer<String> onError) {

        if (isRecording)
            return;

        isRecording = true;

        recognitionThread = new Thread(() -> {
            try {
                // S'assurer que le modèle est bien chargé avant de le donner à Recognizer
                // (sinon crash JNA memory access)
                if (model == null) {
                    loadModel();
                    if (model == null) {
                        Platform.runLater(
                                () -> onError.accept("Erreur : Impossible de charger le dossier du modèle vocal."));
                        isRecording = false;
                        return;
                    }
                }

                Recognizer recognizer = new Recognizer(model, 16000.0f);

                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    // Fallback to searching all mixers for a compatible line
                    boolean found = false;
                    for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                        Mixer mixer = AudioSystem.getMixer(mixerInfo);
                        if (mixer.isLineSupported(info)) {
                            microphone = (TargetDataLine) mixer.getLine(info);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new RuntimeException(
                                "Microphone non supporté avec le format 16000Hz Mono 16bit ! Vérifiez vos paramètres système.");
                    }
                } else {
                    microphone = (TargetDataLine) AudioSystem.getLine(info);
                }

                microphone.open(format);
                microphone.start();

                byte[] buffer = new byte[4096];

                while (isRecording) {

                    int bytesRead = microphone.read(buffer, 0, buffer.length);

                    if (bytesRead > 0) {

                        if (recognizer.acceptWaveForm(buffer, bytesRead)) {

                            String result = recognizer.getResult();
                            String text = extractText(result);

                            if (!text.isEmpty()) {
                                // Fix encoding if the system default isn't UTF-8
                                String utf8Text = new String(
                                        text.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
                                        java.nio.charset.StandardCharsets.UTF_8);
                                Platform.runLater(() -> onResult.accept(utf8Text));
                            }
                        }
                    }
                }

                String finalResult = recognizer.getFinalResult();
                String finalText = extractText(finalResult);

                if (!finalText.isEmpty()) {
                    String utf8FinalText = new String(finalText.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
                            java.nio.charset.StandardCharsets.UTF_8);
                    Platform.runLater(() -> onResult.accept(utf8FinalText));
                }

                microphone.stop();
                microphone.close();
                recognizer.close(); // ✅ on ferme seulement le recognizer

            } catch (Exception e) {

                Platform.runLater(() -> onError.accept("Erreur reconnaissance vocale : " + e.getMessage()));

                e.printStackTrace();
            }

            isRecording = false;
        });

        recognitionThread.setDaemon(true);
        recognitionThread.start();
    }

    public void stopListening() {
        isRecording = false;
    }

    public boolean isRecording() {
        return isRecording;
    }

    private String extractText(String json) {
        try {
            int start = json.indexOf("\"text\"");
            if (start != -1) {
                int firstQuote = json.indexOf("\"", start + 7);
                int secondQuote = json.indexOf("\"", firstQuote + 1);
                return json.substring(firstQuote + 1, secondQuote).trim();
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}