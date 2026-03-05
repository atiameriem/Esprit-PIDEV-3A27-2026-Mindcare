package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            java.net.URL resource = getClass().getResource("/views/Login.fxml");
            if (resource == null) {
                throw new IOException("Fichier FXML /views/Login.fxml introuvable !");
            }
            Parent root = FXMLLoader.load(resource);
            primaryStage.setTitle("MindCare - Bienvenue");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Erreur fatale au démarrage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
