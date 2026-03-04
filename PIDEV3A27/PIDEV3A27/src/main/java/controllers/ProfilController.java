package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.LoginHistory;
import models.User;
import services.LoginHistoryService;
import services.UserService;
import utils.UserSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ProfilController {

    // ================= TABS =================
    @FXML
    private Button monProfilTabBtn;
    @FXML
    private Button historiqueTabBtn;
    @FXML
    private Button gestionUsersTabBtn;

    @FXML
    private VBox monProfilView;
    @FXML
    private VBox historiqueView;
    @FXML
    private VBox gestionUsersView;

    // ================= MON PROFIL =================
    @FXML
    private TextField nomField;
    @FXML
    private TextField prenomField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField telephoneField;
    @FXML
    private PasswordField oldPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label profileMsgLabel;
    @FXML
    private Label passwordMsgLabel;

    // ================= HISTORIQUE =================
    @FXML
    private VBox historyListContainer;

    // ================= GESTION UTILISATEURS =================
    @FXML
    private TextField searchUserField;
    @FXML
    private ComboBox<String> roleFilterCombo;
    @FXML
    private FlowPane usersGrid;

    private final UserService userService = new UserService();
    private final LoginHistoryService loginHistoryService = new LoginHistoryService();
    private List<User> allUsers;

    private static final String ACTIVE_TAB_STYLE = "-fx-background-color: #e8f0fe; -fx-text-fill: #1a73e8; -fx-font-weight: bold; "
            +
            "-fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand; " +
            "-fx-border-color: #1a73e8; -fx-border-radius: 25; -fx-border-width: 1;";
    private static final String INACTIVE_TAB_STYLE = "-fx-background-color: white; -fx-text-fill: #5f6368; -fx-font-weight: bold; "
            +
            "-fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand; " +
            "-fx-border-color: #dadce0; -fx-border-radius: 25; -fx-border-width: 1;";

    @FXML
    public void initialize() {
        loadUserData();

        User currentUser = UserSession.getInstance().getUser();
        if (currentUser != null && currentUser.getRole() == User.Role.Admin) {
            gestionUsersTabBtn.setVisible(true);
            gestionUsersTabBtn.setManaged(true);

            roleFilterCombo.getItems().addAll("Tous", "Admin", "RespensableC", "Patient", "Psychologue");
            roleFilterCombo.setValue("Tous");

            searchUserField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers());
            roleFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterUsers());

            refreshUserGrid();
        }
    }

    private void loadUserData() {
        User user = UserSession.getInstance().getUser();
        if (user != null) {
            nomField.setText(user.getNom());
            prenomField.setText(user.getPrenom());
            emailField.setText(user.getEmail());
            telephoneField.setText(user.getTelephone());
        }
    }

    // ================= TAB NAVIGATION =================

    @FXML
    private void showMonProfil() {
        show(monProfilView);
        hide(historiqueView);
        hide(gestionUsersView);
        monProfilTabBtn.setStyle(ACTIVE_TAB_STYLE);
        historiqueTabBtn.setStyle(INACTIVE_TAB_STYLE);
        gestionUsersTabBtn.setStyle(INACTIVE_TAB_STYLE);
    }

    @FXML
    private void showHistorique() {
        hide(monProfilView);
        show(historiqueView);
        hide(gestionUsersView);
        monProfilTabBtn.setStyle(INACTIVE_TAB_STYLE);
        historiqueTabBtn.setStyle(ACTIVE_TAB_STYLE);
        gestionUsersTabBtn.setStyle(INACTIVE_TAB_STYLE);
        loadLoginHistory();
    }

    @FXML
    private void showGestionUsers() {
        hide(monProfilView);
        hide(historiqueView);
        show(gestionUsersView);
        monProfilTabBtn.setStyle(INACTIVE_TAB_STYLE);
        historiqueTabBtn.setStyle(INACTIVE_TAB_STYLE);
        gestionUsersTabBtn.setStyle(ACTIVE_TAB_STYLE);
        refreshUserGrid();
    }

    private void show(VBox view) {
        view.setVisible(true);
        view.setManaged(true);
    }

    private void hide(VBox view) {
        view.setVisible(false);
        view.setManaged(false);
    }

    // ================= HISTORIQUE CONNEXIONS =================

    private void loadLoginHistory() {
        historyListContainer.getChildren().clear();

        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null)
            return;

        List<LoginHistory> history = loginHistoryService.getByUser(currentUser.getId());

        if (history.isEmpty()) {
            Label emptyLabel = new Label("Aucune connexion enregistrée pour l'instant.");
            emptyLabel.setStyle("-fx-text-fill: #5f6368; -fx-font-size: 14px; -fx-padding: 30;");
            historyListContainer.getChildren().add(emptyLabel);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss");

        for (LoginHistory h : history) {
            HBox card = buildHistoryCard(h, formatter);
            historyListContainer.getChildren().add(card);
        }
    }

    private HBox buildHistoryCard(LoginHistory h, DateTimeFormatter formatter) {
        HBox card = new HBox(18);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 18 25; -fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);");

        // Device Icon Circle
        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(52, 52);
        iconCircle.setMaxSize(52, 52);
        String bgColor = getBgColorForType(h.getDeviceType());
        iconCircle.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 50;");

        Label iconLabel = new Label(h.getDeviceIcon());
        iconLabel.setStyle("-fx-font-size: 22px;");
        iconCircle.getChildren().add(iconLabel);

        // Info
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String deviceDisplay = (h.getDeviceName() != null && !h.getDeviceName().isBlank())
                ? h.getDeviceName()
                : "Appareil inconnu";

        Label deviceLabel = new Label(deviceDisplay);
        deviceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #202124;");

        HBox metaRow = new HBox(12);
        metaRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label typeLabel = buildBadge(h.getDeviceType() != null ? h.getDeviceType() : "Unknown", "#e8f0fe", "#1a73e8");
        Label osLabel = buildBadge("🖥 " + (h.getOsName() != null ? h.getOsName() : "N/A"), "#f1f3f4", "#5f6368");
        Label ipLabel = buildBadge("🌐 " + (h.getIpAddress() != null ? h.getIpAddress() : "N/A"), "#fef7e0", "#f29900");

        metaRow.getChildren().addAll(typeLabel, osLabel, ipLabel);
        infoBox.getChildren().addAll(deviceLabel, metaRow);

        // Date
        VBox dateBox = new VBox(3);
        dateBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label dateLabel = new Label(h.getLoginDate() != null ? formatter.format(h.getLoginDate()) : "—");
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5f6368;");

        Label calIcon = new Label("📅");
        calIcon.setStyle("-fx-font-size: 16px;");

        dateBox.getChildren().addAll(calIcon, dateLabel);

        card.getChildren().addAll(iconCircle, infoBox, dateBox);
        return card;
    }

    private Label buildBadge(String text, String bg, String fg) {
        Label badge = new Label(text);
        badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-padding: 3 10; -fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold;");
        return badge;
    }

    private String getBgColorForType(String type) {
        if (type == null)
            return "#e8f0fe";
        return switch (type) {
            case "Laptop" -> "#e6f4ea";
            case "Mobile" -> "#fce8e6";
            case "Tablet" -> "#fef7e0";
            default -> "#e8f0fe";
        };
    }

    @FXML
    private void handleClearHistory() {
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Effacer l'historique");
        confirm.setHeaderText("Effacer tout l'historique de connexion ?");
        confirm.setContentText("Cette action est irréversible.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            loginHistoryService.clearHistory(currentUser.getId());
            loadLoginHistory();
        }
    }

    // ================= PROFIL ACTIONS =================

    @FXML
    private void handleUpdateProfile() {
        User user = UserSession.getInstance().getUser();
        if (user == null)
            return;

        user.setNom(nomField.getText());
        user.setPrenom(prenomField.getText());
        user.setTelephone(telephoneField.getText());

        try {
            userService.update(user);
            showFeedback(profileMsgLabel, "✅ Profil mis à jour avec succès !", "#1a73e8");
        } catch (SQLException e) {
            e.printStackTrace();
            showFeedback(profileMsgLabel, "❌ Erreur lors de la mise à jour.", "#d93025");
        }
    }

    @FXML
    private void handleChangePassword() {
        User user = UserSession.getInstance().getUser();
        if (user == null)
            return;

        String oldPass = oldPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showFeedback(passwordMsgLabel, "❌ Veuillez remplir tous les champs.", "#d93025");
            return;
        }
        if (!user.getMotDePasse().equals(oldPass)) {
            showFeedback(passwordMsgLabel, "❌ Ancien mot de passe incorrect.", "#d93025");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            showFeedback(passwordMsgLabel, "❌ Les mots de passe ne correspondent pas.", "#d93025");
            return;
        }
        if (newPass.length() < 8) {
            showFeedback(passwordMsgLabel, "❌ Minimum 8 caractères.", "#d93025");
            return;
        }

        try {
            user.setMotDePasse(newPass);
            userService.update(user);
            showFeedback(passwordMsgLabel, "✅ Mot de passe modifié !", "#1a73e8");
            oldPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } catch (SQLException e) {
            e.printStackTrace();
            showFeedback(passwordMsgLabel, "❌ Erreur serveur.", "#d93025");
        }
    }

    private void showFeedback(Label label, String text, String color) {
        label.setText(text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        label.setVisible(true);
    }

    // ================= GESTION UTILISATEURS =================

    public void refreshUserGrid() {
        try {
            allUsers = userService.read();
            filterUsers();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterUsers() {
        String search = (searchUserField.getText() != null) ? searchUserField.getText().toLowerCase() : "";
        String roleFilter = (roleFilterCombo.getValue() != null) ? roleFilterCombo.getValue() : "Tous";

        List<User> filtered = allUsers.stream().filter(u -> {
            boolean matchesSearch = u.getNom().toLowerCase().contains(search) ||
                    u.getPrenom().toLowerCase().contains(search) ||
                    u.getEmail().toLowerCase().contains(search);
            boolean matchesRole = roleFilter.equals("Tous") || u.getRole().name().equals(roleFilter);
            return matchesSearch && matchesRole;
        }).collect(Collectors.toList());

        displayUsers(filtered);
    }

    private void displayUsers(List<User> users) {
        usersGrid.getChildren().clear();
        for (User u : users) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UserCard.fxml"));
                Parent card = loader.load();
                UserCardController controller = loader.getController();
                controller.setData(u, this);
                usersGrid.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleAddUser() {
        showUserDialog(null);
    }

    public void handleUpdateUser(User user) {
        showUserDialog(user);
    }

    public void handleDeleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer l'utilisateur " + user.getNom() + " ?");
        alert.setContentText("Cette action est irréversible.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                userService.delete(user.getId());
                refreshUserGrid();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void showUserDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UserDialog.fxml"));
            Parent root = loader.load();
            UserDialogController controller = loader.getController();
            controller.setUser(user);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(user == null ? "Ajouter Utilisateur" : "Modifier Utilisateur");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.isSaveClicked()) {
                User handledUser = controller.getUser();
                if (handledUser.getId() == 0) {
                    userService.create(handledUser);
                } else {
                    userService.update(handledUser);
                }
                refreshUserGrid();

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setHeaderText(null);
                success.setContentText("Opération réussie avec succès !");
                success.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Action impossible");
            alert.setContentText("Le système n'a pas pu enregistrer l'utilisateur : " + e.getMessage());
            alert.showAndWait();
        }
    }
}
