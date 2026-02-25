package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import models.Reclamation;
import models.User;
import services.ReclamationService;
import services.SpeechToTextService;
import utils.UserSession;
import javafx.application.Platform;

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
    @FXML
    private Button micButton;
    @FXML
    private Label micStatusLabel;

    private ToggleGroup urgenceGroup;
    private final ReclamationService reclamationService = new ReclamationService();
    private final SpeechToTextService speechToTextService = new SpeechToTextService();
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
    private void handleMicrophone() {
        if (speechToTextService.isRecording()) {
            // Stop recording
            speechToTextService.stopListening();
            micButton.setText("🎤 Dictée vocale");
            micStatusLabel.setText("");
        } else {
            // Start recording
            micButton.setText("🛑 Arrêter");
            micStatusLabel.setText("Écoute en cours...");

            speechToTextService.startListening(
                    text -> Platform.runLater(() -> {
                        String currentText = descriptionArea.getText();
                        if (!currentText.isEmpty() && !currentText.endsWith(" ")) {
                            currentText += " ";
                        }
                        descriptionArea.setText(currentText + text);
                    }),
                    error -> Platform.runLater(() -> {
                        micStatusLabel.setText(error);
                        micButton.setText("🎤 Dictée vocale");
                        showAlert(Alert.AlertType.ERROR, "Erreur Microphone", error);
                    }));
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
        if (speechToTextService.isRecording()) {
            handleMicrophone(); // Stop recording if active
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
