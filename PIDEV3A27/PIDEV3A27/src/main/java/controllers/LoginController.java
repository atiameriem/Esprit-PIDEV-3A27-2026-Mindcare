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

import java.io.IOException;
import java.time.LocalDate;

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
    private TextField signupAgeField;
    @FXML
    private PasswordField signupPasswordField;
    @FXML
    private ComboBox<User.Role> signupRoleComboBox;
    @FXML
    private Label signupMessageLabel;

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

        try {
            newUser.setAge(Integer.parseInt(signupAgeField.getText()));
        } catch (NumberFormatException e) {
            signupMessageLabel.setText("L'âge doit être un nombre.");
            return;
        }

        newUser.setDateInscription(LocalDate.parse(LocalDate.now().toString()));

        try {
            UserService us = new UserService();
            us.create(newUser);
            signupMessageLabel.setText("Compte créé avec succès !");
            handleBackToLogin(event);

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
