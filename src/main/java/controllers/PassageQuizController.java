package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.Question;
import models.Quiz;
import models.Reponse;
import services.ServiceQuestion;
import services.ServiceQuiz;
import services.ServiceReponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PassageQuizController {

    // ⬇️ Composants FXML
    @FXML private Label  labelTitreQuiz;
    @FXML private Label  labelDescriptionQuiz;
    @FXML private VBox   listeQuestions;
    @FXML private Button btnSoumettre;

    // ⬇️ Services pour accéder à la base de données
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private final ServiceReponse  serviceReponse  = new ServiceReponse();
    private final ServiceQuiz     serviceQuiz     = new ServiceQuiz();

    // ⬇️ Identifiant du patient (à remplacer par la session utilisateur)
    private final int idPatient = 4;

    // ⬇️ Quiz en cours
    private Quiz quiz;

    // ⬇️ Map associant chaque idQuestion à un ToggleGroup pour la sélection des réponses
    private final Map<Integer, ToggleGroup> toggleGroups = new HashMap<>();


    // ⬇️ Définit le quiz à passer et initialise l'affichage des questions
    // Appelé depuis PasserTestsController après chargement du FXML
    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        labelTitreQuiz.setText(quiz.getTitre());
        labelDescriptionQuiz.setText(
                quiz.getDescription() != null ? quiz.getDescription() : ""
        );
        chargerQuestions();
    }

    // ⬇️ Charge toutes les questions du quiz depuis la base de données
    // Prépare les ToggleGroups pour permettre la sélection des réponses
    private void chargerQuestions() {
        try {
            List<Question> questions =
                    serviceQuestion.getQuestionsByQuizAvecChoix(quiz.getIdQuiz());

            // ⬇️ Vider l'affichage précédent et les groupes de toggles
            listeQuestions.getChildren().clear();
            toggleGroups.clear();

            // ⬇️ Créer une carte pour chaque question
            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                listeQuestions.getChildren().add(creerCarteQuestion(q, i + 1));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement questions : " + e.getMessage());
        }
    }

    // ⬇️ Crée une carte visuelle pour une question, avec ses choix radio
    // Chaque choix est associé à un ToggleGroup pour sélectionner la réponse
    private VBox creerCarteQuestion(Question question, int numero) {
        VBox carte = new VBox(14);
        carte.setPadding(new Insets(20));
        carte.setStyle("-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

        // ⬇️ Texte de la question
        Label texte = new Label(numero + ". " + question.getTexteQuestion());
        texte.setStyle("-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #2c3e50;" +
                "-fx-wrap-text: true;");
        texte.setWrapText(true);

        // ⬇️ Créer un ToggleGroup pour cette question
        ToggleGroup group = new ToggleGroup();
        toggleGroups.put(question.getIdQuestion(), group);

        VBox choixBox = new VBox(10);

        // ⬇️ Ajouter chaque choix de réponse à la carte
        List<Reponse> choix = question.getReponses();
        if (choix != null) {
            for (Reponse c : choix) {
                RadioButton rb = new RadioButton(c.getTexteReponse());
                rb.setToggleGroup(group);
                rb.setUserData(c); // stocker la Reponse dans le bouton
                rb.setStyle("-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #2c3e50;" +
                        "-fx-cursor: hand;");
                choixBox.getChildren().add(rb);
            }
        }

        carte.getChildren().addAll(texte, choixBox);
        return carte;
    }

    // ⬇️ Valide et soumet le test
    // Vérifie que toutes les questions ont une réponse,
    // enregistre les réponses en base et calcule le score
    @FXML
    private void soumettreTest() {
        // ⬇️ Vérifier que toutes les questions ont été répondues
        for (Map.Entry<Integer, ToggleGroup> entry : toggleGroups.entrySet()) {
            if (entry.getValue().getSelectedToggle() == null) {
                afficherAlerte("Attention",
                        "Veuillez répondre à toutes les questions avant de soumettre.");
                return;
            }
        }

        try {
            // ⬇️ Enregistrer chaque réponse en base
            for (Map.Entry<Integer, ToggleGroup> entry : toggleGroups.entrySet()) {
                int     idQuestion = entry.getKey();
                Toggle  selected   = entry.getValue().getSelectedToggle();
                Reponse choix      = (Reponse) selected.getUserData();

                Reponse reponsePatient = new Reponse(
                        quiz.getIdQuiz(),
                        idQuestion,
                        idPatient,
                        choix.getTexteReponse(),
                        choix.getValeur()
                );
                serviceReponse.add(reponsePatient);
            }

            // ⬇️ Calculer et sauvegarder le score final
            String resultat = serviceQuiz.calculerEtSauvegarderScore(
                    quiz.getIdQuiz(), idPatient
            );

            // ⬇️ Afficher le résultat dans une fenêtre
            afficherResultat(resultat);

        } catch (SQLException e) {
            System.err.println("❌ Erreur soumission : " + e.getMessage());
        }
    }

    // ⬇️ Affiche le résultat final du test
    private void afficherResultat(String resultat) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Résultat du test");
        alert.setHeaderText("✅ Test soumis avec succès !");
        alert.setContentText(resultat);
        alert.showAndWait();

        // ⬇️ Retourner à la liste après confirmation
        retourListe();
    }

    // ⬇️ Affiche une alerte warning avec un titre et un message
    private void afficherAlerte(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ⬇️ Retourne à la liste des tests disponibles
    @FXML
    private void retourListe() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/PasserTests.fxml")
            );
            Node vue = loader.load();

            VBox parent = (VBox) listeQuestions.getScene()
                    .lookup("#contentArea");
            if (parent != null) {
                parent.getChildren().setAll(vue);
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur retour liste : " + e.getMessage());
        }
    }
}
