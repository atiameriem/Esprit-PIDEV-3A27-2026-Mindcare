package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;

public class ConsulterSupportController {

    @FXML
    private ProgressBar progressBar;

    @FXML
    public void initialize() {
        progressBar.setProgress(0.6);
    }

    /* ========================= */
    /* ===== ACTIONS HEADER ==== */
    /* ========================= */

    @FXML
    private void inscrireCours() {
        showMessage("Inscription",
                "Vous êtes maintenant inscrit au cours !");
    }

    /* ========================= */
    /* ===== CONTENU PRINCIPAL == */
    /* ========================= */

    @FXML
    private void playPodcast() {
        showMessage("Podcast",
                "Lecture du podcast en cours...");
    }

    @FXML
    private void openPrincipes() {
        showMessage("Document",
                "Ouverture du document PDF...");
    }

    @FXML
    private void playVideo() {
        showMessage("Vidéo",
                "Lecture de la vidéo...");
    }

    /* ========================= */
    /* ===== AUTRES MODULES ==== */
    /* ========================= */

    @FXML
    private void openExercice() {
        showMessage("Exercice",
                "Ouverture de l'exercice pratique...");
    }

    @FXML
    private void openCaseStudy() {
        showMessage("Étude de cas",
                "Ouverture de l'étude de cas...");
    }

    /* ========================= */
    /* ===== UTILITAIRE ======== */
    /* ========================= */

    private void showMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
