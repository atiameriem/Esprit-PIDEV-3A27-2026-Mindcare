package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;

import java.time.LocalDate;
import java.sql.SQLException;
import services.UserService;

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
    private DatePicker dobPicker;
    @FXML
    private PasswordField passwordField;
    @FXML
    private RadioButton patientRadio;
    @FXML
    private RadioButton psychologueRadio;
    @FXML
    private RadioButton adminRadio;
    @FXML
    private RadioButton responsableRadio;
    @FXML
    private ToggleGroup roleToggleGroup;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private User user;
    private boolean saveClicked = false;

    @FXML
    public void initialize() {
    }

    public void setUser(User user) {
        this.user = user;

        if (user != null) {
            nomField.setText(user.getNom());
            prenomField.setText(user.getPrenom());
            emailField.setText(user.getEmail());
            telephoneField.setText(user.getTelephone());
            dobPicker.setValue(user.getDateNaissance());
            passwordField.setText(user.getMotDePasse());

            if (user.getRole() != null) {
                switch (user.getRole()) {
                    case Patient:
                        patientRadio.setSelected(true);
                        break;
                    case Psychologue:
                        psychologueRadio.setSelected(true);
                        break;
                    case ResponsableC:
                        responsableRadio.setSelected(true);
                        break;
                    case Admin:
                        adminRadio.setSelected(true);
                        break;
                }
            }
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
            user.setNom(nomField.getText().trim());
            user.setPrenom(prenomField.getText().trim());
            user.setEmail(emailField.getText().trim());
            user.setTelephone(telephoneField.getText().trim());

            LocalDate dob = dobPicker.getValue();
            user.setDateNaissance(dob);
            user.setMotDePasse(passwordField.getText()); // hash si besoin

            User.Role selectedRole = User.Role.Patient;
            if (psychologueRadio.isSelected())
                selectedRole = User.Role.Psychologue;
            else if (adminRadio.isSelected())
                selectedRole = User.Role.Admin;
            else if (responsableRadio.isSelected())
                selectedRole = User.Role.ResponsableC;
            user.setRole(selectedRole);

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

        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
            errorMessage += "Nom invalide!\n";
        }
        if (prenomField.getText() == null || prenomField.getText().trim().isEmpty()) {
            errorMessage += "Prénom invalide!\n";
        }
        if (emailField.getText() == null || emailField.getText().trim().isEmpty()) {
            errorMessage += "Email invalide!\n";
        } else {
            String email = emailField.getText().trim();
            if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                errorMessage += "Format d'email invalide (ex: user@domain.com)!\n";
            }
        }

        if (roleToggleGroup.getSelectedToggle() == null) {
            errorMessage += "Rôle invalide!\n";
        }

        String phone = telephoneField.getText();
        if (phone == null || phone.length() != 8 || !phone.matches("\\d+")) {
            errorMessage += "Le numéro de téléphone doit contenir exactement 8 chiffres!\n";
        }
        if (passwordField.getText() == null || passwordField.getText().length() < 8) {
            errorMessage += "Le mot de passe doit contenir au moins 8 caractères!\n";
        }

        // Email Uniqueness Check
        UserService us = new UserService();
        try {
            boolean exists;
            if (user == null || user.getId() == 0) {
                exists = us.existsByEmail(emailField.getText());
            } else {
                exists = us.existsByEmailExcludeId(emailField.getText(), user.getId());
            }
            if (exists) {
                errorMessage += "Cet email est déjà utilisé!\n";
            }
        } catch (SQLException e) {
            errorMessage += "Erreur de validation email en base.\n";
        }

        // Validation basique pour Date de Naissance
        if (dobPicker.getValue() == null) {
            errorMessage += "Date de naissance invalide!\n";
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
