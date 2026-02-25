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

    // ✅ Constructeur : charge le modèle UNE SEULE FOIS
    public SpeechToTextService() {
        try {
            if (model == null) {
                String modelPath = "C:\\Users\\eyabd\\OneDrive\\Bureau\\PIDEV3A277\\PIDEV3A27\\PIDEV3A27\\src\\main\\resources\\models\\vosk-model-small-fr-0.22";

                if (!new File(modelPath).exists()) {
                    throw new RuntimeException("Modèle introuvable: " + modelPath);
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

                Recognizer recognizer = new Recognizer(model, 16000.0f);

                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    throw new RuntimeException("Microphone non supporté !");
                }

                microphone = (TargetDataLine) AudioSystem.getLine(info);
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