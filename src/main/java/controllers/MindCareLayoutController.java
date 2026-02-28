package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import utils.Session;

import java.io.IOException;

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
    @FXML private TextField searchField;
    @FXML private VBox contentArea;

    @FXML
    public void initialize() {
        if (sortCombo != null) {
            sortCombo.getItems().setAll("Plus récents", "Plus anciens", "Plus aimés");
            sortCombo.getSelectionModel().selectFirst();
        }
        appliquerPermissions();
        chargerVueParDefaut(); // ✅ Vue par défaut selon rôle
    }

    private void appliquerPermissions() {
        var role = Session.getRoleConnecte();
        if (role == null) return;

        switch (role) {
            case USER:
                masquerBoutons(btnCompteRendu, btnLocaux,
                        btnReserverFormation, btnReclamation);
                break;

            case RESPONSABLE_CENTRE:
                masquerBoutons(btnPasserTests, btnChatbot,
                        btnCompteRendu, btnSuivie);
                break;

            case PSYCHOLOGUE:
                // ✅ Ajout de btnChatbot et btnSuivie
                masquerBoutons(btnPasserTests, btnChatbot,
                        btnSuivie, btnReserverFormation, btnLocaux);
                break;

            case ADMIN:
                break;
        }
    }

    // ✅ Vue par défaut selon le rôle connecté
    private void chargerVueParDefaut() {
        var role = Session.getRoleConnecte();
        if (role == null) { loadAccueil(); return; }

        switch (role) {
            case USER              -> loadView("Suivie.fxml");
            case PSYCHOLOGUE       -> loadView("EspacePraticien.fxml");
            case RESPONSABLE_CENTRE-> loadView("Accueil.fxml");
            case ADMIN             -> loadView("Accueil.fxml");
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

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/" + fxmlFile)
            );
            Node view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof SuivieQuizController) {
                ((SuivieQuizController) controller).setParentController(this);
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la vue: " + fxmlFile);
            e.printStackTrace();
        }
    }

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