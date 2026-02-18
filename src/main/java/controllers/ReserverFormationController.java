package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Formation;
import services.FormationService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

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

        // Image
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
                    // Try as resource if file not found
                    imageView.setImage(new Image(getClass().getResource("/images/psychologie.jpg").toExternalForm()));
                }
            } else {
                imageView.setImage(new Image(getClass().getResource("/images/psychologie.jpg").toExternalForm()));
            }
        } catch (Exception e) {
            System.err.println("Error loading image for formation " + f.getTitre() + ": " + e.getMessage());
        }

        // Title
        Label titleLabel = new Label(f.getTitre());
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Formateur (Placeholder as it's not in Formation model yet)
        Label formateurLabel = new Label("👤 Expert Certifié");
        formateurLabel.setStyle("-fx-text-fill: #777; -fx-font-size: 12px;");

        // Date/Duree
        Label dureeLabel = new Label("⏱ " + f.getDuree());
        dureeLabel.setStyle("-fx-text-fill: #777; -fx-font-size: 12px;");

        // Buttons
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        Button reserveBtn = new Button("Réserver");
        reserveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        reserveBtn.setOnAction(e -> openReservationPopup());

        Button detailsBtn = new Button("Voir détails");
        detailsBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #7B1FA2; -fx-border-color: #7B1FA2; -fx-border-radius: 5;");

        buttonsBox.getChildren().addAll(reserveBtn, detailsBtn);

        card.getChildren().addAll(imageView, titleLabel, formateurLabel, dureeLabel, buttonsBox);
        return card;
    }

    @FXML
    private void openReservationPopup() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/ReservationPopup.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Réserver cette formation");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAjouterFormation() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/AjouterFormation.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Ajouter une formation");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            stage.showAndWait();

            // Refresh list after window closes
            loadFormations();

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
