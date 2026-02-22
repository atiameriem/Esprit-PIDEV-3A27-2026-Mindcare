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
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PassageQuizController {

    @FXML private Label  labelTitreQuiz;
    @FXML private Label  labelDescriptionQuiz;
    @FXML private VBox   listeQuestions;
    @FXML private Button btnSoumettre;

    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private final ServiceReponse  serviceReponse  = new ServiceReponse();
    private final ServiceQuiz     serviceQuiz     = new ServiceQuiz();


    private int idPatient = Session.getUserId() > 0 ? Session.getUserId() : 3;
    private Quiz quiz;
    private final Map<Integer, ToggleGroup> toggleGroups = new HashMap<>();

    // ══════════════════════════════════════════════════════════════
    // Appelé depuis PasserTestsController
    // ══════════════════════════════════════════════════════════════
    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        labelTitreQuiz.setText(quiz.getTitre());
        labelDescriptionQuiz.setText(
                quiz.getDescription() != null ? quiz.getDescription() : ""
        );
        chargerQuestions();
    }

    private void chargerQuestions() {
        try {
            List<Question> questions =
                    serviceQuestion.getQuestionsByQuizAvecChoix(quiz.getIdQuiz());
            listeQuestions.getChildren().clear();
            toggleGroups.clear();
            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                listeQuestions.getChildren().add(creerCarteQuestion(q, i + 1));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement questions : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Crée une carte question
    // ══════════════════════════════════════════════════════════════
    private VBox creerCarteQuestion(Question question, int numero) {
        VBox carte = new VBox(14);
        carte.setPadding(new Insets(20));
        carte.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);"
        );

        Label texte = new Label(numero + ". " + question.getTexteQuestion());
        texte.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #2c3e50;" +
                        "-fx-wrap-text: true;"
        );
        texte.setWrapText(true);

        ToggleGroup group = new ToggleGroup();
        toggleGroups.put(question.getIdQuestion(), group);

        VBox choixBox = new VBox(10);
        List<Reponse> choix = question.getReponses();
        if (choix != null) {
            for (Reponse c : choix) {
                RadioButton rb = new RadioButton(c.getTexteReponse());
                rb.setToggleGroup(group);
                rb.setUserData(c);
                rb.setStyle(
                        "-fx-font-size: 13px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-text-fill: #2c3e50;" +
                                "-fx-cursor: hand;"
                );
                choixBox.getChildren().add(rb);
            }
        }

        carte.getChildren().addAll(texte, choixBox);
        return carte;
    }

    // ══════════════════════════════════════════════════════════════
    // Soumettre le test
    // ══════════════════════════════════════════════════════════════
    @FXML
    private void soumettreTest() {
        for (Map.Entry<Integer, ToggleGroup> entry : toggleGroups.entrySet()) {
            if (entry.getValue().getSelectedToggle() == null) {
                afficherAlerte("Attention",
                        "Veuillez répondre à toutes les questions avant de soumettre.");
                return;
            }
        }

        try {
            int scoreTotal = 0;
            int nbQuestions = toggleGroups.size();

            for (Map.Entry<Integer, ToggleGroup> entry : toggleGroups.entrySet()) {
                int     idQuestion = entry.getKey();
                Toggle  selected   = entry.getValue().getSelectedToggle();
                Reponse choix      = (Reponse) selected.getUserData();
                scoreTotal        += choix.getValeur();

                Reponse reponsePatient = new Reponse(
                        quiz.getIdQuiz(),
                        idQuestion,
                        idPatient,
                        choix.getTexteReponse(),
                        choix.getValeur()
                );
                serviceReponse.add(reponsePatient);
            }

            String resultat = serviceQuiz.calculerEtSauvegarderScore(
                    quiz.getIdQuiz(), idPatient
            );

            afficherResultat(resultat, scoreTotal, nbQuestions);

        } catch (SQLException e) {
            System.err.println("❌ Erreur soumission : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Affichage résultat animé
    // ══════════════════════════════════════════════════════════════
    private void afficherResultat(String resultat, int score, int total) {

        int pourcentage = total > 0 ? (score * 100) / (total * 6) : 0;
        pourcentage = Math.min(100, Math.max(0, pourcentage));

        // ── Parser le résultat "Score: 15 | Niveau: élevé | Conseils: ..." ──
        String niveauTexte = "Inconnu";
        String conseilTexte = "";
        String scoreTexte = String.valueOf(score);

        try {
            String[] parties = resultat.split("\\|");
            for (String partie : parties) {
                partie = partie.trim();
                if (partie.startsWith("Score:"))
                    scoreTexte = partie.replace("Score:", "").trim();
                else if (partie.startsWith("Niveau:"))
                    niveauTexte = partie.replace("Niveau:", "").trim();
                else if (partie.startsWith("Conseils:"))
                    conseilTexte = partie.replace("Conseils:", "").trim();
            }
        } catch (Exception ignored) {}

        // ── Couleurs selon niveau ─────────────────────────────────
        String couleurScore, couleurBg, emoji, messageMotivation;
        if (pourcentage >= 70) {
            couleurScore = "#27ae60"; couleurBg = "#eafaf1";
            emoji = "🏆"; messageMotivation = "Excellent résultat !";
        } else if (pourcentage >= 40) {
            couleurScore = "#f39c12"; couleurBg = "#fef9e7";
            emoji = "⭐"; messageMotivation = "Bon effort, continue !";
        } else {
            couleurScore = "#e74c3c"; couleurBg = "#fdedec";
            emoji = "💪"; messageMotivation = "N'abandonne pas !";
        }

        final int    pct          = pourcentage;
        final String cFinal       = couleurScore;
        final String bgFinal      = couleurBg;
        final String niveauFinal  = niveauTexte;
        final String conseilFinal = conseilTexte;
        final String scoreFinal   = scoreTexte;

        // ══════════════════════════════════════════════════════════
        // ROOT
        // ══════════════════════════════════════════════════════════
        VBox root = new VBox(16);
        root.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        root.setPadding(new Insets(24, 20, 24, 20));
        root.setStyle("-fx-background-color: #dce8f0;");

        // ══════════════════════════════════════════════════════════
        // CARTE HERO (emoji + score + barre)
        // ══════════════════════════════════════════════════════════
        VBox carteHero = new VBox(14);
        carteHero.setAlignment(javafx.geometry.Pos.CENTER);
        carteHero.setPadding(new Insets(30, 30, 24, 30));
        carteHero.setMaxWidth(380);
        carteHero.setStyle(
                "-fx-background-color: #2c4a6e;" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(44,74,110,0.35), 18, 0, 0, 6);"
        );

        // Emoji cercle
        javafx.scene.layout.StackPane emojiPane = new javafx.scene.layout.StackPane();
        emojiPane.setMinSize(80, 80); emojiPane.setMaxSize(80, 80);
        emojiPane.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15);" +
                        "-fx-background-radius: 40;"
        );
        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 34px;");
        emojiPane.getChildren().add(lblEmoji);

        // Titre quiz
        Label lblNomQuiz = new Label(quiz.getTitre());
        lblNomQuiz.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-text-fill: rgba(255,255,255,0.7);" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-color: rgba(255,255,255,0.1);" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 4 14 4 14;"
        );

        // Grand score %
        Label lblPct = new Label(pct + "%");
        lblPct.setStyle(
                "-fx-font-size: 60px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: white;"
        );

        // Animation compteur
        javafx.animation.Timeline compteur = new javafx.animation.Timeline();
        for (int i = 0; i <= pct; i++) {
            final int val = i;
            compteur.getKeyFrames().add(new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(i * (1100.0 / Math.max(pct, 1))),
                    e -> lblPct.setText(val + "%")
            ));
        }

        // Message motivation
        Label lblMotiv = new Label(messageMotivation);
        lblMotiv.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: rgba(255,255,255,0.9);" +
                        "-fx-font-weight: bold;"
        );

        // Barre de progression
        javafx.scene.layout.StackPane barreContainer = new javafx.scene.layout.StackPane();
        barreContainer.setPrefWidth(280); barreContainer.setPrefHeight(10);
        barreContainer.setStyle(
                "-fx-background-color: rgba(255,255,255,0.2);" +
                        "-fx-background-radius: 5;"
        );
        javafx.scene.layout.Pane barreFill = new javafx.scene.layout.Pane();
        barreFill.setPrefHeight(10); barreFill.setPrefWidth(0);
        barreFill.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 5;"
        );
        javafx.scene.layout.StackPane.setAlignment(barreFill, javafx.geometry.Pos.CENTER_LEFT);
        barreContainer.getChildren().add(barreFill);

        javafx.animation.Timeline animBarre = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(barreFill.prefWidthProperty(), 0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(1100),
                        new javafx.animation.KeyValue(
                                barreFill.prefWidthProperty(),
                                280.0 * pct / 100.0,
                                javafx.animation.Interpolator.EASE_OUT
                        ))
        );

        carteHero.getChildren().addAll(emojiPane, lblNomQuiz, lblPct, lblMotiv, barreContainer);

        // ══════════════════════════════════════════════════════════
        // LIGNE : Score brut + Niveau (2 cartes côte à côte)
        // ══════════════════════════════════════════════════════════
        HBox ligneStats = new HBox(12);
        ligneStats.setMaxWidth(380);
        ligneStats.setAlignment(javafx.geometry.Pos.CENTER);

        // Carte Score
        VBox carteScore = creerCarteInfo(
                "🎯", "Score obtenu", scoreFinal + " pts",
                "#6c5ce7", "rgba(108,92,231,0.08)"
        );
        HBox.setHgrow(carteScore, javafx.scene.layout.Priority.ALWAYS);

        // Carte Niveau
        VBox carteNiveau = creerCarteInfo(
                "📊", "Niveau", niveauFinal,
                cFinal, bgFinal
        );
        HBox.setHgrow(carteNiveau, javafx.scene.layout.Priority.ALWAYS);

        ligneStats.getChildren().addAll(carteScore, carteNiveau);

        // ══════════════════════════════════════════════════════════
        // CARTE CONSEIL
        // ══════════════════════════════════════════════════════════
        VBox carteConseil = new VBox(10);
        carteConseil.setMaxWidth(380);
        carteConseil.setPadding(new Insets(18, 20, 18, 20));
        carteConseil.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);"
        );

        HBox titreConseil = new HBox(8);
        titreConseil.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label iconeConseil = new Label("💡");
        iconeConseil.setStyle("-fx-font-size: 16px;");
        Label lblTitreConseil = new Label("Conseil personnalisé");
        lblTitreConseil.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: #2c3e50;"
        );
        titreConseil.getChildren().addAll(iconeConseil, lblTitreConseil);

        Label lblConseil = new Label(
                conseilFinal.isEmpty() ? "Continue à pratiquer régulièrement pour améliorer tes résultats." : conseilFinal
        );
        lblConseil.setWrapText(true);
        lblConseil.setMaxWidth(340);
        lblConseil.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-text-fill: #7f8c8d;" +
                        "-fx-line-spacing: 3;"
        );

        carteConseil.getChildren().addAll(titreConseil, lblConseil);

        // ══════════════════════════════════════════════════════════
        // BOUTON RETOUR
        // ══════════════════════════════════════════════════════════
        Button btnOk = new Button("← Retour aux tests");
        btnOk.setMaxWidth(380);
        btnOk.setPrefHeight(46);
        btnOk.setStyle(
                "-fx-background-color: #2c4a6e;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 12;" +
                        "-fx-cursor: hand;"
        );
        btnOk.setOnMouseEntered(e -> btnOk.setStyle(
                "-fx-background-color: #1a3a5c;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 12;" +
                        "-fx-cursor: hand;"
        ));
        btnOk.setOnMouseExited(e -> btnOk.setStyle(
                "-fx-background-color: #2c4a6e;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 12;" +
                        "-fx-cursor: hand;"
        ));

        // Animation scale bouton
        javafx.animation.ScaleTransition scaleEmoji =
                new javafx.animation.ScaleTransition(javafx.util.Duration.millis(500), carteHero);
        scaleEmoji.setFromX(0.5); scaleEmoji.setFromY(0.5);
        scaleEmoji.setToX(1.0);   scaleEmoji.setToY(1.0);
        scaleEmoji.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        javafx.animation.FadeTransition fadeStats =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), ligneStats);
        fadeStats.setFromValue(0); fadeStats.setToValue(1);

        javafx.animation.FadeTransition fadeConseil =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), carteConseil);
        fadeConseil.setFromValue(0); fadeConseil.setToValue(1);

        root.getChildren().addAll(carteHero, ligneStats, carteConseil, btnOk);

        // ══════════════════════════════════════════════════════════
        // STAGE
        // ══════════════════════════════════════════════════════════
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #dce8f0; -fx-background-color: #dce8f0; -fx-border-color: transparent;");

        javafx.scene.Scene scene = new javafx.scene.Scene(scroll, 420, 580);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setScene(scene);
        stage.setTitle("Résultat du test");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        btnOk.setOnAction(e -> { stage.close(); retourListe(); });

        stage.setOnShown(e -> {
            scaleEmoji.play();
            javafx.animation.PauseTransition p1 =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
            p1.setOnFinished(ev -> { compteur.play(); animBarre.play(); });
            p1.play();
            javafx.animation.PauseTransition p2 =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
            p2.setOnFinished(ev -> { fadeStats.play(); fadeConseil.play(); });
            p2.play();
        });

        stage.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
