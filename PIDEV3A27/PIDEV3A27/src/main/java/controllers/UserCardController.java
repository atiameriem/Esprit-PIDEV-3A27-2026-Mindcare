package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import models.User;

public class UserCardController {

    @FXML
    private Label initialsLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label phoneLabel;

    private User user;
    private ProfilController parentController;

    public void setData(User user, ProfilController parentController) {
        this.user = user;
        this.parentController = parentController;

        nameLabel.setText(user.getNom() + " " + user.getPrenom());
        roleLabel.setText(user.getRole().toString());
        emailLabel.setText(user.getEmail());
        phoneLabel.setText(user.getTelephone() != null ? user.getTelephone() : "N/A");

        String initials = "";
        if (user.getNom() != null && !user.getNom().isEmpty())
            initials += user.getNom().substring(0, 1).toUpperCase();
        if (user.getPrenom() != null && !user.getPrenom().isEmpty())
            initials += user.getPrenom().substring(0, 1).toUpperCase();
        initialsLabel.setText(initials);
    }

    @FXML
    private void handleEdit() {
        if (parentController != null) {
            parentController.handleUpdateUser(user);
        }
    }

    @FXML
    private void handleDelete() {
        if (parentController != null) {
            parentController.handleDeleteUser(user);
        }
    }
}
