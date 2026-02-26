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
import services.ResultFusionService;
import services.ServiceGroq;
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
    private final ServiceGroq         serviceGroq   = new ServiceGroq();
    // ✅ ResultFusionService — HuggingFace NLP + Groq enrichi
    private final ResultFusionService fusionService = new ResultFusionService();

    private int  idPatient = -1;
    private Quiz quiz;
    private final Map<Integer, ToggleGroup> toggleGroups = new HashMap<>();

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
                quiz.getDescription() != null ? quiz.getDescription() : "");
        chargerQuestions();
    }

    private void chargerQuestions() {
        try {
            List<Question> questions =
                    serviceQuestion.getQuestionsByQuizAvecChoix(quiz.getIdQuiz());
            listeQuestions.getChildren().clear();
            toggleGroups.clear();
            for (int i = 0; i < questions.size(); i++)
                listeQuestions.getChildren().add(
                        creerCarteQuestion(questions.get(i), i + 1));
        } catch (SQLException e) {
            System.err.println("❌ Questions : " + e.getMessage());
        }
    }

    private VBox creerCarteQuestion(Question question, int numero) {
        VBox carte = new VBox(14);
        carte.setPadding(new Insets(20));
        carte.setStyle("-fx-background-color:white;"
                + "-fx-background-radius:12;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        Label texte = new Label(numero + ". " + question.getTexteQuestion());
        texte.setStyle("-fx-font-size:14px; -fx-font-weight:bold;"
                + "-fx-text-fill:#2c3e50; -fx-wrap-text:true;");
        texte.setWrapText(true);
        ToggleGroup group = new ToggleGroup();
        toggleGroups.put(question.getIdQuestion(), group);
        VBox choixBox = new VBox(10);
        List<Reponse> choix = question.getReponses();
        if (choix != null) {
            for (Reponse c : choix) {
                RadioButton rb = new RadioButton(c.getTexteReponse());
                rb.setToggleGroup(group); rb.setUserData(c);
                rb.setStyle("-fx-font-size:13px; -fx-font-weight:bold;"
                        + "-fx-text-fill:#2c3e50; -fx-cursor:hand;");
                choixBox.getChildren().add(rb);
            }
        }
        carte.getChildren().addAll(texte, choixBox);
        return carte;
    }

    @FXML
    private void soumettreTest() {
        if (idPatient <= 0) {
            afficherAlerte("Erreur", "Aucun patient connecté."); return;
        }
        for (Map.Entry<Integer, ToggleGroup> entry : toggleGroups.entrySet()) {
            if (entry.getValue().getSelectedToggle() == null) {
                afficherAlerte("Attention", "Veuillez répondre à toutes les questions.");
                return;
            }
        }
        try {
            int scoreTotal  = 0;
            int nbQuestions = toggleGroups.size();
            for (Map.Entry<Integer, ToggleGroup> entry : toggleGroups.entrySet()) {
                int     idQuestion = entry.getKey();
                Toggle  selected   = entry.getValue().getSelectedToggle();
                Reponse choix      = (Reponse) selected.getUserData();
                scoreTotal += choix.getValeur();
                serviceReponse.add(new Reponse(
                        quiz.getIdQuiz(), idQuestion, idPatient,
                        choix.getTexteReponse(), choix.getValeur()));
            }
            System.out.println("✅ Score soumis — patient ID="
                    + idPatient + " score=" + scoreTotal);
            String resultat = serviceQuiz.calculerEtSauvegarderScore(
                    quiz.getIdQuiz(), idPatient);
            SuivieController.rafraichir();
            afficherResultat(resultat, scoreTotal, nbQuestions);
        } catch (SQLException e) {
            System.err.println("❌ Soumission : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ CALCUL MAX RÉEL depuis les réponses de la DB
    // ══════════════════════════════════════════════════════════════
    private int calculerScoreMax(int nbQuestionsDefault) {
        int scoreMaxReel = 0;
        try {
            List<Question> questions =
                    serviceQuestion.getQuestionsByQuizAvecChoix(quiz.getIdQuiz());
            for (Question q : questions) {
                if (q.getReponses() != null && !q.getReponses().isEmpty()) {
                    scoreMaxReel += q.getReponses().stream()
                            .mapToInt(Reponse::getValeur).max().orElse(6);
                }
            }
        } catch (Exception e) {
            scoreMaxReel = nbQuestionsDefault * 6;
        }
        return scoreMaxReel > 0 ? scoreMaxReel : nbQuestionsDefault * 6;
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ DOUBLE VARIABLE — logique complète
    //
    //  pctReel      = score brut / max   (jamais inversé)
    //                 → utilisé par IA + niveau (logique clinique)
    //
    //  pctAffichage = inversion si stress
    //                 → utilisé par UI (barre, couleur, emoji)
    //
    //  Exemple stress élevé (score=80%) :
    //    pctReel      = 80%  → Niveau "Critique"  ✅
    //    pctAffichage = 20%  → barre rouge        ✅
    //    IA = "stress élevé, difficultés présentes" ✅
    // ══════════════════════════════════════════════════════════════
    private void afficherResultat(String resultat, int score, int nbQuestions) {

        int scoreMax    = calculerScoreMax(nbQuestions);
        String titreLow = quiz.getTitre().toLowerCase();

        // ✅ Grand score = bien pour TOUS les tests → pas d'inversion
        int pctReel = (int) Math.min(100, Math.max(0, (score * 100.0) / scoreMax));
        int pctAffichage = pctReel;

        System.out.println("📊 score=" + score + " / max=" + scoreMax
                + " | pct=" + pctReel + "% | quiz=" + quiz.getTitre());

        // ── Niveau basé sur pctReel (logique clinique) ─────────────
        String niveau = calculerNiveau(pctReel, titreLow);

        // ── Score affiché ───────────────────────────────────────────
        String scoreTexte = String.valueOf(score);
        try {
            for (String p : resultat.split("\\|")) {
                p = p.trim();
                if (p.startsWith("Score:"))
                    scoreTexte = p.replace("Score:", "").trim();
            }
        } catch (Exception ignored) {}

        // ── Couleurs/emoji basés sur pctAffichage (visuel) ─────────
        String couleurScore, couleurBg, emoji, messageMotiv;
        if (pctAffichage >= 70) {
            couleurScore = "#27ae60"; couleurBg = "#eafaf1";
            emoji = "🏆"; messageMotiv = "Excellent résultat ! 🎉";
        } else if (pctAffichage >= 40) {
            couleurScore = "#f39c12"; couleurBg = "#fef9e7";
            emoji = "⭐"; messageMotiv = "Bon effort, continue ! 💪";
        } else {
            couleurScore = "#e74c3c"; couleurBg = "#fdedec";
            emoji = "💪"; messageMotiv = "N'abandonne pas ! 🌱";
        }

        final int    pctDisplay = pctAffichage;
        final int    pctIA      = pctReel;       // ← IA voit le vrai score
        final String cF         = couleurScore;
        final String bgF        = couleurBg;
        final String niveauF    = niveau;
        final String scoreF     = scoreTexte;

        // ── ROOT ───────────────────────────────────────────────────
        VBox root = new VBox(16);
        root.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        root.setPadding(new Insets(24, 20, 24, 20));
        root.setStyle("-fx-background-color:#dce8f0;");

        // ── CARTE HERO ─────────────────────────────────────────────
        VBox carteHero = new VBox(14);
        carteHero.setAlignment(javafx.geometry.Pos.CENTER);
        carteHero.setPadding(new Insets(30, 30, 24, 30));
        carteHero.setMaxWidth(380);
        carteHero.setStyle("-fx-background-color:#2c4a6e;"
                + "-fx-background-radius:20;"
                + "-fx-effect:dropshadow(gaussian,rgba(44,74,110,0.35),18,0,0,6);");

        javafx.scene.layout.StackPane emojiPane = new javafx.scene.layout.StackPane();
        emojiPane.setMinSize(80, 80); emojiPane.setMaxSize(80, 80);
        emojiPane.setStyle("-fx-background-color:rgba(255,255,255,0.15); -fx-background-radius:40;");
        Label lblEmoji = new Label(emoji); lblEmoji.setStyle("-fx-font-size:34px;");
        emojiPane.getChildren().add(lblEmoji);

        Label lblNomQuiz = new Label(quiz.getTitre());
        lblNomQuiz.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(255,255,255,0.7);"
                + "-fx-font-weight:bold; -fx-background-color:rgba(255,255,255,0.1);"
                + "-fx-background-radius:20; -fx-padding:4 14 4 14;");

        // ✅ Affiche pctAffichage (rouge pour stress élevé)
        Label lblPct = new Label("0%");
        lblPct.setStyle("-fx-font-size:60px; -fx-font-weight:900; -fx-text-fill:white;");

        javafx.animation.Timeline compteur = new javafx.animation.Timeline();
        for (int i = 0; i <= pctDisplay; i++) {
            final int val = i;
            compteur.getKeyFrames().add(new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(i * (1100.0 / Math.max(pctDisplay, 1))),
                    e -> lblPct.setText(val + "%")));
        }

        Label lblMotiv = new Label(messageMotiv);
        lblMotiv.setStyle("-fx-font-size:14px; -fx-text-fill:rgba(255,255,255,0.9); -fx-font-weight:bold;");

        javafx.scene.layout.StackPane barreContainer = new javafx.scene.layout.StackPane();
        barreContainer.setPrefWidth(280); barreContainer.setPrefHeight(10);
        barreContainer.setStyle("-fx-background-color:rgba(255,255,255,0.2); -fx-background-radius:5;");
        javafx.scene.layout.Pane barreFill = new javafx.scene.layout.Pane();
        barreFill.setPrefHeight(10); barreFill.setPrefWidth(0);
        // ✅ Couleur barre basée sur pctAffichage
        String couleurBarre = pctDisplay >= 70 ? "#10B981" : pctDisplay >= 40 ? "#F59E0B" : "#EF4444";
        barreFill.setStyle("-fx-background-color:" + couleurBarre + "; -fx-background-radius:5;");
        javafx.scene.layout.StackPane.setAlignment(barreFill, javafx.geometry.Pos.CENTER_LEFT);
        barreContainer.getChildren().add(barreFill);

        javafx.animation.Timeline animBarre = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(barreFill.prefWidthProperty(), 0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(1100),
                        new javafx.animation.KeyValue(barreFill.prefWidthProperty(),
                                280.0 * pctDisplay / 100.0,
                                javafx.animation.Interpolator.EASE_OUT)));

        carteHero.getChildren().addAll(emojiPane, lblNomQuiz, lblPct, lblMotiv, barreContainer);

        // ── STATS ──────────────────────────────────────────────────
        HBox ligneStats = new HBox(12);
        ligneStats.setMaxWidth(380); ligneStats.setAlignment(javafx.geometry.Pos.CENTER);
        VBox carteScore = creerCarteInfo("🎯", "Score obtenu", scoreF + " pts",
                "#6c5ce7", "rgba(108,92,231,0.08)");
        HBox.setHgrow(carteScore, Priority.ALWAYS);
        // ✅ Niveau basé sur pctReel (logique clinique correcte)
        VBox carteNiveau = creerCarteInfo("📊", "Niveau", niveauF, cF, bgF);
        HBox.setHgrow(carteNiveau, Priority.ALWAYS);
        ligneStats.getChildren().addAll(carteScore, carteNiveau);

        // ── CARTE IA ───────────────────────────────────────────────
        VBox carteConseil = new VBox(10);
        carteConseil.setMaxWidth(380); carteConseil.setPadding(new Insets(20));
        carteConseil.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,3);");

        HBox titreConseil = new HBox(8);
        titreConseil.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label iconeConseil = new Label("🧠"); iconeConseil.setStyle("-fx-font-size:18px;");
        Label lblTitreConseil = new Label("Analyse & Plan IA");
        lblTitreConseil.setStyle("-fx-font-size:14px; -fx-font-weight:900; -fx-text-fill:#2c3e50;");
        titreConseil.getChildren().addAll(iconeConseil, lblTitreConseil);

        Label lblConseilIA = new Label("🤖 Génération de votre plan personnalisé...");
        lblConseilIA.setWrapText(true); lblConseilIA.setMaxWidth(340);
        lblConseilIA.setStyle("-fx-font-size:12px; -fx-text-fill:#7f8c8d; -fx-font-style:italic;");

        VBox blocEmotion    = creerBlocTraitement(
                "🎭", "Émotion détectée (NLP)",
                "Analyse HuggingFace en cours...", "#FFF0F0", "#FF6B6B");
        VBox blocAnalyse    = creerBlocTraitement("🔍", "Analyse psychologique",
                "Analyse en cours...", "#EDE9FE", "#7C3AED");
        VBox blocTraitement = creerBlocTraitement("💊", "Plan de traitement",
                "Traitement en cours...", "#FEF3C7", "#D97706");
        VBox blocExercices  = creerBlocTraitement("🏃", "Exercices recommandés",
                "Exercices en cours...", "#ECFDF5", "#059669");

        carteConseil.getChildren().addAll(titreConseil, lblConseilIA, new Separator(),
                blocEmotion, blocAnalyse, blocTraitement, blocExercices);

        // ✅ Architecture hybride :
        //    Controller → ResultFusionService
        //                   → EmotionAnalyzer (HuggingFace NLP)
        //                   → ServiceGroq (LLM enrichi émotion)
        new Thread(() -> {
            try {
                ResultFusionService.ResultatFusionne res =
                        fusionService.analyser(quiz.getTitre(), pctIA);

                String emotionDetail = res.emotionLabel
                        + "  —  confiance " + (int)(res.emotionScore * 100) + "%";

                javafx.application.Platform.runLater(() -> {
                    // ✅ Émotion affichée en premier
                    mettreAJourBloc(blocEmotion, emotionDetail);

                    lblConseilIA.setText(res.analyse);
                    lblConseilIA.setStyle("-fx-font-size:12px; -fx-text-fill:#374151;"
                            + "-fx-font-style:normal; -fx-line-spacing:3;");
                    mettreAJourBloc(blocAnalyse,    res.analyse);
                    mettreAJourBloc(blocTraitement, res.traitement);
                    mettreAJourBloc(blocExercices,  res.exercices);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    mettreAJourBloc(blocEmotion, "😐 Neutre — analyse indisponible");
                    lblConseilIA.setText("Continuez à pratiquer et consultez un professionnel.");
                });
            }
        }).start();

        // ── BOUTON RETOUR ──────────────────────────────────────────
        Button btnOk = new Button("← Retour aux tests");
        btnOk.setMaxWidth(380); btnOk.setPrefHeight(46);
        btnOk.setStyle("-fx-background-color:#2c4a6e; -fx-text-fill:white;"
                + "-fx-font-size:14px; -fx-font-weight:bold;"
                + "-fx-background-radius:12; -fx-cursor:hand;");
        btnOk.setOnMouseEntered(e -> btnOk.setStyle(
                "-fx-background-color:#1a3a5c; -fx-text-fill:white;"
                        + "-fx-font-size:14px; -fx-font-weight:bold;"
                        + "-fx-background-radius:12; -fx-cursor:hand;"));
        btnOk.setOnMouseExited(e -> btnOk.setStyle(
                "-fx-background-color:#2c4a6e; -fx-text-fill:white;"
                        + "-fx-font-size:14px; -fx-font-weight:bold;"
                        + "-fx-background-radius:12; -fx-cursor:hand;"));

        // ── ANIMATIONS ─────────────────────────────────────────────
        javafx.animation.ScaleTransition scaleHero =
                new javafx.animation.ScaleTransition(javafx.util.Duration.millis(500), carteHero);
        scaleHero.setFromX(0.5); scaleHero.setFromY(0.5);
        scaleHero.setToX(1.0); scaleHero.setToY(1.0);
        scaleHero.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        javafx.animation.FadeTransition fadeStats =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(600), ligneStats);
        fadeStats.setFromValue(0); fadeStats.setToValue(1);

        javafx.animation.FadeTransition fadeConseil =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(600), carteConseil);
        fadeConseil.setFromValue(0); fadeConseil.setToValue(1);

        root.getChildren().addAll(carteHero, ligneStats, carteConseil, btnOk);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:#dce8f0; -fx-background-color:#dce8f0; -fx-border-color:transparent;");

        javafx.scene.Scene scene = new javafx.scene.Scene(scroll, 420, 600);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setScene(scene); stage.setTitle("Résultat du test");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        btnOk.setOnAction(e -> { stage.close(); SuivieController.rafraichir(); retourListe(); });

        stage.setOnShown(e -> {
            scaleHero.play();
            javafx.animation.PauseTransition p1 =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
            p1.setOnFinished(ev -> { compteur.play(); animBarre.play(); }); p1.play();
            javafx.animation.PauseTransition p2 =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
            p2.setOnFinished(ev -> { fadeStats.play(); fadeConseil.play(); }); p2.play();
        });

        stage.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ Grand score = patient va bien — pour TOUS les tests
    private String calculerNiveau(int pctReel, String titreLow) {
        if (titreLow.contains("stress")) {
            if      (pctReel >= 70) return "Bien géré";
            else if (pctReel >= 40) return "Modéré";
            else                    return "Critique";
        } else if (titreLow.contains("humeur")) {
            if      (pctReel >= 70) return "Bonne humeur";
            else if (pctReel >= 40) return "Humeur moyenne";
            else                    return "Humeur basse";
        } else {
            if      (pctReel >= 70) return "Excellent";
            else if (pctReel >= 40) return "Moyen";
            else                    return "Faible";
        }
    }

    private VBox creerBlocTraitement(String icone, String titre,
                                     String contenu, String couleurBg,
                                     String couleurBord) {
        VBox bloc = new VBox(0);
        bloc.setStyle(
                "-fx-background-color:" + couleurBg + ";"
                        + "-fx-background-radius:16;"
                        + "-fx-border-color:" + couleurBord + ";"
                        + "-fx-border-radius:16;"
                        + "-fx-border-width:1.5;"
                        + "-fx-effect:dropshadow(gaussian,"
                        + "rgba(0,0,0,0.05),6,0,0,2);");

        // ── Entête avec emoji dans cercle ──────────────────────────
        HBox entete = new HBox(12);
        entete.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        entete.setPadding(new Insets(14, 16, 10, 16));

        // Cercle emoji
        javafx.scene.layout.StackPane cercleEmoji =
                new javafx.scene.layout.StackPane();
        cercleEmoji.setMinSize(46, 46);
        cercleEmoji.setMaxSize(46, 46);
        cercleEmoji.setStyle(
                "-fx-background-color:" + couleurBord + ";"
                        + "-fx-background-radius:23;");

        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size:22px;");
        cercleEmoji.getChildren().add(lblIcone);

        // Titre + sous-titre
        VBox textBloc = new VBox(2);
        Label lblTitre = new Label(titre);
        lblTitre.setStyle(
                "-fx-font-size:13px; -fx-font-weight:900;"
                        + "-fx-text-fill:" + couleurBord + ";");
        Label lblStatus = new Label("En attente...");
        lblStatus.setStyle(
                "-fx-font-size:10px; -fx-text-fill:#94a3b8;"
                        + "-fx-font-style:italic;");
        textBloc.setUserData(lblStatus);
        textBloc.getChildren().addAll(lblTitre, lblStatus);

        entete.getChildren().addAll(cercleEmoji, textBloc);

        // ── Séparateur fin ─────────────────────────────────────────
        javafx.scene.layout.Pane ligne =
                new javafx.scene.layout.Pane();
        ligne.setPrefHeight(1);
        ligne.setStyle("-fx-background-color:" + couleurBord
                + "33;");

        // ── Contenu ────────────────────────────────────────────────
        Label lblContenu = new Label(contenu);
        lblContenu.setWrapText(true);
        lblContenu.setMaxWidth(Double.MAX_VALUE);
        lblContenu.setPadding(new Insets(10, 16, 14, 16));
        lblContenu.setStyle(
                "-fx-font-size:12px; -fx-text-fill:#374151;"
                        + "-fx-line-spacing:4;");

        bloc.setUserData(lblContenu);
        bloc.getChildren().addAll(entete, ligne, lblContenu);
        return bloc;
    }

    private void mettreAJourBloc(VBox bloc, String contenu) {
        if (bloc.getUserData() instanceof Label) ((Label) bloc.getUserData()).setText(contenu);
    }

    private VBox creerCarteInfo(String icone, String label,
                                String valeur, String couleur, String couleurBg) {
        VBox carte = new VBox(6); carte.setAlignment(javafx.geometry.Pos.CENTER);
        carte.setPadding(new Insets(16, 12, 16, 12));
        carte.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        javafx.scene.layout.StackPane iconePane = new javafx.scene.layout.StackPane();
        iconePane.setMinSize(44, 44); iconePane.setMaxSize(44, 44);
        iconePane.setStyle("-fx-background-color:" + couleurBg + "; -fx-background-radius:22;");
        Label lblIcone = new Label(icone); lblIcone.setStyle("-fx-font-size:18px;");
        iconePane.getChildren().add(lblIcone);
        Label lblLabel = new Label(label); lblLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#95a5a6; -fx-font-weight:bold;");
        Label lblValeur = new Label(valeur); lblValeur.setStyle("-fx-font-size:15px; -fx-font-weight:900; -fx-text-fill:" + couleur + ";");
        carte.getChildren().addAll(iconePane, lblLabel, lblValeur);
        return carte;
    }

    private void afficherAlerte(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre); alert.setHeaderText(null);
        alert.setContentText(message); alert.showAndWait();
    }

    @FXML private void retourListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PasserTests.fxml"));
            Node vue = loader.load();
            VBox parent = (VBox) listeQuestions.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (IOException e) { System.err.println("❌ Retour : " + e.getMessage()); }
    }
}