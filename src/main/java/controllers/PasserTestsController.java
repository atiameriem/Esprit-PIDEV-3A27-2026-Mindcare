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
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class PasserTestsController {

    @FXML private VBox listeTests;
    @FXML private Button btnNouveauTest;

    private final ServiceQuiz     serviceQuiz     = new ServiceQuiz();
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();

    // ← à remplacer par la session utilisateur
    private final int idPsychologue = 6;

    @FXML
    public void initialize() {
        // ✅ Boutons Modifier/Supprimer/Nouveau — cachés pour les patients
        var role = Session.getRoleConnecte();
        boolean estPsychologue = (role == utils.Session.Role.PSYCHOLOGUE
                || role == utils.Session.Role.ADMIN);

        if (btnNouveauTest != null) {
            btnNouveauTest.setVisible(estPsychologue);
            btnNouveauTest.setManaged(estPsychologue);
        }

        chargerQuizzes();
    }

    private void chargerQuizzes() {
        try {
            List<Quiz> quizzes = serviceQuiz.getAll();
            listeTests.getChildren().clear();

            for (Quiz quiz : quizzes) {
                int nbQuestions = serviceQuestion.countQuestionsByQuiz(quiz.getIdQuiz());
                listeTests.getChildren().add(creerCarteQuiz(quiz, nbQuestions));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement quiz : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
// Crée une carte quiz
// ══════════════════════════════════════════════════════════════
    private HBox creerCarteQuiz(Quiz quiz, int nbQuestions) {

        var role = Session.getRoleConnecte();
        boolean estPsychologue = (role == utils.Session.Role.PSYCHOLOGUE
                || role == utils.Session.Role.ADMIN);

        // ── Carte principale ───────────────────────────────────────
        HBox carte = new HBox(16);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setPadding(new Insets(18, 20, 18, 20));
        carte.setStyle("-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

        // ── Icône cerveau ──────────────────────────────────────────
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(50, 50);
        iconBox.setMinSize(50, 50);
        iconBox.setStyle("-fx-background-color: #dce8f0; -fx-background-radius: 50;");

        Label icon = new Label("🧠");
        icon.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(icon);

        // ── Infos quiz ─────────────────────────────────────────────
        VBox infos = new VBox(4);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label titre = new Label(quiz.getTitre());
        titre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label description = new Label(
                quiz.getDescription() != null ? quiz.getDescription() : "");
        description.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        description.setWrapText(true);

        // ── Meta (durée + questions) ──────────────────────────────
        HBox meta = new HBox(16);
        meta.setAlignment(Pos.CENTER_LEFT);

        int dureeMin = Math.max(5, nbQuestions * 2);

        Label duree = new Label("⏱ " + dureeMin + " min");
        duree.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        Label questions = new Label("📋 " + nbQuestions + " questions");
        questions.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        meta.getChildren().addAll(duree, questions);
        infos.getChildren().addAll(titre, description, meta);

        // ── Boutons action ─────────────────────────────────────────
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnPasser = new Button("Passer  ›");
        btnPasser.setStyle("-fx-background-color: #2c4a6e;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20 10 20;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;");
        btnPasser.setOnAction(e -> passerQuiz(quiz));

        // Boutons visibles uniquement pour PSYCHOLOGUE et ADMIN
        if (estPsychologue) {

            Button btnEdit = new Button("✏");
            btnEdit.setStyle("-fx-background-color: transparent;" +
                    "-fx-font-size: 14px;" +
                    "-fx-cursor: hand;" +
                    "-fx-text-fill: #95a5a6;");
            btnEdit.setOnAction(e -> ouvrirEditionQuiz(quiz));

            Button btnDelete = new Button("🗑");
            btnDelete.setStyle("-fx-background-color: transparent;" +
                    "-fx-font-size: 14px;" +
                    "-fx-cursor: hand;" +
                    "-fx-text-fill: #e74c3c;");
            btnDelete.setOnAction(e -> supprimerQuiz(quiz));

            actions.getChildren().addAll(btnEdit, btnDelete);
        }

        actions.getChildren().add(btnPasser);

        // ── Assemblage final ───────────────────────────────────────
        carte.getChildren().addAll(iconBox, infos, actions);

        return carte;
    }
    // ══════════════════════════════════════════════════════════════
    // Actions
    // ══════════════════════════════════════════════════════════════

    private void passerQuiz(Quiz quiz) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/PassageQuiz.fxml")
            );
            Node vue = loader.load();

            // Passer le quiz au controller suivant
            PassageQuizController ctrl = loader.getController();
            ctrl.setQuiz(quiz);

            // Charger dans le contentArea parent
            VBox parent = (VBox) listeTests.getScene()
                    .lookup("#contentArea");
            if (parent != null) {
                parent.getChildren().setAll(vue);
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur ouverture PassageQuiz : " + e.getMessage());
        }
    }

    private void ouvrirEditionQuiz(Quiz quiz) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/NouveauTest.fxml")
            );
            Node vue = loader.load();

            // Passer le quiz au controller en mode édition
            NouveauTestController ctrl = loader.getController();
            ctrl.setQuizPourEdition(quiz); // ← pré-remplissage automatique

            VBox parent = (VBox) listeTests.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);

        } catch (IOException e) {
            System.err.println("❌ Erreur ouverture édition : " + e.getMessage());
        }
    }

    private void supprimerQuiz(Quiz quiz) {
        try {
            serviceQuiz.delete(quiz);
            chargerQuizzes(); // rafraîchir la liste
            System.out.println("🗑 Quiz supprimé : " + quiz.getTitre());
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression : " + e.getMessage());
        }
    }

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