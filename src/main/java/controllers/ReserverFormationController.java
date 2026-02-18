package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.Formation;
import services.FormationService;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

public class ReserverFormationController {

    @FXML
    private FlowPane formationsFlowPane;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortCombo;

    @FXML
    private ComboBox<String> categoryCombo;

    private final FormationService formationService = new FormationService();

    @FXML
    public void initialize() {
        loadFormations();
    }

    private void loadFormations() {
        formationsFlowPane.getChildren().clear();
        try {
            List<Formation> formations = formationService.read();
            for (Formation f : formations) {
                VBox card = createFormationCard(f);
                formationsFlowPane.getChildren().add(card);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les formations : " + e.getMessage());
        }
    }

    private VBox createFormationCard(Formation f) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white;" +
                "-fx-padding: 15;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4);");
        card.setPrefWidth(260);

        // --- IMAGE ET ACTIONS ---
        StackPane imageContainer = new StackPane();

        ImageView imageView = new ImageView();
        imageView.setFitHeight(150);
        imageView.setFitWidth(230);
        imageView.setPreserveRatio(true);

        try {
            if (f.getImagePath() != null && !f.getImagePath().isEmpty()) {
                File file = new File(f.getImagePath());
                if (file.exists()) {
                    imageView.setImage(new Image(file.toURI().toString()));
                } else {
                    imageView.setImage(new Image(getClass().getResource("/images/psychologie.jpg").toExternalForm()));
                }
            } else {
                imageView.setImage(new Image(getClass().getResource("/images/psychologie.jpg").toExternalForm()));
            }
        } catch (Exception e) {
            System.err.println("Error loading image for formation " + f.getTitre() + ": " + e.getMessage());
        }

        // Boutons Edit/Suppr overlay
        HBox topActions = new HBox(8);
        topActions.setAlignment(Pos.TOP_RIGHT);
        topActions.setPadding(new Insets(5));
        topActions.setPickOnBounds(false); // Permet de cliquer sur l'image si on ne clique pas sur les boutons

        Button editBtn = new Button("✎");
        editBtn.setStyle(
                "-fx-background-color: rgba(227, 242, 253, 0.9); -fx-text-fill: #1976D2; -fx-cursor: hand; -fx-background-radius: 15; -fx-font-weight: bold;");
        editBtn.setOnAction(e -> openEditPopup(f));

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle(
                "-fx-background-color: rgba(255, 235, 238, 0.9); -fx-text-fill: #D32F2F; -fx-cursor: hand; -fx-background-radius: 15; -fx-font-weight: bold;");
        deleteBtn.setOnAction(e -> confirmDelete(f));

        topActions.getChildren().addAll(editBtn, deleteBtn);
        imageContainer.getChildren().addAll(imageView, topActions);

        // --- INFOS ---
        Label titleLabel = new Label(f.getTitre());
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label formateurLabel = new Label("👤 Expert Certifié");
        formateurLabel.setStyle("-fx-text-fill: #777; -fx-font-size: 12px;");

        Label dureeLabel = new Label("⏱ " + f.getDuree());
        dureeLabel.setStyle("-fx-text-fill: #777; -fx-font-size: 12px;");

        // --- BOUTONS BAS ---
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        Button reserveBtn = new Button("Réserver");
        reserveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        reserveBtn.setOnAction(e -> openReservationPopup());

        Button detailsBtn = new Button("Voir détails");
        detailsBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #7B1FA2; -fx-border-color: #7B1FA2; -fx-border-radius: 5;");

        buttonsBox.getChildren().addAll(reserveBtn, detailsBtn);

