package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Formation;
import models.Module;
import models.Contenu;
import models.Participation;
import services.*;
import javafx.application.Platform;
import java.net.URI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javafx.scene.web.WebView;
import utils.JitsiMeetWindowF;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FormationController {

    @FXML
    private TextField searchField;

    // === ÉLÉMENTS DE LA VUE PRINCIPALE ===
    @FXML
    private FlowPane formationsFlowPane;
    @FXML
    private FlowPane participationsFlowPane;
    @FXML
    private VBox allFormationsView;
    @FXML
    private VBox myFormationsView;
    @FXML
    private VBox noParticipationsBox;
    @FXML
    private VBox demandesView, inscritsView, moduleDetailsView, formationDetailsView, sessionsView;
    @FXML
    private VBox demandesList, inscritsList, notificationsList, sessionsList, calendarContainer;
    @FXML
    private VBox activeSeanceView;
    @FXML
    private StackPane jitsiContainer;
    @FXML
    private Label activeSeanceTitle, activeSeanceSubtitle;
    @FXML
    private VBox calendarPopup;
    @FXML
    private Button allFormationsBtn, myFormationsBtn, demandesBtn, inscritsBtn, sessionsTabBtn, aiInterviewBtn;
    @FXML
    private Button addFormationBtn;
    @FXML
    private HBox mainHeader, tabsContainer;

    // === IA & WORD ===
    @FXML
    private VBox aiInterviewView;
    @FXML
    private VBox wordViewerView;
    @FXML
    private VBox aiChatContainer;
    @FXML
    private ScrollPane aiChatScrollPane;
    @FXML
    private TextArea aiMessageField;
    @FXML
    private Button aiSendBtn;
    @FXML
    private Button btnGenererCours;
    @FXML
    private ProgressIndicator aiLoading;
    @FXML
    private Label aiStatutLabel;
    @FXML
    private Label wordViewerTitre;
    @FXML
    private VBox wordContentContainer;
    @FXML
    private VBox wordExplicationPanel;
    @FXML
    private ScrollPane wordScrollPane;
    @FXML
    private Label wordMotLabel;
    @FXML
    private Label wordExplicationLabel;
    @FXML
    private Label wordTraductionLabel;
    @FXML
    private Label wordExempleLabel;
    @FXML
    private Button wordExpliquerBtn;
    @FXML
    private ProgressIndicator wordExplicationLoading;
    @FXML
    private TextField wordInputField;

    private AIInterviewController aiController;
    private WordViewerPatient wordViewer;

    // === NOTIFICATIONS ===
    @FXML
    private StackPane notificationBell;
    @FXML
    private StackPane notificationBadge;
    @FXML
    private Label notificationCountLabel;
    @FXML
    private VBox notificationsPopup;

    // === DICTIONNAIRE IA POPUP ===
    @FXML
    private VBox dicoPopup;
    @FXML
    private TextField dicoInputField;
    @FXML
    private Label dicoExplicationLabel;
    @FXML
    private ProgressIndicator dicoLoading;
    @FXML
    private Button dicoExpliquerBtn;
    @FXML
    private Button btnDictionnaireIA;
    @FXML
    private Button btnGenererIA;

    // === VUES ADMIN / PSY ===
    @FXML
    private ComboBox<Formation> inscritsFormationCombo;

    // === FILTRES & RECHERCHE ===

    @FXML
    private Pane filterBar;
    @FXML
    private Pane myFilterBar;

    @FXML
    private ComboBox<String> categoryCombo;
    @FXML
    private TextField myParticipationsSearchField;
    @FXML
    private TextField sessionsSearchField;
    @FXML
    private CheckBox onlyMineCheckbox;
    @FXML
    private ComboBox<String> categorieFormationCombo;

    private static final List<String> CATEGORIES = List.of(
            "Psychologie clinique",
            "Santé mentale",
            "Thérapies spécialisées",
            "Bien-être thérapeutique");

    // === ÉLÉMENTS D'APPRENTISSAGE ===
    @FXML
    private Label detailFormationTitle;
    @FXML
    private VBox modulesLearningList;
    @FXML
    private Label selectedModuleTitle;
    @FXML
    private Label selectedModuleDesc;
    @FXML
    private Button btnTopEditModule;
    @FXML
    private Button btnExportWord;
    @FXML
    private Button btnSyncWord;
    @FXML
    private VBox contentsLearningContainer;

    private Module viewedModule; // Currently viewed module for Sync logic

    // === ÉLÉMENTS DU FORMULAIRE (AjouterFormation.fxml) ===
    @FXML
    private TextField titreField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField dureeField;
    @FXML
    private ComboBox<String> niveauCombo;
    @FXML
    private ImageView imagePreview;
    @FXML
    private Label imagePathLabel;
    @FXML
    private Button mainButton;
    @FXML
    private TextField moduleTitreField;
    @FXML
    private TextField youtubeSearchField;
    @FXML
    private TextArea moduleDescriptionArea;
    @FXML
    private ListView<String> contenusListView;
    @FXML
    private ListView<Module> modulesListView;

    // === SERVICES ET ÉTAT ===
    private final FormationService formationService = new FormationService();
    private final ModuleServiceF moduleServiceF = new ModuleServiceF();
    private final ContenuServiceF contenuServiceF = new ContenuServiceF();
    private final ParticipationServiceF participationServiceF = new ParticipationServiceF();
    private final SeanceGroupeServiceF seanceService = new SeanceGroupeServiceF();
    private final UserServiceF userServiceF = new UserServiceF();
    private final EmailServiceF emailServiceF = new EmailServiceF();
    private final int currentUserId;
    private final String currentUserRole;
    private final String currentUserEmail;
    private final String currentUserFullName;

    private String currentWordPath; // Pour la sauvegarde Word

    private static final String ACTIVE_TAB_STYLE = "-fx-background-color: transparent; -fx-text-fill: #5C98A8; -fx-font-weight: 800; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-color: #5C98A8; -fx-border-width: 0 0 3 0;";
    private static final String INACTIVE_TAB_STYLE = "-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: 600; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 0 0 3 0;";

    @FXML
    private VBox step1Container, step2Container, modulesPreviewContainer;
    @FXML
    private Label step1Title, step2Title;
    @FXML
    private Button nextStepBtn, prevStepBtn;

    private boolean editFormationOnly = false;
    private boolean editModuleOnly = false;

    public FormationController() {
        utils.UserSession session = utils.UserSession.getInstance();
        models.User user = (session != null) ? session.getUser() : null;
        if (user != null) {
            this.currentUserId = user.getId_users();
            this.currentUserRole = user.getRole();
            this.currentUserEmail = user.getEmail();
            this.currentUserFullName = user.getPrenom() + " " + user.getNom();
        } else {
            this.currentUserId = -1;
            this.currentUserRole = "guest";
            this.currentUserEmail = null;
            this.currentUserFullName = "Votre Psychologue MindCare";
        }
    }

    private boolean isAdmin() {
        return "Admin".equalsIgnoreCase(currentUserRole);
    }

    private boolean isPsychologue() {
        return "Psychologue".equalsIgnoreCase(currentUserRole);
    }

    private boolean isPatient() {
        return "Patient".equalsIgnoreCase(currentUserRole) || "ResponsableC".equalsIgnoreCase(currentUserRole);
    }

    private boolean isStaff() {
        return isAdmin() || isPsychologue();
    }

    private String selectedImagePath;
    private Formation existingFormation;
    private Formation selectedFormationForLearning;
    private boolean isEditMode = false;
    private Module moduleEnCours;
    private List<Module> modulesToAdd = new ArrayList<>();
    private List<Participation> userParticipations = new ArrayList<>();

    @FXML
    public void initialize() {
        // Configuration initiale de la visibilité selon le rôle
        boolean isAdminOrPsy = isStaff();

        if (addFormationBtn != null) {
            addFormationBtn.setVisible(isAdminOrPsy);
            addFormationBtn.setManaged(isAdminOrPsy);
        }
        if (myFormationsBtn != null) {
            myFormationsBtn.setVisible(!isAdminOrPsy);
            myFormationsBtn.setManaged(!isAdminOrPsy);
        }
        if (demandesBtn != null) {
            // Replaced by the notification bell for a cleaner UI as per user request
            demandesBtn.setVisible(false);
            demandesBtn.setManaged(false);
        }
        if (inscritsBtn != null) {
            inscritsBtn.setVisible(isAdminOrPsy);
            inscritsBtn.setManaged(isAdminOrPsy);
        }
        if (sessionsTabBtn != null) {
            sessionsTabBtn.setVisible(true); // Sessions are relevant for all roles
            sessionsTabBtn.setManaged(true);
        }

        if (btnDictionnaireIA != null) {
            btnDictionnaireIA.setVisible(!isAdminOrPsy);
            btnDictionnaireIA.setManaged(!isAdminOrPsy);
        }
        if (btnGenererIA != null) {
            btnGenererIA.setVisible(isAdminOrPsy);
            btnGenererIA.setManaged(isAdminOrPsy);
        }

        // Initialize notifications
        updateNotificationContent();
        if (onlyMineCheckbox != null) {
            onlyMineCheckbox.setVisible(false);
            onlyMineCheckbox.setManaged(false);
        }

        if (niveauCombo != null) {
            niveauCombo.getItems().setAll("Débutant", "Intermédiaire", "Avancé");
        }

        if (formationsFlowPane != null) {
            loadAllFormations();
        }

        if (inscritsFormationCombo != null) {
            try {
                List<Formation> myFormations = formationService.findByOwner(currentUserId);
                inscritsFormationCombo.getItems().setAll(myFormations);
                inscritsFormationCombo.setCellFactory(lv -> new ListCell<Formation>() {
                    @Override
                    protected void updateItem(Formation item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item.getTitre());
                    }
                });
                inscritsFormationCombo.setButtonCell(new ListCell<Formation>() {
                    @Override
                    protected void updateItem(Formation item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item.getTitre());
                    }
                });
                inscritsFormationCombo.setOnAction(e -> loadInscritsForFormation());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (categoryCombo != null) {
            categoryCombo.getItems().add("Toutes");
            categoryCombo.getItems().addAll(CATEGORIES);
            categoryCombo.getSelectionModel().selectFirst();
            categoryCombo.setOnAction(e -> {
                if (allFormationsView.isVisible())
                    filterAllFormations();
                else if (myFormationsView.isVisible())
                    filterMyParticipations();
            });
        }

        if (categorieFormationCombo != null) {
            categorieFormationCombo.getItems().setAll(CATEGORIES);
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> filterAllFormations());
        }

        if (myParticipationsSearchField != null) {
            myParticipationsSearchField.textProperty().addListener((obs, oldV, newV) -> filterMyParticipations());
        }

        if (sessionsSearchField != null) {
            sessionsSearchField.textProperty().addListener((obs, oldV, newV) -> loadSessions());
        }

        if (modulesListView != null) {
            modulesListView.setCellFactory(lv -> new ListCell<Module>() {

                @Override
                protected void updateItem(Module item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getTitre());
                    }
                }

            });
            modulesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadModuleToEdit(newVal);
                }
            });
        }
    }

    // === NAVIGATION ===

    private void switchView(Pane target) {
        resetAllViews();
        target.setVisible(true);
        target.setManaged(true);

        // Cacher l'en-tête et les onglets dans les vues de détails et l'assistant IA
        boolean isDetailView = (target == formationDetailsView || target == moduleDetailsView
                || target == aiInterviewView);
        if (mainHeader != null) {
            mainHeader.setVisible(!isDetailView);
            mainHeader.setManaged(!isDetailView);
        }
        if (tabsContainer != null) {
            tabsContainer.setVisible(!isDetailView);
            tabsContainer.setManaged(!isDetailView);
        }

        // Amener au premier plan pour éviter d'être "bloqué" par d'autres vues derrière
        target.toFront();
    }

    private void resetAllViews() {
        if (allFormationsView != null) {
            allFormationsView.setVisible(false);
            allFormationsView.setManaged(false);
        }
        if (myFormationsView != null) {
            myFormationsView.setVisible(false);
            myFormationsView.setManaged(false);
        }
        if (demandesView != null) {
            demandesView.setVisible(false);
            demandesView.setManaged(false);
        }
        if (inscritsView != null) {
            inscritsView.setVisible(false);
            inscritsView.setManaged(false);
        }
        if (formationDetailsView != null) {
            formationDetailsView.setVisible(false);
            formationDetailsView.setManaged(false);
        }
        if (moduleDetailsView != null) {
            moduleDetailsView.setVisible(false);
            moduleDetailsView.setManaged(false);
        }
        if (sessionsView != null) {
            sessionsView.setVisible(false);
            sessionsView.setManaged(false);
        }
        if (activeSeanceView != null) {
            activeSeanceView.setVisible(false);
            activeSeanceView.setManaged(false);
        }
        if (filterBar != null) {
            filterBar.setVisible(false);
            filterBar.setManaged(false);
        }
        if (myFilterBar != null) {
            myFilterBar.setVisible(false);
            myFilterBar.setManaged(false);
        }
        if (aiInterviewView != null) {
            aiInterviewView.setVisible(false);
            aiInterviewView.setManaged(false);
        }
        if (wordViewerView != null) {
            wordViewerView.setVisible(false);
            wordViewerView.setManaged(false);
        }
        // Masquer les champs de recherche par défaut
        if (searchField != null) {
            searchField.setVisible(false);
            searchField.setManaged(false);
        }
        if (categoryCombo != null) {
            categoryCombo.setVisible(false);
            categoryCombo.setManaged(false);
        }
        if (myParticipationsSearchField != null) {
            myParticipationsSearchField.setVisible(false);
            myParticipationsSearchField.setManaged(false);
        }
        if (sessionsSearchField != null) {
            sessionsSearchField.setVisible(false);
            sessionsSearchField.setManaged(false);
        }
    }

    @FXML
    private void handleBackFromProgramme() {
        boolean isAdminOrPsy = "admin".equalsIgnoreCase(currentUserRole)
                || "psychologue".equalsIgnoreCase(currentUserRole);
        if (isAdminOrPsy) {
            showAllFormations();
        } else {
            showMyFormations();
        }
    }

    @FXML
    private void backToModules() {
        switchView(formationDetailsView);
    }

    @FXML
    private void showAllFormations() {
        switchView(allFormationsView);
        resetTabStyles();
        if (allFormationsBtn != null) {
            allFormationsBtn.setStyle(ACTIVE_TAB_STYLE);
        }
        // Activer la recherche globale (Rechercher... + Catégorie)
        if (searchField != null) {
            searchField.setVisible(true);
            searchField.setManaged(true);
        }
        if (categoryCombo != null) {
            categoryCombo.setVisible(true);
            categoryCombo.setManaged(true);
        }
        loadAllFormations();
    }

    @FXML
    private void showMyFormations() {
        switchView(myFormationsView);
        resetTabStyles();
        if (myFormationsBtn != null) {
            myFormationsBtn.setStyle(ACTIVE_TAB_STYLE);
        }
        // Activer la recherche spécifique à mes participations
        if (myParticipationsSearchField != null) {
            myParticipationsSearchField.setVisible(true);
            myParticipationsSearchField.setManaged(true);
        }
        if (categoryCombo != null) {
            categoryCombo.setVisible(true);
            categoryCombo.setManaged(true);
        }
        loadMyParticipations();
    }

    @FXML
    private void showDemandes() {
        switchView(demandesView);
        resetTabStyles();
        if (demandesBtn != null) {
            demandesBtn.setStyle(ACTIVE_TAB_STYLE);
        }
        loadPendingDemandes();
    }

    @FXML
    private void showInscritsView() {
        switchView(inscritsView);
        resetTabStyles();
        if (inscritsBtn != null) {
            inscritsBtn.setStyle(ACTIVE_TAB_STYLE);
        }
        // Refresh formations in combo if they were added
        try {
            List<Formation> targetFormations;
            if (isAdmin()) {
                targetFormations = formationService.read(); // Admin sees all
            } else {
                targetFormations = formationService.findByOwner(currentUserId);
            }
            if (inscritsFormationCombo != null) {
                inscritsFormationCombo.getItems().setAll(targetFormations);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showSessionsView() {
        switchView(sessionsView);
        resetTabStyles();
        if (sessionsTabBtn != null) {
            sessionsTabBtn.setStyle(ACTIVE_TAB_STYLE);
        }
        if (sessionsSearchField != null) {
            sessionsSearchField.setVisible(true);
            sessionsSearchField.setManaged(true);
        }
        loadSessions();
    }

    @FXML
    private void loadSessions() {
        if (sessionsList == null)
            return;
        sessionsList.getChildren().clear();
        String query = (sessionsSearchField != null) ? sessionsSearchField.getText().toLowerCase() : "";

        try {
            List<models.SeanceGroupe> seances;
            if (isAdmin()) {
                seances = seanceService.read(); // Admin sees all
            } else if (isPsychologue()) {
                seances = seanceService.findByPsychologue(currentUserId);
            } else {
                seances = new ArrayList<>();
                List<Participation> ps = participationServiceF.findByUserId(currentUserId);
                for (Participation p : ps) {
                    if ("accepté".equalsIgnoreCase(p.getStatut())) {
                        seances.addAll(seanceService.findByFormation(p.getIdFormation()));
                    }
                }
            }

            // 📅 Filtrage et Tri
            seances.sort(java.util.Comparator.comparing(models.SeanceGroupe::getDateHeure));

            List<models.SeanceGroupe> filtered = new ArrayList<>();
            for (models.SeanceGroupe s : seances) {
                boolean matchSearch = s.getTitre().toLowerCase().contains(query) ||
                        (s.getTitreFormation() != null && s.getTitreFormation().toLowerCase().contains(query));

                if (!matchSearch)
                    continue;

                boolean isCreator = s.getIdUsers() == currentUserId;
                boolean isStarted = "EN_COURS".equalsIgnoreCase(s.getStatut());
                boolean isTimeReached = LocalDateTime.now().isAfter(s.getDateHeure());

                // Si ce n'est pas le créateur (ou admin/psy) et que la séance n'est pas prête,
                // on ne l'affiche pas
                if (!isCreator && !isStaff() && (!isTimeReached || !isStarted)) {
                    continue;
                }
                filtered.add(s);
            }

            if (filtered.isEmpty()) {
                String msg = query.isEmpty()
                        ? "Aucune séance de groupe programmée ou disponible pour le moment."
                        : "Aucune séance ne correspond à votre recherche.";

                Label empty = new Label(msg);
                empty.setStyle("-fx-text-fill: #6B7280; -fx-font-style: italic;");
                sessionsList.getChildren().add(empty);
            } else {
                for (models.SeanceGroupe s : filtered) {
                    sessionsList.getChildren().add(createSessionListItem(s));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createSessionListItem(models.SeanceGroupe s) {
        HBox item = new HBox(20);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle(
                "-fx-background-color: white; " +
                        "-fx-padding: 20; " +
                        "-fx-background-radius: 18; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5); " +
                        "-fx-border-color: #F1F5F9; " +
                        "-fx-border-radius: 18;");

        // Hover effect for the whole card
        item.setOnMouseEntered(e -> item.setStyle(item.getStyle()
                + "-fx-background-color: #F8FAFC; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 8);"));
        item.setOnMouseExited(e -> item.setStyle(item.getStyle().replace(
                "-fx-background-color: #F8FAFC; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 8);", "")));

        // Left Icon (Jitsi/Video)
        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinWidth(60);
        iconBox.setMinHeight(60);
        iconBox.setStyle("-fx-background-color: #E0F2FE; -fx-background-radius: 15;");
        Label icon = new Label("🎥");
        icon.setStyle("-fx-font-size: 28px;");
        iconBox.getChildren().add(icon);

        // Session Information
        VBox info = new VBox(5);
        Label t = new Label(s.getTitre());
        t.setStyle("-fx-font-weight: 800; -fx-font-size: 17px; -fx-text-fill: #1E293B;");

        Label fTitle = new Label("📖 Formation : " + s.getTitreFormation());
        fTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #5C98A8; -fx-font-weight: bold;");

        HBox metaData = new HBox(15);
        Label dateLabel = new Label("📅 " + s.getDateHeure().format(DateTimeFormatter.ofPattern("EEE dd MMM, HH:mm")));
        dateLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        Label durationLabel = new Label("⏱ " + s.getDureeMinutes() + " min");
        durationLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        metaData.getChildren().addAll(dateLabel, durationLabel);

        info.getChildren().addAll(t, fTitle, metaData);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status Badge
        Label st = new Label();
        String currentStatut = s.getStatut() != null ? s.getStatut() : "PLANIFIEE";
        st.setText(currentStatut.replace("_", " "));

        String stColor = "#64748B"; // Default
        String stBg = "#F1F5F9";

        switch (currentStatut) {
            case "PLANIFIEE" -> {
                stColor = "#3B82F6";
                stBg = "#DBEAFE";
            }
            case "EN_COURS" -> {
                stColor = "#10B981";
                stBg = "#D1FAE5";
            }
            case "TERMINEE" -> {
                stColor = "#475569";
                stBg = "#F1F5F9";
            }
            case "ANNULEE" -> {
                stColor = "#EF4444";
                stBg = "#FEE2E2";
            }
        }

        st.setStyle("-fx-background-color: " + stBg + "; -fx-text-fill: " + stColor +
                "; -fx-padding: 6 12; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: 800; -fx-text-transform: uppercase;");

        boolean isCreator = s.getIdUsers() == currentUserId;
        boolean isStarted = "EN_COURS".equalsIgnoreCase(s.getStatut());
        boolean isTimeReached = LocalDateTime.now().isAfter(s.getDateHeure());

        // Bouton Lancer/Continuer/Rejoindre : uniquement pour psychologue et admin
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button join = new Button();
        join.setPrefWidth(120);

        if (isCreator) {
            // Seul le créateur (Psy) peut Lancer/Continuer
            join.setText(isStarted ? "Continuer" : "Lancer");
            boolean canLaunch = !LocalDateTime.now().isBefore(s.getDateHeure().minusMinutes(15));
            join.setDisable(!canLaunch);
            join.setStyle("-fx-background-color: " + (canLaunch ? "#10B981" : "#A7F3D0")
                    + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: "
                    + (canLaunch ? "hand" : "default") + ";");
        } else if (isAdmin()) {
            // Admin : peut rejoindre pour tester dès que c'est "EN_COURS"
            join.setText("Rejoindre");
            join.setDisable(!isStarted);
            join.setStyle("-fx-background-color: " + (isStarted ? "#5C98A8" : "#CBD5E1")
                    + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: "
                    + (isStarted ? "hand" : "default") + ";");
        } else {
            // Patient ou autre Psy : peut rejoindre seulement si le créateur est PRÉSENT
            join.setText("Rejoindre");
            boolean psyPresent = seanceService.isUserPresent(s.getSeanceId(), s.getIdUsers());
            boolean canJoin = isStarted && psyPresent;

            // On laisse le bouton activé pour permettre le double-clic de suppression
            join.setDisable(false);

            if (!isStarted) {
                join.setText(isTimeReached ? "En attente..." : "À venir");
            } else if (!psyPresent) {
                join.setText("Psy absente...");
            }

            join.setStyle("-fx-background-color: " + (canJoin ? "#5C98A8" : "#94A3B8")
                    + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");

            // Gestion du double clic pour supprimer la participation (demande)
            join.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY && event.getClickCount() == 2) {
                    if ("En attente...".equals(join.getText())) {
                        try {
                            participationServiceF.deleteByUserAndFormation(currentUserId, s.getIdFormation());
                            showAlert("Succès", "Votre demande de participation a été annulée.");
                            loadSessions(); // Rafraîchir
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }

        join.setOnAction(e -> {
            boolean isStartedNow = "EN_COURS".equalsIgnoreCase(s.getStatut());
            boolean psyPresentNow = seanceService.isUserPresent(s.getSeanceId(), s.getIdUsers());

            if (isCreator && !isStartedNow) {
                try {
                    seanceService.updateStatut(s.getSeanceId(), "EN_COURS");
                    s.setStatut("EN_COURS");
                    loadSessions();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                openSeanceInterface(s);
            } else if (isCreator || isAdmin() || (isStartedNow && psyPresentNow)) {
                openSeanceInterface(s);
            } else {
                // Si pas prêt, on ne fait rien ou on affiche un petit message
                System.out.println("Séance non démarrée ou Psy absente.");
            }
        });

        // Add join button for everyone EXCEPT Admin (per user request)
        if (!isAdmin()) {
            actions.getChildren().add(join);
        }

        // L'admin ne nécessite pas de bouton supprimer/lancer
        // Seul le créateur peut annuler/supprimer la séance s'il n'est pas admin
        if (isCreator && !isAdmin()) {
            Button del = new Button("🗑");
            del.setTooltip(new Tooltip("Annuler la séance"));
            del.setStyle(
                    "-fx-background-color: #FEF2F2; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand; -fx-border-color: #FECACA; -fx-border-radius: 8;");
            del.setOnAction(e -> handleDeleteSeance(s));
            actions.getChildren().add(del);
        }

        item.getChildren().addAll(iconBox, info, spacer, st, actions);
        return item;
    }

    private void handleDeleteSeance(models.SeanceGroupe s) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Annulation");
        alert.setHeaderText("Souhaitez-vous vraiment annuler cette séance ?");
        alert.setContentText("Cette action supprimera la séance et les liens associés.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                // 🔄 Recharger la séance depuis la DB pour avoir le google_event_id à jour
                // (peut avoir été sauvegardé en async après la création)
                models.SeanceGroupe fresh = seanceService.findById(s.getSeanceId());
                final models.SeanceGroupe seanceToDelete = (fresh != null) ? fresh : s;

                if (seanceToDelete.getGoogleEventId() == null || seanceToDelete.getGoogleEventId().isEmpty()) {
                    System.out.println(
                            "ℹ️ Cette séance n'a pas de google_event_id (ancienne séance ou Calendar non disponible).");
                } else {
                    System.out.println("🗑️ Suppression Calendar pour event: " + seanceToDelete.getGoogleEventId());
                }

                // 1️⃣ Envoyer email d'annulation aux participants (avec .ics CANCEL)
                new Thread(() -> {
                    try {
                        EmailServiceF emailServiceF = new EmailServiceF();
                        int sent = emailServiceF.envoyerAnnulationSeance(seanceToDelete,
                                seanceToDelete.getIdFormation());
                        System.out.println("✅ Emails annulation envoyés : " + sent
                                + " (doit être supprimé de MindCare et de tous les participants !!)");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();

                // 2️⃣ Supprimer de Google Calendar
                if (seanceToDelete.getGoogleEventId() != null && !seanceToDelete.getGoogleEventId().isEmpty()) {
                    new Thread(() -> {
                        try {
                            new GoogleCalendarServiceF().deleteEventFromCalendar(
                                    seanceToDelete.getGoogleEventId(), currentUserEmail);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                }

                // 3️⃣ Supprimer de la DB
                seanceService.delete(s.getSeanceId());
                loadSessions();
                showAlert("Succès",
                        "La séance a été annulée, supprimée de l'agenda et les participants ont été informés.");
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Erreur", "Erreur lors de l'annulation : " + e.getMessage());
            }
        }
    }

    private void resetTabStyles() {
        if (allFormationsBtn != null)
            allFormationsBtn.setStyle(INACTIVE_TAB_STYLE);
        if (myFormationsBtn != null)
            myFormationsBtn.setStyle(INACTIVE_TAB_STYLE);
        if (demandesBtn != null)
            demandesBtn.setStyle(INACTIVE_TAB_STYLE);
        if (inscritsBtn != null)
            inscritsBtn.setStyle(INACTIVE_TAB_STYLE);
        if (sessionsTabBtn != null)
            sessionsTabBtn.setStyle(INACTIVE_TAB_STYLE);
    }

    private void openModulesView(Participation p) {
        try {
            Formation f = formationService.findById(p.getIdFormation());
            if (f != null)
                loadModulesForLearning(f);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void refreshModules() {
        if (selectedFormationForLearning != null) {
            loadModulesForLearning(selectedFormationForLearning);
        }
    }

    private void loadModulesForLearning(Formation f) {
        this.selectedFormationForLearning = f;
        switchView(formationDetailsView);
        detailFormationTitle.setText(f.getTitre());
        modulesLearningList.getChildren().clear();

        boolean isCreator = (f.getIdCreateur() == currentUserId);

        if (btnGenererIA != null) {
            btnGenererIA.setVisible(isCreator);
            btnGenererIA.setManaged(isCreator);
        }

        try {
            List<Module> modules = moduleServiceF.findByFormationId(f.getId());
            for (Module m : modules) {
                HBox card = new HBox(15);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle(
                        "-fx-background-color: white; -fx-padding: 8 15; -fx-background-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(92,152,168,0.06), 10, 0, 0, 3); " +
                                "-fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-cursor: hand;");

                card.setOnMouseEntered(e -> card.setStyle(
                        "-fx-background-color: #EAF3F5; -fx-padding: 12 18; -fx-background-radius: 16; " +
                                "-fx-effect: dropshadow(gaussian, rgba(92,152,168,0.18), 16, 0, 0, 6); " +
                                "-fx-border-color: #AACFD9; -fx-border-radius: 16; -fx-cursor: hand;"));
                card.setOnMouseExited(e -> card.setStyle(
                        "-fx-background-color: white; -fx-padding: 12 18; -fx-background-radius: 16; " +
                                "-fx-effect: dropshadow(gaussian, rgba(92,152,168,0.08), 12, 0, 0, 4); " +
                                "-fx-border-color: #DDECEF; -fx-border-radius: 16; -fx-cursor: hand;"));
                card.setOnMouseClicked(e -> openModuleContent(m));

                VBox leftSide = new VBox();
                leftSide.setAlignment(Pos.CENTER);
                leftSide.setMinWidth(40);
                leftSide.setMinHeight(40);
                leftSide.setStyle("-fx-background-color: #EAF3F5; -fx-background-radius: 10;");

                Label icon = new Label("📚");
                icon.setStyle("-fx-font-size: 16px;");
                leftSide.getChildren().add(icon);

                VBox text = new VBox(2);
                Label t = new Label(m.getTitre());
                t.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #1E3A8A;");
                text.getChildren().add(t);

                String rawDesc = m.getDescription();
                if (rawDesc != null && !rawDesc.trim().isEmpty()) {
                    String cleanDesc = rawDesc.replace("ARTICLE_TITRE:", "").trim();
                    if (cleanDesc.length() > 40) {
                        cleanDesc = cleanDesc.substring(0, 40) + "...";
                    }
                    Label dPreview = new Label(cleanDesc);
                    dPreview.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
                    text.getChildren().add(dPreview);
                }

                Pane spacer = new Pane();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox actions = new HBox(10);
                actions.setAlignment(Pos.CENTER_RIGHT);

                if (isCreator) {
                    Button editM = new Button("✎");
                    editM.setStyle(
                            "-fx-background-color: #F0F9FF; -fx-text-fill: #0284C7; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand; -fx-border-color: #BAE6FD; -fx-border-radius: 8;");
                    editM.setOnAction(e -> {
                        e.consume();
                        openEditModule(f, m);
                    });
                    actions.getChildren().add(editM);
                }

                if (isCreator || isAdmin()) {
                    Button deleteM = new Button("🗑");
                    deleteM.setStyle(
                            "-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand; -fx-border-color: #FECACA; -fx-border-radius: 8;");
                    deleteM.setOnAction(e -> {
                        e.consume();
                        handleDeleteModule(m, f);
                    });
                    actions.getChildren().add(deleteM);
                }

                Label arrow = new Label("➡");
                arrow.setStyle("-fx-text-fill: #5C98A8; -fx-font-size: 20px; -fx-font-weight: bold;");
                actions.getChildren().add(arrow);

                card.getChildren().addAll(leftSide, text, spacer, actions);
                modulesLearningList.getChildren().add(card);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteModule(Module m, Formation f) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le module '" + m.getTitre() + "' ?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    moduleServiceF.delete(m.getId());
                    loadModulesForLearning(f); // Refresh
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void openModuleContent(Module m) {

        switchView(moduleDetailsView);

        // --- LOGIQUE DE NOTATION AUTOMATIQUE ---
        // Si c'est un patient et qu'il n'a pas encore noté cette formation
        if (isPatient()) {
            try {
                int fId = m.getFormationId(); // On a besoin de l'ID formation
                if (!participationServiceF.hasRated(currentUserId, fId)) {
                    // Déclencher le popup après 10 secondes de "temps passing"
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                            javafx.util.Duration.seconds(10));
                    pause.setOnFinished(event -> showRatingPopup(fId));
                    pause.play();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        selectedModuleTitle.setText(m.getTitre());
        selectedModuleTitle.setStyle("-fx-text-fill: #0369A1; -fx-font-weight: 900;");

        // Permettre au patient de sélectionner du texte et obtenir une définition
        if (isPatient()) {
            // Ajout d'une instruction discrète
            Label hint = new Label("💡 Sélectionnez un mot ou une phrase pour obtenir une définition");
            hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #5C98A8; -fx-font-style: italic; -fx-padding: 4 0;");
            hint.setManaged(true);
            hint.setVisible(true);
            // On l'ajoutera au container après le titre
            javafx.application.Platform.runLater(() -> {
                if (contentsLearningContainer.getChildren().size() == 0) {
                    contentsLearningContainer.getChildren().add(0, hint);
                }
            });
        }
        // Masquer la description du header car elle prend trop de place et va être
        // formatée dans le corps
        selectedModuleDesc.setVisible(false);
        selectedModuleDesc.setManaged(false);

        this.viewedModule = m;

        boolean isCreator = (selectedFormationForLearning != null
                && selectedFormationForLearning.getIdCreateur() == currentUserId);

        if (btnTopEditModule != null) {
            btnTopEditModule.setVisible(isCreator);
            btnTopEditModule.setManaged(isCreator);
            if (isCreator) {
                btnTopEditModule.setOnAction(e -> openEditorDialog(m));
            }
        }

        // Word Sync and Export buttons for AI-generated content (description has tags)
        boolean isAIContent = m.getDescription() != null && m.getDescription().contains("ARTICLE_TITRE:");
        if (btnExportWord != null) {
            btnExportWord.setVisible(isCreator && isAIContent);
            btnExportWord.setManaged(isCreator && isAIContent);
        }
        if (btnSyncWord != null) {
            btnSyncWord.setVisible(isCreator && isAIContent);
            btnSyncWord.setManaged(isCreator && isAIContent);
        }

        contentsLearningContainer.getChildren().clear();

        // 1. Affichage du contenu formatté (Cours)
        renderFormattedDescription(m.getDescription(), contentsLearningContainer);

        // 2. L'éditeur est maintenant accessible via btnTopEditModule dans le header

        try {
            List<Contenu> contenus = contenuServiceF.findByModuleId(m.getId());
            if (contenus.isEmpty()) {
                return;
            }

            for (Contenu c : contenus) {
                VBox card = new VBox(10);
                card.setStyle(
                        "-fx-background-color: #F8FAFC; -fx-padding: 20; -fx-background-radius: 15; " +
                                "-fx-border-color: #E2E8F0; -fx-border-width: 1;");

                String path = c.getChemin();
                String type = c.getType();

                Label contentLabel = new Label();
                contentLabel.setStyle("-fx-font-weight: 800; -fx-text-fill: #1E3A8A; -fx-font-size: 13px;");
                if ("VIDEO".equals(type) && (path.contains("youtube.com") || path.contains("youtu.be"))) {
                    String vId = extractYouTubeId(path);
                    contentLabel.setText("🎬 Vidéo YouTube...");
                    new Thread(() -> {
                        String title = YouTubeServiceF.getVideoTitle(vId);
                        if (title != null) {
                            javafx.application.Platform.runLater(() -> contentLabel.setText("🎬 " + title));
                        } else {
                            javafx.application.Platform.runLater(() -> contentLabel.setText("🎬 Vidéo externe"));
                        }
                    }).start();
                } else if ("VIDEO".equals(type)) {
                    contentLabel.setText("🎬 Vidéo locale : " + new File(path).getName());
                } else {
                    contentLabel.setText(
                            type + " : " + (path.startsWith("http") ? "Lien externe" : new File(path).getName()));
                }
                card.getChildren().add(contentLabel);

                // ===== VIDEO =====
                if ("VIDEO".equals(type)) {
                    StackPane videoCard = new StackPane();
                    videoCard.setPrefHeight(250);
                    videoCard.setStyle("-fx-background-color: #000; -fx-background-radius: 12; -fx-cursor: hand;");

                    ImageView thumbnailView = new ImageView();
                    thumbnailView.setFitHeight(250);
                    thumbnailView.setPreserveRatio(true);

                    if (path.contains("youtube.com") || path.contains("youtu.be")) {
                        String videoId = extractYouTubeId(path);
                        String thumbUrl = "https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg";
                        try {
                            thumbnailView.setImage(new Image(thumbUrl));
                        } catch (Exception e) {
                        }
                    }

                    Label playIcon = new Label("▶");
                    playIcon.setStyle(
                            "-fx-font-size: 50px; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 0);");

                    videoCard.getChildren().addAll(thumbnailView, playIcon);
                    card.getChildren().add(videoCard);

                    // Correction du StackOverflow : Utiliser un handler nommé au lieu de
                    // fireEvent(e)
                    final javafx.event.EventHandler<javafx.scene.input.MouseEvent> openVideoHandler = new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
                        @Override
                        public void handle(javafx.scene.input.MouseEvent clickEvent) {
                            if (path.contains("youtube.com") || path.contains("youtu.be")) {
                                // Afficher un indicateur de chargement
                                Label loading = new Label("⏳ Chargement de la vidéo...");
                                loading.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
                                videoCard.getChildren().clear();
                                videoCard.setPrefHeight(520);
                                videoCard.setAlignment(Pos.CENTER);
                                videoCard.getChildren().add(loading);
                                videoCard.setOnMouseClicked(null);

                                // Extraire l'URL directe en background
                                new Thread(() -> {
                                    try {
                                        System.out.println("[YT-DLP] Lancement pour : " + path);
                                        ProcessBuilder pb = new ProcessBuilder(
                                                "C:\\yt-dlp\\yt-dlp.exe",
                                                "-f", "best[ext=mp4]/best",
                                                "--get-url",
                                                "--no-warnings",
                                                path);
                                        pb.redirectErrorStream(false);
                                        Process process = pb.start();

                                        String streamUrl = new String(process.getInputStream().readAllBytes()).trim();
                                        process.waitFor();

                                        String finalUrl = streamUrl.split("\n")[0].trim();
                                        if (finalUrl.isEmpty() || !finalUrl.startsWith("http")) {
                                            Platform.runLater(() -> loading.setText("❌ URL invalide : " + finalUrl));
                                            return;
                                        }

                                        Platform.runLater(() -> {
                                            utils.VlcJavaFXPlayer vlcPlayer = new utils.VlcJavaFXPlayer();
                                            VBox playerBox = vlcPlayer.buildPlayer(finalUrl);

                                            HBox controls = (HBox) playerBox.getChildren().get(1);
                                            Button closeBtn = (Button) controls.getChildren().get(5);
                                            closeBtn.setOnAction(ev -> {
                                                vlcPlayer.cleanup();
                                                videoCard.getChildren().clear();
                                                videoCard.setPrefHeight(250);
                                                videoCard.getChildren().addAll(thumbnailView, playIcon);
                                                videoCard.setCursor(javafx.scene.Cursor.HAND);
                                                // Re-attacher le handler d'origine
                                                videoCard.setOnMouseClicked(this);
                                            });

                                            videoCard.getChildren().clear();
                                            videoCard.setPrefHeight(520);
                                            videoCard.getChildren().add(playerBox);
                                        });
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        Platform.runLater(() -> loading.setText("❌ Erreur : " + ex.getMessage()));
                                    }
                                }).start();
                            } else {
                                // Vidéo locale
                                File videoFile = resolveFilePath(path);
                                if (videoFile != null && videoFile.exists()) {
                                    utils.VlcJavaFXPlayer vlcPlayer = new utils.VlcJavaFXPlayer();
                                    VBox playerBox = vlcPlayer.buildPlayer(videoFile.getAbsolutePath());

                                    Button closeInCard = (Button) ((HBox) playerBox.getChildren().get(1))
                                            .getChildren().get(5);
                                    closeInCard.setOnAction(ev -> {
                                        vlcPlayer.cleanup();
                                        videoCard.getChildren().clear();
                                        videoCard.setPrefHeight(250);
                                        videoCard.getChildren().addAll(thumbnailView, playIcon);
                                        videoCard.setCursor(javafx.scene.Cursor.HAND);
                                        // Re-attacher le handler d'origine
                                        videoCard.setOnMouseClicked(this);
                                    });

                                    videoCard.getChildren().clear();
                                    videoCard.setPrefHeight(520);
                                    videoCard.getChildren().add(playerBox);
                                    videoCard.setOnMouseClicked(null);
                                } else {
                                    showAlert("Erreur", "Fichier introuvable : " + path);
                                }
                            }
                        }
                    };

                    videoCard.setOnMouseClicked(openVideoHandler);

                } else if ("PDF".equals(type)) {
                    if (path.startsWith("http")) {
                        WebView pdfView = new WebView();
                        pdfView.setPrefHeight(380);
                        pdfView.setMaxHeight(400);
                        String viewerUrl = "https://docs.google.com/gview?url=" + path + "&embedded=true";
                        pdfView.getEngine().load(viewerUrl);
                        card.getChildren().add(pdfView);
                    } else {
                        File pdfFile = resolveFilePath(path);
                        if (pdfFile != null && pdfFile.exists()) {
                            card.getChildren().add(createPdfViewer(pdfFile));
                        } else {
                            Label err = new Label("❌ Fichier PDF introuvable : " + path);
                            err.setStyle("-fx-text-fill: red;");
                            card.getChildren().add(err);
                        }
                    }

                    // ===== WORD (Programme) =====
                } else if ("WORD".equalsIgnoreCase(type)) {
                    Button b = new Button("📄 Voir le programme");
                    b.setStyle(
                            "-fx-background-color: linear-gradient(to right, #3A6B7E, #5C98A8); -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand;");
                    b.setOnAction(e -> ouvrirWordViewer(path, "Programme Thérapeutique", currentUserId, c.getId()));
                    card.getChildren().add(b);

                    // ===== PODCAST / AUDIO =====
                } else if ("PODCAST".equals(type) || "AUDIO".equals(type)) {
                    String audioSrc = path.startsWith("http") ? path : null;
                    if (audioSrc == null) {
                        File audioFile = resolveFilePath(path);
                        if (audioFile != null && audioFile.exists())
                            audioSrc = audioFile.toURI().toString();
                    }

                    if (audioSrc != null) {
                        // ✅ WebView au lieu de MediaPlayer
                        String html = "<html><head><style>"
                                + "body { background:#1E293B; display:flex; justify-content:center; align-items:center; height:100vh; margin:0; }"
                                + "audio { width:90%; outline:none; }"
                                + "</style></head><body>"
                                + "<audio controls autoplay src='" + audioSrc + "'></audio>"
                                + "</body></html>";

                        WebView audioView = new WebView();
                        audioView.setPrefHeight(80);
                        audioView.setPrefWidth(850);
                        audioView.getEngine().setJavaScriptEnabled(true);
                        audioView.getEngine().loadContent(html, "text/html");
                        card.getChildren().add(audioView);
                    } else {
                        card.getChildren().add(new Label("❌ Fichier audio introuvable."));
                    }

                } else {
                    Button b = new Button("📂 Ouvrir le contenu");
                    b.setStyle(
                            "-fx-background-color: #5C98A8; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold;");
                    b.setOnAction(e -> {
                        try {
                            if (path.startsWith("http"))
                                java.awt.Desktop.getDesktop().browse(new URI(path));
                            else
                                java.awt.Desktop.getDesktop().open(new File(path));
                        } catch (Exception ex) {
                            showAlert("Erreur", ex.getMessage());
                        }
                    });
                    card.getChildren().add(b);
                }

                contentsLearningContainer.getChildren().add(card);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // === CHARGEMENT ===

    private void loadAllFormations() {
        if (formationsFlowPane == null)
            return;
        filterAllFormations();
    }

    private void loadMyParticipations() {
        if (participationsFlowPane == null)
            return;
        filterMyParticipations();
    }

    @FXML
    private void filterAllFormations() {
        if (formationsFlowPane == null)
            return;
        String query = (searchField != null) ? searchField.getText().toLowerCase() : "";
        String cat = (categoryCombo != null) ? categoryCombo.getValue() : "Toutes";

        formationsFlowPane.getChildren().clear();
        try {
            userParticipations = participationServiceF.findByUserId(currentUserId);
            List<Formation> list = formationService.read();
            for (Formation f : list) {
                boolean matchSearch = f.getTitre().toLowerCase().contains(query) ||
                        f.getDescription().toLowerCase().contains(query);
                boolean matchCat = cat == null || cat.equals("Toutes") || cat.equals(f.getCategorie());

                // Admin sees everything, Psychologist only sees their own formations
                boolean matchMine = isAdmin() || !isPsychologue() || f.getIdCreateur() == currentUserId;

                if (matchSearch && matchCat && matchMine) {
                    formationsFlowPane.getChildren().add(createFormationCard(f));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadPendingDemandes() {
        if (demandesList == null)
            return;
        demandesList.getChildren().clear();
        try {
            // Un admin voit toutes les demandes, un psy ne voit que celles de ses
            // formations
            List<Formation> targetFormations;
            if (isAdmin()) {
                targetFormations = formationService.read();
            } else {
                targetFormations = formationService.findByOwner(currentUserId);
            }
            for (Formation f : targetFormations) {
                List<Participation> ps = participationServiceF.findByFormationId(f.getId());
                for (Participation p : ps) {
                    if ("en attente".equalsIgnoreCase(p.getStatut())) {
                        demandesList.getChildren().add(createDemandeItem(p, f.getTitre()));
                    }
                }
            }
            if (demandesList.getChildren().isEmpty()) {
                javafx.scene.control.Label empty = new javafx.scene.control.Label(
                        "Aucune demande d'inscription en attente.");
                empty.setStyle("-fx-text-fill: #5F7F8B; -fx-font-style: italic; -fx-padding: 20 0;");
                demandesList.getChildren().add(empty);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox createDemandeItem(Participation p, String formationTitle) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle(
                "-fx-background-color: #F8FBFC; -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: #DDECEF; -fx-border-radius: 12;");

        VBox info = new VBox(5);
        Label userLabel = new Label("Patient: " + p.getTitreFormation()); // titreFormation contient déjà Nom + Prénom
        userLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label formLabel = new Label("Formation: " + formationTitle);
        formLabel.setStyle("-fx-text-fill: #5F7F8B; -fx-font-size: 12px;");
        info.getChildren().addAll(userLabel, formLabel);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button accept = new Button("Accepter");
        accept.setStyle(
                "-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;");
        accept.setOnAction(e -> handleUpdateStatus(p.getIdParticipation(), "accepté"));

        Button refuse = new Button("Refuser");
        refuse.setStyle(
                "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;");
        refuse.setOnAction(e -> handleUpdateStatus(p.getIdParticipation(), "refusé"));

        item.getChildren().addAll(info, spacer, accept, refuse);
        return item;
    }

    private void handleUpdateStatus(int idParticipation, String status) {
        try {
            participationServiceF.updateStatut(idParticipation, status);
            loadPendingDemandes();
            updateNotificationContent();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadInscritsForFormation() {
        if (inscritsList == null || inscritsFormationCombo.getValue() == null)
            return;
        inscritsList.getChildren().clear();
        Formation selected = inscritsFormationCombo.getValue();
        try {
            List<Participation> all = participationServiceF.findByFormationId(selected.getId());
            boolean any = false;
            for (Participation p : all) {
                if ("accepté".equalsIgnoreCase(p.getStatut())) {
                    any = true;
                    HBox item = new HBox(15);
                    item.setAlignment(Pos.CENTER_LEFT);
                    item.setStyle(
                            "-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 12; " +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4); " +
                                    "-fx-border-color: #F1F5F9; -fx-border-radius: 12;");

                    // Elegant user avatar placeholder
                    VBox avatar = new VBox();
                    avatar.setAlignment(Pos.CENTER);
                    avatar.setMinWidth(45);
                    avatar.setMinHeight(45);
                    avatar.setStyle("-fx-background-color: #E0F2FE; -fx-background-radius: 50;");
                    Label icon = new Label("👤");
                    icon.setStyle("-fx-font-size: 20px;");
                    avatar.getChildren().add(icon);

                    VBox details = new VBox(3);
                    Label name = new Label(p.getTitreFormation()); // Assuming this represents the user's name in this
                                                                   // context
                    name.setStyle("-fx-font-weight: 800; -fx-text-fill: #1E293B; -fx-font-size: 15px;");
                    Label date = new Label("Inscrit le : "
                            + new java.text.SimpleDateFormat("dd MMM yyyy").format(p.getDateInscription()));
                    date.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
                    details.getChildren().addAll(name, date);

                    item.getChildren().addAll(avatar, details);
                    inscritsList.getChildren().add(item);
                }
            }
            if (!any) {
                Label empty = new Label("Aucun patient accepté pour cette formation.");
                empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-padding: 20 0;");
                inscritsList.getChildren().add(empty);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void filterMyParticipations() {
        if (participationsFlowPane == null)
            return;
        String query = (myParticipationsSearchField != null) ? myParticipationsSearchField.getText().toLowerCase() : "";
        String cat = (categoryCombo != null) ? categoryCombo.getValue() : "Toutes";

        participationsFlowPane.getChildren().clear();
        try {
            userParticipations = participationServiceF.findByUserId(currentUserId);
            boolean hasAccepted = false;
            for (Participation p : userParticipations) {
                boolean matchSearch = p.getTitreFormation().toLowerCase().contains(query) ||
                        (p.getDescription() != null && p.getDescription().toLowerCase().contains(query));
                boolean matchCat = cat == null || cat.equals("Toutes") || cat.equals(p.getCategorie());
                boolean isAccepted = "accepté".equalsIgnoreCase(p.getStatut());

                if (matchSearch && matchCat && isAccepted) {
                    participationsFlowPane.getChildren().add(createParticipationCard(p));
                    hasAccepted = true;
                }
            }
            noParticipationsBox.setVisible(!hasAccepted);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // === LOGIQUE DES NOTIFICATIONS ===

    @FXML
    private void showNotificationPopup() {
        if (notificationsPopup != null) {
            notificationsPopup.setVisible(!notificationsPopup.isVisible());
            if (notificationsPopup.isVisible()) {
                updateNotificationContent();
            }
        }
    }

    private HBox createSeanceNotifItem(models.SeanceGroupe s) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle(
                "-fx-background-color: #F0F9FF; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #BAE6FD;");

        VBox vb = new VBox(3);
        Label t = new Label("🎥 " + s.getTitre());
        t.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0369A1;");

        Label fTitle = new Label("📚 " + s.getTitreFormation());
        fTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #0EA5E9; -fx-font-weight: bold;");

        Label d = new Label("📅 " + s.getDateHeure().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
        d.setStyle("-fx-font-size: 11px; -fx-text-fill: #0C4A6E;");
        vb.getChildren().addAll(t, fTitle, d);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button join = new Button("Rejoindre");
        join.setStyle(
                "-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");

        join.setOnAction(e -> {
            closeNotifications();
            showSessionsView(); // Redirection vers l'onglet Séances en ligne
        });

        item.getChildren().addAll(vb, spacer, join);
        return item;
    }

    private void openSeanceInterface(models.SeanceGroupe s) {
        try {
            String userName = "Participant";
            utils.UserSession userSession = utils.UserSession.getInstance();
            if (userSession != null && userSession.getUser() != null) {
                userName = userSession.getUser().getPrenom() + " " + userSession.getUser().getNom();
            }

            closeNotifications();
            String jitsiUrl = buildJitsiUrl(s, userName);

            // switchView(activeSeanceView); // Supprimé à la demande du user
            // ✅ Ouverture en OVERLAY AWT recouvrant l'application
            javafx.stage.Stage stage = (javafx.stage.Stage) jitsiContainer.getScene().getWindow();
            JitsiMeetWindowF.openOverlay(stage, s.getSeanceId(), currentUserId, jitsiUrl, s.getTitre(),
                    "Formation : " + s.getTitreFormation(), (isAdmin() || isPsychologue()), s.getPsychoName());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la séance : " + e.getMessage());
        }
    }

    private String buildJitsiUrl(models.SeanceGroupe s, String userName) {
        try {
            String name = java.net.URLEncoder.encode(userName, "UTF-8");
            String baseUrl = "https://meet.ffmuc.net/MindCare-Session-" + s.getSeanceId();
            return baseUrl + "#userInfo.displayName=\"" + name + "\"" +
                    "&userInfo.role=\"" + currentUserRole + "\"" +
                    "&config.prejoinPageEnabled=false" +
                    "&config.startWithAudioMuted=false" +
                    "&config.startWithVideoMuted=false";
        } catch (Exception e) {
            return "about:blank";
        }
    }

    @FXML
    private void closeNotifications() {
        if (notificationsPopup != null) {
            notificationsPopup.setVisible(false);
        }
    }

    // === DICTIONNAIRE IA POPUP ===

    @FXML
    private void toggleDicoPopup() {
        if (dicoPopup != null) {
            dicoPopup.setVisible(!dicoPopup.isVisible());
            if (dicoPopup.isVisible()) {
                dicoPopup.toFront();
                if (dicoInputField != null) {
                    dicoInputField.requestFocus();
                }
            }
        }
    }

    @FXML
    private void onDicoTraduire() {
        String motSaisi = dicoInputField != null ? dicoInputField.getText() : null;
        if (motSaisi == null || motSaisi.trim().isEmpty()) {
            return;
        }

        if (dicoLoading != null)
            dicoLoading.setVisible(true);
        if (dicoExplicationLabel != null)
            dicoExplicationLabel.setText("Analyse en cours...");
        if (dicoExpliquerBtn != null)
            dicoExpliquerBtn.setDisable(true);

        String motFinal = motSaisi.trim();

        new Thread(() -> {
            String systemPrompt = "Tu es un psychologue qui explique des mots, concepts ou expressions liés au domaine médical et psychologique. Fournis UNE DÉFINITION simple et claire pour être compréhensible par un patient. Ne fournis PAS de traduction, ni d'exemples complexes. Juste la définition claire. IMPORTANT: Si le mot demandé n'a AUCUN LIEN avec la santé mentale, la psychologie, la thérapie ou la médecine, refuse poliment de répondre en expliquant que tu ne définis que le vocabulaire médical.";
            String userPrompt = "Définis : \"" + motFinal + "\".";

            String reponse = GroqServiceF.appeler(systemPrompt, userPrompt, GroqServiceF.MODEL_RAPIDE);

            javafx.application.Platform.runLater(() -> {
                if (dicoLoading != null)
                    dicoLoading.setVisible(false);
                if (dicoExpliquerBtn != null)
                    dicoExpliquerBtn.setDisable(false);

                if (reponse != null && !reponse.isEmpty()) {
                    if (dicoExplicationLabel != null)
                        dicoExplicationLabel.setText(reponse.trim());
                } else {
                    if (dicoExplicationLabel != null)
                        dicoExplicationLabel.setText("❌ Erreur. Réessayez.");
                }
            });
        }).start();
    }

    @FXML
    private void closeActiveSeance() {
        if (activeSeanceView != null) {
            activeSeanceView.setVisible(false);
            activeSeanceView.setManaged(false);
        }
        showSessionsView();
    }

    private void updateNotificationContent() {
        if (notificationsList == null)
            return;
        notificationsList.getChildren().clear();
        int count = 0;

        try {
            boolean isAdminOrPsy = isStaff();

            if (isAdminOrPsy) {
                // Pour Admin/Psy: On affiche les nouvelles demandes
                List<Formation> myFormations = formationService.findByOwner(currentUserId);
                for (Formation f : myFormations) {
                    List<Participation> ps = participationServiceF.findByFormationId(f.getId());
                    for (Participation p : ps) {
                        if ("en attente".equalsIgnoreCase(p.getStatut())) {
                            notificationsList.getChildren().add(createAdminNotifItem(p, f.getTitre()));
                            count++;
                        }
                    }
                }
            } else {
                // Pour Patient: On affiche les status des demandes et les SÉANCES EN LIGNE
                List<Participation> myPs = participationServiceF.findByUserId(currentUserId);
                for (Participation p : myPs) {
                    if (!"en attente".equalsIgnoreCase(p.getStatut())) {
                        notificationsList.getChildren().add(createPatientNotifItem(p));
                        count++;
                    }
                    // Si accepté, on check les séances à venir
                    if ("accepté".equalsIgnoreCase(p.getStatut())) {
                        List<models.SeanceGroupe> seances = seanceService.findByFormation(p.getIdFormation());
                        for (models.SeanceGroupe s : seances) {
                            if (s.getDateHeure().isAfter(LocalDateTime.now())) {
                                notificationsList.getChildren().add(createSeanceNotifItem(s));
                                count++;
                            }
                        }
                    }
                }
            }

            if (count == 0) {
                Label empty = new Label("Aucune nouvelle notification.");
                empty.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-padding: 10;");
                notificationsList.getChildren().add(empty);
                notificationBadge.setVisible(false);
            } else {
                notificationBadge.setVisible(true);
                notificationCountLabel.setText(String.valueOf(count));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createAdminNotifItem(Participation p, String formationTitle) {
        VBox item = new VBox(8);
        item.setStyle(
                "-fx-background-color: #F8FBFC; -fx-padding: 12; -fx-background-radius: 10; -fx-border-color: #DDECEF; -fx-border-radius: 10;");

        Label title = new Label("Nouvelle demande");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #5C98A8; -fx-font-size: 13px;");

        Label content = new Label(p.getTitreFormation() + " souhaite participer à " + formationTitle);
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 12px; -fx-text-fill: #1F2A33;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btn = new Button("Gérer");
        btn.setStyle(
                "-fx-background-color: #5C98A8; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 5;");
        btn.setOnAction(e -> {
            showDemandes();
            closeNotifications();
        });
        actions.getChildren().add(btn);

        item.getChildren().addAll(title, content, actions);
        return item;
    }

    private VBox createPatientNotifItem(Participation p) {
        VBox item = new VBox(5);
        boolean isAccepted = "accepté".equalsIgnoreCase(p.getStatut());
        String color = isAccepted ? "#10B981" : "#EF4444";
        item.setStyle("-fx-background-color: #F8FBFC; -fx-padding: 12; -fx-background-radius: 10; -fx-border-color: "
                + color + "; -fx-border-radius: 10; -fx-border-width: 0 0 0 4;");

        Label title = new Label(isAccepted ? "Demande Acceptée ! 🎉" : "Demande Refusée");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-font-size: 13px;");

        Label content = new Label("Votre demande pour '" + p.getTitreFormation() + "' a été " + p.getStatut() + ".");
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 12px; -fx-text-fill: #1F2A33;");

        item.getChildren().addAll(title, content);
        return item;
    }

    // === CARTES ===
    private VBox createFormationCard(Formation f) {
        VBox card = new VBox(12);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(240);

        StackPane imgCont = new StackPane();
        ImageView iv = new ImageView();
        iv.setFitHeight(130);
        iv.setFitWidth(215);
        iv.setPreserveRatio(true);
        setImageSafe(iv, f.getImagePath());

        // Boutons d'administration en haut à droite
        HBox adminBtns = new HBox(8);
        adminBtns.setAlignment(Pos.TOP_RIGHT);
        adminBtns.setPadding(new Insets(10));
        adminBtns.setPickOnBounds(true); // Ensure clicks are caught

        boolean isCreator = (f.getIdCreateur() == currentUserId);
        boolean canManage = isCreator; // Admin/Psy can only manage if they are the owner

        if (isCreator) {
            Button editIcon = new Button("✎");
            editIcon.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.9); -fx-text-fill: #5C98A8; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 14px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
            editIcon.setOnAction(e -> {
                e.consume();
                openEditFormation(f);
            });
            adminBtns.getChildren().add(editIcon);
        }

        if (isCreator || isAdmin()) {
            Button delIcon = new Button("🗑");
            delIcon.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.9); -fx-text-fill: #EF4444; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 14px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
            delIcon.setOnAction(e -> {
                e.consume();
                handleDeleteFormation(f);
            });
            adminBtns.getChildren().add(delIcon);
        }

        if (canManage || isAdmin()) {
            card.setOnMouseClicked(e -> {
                if (e.getClickCount() == 1) {
                    loadModulesForLearning(f);
                }
            });
            card.setStyle(card.getStyle() + "-fx-cursor: hand;");
        }
        imgCont.getChildren().setAll(iv, adminBtns);

        Label t = new Label(f.getTitre());
        t.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1F2A33;");
        t.setWrapText(true);

        // Ajout des étoiles de notation
        HBox stars = createRatingStars(f.getAverageRating());

        HBox info = new HBox(10);
        info.setAlignment(Pos.CENTER_LEFT);

        String creatorName = "Expert MindCare";
        try {
            models.User u = userServiceF.findById(f.getIdCreateur());
            if (u != null)
                creatorName = "Dr " + u.getPrenom() + " " + u.getNom();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Label exp = new Label("👤 " + creatorName);
        exp.setStyle("-fx-text-fill: #6E8E9A; -fx-font-size: 11px;");
        Label dur = new Label("⏱ " + f.getDuree());
        dur.setStyle("-fx-text-fill: #6E8E9A; -fx-font-size: 11px;");

        // Nombre d'inscrits
        int count = 0;
        try {
            count = participationServiceF.countParticipants(f.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Label ins = new Label("👥 " + count + " inscrits");
        ins.setStyle("-fx-text-fill: #5C98A8; -fx-font-size: 11px; -fx-font-weight: bold;");

        info.getChildren().addAll(exp, dur, ins);

        HBox bottom = new HBox(10);
        bottom.setAlignment(Pos.CENTER_LEFT);

        if (isAdmin()) {
            // Admin doesn't join, but can manage
        } else if (isPsychologue() && f.getIdCreateur() == currentUserId) {
            // Creator doesn't join themselves
        } else {
            Participation userP = userParticipations.stream().filter(p -> p.getIdFormation() == f.getId()).findFirst()
                    .orElse(null);

            if (userP != null) {
                String status = userP.getStatut();
                if ("accepté".equalsIgnoreCase(status)) {
                    Label jl = new Label("Déjà rejoint ✅");
                    jl.setStyle(
                            "-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-color: #ECFDF5; -fx-background-radius: 10;");
                    bottom.getChildren().add(jl);
                } else if ("en attente".equalsIgnoreCase(status)) {
                    Button pl = new Button("En attente ⏳");
                    pl.setTooltip(new Tooltip("Double-cliquez pour annuler votre demande"));
                    pl.setStyle(
                            "-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-color: #FFFBEB; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: transparent;");

                    // Double click to cancel registration
                    pl.setOnMouseClicked(event -> {
                        if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY && event.getClickCount() == 2) {
                            try {
                                participationServiceF.deleteByUserAndFormation(currentUserId, f.getId());
                                showAlert("Succès", "Votre demande d'inscription a été annulée.");
                                loadAllFormations();
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                    bottom.getChildren().add(pl);
                }
            } else {
                Button join = new Button("Rejoindre");
                join.setStyle(
                        "-fx-background-color: #5C98A8; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-font-weight: 800; -fx-padding: 9 22;");
                join.setOnAction(e -> handleRegistration(f));
                bottom.getChildren().add(join);
            }
        }

        Button det = new Button("Détails");
        det.setStyle(
                "-fx-background-color: #F0F9FF; -fx-text-fill: #0284C7; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 15; -fx-border-color: #BAE6FD; -fx-border-radius: 10;");
        det.setOnAction(e -> showFormationDetailsDialog(f));

        bottom.getChildren().add(det);

        if (isCreator || isAdmin()) {
            Button addSeance = new Button("🎥 +");
            addSeance.setTooltip(new Tooltip("Planifier une séance en ligne"));
            addSeance.setStyle(
                    "-fx-background-color: #EAF3F5; -fx-text-fill: #5C98A8; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 12; -fx-border-color: #AACFD9; -fx-border-radius: 10;");
            addSeance.setOnAction(e -> showAddSeanceDialog(f));
            bottom.getChildren().add(addSeance);
        }

        card.getChildren().addAll(imgCont, t, stars, info, bottom);
        return card;
    }

    private void handleDeleteFormation(Formation f) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer la formation ?");
        alert.setContentText("Cette action est irréversible.");
        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                formationService.delete(f.getId());
                loadAllFormations();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void showFormationDetailsDialog(Formation f) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de la Formation");

        // Main container
        VBox content = new VBox(0);
        content.setPrefWidth(520);
        content.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 16;");

        // ── Header — Simple & Pro ──
        HBox header = new HBox(16);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: white; " +
                        "-fx-padding: 24 28; -fx-background-radius: 16 16 0 0; " +
                        "-fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

        Label iconLbl = new Label("🎓");
        iconLbl.setStyle("-fx-font-size: 32px; -fx-text-fill: #5C98A8;"); // Icon as accent

        VBox headerText = new VBox(2);
        Label title = new Label(f.getTitre());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #1E293B;");
        title.setWrapText(true);

        int count = 0;
        try {
            count = participationServiceF.countParticipants(f.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Label sub = new Label(f.getNiveau() + "  •  " + f.getDuree() + "  •  👥 " + count + " inscrits");
        sub.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px; -fx-font-weight: 600;");
        headerText.getChildren().addAll(title, sub);
        header.getChildren().addAll(iconLbl, headerText);

        // ── Body — Minimalist — Simple & Pro ──
        VBox body = new VBox(24);
        body.setStyle("-fx-padding: 24 28;");

        // Section À propos
        VBox descSec = new VBox(8);
        Label dTitle = new Label("📌 À propos");
        dTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #5C98A8;");
        Label desc = new Label(
                f.getDescription() == null || f.getDescription().isEmpty() ? "Génération du résumé par l'IA..."
                        : f.getDescription());
        desc.setWrapText(true);
        desc.setPrefWidth(460); // Use prefered width to force wrap
        desc.setMaxWidth(464);
        desc.setMinHeight(Region.USE_PREF_SIZE);
        desc.setStyle("-fx-text-fill: #334155; -fx-font-size: 14px; -fx-line-spacing: 1.5;");
        descSec.getChildren().addAll(dTitle, desc);

        // AI Generation logic
        if (f.getDescription() == null || f.getDescription().trim().isEmpty()
                || f.getDescription().equals("Aucune description disponible.")) {
            Thread aiThread = new Thread(() -> {
                try {
                    List<Module> mods = moduleServiceF.findByFormationId(f.getId());
                    if (!mods.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Titre: ").append(f.getTitre()).append(". Modules: ");
                        for (Module m : mods)
                            sb.append(m.getTitre()).append(", ");

                        String prompt = "Résume en 3 phrases ce que l'utilisateur va apprendre dans cette formation en psychologie: "
                                + sb.toString()
                                + ". Si tu ne comprends pas ou si le contenu est insuffisant, réponds uniquement par 'Aucun'.";
                        String summary = GroqServiceF.appeler(prompt);

                        javafx.application.Platform.runLater(() -> {
                            if (summary != null && !summary.equalsIgnoreCase("Aucun") && !summary.isEmpty()) {
                                desc.setText(summary);
                                f.setDescription(summary); // cache it
                            } else {
                                desc.setText("Aucun");
                            }
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> desc.setText("Aucun"));
                    }
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> desc.setText("Aucun"));
                }
            });
            aiThread.setDaemon(true);
            aiThread.start();
        }

        // Section Programme
        VBox progSec = new VBox(12);
        Label pTitle = new Label("📚 Programme");
        pTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #5C98A8;");
        VBox progRows = new VBox(8);
        try {
            List<Module> mods = moduleServiceF.findByFormationId(f.getId());
            if (mods.isEmpty()) {
                Label empty = new Label("Aucun module répertorié.");
                empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
                progRows.getChildren().add(empty);
            } else {
                for (Module m : mods) {
                    HBox row = new HBox(12);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    Label bullet = new Label("●"); // Bullet plus sobre
                    bullet.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 8px;");
                    Label ml = new Label(m.getTitre());
                    ml.setStyle("-fx-text-fill: #1E293B; -fx-font-weight: 500; -fx-font-size: 14px;");
                    row.getChildren().addAll(bullet, ml);
                    progRows.getChildren().add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        progSec.getChildren().addAll(pTitle, progRows);

        body.getChildren().addAll(descSec, progSec);

        ScrollPane scrollBody = new ScrollPane(body);
        scrollBody.setFitToWidth(true);
        scrollBody.setPrefHeight(450); // Hauteur confortable avant scroll
        scrollBody.setMaxHeight(550);
        scrollBody.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollBody.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        content.getChildren().addAll(header, scrollBody);

        // Style the dialog pane
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #F8FAFC; -fx-padding: 0; -fx-background-radius: 16;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button closeBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setStyle("-fx-background-color: white; -fx-text-fill: #64748B; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-padding: 8 22; -fx-cursor: hand;");
        dialog.showAndWait();
    }

    private VBox createParticipationCard(Participation p) {
        VBox card = new VBox(15);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 12, 0, 0, 6); -fx-cursor: hand; " +
                        "-fx-border-color: #F1F5F9; -fx-border-radius: 15;");
        card.setPrefWidth(240);

        // Premium Hover Effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 20, 0, 0, 10); -fx-background-color: #F8FAFC;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace(
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 20, 0, 0, 10); -fx-background-color: #F8FAFC;",
                "")));
        card.setOnMouseClicked(e -> openModulesView(p));

        StackPane imgCont = new StackPane();
        ImageView iv = new ImageView();
        iv.setFitHeight(130);
        iv.setFitWidth(215);
        iv.setPreserveRatio(true);
        // Rounded corners for image
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(215, 130);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        iv.setClip(clip);
        setImageSafe(iv, p.getImagePath());
        imgCont.getChildren().add(iv);

        VBox details = new VBox(8);
        Label t = new Label(p.getTitreFormation());
        t.setStyle("-fx-font-weight: 800; -fx-font-size: 16px; -fx-text-fill: #1E293B;");
        t.setWrapText(true);
        t.setMinHeight(40);

        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        String stColor = "#64748B";
        String stBg = "#F1F5F9";
        String displayStatut = p.getStatut().toUpperCase();

        switch (p.getStatut().toLowerCase()) {
            case "accepté" -> {
                stColor = "#10B981";
                stBg = "#D1FAE5";
            }
            case "en attente" -> {
                stColor = "#F59E0B";
                stBg = "#FEF3C7";
            }
            case "refusé" -> {
                stColor = "#EF4444";
                stBg = "#FEE2E2";
            }
        }

        Label stLabel = new Label(displayStatut);
        stLabel.setStyle("-fx-background-color: " + stBg + "; -fx-text-fill: " + stColor
                + "; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 10px; -fx-font-weight: 800;");
        statusBox.getChildren().add(stLabel);

        VBox actions = new VBox(10);
        boolean isAccepted = "accepté".equalsIgnoreCase(p.getStatut());

        Button start = new Button(isAccepted ? "🚀 Commencer" : "⏳ En attente");
        start.setMaxWidth(Double.MAX_VALUE);
        start.setDisable(!isAccepted);
        start.setStyle(
                "-fx-background-color: " + (isAccepted ? "#5C98A8" : "#E2E8F0") + "; " +
                        "-fx-text-fill: " + (isAccepted ? "white" : "#94A3B8") + "; " +
                        "-fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 10; -fx-cursor: hand;");
        start.setOnAction(e -> {
            e.consume();
            openModulesView(p);
        });
        actions.getChildren().add(start);

        // Rating Button
        try {
            if (isAccepted && !participationServiceF.hasRated(currentUserId, p.getIdFormation())) {
                Button rate = new Button("⭐ Noter cette formation");
                rate.setMaxWidth(Double.MAX_VALUE);
                rate.setStyle("-fx-background-color: #FFFDF0; -fx-text-fill: #D97706; -fx-border-color: #FCD34D; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 10; -fx-cursor: hand;");
                rate.setOnAction(e -> {
                    e.consume();
                    showRatingPopup(p.getIdFormation());
                });
                actions.getChildren().add(rate);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Button cancel = new Button("Annuler l'inscription");
        cancel.setMaxWidth(Double.MAX_VALUE);
        cancel.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-cursor: hand;");
        cancel.setOnAction(e -> {
            e.consume();
            try {
                participationServiceF.delete(p.getIdParticipation());
                loadMyParticipations();
                loadAllFormations();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        actions.getChildren().add(cancel);

        String creatorName = "Expert MindCare";
        try {
            models.User u = userServiceF.findById(p.getIdCreateur());
            if (u != null)
                creatorName = "Dr " + u.getPrenom() + " " + u.getNom();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Label expLabel = new Label("👤 " + creatorName);
        expLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: 600;");

        card.getChildren().addAll(imgCont, t, expLabel, statusBox, actions);
        return card;
    }

    private void handleRegistration(Formation f) {
        Participation p = new Participation();
        p.setIdUser(currentUserId);
        p.setIdFormation(f.getId());
        p.setDateInscription(new Date());
        // Les patients sont "en attente", les admins/psys sont "accepté" par défaut
        boolean isAdminOrPsy = isStaff();

        // If admin/psy manually triggers registration (e.g. from search), force it to
        // accepted
        p.setStatut(isAdminOrPsy ? "accepté" : "en attente");
        try {
            participationServiceF.create(p);
            loadAllFormations();
            updateNotificationContent();
            if (!isAdminOrPsy) {
                showAlert("Inscription envoyée", "Votre demande est en attente de validation par un administrateur.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showRatingPopup(int formationId) {
        javafx.application.Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RatingPopup.fxml"));
                Parent root = loader.load();
                RatingPopupController ctrl = loader.getController();
                ctrl.setFormationId(formationId);

                Stage stage = new Stage();
                stage.setTitle("Évaluez votre formation");
                if (allFormationsView != null && allFormationsView.getScene() != null) {
                    stage.initOwner(allFormationsView.getScene().getWindow());
                }
                stage.setScene(new javafx.scene.Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private HBox createRatingStars(double average) {
        HBox stars = new HBox(2);
        stars.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= Math.round(average) ? "★" : "☆");
            star.setStyle("-fx-text-fill: #FBC02D; -fx-font-size: 14px;");
            stars.getChildren().add(star);
        }
        if (average > 0) {
            Label sum = new Label(String.format(" (%.1f)", average));
            sum.setStyle("-fx-text-fill: #6E8E9A; -fx-font-size: 11px;");
            stars.getChildren().add(sum);
        }
        return stars;
    }

    private void loadModuleToEdit(Module m) {
        this.moduleEnCours = m;
        moduleTitreField.setText(m.getTitre());
        moduleDescriptionArea.setText(m.getDescription());
        contenusListView.getItems().clear();
        if (m.getId() > 0 && m.getContenus().isEmpty()) {
            try {
                m.setContenus(contenuServiceF.findByModuleId(m.getId()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (Contenu c : m.getContenus()) {
            String path = c.getChemin();
            String fileName = path.startsWith("http") ? "URL Vidéo" : new File(path).getName();
            contenusListView.getItems().add(c.getType() + " - " + fileName);
        }
    }

    @FXML
    private void selectImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            String relativePath = copyToUploads(f, "images");
            selectedImagePath = (relativePath != null) ? relativePath : f.getAbsolutePath();
            imagePathLabel.setText(f.getName());
            setImageSafe(imagePreview, selectedImagePath);
        }
    }

    @FXML
    private void ajouterPDF() {
        choisirFichier("PDF", "*.pdf");
    }

    @FXML
    private void ajouterVideo() {
        choisirFichier("VIDEO", "*.mp4", "*.mkv", "*.avi");
    }

    private void choisirFichier(String type, String... exts) {
        if (moduleEnCours == null)
            moduleEnCours = new Module();
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(type, exts));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            // Copy file into the project uploads folder
            String subDir;
            switch (type) {
                case "VIDEO":
                    subDir = "videos";
                    break;
                case "PDF":
                    subDir = "pdfs";
                    break;
                default:
                    subDir = "audio";
                    break;
            }
            String relativePath = copyToUploads(f, subDir);
            String savedPath = (relativePath != null) ? relativePath : f.getAbsolutePath();

            Contenu c = new Contenu();
            c.setType(type);
            c.setChemin(savedPath);
            moduleEnCours.ajouterContenu(c);
            contenusListView.getItems().add(type + " - " + f.getName());
        }
    }

    /**
     * Resolves any file path — absolute or relative (uploads\videos\...) — to a
     * File.
     * Returns null if the path cannot be resolved.
     */
    private File resolveFilePath(String path) {
        if (path == null || path.isEmpty())
            return null;

        // Si le chemin commence par "../", on le nettoie pour la résolution interne
        String cleanPath = path.startsWith("../") ? path.substring(3) : path;
        // On remplace les \ par / pour la plateforme
        cleanPath = cleanPath.replace("\\", "/");

        // 1. Chemin absolu direct
        File direct = new File(path);
        if (direct.isAbsolute() && direct.exists())
            return direct;

        // 2. Chemin relatif depuis le root du projet (pour les uploads)
        File fromProjectRoot = new File(System.getProperty("user.dir"), cleanPath);
        if (fromProjectRoot.exists())
            return fromProjectRoot;

        // 3. Chemin relatif depuis resources (pour les assets statiques)
        File fromResources = getResourcesBase().resolve(cleanPath).toFile();
        if (fromResources.exists())
            return fromResources;

        // 4. Fallback spécial MySQL (backslashes supprimés)
        if (!path.contains("/") && !path.contains("\\") && path.contains("uploads")) {
            String dir = null;
            String fileName = null;
            if (path.contains("uploadspdfs")) {
                dir = "pdfs";
                fileName = path.substring(path.indexOf("uploadspdfs") + 11);
            } else if (path.contains("uploadsvideos")) {
                dir = "videos";
                fileName = path.substring(path.indexOf("uploadsvideos") + 13);
            } else if (path.contains("uploadsaudio")) {
                dir = "audio";
                fileName = path.substring(path.indexOf("uploadsaudio") + 12);
            } else if (path.contains("uploadsimages")) {
                dir = "images";
                fileName = path.substring(path.indexOf("uploadsimages") + 13);
            }
            if (dir != null && fileName != null) {
                File recovered = getUploadsBase().resolve(dir).resolve(fileName).toFile();
                if (recovered.exists())
                    return recovered;
            }
        }

        System.out.println("❌ Fichier introuvable : " + path);
        System.out.println("   Cherché dans (clean) : " + fromResources.getAbsolutePath());
        return null;
    }

    /** Returns the src/main/resources directory (project resources root). */
    private Path getResourcesBase() {
        return Paths.get(System.getProperty("user.dir"), "src", "main", "resources");
    }

    /** Returns the project root directory (for dynamic uploads). */
    private Path getProjectRoot() {
        return Paths.get(System.getProperty("user.dir"));
    }

    /**
     * Returns the uploads/ folder at the project root.
     */
    private Path getUploadsBase() {
        return getProjectRoot().resolve("uploads");
    }

    /**
     * Copies a file into uploads/{subDir}/ and returns its relative path
     * (e.g. "../uploads/videos/my_movie.mp4") for DB storage.
     * Returns null if the copy fails.
     */
    private String copyToUploads(File source, String subDir) {
        try {
            Path destDir = getUploadsBase().resolve(subDir);
            Files.createDirectories(destDir);
            String name = source.getName();
            Path dest = destDir.resolve(name);
            // Avoid overwriting: add timestamp prefix on collision
            if (Files.exists(dest)) {
                name = System.currentTimeMillis() + "_" + name;
                dest = destDir.resolve(name);
            }
            Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            // Saved path uses '../' relative path as requested and ALWAYS uses '/'
            return "../uploads/" + subDir + "/" + name;
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur copie", "Impossible de copier le fichier : " + e.getMessage());
            return null;
        }
    }

    @FXML
    private void supprimerContenu() {
        int idx = contenusListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && moduleEnCours != null) {
            Contenu c = moduleEnCours.getContenus().get(idx);
            if (c.getId() > 0) {
                try {
                    contenuServiceF.delete(c.getId());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            moduleEnCours.getContenus().remove(idx);
            contenusListView.getItems().remove(idx);
        }
    }

    @FXML
    private void ajouterModule() {
        if (moduleTitreField.getText().isEmpty()) {
            showAlert("Erreur", "Titre du module requis.");
            return;
        }
        if (moduleEnCours == null)
            moduleEnCours = new Module();
        moduleEnCours.setTitre(moduleTitreField.getText());
        moduleEnCours.setDescription(moduleDescriptionArea.getText());

        if (!modulesToAdd.contains(moduleEnCours)) {
            modulesToAdd.add(moduleEnCours);
        }
        modulesListView.getItems().setAll(modulesToAdd);

        // Reset
        moduleEnCours = new Module();
        moduleTitreField.clear();
        moduleDescriptionArea.clear();
        contenusListView.getItems().clear();
    }

    @FXML
    private void goToNextStep() {
        if (titreField.getText().isEmpty()) {
            showAlert("Erreur", "Veuillez saisir un titre avant de continuer.");
            return;
        }
        goToStep(2);
    }

    @FXML
    private void goToPreviousStep() {
        goToStep(1);
    }

    private void goToStep(int step) {

        if (step1Title != null) {
            step1Title.setText(editFormationOnly ? "Modifier les Informations" : "Informations Générales");
            // Hide the header (title + separator) if we are in isolated mode
            step1Title.getParent().setVisible(!editModuleOnly);
            step1Title.getParent().setManaged(!editModuleOnly);
        }
        if (step2Title != null) {
            step2Title.setText(editModuleOnly ? "Modifier le Module" : "Programme & Contenus");
            // Hide the header (title + separator) if we are in isolated mode
            step2Title.getParent().setVisible(!editFormationOnly);
            step2Title.getParent().setManaged(!editFormationOnly);
        }

        if (step1Container != null) {
            step1Container.setVisible(step == 1 && !editModuleOnly);
            step1Container.setManaged(step == 1 && !editModuleOnly);
        }
        if (step2Container != null) {
            step2Container.setVisible(step == 2 && !editFormationOnly);
            step2Container.setManaged(step == 2 && !editFormationOnly);
        }

        if (nextStepBtn != null) {
            nextStepBtn.setVisible(step == 1 && !editFormationOnly && !editModuleOnly);
            nextStepBtn.setManaged(step == 1 && !editFormationOnly && !editModuleOnly);
        }
        if (prevStepBtn != null) {
            prevStepBtn.setVisible(step == 2 && !editFormationOnly && !editModuleOnly);
            prevStepBtn.setManaged(step == 2 && !editFormationOnly && !editModuleOnly);
        }
        if (mainButton != null) {
            boolean showSave = (step == 2 && !editFormationOnly) || (step == 1 && editFormationOnly) || editModuleOnly;
            mainButton.setVisible(showSave);
            mainButton.setManaged(showSave);
            if (isEditMode)
                mainButton.setText("Mettre à jour");
        }
    }

    @FXML
    private void closePopup() {
        if (titreField != null && titreField.getScene() != null) {
            ((Stage) titreField.getScene().getWindow()).close();
        }
    }

    @FXML
    private void supprimerModule() {
        Module selected = modulesListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getId() > 0) {
                try {
                    moduleServiceF.delete(selected.getId());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            modulesToAdd.remove(selected);
            modulesListView.getItems().setAll(modulesToAdd);
        }
    }

    @FXML
    public void openAjouterFormation() {
        this.isEditMode = false;
        this.existingFormation = null;
        this.modulesToAdd.clear();
        showFormationPopup(1, null, false, false);
    }

    public void openEditFormation(Formation f) {
        this.isEditMode = true;
        this.existingFormation = f;
        showFormationPopup(1, null, true, false);
    }

    private void openEditModule(Formation f, Module m) {
        this.isEditMode = true;
        this.existingFormation = f;
        showFormationPopup(2, m, false, true);
    }

    private void showFormationPopup(int startStep, Module targetModule, boolean formOnly, boolean modOnly) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AjouterFormation.fxml"));
            Parent root = loader.load();
            FormationController ctrl = loader.getController();

            ctrl.isEditMode = this.isEditMode; // Pass the current edit mode
            ctrl.editFormationOnly = formOnly;
            ctrl.editModuleOnly = modOnly;

            if (this.isEditMode && existingFormation != null) { // Use this.isEditMode
                ctrl.existingFormation = existingFormation;
                ctrl.titreField.setText(existingFormation.getTitre());
                ctrl.descriptionArea.setText(existingFormation.getDescription());
                ctrl.dureeField.setText(existingFormation.getDuree());
                ctrl.niveauCombo.setValue(existingFormation.getNiveau());
                ctrl.categorieFormationCombo.setValue(existingFormation.getCategorie());
                ctrl.selectedImagePath = existingFormation.getImagePath();
                ctrl.setImageSafe(ctrl.imagePreview, ctrl.selectedImagePath);

                // Charger les modules
                ctrl.modulesToAdd = moduleServiceF.findByFormationId(existingFormation.getId());
                ctrl.modulesListView.getItems().setAll(ctrl.modulesToAdd);

                if (targetModule != null) {
                    // Trouver le module correspondant dans la liste chargée
                    Module toEdit = ctrl.modulesToAdd.stream()
                            .filter(mod -> mod.getId() == targetModule.getId())
                            .findFirst()
                            .orElse(targetModule);
                    ctrl.loadModuleToEdit(toEdit);
                    // Masquer la prévisualisation pour se concentrer sur l'édition du module
                    if (ctrl.modulesPreviewContainer != null) {
                        ctrl.modulesPreviewContainer.setVisible(false);
                        ctrl.modulesPreviewContainer.setManaged(false);
                    }
                }
            }
            ctrl.goToStep(startStep);

            Stage stage = new Stage();
            stage.setTitle(this.isEditMode ? "Modifier Formation" : "Ajouter une Formation"); // Use this.isEditMode
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
            loadAllFormations();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir l'interface : " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveAction() {
        if (titreField.getText().isEmpty()) {
            showAlert("Erreur", "Le titre est obligatoire.");
            return;
        }

        try {
            Formation f = isEditMode ? existingFormation : new Formation();
            f.setTitre(titreField.getText());
            f.setDescription(descriptionArea.getText());
            f.setDuree(dureeField.getText());
            f.setNiveau(niveauCombo.getValue());
            f.setCategorie(categorieFormationCombo.getValue());
            f.setImagePath(selectedImagePath);
            // Ensure the current module being edited is included if it has a title
            if (moduleEnCours != null && moduleTitreField != null && !moduleTitreField.getText().isEmpty()) {
                moduleEnCours.setTitre(moduleTitreField.getText());
                moduleEnCours.setDescription(moduleDescriptionArea.getText());
                if (!modulesToAdd.contains(moduleEnCours)) {
                    modulesToAdd.add(moduleEnCours);
                }
            }

            int formationId;
            if (isEditMode) {
                formationService.update(f);
                formationId = f.getId();
            } else {
                f.setIdCreateur(currentUserId);
                formationId = formationService.create(f);
            }

            // Upsert modules and their contents
            for (Module m : modulesToAdd) {
                m.setFormationId(formationId);
                int moduleId;
                if (m.getId() == 0) {
                    moduleId = moduleServiceF.create(m);
                } else {
                    moduleServiceF.update(m);
                    moduleId = m.getId();
                }

                // Save contents of each module
                List<Contenu> contents = m.getContenus();
                if (contents != null) {
                    for (Contenu c : contents) {
                        c.setModuleId(moduleId);
                        if (c.getId() == 0) {
                            contenuServiceF.create(c);
                        } else {
                            contenuServiceF.update(c);
                        }
                    }
                }
            }

            if (titreField.getScene() != null && titreField.getScene().getWindow() != null) {
                ((Stage) titreField.getScene().getWindow()).close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    private void syncGoogleCalendar() {
        // Test de la connexion SMTP (JavaMail) — remplace l'authentification OAuth
        // Google
        Thread t = new Thread(() -> {
            try {
                GoogleCalendarServiceF calendarService = new GoogleCalendarServiceF();
                boolean ok = calendarService.testEmailConnection();
                javafx.application.Platform.runLater(() -> {
                    if (ok) {
                        showAlert("Succès",
                                "✅ Connexion email (SMTP) opérationnelle !\nLes invitations seront envoyées par email lors de la création d'une séance.");
                    } else {
                        showAlert("Erreur",
                                "❌ Connexion SMTP échouée.\nVérifiez l'adresse email et le mot de passe d'application dans EmailService.java");
                    }
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> showAlert("Erreur", "❌ Erreur : " + ex.getMessage()));
                ex.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void showAddSeanceDialog(Formation f) {
        Dialog<models.SeanceGroupe> dialog = new Dialog<>();
        dialog.setTitle("MindCare - Planification");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");

        VBox container = new VBox(0);
        container.setPrefWidth(450);

        // Form
        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(15);
        form.setPadding(new Insets(25));

        TextField titreField = new TextField();
        titreField.setPromptText("Ex: Discussion sur le module 1");
        titreField.setStyle(
                "-fx-background-radius: 10; -fx-padding: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10;");

        DatePicker datePick = new DatePicker(java.time.LocalDate.now());
        datePick.setMaxWidth(Double.MAX_VALUE);
        datePick.setStyle("-fx-background-radius: 10;");

        TextField heureField = new TextField("14:00");
        heureField.setStyle(
                "-fx-background-radius: 10; -fx-padding: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10;");

        Spinner<Integer> dureeSpin = new Spinner<>(15, 240, 90, 15);
        dureeSpin.setMaxWidth(Double.MAX_VALUE);
        dureeSpin.setStyle("-fx-background-radius: 10;");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Ordre du jour, points à aborder...");
        descArea.setPrefRowCount(3);
        descArea.setStyle(
                "-fx-background-radius: 10; -fx-padding: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 10;");

        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #475569; -fx-font-size: 13px;";
        Label l1 = new Label("Titre de la session");
        l1.setStyle(labelStyle);
        Label l2 = new Label("Date");
        l2.setStyle(labelStyle);
        Label l3 = new Label("Heure (HH:mm)");
        l3.setStyle(labelStyle);
        Label l4 = new Label("Durée (minutes)");
        l4.setStyle(labelStyle);
        Label l5 = new Label("Description");
        l5.setStyle(labelStyle);

        form.add(l1, 0, 0);
        form.add(titreField, 0, 1, 2, 1);
        form.add(l2, 0, 2);
        form.add(datePick, 0, 3);
        form.add(l3, 1, 2);
        form.add(heureField, 1, 3);
        form.add(l4, 0, 4, 2, 1);
        form.add(dureeSpin, 0, 5, 2, 1);
        form.add(l5, 0, 6, 2, 1);
        form.add(descArea, 0, 7, 2, 1);

        container.getChildren().addAll(form);
        dialogPane.setContent(container);

        ButtonType saveBtn = new ButtonType("Planifier la séance", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Style buttons
        Button ok = (Button) dialogPane.lookupButton(saveBtn);
        ok.setStyle(
                "-fx-background-color: #5C98A8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;");
        Button cancel = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancel.setStyle(
                "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;");

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                models.SeanceGroupe seance = new models.SeanceGroupe();
                seance.setTitre(titreField.getText());
                seance.setIdFormation(f.getId());
                seance.setIdUsers(currentUserId);
                seance.setDescription(descArea.getText());
                seance.setDureeMinutes(dureeSpin.getValue());
                try {
                    LocalDateTime dt = LocalDateTime.of(datePick.getValue(),
                            java.time.LocalTime.parse(heureField.getText()));
                    if (dt.isBefore(LocalDateTime.now())) {
                        showAlert("Erreur", "La date de la séance ne peut pas être dans le passé.");
                        return null;
                    }
                    seance.setDateHeure(dt);
                    return seance;
                } catch (Exception ex) {
                    showAlert("Erreur", "Format d'heure invalide (HH:mm)");
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(s -> {
            try {
                s.genererLienJitsi();
                seanceService.create(s);
                showAlert("Succès", "La séance '" + s.getTitre() + "' a été planifiée !");
                updateNotificationContent();

                Thread t = new Thread(() -> {
                    try {
                        GoogleCalendarServiceF calendarService = new GoogleCalendarServiceF();
                        calendarService.addSeanceToCalendar(s, f.getId(), currentUserEmail, currentUserFullName);
                        // ✅ Sauvegarder uniquement le google_event_id en DB (requête ciblée)
                        if (s.getGoogleEventId() != null && !s.getGoogleEventId().isEmpty()) {
                            seanceService.updateGoogleEventId(s.getSeanceId(), s.getGoogleEventId());
                        } else {
                            System.err.println("⚠️ Google Calendar n'a pas retourné d'Event ID pour la séance : "
                                    + s.getSeanceId());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                t.setDaemon(true);
                t.start();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void setImageSafe(ImageView iv, String path) {
        try {
            if (path != null && !path.isEmpty()) {
                File f = resolveFilePath(path);
                if (f != null && f.exists()) {
                    iv.setImage(new Image(f.toURI().toString()));
                    return;
                }
            }

            // Default image placeholder
            java.net.URL placeholderUrl = getClass().getResource("/images/psychologie.jpg");
            if (placeholderUrl == null) {
                // Try fallback to psycho.jpg in uploads
                placeholderUrl = getClass().getResource("/uploads/images/psycho.jpg");
            }

            if (placeholderUrl != null) {
                iv.setImage(new Image(placeholderUrl.toExternalForm()));
            } else {
                iv.setImage(null); // Or a generic placeholder if needed
            }
        } catch (Exception e) {
            iv.setImage(null);
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    private void rechercherVideosYouTube() {
        String query = "";
        if (youtubeSearchField != null && !youtubeSearchField.getText().trim().isEmpty()) {
            query = youtubeSearchField.getText().trim();
        } else if (moduleTitreField != null && !moduleTitreField.getText().trim().isEmpty()) {
            query = moduleTitreField.getText().trim();
        }

        if (query.isEmpty()) {
            showAlert("Erreur",
                    "Entrez un terme de recherche ou un titre de module d'abord pour lancer la recherche YouTube.");
            return;
        }

        final String finalQuery = query;

        new Thread(() -> {
            try {
                com.google.gson.JsonArray items = YouTubeServiceF.search(finalQuery);
                Platform.runLater(() -> mostrarResultadosYouTube(items));
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert("Problème API",
                        "Impossible de se connecter au service vidéo. Vous pouvez saisir une URL YouTube manuellement. "
                                + ex.getMessage()));
            }
        }).start();
    }

    private void mostrarResultadosYouTube(com.google.gson.JsonArray items) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Méditation & Relaxation - Vidéos Suggérées");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(600);

        java.net.URL cssUrl = getClass().getResource("/mindcare.css");
        if (cssUrl != null) {
            dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }

        VBox box = new VBox(20);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: white;");

        for (int i = 0; i < items.size(); i++) {
            com.google.gson.JsonObject item = items.get(i).getAsJsonObject();
            String title = item.has("title") ? item.get("title").getAsString() : "Sans titre";
            String description = item.has("description") ? item.get("description").getAsString() : "";
            String thumbnail = item.has("thumbnail") ? item.get("thumbnail").getAsString() : "";
            String videoId = item.has("videoId") ? item.get("videoId").getAsString() : "";
            String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

            HBox videoCard = new HBox(15);
            videoCard.setStyle(
                    "-fx-background-color: #F8FAFC; -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: #E2E8F0; -fx-border-radius: 12;");
            videoCard.setAlignment(Pos.CENTER_LEFT);

            // Thumbnail
            ImageView thumbView = new ImageView();
            thumbView.setFitWidth(120);
            thumbView.setFitHeight(90);
            thumbView.setPreserveRatio(true);
            if (!thumbnail.isEmpty()) {
                thumbView.setImage(new Image(thumbnail));
            }

            VBox infoBox = new VBox(5);
            infoBox.setPrefWidth(300);
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1F2A33;");
            titleLabel.setWrapText(true);

            Label descLabel = new Label(description);
            descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
            descLabel.setWrapText(true);
            descLabel.setMaxHeight(40);

            HBox videoActions = new HBox(10);

            Button previewBtn = new Button("👁 Visionner");
            previewBtn.setStyle(
                    "-fx-background-color: #E2E8F0; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 11px;");
            previewBtn.setOnAction(e -> openVideoPreview(videoUrl, title));

            Button addBtn = new Button("➕ Ajouter");
            addBtn.setStyle(
                    "-fx-background-color: #5C98A8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 11px;");
            addBtn.setOnAction(e -> {
                Contenu c = new Contenu();
                c.setType("VIDEO");
                c.setChemin(videoUrl);
                if (moduleEnCours == null)
                    moduleEnCours = new Module();
                moduleEnCours.ajouterContenu(c);
                contenusListView.getItems().add("VIDEO (YouTube) - " + title);
                dialog.close();
            });

            videoActions.getChildren().addAll(previewBtn, addBtn);
            infoBox.getChildren().addAll(titleLabel, descLabel, videoActions);

            videoCard.getChildren().addAll(thumbView, infoBox);
            box.getChildren().add(videoCard);
        }

        if (items.size() == 0) {
            box.getChildren().add(new Label("Aucune vidéo trouvée pour ce terme. Vérifiez votre clé API."));
        }

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setPrefHeight(500);
        sp.setStyle("-fx-background-color: transparent; -fx-background: white;");
        dialog.getDialogPane().setContent(sp);
        dialog.showAndWait();
    }

    private VBox createPdfViewer(File pdfFile) {
        VBox viewer = new VBox(6);
        viewer.setAlignment(Pos.CENTER);
        viewer.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE); // Le fond noir s'ajuste à la taille du contenu
        viewer.setStyle(
                "-fx-background-color: #525659; -fx-padding: 6; -fx-background-radius: 8;"); // Padding réduit

        final int[] currentPage = { 0 };
        final double[] zoom = { 1.5 };
        final double[] currentWidth = { 650.0 }; // Taille initiale plus grande

        ImageView pageView = new ImageView();
        pageView.setPreserveRatio(true);
        pageView.setFitWidth(currentWidth[0]);

        Label fileLabel = new Label("📄 " + pdfFile.getName());
        fileLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        Button toggleBtn = new Button("▼");
        toggleBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(toggleBtn, fileLabel);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: #323639; -fx-padding: 10; -fx-background-radius: 8;");

        // Cacher par défaut
        controls.setVisible(false);
        controls.setManaged(false);
        pageView.setVisible(false);
        pageView.setManaged(false);

        toggleBtn.setOnAction(e -> {
            boolean visible = !pageView.isVisible();
            pageView.setVisible(visible);
            pageView.setManaged(visible);
            controls.setVisible(visible);
            controls.setManaged(visible);
            toggleBtn.setText(visible ? "▲" : "▼");
        });

        Button prevBtn = new Button("◀ Précédent");
        Button nextBtn = new Button("Suivant ▶");
        Button zoomInBtn = new Button("🔍+");
        Button zoomOutBtn = new Button("🔍−");
        Label pageLabel = new Label("Chargement...");

        prevBtn.setStyle(
                "-fx-background-color: #5C98A8; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold;");
        nextBtn.setStyle(
                "-fx-background-color: #5C98A8; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold;");
        zoomInBtn.setStyle("-fx-background-color: #4B5563; -fx-text-fill: white; -fx-background-radius: 8;");
        zoomOutBtn.setStyle("-fx-background-color: #4B5563; -fx-text-fill: white; -fx-background-radius: 8;");
        pageLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        controls.getChildren().addAll(prevBtn, pageLabel, nextBtn, zoomInBtn, zoomOutBtn);

        try {
            org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfFile);
            int totalPages = document.getNumberOfPages();

            Runnable renderPage = () -> {
                try {
                    org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(
                            document);
                    java.awt.image.BufferedImage buffered = renderer.renderImageWithDPI(currentPage[0],
                            (float) (zoom[0] * 72));

                    // ✅ Conversion sans javafx.swing
                    javafx.scene.image.Image fxImage = convertToFxImage(buffered);

                    Platform.runLater(() -> {
                        pageView.setImage(fxImage);
                        pageLabel.setText("Page " + (currentPage[0] + 1) + " / " + totalPages);
                        prevBtn.setDisable(currentPage[0] == 0);
                        nextBtn.setDisable(currentPage[0] == totalPages - 1);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> pageLabel.setText("❌ Erreur page " + (currentPage[0] + 1)));
                    e.printStackTrace();
                }
            };

            new Thread(renderPage).start();

            prevBtn.setOnAction(e -> {
                if (currentPage[0] > 0) {
                    currentPage[0]--;
                    new Thread(renderPage).start();
                }
            });
            nextBtn.setOnAction(e -> {
                if (currentPage[0] < totalPages - 1) {
                    currentPage[0]++;
                    new Thread(renderPage).start();
                }
            });
            zoomInBtn.setOnAction(e -> {
                zoom[0] = Math.min(zoom[0] + 0.3, 3.0);
                currentWidth[0] = 650.0 * (zoom[0] / 1.5);
                pageView.setFitWidth(currentWidth[0]);
                new Thread(renderPage).start();
            });
            zoomOutBtn.setOnAction(e -> {
                zoom[0] = Math.max(zoom[0] - 0.3, 0.5);
                currentWidth[0] = 650.0 * (zoom[0] / 1.5);
                pageView.setFitWidth(currentWidth[0]);
                new Thread(renderPage).start();
            });

        } catch (Exception e) {
            Label err = new Label("❌ Erreur : " + e.getMessage());
            err.setStyle("-fx-text-fill: red;");
            viewer.getChildren().add(err);
            return viewer;
        }

        viewer.getChildren().addAll(header, controls, pageView);
        return viewer;
    }

    // ✅ Méthode de conversion BufferedImage → JavaFX Image
    private javafx.scene.image.Image convertToFxImage(
            java.awt.image.BufferedImage buffered) {
        java.awt.image.BufferedImage argb = new java.awt.image.BufferedImage(
                buffered.getWidth(), buffered.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = argb.createGraphics();
        g.drawImage(buffered, 0, 0, null);
        g.dispose();
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(argb, "png", baos);
            return new javafx.scene.image.Image(
                    new java.io.ByteArrayInputStream(baos.toByteArray()));
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // === UTILS WEBVIEW VIDÉO ===

    private String extractYouTubeId(String path) {
        String videoId = "";
        if (path.contains("v=")) {
            int vPos = path.indexOf("v=");
            videoId = path.substring(vPos + 2);
            int ampPos = videoId.indexOf("&");
            if (ampPos != -1)
                videoId = videoId.substring(0, ampPos);
        } else if (path.contains("youtu.be/")) {
            int slashPos = path.lastIndexOf("/");
            videoId = path.substring(slashPos + 1);
            int qPos = videoId.indexOf("?");
            if (qPos != -1)
                videoId = videoId.substring(0, qPos);
        }
        return videoId;
    }

    private void openVideoPreview(String url, String title) {
        try {
            String finalUrl = url;
            if (url.contains("youtube.com") || url.contains("youtu.be")) {
                // Ouvrir simplement la page YouTube dans le navigateur par défaut
                finalUrl = url;
            }

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(finalUrl));
                    return;
                }
            }

            // Fallback si Desktop non supporté
            showAlert("Prévisualisation vidéo",
                    "Ouvrez cette URL dans votre navigateur :\n" + finalUrl);
        } catch (Exception e) {
            showAlert("Erreur prévisualisation vidéo",
                    "Impossible d'ouvrir la vidéo dans votre navigateur.\n" + e.getMessage());
        }
    }

    // ── Navigation IA & Word ──
    @FXML
    private void showAIInterview() {
        // Seuls les psychologues et admins peuvent accéder à l'assistant IA
        boolean canUseAI = isStaff();
        if (!canUseAI) {
            showAlert("Accès refusé", "Cette fonctionnalité est réservée aux psychologues et administrateurs.");
            return;
        }

        if (selectedFormationForLearning == null) {
            // Redirige vers la liste des formations pour que l'utilisateur en choisisse une
            showAlert("Sélectionnez une formation",
                    "Veuillez d'abord cliquer sur une de vos formations, puis utiliser le bouton IA 🤖 directement depuis la carte ou la liste des modules.");
            showAllFormations();
            return;
        }

        launchAIInterviewForFormation(selectedFormationForLearning);
    }

    /** Lance l'assistant IA directement pour une formation donnée (sans popup) */
    public void launchAIInterviewForFormation(Formation f) {
        this.selectedFormationForLearning = f;

        // Reinitialiser le controller IA à chaque nouvelle session pour cette formation
        aiController = new AIInterviewController();
        aiController.setParentController(this);
        aiController.initAvecVBox(
                aiChatContainer, aiChatScrollPane,
                aiMessageField, aiSendBtn,
                btnGenererCours, aiLoading, aiStatutLabel);
        aiController.setIdFormationSelectionnee(f.getId());

        switchView(aiInterviewView);
    }

    @FXML
    private void backFromAIInterview() {
        switchView(formationDetailsView);
    }

    @FXML
    private void backFromWordViewer() {
        hideAllViews();
        moduleDetailsView.setVisible(true);
        moduleDetailsView.setManaged(true);
    }

    @FXML
    private void handleExportWord() {
        if (viewedModule == null)
            return;
        try {
            String path = GeneratorServiceF.ecrireWord(viewedModule.getTitre(), viewedModule.getDescription());
            this.currentWordPath = path;

            // Open the file with the default system application
            File file = new File(path);
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file);
            }
            showAlert("Word",
                    "✅ Le fichier Word a été généré et ouvert.\nModifiez-le, enregistrez-le, puis cliquez sur 'Synchroniser'.");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "❌ Impossible de générer le fichier Word.");
        }
    }

    @FXML
    private void handleSyncWord() {
        if (viewedModule == null || currentWordPath == null) {
            showAlert("Info", "Générez d'abord le fichier Word.");
            return;
        }

        try {
            String updatedContent = GeneratorServiceF.lireWord(currentWordPath);
            if (updatedContent != null && !updatedContent.isEmpty()) {
                viewedModule.setDescription(updatedContent);
                moduleServiceF.update(viewedModule);

                // Refresh UI
                contentsLearningContainer.getChildren().clear();
                renderFormattedDescription(updatedContent, contentsLearningContainer);

                showAlert("Succès", "✅ Contenu synchronisé depuis le fichier Word !");
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            showAlert("Erreur",
                    "❌ Impossible de lire le fichier Word. Vérifiez qu'il n'est pas ouvert dans une autre application.");
        }
    }

    public void ouvrirWordViewer(String chemin, String titre, int idPatient, int idContenu) {
        hideAllViews();
        wordViewerView.setVisible(true);
        wordViewerView.setManaged(true);
        wordViewerTitre.setText(titre);
        this.currentWordPath = chemin;

        boolean isPsy = isPsychologue();

        wordViewer = new WordViewerPatient();
        wordViewer.initAvecVBox(
                wordContentContainer, wordScrollPane,
                wordExpliquerBtn, wordExplicationPanel,
                wordMotLabel, wordExplicationLabel,
                wordTraductionLabel, wordExempleLabel,
                wordExplicationLoading,
                wordInputField, // <-- Nouveau champ de recherche passé !
                idPatient, idContenu);

        // La même logique pour Psy et Patient : rechercher une définition
        wordViewer.setModeEdition(false);
        wordExpliquerBtn.setDisable(false); // Le bouton est toujours dispo
        wordExpliquerBtn.setOnAction(e -> onExpliquerMot());

        wordViewer.charger(chemin);
    }

    private void onSaveWord() {
        if (wordViewer != null && currentWordPath != null) {
            wordViewer.sauvegarder(currentWordPath);
            showAlert("Succès", "✅ Modifications enregistrées dans le fichier Word.");
        }
    }

    @FXML
    private void onAIEnvoyer() {
        if (aiController != null)
            aiController.onEnvoyer();
    }

    @FXML
    private void onGenererCours() {
        if (aiController != null)
            aiController.onGenererCours();
    }

    @FXML
    private void onExpliquerMot() {
        if (wordViewer != null)
            wordViewer.onExpliquerIA();
    }

    @FXML
    private void fermerExplication() {
        if (wordViewer != null)
            wordViewer.onFermerExplication();
    }

    // Helper — cacher toutes les vues
    private void hideAllViews() {
        if (allFormationsView != null) {
            allFormationsView.setVisible(false);
            allFormationsView.setManaged(false);
        }
        if (myFormationsView != null) {
            myFormationsView.setVisible(false);
            myFormationsView.setManaged(false);
        }
        if (demandesView != null) {
            demandesView.setVisible(false);
            demandesView.setManaged(false);
        }
        if (sessionsView != null) {
            sessionsView.setVisible(false);
            sessionsView.setManaged(false);
        }
        if (inscritsView != null) {
            inscritsView.setVisible(false);
            inscritsView.setManaged(false);
        }
        if (formationDetailsView != null) {
            formationDetailsView.setVisible(false);
            formationDetailsView.setManaged(false);
        }
        if (moduleDetailsView != null) {
            moduleDetailsView.setVisible(false);
            moduleDetailsView.setManaged(false);
        }
        if (aiInterviewView != null) {
            aiInterviewView.setVisible(false);
            aiInterviewView.setManaged(false);
        }
        if (wordViewerView != null) {
            wordViewerView.setVisible(false);
            wordViewerView.setManaged(false);
        }
    }

    // Refactored from addEditorControls
    private void openEditorDialog(Module m) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Éditeur de contenu (Psychologue)");
        dialog.setHeaderText("Modifiez le contenu rédactionnel du programme :");

        ButtonType saveButtonType = new ButtonType("💾 Sauvegarder les modifications", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextArea editDesc = new TextArea(m.getDescription());
        editDesc.setWrapText(true);
        editDesc.setPrefRowCount(15);
        editDesc.setPrefColumnCount(65);
        editDesc.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #DDECEF;");

        dialog.getDialogPane().setContent(editDesc);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        if (saveBtn != null) {
            saveBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #3A6B7E, #5C98A8); -fx-text-fill: white; " +
                            "-fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;");
        }

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return editDesc.getText();
            }
            return null;
        });

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(newText -> {
            try {
                m.setDescription(newText);
                moduleServiceF.update(m);
                // Assuming openModuleContent exists and takes a Module object
                // This call will refresh the view with the updated content
                // If openModuleContent is not in the original file, this line might need
                // adjustment
                // based on how the module content is displayed.
                // For now, we assume it exists and refreshes the current module view.
                openModuleContent(m); // Full refresh
                showAlert("Succès", "Contenu thérapeutique mis à jour !");
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Erreur lors de la sauvegarde : " + ex.getMessage());
            }
        });
    }

    private void renderFormattedDescription(String content, VBox container) {
        if (content == null || content.isEmpty())
            return;

        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            Label lbl = new Label();
            lbl.setWrapText(true);
            lbl.setMaxWidth(850);

            if (line.contains("_TITRE:")) {
                String cleanTitre = line.split("_TITRE:")[1].trim();
                lbl.setText(cleanTitre);
                lbl.setStyle(
                        "-fx-font-size: 19px; -fx-font-weight: 900; -fx-text-fill: #1E3A8A; -fx-padding: 20 0 8 0;");
            } else if (line.contains("_CONTENU:")) {
                String cleanContenu = line.split("_CONTENU:")[1].trim();
                lbl.setText(cleanContenu);
                lbl.setStyle(
                        "-fx-font-size: 15px; -fx-text-fill: #334155; -fx-line-spacing: 1.5; -fx-padding: 0 0 10 0;");
            } else if (line.matches("^\\d+\\..*")) {
                lbl.setText(line);
                lbl.setStyle("-fx-font-size: 15px; -fx-text-fill: #475569; -fx-padding: 5 0 5 25;");
            } else {
                lbl.setText(line);
                lbl.setStyle("-fx-font-size: 15px; -fx-text-fill: #1F2A33; -fx-padding: 2 0;");
            }
            container.getChildren().add(lbl);
        }
    }
}