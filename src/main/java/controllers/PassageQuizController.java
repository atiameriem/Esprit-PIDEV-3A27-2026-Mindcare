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

    // ✅ ID toujours depuis Session — défini dans setQuiz()
    private int  idPatient = -1;
    private Quiz quiz;
    private final Map<Integer, ToggleGroup> toggleGroups = new HashMap<>();

    // ══════════════════════════════════════════════════════════════
    // ✅ Appelé depuis PasserTestsController
    // ══════════════════════════════════════════════════════════════
    public void setQuiz(Quiz quiz) {
        this.idPatient = Session.getUserId();

        System.out.println("👤 PassageQuiz — patient ID=" + idPatient
                + " | quiz=" + quiz.getTitre());

        if (this.idPatient <= 0) {
            System.err.println("❌ Aucun patient connecté !");
            if (labelTitreQuiz != null)
                labelTitreQuiz.setText("⛔ Veuillez vous connecter.");
            return;
        }

        this.quiz = quiz;
        labelTitreQuiz.setText(quiz.getTitre());
        labelDescriptionQuiz.setText(
                quiz.getDescription() != null
                        ? quiz.getDescription() : "");
        chargerQuestions();
    }

    private void chargerQuestions() {
        try {
            List<Question> questions =
                    serviceQuestion.getQuestionsByQuizAvecChoix(
                            quiz.getIdQuiz());
            listeQuestions.getChildren().clear();
            toggleGroups.clear();
            for (int i = 0; i < questions.size(); i++) {
                listeQuestions.getChildren().add(
                        creerCarteQuestion(questions.get(i), i + 1));
            }
        } catch (SQLException e) {
            System.err.println("❌ Questions : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Crée une carte question
    // ══════════════════════════════════════════════════════════════
    private VBox creerCarteQuestion(Question question, int numero) {
        VBox carte = new VBox(14);
        carte.setPadding(new Insets(20));
        carte.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:12;" +
                        "-fx-effect:dropshadow(gaussian," +
                        "rgba(0,0,0,0.06),8,0,0,2);"
        );

        Label texte = new Label(
                numero + ". " + question.getTexteQuestion());
        texte.setStyle(
                "-fx-font-size:14px; -fx-font-weight:bold;" +
                        "-fx-text-fill:#2c3e50; -fx-wrap-text:true;"
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
                        "-fx-font-size:13px; -fx-font-weight:bold;" +
                                "-fx-text-fill:#2c3e50; -fx-cursor:hand;"
                );
                choixBox.getChildren().add(rb);
            }
        }

        carte.getChildren().addAll(texte, choixBox);
        return carte;
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ Soumettre le test
    // ══════════════════════════════════════════════════════════════
    @FXML
    private void soumettreTest() {
        if (idPatient <= 0) {
            afficherAlerte("Erreur", "Aucun patient connecté.");
            return;
        }

        for (Map.Entry<Integer, ToggleGroup> entry
                : toggleGroups.entrySet()) {
            if (entry.getValue().getSelectedToggle() == null) {
                afficherAlerte("Attention",
                        "Veuillez répondre à toutes les questions.");
                return;
            }
        }

        try {
            int scoreTotal  = 0;
            int nbQuestions = toggleGroups.size();

            for (Map.Entry<Integer, ToggleGroup> entry
                    : toggleGroups.entrySet()) {
                int     idQuestion = entry.getKey();
                Toggle  selected   = entry.getValue().getSelectedToggle();
                Reponse choix      = (Reponse) selected.getUserData();
                scoreTotal        += choix.getValeur();

                serviceReponse.add(new Reponse(
                        quiz.getIdQuiz(), idQuestion, idPatient,
                        choix.getTexteReponse(), choix.getValeur()
                ));
            }

            System.out.println("✅ Score soumis — patient ID="
                    + idPatient + " score=" + scoreTotal);

            String resultat = serviceQuiz.calculerEtSauvegarderScore(
                    quiz.getIdQuiz(), idPatient);

            // ✅ Rafraîchir les cercles dès la soumission
            SuivieController.rafraichir();

            afficherResultat(resultat, scoreTotal, nbQuestions);

        } catch (SQLException e) {
            System.err.println("❌ Soumission : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ Calcul pourcentage — max dynamique + inversion
    // ══════════════════════════════════════════════════════════════
    private int calculerPourcentage(int scoreTotal, int nbQuestions) {
        if (nbQuestions <= 0) return 0;

        int scoreMaxReel = 0;
        try {
            List<Question> questions =
                    serviceQuestion.getQuestionsByQuizAvecChoix(
                            quiz.getIdQuiz());
            for (Question q : questions) {
                if (q.getReponses() != null
                        && !q.getReponses().isEmpty()) {
                    scoreMaxReel += q.getReponses().stream()
                            .mapToInt(Reponse::getValeur)
                            .max().orElse(6);
                }
            }
        } catch (Exception e) {
            scoreMaxReel = nbQuestions * 6;
        }

        if (scoreMaxReel <= 0) scoreMaxReel = nbQuestions * 6;

        System.out.println("📊 score=" + scoreTotal
                + " / max=" + scoreMaxReel
                + " | quiz=" + quiz.getTitre());

        int pct = (int) Math.min(100,
                Math.max(0, (scoreTotal * 100.0) / scoreMaxReel));

        String titreLow = quiz.getTitre().toLowerCase();
        if (titreLow.contains("stress")
                || titreLow.contains("humeur")) {
            pct = 100 - pct;
        }

        System.out.println("📊 pct final=" + pct + "%");
        return pct;
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ Niveau depuis pourcentage corrigé
    // ══════════════════════════════════════════════════════════════
    private String calculerNiveau(int pct) {
        String titreLow = quiz.getTitre().toLowerCase();
        if (titreLow.contains("stress")
                || titreLow.contains("humeur")) {
            if      (pct >= 70) return "Bien géré";
            else if (pct >= 40) return "Modéré";
            else                return "Critique";
        } else {
            if      (pct >= 70) return "Excellent";
            else if (pct >= 40) return "Moyen";
            else                return "Faible";
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Affichage résultat animé
    // ══════════════════════════════════════════════════════════════
    private void afficherResultat(String resultat,
                                  int score,
                                  int nbQuestions) {

        int    pourcentage = calculerPourcentage(score, nbQuestions);
        String niveauTexte = calculerNiveau(pourcentage);
        String titreLow    = quiz.getTitre().toLowerCase();

        String scoreTexte = String.valueOf(score);
        try {
            for (String partie : resultat.split("\\|")) {
                partie = partie.trim();
                if (partie.startsWith("Score:"))
                    scoreTexte = partie.replace("Score:", "").trim();
            }
        } catch (Exception ignored) {}

        String couleurScore, couleurBg, emoji, messageMotivation;
        if (pourcentage >= 70) {
            couleurScore      = "#27ae60";
            couleurBg         = "#eafaf1";
            emoji             = "🏆";
            messageMotivation = "Excellent résultat ! 🎉";
        } else if (pourcentage >= 40) {
            couleurScore      = "#f39c12";
            couleurBg         = "#fef9e7";
            emoji             = "⭐";
            messageMotivation = "Bon effort, continue ! 💪";
        } else {
            couleurScore      = "#e74c3c";
            couleurBg         = "#fdedec";
            emoji             = "💪";
            messageMotivation = "N'abandonne pas ! 🌱";
        }

        final int    pct         = pourcentage;
        final String cFinal      = couleurScore;
        final String bgFinal     = couleurBg;
        final String niveauFinal = niveauTexte;
        final String scoreFinal  = scoreTexte;

        // ── ROOT ──────────────────────────────────────────────────
        VBox root = new VBox(16);
        root.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        root.setPadding(new Insets(24, 20, 24, 20));
        root.setStyle("-fx-background-color:#dce8f0;");

        // ── CARTE HERO ────────────────────────────────────────────
        VBox carteHero = new VBox(14);
        carteHero.setAlignment(javafx.geometry.Pos.CENTER);
        carteHero.setPadding(new Insets(30, 30, 24, 30));
        carteHero.setMaxWidth(380);
        carteHero.setStyle(
                "-fx-background-color:#2c4a6e;" +
                        "-fx-background-radius:20;" +
                        "-fx-effect:dropshadow(gaussian," +
                        "rgba(44,74,110,0.35),18,0,0,6);"
        );

        javafx.scene.layout.StackPane emojiPane =
                new javafx.scene.layout.StackPane();
        emojiPane.setMinSize(80, 80);
        emojiPane.setMaxSize(80, 80);
        emojiPane.setStyle(
                "-fx-background-color:rgba(255,255,255,0.15);" +
                        "-fx-background-radius:40;"
        );
        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size:34px;");
        emojiPane.getChildren().add(lblEmoji);

        Label lblNomQuiz = new Label(quiz.getTitre());
        lblNomQuiz.setStyle(
                "-fx-font-size:12px;" +
                        "-fx-text-fill:rgba(255,255,255,0.7);" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-color:rgba(255,255,255,0.1);" +
                        "-fx-background-radius:20;" +
                        "-fx-padding:4 14 4 14;"
        );

        Label lblPct = new Label("0%");
        lblPct.setStyle(
                "-fx-font-size:60px; -fx-font-weight:900;" +
                        "-fx-text-fill:white;"
        );

        javafx.animation.Timeline compteur =
                new javafx.animation.Timeline();
        for (int i = 0; i <= pct; i++) {
            final int val = i;
            compteur.getKeyFrames().add(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.millis(
                                    i * (1100.0 / Math.max(pct, 1))),
                            e -> lblPct.setText(val + "%")
                    )
            );
        }

        Label lblMotiv = new Label(messageMotivation);
        lblMotiv.setStyle(
                "-fx-font-size:14px;" +
                        "-fx-text-fill:rgba(255,255,255,0.9);" +
                        "-fx-font-weight:bold;"
        );

        javafx.scene.layout.StackPane barreContainer =
                new javafx.scene.layout.StackPane();
        barreContainer.setPrefWidth(280);
        barreContainer.setPrefHeight(10);
        barreContainer.setStyle(
                "-fx-background-color:rgba(255,255,255,0.2);" +
                        "-fx-background-radius:5;"
        );
        javafx.scene.layout.Pane barreFill =
                new javafx.scene.layout.Pane();
        barreFill.setPrefHeight(10);
        barreFill.setPrefWidth(0);
        String couleurBarre = pct >= 70 ? "#10B981"
                : pct >= 40 ? "#F59E0B" : "#EF4444";
        barreFill.setStyle(
                "-fx-background-color:" + couleurBarre + ";" +
                        "-fx-background-radius:5;"
        );
        javafx.scene.layout.StackPane.setAlignment(
                barreFill, javafx.geometry.Pos.CENTER_LEFT);
        barreContainer.getChildren().add(barreFill);

        javafx.animation.Timeline animBarre =
                new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(
                                javafx.util.Duration.ZERO,
                                new javafx.animation.KeyValue(
                                        barreFill.prefWidthProperty(), 0)),
                        new javafx.animation.KeyFrame(
                                javafx.util.Duration.millis(1100),
                                new javafx.animation.KeyValue(
                                        barreFill.prefWidthProperty(),
                                        280.0 * pct / 100.0,
                                        javafx.animation.Interpolator.EASE_OUT
                                ))
                );

        carteHero.getChildren().addAll(
                emojiPane, lblNomQuiz, lblPct, lblMotiv, barreContainer);

        // ── LIGNE STATS ───────────────────────────────────────────
        HBox ligneStats = new HBox(12);
        ligneStats.setMaxWidth(380);
        ligneStats.setAlignment(javafx.geometry.Pos.CENTER);

        VBox carteScore = creerCarteInfo(
                "🎯", "Score obtenu", scoreFinal + " pts",
                "#6c5ce7", "rgba(108,92,231,0.08)");
        HBox.setHgrow(carteScore, Priority.ALWAYS);

        VBox carteNiveau = creerCarteInfo(
                "📊", "Niveau", niveauFinal, cFinal, bgFinal);
        HBox.setHgrow(carteNiveau, Priority.ALWAYS);

        ligneStats.getChildren().addAll(carteScore, carteNiveau);

        // ── CARTE CONSEIL IA ──────────────────────────────────────
        VBox carteConseil = new VBox(14);
        carteConseil.setMaxWidth(380);
        carteConseil.setPadding(new Insets(20));
        carteConseil.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian," +
                        "rgba(0,0,0,0.06),10,0,0,3);"
        );

        HBox titreConseil = new HBox(8);
        titreConseil.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label iconeConseil = new Label("🧠");
        iconeConseil.setStyle("-fx-font-size:18px;");
        Label lblTitreConseil = new Label(
                "Analyse & Plan de traitement IA");
        lblTitreConseil.setStyle(
                "-fx-font-size:14px; -fx-font-weight:900;" +
                        "-fx-text-fill:#2c3e50;"
        );
        titreConseil.getChildren().addAll(iconeConseil, lblTitreConseil);

        Label lblConseilIA = new Label(
                "🤖 Génération de votre plan personnalisé...");
        lblConseilIA.setWrapText(true);
        lblConseilIA.setMaxWidth(340);
        lblConseilIA.setStyle(
                "-fx-font-size:12px; -fx-text-fill:#7f8c8d;" +
                        "-fx-font-style:italic;"
        );

        Separator sep = new Separator();

        VBox blocAnalyse    = creerBlocTraitement(
                "🔍", "Analyse psychologique",
                "Analyse en cours...", "#EDE9FE", "#7C3AED");
        VBox blocTraitement = creerBlocTraitement(
                "💊", "Plan de traitement",
                "Traitement en cours...", "#FEF3C7", "#D97706");
        VBox blocExercices  = creerBlocTraitement(
                "🏃", "Exercices recommandés",
                "Exercices en cours...", "#ECFDF5", "#059669");

        carteConseil.getChildren().addAll(
                titreConseil, lblConseilIA, sep,
                blocAnalyse, blocTraitement, blocExercices);

        // ✅ Appel IA en arrière-plan
        new Thread(() -> {
            try {
                int be = titreLow.contains("bien") ? pct
                        : titreLow.contains("stress") ? 100 - pct : 50;
                int st = titreLow.contains("stress") ? 100 - pct : 50;
                int hu = titreLow.contains("humeur") ? pct : 50;

                String promptAnalyse =
                        "Tu es un psychologue clinicien expert. "
                                + "Un patient  a passé "
                                + "le test '" + quiz.getTitre()
                                + "' et obtenu un score de " + pct + "%. "
                                + "Donne une analyse clinique précise en "
                                + "2-3 phrases sur son état psychologique. "
                                + "Sois bienveillant et professionnel. "
                                + "En français.";

                String promptTraitement =
                        "Tu es un psychologue clinicien expert. "
                                + "Un patient a obtenu " + pct + "% au test '"
                                + quiz.getTitre() + "'. "
                                + "Propose un plan de traitement concret : "
                                + "type de thérapie, fréquence des séances, "
                                + "techniques spécifiques (TCC, mindfulness, "
                                + "EMDR, etc.). 2-3 phrases précises. "
                                + "En français.";

                String promptExercices =
                        "Tu es un psychologue clinicien expert. "
                                + "Un patient a obtenu " + pct + "% au test '"
                                + quiz.getTitre() + "'. "
                                + "Propose exactement 3 exercices pratiques "
                                + "quotidiens avec durée et fréquence. "
                                + "Format : Nom (durée, fréquence). "
                                + "Séparés par des points. En français.";

                String analyse    = appelerGroqAvecPrompt(promptAnalyse);
                String traitement = appelerGroqAvecPrompt(promptTraitement);
                String exercices  = appelerGroqAvecPrompt(promptExercices);

                String analyseF = analyse != null ? analyse
                        : "Votre profil nécessite une attention "
                        + "particulière sur le plan psychologique.";
                String traitementF = traitement != null ? traitement
                        : "Une consultation avec un professionnel "
                        + "de santé mentale est recommandée.";
                String exercicesF = exercices != null ? exercices
                        : "Respiration 4-7-8 (5 min, matin). "
                        + "Méditation guidée (10 min, soir). "
                        + "Marche rapide (20 min, quotidien).";

                javafx.application.Platform.runLater(() -> {
                    lblConseilIA.setText(analyseF);
                    lblConseilIA.setStyle(
                            "-fx-font-size:12px;" +
                                    "-fx-text-fill:#374151;" +
                                    "-fx-font-style:normal;" +
                                    "-fx-line-spacing:3;"
                    );
                    mettreAJourBloc(blocAnalyse,    analyseF);
                    mettreAJourBloc(blocTraitement, traitementF);
                    mettreAJourBloc(blocExercices,  exercicesF);
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        lblConseilIA.setText(
                                "Continuez à pratiquer régulièrement "
                                        + "et consultez un professionnel si besoin.")
                );
            }
        }).start();

        // ── BOUTON RETOUR ─────────────────────────────────────────
        Button btnOk = new Button("← Retour aux tests");
        btnOk.setMaxWidth(380);
        btnOk.setPrefHeight(46);
        btnOk.setStyle(
                "-fx-background-color:#2c4a6e; -fx-text-fill:white;" +
                        "-fx-font-size:14px; -fx-font-weight:bold;" +
                        "-fx-background-radius:12; -fx-cursor:hand;"
        );
        btnOk.setOnMouseEntered(e -> btnOk.setStyle(
                "-fx-background-color:#1a3a5c; -fx-text-fill:white;" +
                        "-fx-font-size:14px; -fx-font-weight:bold;" +
                        "-fx-background-radius:12; -fx-cursor:hand;"
        ));
        btnOk.setOnMouseExited(e -> btnOk.setStyle(
                "-fx-background-color:#2c4a6e; -fx-text-fill:white;" +
                        "-fx-font-size:14px; -fx-font-weight:bold;" +
                        "-fx-background-radius:12; -fx-cursor:hand;"
        ));

        // ── ANIMATIONS ────────────────────────────────────────────
        javafx.animation.ScaleTransition scaleHero =
                new javafx.animation.ScaleTransition(
                        javafx.util.Duration.millis(500), carteHero);
        scaleHero.setFromX(0.5); scaleHero.setFromY(0.5);
        scaleHero.setToX(1.0);   scaleHero.setToY(1.0);
        scaleHero.setInterpolator(
                javafx.animation.Interpolator.EASE_OUT);

        javafx.animation.FadeTransition fadeStats =
                new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(600), ligneStats);
        fadeStats.setFromValue(0); fadeStats.setToValue(1);

        javafx.animation.FadeTransition fadeConseil =
                new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(600), carteConseil);
        fadeConseil.setFromValue(0); fadeConseil.setToValue(1);

        root.getChildren().addAll(
                carteHero, ligneStats, carteConseil, btnOk);

        // ── STAGE ─────────────────────────────────────────────────
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background:#dce8f0;" +
                        "-fx-background-color:#dce8f0;" +
                        "-fx-border-color:transparent;"
        );

        javafx.scene.Scene scene =
                new javafx.scene.Scene(scroll, 420, 600);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setScene(scene);
        stage.setTitle("Résultat du test");
        stage.initModality(
                javafx.stage.Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        // ✅ Rafraîchir les cercles au retour également
        btnOk.setOnAction(e -> {
            stage.close();
            SuivieController.rafraichir();
            retourListe();
        });

        stage.setOnShown(e -> {
            scaleHero.play();
            javafx.animation.PauseTransition p1 =
                    new javafx.animation.PauseTransition(
                            javafx.util.Duration.millis(200));
            p1.setOnFinished(ev -> {
                compteur.play();
                animBarre.play();
            });
            p1.play();
            javafx.animation.PauseTransition p2 =
                    new javafx.animation.PauseTransition(
                            javafx.util.Duration.millis(400));
            p2.setOnFinished(ev -> {
                fadeStats.play();
                fadeConseil.play();
            });
            p2.play();
        });

        stage.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════
    private VBox creerBlocTraitement(String icone, String titre,
                                     String contenu,
                                     String couleurBg,
                                     String couleurBord) {
        VBox bloc = new VBox(6);
        bloc.setPadding(new Insets(12, 14, 12, 14));
        bloc.setStyle(
                "-fx-background-color:" + couleurBg + ";" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:" + couleurBord + ";" +
                        "-fx-border-radius:12;" +
                        "-fx-border-width:1.5;"
        );
        HBox entete = new HBox(8);
        entete.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size:14px;");
        Label lblTitre = new Label(titre);
        lblTitre.setStyle(
                "-fx-font-size:12px; -fx-font-weight:900;" +
                        "-fx-text-fill:" + couleurBord + ";");
        entete.getChildren().addAll(lblIcone, lblTitre);

        Label lblContenu = new Label(contenu);
        lblContenu.setWrapText(true);
        lblContenu.setMaxWidth(320);
        lblContenu.setStyle(
                "-fx-font-size:11px; -fx-text-fill:#374151;" +
                        "-fx-line-spacing:3;");
        bloc.setUserData(lblContenu);
        bloc.getChildren().addAll(entete, lblContenu);
        return bloc;
    }

    private void mettreAJourBloc(VBox bloc, String contenu) {
        if (bloc.getUserData() instanceof Label)
            ((Label) bloc.getUserData()).setText(contenu);
    }

    private VBox creerCarteInfo(String icone, String label,
                                String valeur, String couleur,
                                String couleurBg) {
        VBox carte = new VBox(6);
        carte.setAlignment(javafx.geometry.Pos.CENTER);
        carte.setPadding(new Insets(16, 12, 16, 12));
        carte.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian," +
                        "rgba(0,0,0,0.06),8,0,0,2);"
        );
        javafx.scene.layout.StackPane iconePane =
                new javafx.scene.layout.StackPane();
        iconePane.setMinSize(44, 44);
        iconePane.setMaxSize(44, 44);
        iconePane.setStyle(
                "-fx-background-color:" + couleurBg + ";" +
                        "-fx-background-radius:22;");
        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size:18px;");
        iconePane.getChildren().add(lblIcone);

        Label lblLabel = new Label(label);
        lblLabel.setStyle(
                "-fx-font-size:10px; -fx-text-fill:#95a5a6;" +
                        "-fx-font-weight:bold;");

        Label lblValeur = new Label(valeur);
        lblValeur.setStyle(
                "-fx-font-size:15px; -fx-font-weight:900;" +
                        "-fx-text-fill:" + couleur + ";");

        carte.getChildren().addAll(iconePane, lblLabel, lblValeur);
        return carte;
    }

    private String appelerGroqAvecPrompt(String prompt) {
        try {
            java.net.URL url = new java.net.URL(
                    "");
            java.net.HttpURLConnection conn =
                    (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization",
                    "");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            String promptEscape = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .replace("\r", " ");

            String corps = "{\"model\":\"llama-3.3-70b-versatile\","
                    + "\"messages\":[{\"role\":\"user\","
                    + "\"content\":\"" + promptEscape + "\"}],"
                    + "\"max_tokens\":200,\"temperature\":0.7}";

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(corps.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader br =
                             new java.io.BufferedReader(
                                     new java.io.InputStreamReader(
                                             conn.getInputStream(),
                                             java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null)
                        sb.append(line);
                }
                String json  = sb.toString();
                int    debut = json.indexOf("\"content\":\"") + 11;
                int    fin   = debut;
                while (fin < json.length()) {
                    if (json.charAt(fin) == '"'
                            && json.charAt(fin - 1) != '\\') break;
                    fin++;
                }
                return json.substring(debut, fin)
                        .replace("\\n", "\n")
                        .replace("\\t", " ")
                        .trim();
            }
        } catch (Exception e) {
            System.err.println("❌ Groq : " + e.getMessage());
        }
        return null;
    }

    private void afficherAlerte(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void retourListe() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/PasserTests.fxml"));
            Node vue = loader.load();
            VBox parent = (VBox) listeQuestions.getScene()
                    .lookup("#contentArea");
            if (parent != null)
                parent.getChildren().setAll(vue);
        } catch (IOException e) {
            System.err.println("❌ Retour : " + e.getMessage());
        }

    }

}
