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
import services.LoginHistoryService;
import javafx.stage.FileChooser;
import javafx.scene.layout.VBox;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.sql.SQLException;
import services.GoogleAuthService;
import services.FaceAuthService;
import com.google.api.services.oauth2.model.Userinfo;

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
    private RadioButton patientRadio;
    @FXML
    private RadioButton psychologueRadio;
    @FXML
    private ToggleGroup roleToggleGroup;
    @FXML
    private Label signupMessageLabel;
    @FXML
    private VBox badgeUploadBox;
    @FXML
    private Label badgeNameLabel;
    @FXML
    private Label faceStatusLabel;

    private String selectedBadgePath = null;
    private int pendingFaceUserId = -1; // Used during signup to defer face enrollment

    @FXML
    public void initialize() {
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

                // Enregistrer l'historique de connexion
                new LoginHistoryService().recordLogin(user.getId());

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
    private void handleGoogleLogin(ActionEvent event) {
        try {
            Userinfo googleUser = GoogleAuthService.authenticate();

            if (googleUser != null && googleUser.getEmail() != null) {
                UserService userService = new UserService();
                User user = userService.getByEmail(googleUser.getEmail());

                if (user == null) {
                    user = new User();
                    user.setEmail(googleUser.getEmail());
                    user.setNom(googleUser.getFamilyName() != null ? googleUser.getFamilyName() : "Utilisateur");
                    user.setPrenom(googleUser.getGivenName() != null ? googleUser.getGivenName() : "Google");
                    user.setMotDePasse("GOOGLE_USER");
                    user.setRole(User.Role.Patient);
                    user.setDateInscription(LocalDate.now());
                    user.setTelephone("00000000");

                    userService.create(user);
                }

                utils.UserSession.getInstance().setUser(user);
                System.out.println("Google Login success: " + user.getEmail());
                new LoginHistoryService().recordLogin(user.getId());
                messageLabel.setText("Connexion Google réussie !");
                loadMainView(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur lors de la connexion Google.");
        }
    }

    @FXML
    private void handleFaceLogin(ActionEvent event) {
        String email = emailField.getText();

        try {
            FaceAuthService faceService = new FaceAuthService();
            UserService userService = new UserService();
            User user = null;

            if (email.isEmpty()) {
                // Mode Identification : reconnaître l'utilisateur sans email
                messageLabel.setText("📷 Reconnaissance faciale en cours...");
                int identifiedId = faceService.identifyUser();

                if (identifiedId != -1) {
                    user = userService.getById(identifiedId);
                } else {
                    messageLabel.setText("❌ Visage non reconnu.");
                    return;
                }
            } else {
                // Mode Vérification : vérifier le visage pour l'email donné
                user = userService.getByEmail(email);
                if (user == null) {
                    messageLabel.setText("Utilisateur non trouvé.");
                    return;
                }

                messageLabel.setText("📷 Vérification faciale...");
                boolean match = faceService.verifyFace(user.getId());
                if (!match) {
                    messageLabel.setText("❌ Visage ne correspond pas.");
                    return;
                }
            }

            // Si on arrive ici, l'utilisateur est authentifié
            if (user != null) {
                utils.UserSession.getInstance().setUser(user);
                new LoginHistoryService().recordLogin(user.getId());
                messageLabel.setText("✅ Bienvenue " + user.getNom() + " !");
                loadMainView(event);
            }

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Erreur Face ID : " + e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        loadView(event, "/views/ForgotPassword.fxml", "Mot de passe oublié");
    }

    // ================= SIGNUP =================

    @FXML
    private void handleCaptureFaceSignup(ActionEvent event) {
        try {
            if (faceStatusLabel != null)
                faceStatusLabel.setText("📷 Regardez la caméra...");
            FaceAuthService faceService = new FaceAuthService();

            // Étape d'unicité Face ID : vérifier si ce visage appartient déjà à quelqu'un
            int identifiedId = faceService.identifyUser();
            if (identifiedId != -1) {
                if (faceStatusLabel != null)
                    faceStatusLabel.setText("❌ Ce visage est déjà associé à un compte.");
                return;
            }

            // Capture temporaire (-999)
            boolean success = faceService.enrollFace(-999);
            if (success) {
                if (faceStatusLabel != null)
                    faceStatusLabel.setText("✅ Visage capturé !");
                pendingFaceUserId = -999;
            } else {
                if (faceStatusLabel != null)
                    faceStatusLabel.setText("❌ Aucun visage détecté. Réessayez.");
            }
        } catch (Exception e) {
            if (faceStatusLabel != null)
                faceStatusLabel.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

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

        User.Role selectedRole = patientRadio.isSelected() ? User.Role.Patient : User.Role.Psychologue;
        newUser.setRole(selectedRole);
        newUser.setDateInscription(LocalDate.now());

        UserService us = new UserService();

        // Validation d'unicité et de sécurité
        try {
            if (us.existsByEmail(newUser.getEmail())) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "L'adresse mail doit être unique.");
                return;
            }
            if (us.existsByPhone(newUser.getTelephone())) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Ce numéro de téléphone est déjà utilisé.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de validation lors de l'inscription.");
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
            int newId = newUser.getId();

            if (newId <= 0) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la création du compte.");
                return;
            }

            // Si un visage a été capturé pendant l'inscription, on le lie au nouvel ID
            if (pendingFaceUserId == -999) {
                FaceAuthService faceService = new FaceAuthService();
                faceService.renameFaceData(-999, newId);
                pendingFaceUserId = -1; // Reset
            }

            // Automatic Login after Signup
            utils.UserSession.getInstance().setUser(newUser);
            System.out.println("User signed up and logged in: " + newUser.getNom());

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Inscription réussie !");
            loadMainView(event);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'inscription : " + e.getMessage());
        }
    }

    private boolean isSignupValid() {
        String email = signupEmailField.getText();
        String phone = signupPhoneField.getText();

        if (signupNomField.getText().isEmpty() ||
                signupPrenomField.getText().isEmpty() ||
                email.isEmpty() ||
                phone.isEmpty() ||
                signupPasswordField.getText().isEmpty() ||
                roleToggleGroup.getSelectedToggle() == null) {

            signupMessageLabel.setText("Veuillez remplir tous les champs.");
            return false;
        }

        // Password Length Check
        if (signupPasswordField.getText().length() < 8) {
            signupMessageLabel.setText("Le mot de passe doit contenir au moins 8 caractères.");
            signupMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            return false;
        }

        // Add Email Domain Validation
        String emailLower = email.toLowerCase();
        if (!emailLower.contains("@") || (!emailLower.endsWith(".com") && !emailLower.endsWith(".tn"))) {
            signupMessageLabel.setText("L'email doit contenir @ et se terminer par .com ou .tn.");
            signupMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            return false;
        }

        // Add Phone Length Validation
        if (phone.length() != 8 || !phone.matches("\\d+")) {
            signupMessageLabel.setText("Le numéro de téléphone doit contenir exactement 8 chiffres.");
            signupMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            return false;
        }
        return true;

    }

    @FXML
    private void handleRoleSelection() {
        if (psychologueRadio.isSelected()) {
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
            java.net.URL url = getClass().getResource(path);
            if (url == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Fichier FXML introuvable : " + path);
                return;
            }
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur de Navigation",
                    "Impossible de charger la vue : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
