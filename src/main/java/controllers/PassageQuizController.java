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
import services.ServiceGroqQuiz;
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
    private final ServiceGroqQuiz serviceGroqQuiz = new ServiceGroqQuiz();

    private int  idPatient = -1;
    private Quiz quiz;
    private final Map<Integer, ToggleGroup> toggleGroups = new HashMap<>();

    public void setQuiz(Quiz quiz) {
        this.idPatient = Session.getUserId();
        System.out.println("PassageQuiz — patient ID=" + idPatient
                + " | quiz=" + quiz.getTitre());
        if (this.idPatient <= 0) {
            System.err.println("Aucun patient connecte !");
            if (labelTitreQuiz != null)
                labelTitreQuiz.setText("Veuillez vous connecter.");
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
            System.err.println("Questions : " + e.getMessage());
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
            afficherAlerte("Erreur", "Aucun patient connecte."); return;
        }
        for (Map.Entry<Integer, ToggleGroup> entry : toggleGroups.entrySet()) {
            if (entry.getValue().getSelectedToggle() == null) {
                afficherAlerte("Attention", "Veuillez repondre a toutes les questions.");
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
            System.out.println("Score soumis — patient ID="
                    + idPatient + " score=" + scoreTotal);
            String resultat = serviceQuiz.calculerEtSauvegarderScore(
                    quiz.getIdQuiz(), idPatient);
            SuivieQuizController.rafraichir();
            afficherResultat(resultat, scoreTotal, nbQuestions);
        } catch (SQLException e) {
            System.err.println("Soumission : " + e.getMessage());
        }
    }

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

    private void afficherResultat(String resultat, int score, int nbQuestions) {

        int scoreMax    = calculerScoreMax(nbQuestions);
        String titreLow = quiz.getTitre().toLowerCase();

        int pctReel      = (int) Math.min(100, Math.max(0, (score * 100.0) / scoreMax));
        int pctAffichage = pctReel;

        System.out.println("score=" + score + " / max=" + scoreMax
                + " | pct=" + pctReel + "% | quiz=" + quiz.getTitre());

        String niveau = calculerNiveau(pctReel, titreLow);

        String scoreTexte = String.valueOf(score);
        try {
            for (String p : resultat.split("\\|")) {
                p = p.trim();
                if (p.startsWith("Score:"))
                    scoreTexte = p.replace("Score:", "").trim();
            }
        } catch (Exception ignored) {}

        String couleurScore, couleurBg, emoji, messageMotiv;
        if (pctAffichage >= 70) {
            couleurScore = "#27ae60"; couleurBg = "#eafaf1";
            emoji = "\u2605"; messageMotiv = "Excellent resultat !";
        } else if (pctAffichage >= 40) {
            couleurScore = "#f39c12"; couleurBg = "#fef9e7";
            emoji = "\u2606"; messageMotiv = "Bon effort, continue !";
        } else {
            couleurScore = "#e74c3c"; couleurBg = "#fdedec";
            emoji = "\u2665"; messageMotiv = "N'abandonne pas !";
        }

        final int    pctDisplay = pctAffichage;
        final int    pctIA      = pctReel;
        final String cF         = couleurScore;
        final String bgF        = couleurBg;
        final String niveauF    = niveau;
        final String scoreF     = scoreTexte;

        // ── ROOT ───────────────────────────────────────────────────
        VBox root = new VBox(16);
        root.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        root.setPadding(new Insets(24, 20, 24, 20));
        root.setStyle("-fx-background-color:linear-gradient(to bottom, #dce8f0, #eaf2f8);");

        // ── CARTE HERO ─────────────────────────────────────────────
        VBox carteHero = new VBox(14);
        carteHero.setAlignment(javafx.geometry.Pos.CENTER);
        carteHero.setPadding(new Insets(30, 30, 24, 30));
        carteHero.setMaxWidth(380);
        carteHero.setStyle("-fx-background-color:linear-gradient(to bottom right, #2c4a6e, #1a3a5c);"
                + "-fx-background-radius:20;"
                + "-fx-effect:dropshadow(gaussian,rgba(44,74,110,0.45),22,0,0,8);");

        javafx.scene.layout.StackPane emojiPane = new javafx.scene.layout.StackPane();
        emojiPane.setMinSize(80, 80); emojiPane.setMaxSize(80, 80);
        emojiPane.setStyle("-fx-background-color:rgba(255,255,255,0.15); -fx-background-radius:40;");
        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size:34px;");
        emojiPane.getChildren().add(lblEmoji);

        Label lblNomQuiz = new Label(quiz.getTitre());
        lblNomQuiz.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(255,255,255,0.7);"
                + "-fx-font-weight:bold; -fx-background-color:rgba(255,255,255,0.1);"
                + "-fx-background-radius:20; -fx-padding:4 14 4 14;");

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
        VBox carteScore = creerCarteInfo("\u25CE", "Score obtenu", scoreF + " pts",
                "#6c5ce7", "rgba(108,92,231,0.08)");
        HBox.setHgrow(carteScore, Priority.ALWAYS);
        VBox carteNiveau = creerCarteInfo("\u25A0", "Niveau", niveauF, cF, bgF);
        HBox.setHgrow(carteNiveau, Priority.ALWAYS);
        ligneStats.getChildren().addAll(carteScore, carteNiveau);

        // ── CARTE IA ───────────────────────────────────────────────
        VBox carteConseil = new VBox(10);
        carteConseil.setMaxWidth(380); carteConseil.setPadding(new Insets(20));
        carteConseil.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:#e8edf5; -fx-border-width:1.5; -fx-border-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(44,74,110,0.10),14,0,0,4);");

        HBox titreConseil = new HBox(8);
        titreConseil.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titreConseil.setPadding(new Insets(0, 0, 8, 0));
        titreConseil.setStyle("-fx-border-color:transparent transparent #e8edf5 transparent; -fx-border-width:0 0 1 0;");
        Label iconeConseil = new Label("\u26A1");
        iconeConseil.setStyle("-fx-font-size:16px; -fx-text-fill:#2c4a6e;");
        Label lblTitreConseil = new Label("Analyse & Plan IA");
        lblTitreConseil.setStyle("-fx-font-size:15px; -fx-font-weight:900; -fx-text-fill:#2c3e50; -fx-font-style:italic;");
        titreConseil.getChildren().addAll(iconeConseil, lblTitreConseil);

        // 3 blocs séparés — chacun recevra sa propre section
        VBox blocAnalyse    = creerBlocTraitement("\u26B2", "Analyse psychologique",
                "Analyse en cours...", "#EDE9FE", "#7C3AED");
        VBox blocTraitement = creerBlocTraitement("\u2665", "Plan de traitement",
                "Traitement en cours...", "#FEF3C7", "#D97706");
        VBox blocExercices  = creerBlocTraitement("\u25B6", "Exercices recommandes",
                "Exercices en cours...", "#ECFDF5", "#059669");

        carteConseil.getChildren().addAll(titreConseil, new Separator(),
                blocAnalyse, blocTraitement, blocExercices);

        // ── APPEL GROQ ─────────────────────────────────────────────
        new Thread(() -> {
            try {
                String prompt      = construirePrompt(quiz.getTitre(), pctIA);
                String reponseGroq = serviceGroqQuiz.appellerGroq(prompt);

                System.out.println("=== REPONSE GROQ RAW ===\n" + reponseGroq + "\n========================");

                // ── Parsing robuste : accepte "SECTION:" ET "SECTION :" ──
                String analyse    = extraireSection(reponseGroq, "ANALYSE");
                String traitement = extraireSection(reponseGroq, "TRAITEMENT");
                String exercices  = extraireSection(reponseGroq, "EXERCICES");

                // ── Nettoyage des guillemets échappés (\") ───────────
                if (analyse    != null) analyse    = nettoyer(analyse);
                if (traitement != null) traitement = nettoyer(traitement);
                if (exercices  != null) exercices  = nettoyer(exercices);

                // ── Fallbacks si parsing rate ──────────────────────
                if (analyse == null || analyse.isBlank())
                    analyse = reponseGroq != null ? nettoyer(reponseGroq)
                            : "Continuez a pratiquer et consultez un professionnel.";
                if (traitement == null || traitement.isBlank())
                    traitement = "Consultez un professionnel de sante pour un suivi personnalise.";
                if (exercices == null || exercices.isBlank())
                    exercices = "1. Respiration profonde 10 min/jour.\n2. Journal de bord quotidien.\n3. Marche de 20 minutes en plein air.";

                final String a = analyse, t = traitement, ex = exercices;

                javafx.application.Platform.runLater(() -> {
                    mettreAJourBloc(blocAnalyse,    a);
                    mettreAJourBloc(blocTraitement, t);
                    mettreAJourBloc(blocExercices,  ex);
                });

            } catch (Exception e) {
                System.err.println("Groq erreur : " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    mettreAJourBloc(blocAnalyse,
                            "Continuez a pratiquer et consultez un professionnel.");
                    mettreAJourBloc(blocTraitement,
                            "Consultez un professionnel de sante.");
                    mettreAJourBloc(blocExercices,
                            "Pratiquez la respiration profonde 10 min/jour.");
                });
            }
        }).start();

        // ── BOUTON RETOUR ──────────────────────────────────────────
        Button btnOk = new Button("\u2190 Retour aux tests");
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
        stage.setScene(scene); stage.setTitle("Resultat du test");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        btnOk.setOnAction(e -> { stage.close(); SuivieQuizController.rafraichir(); retourListe(); });

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
    // PROMPT — structure stricte pour forcer le format
    // ══════════════════════════════════════════════════════════════
    private String construirePrompt(String titreQuiz, int pctScore) {
        return "Tu es un psychologue clinicien expert. Reponds UNIQUEMENT en francais.\n"
                + "Un patient a obtenu " + pctScore + "% au test psychologique '" + titreQuiz + "'.\n\n"
                + "Reponds en respectant EXACTEMENT ce format avec ces 3 labels en majuscules :\n\n"
                + "ANALYSE: [Ton analyse psychologique du score en 3 phrases.]\n\n"
                + "TRAITEMENT: [Ton plan de traitement concret en 3 phrases.]\n\n"
                + "EXERCICES: [3 exercices pratiques numerotes 1. 2. 3. avec description courte.]\n\n"
                + "IMPORTANT: utilise exactement ANALYSE: TRAITEMENT: EXERCICES: sans espace avant les deux-points.";
    }

    // ══════════════════════════════════════════════════════════════
    // PARSING ROBUSTE — accepte "SECTION:" et "SECTION :"
    // ══════════════════════════════════════════════════════════════
    private String extraireSection(String texte, String section) {
        if (texte == null || texte.isBlank()) return null;
        try {
            // Normaliser : remplacer "SECTION :" par "SECTION:"
            String normalise = texte
                    .replace("ANALYSE :",    "ANALYSE:")
                    .replace("TRAITEMENT :", "TRAITEMENT:")
                    .replace("EXERCICES :",  "EXERCICES:");

            String prefixe = section + ":";
            int idx = normalise.indexOf(prefixe);
            if (idx < 0) return null;

            int debut = idx + prefixe.length();
            int fin   = normalise.length();

            // Trouver la prochaine section pour couper
            for (String s : new String[]{"ANALYSE:", "TRAITEMENT:", "EXERCICES:"}) {
                if (s.equals(prefixe)) continue;
                int pos = normalise.indexOf(s, debut);
                if (pos > debut && pos < fin) fin = pos;
            }

            return normalise.substring(debut, fin).trim();
        } catch (Exception e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // NETTOYAGE — supprime \" et autres artefacts Groq
    // ══════════════════════════════════════════════════════════════
    private String nettoyer(String texte) {
        if (texte == null) return null;
        return texte
                .replace("\\\"", "\"")   // \" → "
                .replace("\\'",  "'")    // \' → '
                .replace("\\n",  "\n")   // \n littéral → saut de ligne
                .replace("**",   "")     // bold markdown
                .replace("##",   "")     // heading markdown
                .trim();
    }

    private String calculerNiveau(int pctReel, String titreLow) {
        if (titreLow.contains("stress")) {
            if      (pctReel >= 70) return "Bien gere";
            else if (pctReel >= 40) return "Modere";
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
                        + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

        HBox entete = new HBox(12);
        entete.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        entete.setPadding(new Insets(14, 16, 10, 16));

        javafx.scene.layout.StackPane cercleEmoji = new javafx.scene.layout.StackPane();
        cercleEmoji.setMinSize(46, 46); cercleEmoji.setMaxSize(46, 46);
        cercleEmoji.setStyle("-fx-background-color:" + couleurBord + "; -fx-background-radius:23;");
        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size:22px;");
        cercleEmoji.getChildren().add(lblIcone);

        VBox textBloc = new VBox(2);
        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-size:13px; -fx-font-weight:900; -fx-text-fill:" + couleurBord + ";");
        Label lblStatus = new Label("En attente...");
        lblStatus.setStyle("-fx-font-size:10px; -fx-text-fill:#94a3b8; -fx-font-style:italic;");
        textBloc.getChildren().addAll(lblTitre, lblStatus);
        entete.getChildren().addAll(cercleEmoji, textBloc);

        javafx.scene.layout.Pane ligne = new javafx.scene.layout.Pane();
        ligne.setPrefHeight(1);
        ligne.setStyle("-fx-background-color:" + couleurBord + "33;");

        Label lblContenu = new Label(contenu);
        lblContenu.setWrapText(true);
        lblContenu.setMaxWidth(Double.MAX_VALUE);
        lblContenu.setPadding(new Insets(10, 16, 14, 16));
        lblContenu.setStyle("-fx-font-size:12.5px; -fx-text-fill:#374151; -fx-line-spacing:4;");

        Map<String, Label> dataMap = new HashMap<>();
        dataMap.put("contenu", lblContenu);
        dataMap.put("status",  lblStatus);
        bloc.setUserData(dataMap);

        bloc.getChildren().addAll(entete, ligne, lblContenu);
        return bloc;
    }

    @SuppressWarnings("unchecked")
    private void mettreAJourBloc(VBox bloc, String contenu) {
        Object data = bloc.getUserData();
        if (data instanceof Map) {
            Map<String, Label> map = (Map<String, Label>) data;
            if (map.get("contenu") != null) map.get("contenu").setText(contenu);
            if (map.get("status")  != null) map.get("status").setText("Termine");
        }
    }

    private VBox creerCarteInfo(String icone, String label,
                                String valeur, String couleur, String couleurBg) {
        VBox carte = new VBox(6);
        carte.setAlignment(javafx.geometry.Pos.CENTER);
        carte.setPadding(new Insets(16, 12, 16, 12));
        carte.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        javafx.scene.layout.StackPane iconePane = new javafx.scene.layout.StackPane();
        iconePane.setMinSize(44, 44); iconePane.setMaxSize(44, 44);
        iconePane.setStyle("-fx-background-color:" + couleurBg + "; -fx-background-radius:22;");
        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size:18px;");
        iconePane.getChildren().add(lblIcone);
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#95a5a6; -fx-font-weight:bold;");
        Label lblValeur = new Label(valeur);
        lblValeur.setStyle("-fx-font-size:15px; -fx-font-weight:900; -fx-text-fill:" + couleur + ";");
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
        } catch (IOException e) { System.err.println("Retour : " + e.getMessage()); }
    }
}