        card.getChildren().addAll(imageContainer, titleLabel, formateurLabel, dureeLabel, buttonsBox);
        return card;
    }

    private void confirmDelete(Formation f) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer la formation ?");
        alert.setContentText("Voulez-vous vraiment supprimer : " + f.getTitre());

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                formationService.delete(f.getId());
                loadFormations(); // Rafraîchit la grille
            } catch (SQLException e) {
                showAlert("Erreur", "Suppression impossible : " + e.getMessage());
            }
        }
    }

    private void openEditPopup(Formation f) {
        Dialog<Formation> dialog = new Dialog<>();
        dialog.setTitle("Modifier la formation");
        dialog.setHeaderText("Mise à jour de : " + f.getTitre());

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 30, 20, 30));

        // Champs
        TextField titreField = new TextField(f.getTitre());
        TextField dureeField = new TextField(f.getDuree());
        TextArea descArea = new TextArea(f.getDescription());
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        ComboBox<String> niveauCombo = new ComboBox<>();
        niveauCombo.getItems().addAll("Débutant", "Intermédiaire", "Avancé");
        niveauCombo.setValue(f.getNiveau());

        // Image Preview (No path field as requested)
        ImageView preview = new ImageView();
        preview.setFitHeight(100);
        preview.setFitWidth(150);
        preview.setPreserveRatio(true);
        preview.setStyle("-fx-border-color: #DDD; -fx-border-width: 1;");

        final String[] currentPath = { f.getImagePath() };

        loadPreview(preview, currentPath[0]);

        Button btnBrowse = new Button("📸 Changer l'image");
        btnBrowse.setStyle("-fx-background-color: #7B1FA2; -fx-text-fill: white;");
        btnBrowse.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choisir une image");
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File selectedFile = fileChooser.showOpenDialog(grid.getScene().getWindow());
            if (selectedFile != null) {
                currentPath[0] = selectedFile.getAbsolutePath();
                loadPreview(preview, currentPath[0]);
            }
        });

        // Organisation
        grid.add(new Label("Titre :"), 0, 0);
        grid.add(titreField, 1, 0);

        grid.add(new Label("Durée :"), 0, 1);
        grid.add(dureeField, 1, 1);

        grid.add(new Label("Niveau :"), 0, 2);
        grid.add(niveauCombo, 1, 2);

        grid.add(new Label("Description :"), 0, 3);
        grid.add(descArea, 1, 3);

        grid.add(new Label("Aperçu :"), 0, 4);
        VBox imageBox = new VBox(10, preview, btnBrowse);
        imageBox.setAlignment(Pos.CENTER);
        grid.add(imageBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                f.setTitre(titreField.getText());
                f.setDuree(dureeField.getText());
                f.setDescription(descArea.getText());
                f.setNiveau(niveauCombo.getValue());
                f.setImagePath(currentPath[0]);
                return f;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                formationService.update(result);
                loadFormations();
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de modifier : " + e.getMessage());
            }
        });
    }

    private void loadPreview(ImageView iv, String path) {
        try {
            if (path != null && !path.isEmpty()) {
                File f = new File(path);
                if (f.exists()) {
                    iv.setImage(new Image(f.toURI().toString()));
                } else {
                    iv.setImage(new Image(getClass().getResource("/images/psychologie.jpg").toExternalForm()));
                }
            } else {
                iv.setImage(new Image(getClass().getResource("/images/psychologie.jpg").toExternalForm()));
            }
        } catch (Exception e) {
            iv.setImage(null);
        }
    }

    @FXML
    private void openAjouterFormation() {
        try {
            // Charger le contenu du FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AjouterFormation.fxml"));
            Parent root = loader.load();

            // Créer le Dialog (Pop-up)
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Ajouter une formation");

            // On définit le contenu du FXML comme corps du pop-up
            dialog.getDialogPane().setContent(root);

            // Ajouter un bouton de fermeture (obligatoire pour pouvoir fermer le pop-up)
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            // Optionnel : Cacher le bouton de fermeture si ton FXML a déjà ses propres
            // boutons
            // dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

            dialog.showAndWait();

            // Rafraîchir la liste après l'ajout
            loadFormations();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le formulaire d'ajout.");
        }
    }

    @FXML
    private void openReservationPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ReservationPopup.fxml"));
            Parent root = loader.load();

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Réserver cette formation");
            dialog.getDialogPane().setContent(root);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            dialog.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