// Méthode helper — crée une petite carte info
// ══════════════════════════════════════════════════════════════
    private VBox creerCarteInfo(String icone, String label, String valeur,
                                String couleur, String couleurBg) {
        VBox carte = new VBox(6);
        carte.setAlignment(javafx.geometry.Pos.CENTER);
        carte.setPadding(new Insets(16, 12, 16, 12));
        carte.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);"
        );

        javafx.scene.layout.StackPane iconePane = new javafx.scene.layout.StackPane();
        iconePane.setMinSize(44, 44); iconePane.setMaxSize(44, 44);
        iconePane.setStyle(
                "-fx-background-color: " + couleurBg + ";" +
                        "-fx-background-radius: 22;"
        );
        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size: 18px;");
        iconePane.getChildren().add(lblIcone);

        Label lblLabel = new Label(label);
        lblLabel.setStyle(
                "-fx-font-size: 10px;" +
                        "-fx-text-fill: #95a5a6;" +
                        "-fx-font-weight: bold;"
        );

        Label lblValeur = new Label(valeur);
        lblValeur.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: " + couleur + ";"
        );

        carte.getChildren().addAll(iconePane, lblLabel, lblValeur);
        return carte;
    }
    // ══════════════════════════════════════════════════════════════
    // Alerte warning
    // ══════════════════════════════════════════════════════════════
    private void afficherAlerte(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // Retour à la liste des tests
    // ══════════════════════════════════════════════════════════════
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