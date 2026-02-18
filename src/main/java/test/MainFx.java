package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFx extends Application {


    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/MindCareLayout.fxml"));
        Scene scene = new Scene(root, 1200, 700);
        stage.setTitle("MindCare");
        stage.setScene(scene);
        stage.show();

        // Pour passer à MindCarepatient.fxml plus tard :
        // Parent root1 = FXMLLoader.load(getClass().getResource("/MindCareLayoutpatient.fxml"));
        // Scene scene1 = new Scene(root1, 1200, 700);
        // stage.setScene(scene1);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
