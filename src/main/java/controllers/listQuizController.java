package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import models.Quiz;
import services.ServiceQuestion;
import services.ServiceQuiz;
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class listQuizController {

    @FXML private VBox   listeTests;
    @FXML private Button btnNouveauTest;

    private final ServiceQuiz     serviceQuiz     = new ServiceQuiz();
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();

    private int idUtilisateur = -1;

    @FXML
    public void initialize() {
        idUtilisateur = Session.getUserId();
        Session.Role role = Session.getRoleConnecte();

        System.out.println("👤 listQuiz — ID=" + idUtilisateur + " | role=" + role);

        // ✅ Rôle non autorisé → cacher tout
        if (role != Session.Role.USER
                && role != Session.Role.PSYCHOLOGUE
                && role != Session.Role.ADMIN) {
            if (listeTests != null) {
                listeTests.getChildren().clear();
                listeTests.setVisible(false);
                listeTests.setManaged(false);
            }
            if (btnNouveauTest != null) {
                btnNouveauTest.setVisible(false);
                btnNouveauTest.setManaged(false);
            }
            return;
        }

        boolean estPsychologue = (role == Session.Role.PSYCHOLOGUE
                || role == Session.Role.ADMIN);

        // ✅ Bouton "Nouveau test" : visible seulement psychologue/admin
        if (btnNouveauTest != null) {
            btnNouveauTest.setVisible(estPsychologue);
            btnNouveauTest.setManaged(estPsychologue);
        }

        chargerQuizzes();
    }

    private void chargerQuizzes() {
        try {
            Session.Role role = Session.getRoleConnecte();
            int userId = Session.getUserId();
            List<Quiz> quizzes;

            if (role == Session.Role.USER) {
                // ✅ Patient → voit les quiz globaux (id_users = 0)
                //              + les quiz qui lui sont assignés (id_users = son id)
                quizzes = serviceQuiz.getQuizDisponiblesPatient(userId);

                // ✅ Si toujours vide, fallback → tous les quiz actifs
                if (quizzes == null || quizzes.isEmpty()) {
                    System.out.println("⚠️ getQuizDisponiblesPatient vide → fallback read()");
                    quizzes = serviceQuiz.read();
                }
            } else {
                quizzes = serviceQuiz.read();
            }

            System.out.println("📋 Quiz chargés : " + (quizzes != null ? quizzes.size() : 0));

            listeTests.getChildren().clear();

            if (quizzes == null || quizzes.isEmpty()) {
                Label vide = new Label("Aucun test disponible pour le moment.");
                vide.setStyle("-fx-font-size:14px; -fx-text-fill:#6E8E9A; -fx-padding:20;");
                listeTests.getChildren().add(vide);
                return;
            }

            for (Quiz quiz : quizzes) {
                int nbQ = serviceQuestion.countQuestionsByQuiz(quiz.getIdQuiz());
                listeTests.getChildren().add(creerCarteQuiz(quiz, nbQ));
            }

        } catch (SQLException e) {
            System.err.println("❌ Chargement quiz : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private HBox creerCarteQuiz(Quiz quiz, int nbQuestions) {
        var role = Session.getRoleConnecte();

        boolean estPsychologue = (role == Session.Role.PSYCHOLOGUE || role == Session.Role.ADMIN);
        boolean estPatient     = (role == Session.Role.USER);

        HBox carte = new HBox(16);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setPadding(new Insets(18, 20, 18, 20));
        carte.setStyle(
                "-fx-background-color: rgba(255,255,255,0.78);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: rgba(92,152,168,0.14);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(92,152,168,0.09), 10, 0, 0, 2);"
        );

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(50, 50); iconBox.setMinSize(50, 50);
        iconBox.setStyle(
                "-fx-background-color: rgba(92,152,168,0.15);" +
                        "-fx-background-radius: 50;");
        Label icon = new Label("🧠");
        icon.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(icon);

        VBox infos = new VBox(4);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label titre = new Label(quiz.getTitre());
        titre.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #1F2A33;");

        Label description = new Label(quiz.getDescription() != null ? quiz.getDescription() : "");
        description.setStyle("-fx-font-size: 12px; -fx-text-fill: #6E8E9A; -fx-font-weight: 600;");
        description.setWrapText(true);

        HBox meta = new HBox(16);
        meta.setAlignment(Pos.CENTER_LEFT);
        int dureeMin = Math.max(5, nbQuestions * 2);

        Label duree     = new Label("⏱ " + dureeMin + " min");
        duree.setStyle("-fx-font-size: 11px; -fx-text-fill: #8AA8B2;");
        Label questions = new Label("📋 " + nbQuestions + " questions");
        questions.setStyle("-fx-font-size: 11px; -fx-text-fill: #8AA8B2;");

        meta.getChildren().addAll(duree, questions);
        infos.getChildren().addAll(titre, description, meta);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        if (estPsychologue) {
            Button btnEdit = new Button("✏");
            btnEdit.setStyle(
                    "-fx-background-color: rgba(92,152,168,0.10);" +
                            "-fx-font-size: 13px; -fx-cursor: hand; -fx-text-fill: #5C98A8;" +
                            "-fx-background-radius: 8; -fx-border-color: rgba(92,152,168,0.25);" +
                            "-fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 6 10;");
            btnEdit.setOnAction(e -> ouvrirEditionQuiz(quiz));

            Button btnDelete = new Button("🗑");
            btnDelete.setStyle(
                    "-fx-background-color: rgba(239,68,68,0.08);" +
                            "-fx-font-size: 13px; -fx-cursor: hand; -fx-text-fill: #EF4444;" +
                            "-fx-background-radius: 8; -fx-border-color: rgba(239,68,68,0.20);" +
                            "-fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 6 10;");
            // ✅ delete(int id) au lieu de delete(Quiz quiz)
            btnDelete.setOnAction(e -> supprimerQuiz(quiz));

            actions.getChildren().addAll(btnEdit, btnDelete);

        } else if (estPatient) {
            Button btnPasser = new Button("Passer  ›");
            btnPasser.setStyle(
                    "-fx-background-color: #5C98A8; -fx-text-fill: white;" +
                            "-fx-font-size: 12px; -fx-font-weight: 800;" +
                            "-fx-padding: 9 20 9 20; -fx-background-radius: 12;" +
                            "-fx-border-color: transparent; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(92,152,168,0.30), 8, 0, 0, 2);");
            btnPasser.setOnAction(e -> passerQuiz(quiz));
            actions.getChildren().add(btnPasser);
        }

        carte.getChildren().addAll(iconBox, infos, actions);
        return carte;
    }

    private void passerQuiz(Quiz quiz) {
        if (Session.getUserId() <= 0) {
            System.err.println("❌ Aucun utilisateur connecté !");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PassageQuiz.fxml"));
            Node vue = loader.load();
            PasserTestsController ctrl = loader.getController();
            ctrl.setQuiz(quiz);
            VBox parent = (VBox) listeTests.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (IOException e) {
            System.err.println("❌ PassageQuiz : " + e.getMessage());
        }
    }

    private void ouvrirEditionQuiz(Quiz quiz) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NouveauTestQuiz.fxml"));
            Node vue = loader.load();
            NouveauTestQuizController ctrl = loader.getController();
            ctrl.setQuizPourEdition(quiz);
            VBox parent = (VBox) listeTests.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (IOException e) {
            System.err.println("❌ Edition : " + e.getMessage());
        }
    }

    private void supprimerQuiz(Quiz quiz) {
        try {
            // ✅ delete(int id) au lieu de delete(Quiz quiz)
            serviceQuiz.delete(quiz.getIdQuiz());
            chargerQuizzes();
            System.out.println("🗑 Supprimé : " + quiz.getTitre());
        } catch (SQLException e) {
            System.err.println("❌ Suppression : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirNouveauTest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NouveauTestQuiz.fxml"));
            Node vue = loader.load();
            VBox parent = (VBox) listeTests.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (IOException e) {
            System.err.println("❌ NouveauTest : " + e.getMessage());
        }
    }
}