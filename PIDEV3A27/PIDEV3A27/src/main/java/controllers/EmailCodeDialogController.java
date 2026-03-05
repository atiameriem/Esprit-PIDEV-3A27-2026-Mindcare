package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class EmailCodeDialogController {

    @FXML
    private Label emailLabel;
    @FXML
    private HBox digitRow;

    private Stage stage;

    /**
     * Ouvre le dialog FXML avec le code affiché sous forme de cases stylisées.
     */
    public static void show(String code, String email) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    EmailCodeDialogController.class.getResource("/views/EmailCodeDialog.fxml"));
            Parent root = loader.load();

            EmailCodeDialogController ctrl = loader.getController();
            ctrl.setup(code, email);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.TRANSPARENT);
            dialog.setTitle("Code de réinitialisation");
            ctrl.stage = dialog;

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialog.setScene(scene);
            dialog.centerOnScreen();
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Configure le dialog avec le code et l'email.
     */
    private void setup(String code, String email) {
        emailLabel.setText("Envoyé à " + email);

        // Créer une boîte par chiffre
        for (char c : code.toCharArray()) {
            Label digit = new Label(String.valueOf(c));
            digit.setPrefSize(52, 62);
            digit.setAlignment(Pos.CENTER);
            digit.setStyle(
                    "-fx-font-size: 28px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #4DABF7;" +
                            "-fx-font-family: 'Consolas';" +
                            "-fx-background-color: #1E2328;" +
                            "-fx-border-color: #2D3139;" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;");
            digitRow.getChildren().add(digit);
        }
    }

    @FXML
    private void handleClose() {
        if (stage != null)
            stage.close();
    }
}
