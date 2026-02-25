package controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import models.Reclamation;
import services.ReclamationService;

import java.sql.SQLException;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

public class AdminReclamationsController {

    @FXML
    private FlowPane reclamationsGrid;

    @FXML
    private javafx.scene.control.TextField searchField;

    @FXML
    private javafx.scene.control.ComboBox<String> sortComboBox;

    private final ReclamationService reclamationService = new ReclamationService();
    private List<Reclamation> allReclamations;

    @FXML
    public void initialize() {
        sortComboBox.getItems().addAll("Toutes", "Haute -> Basse", "Basse -> Haute");
        sortComboBox.setValue("Toutes");

        loadAllReclamations();

        searchField.textProperty().addListener((obs, oldV, newV) -> updateView());
        sortComboBox.valueProperty().addListener((obs, oldV, newV) -> updateView());
    }

    private void loadAllReclamations() {
        try {
            allReclamations = reclamationService.getAll();
            updateView();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateView() {
        if (allReclamations == null)
            return;

        String search = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase();
        String sortOption = sortComboBox.getValue();

        List<Reclamation> filtered = allReclamations.stream()
                .filter(r -> r.getObjet().toLowerCase().contains(search) || r.getStatut().toLowerCase().contains(search)
                        || r.getDescription().toLowerCase().contains(search))
                .collect(Collectors.toList());

        if ("Haute -> Basse".equals(sortOption)) {
            filtered.sort(Comparator.comparingInt(this::getPriorityValue).thenComparing(Reclamation::getId).reversed());
        } else if ("Basse -> Haute".equals(sortOption)) {
            filtered.sort(Comparator.comparingInt(this::getPriorityValue).thenComparing(Reclamation::getId));
        }

        reclamationsGrid.getChildren().clear();

        for (Reclamation r : filtered) {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/views/ReclamationAdminCard.fxml"));
                javafx.scene.Node card = loader.load();

                ReclamationAdminCardController controller = loader.getController();
                controller.setData(r, this);

                reclamationsGrid.getChildren().add(card);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int getPriorityValue(Reclamation r) {
        if (r.getUrgence() == null)
            return 0;
        switch (r.getUrgence().toLowerCase()) {
            case "high":
                return 3;
            case "medium":
                return 2;
            case "low":
                return 1;
            default:
                return 0;
        }
    }

    @FXML
    public void refresh() {
        loadAllReclamations();
    }
}
