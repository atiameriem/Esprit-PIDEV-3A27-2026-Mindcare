package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.User;
import services.ServiceAuth;
import utils.Session;

public class LoginController {

    @FXML private TextField tfEmailOrUsername;
    @FXML private PasswordField pfPassword;
    @FXML private Label lblError;

    private final ServiceAuth serviceAuth = new ServiceAuth();

    @FXML
    private void onLogin(ActionEvent event) {
        lblError.setText("");

        String ident = tfEmailOrUsername.getText();
        String pass = pfPassword.getText();

        User u = serviceAuth.login(ident, pass);
        if (u == null) {
            lblError.setText("Email/Nom ou mot de passe incorrect.");
            return;
        }

        // Session
        Session.idUsers = u.getIdUsers();
        Session.fullname = u.getFullname();
        Session.email = (u.getEmail() == null) ? "" : u.getEmail().trim();
        Session.role = (u.getRole() == null) ? "" : u.getRole().trim();

        // Redirect -> layout principal
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/MindCareLayout.fxml"));
            Stage stage = (Stage) tfEmailOrUsername.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            lblError.setText("Erreur de chargement: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("LOGIN OK => id=" + Session.idUsers + " role=" + Session.role + " email=" + Session.email);

    }

}
