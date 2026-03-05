package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Reclamation;
import services.ReclamationService;

import java.sql.SQLException;

public class AdminRepondreController {

    @FXML
    private Label objetHeaderLabel;
    @FXML
    private Label userNameDetail;
    @FXML
    private Label objetDetail;
    @FXML
    private Label descriptionDetail;
    @FXML
    private TextArea reponseArea;
    @FXML
    private VBox summaryBoxIA;
    @FXML
    private Label summaryLabelIA;
    @FXML
    private Label categorieDetail;
    @FXML
    private VBox categorieBox;

    private Stage stage;
    private Reclamation reclamation;
    private AdminReclamationsController parentController;
    private final ReclamationService reclamationService = new ReclamationService();

    /**
     * Ouvre le dialog de réponse admin en modal.
     */
    public static void open(Reclamation reclamation, String userName, AdminReclamationsController parent) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AdminRepondreController.class.getResource("/views/AdminRepondre.fxml"));
            Parent root = loader.load();

            AdminRepondreController ctrl = loader.getController();
            ctrl.setup(reclamation, userName, parent);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Répondre - " + reclamation.getObjet());
            dialog.setScene(new Scene(root));
            ctrl.stage = dialog;
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setup(Reclamation reclamation, String userName, AdminReclamationsController parent) {
        this.reclamation = reclamation;
        this.parentController = parent;

        objetHeaderLabel.setText(reclamation.getObjet());
        userNameDetail.setText(userName);
        objetDetail.setText(reclamation.getObjet());
        descriptionDetail.setText(reclamation.getDescription());

        // AI Summary in Dialog
        if (summaryBoxIA != null) {
            String resume = reclamation.getResume();
            boolean hasResume = resume != null && !resume.trim().isEmpty();
            summaryBoxIA.setVisible(hasResume);
            summaryBoxIA.setManaged(hasResume);
            if (hasResume && summaryLabelIA != null) {
                summaryLabelIA.setText(resume);
            }
        }

        // Category in Dialog
        if (categorieBox != null) {
            String cat = reclamation.getCategorie();
            boolean hasCat = cat != null && !cat.trim().isEmpty();
            categorieBox.setVisible(hasCat);
            categorieBox.setManaged(hasCat);
            if (hasCat && categorieDetail != null) {
                categorieDetail.setText(cat.toUpperCase());
            }
        }
    }

    @FXML
    private void handleRepondre() {
        String reponse = reponseArea.getText();
        if (reponse == null || reponse.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("Veuillez entrer une réponse avant de valider.");
            alert.showAndWait();
            return;
        }

        try {
            // Mark as Traité
            reclamation.setStatut("Traité");
            reclamation.setReponse(reponse);
            // Store the response in the database
            reclamationService.update(reclamation);

            if (parentController != null)
                parentController.refresh();
            if (stage != null)
                stage.close();

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("Erreur : " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleCancel() {
        if (stage != null)
            stage.close();
    }
}
