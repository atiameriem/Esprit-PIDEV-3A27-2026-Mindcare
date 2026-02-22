package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Reclamation;
import services.ReclamationService;

import java.io.IOException;
import java.sql.SQLException;

public class ReclamationCardController {

    @FXML
    private Label typeLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label statutLabel;
    @FXML
    private Label urgenceLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label typeEmoji;
    @FXML
    private StackPane iconContainer;

    private Reclamation reclamation;
    private ReclamationService reclamationService = new ReclamationService();
    private MesReclamationsController parentController;

    public void setData(Reclamation reclamation, MesReclamationsController parentController) {
        this.reclamation = reclamation;
        this.parentController = parentController;

        typeLabel.setText(reclamation.getObjet());
        dateLabel.setText(reclamation.getDate() != null ? reclamation.getDate().toString() : "N/A");
        statutLabel.setText(reclamation.getStatut());
        descriptionLabel.setText(reclamation.getDescription());

        // Urgence styling
        String urgence = reclamation.getUrgence() != null ? reclamation.getUrgence().toUpperCase() : "MEDIUM";
        urgenceLabel.setText(urgence);
        switch (urgence) {
            case "HIGH":
            case "HIGHT":
                urgenceLabel.setStyle(
                        "-fx-background-color: #e53e3e; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
                break;
            case "MEDIUM":
                urgenceLabel.setStyle(
                        "-fx-background-color: #ed8936; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
                break;
            case "LOW":
                urgenceLabel.setStyle(
                        "-fx-background-color: #48bb78; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
                break;
            default:
                urgenceLabel.setStyle(
                        "-fx-background-color: #cbd5e0; -fx-text-fill: #4a5568; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
        }

        // Dynamic styling for status
        if ("En attente".equalsIgnoreCase(reclamation.getStatut())) {
            statutLabel.setStyle(
                    "-fx-background-color: #fbd38d; -fx-text-fill: #744210; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else if ("Traité".equalsIgnoreCase(reclamation.getStatut())) {
            statutLabel.setStyle(
                    "-fx-background-color: #c6f6d5; -fx-text-fill: #22543d; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleEdit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ReclamationDialog.fxml"));
            Parent root = loader.load();

            ReclamationDialogController controller = loader.getController();
            controller.setReclamation(reclamation);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Modifier Réclamation");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(typeLabel.getScene().getWindow());
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                reclamationService.update(reclamation);
                setData(reclamation, parentController); // Update current card
                if (parentController != null) {
                    parentController.loadReclamations(); // Refresh grid
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Erreur lors de la modification : " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous supprimer cette réclamation ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                reclamationService.delete(reclamation.getId());
                if (parentController != null) {
                    parentController.loadReclamations();
                }
            } catch (SQLException e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setContentText(e.getMessage());
                error.showAndWait();
            }
        }
    }
}
