package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import models.User;
import utils.UserSession;
import javafx.event.ActionEvent;

import java.io.IOException;

public class MindCareLayoutController {

    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private TextField searchField;
    @FXML
    private VBox contentArea;
    @FXML
    private Label userNameLabel;

    @FXML
    public void initialize() {
        if (sortCombo != null) {
            sortCombo.getItems().setAll("Plus récents", "Plus anciens", "Plus aimés");
            sortCombo.getSelectionModel().selectFirst();
        }

        // Afficher le nom de l'utilisateur connecté
        User user = UserSession.getInstance().getUser();
        if (user != null) {
            userNameLabel.setText(user.getNom() + " " + user.getPrenom());
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
        loadView("Reclamation.fxml");
    }

    @FXML
    private void loadReserverFormation() {
        loadView("Formation.fxml");
    }

    @FXML
    private void loadConsulterSupport() {
        loadView("Statistiques.fxml");
    }

    @FXML
    private void loadLocaux() {
        loadView("Locaux.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Clean session
        UserSession.getInstance().cleanUserSession();

        // Navigate back to Login
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
