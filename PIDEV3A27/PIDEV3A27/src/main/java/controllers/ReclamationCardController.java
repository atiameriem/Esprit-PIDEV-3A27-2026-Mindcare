package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
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
    @FXML
    private Button editBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private javafx.scene.layout.VBox reponseContainer;
    @FXML
    private Label reponseLabel;
    @FXML
    private Label categorieLabel;
    @FXML
    private HBox urgencyContainer;
    @FXML
    private HBox categorieContainer;

    private Reclamation reclamation;
    private final ReclamationService reclamationService = new ReclamationService();
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

        // Status styling
        boolean traite = "Traité".equalsIgnoreCase(reclamation.getStatut());
        if (traite) {
            statutLabel.setStyle(
                    "-fx-background-color: #c6f6d5; -fx-text-fill: #22543d; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            statutLabel.setStyle(
                    "-fx-background-color: #fbd38d; -fx-text-fill: #744210; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: bold;");
        }

        // Lock edit/delete when Traité
        if (editBtn != null) {
            editBtn.setDisable(traite);
            editBtn.setOpacity(traite ? 0.4 : 1.0);
        }
        if (deleteBtn != null) {
            deleteBtn.setDisable(traite);
            deleteBtn.setOpacity(traite ? 0.4 : 1.0);
        }

        // Response section
        if (reponseContainer != null) {
            String reponse = reclamation.getReponse();
            boolean hasReponse = reponse != null && !reponse.trim().isEmpty();
            reponseContainer.setVisible(hasReponse);
            reponseContainer.setManaged(hasReponse);
            if (hasReponse && reponseLabel != null) {
                reponseLabel.setText(reponse);
            }
        }

        // Categorie section
        if (categorieContainer != null) {
            String cat = reclamation.getCategorie();
            boolean hasCat = cat != null && !cat.trim().isEmpty();
            categorieContainer.setVisible(hasCat);
            categorieContainer.setManaged(hasCat);
            if (hasCat && categorieLabel != null) {
                categorieLabel.setText(cat.toUpperCase());
                categorieContainer.setStyle(
                        "-fx-background-color: #E0E7FF; -fx-padding: 5 12; -fx-background-radius: 8; -fx-border-color: #C7D2FE; -fx-border-width: 1;");
                categorieLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #4338CA;");
            }
        }

        // Final Urgency Sync with container
        switch (urgence) {
            case "HIGH":
            case "HIGHT":
                urgencyContainer
                        .setStyle("-fx-background-color: #FEE2E2; -fx-padding: 5 12; -fx-background-radius: 8;");
                urgenceLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #EF4444;");
                break;
            case "MEDIUM":
                urgencyContainer
                        .setStyle("-fx-background-color: #FEF3C7; -fx-padding: 5 12; -fx-background-radius: 8;");
                urgenceLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #D97706;");
                break;
            default:
                urgencyContainer
                        .setStyle("-fx-background-color: #DCFCE7; -fx-padding: 5 12; -fx-background-radius: 8;");
                urgenceLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #16A34A;");
        }
    }

    @FXML
    private void handleEdit() {
        // Block if already treated
        if ("Traité".equalsIgnoreCase(reclamation.getStatut())) {
            showAlert(Alert.AlertType.WARNING, "Modification impossible",
                    "Cette réclamation a déjà été traitée et ne peut plus être modifiée.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ReclamationDialog.fxml"));
            Parent root = loader.load();

            ReclamationDialogController controller = loader.getController();
            controller.setReclamation(reclamation);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Modifier Réclamation");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(typeLabel.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                reclamationService.update(reclamation);
                setData(reclamation, parentController);
                if (parentController != null)
                    parentController.loadReclamations();
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification : " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        // Block if already treated
        if ("Traité".equalsIgnoreCase(reclamation.getStatut())) {
            showAlert(Alert.AlertType.WARNING, "Suppression impossible",
                    "Cette réclamation a déjà été traitée et ne peut plus être supprimée.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous supprimer cette réclamation ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                reclamationService.delete(reclamation.getId());
                if (parentController != null)
                    parentController.loadReclamations();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
