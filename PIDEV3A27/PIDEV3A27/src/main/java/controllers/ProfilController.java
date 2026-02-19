package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.util.Callback;
import models.User;
import services.UserService;

import java.io.IOException;
import java.util.List;

public class ProfilController {

    // ================= TABLE USERS =================
    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, String> colNom;
    @FXML
    private TableColumn<User, String> colPrenom;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colTelephone;
    @FXML
    private TableColumn<User, String> colDate;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, Void> colAction;

    // ================= PROFIL FIELDS =================
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label profileMessageLabel;
    @FXML private Label passwordMessageLabel;

    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();

        // -------- CONFIG TABLE --------
        colNom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNom()));
        colPrenom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPrenom()));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colTelephone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTelephone()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty());
        colRole.setCellValueFactory(data -> {
            if (data.getValue() != null && data.getValue().getRole() != null) {
                return new SimpleStringProperty(data.getValue().getRole().toString());
            }
            return new SimpleStringProperty("");
        });

        // -------- ACTIONS COLUMN --------
        addButtonToTable();

        // -------- CHARGER UTILISATEURS DE LA DB --------
        loadUsersFromDB();

        System.out.println("Vue Profil chargée");
    }

    // ================= LOAD USERS =================
    private void loadUsersFromDB() {
        try {
            List<User> list = userService.getAll();
            usersList.clear();
            if (list != null) usersList.addAll(list);
            usersTable.setItems(usersList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les utilisateurs depuis la base.");
        }
    }

    // ================= PROFIL =================
    @FXML
    private void handleSave() {
        if (nomField.getText().isEmpty() || emailField.getText().isEmpty()) {
            profileMessageLabel.setText("Veuillez remplir tous les champs.");
            return;
        }
        profileMessageLabel.setText("Profil mis à jour avec succès !");
        // Ici tu peux appeler userService.update pour le profil courant
    }

    // ================= PASSWORD =================
    @FXML
    private void handleChangePassword() {
        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
            passwordMessageLabel.setText("Les mots de passe ne correspondent pas.");
            return;
        }
        passwordMessageLabel.setText("Mot de passe modifié avec succès !");
        // Ici tu peux appeler userService.updatePassword(userId, newPassword)
    }

    // ================= CRUD USERS =================
    @FXML
    private void handleAddUser() {
        User newUser = showUserDialog(null);
        if (newUser != null) {
            try {
                userService.create(newUser);
                usersList.add(newUser);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter l'utilisateur : " + e.getMessage());
            }
        }
    }

    private void handleUpdateUser(User user) {
        User updatedUser = showUserDialog(user);
        if (updatedUser != null) {
            try {
                userService.update(updatedUser);
                int index = usersList.indexOf(user);
                if (index >= 0) usersList.set(index, updatedUser);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour l'utilisateur : " + e.getMessage());
            }
        }
    }

    private void handleDeleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'utilisateur " + user.getNom() + " ?");
        alert.setContentText("Cette action est irréversible.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.delete(user.getId());
                    usersList.remove(user);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer l'utilisateur : " + e.getMessage());
                }
            }
        });
    }

    // ================= DIALOGUE =================
    private User showUserDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UserDialog.fxml"));
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(user == null ? "Ajouter Utilisateur" : "Modifier Utilisateur");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(usersTable.getScene().getWindow());
            dialogStage.setScene(new Scene(page));

            UserDialogController controller = loader.getController();
            controller.setUser(user);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                return controller.getUser();
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le dialogue utilisateur.");
        }
        return null;
    }

    // ================= ACTIONS TABLE =================
    private void addButtonToTable() {
        colAction.setCellFactory(col -> new TableCell<>() {

            private final Button btnEdit = new Button("✏️");
            private final Button btnDelete = new Button("🗑️");

            {
                btnEdit.setStyle("-fx-background-color: #bca4d5; -fx-text-fill: black; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #5022c5; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");

                btnEdit.setOnAction(event -> {
                    User user = getTableView().getItems().get(getTableRow().getIndex());
                    if (user != null) handleUpdateUser(user);
                });

                btnDelete.setOnAction(event -> {
                    User user = getTableView().getItems().get(getTableRow().getIndex());
                    if (user != null) handleDeleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(new HBox(5, btnEdit, btnDelete));
            }
        });
    }

    // ================= UTIL =================
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
