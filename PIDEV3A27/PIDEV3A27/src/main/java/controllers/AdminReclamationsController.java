package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Reclamation;
import services.ReclamationService;

import java.sql.SQLException;
import java.util.List;

public class AdminReclamationsController {

    @FXML
    private TableView<Reclamation> adminTable;
    @FXML
    private TableColumn<Reclamation, Integer> colId;
    @FXML
    private TableColumn<Reclamation, Integer> colUser;
    @FXML
    private TableColumn<Reclamation, String> colObjet;
    @FXML
    private TableColumn<Reclamation, String> colUrgence;
    @FXML
    private TableColumn<Reclamation, Object> colDate;
    @FXML
    private TableColumn<Reclamation, String> colStatut;
    @FXML
    private TableColumn<Reclamation, String> colDescription;
    @FXML
    private TableColumn<Reclamation, Void> colAction;

    private final ReclamationService reclamationService = new ReclamationService();
    private final ObservableList<Reclamation> allReclamations = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("idUser"));
        colObjet.setCellValueFactory(new PropertyValueFactory<>("objet"));
        colUrgence.setCellValueFactory(new PropertyValueFactory<>("urgence"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        addActionButtons();
        loadAllReclamations();
    }

    private void loadAllReclamations() {
        try {
            List<Reclamation> list = reclamationService.getAll();
            allReclamations.setAll(list);
            adminTable.setItems(allReclamations);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void refresh() {
        loadAllReclamations();
    }

    private void addActionButtons() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button treatBtn = new Button("Traiter");

            {
                treatBtn.setStyle(
                        "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 10; -fx-background-radius: 5;");
                treatBtn.setOnAction(event -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    handleTraiter(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    if ("Traité".equalsIgnoreCase(r.getStatut())) {
                        Label doneLabel = new Label("✓ Terminé");
                        doneLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        setGraphic(doneLabel);
                    } else {
                        setGraphic(treatBtn);
                    }
                }
            }
        });
    }

    private void handleTraiter(Reclamation r) {
        try {
            r.setStatut("Traité");
            reclamationService.update(r);
            loadAllReclamations(); // Refresh
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
