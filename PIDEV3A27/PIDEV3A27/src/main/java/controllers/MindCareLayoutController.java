package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.UserSession;

import java.io.IOException;

public class MindCareLayoutController {

    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private TextField searchField;
    @FXML
    private VBox contentArea;

    @FXML
    public void initialize() {
        if (sortCombo != null) {
            sortCombo.getItems().setAll("Plus récents", "Plus anciens", "Plus aimés");
            sortCombo.getSelectionModel().selectFirst();
        }

        // Charger la page d'accueil par défaut
        loadAccueil();
    }

    /**
     * Méthode générique pour charger une vue FXML dans la zone de contenu
     */
    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxmlFile));
            Node view = loader.load();

            // Remplacer le contenu de contentArea
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la vue: " + fxmlFile);
            e.printStackTrace();
        }
    }

    // === Méthodes d'action pour chaque bouton ===

    @FXML
    private void loadAccueil() {
        loadView("Accueil.fxml");
    }

    @FXML
    private void loadRendezVous() {
        loadView("RendezVous.fxml");
    }

    @FXML
    private void loadCompteRendu() {
        loadView("CompteRendu.fxml");
    }

    @FXML
    private void loadForum() {
        loadView("Forum.fxml");
    }

    @FXML
    private void loadChatbot() {
        loadView("Chatbot.fxml");
    }

    @FXML
    private void loadPasserTests() {
        loadView("PasserTests.fxml");
    }

    @FXML
    private void loadSuivie() {
        loadView("Suivie.fxml");
    }

    @FXML
    private void loadProfil() {
        loadView("Profil.fxml");
    }

    @FXML
    private void loadReclamation() {
        loadView("ReclamationTabs.fxml");
    }

    @FXML
    private void loadReserverFormation() {
        loadView("ReserverFormation.fxml");
    }

    @FXML
    private void loadConsulterSupport() {
        loadView("ConsulterSupport.fxml");
    }

    @FXML
    private void loadLocaux() {
        loadView("Locaux.fxml");
    }

    @FXML
    private void handleLogout() {
        try {
            // Nettoyer la session
            UserSession.getInstance().cleanUserSession();

            // Charger la page de Login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Parent root = loader.load();

            // Remplacer la scène actuelle
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setTitle("MindCare - Connexion");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
