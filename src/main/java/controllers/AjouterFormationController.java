package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import models.Formation;

import java.io.File;

public class AjouterFormationController {

    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField dureeField;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private ImageView imagePreview;
    @FXML private Label imagePathLabel;

    private String selectedImagePath;

    @FXML
    public void initialize() {
        niveauCombo.getItems().addAll("Débutant", "Intermédiaire", "Avancé");
    }

    @FXML
    private void goToModules(ActionEvent event) {
        try {

            if (titreField.getText().isEmpty()) {
                showAlert("Erreur", "Le titre est obligatoire.");
                return;
            }

            Formation formation = new Formation(
                    titreField.getText(),
                    descriptionArea.getText(),
                    dureeField.getText(),
                    niveauCombo.getValue(),
                    selectedImagePath != null ? selectedImagePath : ""
            );

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/AjouterModules.fxml"));

            Parent root = loader.load();

            AjouterModulesController controller = loader.getController();
            controller.setFormation(formation);

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void selectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File selectedFile = fileChooser.showOpenDialog(imagePreview.getScene().getWindow());
        if (selectedFile != null) {
            selectedImagePath = selectedFile.getAbsolutePath();
            imagePathLabel.setText(selectedFile.getName());
            
            // Afficher l'aperçu de l'image
            Image image = new Image("file:" + selectedImagePath);
            imagePreview.setImage(image);
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
