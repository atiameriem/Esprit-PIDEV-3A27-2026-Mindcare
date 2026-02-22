package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
//C’est la classe principale qui lance l’application graphique.

// Ça veut dire Ma classe devient une application JavaFX.
public class MainFx extends Application {

    //JavaFX va automatiquement appeler la méthode start()
    @Override
    //stage = la fenêtre principale
    public void start(Stage stage) throws Exception {
        //charge un fichier FXML
        //on va testser session
        //Parent root : est le conteneur principal de la scène.
        Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
        //Scene = ce qu’on affiche dans la fenêtre
        //root = l’interface chargée ,largezuer et( hauteur
        Scene scene = new Scene(root, 520, 360);
        stage.setTitle("MindCare - Connexion");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}