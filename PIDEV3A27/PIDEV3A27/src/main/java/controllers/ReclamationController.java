package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import models.Reclamation;
import models.TypeReclamation;
import models.User;
import services.ReclamationService;
import utils.UserSession;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ReclamationController {

    // ===== FORMULAIRE =====
    @FXML
    private ComboBox<TypeReclamation> typeComboBox;

    @FXML
    private TextArea descriptionArea;

    // ===== TABLE =====
    @FXML
    private TableView<Reclamation> reclamationTable;

    @FXML
    private TableColumn<Reclamation, TypeReclamation> colType;

    @FXML
    private TableColumn<Reclamation, Object> colDate;

    @FXML
    private TableColumn<Reclamation, String> colStatut;

    @FXML
    private TableColumn<Reclamation, String> colDescription;

    @FXML
    private TableColumn<Reclamation, Void> colAction;

    private final ReclamationService reclamationService = new ReclamationService();
    private final ObservableList<Reclamation> reclamationList = FXCollections.observableArrayList();
    private Reclamation selectedReclamation = null;

    @FXML
    public void initialize() {

        // Remplir le ComboBox avec les valeurs de l'enum
        typeComboBox.setItems(FXCollections.observableArrayList(TypeReclamation.values()));

        // Colonnes
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        addActionButtons();
        loadReclamations();
    }

    // ================= LOAD =================
    private void loadReclamations() {

        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null)
            return;

        try {
            List<Reclamation> all = reclamationService.getAll();

            List<Reclamation> filtered = all.stream()
                    .filter(r -> r.getIdUser() == currentUser.getId())
                    .collect(Collectors.toList());

            reclamationList.setAll(filtered);
            reclamationTable.setItems(reclamationList);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les réclamations : " + e.getMessage());
        }
    }

    // ================= AJOUTER =================
    @FXML
    private void handleEnvoyer() {

        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté.");
            return;
        }

        TypeReclamation selectedType = typeComboBox.getValue();
        String description = descriptionArea.getText();

        if (selectedType == null || description == null || description.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Remplissez tous les champs.");
            return;
        }

        Reclamation r = new Reclamation();
        r.setIdUser(currentUser.getId());
        r.setType(selectedType);
        r.setDescription(description.trim());
        r.setStatut("En attente");

        try {
            reclamationService.create(r);
            clearFields();
            loadReclamations();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation ajoutée.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Ajout impossible : " + e.getMessage());
        }
    }

    // ================= UPDATE =================
    private void handleUpdate(Reclamation r) {

        TypeReclamation selectedType = typeComboBox.getValue();
        String description = descriptionArea.getText();

        if (selectedType == null || description == null || description.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Remplissez tous les champs.");
            return;
        }

        r.setType(selectedType);
        r.setDescription(description.trim());

        try {
            reclamationService.update(r);
            clearFields();
            loadReclamations();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation modifiée.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Modification impossible : " + e.getMessage());
        }
    }

    // ================= DELETE =================
    private void handleDelete(Reclamation r) {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous supprimer cette réclamation ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                reclamationService.delete(r.getId());
                loadReclamations();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réclamation supprimée.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible : " + e.getMessage());
            }
        }
    }

    // ================= RESET =================
    @FXML
    private void handleAnnuler() {
        clearFields();
    }

    private void clearFields() {
        typeComboBox.setValue(null);
        descriptionArea.clear();
        selectedReclamation = null;
    }

    // ================= ALERT =================
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ================= ACTION BUTTONS =================
    private void addActionButtons() {

        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button editBtn = new Button("✏");
            private final Button deleteBtn = new Button("🗑");
            private final HBox pane = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #FFC107; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white;");

                editBtn.setOnAction(event -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    typeComboBox.setValue(r.getType());
                    descriptionArea.setText(r.getDescription());
                    selectedReclamation = r;
                    //handleUpdate(r);
                });

                deleteBtn.setOnAction(event -> {
                    Reclamation r = getTableView().getItems().get(getIndex());
                    handleDelete(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }
}
