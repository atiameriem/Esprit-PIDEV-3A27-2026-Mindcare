package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import models.Reclamation;
import models.User;
import services.ReclamationService;
import utils.UserSession;

import java.sql.SQLException;

public class ReclamationController {

    @FXML
    private TextField objetField;
    @FXML
    private RadioButton highRadio;
    @FXML
    private RadioButton mediumRadio;
    @FXML
    private RadioButton lowRadio;
    @FXML
    private TextArea descriptionArea;

    private ToggleGroup urgenceGroup;
    private final ReclamationService reclamationService = new ReclamationService();
    private Reclamation selectedReclamation = null;

    @FXML
    public void initialize() {
        urgenceGroup = new ToggleGroup();
        highRadio.setToggleGroup(urgenceGroup);
        mediumRadio.setToggleGroup(urgenceGroup);
        lowRadio.setToggleGroup(urgenceGroup);

        // Par défaut
        mediumRadio.setSelected(true);
    }

    @FXML
    private void handleEnvoyer() {
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté.");
            return;
        }

        String objet = objetField.getText();
        String description = descriptionArea.getText();
        RadioButton selectedUrgence = (RadioButton) urgenceGroup.getSelectedToggle();
        String urgence = (selectedUrgence != null) ? selectedUrgence.getText() : "Medium";

        if (objet == null || objet.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir l'objet et la description.");
            return;
        }

        try {
            if (selectedReclamation != null) {
                selectedReclamation.setObjet(objet.trim());
                selectedReclamation.setUrgence(urgence);
                selectedReclamation.setDescription(description.trim());
                reclamationService.update(selectedReclamation);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation modifiée.");
            } else {
                Reclamation r = new Reclamation();
                r.setIdUser(currentUser.getId());
                r.setObjet(objet.trim());
                r.setUrgence(urgence);
                r.setDescription(description.trim());
                r.setStatut("En attente");

                reclamationService.create(r);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation ajoutée.");
            }
            clearFields();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        clearFields();
    }

    private void clearFields() {
        objetField.clear();
        descriptionArea.clear();
        mediumRadio.setSelected(true);
        selectedReclamation = null;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
