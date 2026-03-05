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

public class PasserTestsController {

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
        Session.Role role = Session.getRoleConnecte();
        this.idPatient = Session.getUserId();

        // ✅ Seul le patient peut passer un test
        if (role != Session.Role.USER) {
            if (labelTitreQuiz != null)
                labelTitreQuiz.setText("⛔ Réservé aux patients.");
            if (labelDescriptionQuiz != null)
                labelDescriptionQuiz.setText("Les psychologues ne passent pas les tests.");
            if (btnSoumettre != null) {
                btnSoumettre.setDisable(true);
                btnSoumettre.setVisible(false);
            }
            return;
        }

        // ✅ Patient non connecté
        if (this.idPatient <= 0) {
            if (labelTitreQuiz != null)
                labelTitreQuiz.setText("Veuillez vous connecter.");
            return;
        }

        // ✅ Patient connecté → charger le quiz
        this.quiz = quiz;
        labelTitreQuiz.setText(quiz.getTitre());
        labelDescriptionQuiz.setText(
                quiz.getDescription() != null ? quiz.getDescription() : "");
        chargerQuestions();
    }
    private void chargerQuestions() {
        try {
            List<Question> questions = serviceQuestion.getQuestionsByQuizAvecChoix(quiz.getIdQuiz());
            listeQuestions.getChildren().clear();
            toggleGroups.clear();
            for (int i = 0; i < questions.size(); i++)
                listeQuestions.getChildren().add(creerCarteQuestion(questions.get(i), i + 1));
        } catch (SQLException e) {
            System.err.println("Questions : " + e.getMessage());
        }
    }

    private VBox creerCarteQuestion(Question question, int numero) {
        VBox carte = new VBox(14);
        carte.setPadding(new Insets(20));
        carte.setStyle(
                "-fx-background-color: rgba(255,255,255,0.78);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: rgba(92,152,168,0.14);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(92,152,168,0.09), 10, 0, 0, 2);"
        );

        Label texte = new Label(numero + ". " + question.getTexteQuestion());
        texte.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: 900;" +
                        "-fx-text-fill: #1F2A33; -fx-wrap-text: true;"
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
                        "-fx-font-size: 13px; -fx-font-weight: 600;" +
                                "-fx-text-fill: #1F2A33; -fx-cursor: hand;"
                );
                choixBox.getChildren().add(rb);
            }
        }
        carte.getChildren().addAll(texte, choixBox);
        return carte;
    }

    @FXML
    private void soumettreTest() {
        // ✅ Double vérification du rôle au moment de la soumission
        if (Session.getRoleConnecte() != Session.Role.USER) {
            afficherAlerte("Erreur", "Seuls les patients peuvent soumettre un test.");
            return;
        }

        if (idPatient <= 0) {
            afficherAlerte("Erreur", "Aucun patient connecté.");
            return;
        }

        for (Map.Entry<Integer, ToggleGroup> entry : toggleGroups.entrySet()) {
            if (entry.getValue().getSelectedToggle() == null) {
                afficherAlerte("Attention", "Veuillez répondre à toutes les questions.");
                return;
            }
        }

        try {
            int scoreTotal = 0, nbQuestions = toggleGroups.size();
            for (Map.Entry<Integer, ToggleGroup> entry : toggleGroups.entrySet()) {
                int idQuestion = entry.getKey();
                Reponse choix = (Reponse) entry.getValue().getSelectedToggle().getUserData();
                scoreTotal += choix.getValeur();
                // ✅ create() — interface IService
                serviceReponse.create(new Reponse(
                        quiz.getIdQuiz(), idQuestion, idPatient,
                        choix.getTexteReponse(), choix.getValeur()));
            }
            String resultat = serviceQuiz.calculerEtSauvegarderScore(
                    quiz.getIdQuiz(), idPatient);
            SuivieController.rafraichir();
            afficherResultat(resultat, scoreTotal, nbQuestions);
        } catch (SQLException e) {
            System.err.println("Soumission : " + e.getMessage());
        }
    }

    private int calculerScoreMax(int nbQuestionsDefault) {
        int scoreMaxReel = 0;
        try {
            List<Question> questions = serviceQuestion.getQuestionsByQuizAvecChoix(quiz.getIdQuiz());
            for (Question q : questions) {
                if (q.getReponses() != null && !q.getReponses().isEmpty())
                    scoreMaxReel += q.getReponses().stream().mapToInt(Reponse::getValeur).max().orElse(6);
            }
        } catch (Exception e) { scoreMaxReel = nbQuestionsDefault * 6; }
        return scoreMaxReel > 0 ? scoreMaxReel : nbQuestionsDefault * 6;
    }

    private void afficherResultat(String resultat, int score, int nbQuestions) {
        int scoreMax     = calculerScoreMax(nbQuestions);
        String titreLow  = quiz.getTitre().toLowerCase();
        int pctReel      = (int) Math.min(100, Math.max(0, (score * 100.0) / scoreMax));
        String niveau    = calculerNiveau(pctReel, titreLow);
        String scoreTexte = String.valueOf(score);
        try {
            for (String p : resultat.split("\\|")) {
                p = p.trim();
                if (p.startsWith("Score:")) scoreTexte = p.replace("Score:", "").trim();
            }
        } catch (Exception ignored) {}

        String couleurScore, couleurBg, emoji, messageMotiv;
        if (pctReel >= 70) {
            couleurScore = "#059669"; couleurBg = "rgba(16,185,129,0.08)";
            emoji = "✦"; messageMotiv = "Excellent résultat !";
        } else if (pctReel >= 40) {
            couleurScore = "#D97706"; couleurBg = "rgba(245,158,11,0.08)";
            emoji = "◈"; messageMotiv = "Bon effort, continue !";
        } else {
            couleurScore = "#EF4444"; couleurBg = "rgba(239,68,68,0.08)";
            emoji = "♡"; messageMotiv = "N'abandonne pas !";
        }

        final int    pctF    = pctReel;
        final String cF      = couleurScore;
        final String bgF     = couleurBg;
        final String niveauF = niveau;
        final String scoreF  = scoreTexte;

        VBox root = new VBox(16);
        root.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        root.setPadding(new Insets(24, 20, 24, 20));
        root.setStyle("-fx-background-color: #EAF3F5;");

        VBox carteHero = new VBox(14);
        carteHero.setAlignment(javafx.geometry.Pos.CENTER);
        carteHero.setPadding(new Insets(30, 30, 24, 30));
        carteHero.setMaxWidth(400);
        carteHero.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #5C98A8, #3A7A8C);" +
                        "-fx-background-radius: 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(92,152,168,0.40), 20, 0, 0, 6);"
        );

        javafx.scene.layout.StackPane emojiPane = new javafx.scene.layout.StackPane();
        emojiPane.setMinSize(80, 80); emojiPane.setMaxSize(80, 80);
        emojiPane.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-background-radius: 40;");
        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 34px;");
        emojiPane.getChildren().add(lblEmoji);

        Label lblNomQuiz = new Label(quiz.getTitre());
        lblNomQuiz.setStyle(
                "-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.80);" +
                        "-fx-font-weight: 800; -fx-background-color: rgba(255,255,255,0.12);" +
                        "-fx-background-radius: 20; -fx-padding: 4 14;"
        );

        Label lblPct = new Label("0%");
        lblPct.setStyle("-fx-font-size: 58px; -fx-font-weight: 900; -fx-text-fill: white;");

        javafx.animation.Timeline compteur = new javafx.animation.Timeline();
        for (int i = 0; i <= pctF; i++) {
            final int val = i;
            compteur.getKeyFrames().add(new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(i * (1100.0 / Math.max(pctF, 1))),
                    e -> lblPct.setText(val + "%")));
        }

        Label lblMotiv = new Label(messageMotiv);
        lblMotiv.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.92); -fx-font-weight: 800;");

        javafx.scene.layout.StackPane barreContainer = new javafx.scene.layout.StackPane();
        barreContainer.setPrefWidth(280); barreContainer.setPrefHeight(10);
        barreContainer.setStyle("-fx-background-color: rgba(255,255,255,0.20); -fx-background-radius: 5;");
        javafx.scene.layout.Pane barreFill = new javafx.scene.layout.Pane();
        barreFill.setPrefHeight(10); barreFill.setPrefWidth(0);
        String couleurBarre = pctF >= 70 ? "#10B981" : pctF >= 40 ? "#F59E0B" : "#EF4444";
        barreFill.setStyle("-fx-background-color: " + couleurBarre + "; -fx-background-radius: 5;");
        javafx.scene.layout.StackPane.setAlignment(barreFill, javafx.geometry.Pos.CENTER_LEFT);
        barreContainer.getChildren().add(barreFill);
        javafx.animation.Timeline animBarre = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(barreFill.prefWidthProperty(), 0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(1100),
                        new javafx.animation.KeyValue(barreFill.prefWidthProperty(),
                                280.0 * pctF / 100.0, javafx.animation.Interpolator.EASE_OUT)));

        carteHero.getChildren().addAll(emojiPane, lblNomQuiz, lblPct, lblMotiv, barreContainer);

        HBox ligneStats = new HBox(12);
        ligneStats.setMaxWidth(400); ligneStats.setAlignment(javafx.geometry.Pos.CENTER);
        VBox carteScore = creerCarteInfo("◎", "Score obtenu", scoreF + " pts", "#7C3AED", "rgba(167,139,250,0.10)");
        HBox.setHgrow(carteScore, Priority.ALWAYS);
        VBox carteNiveau = creerCarteInfo("■", "Niveau", niveauF, cF, bgF);
        HBox.setHgrow(carteNiveau, Priority.ALWAYS);
        ligneStats.getChildren().addAll(carteScore, carteNiveau);

        VBox carteConseil = new VBox(10);
        carteConseil.setMaxWidth(400); carteConseil.setPadding(new Insets(20));
        carteConseil.setStyle(
                "-fx-background-color: rgba(255,255,255,0.78);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: rgba(92,152,168,0.14);" +
                        "-fx-border-width: 1.5; -fx-border-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(92,152,168,0.09), 12, 0, 0, 3);"
        );

        HBox titreConseil = new HBox(8);
        titreConseil.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titreConseil.setPadding(new Insets(0, 0, 8, 0));
        titreConseil.setStyle("-fx-border-color: transparent transparent rgba(92,152,168,0.15) transparent; -fx-border-width: 0 0 1 0;");
        Label iconeConseil = new Label("⚡");
        iconeConseil.setStyle("-fx-font-size: 15px;");
        Label lblTitreConseil = new Label("Analyse & Plan IA");
        lblTitreConseil.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #1F2A33;");
        titreConseil.getChildren().addAll(iconeConseil, lblTitreConseil);

        VBox blocAnalyse    = creerBlocTraitement("◈", "Analyse psychologique",  "Analyse en cours...",   "rgba(167,139,250,0.10)", "#7C3AED");
        VBox blocTraitement = creerBlocTraitement("♡", "Plan de traitement",     "Traitement en cours...", "rgba(245,158,11,0.08)",  "#D97706");
        VBox blocExercices  = creerBlocTraitement("▶", "Exercices recommandés",  "Exercices en cours...", "rgba(16,185,129,0.08)",  "#059669");

        carteConseil.getChildren().addAll(titreConseil, new Separator(), blocAnalyse, blocTraitement, blocExercices);

        new Thread(() -> {
            try {
                String prompt      = construirePrompt(quiz.getTitre(), pctF);
                String reponseGroq = serviceGroqQuiz.appellerGroq(prompt);
                String analyse    = extraireSection(reponseGroq, "ANALYSE");
                String traitement = extraireSection(reponseGroq, "TRAITEMENT");
                String exercices  = extraireSection(reponseGroq, "EXERCICES");
                if (analyse    != null) analyse    = nettoyer(analyse);
                if (traitement != null) traitement = nettoyer(traitement);
                if (exercices  != null) exercices  = nettoyer(exercices);
                if (analyse == null || analyse.isBlank())       analyse    = reponseGroq != null ? nettoyer(reponseGroq) : "Consultez un professionnel.";
                if (traitement == null || traitement.isBlank()) traitement = "Consultez un professionnel de santé pour un suivi personnalisé.";
                if (exercices  == null || exercices.isBlank())  exercices  = "1. Respiration profonde 10 min/jour.\n2. Journal de bord quotidien.\n3. Marche de 20 minutes en plein air.";
                final String a = analyse, t = traitement, ex = exercices;
                javafx.application.Platform.runLater(() -> {
                    mettreAJourBloc(blocAnalyse, a);
                    mettreAJourBloc(blocTraitement, t);
                    mettreAJourBloc(blocExercices, ex);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    mettreAJourBloc(blocAnalyse,    "Continuez a pratiquer et consultez un professionnel.");
                    mettreAJourBloc(blocTraitement, "Consultez un professionnel de santé.");
                    mettreAJourBloc(blocExercices,  "Pratiquez la respiration profonde 10 min/jour.");
                });
            }
        }).start();

        Button btnOk = new Button("← Retour aux tests");
        btnOk.setMaxWidth(400); btnOk.setPrefHeight(46);
        btnOk.setStyle(
                "-fx-background-color: #5C98A8; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-font-weight: 800;" +
                        "-fx-background-radius: 12; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(92,152,168,0.30), 8, 0, 0, 2);"
        );

        javafx.animation.ScaleTransition scaleHero =
                new javafx.animation.ScaleTransition(javafx.util.Duration.millis(500), carteHero);
        scaleHero.setFromX(0.5); scaleHero.setFromY(0.5);
        scaleHero.setToX(1.0);   scaleHero.setToY(1.0);
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
        scroll.setStyle("-fx-background: #EAF3F5; -fx-background-color: #EAF3F5; -fx-border-color: transparent;");

        javafx.scene.Scene scene = new javafx.scene.Scene(scroll, 440, 620);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setScene(scene); stage.setTitle("Résultat du test");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        btnOk.setOnAction(e -> { stage.close(); SuivieController.rafraichir(); retourListe(); });

        stage.setOnShown(e -> {
            scaleHero.play();
            javafx.animation.PauseTransition p1 = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
            p1.setOnFinished(ev -> { compteur.play(); animBarre.play(); }); p1.play();
            javafx.animation.PauseTransition p2 = new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
            p2.setOnFinished(ev -> { fadeStats.play(); fadeConseil.play(); }); p2.play();
        });

        stage.showAndWait();
    }

    private String construirePrompt(String titreQuiz, int pctScore) {
        return "Tu es un psychologue clinicien expert. Reponds UNIQUEMENT en francais.\n"
                + "Un patient a obtenu " + pctScore + "% au test psychologique '" + titreQuiz + "'.\n\n"
                + "Reponds en respectant EXACTEMENT ce format :\n\n"
                + "ANALYSE: [Ton analyse psychologique du score en 3 phrases.]\n\n"
                + "TRAITEMENT: [Ton plan de traitement concret en 3 phrases.]\n\n"
                + "EXERCICES: [3 exercices pratiques numerotes 1. 2. 3. avec description courte.]\n\n"
                + "IMPORTANT: utilise exactement ANALYSE: TRAITEMENT: EXERCICES: sans espace avant les deux-points.";
    }

    private String extraireSection(String texte, String section) {
        if (texte == null || texte.isBlank()) return null;
        try {
            String n = texte.replace("ANALYSE :", "ANALYSE:").replace("TRAITEMENT :", "TRAITEMENT:").replace("EXERCICES :", "EXERCICES:");
            String prefixe = section + ":";
            int idx = n.indexOf(prefixe); if (idx < 0) return null;
            int debut = idx + prefixe.length(), fin = n.length();
            for (String s : new String[]{"ANALYSE:", "TRAITEMENT:", "EXERCICES:"}) {
                if (s.equals(prefixe)) continue;
                int pos = n.indexOf(s, debut);
                if (pos > debut && pos < fin) fin = pos;
            }
            return n.substring(debut, fin).trim();
        } catch (Exception e) { return null; }
    }

    private String nettoyer(String texte) {
        if (texte == null) return null;
        return texte.replace("\\\"", "\"").replace("\\'", "'").replace("\\n", "\n").replace("**", "").replace("##", "").trim();
    }

    private String calculerNiveau(int pct, String titreLow) {
        if (titreLow.contains("stress"))
            return pct >= 70 ? "Bien géré" : pct >= 40 ? "Modéré" : "Critique";
        else if (titreLow.contains("humeur"))
            return pct >= 70 ? "Bonne humeur" : pct >= 40 ? "Humeur moyenne" : "Humeur basse";
        else
            return pct >= 70 ? "Excellent" : pct >= 40 ? "Moyen" : "Faible";
    }

    private VBox creerBlocTraitement(String icone, String titre, String contenu, String couleurBg, String couleurBord) {
        VBox bloc = new VBox(0);
        bloc.setStyle(
                "-fx-background-color: " + couleurBg + ";" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + couleurBord + "33;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);"
        );

        HBox entete = new HBox(12);
        entete.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        entete.setPadding(new Insets(14, 16, 10, 16));

        javafx.scene.layout.StackPane cercle = new javafx.scene.layout.StackPane();
        cercle.setMinSize(46, 46); cercle.setMaxSize(46, 46);
        cercle.setStyle("-fx-background-color: " + couleurBord + "22; -fx-background-radius: 23;");
        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size: 22px; -fx-text-fill: " + couleurBord + ";");
        cercle.getChildren().add(lblIcone);

        VBox textBloc = new VBox(2);
        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: " + couleurBord + ";");
        Label lblStatus = new Label("En attente...");
        lblStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: #8AA8B2; -fx-font-style: italic;");
        textBloc.getChildren().addAll(lblTitre, lblStatus);
        entete.getChildren().addAll(cercle, textBloc);

        javafx.scene.layout.Pane ligne = new javafx.scene.layout.Pane();
        ligne.setPrefHeight(1);
        ligne.setStyle("-fx-background-color: " + couleurBord + "25;");

        Label lblContenu = new Label(contenu);
        lblContenu.setWrapText(true);
        lblContenu.setMaxWidth(Double.MAX_VALUE);
        lblContenu.setPadding(new Insets(10, 16, 14, 16));
        lblContenu.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #374151; -fx-line-spacing: 4;");

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
            if (map.get("status")  != null) map.get("status").setText("Terminé ✓");
        }
    }

    private VBox creerCarteInfo(String icone, String label, String valeur, String couleur, String couleurBg) {
        VBox carte = new VBox(6);
        carte.setAlignment(javafx.geometry.Pos.CENTER);
        carte.setPadding(new Insets(16, 12, 16, 12));
        carte.setStyle(
                "-fx-background-color: rgba(255,255,255,0.78);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(92,152,168,0.12);" +
                        "-fx-border-radius: 16; -fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(92,152,168,0.08), 8, 0, 0, 2);"
        );
        javafx.scene.layout.StackPane iconePane = new javafx.scene.layout.StackPane();
        iconePane.setMinSize(44, 44); iconePane.setMaxSize(44, 44);
        iconePane.setStyle("-fx-background-color: " + couleurBg + "; -fx-background-radius: 22;");
        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size: 18px; -fx-text-fill: " + couleur + ";");
        iconePane.getChildren().add(lblIcone);
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #8AA8B2; -fx-font-weight: 700;");
        Label lblValeur = new Label(valeur);
        lblValeur.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: " + couleur + ";");
        carte.getChildren().addAll(iconePane, lblLabel, lblValeur);
        return carte;
    }

    private void afficherAlerte(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre); alert.setHeaderText(null);
        alert.setContentText(message); alert.showAndWait();
    }

    @FXML
    private void retourListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PasserTests.fxml"));
            Node vue = loader.load();
            VBox parent = (VBox) listeQuestions.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (IOException e) { System.err.println("Retour : " + e.getMessage()); }
    }
}