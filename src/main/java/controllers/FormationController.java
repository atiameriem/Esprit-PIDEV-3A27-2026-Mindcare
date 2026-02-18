package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import models.Formation;
import models.Module;
import models.Contenu;
import models.Participation;
import services.FormationService;
import services.ModuleService;
import services.ContenuService;
import services.ParticipationService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class FormationController {

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
    private Button allFormationsBtn;
    @FXML
    private Button myFormationsBtn;

    // === FILTRES & RECHERCHE ===
    @FXML
    private HBox filterBar;
    @FXML
    private Pane myFilterBar;
    @FXML
    private TextField searchField;
    @FXML
    private TextField myParticipationsSearchField;
    @FXML
    private ComboBox<String> categoryCombo;

    // === ÉLÉMENTS D'APPRENTISSAGE ===
    @FXML
    private VBox formationDetailsView;
    @FXML
    private VBox moduleDetailsView;
    @FXML
    private Label detailFormationTitle;
    @FXML
    private VBox modulesLearningList;
    @FXML
    private Label selectedModuleTitle;
    @FXML
    private Label selectedModuleDesc;
    @FXML
    private VBox contentsLearningContainer;

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
    private TextArea moduleDescriptionArea;
    @FXML
    private ListView<String> contenusListView;
    @FXML
    private ListView<Module> modulesListView;

    // === SERVICES ET ÉTAT ===
    private final FormationService formationService = new FormationService();
    private final ModuleService moduleService = new ModuleService();
    private final ContenuService contenuService = new ContenuService();
    private final ParticipationService participationService = new ParticipationService();
    private int currentUserId = 1;

    private String selectedImagePath;
    private Formation existingFormation;
    private boolean isEditMode = false;
    private Module moduleEnCours;
    private List<Module> modulesToAdd = new ArrayList<>();
    private List<Participation> userParticipations = new ArrayList<>();

    @FXML
    public void initialize() {
        if (niveauCombo != null) {
            niveauCombo.getItems().setAll("Débutant", "Intermédiaire", "Avancé");
        }

        if (formationsFlowPane != null) {
            loadAllFormations();
        }

        if (categoryCombo != null) {
            categoryCombo.getItems().setAll("Toutes", "Développement", "Bien-être", "Santé");
            categoryCombo.getSelectionModel().selectFirst();
        }

        if (myParticipationsSearchField != null) {
            myParticipationsSearchField.textProperty().addListener((obs, oldV, newV) -> filterMyParticipations(newV));
        }

        if (modulesListView != null) {
            modulesListView.setCellFactory(lv -> new ListCell<Module>() {
                @Override
                protected void updateItem(Module item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null)
                        setText(null);
                    else
                        setText(item.getTitre() + (item.getId() > 0 ? " (Existant)" : " (Nouveau)"));
                }
            });
            modulesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null)
                    loadModuleToEdit(newVal);
            });
        }
    }

    private void filterMyParticipations(String query) {
        if (participationsFlowPane == null)
            return;
        participationsFlowPane.getChildren().clear();
        String low = query.toLowerCase().trim();
        List<Participation> filtered = userParticipations.stream()
                .filter(p -> p.getTitreFormation().toLowerCase().contains(low))
                .collect(Collectors.toList());
        if (filtered.isEmpty())
            noParticipationsBox.setVisible(true);
        else {
            noParticipationsBox.setVisible(false);
            for (Participation p : filtered)
                participationsFlowPane.getChildren().add(createParticipationCard(p));
        }
    }

    // === NAVIGATION ===

    private void switchView(Pane target) {
        if (allFormationsView != null)
            allFormationsView.setVisible(target == allFormationsView);
        if (myFormationsView != null)
            myFormationsView.setVisible(target == myFilterBar || target == myFormationsView);
        if (formationDetailsView != null)
            formationDetailsView.setVisible(target == formationDetailsView);
        if (moduleDetailsView != null)
            moduleDetailsView.setVisible(target == moduleDetailsView);

        if (filterBar != null) {
            filterBar.setVisible(target == allFormationsView);
            filterBar.setManaged(target == allFormationsView);
        }
        if (myFilterBar != null) {
            myFilterBar.setVisible(target == myFormationsView);
            myFilterBar.setManaged(target == myFormationsView);
        }
    }

    @FXML
    private void showAllFormations() {
        switchView(allFormationsView);
        if (allFormationsBtn != null) {
            allFormationsBtn.setStyle(
                    "-fx-background-color: #5C98A8; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold;");
            myFormationsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: #1F2A33; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold; -fx-border-color: #DDD; -fx-border-radius: 20;");
        }
        loadAllFormations();
    }

    @FXML
    private void showMyFormations() {
        switchView(myFormationsView);
        if (myFormationsBtn != null) {
            myFormationsBtn.setStyle(
                    "-fx-background-color: #5C98A8; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold;");
            allFormationsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: #1F2A33; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold; -fx-border-color: #DDD; -fx-border-radius: 20;");
        }
        loadMyParticipations();
    }

    private void openModulesView(Participation p) {
        switchView(formationDetailsView);
        detailFormationTitle.setText(p.getTitreFormation());
        modulesLearningList.getChildren().clear();
        try {
            List<Module> modules = moduleService.findByFormationId(p.getIdFormation());
            for (Module m : modules) {
                HBox item = new HBox(15);
                item.setAlignment(Pos.CENTER_LEFT);
                item.setStyle(
                        "-fx-background-color: #F8FBFC; -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: #DDECEF; -fx-border-radius: 12; -fx-cursor: hand;");
                item.setOnMouseClicked(e -> openModuleContent(m));
                VBox text = new VBox(5);
                Label t = new Label(m.getTitre());
                t.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1F2A33;");
                String desc = m.getDescription();
                if (desc != null && desc.length() > 85)
                    desc = desc.substring(0, 82) + "...";
                Label d = new Label(desc);
                d.setStyle("-fx-text-fill: #5F7F8B; -fx-font-size: 13px;");
                text.getChildren().addAll(t, d);
                Pane spacer = new Pane();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label arrow = new Label("➜");
                arrow.setStyle("-fx-text-fill: #5C98A8; -fx-font-size: 18px; -fx-font-weight: bold;");
                item.getChildren().addAll(text, spacer, arrow);
                modulesLearningList.getChildren().add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openModuleContent(Module m) {
        switchView(moduleDetailsView);
        selectedModuleTitle.setText(m.getTitre());
        selectedModuleDesc.setText(m.getDescription());
        contentsLearningContainer.getChildren().clear();
        Label dTop = new Label(m.getDescription());
        dTop.setStyle("-fx-text-fill: #5F7F8B; -fx-font-size: 15px; -fx-wrap-text: true; -fx-padding: 0 0 10 0;");
        dTop.setMaxWidth(850);
        contentsLearningContainer.getChildren().add(dTop);
        try {
            List<Contenu> contenus = contenuService.findByModuleId(m.getId());
            for (Contenu c : contenus) {
                VBox card = new VBox(15);
                card.setStyle(
                        "-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2); -fx-border-color: #EEE; -fx-border-radius: 12;");
                Label n = new Label(new File(c.getChemin()).getName());
                Button b = new Button("Consulter");
                b.setOnAction(e -> {
                    try {
                        java.awt.Desktop.getDesktop().open(new File(c.getChemin()));
                    } catch (Exception ex) {
                        showAlert("Erreur", "Impossible d'ouvrir le fichier.");
                    }
                });
                card.getChildren().addAll(n, b);
                contentsLearningContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void backToModules() {
        switchView(formationDetailsView);
    }

    // === CHARGEMENT ===

    private void loadAllFormations() {
        if (formationsFlowPane == null)
            return;
        formationsFlowPane.getChildren().clear();
        try {
            userParticipations = participationService.findByUserId(currentUserId);
            List<Formation> list = formationService.read();
            for (Formation f : list)
                formationsFlowPane.getChildren().add(createFormationCard(f));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMyParticipations() {
        if (participationsFlowPane == null)
            return;
        participationsFlowPane.getChildren().clear();
        try {
            userParticipations = participationService.findByUserId(currentUserId);
            if (userParticipations.isEmpty())
                noParticipationsBox.setVisible(true);
            else {
                noParticipationsBox.setVisible(false);
                for (Participation p : userParticipations)
                    participationsFlowPane.getChildren().add(createParticipationCard(p));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean hasJoined(int fId) {
        return userParticipations.stream().anyMatch(p -> p.getIdFormation() == fId);
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
        HBox adminBtns = new HBox(5);
        adminBtns.setAlignment(Pos.TOP_RIGHT);
        adminBtns.setPadding(new Insets(8));

        Button editBtn = new Button("✎");
        editBtn.setStyle(
                "-fx-background-color: rgba(249, 250, 251, 0.9); -fx-text-fill: #4B5563; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 5 8; -fx-border-color: #E5E7EB; -fx-border-radius: 8;");
        editBtn.setOnAction(e -> openEditFormation(f));

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle(
                "-fx-background-color: rgba(254, 242, 242, 0.9); -fx-text-fill: #EF4444; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 5 8; -fx-border-color: #FECACA; -fx-border-radius: 8;");
        deleteBtn.setOnAction(e -> handleDeleteFormation(f));

        adminBtns.getChildren().addAll(editBtn, deleteBtn);
        imgCont.getChildren().addAll(iv, adminBtns);

        Label t = new Label(f.getTitre());
        t.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1F2A33;");
        t.setWrapText(true);

        HBox info = new HBox(10);
        info.setAlignment(Pos.CENTER_LEFT);
        Label exp = new Label("👤 Expert");
        exp.setStyle("-fx-text-fill: #6E8E9A; -fx-font-size: 12px;");
        Label dur = new Label("⏱ " + f.getDuree());
        dur.setStyle("-fx-text-fill: #6E8E9A; -fx-font-size: 12px;");
        info.getChildren().addAll(exp, dur);

        HBox bottom = new HBox(10);
        bottom.setAlignment(Pos.CENTER_LEFT);

        if (hasJoined(f.getId())) {
            Label jl = new Label("Déjà rejoint ✅");
            jl.setStyle(
                    "-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-color: #ECFDF5; -fx-background-radius: 10; -fx-cursor: hand;");
            jl.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    Participation p = userParticipations.stream().filter(pr -> pr.getIdFormation() == f.getId())
                            .findFirst().orElse(null);
                    if (p != null) {
                        try {
                            participationService.delete(p.getIdParticipation());
                            loadAllFormations();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
            bottom.getChildren().add(jl);
        } else {
            Button join = new Button("Rejoindre");
            join.setStyle(
                    "-fx-background-color: linear-gradient(to right, #5C98A8, #7FB9C7); -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 20;");
            join.setOnAction(e -> handleRegistration(f));
            bottom.getChildren().add(join);
        }

        Button det = new Button("Détails");
        det.setStyle(
                "-fx-background-color: #F0F9FF; -fx-text-fill: #0284C7; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 15; -fx-border-color: #BAE6FD; -fx-border-radius: 10;");
        det.setOnAction(e -> showFormationDetailsDialog(f));

        bottom.getChildren().add(det);
        card.getChildren().addAll(imgCont, t, info, bottom);
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
        VBox content = new VBox(15);
        content.setPrefWidth(500);
        content.setStyle("-fx-padding: 25; -fx-background-color: white; -fx-background-radius: 20;");

        Label title = new Label(f.getTitre());
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1F2A33;");

        Label sub = new Label(f.getNiveau() + " • " + f.getDuree());
        sub.setStyle("-fx-text-fill: #0284C7; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label dTitle = new Label("À propos");
        dTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1F2A33;");
        Label desc = new Label(f.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #4B5563; -fx-line-spacing: 3;");

        VBox mBox = new VBox(10);
        Label mTitle = new Label("Programme");
        mTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1F2A33;");
        mBox.getChildren().add(mTitle);
        try {
            List<Module> mods = moduleService.findByFormationId(f.getId());
            if (mods.isEmpty())
                mBox.getChildren().add(new Label("Aucun module répertorié."));
            else {
                for (Module m : mods) {
                    Label ml = new Label("• " + m.getTitre());
                    ml.setStyle("-fx-text-fill: #1F2A33; -fx-font-weight: 500;");
                    mBox.getChildren().add(ml);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        content.getChildren().addAll(title, sub, new Separator(), dTitle, desc, mBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private VBox createParticipationCard(Participation p) {
        VBox card = new VBox(15);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 4); -fx-cursor: hand;");
        card.setPrefWidth(240);
        card.setOnMouseClicked(e -> openModulesView(p));
        ImageView iv = new ImageView();
        iv.setFitHeight(130);
        iv.setFitWidth(210);
        iv.setPreserveRatio(true);
        setImageSafe(iv, p.getImagePath());
        Label t = new Label(p.getTitreFormation());
        t.setStyle("-fx-font-weight: bold; -fx-font-size: 17px; -fx-text-fill: #1F2A33;");
        HBox status = new HBox(8);
        status.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(4,
                p.getStatut().equals("accepté") ? Color.valueOf("#10B981") : Color.valueOf("#10B981"));
        String displayStatut = p.getStatut().equalsIgnoreCase("en attente") ? "ACTIF" : p.getStatut().toUpperCase();
        Label sTxt = new Label(displayStatut);
        sTxt.setStyle("-fx-text-fill: #5C98A8; -fx-font-size: 11px; -fx-font-weight: bold;");
        status.getChildren().addAll(dot, sTxt);
        Button start = new Button("Commencer");
        start.setMaxWidth(Double.MAX_VALUE);
        start.setStyle(
                "-fx-background-color: linear-gradient(to right, #5C98A8, #7FB9C7); -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8;");
        start.setOnAction(e -> openModulesView(p));
        Button cancel = new Button("Annuler l'inscription");
        cancel.setMaxWidth(Double.MAX_VALUE);
        cancel.setStyle(
                "-fx-background-color: #FEF2F2; -fx-text-fill: #EF4444; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8;");
        cancel.setOnAction(e -> {
            try {
                participationService.delete(p.getIdParticipation());
                loadMyParticipations();
                loadAllFormations();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        card.getChildren().addAll(iv, t, status, start, cancel);
        return card;
    }

    private void handleRegistration(Formation f) {
        Participation p = new Participation();
        p.setIdUser(currentUserId);
        p.setIdFormation(f.getId());
        p.setDateInscription(new Date());
        p.setStatut("accepté");
        try {
            participationService.create(p);
            loadAllFormations();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadModuleToEdit(Module m) {
        this.moduleEnCours = m;
        moduleTitreField.setText(m.getTitre());
        moduleDescriptionArea.setText(m.getDescription());
        contenusListView.getItems().clear();
        if (m.getId() > 0 && m.getContenus().isEmpty()) {
            try {
                m.setContenus(contenuService.findByModuleId(m.getId()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (Contenu c : m.getContenus()) {
            File f = new File(c.getChemin());
            contenusListView.getItems().add(c.getType() + " - " + f.getName());
        }
    }

    @FXML
    private void selectImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            selectedImagePath = f.getAbsolutePath();
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

    @FXML
    private void ajouterPodcast() {
        choisirFichier("PODCAST", "*.mp3", "*.wav");
    }

    private void choisirFichier(String type, String... exts) {
        if (moduleEnCours == null)
            moduleEnCours = new Module();
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(type, exts));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            Contenu c = new Contenu();
            c.setType(type);
            c.setChemin(f.getAbsolutePath());
            moduleEnCours.ajouterContenu(c);
            contenusListView.getItems().add(type + " - " + f.getName());
        }
    }

    @FXML
    private void supprimerContenu() {
        int idx = contenusListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && moduleEnCours != null) {
            Contenu c = moduleEnCours.getContenus().get(idx);
            if (c.getId() > 0) {
                try {
                    contenuService.delete(c.getId());
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
                    moduleService.delete(selected.getId());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            modulesToAdd.remove(selected);
            modulesListView.getItems().setAll(modulesToAdd);
        }
    }

    @FXML
    private void openAjouterFormation() {
        this.isEditMode = false;
        this.existingFormation = null;
        this.modulesToAdd.clear();
        showFormationPopup();
    }

    private void openEditFormation(Formation f) {
        this.isEditMode = true;
        this.existingFormation = f;
        showFormationPopup();
    }

    private void showFormationPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AjouterFormation.fxml"));
            Parent root = loader.load();
            FormationController ctrl = loader.getController();

            if (isEditMode && existingFormation != null) {
                ctrl.titreField.setText(existingFormation.getTitre());
                ctrl.descriptionArea.setText(existingFormation.getDescription());
                ctrl.dureeField.setText(existingFormation.getDuree());
                ctrl.niveauCombo.setValue(existingFormation.getNiveau());
                ctrl.selectedImagePath = existingFormation.getImagePath();
                ctrl.setImageSafe(ctrl.imagePreview, ctrl.selectedImagePath);
                ctrl.mainButton.setText("Mettre à jour la Formation");
                ctrl.isEditMode = true;
                ctrl.existingFormation = existingFormation;
                // Charger les modules
                ctrl.modulesToAdd = moduleService.findByFormationId(existingFormation.getId());
                ctrl.modulesListView.getItems().setAll(ctrl.modulesToAdd);
            }

            Stage stage = new Stage();
            stage.setTitle(isEditMode ? "Modifier Formation" : "Ajouter une Formation");
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
            loadAllFormations();
        } catch (Exception e) {
            e.printStackTrace();
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
            f.setImagePath(selectedImagePath);

            if (isEditMode) {
                formationService.update(f);
                // On met à jour les modules (logique simplifiée)
                for (Module m : modulesToAdd) {
                    if (m.getId() == 0) {
                        m.setFormationId(f.getId());
                        moduleService.create(m);
                    } else {
                        moduleService.update(m);
                    }
                }
            } else {
                int id = formationService.create(f);
                for (Module m : modulesToAdd) {
                    m.setFormationId(id);
                    moduleService.create(m);
                }
            }
            ((Stage) titreField.getScene().getWindow()).close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setImageSafe(ImageView iv, String path) {
        try {
            if (path != null && !path.isEmpty()) {
                File f = new File(path);
                if (f.exists()) {
                    iv.setImage(new Image(f.toURI().toString()));
                    return;
                }
            }
            iv.setImage(new Image(getClass().getResource("/images/psychologie.jpg").toExternalForm()));
        } catch (Exception e) {
            try {
                iv.setImage(new Image(getClass().getResource("/images/psychologie.jpg").toExternalForm()));
            } catch (Exception ex) {
                /* Image Missing */ }
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}