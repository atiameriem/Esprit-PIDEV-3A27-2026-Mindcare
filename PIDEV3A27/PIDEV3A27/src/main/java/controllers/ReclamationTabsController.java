package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import models.User;
import utils.UserSession;

public class ReclamationTabsController {

    @FXML
    private Button addTabBtn, myReclamationsTabBtn, adminTabBtn;
    @FXML
    private VBox addContent, myReclamationsContent, adminContent;

    @FXML
    private MesReclamationsController mesReclamationsController;

    @FXML
    public void initialize() {
        User currentUser = UserSession.getInstance().getUser();

        if (currentUser != null) {
            User.Role role = currentUser.getRole();
            System.out.println("🔑 Role connecté : " + role);

            if (role == User.Role.Admin) {
                // Admin ONLY: sees administration panel, cannot add or view own reclamations
                addTabBtn.setManaged(false);
                addTabBtn.setVisible(false);
                myReclamationsTabBtn.setManaged(false);
                myReclamationsTabBtn.setVisible(false);
                showAdminTab();
            } else {
                // ResponsableC, Patient, Psychologue: standard view
                adminTabBtn.setManaged(false);
                adminTabBtn.setVisible(false);
                showAddTab();
            }
        }
    }

    @FXML
    private void showAddTab() {
        switchTab(addContent, addTabBtn);
    }

    @FXML
    private void showMyTab() {
        switchTab(myReclamationsContent, myReclamationsTabBtn);
        if (mesReclamationsController != null) {
            mesReclamationsController.loadReclamations();
        }
    }

    @FXML
    private void showAdminTab() {
        switchTab(adminContent, adminTabBtn);
    }

    private void switchTab(VBox content, Button btn) {
        // Reset all
        addContent.setVisible(false);
        addContent.setManaged(false);
        myReclamationsContent.setVisible(false);
        myReclamationsContent.setManaged(false);
        adminContent.setVisible(false);
        adminContent.setManaged(false);

        resetBtnStyle(addTabBtn);
        resetBtnStyle(myReclamationsTabBtn);
        resetBtnStyle(adminTabBtn);

        // Show selected
        content.setVisible(true);
        content.setManaged(true);

        // Premium active style
        btn.setStyle(
                "-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 12; -fx-cursor: hand;");
    }

    private void resetBtnStyle(Button btn) {
        btn.setStyle(
                "-fx-background-color: white; -fx-text-fill: #546E7A; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 12; -fx-border-color: #E0E0E0; -fx-border-radius: 12; -fx-cursor: hand;");
    }
}
