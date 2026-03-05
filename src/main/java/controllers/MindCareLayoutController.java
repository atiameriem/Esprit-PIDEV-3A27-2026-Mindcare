package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.Session;

import java.io.IOException;

public class MindCareLayoutController {

    @FXML private VBox contentArea;

    // Labels dans sidebar
    @FXML private Label profileNameLabel;
    @FXML private Label profileRoleLabel;

    // ✅ Bouton stats dans sidebar (ajoute fx:id="btnStats" dans MindCareLayout.fxml)
    @FXML private Button btnStats;

    @FXML
    public void initialize() {
        // Afficher nom + rôle
        if (profileNameLabel != null) {
            profileNameLabel.setText(Session.getFullName() == null ? "" : Session.getFullName());
        }
        if (profileRoleLabel != null) {
            if (Session.isPsychologue()) profileRoleLabel.setText("Rôle : Psychologue");
            else if (Session.isPatient()) profileRoleLabel.setText("Rôle : Patient");
            else profileRoleLabel.setText("");
        }

        // ✅ Cacher Statistiques si patient
        if (btnStats != null) {
            boolean isPsy = Session.isPsychologue();
            btnStats.setVisible(isPsy);
            btnStats.setManaged(isPsy); // مهم: كي يختفي ماياخذش place
        }

        // Charger accueil par défaut
        loadAccueil();
    }

    @FXML
    public void loadRendezVousStats() {
        if (!Session.isPsychologue()) return;
        loadView("RendezVousStats.fxml");
    }

    /**
     * Charger une vue FXML dans la zone de contenu (centre)
     */
    public void loadView(String fxmlFile) {
        try {
            // ✅ sécurité: si contentArea non injectée, essaye lookup
            if (contentArea == null) {
                // chercher dans la scene actuelle (si possible)
                // (si contentArea est dans le layout, donne-lui aussi id="contentArea" dans le FXML)
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxmlFile));
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la vue: " + fxmlFile);
            e.printStackTrace();
        }
    }

    // ===== Navigation =====

    @FXML
    private void loadAccueil() {
        loadView("Accueil.fxml");
    }

    @FXML
    private void loadRendezVous() {
        if (Session.isPsychologue()) {
            loadView("RendezVous.fxml");
        } else {
            loadView("RendezVousCrud.fxml");
        }
    }

    @FXML
    private void loadCompteRendu() {
        if (Session.isPsychologue()) {
            loadView("CompteRendu.fxml");
        } else {
            loadView("CompteRenduRead.fxml");
        }
    }

    @FXML private void loadForum() { loadView("Forum.fxml"); }
    @FXML private void loadChatbot() { loadView("Chatbot.fxml"); }
    @FXML private void loadPasserTests() { loadView("PasserTests.fxml"); }
    @FXML private void loadSuivie() { loadView("Suivie.fxml"); }
    @FXML private void loadProfil() { loadView("Profil.fxml"); }
    @FXML private void loadReclamation() { loadView("Reclamation.fxml"); }
    @FXML private void loadReserverFormation() { loadView("ReserverFormation.fxml"); }
    @FXML private void loadConsulterSupport() { loadView("ConsulterSupport.fxml"); }
    @FXML private void loadLocaux() { loadView("Locaux.fxml"); }

    // ===== Logout =====
    @FXML
    private void handleLogout() {
        try {
            Session.logout();
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Scene scene = new Scene(root, 520, 360);
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setTitle("MindCare - Connexion");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}