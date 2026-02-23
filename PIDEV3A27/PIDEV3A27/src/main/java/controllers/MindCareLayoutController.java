package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.User;
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
    private Label usernameLabel;
    @FXML
    private Button accueilBtn;
    @FXML
    private TitledPane consultationPane;
    @FXML
    private TitledPane forumPane;
    @FXML
    private TitledPane testPane;
    @FXML
    private TitledPane comptePane;
    @FXML
    private TitledPane formationPane;
    @FXML
    private Button locauxBtn;

    @FXML
    public void initialize() {
        if (sortCombo != null) {
            sortCombo.getItems().setAll("Plus récents", "Plus anciens", "Plus aimés");
            sortCombo.getSelectionModel().selectFirst();
        }

        // Apply Role-Based Visibility
        applyRoleConstraints();

        // Charger la page d'accueil par défaut
        loadAccueil();
    }

    private void applyRoleConstraints() {
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser != null) {
            String name = (currentUser.getPrenom() != null ? currentUser.getPrenom() : "") + " " +
                    (currentUser.getNom() != null ? currentUser.getNom() : "");
            usernameLabel.setText(name.trim().isEmpty() ? "Utilisateur" : name);

            if (currentUser.getRole() == User.Role.Admin) {
                // Admin specific view: Focus on Management
                consultationPane.setVisible(false);
                consultationPane.setManaged(false);
                forumPane.setVisible(false);
                forumPane.setManaged(false);
                testPane.setVisible(false);
                testPane.setManaged(false);
                formationPane.setVisible(false);
                formationPane.setManaged(false);
                // Admin stays with Compte (for User Mgmt) and Reclamation (for Treatment)
            } else {
                // Patient / Psychologue: usage views
                // They see everything except maybe some admin-only tabs inside views
            }
        }
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
