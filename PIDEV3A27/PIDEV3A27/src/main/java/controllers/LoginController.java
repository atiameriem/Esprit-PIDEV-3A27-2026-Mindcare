package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
// Removed standalone Role import to use User.Role

import services.UserService;
import javafx.stage.FileChooser;
import javafx.scene.layout.VBox;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;

    @FXML
    private TextField signupNomField;
    @FXML
    private TextField signupPrenomField;
    @FXML
    private TextField signupEmailField;
    @FXML
    private TextField signupPhoneField;
    @FXML
    private DatePicker signupDobPicker;
    @FXML
    private PasswordField signupPasswordField;
    @FXML
    private ComboBox<User.Role> signupRoleComboBox;
    @FXML
    private Label signupMessageLabel;
    @FXML
    private VBox badgeUploadBox;
    @FXML
    private Label badgeNameLabel;

    private String selectedBadgePath = null;

    @FXML
    public void initialize() {
        if (signupRoleComboBox != null) {
            signupRoleComboBox.getItems().setAll(User.Role.values());
        }
    }

    // ================= LOGIN =================
    @FXML
    private void handleLogin(ActionEvent event) {

        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        try {
            UserService us = new UserService();
            User user = us.authenticate(email, password);

            if (user != null) {
                // Set User Session
                utils.UserSession.getInstance().setUser(user);
                System.out.println("User logged in: " + user.getNom());

                messageLabel.setText("Connexion réussie !");
                loadMainView(event);
            } else {
                messageLabel.setText("Identifiants incorrects.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur base de données.");
        }
    }

    @FXML
    private void handleForgotPassword() {

        System.out.println("Mot de passe oublié cliqué");
        if (messageLabel != null) {
            messageLabel.setText("Fonctionnalité en cours de développement.");
        }
    }

    // ================= SIGNUP =================
    @FXML
    private void handleSignupAction(ActionEvent event) {

        if (!isSignupValid())
            return;

        User newUser = new User();
        newUser.setNom(signupNomField.getText());
        newUser.setPrenom(signupPrenomField.getText());
        newUser.setEmail(signupEmailField.getText());
        newUser.setTelephone(signupPhoneField.getText());
        newUser.setMotDePasse(signupPasswordField.getText());
        newUser.setRole(signupRoleComboBox.getValue());
        newUser.setDateInscription(LocalDate.now());

        UserService us = new UserService();

        // Email Uniqueness Check
        try {
            if (us.existsByEmail(newUser.getEmail())) {
                signupMessageLabel.setText("Cet email est déjà utilisé.");
                return;
            }
        } catch (SQLException e) {
            signupMessageLabel.setText("Erreur de validation email.");
            return;
        }

        // Age Calculation from DatePicker
        LocalDate dob = signupDobPicker.getValue();
        if (dob == null) {
            signupMessageLabel.setText("Veuillez saisir votre date de naissance.");
            return;
        }
        newUser.setDateNaissance(dob);

        // Approval Logic
        if (newUser.getRole() == User.Role.Psychologue) {
            if (selectedBadgePath == null) {
                signupMessageLabel.setText("Le badge est obligatoire pour les psychologues.");
                return;
            }
            newUser.setBadge(selectedBadgePath);
        }

        try {
            us.create(newUser);

            // Automatic Login after Signup
            utils.UserSession.getInstance().setUser(newUser);
            System.out.println("User signed up and logged in: " + newUser.getNom());

            signupMessageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            signupMessageLabel.setText("Inscription réussie !");
            loadMainView(event);
        } catch (SQLException e) {
            e.printStackTrace();
            signupMessageLabel.setText("Erreur lors de l'inscription.");
        } catch (Exception e) {
            e.printStackTrace();
            signupMessageLabel.setText("Erreur lors de la création.");
        }
    }

    private boolean isSignupValid() {
        if (signupNomField.getText().isEmpty() ||
                signupPrenomField.getText().isEmpty() ||
                signupEmailField.getText().isEmpty() ||
                signupPasswordField.getText().isEmpty() ||
                signupRoleComboBox.getValue() == null) {

            signupMessageLabel.setText("Veuillez remplir tous les champs.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleRoleSelection() {
        if (signupRoleComboBox.getValue() == User.Role.Psychologue) {
            badgeUploadBox.setVisible(true);
            badgeUploadBox.setManaged(true);
        } else {
            badgeUploadBox.setVisible(false);
            badgeUploadBox.setManaged(false);
            selectedBadgePath = null;
            badgeNameLabel.setText("Aucun fichier sélectionné");
        }
    }

    @FXML
    private void handleUploadBadge() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner votre badge professionnel");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(signupMessageLabel.getScene().getWindow());
        if (selectedFile != null) {
            selectedBadgePath = selectedFile.getAbsolutePath();
            badgeNameLabel.setText(selectedFile.getName());
        }
    }

    private void loadMainView(ActionEvent event) {
        loadView(event, "/MindCareLayout.fxml", "MindCare");
    }

    @FXML
    private void handleGoToSignup(ActionEvent event) {
        loadView(event, "/views/Signup.fxml", "Créer un compte");
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        loadView(event, "/views/Login.fxml", "Connexion");
    }

    private void loadView(ActionEvent event, String path, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
