package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import models.Reclamation;
import models.User;
import services.ReclamationService;
import services.SpeechToTextService;
import services.GrokService;
import utils.UserSession;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class ReclamationController {

    @FXML
    private TextField objetField;
    @FXML
    private ToggleButton highToggle;
    @FXML
    private ToggleButton mediumToggle;
    @FXML
    private ToggleButton lowToggle;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Button micButton;
    @FXML
    private Label micStatusLabel;

    private ToggleGroup urgenceGroup;
    private final ReclamationService reclamationService = new ReclamationService();
    private final SpeechToTextService speechToTextService = new SpeechToTextService();
    private final GrokService grokService = new GrokService();
    private Reclamation selectedReclamation = null;

    @FXML
    public void initialize() {
        urgenceGroup = new ToggleGroup();
        highToggle.setToggleGroup(urgenceGroup);
        mediumToggle.setToggleGroup(urgenceGroup);
        lowToggle.setToggleGroup(urgenceGroup);

        // Default selection
        mediumToggle.setSelected(true);
        updateUrgenceStyle();

        // Update styles on toggle change
        urgenceGroup.selectedToggleProperty().addListener((obs, old, newVal) -> updateUrgenceStyle());
    }

    private void updateUrgenceStyle() {
        // Reset all
        setToggleStyle(highToggle, "#FEE2E2", "#DC2626", false);
        setToggleStyle(mediumToggle, "#FEF3C7", "#D97706", false);
        setToggleStyle(lowToggle, "#DCFCE7", "#16A34A", false);

        // Highlight selected
        Toggle selected = urgenceGroup.getSelectedToggle();
        if (selected == highToggle)
            setToggleStyle(highToggle, "#FEE2E2", "#DC2626", true);
        if (selected == mediumToggle)
            setToggleStyle(mediumToggle, "#FEF3C7", "#D97706", true);
        if (selected == lowToggle)
            setToggleStyle(lowToggle, "#DCFCE7", "#16A34A", true);
    }

    private void setToggleStyle(ToggleButton btn, String bgColor, String textColor, boolean selected) {
        String border = selected ? "2.5" : "1.5";
        btn.setStyle(
                "-fx-background-color: " + (selected ? bgColor : "white") + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: " + bgColor + ";" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: " + border + ";" +
                        "-fx-padding: 7 20;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-cursor: hand;");
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
        ToggleButton selectedUrgence = (ToggleButton) urgenceGroup.getSelectedToggle();
        String rawUrgence = (selectedUrgence != null) ? selectedUrgence.getText() : "Medium";
        // Remove emojis, keep only letters
        String urgence = rawUrgence.replaceAll("[^a-zA-Z]", "").trim();

        if (objet == null || objet.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir l'objet et la description.");
            return;
        }

        if (selectedReclamation != null) {
            selectedReclamation.setObjet(objet.trim());
            selectedReclamation.setUrgence(urgence);
            selectedReclamation.setDescription(description.trim());

            // AI for update too
            grokService.classifyReclamation(objet, description)
                    .thenCombine(grokService.summarizeReclamation(objet, description), (cat, sum) -> {
                        selectedReclamation.setCategorie(cat);
                        return sum;
                    })
                    .thenCombine(grokService.suggestProfessional(objet, description), (sum, prof) -> {
                        selectedReclamation.setResume(sum + "\n\n🎯 PROFESSIONNEL CONSEILLÉ : " + prof);
                        return selectedReclamation;
                    })
                    .thenAccept(r -> {
                        try {
                            reclamationService.update(r);
                            Platform.runLater(() -> {
                                showAlert(Alert.AlertType.INFORMATION, "Succès",
                                        "Réclamation modifiée et ré-analysée !");
                                clearFields();
                            });
                        } catch (SQLException e) {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()));
                        }
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur AI",
                                "Échec de l'analyse AI: " + e.getMessage()));
                        return null;
                    });
        } else {
            Reclamation r = new Reclamation();
            r.setIdUser(currentUser.getId());
            r.setObjet(objet.trim());
            r.setUrgence(urgence);
            r.setDescription(description.trim());
            r.setStatut("En attente");

            CompletableFuture<String> classificationFuture = grokService.classifyReclamation(r.getObjet(),
                    r.getDescription());
            CompletableFuture<String> summaryFuture = grokService.summarizeReclamation(r.getObjet(),
                    r.getDescription());
            CompletableFuture<String> profFuture = grokService.suggestProfessional(r.getObjet(),
                    r.getDescription());

            CompletableFuture.allOf(classificationFuture, summaryFuture, profFuture)
                    .thenAccept(v -> {
                        String category = classificationFuture.join();
                        String summary = summaryFuture.join();
                        String prof = profFuture.join();

                        r.setCategorie(category);
                        r.setResume(summary + "\n\n🎯 PROFESSIONNEL CONSEILLÉ : " + prof);

                        try {
                            reclamationService.create(r);
                            Platform.runLater(() -> {
                                showAlert(Alert.AlertType.INFORMATION, "Succès",
                                        "Réclamation envoyée (Classée: " + category + ") !");
                                clearFields();
                            });
                        } catch (SQLException e) {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()));
                        }
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur AI",
                                "Échec de l'analyse AI: " + e.getMessage()));
                        return null;
                    });
        }
    }

    @FXML
    private void handleMicrophone() {
        if (speechToTextService.isRecording()) {
            speechToTextService.stopListening();
            micButton.setText("🎤 Dictée vocale");
            micStatusLabel.setText("");
        } else {
            micButton.setText("🛑 Arrêter");
            micStatusLabel.setText("Écoute...");
            speechToTextService.startListening(
                    text -> Platform.runLater(() -> {
                        String cur = descriptionArea.getText();
                        if (!cur.isEmpty() && !cur.endsWith(" "))
                            cur += " ";
                        descriptionArea.setText(cur + text);
                    }),
                    error -> Platform.runLater(() -> {
                        micStatusLabel.setText("");
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
        mediumToggle.setSelected(true);
        updateUrgenceStyle();
        selectedReclamation = null;
        if (speechToTextService.isRecording())
            handleMicrophone();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}