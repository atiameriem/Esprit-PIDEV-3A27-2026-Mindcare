package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import models.Formation;
import models.Module;
import models.Contenu;
import services.FormationService;
import services.ModuleService;
import services.ContenuService;

import java.io.File;
import java.sql.SQLException;

public class AjouterModulesController {

    @FXML private TextField moduleTitreField;
    @FXML private TextArea moduleDescriptionArea;
    @FXML private ListView<String> contenusListView;
    @FXML private ListView<String> modulesListView;

    private Formation formation;
    private Module moduleEnCours;

    public void setFormation(Formation formation) {
        this.formation = formation;
    }

    /* ======================= */
    /* ===== AJOUT CONTENU === */
    /* ======================= */

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

    private void ajouterFichier(String type, String extension) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(type + " Files", extension)
        );

        File file = fileChooser.showOpenDialog(null);

        if (file != null) {

            if (moduleEnCours == null) {
                moduleEnCours = new Module(
                        moduleTitreField.getText(),
                        moduleDescriptionArea.getText()
                );
            }

            Contenu contenu = new Contenu(type, file.getAbsolutePath());
            moduleEnCours.ajouterContenu(contenu);

            contenusListView.getItems()
                    .add(type + " - " + file.getName());
        }
    }

    /* ======================= */
    /* ===== AJOUT MODULE ==== */
    /* ======================= */

    @FXML
    private void ajouterModule() {

        if (moduleTitreField.getText().isEmpty()) {
            showAlert("Erreur", "Titre du module obligatoire.");
            return;
        }

        if (moduleEnCours == null) {
            moduleEnCours = new Module(
                    moduleTitreField.getText(),
                    moduleDescriptionArea.getText()
            );
        }

        formation.ajouterModule(moduleEnCours);

        modulesListView.getItems()
                .add(moduleTitreField.getText());

        moduleEnCours = null;
        moduleTitreField.clear();
        moduleDescriptionArea.clear();
        contenusListView.getItems().clear();
    }

    /* ======================= */
    /* ===== SAVE FINAL ====== */
    /* ======================= */

    @FXML
    private void saveFormation() {
        try {
            FormationService formationService = new FormationService();
            ModuleService moduleService = new ModuleService();
            ContenuService contenuService = new ContenuService();

            formationService.create(formation);

            for (Module module : formation.getModules()) {
                module.setFormationId(formation.getId());
                moduleService.create(module);
                for (Contenu contenu : module.getContenus()) {
                    contenu.setModuleId(module.getId());
                    contenuService.create(contenu);
                }
            }

            showAlert("Succès",
                    "Formation enregistrée avec "
                            + formation.getModules().size()
                            + " module(s).");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'enregistrer la formation : " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

