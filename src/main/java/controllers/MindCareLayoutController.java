package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import utils.Session;

import java.io.IOException;
import java.net.URL;

public class MindCareLayoutController {

    @FXML private Button btnAccueil;
    @FXML private Button btnRendezVous;
    @FXML private Button btnCompteRendu;
    @FXML private Button btnForum;
    @FXML private Button btnChatbot;
    @FXML private Button btnPasserTests;
    @FXML private Button btnSuivie;
    @FXML private Button btnProfil;
    @FXML private Button btnReclamation;
    @FXML private Button btnReserverFormation;
    @FXML private Button btnConsulterSupport;
    @FXML private Button btnLocaux;

    @FXML private ComboBox<String> sortCombo;
    @FXML private TextField        searchField;
    @FXML private VBox             contentArea;

    @FXML
    public void initialize() {
        if (sortCombo != null) {
            sortCombo.getItems().setAll("Plus récents", "Plus anciens", "Plus aimés");
            sortCombo.getSelectionModel().selectFirst();
        }
        appliquerPermissions();
        chargerVueParDefaut();
    }

    private void appliquerPermissions() {
        var role = Session.getRoleConnecte();
        if (role == null) return;

        switch (role) {
            case USER ->
                    masquerBoutons(btnCompteRendu, btnLocaux,
                            btnReserverFormation, btnReclamation);

            case RESPONSABLE_CENTRE ->
                    masquerBoutons(btnPasserTests, btnChatbot,
                            btnCompteRendu, btnSuivie);

            case PSYCHOLOGUE ->
                    masquerBoutons(btnPasserTests, btnChatbot,
                            btnSuivie, btnReserverFormation, btnLocaux);

            case ADMIN -> { /* tout visible */ }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Vue par défaut selon le rôle connecté
    // ══════════════════════════════════════════════════════════════
    private void chargerVueParDefaut() {
        var role = Session.getRoleConnecte();
        if (role == null) { loadAccueil(); return; }

        switch (role) {
            case USER               -> loadView("Suivie.fxml");
            case PSYCHOLOGUE        -> loadView("EspacePraticienQuiz.fxml");
            case RESPONSABLE_CENTRE -> loadView("Accueil.fxml");
            case ADMIN              -> loadView("Accueil.fxml");
        }
    }

    private void masquerBoutons(Button... boutons) {
        for (Button btn : boutons) {
            if (btn != null) {
                btn.setVisible(false);
                btn.setManaged(false);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Chargement FXML — avec null-check et message d'erreur clair
    // ══════════════════════════════════════════════════════════════
    private void loadView(String fxmlFile) {
        // ── 1. Résoudre l'URL ─────────────────────────────────────
        URL url = getClass().getResource("/views/" + fxmlFile);

        // ── 2. Null-check — fichier introuvable dans le classpath ──
        if (url == null) {
            System.err.println("╔══════════════════════════════════════════╗");
            System.err.println("║ ❌ FXML introuvable : /views/" + fxmlFile);
            System.err.println("║ Vérifiez que le fichier est dans :       ");
            System.err.println("║   src/main/resources/views/" + fxmlFile);
            System.err.println("║ Et relancez : Build → Rebuild Project    ");
            System.err.println("╚══════════════════════════════════════════╝");

            // ── Afficher message d'erreur dans l'UI au lieu de crasher
            if (contentArea != null) {
                Label lblErr = new Label(
                        "⚠️ Vue introuvable : " + fxmlFile
                                + "\nVérifiez src/main/resources/views/");
                lblErr.setStyle(
                        "-fx-font-size:13px; -fx-text-fill:#ef4444;"
                                + "-fx-padding:24; -fx-font-weight:600;");
                lblErr.setWrapText(true);
                contentArea.getChildren().setAll(lblErr);
            }
            return;
        }

        // ── 3. Chargement normal ───────────────────────────────────
        try {
            FXMLLoader loader = new FXMLLoader(url);
            Node view = loader.load();

            Object controller = loader.getController();
            // ✅ Injecter le parent si le controller le supporte
            if (controller instanceof SuivieQuizController sqc) {
                sqc.setParentController(this);
            }

            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }

            System.out.println("✅ Vue chargée : " + fxmlFile);

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement : " + fxmlFile);
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Actions navigation
    // ══════════════════════════════════════════════════════════════
    @FXML public void loadAccueil()            { loadView("Accueil.fxml"); }
    @FXML private void loadRendezVous()        { loadView("RendezVous.fxml"); }
    @FXML private void loadCompteRendu()       { loadView("CompteRendu.fxml"); }
    @FXML private void loadForum()             { loadView("Forum.fxml"); }
    @FXML private void loadChatbot()           { loadView("Chatbot.fxml"); }
    @FXML private void loadPasserTests()       { loadView("PasserTests.fxml"); }
    @FXML private void loadSuivie()            { loadView("Suivie.fxml"); }
    @FXML private void loadProfil()            { loadView("Profil.fxml"); }
    @FXML private void loadReclamation()       { loadView("Reclamation.fxml"); }
    @FXML private void loadReserverFormation() { loadView("ReserverFormation.fxml"); }
    @FXML private void loadConsulterSupport()  { loadView("ConsulterSupport.fxml"); }
    @FXML private void loadLocaux()            { loadView("Locaux.fxml"); }
}