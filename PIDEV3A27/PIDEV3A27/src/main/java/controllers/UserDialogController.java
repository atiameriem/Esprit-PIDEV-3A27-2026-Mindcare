package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;

import java.time.LocalDate;

public class UserDialogController {

    @FXML
    private TextField nomField;
    @FXML
    private TextField prenomField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField telephoneField;
    @FXML
    private TextField ageField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<User.Role> roleComboBox;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private User user;
    private boolean saveClicked = false;

    @FXML
    public void initialize() {
        // Initialise la ComboBox avec toutes les valeurs de User.Role
        roleComboBox.setItems(FXCollections.observableArrayList(User.Role.values()));
    }

    public void setUser(User user) {
        this.user = user;

        if (user != null) {
            nomField.setText(user.getNom());
            prenomField.setText(user.getPrenom());
            emailField.setText(user.getEmail());
            telephoneField.setText(user.getTelephone());
            ageField.setText(String.valueOf(user.getAge()));
            passwordField.setText(user.getMotDePasse());
            roleComboBox.getItems().setAll(User.Role.values());
            // ✅ fonctionne maintenant
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public User getUser() {
        return user;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            if (user == null) {
                user = new User();
                user.setDateInscription(LocalDate.now());
            } else if (user.getDateInscription() == null) {
                // Si l'utilisateur existait mais n'avait pas de date (cas des anciennes
                // données)
                user.setDateInscription(LocalDate.now());
            }
            user.setNom(nomField.getText());
            user.setPrenom(prenomField.getText());
            user.setEmail(emailField.getText());
            user.setTelephone(telephoneField.getText());
            try {
                user.setAge(Integer.parseInt(ageField.getText()));
            } catch (NumberFormatException e) {
                user.setAge(0); // Valeur par défaut
            }
            user.setMotDePasse(passwordField.getText()); // hash si besoin
            user.setRole(roleComboBox.getValue());

            saveClicked = true;
            closeDialog();
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (nomField.getText() == null || nomField.getText().isEmpty()) {
            errorMessage += "Nom invalide!\n";
        }
        if (prenomField.getText() == null || prenomField.getText().isEmpty()) {
            errorMessage += "Prénom invalide!\n";
        }
        if (emailField.getText() == null || emailField.getText().isEmpty()) {
            errorMessage += "Email invalide!\n";
        }
        if (roleComboBox.getValue() == null) {
            errorMessage += "Rôle invalide!\n";
        }

        // Validation basique pour Age
        try {
            if (ageField.getText() != null && !ageField.getText().isEmpty()) {
                Integer.parseInt(ageField.getText());
            }
        } catch (NumberFormatException e) {
            errorMessage += "Age doit être un nombre!\n";
        }

        if (!errorMessage.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Champs Invalides");
            alert.setHeaderText("Veuillez corriger les champs invalides");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }

        return true;
    }
}
