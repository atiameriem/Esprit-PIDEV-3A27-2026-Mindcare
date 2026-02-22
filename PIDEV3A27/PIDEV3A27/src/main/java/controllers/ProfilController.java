package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import models.User;
import services.UserService;
import utils.UserSession;

import java.io.IOException;
import java.util.List;

public class ProfilController {

    // ================= GRID USERS =================
    @FXML
    private FlowPane usersGrid;

    // ================= PROFIL FIELDS =================
    @FXML
    private TextField nomField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField telephoneField;
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label profileMessageLabel;
    @FXML
    private Label passwordMessageLabel;

    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();

        // -------- INITIALIZER LE PROFIL --------
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser != null) {
            nomField.setText(currentUser.getNom());
            emailField.setText(currentUser.getEmail());
            telephoneField.setText(currentUser.getTelephone() != null ? currentUser.getTelephone() : "");
        }

        // -------- CHARGER UTILISATEURS DE LA DB --------
        loadUsersFromDB();

        System.out.println("Vue Profil chargée avec vue en grille");
    }

    // ================= LOAD USERS =================
    private void loadUsersFromDB() {
        try {
            List<User> list = userService.getAll();
            usersGrid.getChildren().clear();

            if (list != null) {
                for (User user : list) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UserCard.fxml"));
                        Node card = loader.load();

                        UserCardController controller = loader.getController();
                        controller.setData(user, this);

                        usersGrid.getChildren().add(card);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les utilisateurs.");
        }
    }

    // ================= PROFIL =================
    @FXML
    private void handleSave() {
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null)
            return;

        String nom = nomField.getText();
        String email = emailField.getText();
        String phone = telephoneField.getText();

        if (nom.isEmpty() || email.isEmpty()) {
            profileMessageLabel.setText("Nom et Email sont obligatoires.");
            profileMessageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            currentUser.setNom(nom);
            currentUser.setEmail(email);
            currentUser.setTelephone(phone);

            userService.update(currentUser);

            profileMessageLabel.setText("Profil mis à jour avec succès !");
            profileMessageLabel.setStyle("-fx-text-fill: green;");

            // Re-charger la grille si on est admin pour voir le changement si nécessaire
            loadUsersFromDB();

        } catch (Exception e) {
            e.printStackTrace();
            profileMessageLabel.setText("Erreur lors de la mise à jour.");
            profileMessageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // ================= PASSWORD =================
    @FXML
    private void handleChangePassword() {
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null)
            return;

        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            passwordMessageLabel.setText("Veuillez remplir tous les champs.");
            passwordMessageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            passwordMessageLabel.setText("Les mots de passe ne correspondent pas.");
            passwordMessageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Note: Idéalement vérifier currentPass avec le mot de passe actuel en base
        try {
            currentUser.setMotDePasse(newPass);
            userService.update(currentUser);
            passwordMessageLabel.setText("Mot de passe modifié avec succès !");
            passwordMessageLabel.setStyle("-fx-text-fill: green;");

            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } catch (Exception e) {
            e.printStackTrace();
            passwordMessageLabel.setText("Erreur lors du changement de mot de passe.");
            passwordMessageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // ================= CRUD USERS =================
    @FXML
    private void handleAddUser() {
        User newUser = showUserDialog(null);
        if (newUser != null) {
            try {
                userService.create(newUser);
                loadUsersFromDB();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter l'utilisateur.");
            }
        }
    }

    public void handleUpdateUser(User user) {
        User updatedUser = showUserDialog(user);
        if (updatedUser != null) {
            try {
                userService.update(updatedUser);
                loadUsersFromDB();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour l'utilisateur.");
            }
        }
    }

    public void handleDeleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer " + user.getNom() + " ?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.delete(user.getId());
                    loadUsersFromDB();
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer l'utilisateur.");
                }
            }
        });
    }

    private User showUserDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UserDialog.fxml"));
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(user == null ? "Ajouter Utilisateur" : "Modifier Utilisateur");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(usersGrid.getScene().getWindow());
            dialogStage.setScene(new Scene(page));

            UserDialogController controller = loader.getController();
            controller.setUser(user);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                return controller.getUser();
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le dialogue.");
        }
        return null;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
