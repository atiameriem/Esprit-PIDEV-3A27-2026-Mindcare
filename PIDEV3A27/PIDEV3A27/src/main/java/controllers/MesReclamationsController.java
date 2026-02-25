package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.FlowPane;
import models.Reclamation;
import models.User;
import services.ReclamationService;
import utils.UserSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class MesReclamationsController {

    @FXML
    private FlowPane reclamationGrid;

    @FXML
    private javafx.scene.control.TextField searchField;

    private final ReclamationService reclamationService = new ReclamationService();
    private List<Reclamation> allMesReclamations;

    @FXML
    public void initialize() {
        loadReclamations();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterReclamations(newValue);
        });
    }

    public void loadReclamations() {
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null)
            return;

        try {
            List<Reclamation> all = reclamationService.getAll();
            allMesReclamations = all.stream()
                    .filter(r -> r.getIdUser() == currentUser.getId())
                    .collect(Collectors.toList());

            filterReclamations("");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void filterReclamations(String searchText) {
        if (allMesReclamations == null)
            return;

        String lowerCaseFilter = searchText.toLowerCase();

        List<Reclamation> filtered = allMesReclamations.stream()
                .filter(r -> r.getObjet().toLowerCase().contains(lowerCaseFilter)
                        || r.getDescription().toLowerCase().contains(lowerCaseFilter)
                        || r.getStatut().toLowerCase().contains(lowerCaseFilter))
                .collect(Collectors.toList());

        reclamationGrid.getChildren().clear();

        for (Reclamation r : filtered) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ReclamationCard.fxml"));
                Node card = loader.load();

                ReclamationCardController controller = loader.getController();
                controller.setData(r, this);

                reclamationGrid.getChildren().add(card);
            } catch (IOException e) {
                System.err.println("Erreur chargement carte reclamation: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleNewReclamation() {
        // Redirection vers la page de création de réclamation
        // On suppose que MindCareLayoutController gère cela via loadReclamation()
        // Mais ici on n'a pas accès direct au layout controller facilement sans passer
        // par la scène
        // Pour l'instant on affiche un message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Redirection vers le formulaire de réclamation...");
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
