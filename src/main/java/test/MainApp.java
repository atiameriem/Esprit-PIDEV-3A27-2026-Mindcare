package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
        Scene scene = new Scene(root, 1200, 700);
        stage.setTitle("MindCare");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        // Ajouter GStreamer au PATH pour JavaFX Media (fxplugins.dll)
        String gstreamerBin = "C:\\Program Files\\gstreamer\\1.0\\msvc_x86_64\\bin";
        if (new File(gstreamerBin).exists()) {
            String currentPath = System.getenv("PATH");
            if (currentPath == null || !currentPath.contains("gstreamer")) {
                // Modifier le PATH du processus courant via ProcessBuilder trick
                try {
                    ProcessBuilder pb = new ProcessBuilder();
                    pb.environment().put("PATH", gstreamerBin + ";" + currentPath);
                    // Also set system property
                    String libPath = System.getProperty("java.library.path");
                    System.setProperty("java.library.path",
                            gstreamerBin + ";" + (libPath != null ? libPath : ""));
                    System.out.println("✅ GStreamer ajouté au PATH: " + gstreamerBin);
                } catch (Exception e) {
                    System.err.println("⚠ Impossible d'ajouter GStreamer au PATH: " + e.getMessage());
                }
            }
        } else {
            System.out.println("⚠ GStreamer non trouvé dans: " + gstreamerBin);
        }

        launch(args);
    }
}
