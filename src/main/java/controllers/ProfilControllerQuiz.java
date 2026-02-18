package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import services.ServiceReponse;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class ProfilControllerQuiz {

    @FXML
    private VBox containerReponses;

    @FXML
    private Button btnRetour;

    private int idPatient;

    private final ServiceReponse serviceReponse = new ServiceReponse();

    public void setIdPatient(int idPatient) {
        this.idPatient = idPatient;
        afficherResultats();
    }

    @FXML
    private void handleRetour() {
        Stage stage = (Stage) btnRetour.getScene().getWindow();
        stage.close();
    }

    private void afficherResultats() {
        containerReponses.getChildren().clear();

        try {
            List<String> details = serviceReponse.getDetailsReponsesPatient(idPatient);

            // Calculer le score par quiz
            Map<String, Integer> quizScores = new LinkedHashMap<>();

            for (String ligne : details) {
                // Ligne : "Quiz: nom | Question: texte | Réponse: texte | Valeur: x"
                String[] parts = ligne.split("\\|");
                String titreQuiz = parts[0].replace("Quiz: ", "").trim();
                int valeur = Integer.parseInt(parts[3].replace("Valeur:", "").trim());

                quizScores.put(titreQuiz, quizScores.getOrDefault(titreQuiz, 0) + valeur);
            }

            // Afficher chaque quiz et son score total
            for (Map.Entry<String, Integer> entry : quizScores.entrySet()) {
                Label lblQuiz = new Label(entry.getKey() + " → " + entry.getValue() + "%");
                lblQuiz.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
                containerReponses.getChildren().add(lblQuiz);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
