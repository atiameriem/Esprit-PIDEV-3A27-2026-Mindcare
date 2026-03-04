package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.EmailServicep;
import services.UserService;
import java.io.IOException;
import java.util.Random;

public class ForgotPasswordController {

    @FXML
    private TextField forgotEmailField;
    @FXML
    private TextField verificationCodeField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private Label forgotMessageLabel;
    @FXML
    private VBox emailStepBox;
    @FXML
    private VBox resetStepBox;

    private String generatedCode;
    private String userEmail;
    private final UserService userService = new UserService();
    private final EmailServicep emailService = new EmailServicep();

    @FXML
    private void handleSendResetCode() {
        userEmail = forgotEmailField.getText();
        if (userEmail.isEmpty()) {
            setMessage("Veuillez entrer votre email.", true);
            return;
        }

        try {
            if (!userService.existsByEmail(userEmail)) {
                setMessage("Cet email n'existe pas.", true);
                return;
            }

            sendAndPrepareCode();

            emailStepBox.setVisible(false);
            emailStepBox.setManaged(false);
            resetStepBox.setVisible(true);
            resetStepBox.setManaged(true);

        } catch (Exception e) {
            e.printStackTrace();
            setMessage("Erreur lors de l'envoi du code.", true);
        }
    }

    @FXML
    private void handleResetPassword() {
        String enteredCode = verificationCodeField.getText();
        String newPass = newPasswordField.getText();

        if (enteredCode.isEmpty() || newPass.isEmpty()) {
            setMessage("Veuillez remplir tous les champs.", true);
            return;
        }

        if (!enteredCode.equals(generatedCode)) {
            setMessage("Code incorrect.", true);
            return;
        }

        if (newPass.length() < 8) {
            setMessage("Le nouveau mot de passe doit contenir au moins 8 caractères.", true);
            return;
        }

        try {
            userService.updatePassword(userEmail, newPass);
            setMessage("Mot de passe mis à jour ! Redirection vers la connexion...", false);

            // Redirect to login after 2 seconds
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(2));
            pause.setOnFinished(
                    e -> loadView(new ActionEvent(newPasswordField, null), "/views/Login.fxml", "Connexion"));
            pause.play();
        } catch (Exception e) {
            e.printStackTrace();
            setMessage("Erreur lors de la réinitialisation.", true);
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        loadView(event, "/views/Login.fxml", "Connexion");
    }

    private void sendAndPrepareCode() {
        generatedCode = String.format("%06d", new Random().nextInt(999999));

        // Create a background task for email sending
        javafx.concurrent.Task<Boolean> sendTask = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                return emailService.sendVerificationCode(userEmail, generatedCode);
            }
        };

        sendTask.setOnSucceeded(e -> {
            boolean sent = sendTask.getValue();
            if (sent) {
                setMessage("Code envoyé à " + userEmail + " ✓", false);
            } else {
                setMessage("Erreur lors de l'envoi. Vérifiez votre email.", true);
            }
        });

        sendTask.setOnFailed(e -> {
            setMessage("Erreur lors de l'envoi du code.", true);
            sendTask.getException().printStackTrace();
        });

        // Run in a background thread
        new Thread(sendTask).start();
        setMessage("Envoi en cours...", false);
    }

    @FXML
    private void handleResendCode() {
        setMessage("Envoi d'un nouveau code...", false);
        sendAndPrepareCode();
    }

    private void setMessage(String msg, boolean isError) {
        forgotMessageLabel.setText(msg);
        forgotMessageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
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