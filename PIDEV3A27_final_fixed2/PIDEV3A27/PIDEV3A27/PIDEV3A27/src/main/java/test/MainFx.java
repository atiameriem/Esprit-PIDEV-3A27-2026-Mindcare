package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFx extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Démarrer par l'écran de session (login)
        Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
        Scene scene = new Scene(root, 520, 360);
        stage.setTitle("MindCare - Connexion");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
