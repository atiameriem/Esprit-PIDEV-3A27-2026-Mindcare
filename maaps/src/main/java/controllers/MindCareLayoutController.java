package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import models.AppUser;
import services.UserLookupService;
import utils.UserSession;
import javafx.scene.Parent;

import java.io.IOException;
import java.sql.SQLException;

public class MindCareLayoutController {

    @FXML private ComboBox<String> sortCombo;
    @FXML private TextField searchField;
    @FXML private VBox contentArea;

    @FXML private ComboBox<String> sessionCombo;
    @FXML private Button btnHistorique;

    private final UserLookupService userLookupService = new UserLookupService();

    private String currentView = "Accueil.fxml";

    @FXML
    public void initialize() {
        if (sortCombo != null) {
            sortCombo.getItems().setAll("Plus récents", "Plus anciens", "Plus aimés");
            sortCombo.getSelectionModel().selectFirst();
        }

        if (sessionCombo != null) {
            sessionCombo.getItems().setAll("patient", "responsable_centre");
            sessionCombo.getSelectionModel().selectFirst();

            // ✅ ton FXML a déjà onAction="#switchSession" donc pas besoin de setOnAction,
            // mais ce n’est pas grave si on le garde.
            sessionCombo.setOnAction(e -> switchSession());

            switchSession();
        }

        updateRoleBasedUi();
        loadAccueil();
    }

    @FXML
    private void switchSession() {
        if (sessionCombo == null) return;
        String role = sessionCombo.getValue();
        if (role == null) return;

        try {
            AppUser u = userLookupService.getFirstByRole(role);

            if (u == null) {
                u = new AppUser();
                u.setIdUsers(1);
                u.setRole(role);
                u.setNom(role);
            }

            UserSession.setCurrentUser(u);
            System.out.println("✅ Session: " + u.getRole() + " (id=" + u.getIdUsers() + ")");

        } catch (SQLException e) {
            System.err.println("❌ Erreur session user: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de récupérer l'utilisateur: " + e.getMessage());
        }

        reloadCurrentView();

        // ✅ visibilité du menu selon rôle
        updateRoleBasedUi();
    }

    private void updateRoleBasedUi() {
        if (btnHistorique != null) {
            boolean show = UserSession.isResponsableCentre() || UserSession.isAdmin();
            btnHistorique.setVisible(show);
            btnHistorique.setManaged(show);
        }
    }

    private void reloadCurrentView() {
        if ("Locaux.fxml".equals(currentView)) loadLocaux();
        else if ("Reservations.fxml".equals(currentView)) loadReservations();
        else loadView(currentView);
    }

    private void loadView(String fxmlFile) {
        try {
            System.out.println("➡️ loadView: " + fxmlFile);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxmlFile));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
            currentView = fxmlFile;
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement vue: " + fxmlFile);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur FXML", "Impossible de charger: " + fxmlFile + "\n" + e.getMessage());
        }
    }

    private void loadViewWithController(String fxmlFile, java.util.function.Consumer<Object> setup) {
        try {
            System.out.println("➡️ loadViewWithController: " + fxmlFile);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxmlFile));
            Node view = loader.load();

            Object controller = loader.getController();
            if (setup != null && controller != null) setup.accept(controller);

            contentArea.getChildren().setAll(view);
            currentView = fxmlFile;

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement vue (controller): " + fxmlFile);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur FXML", "Impossible de charger: " + fxmlFile + "\n" + e.getMessage());
        }
    }

    // ========= Navigation menu =========
    @FXML public void loadAccueil() { loadView("Accueil.fxml"); }
    @FXML public void loadRendezVous() { loadView("RendezVous.fxml"); }
    @FXML public void loadCompteRendu() { loadView("CompteRendu.fxml"); }
    @FXML public void loadForum() { loadView("Forum.fxml"); }
    @FXML public void loadChatbot() { loadView("Chatbot.fxml"); }
    @FXML public void loadPasserTests() { loadView("PasserTests.fxml"); }
    @FXML public void loadSuivie() { loadView("Suivie.fxml"); }
    @FXML public void loadProfil() { loadView("Profil.fxml"); }
    @FXML public void loadReclamation() { loadView("Reclamation.fxml"); }
    @FXML public void loadReserverFormation() { loadView("ReserverFormation.fxml"); }
    @FXML public void loadConsulterSupport() { loadView("ConsulterSupport.fxml"); }

    @FXML
    public void loadLocaux() {
        System.out.println("✅ CLICK Locaux");
        loadViewWithController("Locaux.fxml", controller -> {
            if (controller instanceof LocauxController lc) {
                lc.setLayoutController(this);
            }
        });
    }

    @FXML
    public void loadReservations() {
        System.out.println("✅ CLICK Reservations");
        loadViewWithController("Reservations.fxml", controller -> {
            if (controller instanceof ReservationsController rc) {
                rc.setLayoutController(this);
            }
        });
    }



    public void loadIntoContent(String fxmlPath) {
        try {
            System.out.println("➡️ loadIntoContent: " + fxmlPath);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // ✅ si la page chargée a besoin du layoutController, on l’injecte
            Object controller = loader.getController();
            if (controller instanceof ReservationsController rc) {
                rc.setLayoutController(this);
            } else if (controller instanceof LocauxController lc) {
                lc.setLayoutController(this);
            }

            contentArea.getChildren().setAll(root);

            // ✅ optionnel : mémoriser la vue courante si tu veux reloadCurrentView
            String file = fxmlPath.startsWith("/views/") ? fxmlPath.substring("/views/".length()) : fxmlPath;
            currentView = file;

        } catch (Exception e) {
            System.err.println("❌ Erreur loadIntoContent: " + fxmlPath);
            e.printStackTrace();

            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur FXML");
            a.setHeaderText("Impossible de charger : " + fxmlPath);
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    @FXML
    public void loadCheckIn() {
        if (!(UserSession.isResponsableCentre() || UserSession.isAdmin())) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Le Check-in est réservé au responsable du centre.");
            return;
        }
        loadView("CheckIn.fxml");
    }

    @FXML
    public void loadHistorique() {
        if (!(UserSession.isResponsableCentre() || UserSession.isAdmin())) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Historique réservé au responsable du centre.");
            return;
        }
        loadView("Historique.fxml");
    }



    @FXML
    public void loadCalendar() {
        loadView("Calendar.fxml");
    }

    @FXML
    public void loadMaps() {
        loadView("Maps.fxml");
    }


    // ========= Ouverture formulaires =========

    public void openLocalForm(int idLocal) {
        loadViewWithController("LocalForm.fxml", controller -> {
            if (controller instanceof LocalFormController fc) {
                fc.setOnDone(this::loadLocaux);
                fc.setEditingId(idLocal);
            }
        });
    }

    public void openReservationForm(int idLocal, String localNom) {
        loadViewWithController("ReservationForm.fxml", controller -> {
            if (controller instanceof ReservationFormController fc) {
                fc.setOnDone(this::loadReservations);
                fc.setLocal(idLocal, localNom);
            }
        });
    }

    public void openReservationEdit(models.Reservation r) {
        loadViewWithController("ReservationForm.fxml", controller -> {
            if (controller instanceof ReservationFormController fc) {
                fc.setOnDone(this::loadReservations);
                fc.setEditingReservation(r);
            }
        });
    }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
