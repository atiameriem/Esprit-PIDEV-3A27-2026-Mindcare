package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import models.User;
import utils.UserSession;

public class ReclamationTabsController {

    @FXML
    private TabPane reclamationTabPane;

    @FXML
    private Tab addTab;

    @FXML
    private Tab myReclamationsTab;

    @FXML
    private Tab adminTab;

    @FXML
    private MesReclamationsController mesReclamationsController;

    @FXML
    public void initialize() {
        User currentUser = UserSession.getInstance().getUser();

        if (currentUser != null) {
            // Seul l'Admin voit l'onglet de gestion
            if (currentUser.getRole() != User.Role.Admin) {
                reclamationTabPane.getTabs().remove(adminTab);
            }
        } else {
            // Si non connecté (normalement impossible ici), on cache l'admin par sécurité
            reclamationTabPane.getTabs().remove(adminTab);
        }

        // Ajouter un écouteur de sélection pour rafraîchir les données quand on change
        // d'onglet
        reclamationTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab == myReclamationsTab && mesReclamationsController != null) {
                mesReclamationsController.loadReclamations();
            }
        });
    }
}
