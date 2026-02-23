package controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import models.Reclamation;
import services.ReclamationService;

import java.sql.SQLException;
import java.util.List;

public class AdminReclamationsController {

    @FXML
    private FlowPane reclamationsGrid;

    private final ReclamationService reclamationService = new ReclamationService();

    @FXML
    public void initialize() {
        loadAllReclamations();
    }

    private void loadAllReclamations() {
        try {
            List<Reclamation> list = reclamationService.getAll();
            reclamationsGrid.getChildren().clear();

            for (Reclamation r : list) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void refresh() {
        loadAllReclamations();
    }
}
