package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import models.Reclamation;
import models.User;
import services.ReclamationService;
import services.UserService;

import java.sql.SQLException;

public class ReclamationAdminCardController {

    @FXML
    private Label userNameLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label urgencyLabel;
    @FXML
    private Label objetLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label statutLabel;
    @FXML
    private Label doneLabel;
    @FXML
    private Button treatBtn;

    private Reclamation reclamation;
    private AdminReclamationsController parentController;
    private final UserService userService = new UserService();
    private final ReclamationService reclamationService = new ReclamationService();

    public void setData(Reclamation reclamation, AdminReclamationsController parentController) {
        this.reclamation = reclamation;
        this.parentController = parentController;

        // Fetch User Name
        try {
            User user = userService.getById(reclamation.getIdUser());
            if (user != null) {
                userNameLabel.setText(user.getNom() + " " + user.getPrenom());
            } else {
                userNameLabel.setText("Utilisateur #" + reclamation.getIdUser());
            }
        } catch (SQLException e) {
            userNameLabel.setText("Utilisateur #" + reclamation.getIdUser());
            e.printStackTrace();
        }

        dateLabel.setText(reclamation.getDate().toString());
        objetLabel.setText(reclamation.getObjet());
        descriptionLabel.setText(reclamation.getDescription());
        urgencyLabel.setText(reclamation.getUrgence());
        statutLabel.setText(reclamation.getStatut());

        // Update UI based on status
        if ("Traité".equalsIgnoreCase(reclamation.getStatut())) {
            treatBtn.setVisible(false);
            treatBtn.setManaged(false);
            doneLabel.setVisible(true);
            doneLabel.setManaged(true);
            statutLabel.setStyle("-fx-text-fill: #27ae60;");
        } else {
            treatBtn.setVisible(true);
            treatBtn.setManaged(true);
            doneLabel.setVisible(false);
            doneLabel.setManaged(false);
            statutLabel.setStyle("-fx-text-fill: #f39c12;");
        }

        // Urgency color
        if ("HIGH".equalsIgnoreCase(reclamation.getUrgence())) {
            urgencyLabel.setStyle(
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold;");
        } else if ("MEDIUM".equalsIgnoreCase(reclamation.getUrgence())) {
            urgencyLabel.setStyle(
                    "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold;");
        } else {
            urgencyLabel.setStyle(
                    "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleTreat() {
        try {
            reclamation.setStatut("Traité");
            reclamationService.update(reclamation);
            if (parentController != null) {
                parentController.refresh();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
