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
import utils.Session;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        if (errorLabel != null) errorLabel.setText("");
    }

    @FXML
    private void handleLogin() {

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String pwd   = passwordField.getText() == null ? "" : passwordField.getText().trim();

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

            // ✅ Correction du mapping rôle DB → enum Session.Role
            // La DB stocke : 'Patient', 'Admin', 'ResponsableC', 'Psychologue'
            // L'enum Session.Role utilise : USER, ADMIN, RESPONSABLEC, PSYCHOLOGUE
            corrigerRoleSession();

            // ✅ Chargement du bon layout selon le rôle
            String fxmlPath = determinerLayout();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) {
                throw new RuntimeException(fxmlPath + " introuvable dans resources !");
            }

            Parent root  = loader.load();
            Scene  scene = new Scene(root, 1200, 700);
            Stage  stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("MindCare");
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur connexion : " + e.getMessage());
        }
    }

    /**
     * ✅ Corrige le mapping entre le rôle stocké en DB et l'enum Session.Role.
     *
     * DB value        → Session.Role
     * "Patient"       → USER
     * "Admin"         → ADMIN
     * "ResponsableC"  → RESPONSABLEC
     * "Psychologue"   → PSYCHOLOGUE
     *
     * Si AuthService a mal mappé le rôle (ex: crash valueOf),
     * cette méthode relit le rôle brut depuis l'utilisateur connecté
     * et reforce le bon enum.
     */
    private void corrigerRoleSession() {
        // Si AuthService a correctement mappé → rien à faire
        if (Session.getRoleConnecte() != null) return;

        // Fallback : lire le rôle brut depuis l'objet utilisateur
        models.Users u = Session.getUtilisateurConnecte();
        if (u == null) return;

        String roleDB = u.getRole();
        if (roleDB == null) return;

        Session.Role roleCorrige = convertirRole(roleDB.trim());

        // Recréer la session avec le bon rôle
        Session.login(
                Session.getUserId(),
                roleCorrige,
                Session.getFullName()
        );
    }

    /**
     * ✅ Convertit la valeur DB en enum Session.Role
     */
    private Session.Role convertirRole(String roleDB) {
        switch (roleDB) {
            case "Patient":      return Session.Role.USER;
            case "Admin":        return Session.Role.ADMIN;
            case "ResponsableC": return Session.Role.RESPONSABLEC;
            case "Psychologue":  return Session.Role.PSYCHOLOGUE;
            default:
                System.err.println("⚠️ Rôle DB inconnu : '" + roleDB + "' → USER par défaut");
                return Session.Role.USER;
        }
    }

    /**
     * ✅ Choisit le bon FXML layout selon le rôle
     *
     * USER + RESPONSABLEC  → /MindCareLayout.fxml
     * ADMIN + PSYCHOLOGUE  → /MindCareLayout.fxml  (ou MindCareLayoutPsy.fxml si séparé)
     */
    private String determinerLayout() {
        Session.Role role = Session.getRoleConnecte();
        if (role == null) return "/MindCareLayout.fxml";

        switch (role) {
            case ADMIN:
            case PSYCHOLOGUE:
                // Si vous avez un layout séparé pour psy/admin, changez ici :
                // return "/MindCareLayoutPsy.fxml";
                return "/MindCareLayout.fxml";
            case USER:
            case RESPONSABLEC:
            default:
                return "/MindCareLayout.fxml";
        }
    }
}