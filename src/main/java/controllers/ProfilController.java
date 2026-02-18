package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import services.ServiceQuiz;

import java.sql.SQLException;
import java.util.List;

public class ProfilController {

    @FXML
    private Button btnRetour;

    @FXML
    private ListView<String> listeQuizzes;

    private int idPatient;
    private final ServiceQuiz serviceQuiz = new ServiceQuiz();

    public void setIdPatient(int idPatient) {
        this.idPatient = idPatient;
        chargerQuizzes();
    }

    private void chargerQuizzes() {
        listeQuizzes.getItems().clear();
        try {
            List<String> historique = serviceQuiz.getHistoriquePatient(idPatient);
            for (String ligne : historique) {
                String nomQuiz = ligne.split(" \\| ")[0];
                String score = ligne.split(" \\| ")[1];
                listeQuizzes.getItems().add(nomQuiz + " - " + score);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement quizzes : " + e.getMessage());
        }
    }

    @FXML
    private void handleRetour() {
        Stage stage = (Stage) btnRetour.getScene().getWindow();
        stage.close();
    }
}
