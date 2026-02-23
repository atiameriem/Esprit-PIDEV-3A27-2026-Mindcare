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
            if (currentUser.getRole() == User.Role.Admin) {
                // Admin: Only sees Management
                reclamationTabPane.getTabs().remove(addTab);
                reclamationTabPane.getTabs().remove(myReclamationsTab);
            } else {
                // Patients/Psychologues: Only see usage
                reclamationTabPane.getTabs().remove(adminTab);
            }
        } else {
            // Default security
            reclamationTabPane.getTabs().clear();
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
