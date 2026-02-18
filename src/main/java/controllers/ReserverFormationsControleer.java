package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;

public class ReserverFormationsControleer {

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private DatePicker datePicker;

    @FXML
    private void saveReservation() {

        String nom = nomField.getText();
        String email = emailField.getText();
        LocalDate date = datePicker.getValue();

        if (nom.isEmpty() || email.isEmpty() || date == null) {
            showAlert("Erreur", "Tous les champs sont obligatoires.");
            return;
        }

        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/psychologie_db",
                    "root",
                    "password");

            String sql = "INSERT INTO reservation (nom, email, date_reservation) VALUES (?, ?, ?)";

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, nom);
            pst.setString(2, email);
            pst.setDate(3, Date.valueOf(date));

            pst.executeUpdate();

            conn.close();

            showAlert("Succès", "Réservation enregistrée !");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Problème connexion base.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
