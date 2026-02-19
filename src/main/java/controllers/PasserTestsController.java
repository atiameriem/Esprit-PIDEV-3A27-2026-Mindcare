package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import models.Question;
import models.Quiz;
import services.ServiceQuestion;
import services.ServiceQuiz;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class PasserTestsController {

    // ⬇️ Conteneur FXML pour la liste des tests
    @FXML private VBox listeTests;

    // ⬇️ Bouton pour créer un nouveau test
    @FXML private Button btnNouveauTest;

    // ⬇️ Services pour accéder aux données des quiz et des questions
    private final ServiceQuiz     serviceQuiz     = new ServiceQuiz();
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();

    // ⬇️ Identifiant du psychologue (à remplacer par la session réelle)
    private final int idPsychologue = 6;

    // ⬇️ Initialisation automatique après chargement du FXML
    @FXML
    public void initialize() {
        chargerQuizzes(); // Charger tous les quiz disponibles
    }

    // ⬇️ Charge tous les quiz et crée leur carte dans l'interface
    private void chargerQuizzes() {
        try {
            List<Quiz> quizzes = serviceQuiz.getAll(); // Récupérer tous les quiz
            listeTests.getChildren().clear();          // Vider l'affichage précédent

            for (Quiz quiz : quizzes) {
                // Compter le nombre de questions pour chaque quiz
                int nbQuestions = serviceQuestion.countQuestionsByQuiz(quiz.getIdQuiz());
                // Ajouter la carte visuelle du quiz
                listeTests.getChildren().add(creerCarteQuiz(quiz, nbQuestions));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement quiz : " + e.getMessage());
        }
    }

    // ⬇️ Crée une carte visuelle pour un quiz avec icône, infos et boutons d'action
    private HBox creerCarteQuiz(Quiz quiz, int nbQuestions) {
        // Carte principale du quiz
        HBox carte = new HBox(16);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setPadding(new Insets(18, 20, 18, 20));
        carte.setStyle("-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

        // ── Icône cerveau pour le visuel
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(50, 50);
        iconBox.setMinSize(50, 50);
        iconBox.setStyle("-fx-background-color: #dce8f0; -fx-background-radius: 50;");
        Label icon = new Label("🧠");
        icon.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(icon);

        // ── Informations sur le quiz (titre, description, métadonnées)
        VBox infos = new VBox(4);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label titre = new Label(quiz.getTitre());
        titre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label description = new Label(quiz.getDescription() != null ? quiz.getDescription() : "");
        description.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        description.setWrapText(true);

        // ── Métadonnées : durée estimée et nombre de questions
        HBox meta = new HBox(16);
        meta.setAlignment(Pos.CENTER_LEFT);

        int dureeMin = Math.max(5, nbQuestions * 2); // 2 min par question, minimum 5 min
        Label duree = new Label("⏱ " + dureeMin + " min");
        duree.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        Label questions = new Label("📋 " + nbQuestions + " questions");
        questions.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        meta.getChildren().addAll(duree, questions);
        infos.getChildren().addAll(titre, description, meta);

        // ── Boutons d'action : Éditer, Supprimer, Passer
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // Bouton édition
        Button btnEdit = new Button("✏");
        btnEdit.setStyle("-fx-background-color: transparent;" +
                "-fx-font-size: 14px;" +
                "-fx-cursor: hand;" +
                "-fx-text-fill: #95a5a6;");
        btnEdit.setOnAction(e -> ouvrirEditionQuiz(quiz));

        // Bouton suppression
        Button btnDelete = new Button("🗑");
        btnDelete.setStyle("-fx-background-color: transparent;" +
                "-fx-font-size: 14px;" +
                "-fx-cursor: hand;" +
                "-fx-text-fill: #e74c3c;");
        btnDelete.setOnAction(e -> supprimerQuiz(quiz));

        // Bouton pour passer le quiz
        Button btnPasser = new Button("Passer  ›");
        btnPasser.setStyle("-fx-background-color: #2c4a6e;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20 10 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;");
        btnPasser.setOnAction(e -> passerQuiz(quiz));

        actions.getChildren().addAll(btnEdit, btnDelete, btnPasser);

        // ⬇️ Assemblage final de la carte
        carte.getChildren().addAll(iconBox, infos, actions);
        return carte;
    }

    // ⬇️ Ouvre l'interface pour passer le quiz
    private void passerQuiz(Quiz quiz) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/PassageQuiz.fxml")
            );
            Node vue = loader.load();

            // Passer le quiz au controller suivant
            PassageQuizController ctrl = loader.getController();
            ctrl.setQuiz(quiz);

            // Charger la vue dans le contentArea parent
            VBox parent = (VBox) listeTests.getScene().lookup("#contentArea");
            if (parent != null) {
                parent.getChildren().setAll(vue);
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur ouverture PassageQuiz : " + e.getMessage());
        }
    }

    // ⬇️ Ouvre l'interface pour créer ou éditer un quiz
    private void ouvrirEditionQuiz(Quiz quiz) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/NouveauTest.fxml")
            );
            Node vue = loader.load();

            // Passer le quiz au controller en mode édition
            NouveauTestController ctrl = loader.getController();
            ctrl.setQuizPourEdition(quiz); // Pré-remplissage automatique des champs

            VBox parent = (VBox) listeTests.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);

        } catch (IOException e) {
            System.err.println("❌ Erreur ouverture édition : " + e.getMessage());
        }
    }

    // ⬇️ Supprime un quiz et rafraîchit la liste
    private void supprimerQuiz(Quiz quiz) {
        try {
            serviceQuiz.delete(quiz);
            chargerQuizzes(); // Rafraîchir la liste après suppression
            System.out.println("🗑 Quiz supprimé : " + quiz.getTitre());
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression : " + e.getMessage());
        }
    }

    // ⬇️ Ouvre l'interface pour créer un nouveau test
    @FXML
    private void ouvrirNouveauTest() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/NouveauTest.fxml")
            );
            Node vue = loader.load();
            VBox parent = (VBox) listeTests.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (IOException e) {
            System.err.println("❌ Erreur ouverture NouveauTest : " + e.getMessage());
        }
    }
}
