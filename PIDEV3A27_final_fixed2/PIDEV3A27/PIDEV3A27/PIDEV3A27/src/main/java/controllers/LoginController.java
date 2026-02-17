package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import services.AuthService;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        if (errorLabel != null) errorLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String pwd = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (email.isEmpty() || pwd.isEmpty()) {
            errorLabel.setText("Veuillez saisir l'email et le mot de passe.");
            return;
        }

        try {
            boolean ok = authService.login(email, pwd);
            if (!ok) {
                errorLabel.setText("Email ou mot de passe incorrect.");
                return;
            }

            Parent root = FXMLLoader.load(getClass().getResource("/MindCareLayout.fxml"));
            Scene scene = new Scene(root, 1200, 700);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("MindCare");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur connexion : " + e.getMessage());
        }
    }
}
