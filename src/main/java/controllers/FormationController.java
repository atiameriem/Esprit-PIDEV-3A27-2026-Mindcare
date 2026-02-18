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
import javafx.scene.layout.HBox;
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

public class FormationController {

    // === ÉLÉMENTS DE LA VUE PRINCIPALE (Formation.fxml) ===
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
    @FXML
    private HBox filterBar;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private ComboBox<String> categoryCombo;

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
    private int currentUserId = 1; // Simulation d'utilisateur connecté (id_users)

    private String selectedImagePath;
    private Formation existingFormation;
    private boolean isEditMode = false;
    private Module moduleEnCours;
    private List<Module> modulesToAdd = new ArrayList<>();

    @FXML
    public void initialize() {
        if (niveauCombo != null) {
            niveauCombo.getItems().setAll("Débutant", "Intermédiaire", "Avancé");
        }

        if (formationsFlowPane != null) {
            loadAllFormations();
        }

        if (sortCombo != null) {
            sortCombo.getItems().setAll("Plus récents", "Titre A-Z");
            sortCombo.getSelectionModel().selectFirst();
        }

        if (categoryCombo != null) {
            categoryCombo.getItems().setAll("Toutes", "Développement", "Bien-être", "Santé");
            categoryCombo.getSelectionModel().selectFirst();
        }

        // Configuration de la liste des modules
        if (modulesListView != null) {
            modulesListView.setCellFactory(lv -> new ListCell<Module>() {
                @Override
                protected void updateItem(Module item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getTitre() + (item.getId() > 0 ? " (Existant)" : " (Nouveau)"));
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

    // === NAVIGATION ===

    @FXML
    private void showAllFormations() {
        if (allFormationsView != null) {
            allFormationsView.setVisible(true);
            myFormationsView.setVisible(false);
            filterBar.setVisible(true);
            allFormationsBtn.setStyle(
                    "-fx-background-color: #5C98A8; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold;");
            myFormationsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: #1F2A33; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold; -fx-border-color: #DDD; -fx-border-radius: 20;");
            loadAllFormations();
        }
    }

    @FXML
    private void showMyFormations() {
        if (myFormationsView != null) {
            allFormationsView.setVisible(false);
            myFormationsView.setVisible(true);
            filterBar.setVisible(false);
            myFormationsBtn.setStyle(
                    "-fx-background-color: #5C98A8; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold;");
            allFormationsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: #1F2A33; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold; -fx-border-color: #DDD; -fx-border-radius: 20;");
            loadMyParticipations();
        }
    }

    // === CHARGEMENT ===

    private void loadAllFormations() {
        if (formationsFlowPane == null)
            return;
        formationsFlowPane.getChildren().clear();
        try {
            List<Formation> formations = formationService.read();
            for (Formation f : formations) {
                formationsFlowPane.getChildren().add(createFormationCard(f));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMyParticipations() {
        if (participationsFlowPane == null)
            return;
        participationsFlowPane.getChildren().clear();
        try {
            List<Participation> participations = participationService.findByUserId(currentUserId);
            if (participations.isEmpty()) {
                noParticipationsBox.setVisible(true);
            } else {
                noParticipationsBox.setVisible(false);
                for (Participation p : participations) {
                    participationsFlowPane.getChildren().add(createParticipationCard(p));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // === CARTES ===

    private VBox createFormationCard(Formation f) {
        VBox card = new VBox(12);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(260);

        StackPane imgCont = new StackPane();
        ImageView iv = new ImageView();
        iv.setFitHeight(150);
        iv.setFitWidth(230);
        iv.setPreserveRatio(true);
        setImageSafe(iv, f.getImagePath());

        HBox topActions = new HBox(8);
        topActions.setAlignment(Pos.TOP_RIGHT);
        topActions.setPadding(new Insets(5));
        topActions.setPickOnBounds(false);

        Button editBtn = new Button("✎");
        editBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 15; -fx-cursor: hand; -fx-text-fill: #5C98A8;");
        editBtn.setOnAction(e -> openEditPopup(f));

        Button delBtn = new Button("🗑");
        delBtn.setStyle(
                "-fx-background-color: rgba(255,235,238,0.9); -fx-text-fill: #D32F2F; -fx-background-radius: 15; -fx-cursor: hand;");
        delBtn.setOnAction(e -> confirmDelete(f));

        topActions.getChildren().addAll(editBtn, delBtn);
        imgCont.getChildren().addAll(iv, topActions);

        Label titre = new Label(f.getTitre());
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1F2A33;");
        titre.setWrapText(true);

        Label expert = new Label("👤 Expert Certifié");
        expert.setStyle("-fx-text-fill: #6E8E9A; -fx-font-size: 12px;");

        HBox bottom = new HBox(10);
        bottom.setAlignment(Pos.CENTER_LEFT);

        Button joinBtn = new Button("Rejoindre");
        joinBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #5C98A8, #7FB9C7); -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;");
        joinBtn.setOnAction(e -> handleRegistration(f));

        Label duree = new Label("⏱ " + f.getDuree());
        duree.setStyle("-fx-text-fill: #6E8E9A; -fx-font-size: 12px;");

        bottom.getChildren().addAll(joinBtn, duree);
        card.getChildren().addAll(imgCont, titre, expert, bottom);
        return card;
    }

    private VBox createParticipationCard(Participation p) {
        VBox card = new VBox(15);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 4);");
        card.setPrefWidth(280);

        Label titre = new Label(p.getTitreFormation());
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 17px; -fx-text-fill: #1F2A33;");
        titre.setWrapText(true);

        HBox statusBox = new HBox(5);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        Label statusDot = new Label("●");
        statusDot.setStyle("-fx-text-fill: #5C98A8;");
        Label statusText = new Label("ACTIF"); // On affiche par défaut ACTIF car le statut est ignoré en DB
        statusText.setStyle("-fx-text-fill: #5C98A8; -fx-font-size: 11px; -fx-font-weight: bold;");
        statusBox.getChildren().addAll(statusDot, statusText);

        Separator sep = new Separator();

        Button cancelBtn = new Button("Annuler l'inscription");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setStyle(
                "-fx-background-color: #FEF2F2; -fx-text-fill: #EF4444; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8;");
        cancelBtn.setOnAction(e -> {
            try {
                participationService.delete(p.getIdParticipation());
                loadMyParticipations();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(titre, statusBox, sep, cancelBtn);
        return card;
    }

    // === ACTIONS ===

    private void handleRegistration(Formation f) {
        try {
            Participation p = new Participation(currentUserId, f.getId(), new Date(), "accepté");
            participationService.create(p);
            showAlert("Succès", "Vous avez rejoint la formation : " + f.getTitre());
            if (formationsFlowPane != null)
                showMyFormations();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de rejoindre : " + e.getMessage());
        }
    }

    private void confirmDelete(Formation f) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la formation ?", ButtonType.YES,
                ButtonType.NO);
        if (alert.showAndWait().get() == ButtonType.YES) {
            try {
                formationService.delete(f.getId());
                loadAllFormations();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void openAjouterFormation() {
        showFormationPopup(null);
    }

    private void openEditPopup(Formation f) {
        showFormationPopup(f);
    }

    private void showFormationPopup(Formation f) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AjouterFormation.fxml"));
            Parent root = loader.load();
            FormationController controller = loader.getController();
            if (f != null)
                controller.setFormation(f);
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(f == null ? "Nouvelle Formation" : "Modifier Formation");
            dialog.getDialogPane().setContent(root);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
            loadAllFormations();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // === LOGIQUE FORMULAIRE ===

    public void setFormation(Formation f) {
        this.existingFormation = f;
        this.isEditMode = true;
        if (titreField != null) {
            titreField.setText(f.getTitre());
            if (descriptionArea != null)
                descriptionArea.setText(f.getDescription());
            dureeField.setText(f.getDuree());
            niveauCombo.setValue(f.getNiveau());
            if (mainButton != null)
                mainButton.setText("Enregistrer les modifications");
            this.selectedImagePath = f.getImagePath();
            setImageSafe(imagePreview, selectedImagePath);
            loadExistingModules(f.getId());
        }
    }

    private void loadExistingModules(int formationId) {
        try {
            modulesListView.getItems().setAll(moduleService.findByFormationId(formationId));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveAction(ActionEvent event) {
        try {
            if (isEditMode) {
                existingFormation.setTitre(titreField.getText());
                existingFormation.setDescription(descriptionArea.getText());
                existingFormation.setDuree(dureeField.getText());
                existingFormation.setNiveau(niveauCombo.getValue());
                existingFormation.setImagePath(selectedImagePath);
                formationService.update(existingFormation);
                saveModules(existingFormation.getId());
                showAlert("Succès", "Mise à jour réussie !");
            } else {
                Formation f = new Formation(titreField.getText(), descriptionArea.getText(), dureeField.getText(),
                        niveauCombo.getValue(), selectedImagePath);
                int id = formationService.create(f);
                saveModules(id);
                showAlert("Succès", "Formation ajoutée !");
            }
            closeWindow();
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void saveModules(int formationId) throws SQLException {
        for (Module m : modulesToAdd) {
            m.setFormationId(formationId);
            moduleService.create(m);
            for (Contenu c : m.getContenus()) {
                if (c.getId() == 0) {
                    c.setModuleId(m.getId());
                    contenuService.create(c);
                }
            }
        }
        modulesToAdd.clear();
    }

    @FXML
    private void ajouterModule() {
        if (moduleTitreField.getText().isEmpty()) {
            showAlert("Erreur", "Titre obligatoire");
            return;
        }
        if (moduleEnCours != null && modulesListView.getItems().contains(moduleEnCours)) {
            moduleEnCours.setTitre(moduleTitreField.getText());
            moduleEnCours.setDescription(moduleDescriptionArea.getText());
            modulesListView.refresh();
        } else {
            Module m = new Module(moduleTitreField.getText(), moduleDescriptionArea.getText());
            if (moduleEnCours != null)
                m.setContenus(new ArrayList<>(moduleEnCours.getContenus()));
            modulesListView.getItems().add(m);
            modulesToAdd.add(m);
        }
        resetModuleForm();
    }

    private void resetModuleForm() {
        moduleEnCours = null;
        moduleTitreField.clear();
        moduleDescriptionArea.clear();
        contenusListView.getItems().clear();
    }

    @FXML
    private void selectImage() {
        FileChooser fc = new FileChooser();
        File f = fc.showOpenDialog(imagePreview.getScene().getWindow());
        if (f != null) {
            selectedImagePath = f.getAbsolutePath();
            imagePreview.setImage(new Image(f.toURI().toString()));
            imagePathLabel.setText(f.getName());
        }
    }

    @FXML
    private void ajouterPDF() {
        ajouterFichier("PDF", "*.pdf");
    }

    @FXML
    private void ajouterVideo() {
        ajouterFichier("VIDEO", "*.mp4");
    }

    @FXML
    private void ajouterPodcast() {
        ajouterFichier("PODCAST", "*.mp3");
    }

    private void ajouterFichier(String type, String ext) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(type, ext));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            if (moduleEnCours == null)
                moduleEnCours = new Module("Nouveau", "");
            moduleEnCours.ajouterContenu(new Contenu(type, f.getAbsolutePath()));
            contenusListView.getItems().add(type + " - " + f.getName());
        }
    }

    @FXML
    private void supprimerModule() {
        Module m = modulesListView.getSelectionModel().getSelectedItem();
        if (m != null) {
            if (m.getId() > 0) {
                try {
                    moduleService.delete(m.getId());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            modulesListView.getItems().remove(m);
            modulesToAdd.remove(m);
            resetModuleForm();
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

    private void closeWindow() {
        if (titreField != null)
            ((Stage) titreField.getScene().getWindow()).close();
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
            iv.setImage(new Image(getClass().getResource("/images/psychologie.jpg").toExternalForm()));
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
