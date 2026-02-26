package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.StrokeLineCap;
import services.ServiceQuiz;
import services.ServiceGroq;
import services.ServiceMusique;
import services.ServiceRappel;
import services.AvatarService;
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SuivieController {

    private MindCareLayoutController parentController;
    private static SuivieController  instance;
    private int currentPatientId = -1;

    @FXML private Label   lblBienvenue;
    @FXML private Label   lblCoinsGagnes;
    @FXML private Label   lblSessionCount;
    @FXML private Label   scoreBienEtre;
    @FXML private Label   scoreStress;
    @FXML private Label   scoreHumeur;
    @FXML private Arc     arcBienEtre;
    @FXML private Arc     arcStress;
    @FXML private Arc     arcHumeur;
    @FXML private Label   lblTrendBienEtre;
    @FXML private Label   lblTrendStress;
    @FXML private Label   lblTrendHumeur;
    @FXML private ComboBox<String>          comboPeriode;
    @FXML private AreaChart<String, Number> evolutionChart;
    @FXML private Label   lblConseil;
    @FXML private VBox    historyBox;
    @FXML private VBox    badgesBox;        // ✅ Ajouter dans Suivie.fxml
    @FXML private Button  btnEspacePsy;
    @FXML private Label   lblTestResultat;

    @FXML private Label lblEmoji1; @FXML private Label lblTitre1; @FXML private Label lblDesc1;
    @FXML private Label lblEmoji2; @FXML private Label lblTitre2; @FXML private Label lblDesc2;
    @FXML private Label lblEmoji3; @FXML private Label lblTitre3; @FXML private Label lblDesc3;

    @FXML private Label  lblTestRecommande;
    @FXML private Button btnPasserTestRecommande;

    // ── Avatar ─────────────────────────────────────────────────
    @FXML private javafx.scene.image.ImageView imgAvatar;
    @FXML private Label  lblAvatarPseudo;
    @FXML private Label  lblAvatarEtat;
    @FXML private Label  lblAvatarMessage;
    @FXML private Button btnPersonnaliserAvatar;
    @FXML private VBox   avatarEtatBox;

    @FXML private Label   lblMusiqueMessage;
    @FXML private Button  btnChargerMusique;
    @FXML private Button  btnPlayPause;
    @FXML private Label   lblPisteEmoji;
    @FXML private Label   lblPisteNom;
    @FXML private Label   lblPisteDuree;
    @FXML private Label   lblEtatLecture;
    @FXML private VBox    listePistesBox;
    @FXML private Label   lblChargementPistes;
    @FXML private Slider  sliderVolume;

    private final ServiceQuiz    serviceQuiz    = new ServiceQuiz();
    private final ServiceGroq    serviceGroq    = new ServiceGroq();
    private final ServiceMusique serviceMusique = new ServiceMusique();
    private final AvatarService  avatarService  = new AvatarService();

    private AvatarService.PrefsAvatar prefsAvatar;

    private int scoreBE = 0, scoreST = 0, scoreHU = 0;
    private String testRecommande = "";
    static boolean rappelDejaVerifie = false;

    private javafx.scene.media.MediaPlayer mediaPlayer;
    private List<ServiceMusique.Piste>     pistesChargees = new ArrayList<>();
    private int                            pisteActuelle  = 0;
    private boolean                        enLecture      = false;

    public static void rafraichir() {
        if (instance != null) {
            Platform.runLater(() -> {
                instance.updateScoresFromDB();
                instance.chargerGraphiqueReel();
                System.out.println("🔄 SuivieController rafraîchi !");
            });
        }
    }

    @FXML
    public void initialize() {
        instance = this;
        var role = Session.getRoleConnecte();
        currentPatientId = Session.getUserId();

        System.out.println("👤 Patient ID=" + currentPatientId
                + " | Nom=" + Session.getFullName() + " | Role=" + role);

        if (btnEspacePsy != null) {
            boolean peutVoir = (role == Session.Role.PSYCHOLOGUE || role == Session.Role.ADMIN);
            btnEspacePsy.setVisible(peutVoir);
            btnEspacePsy.setManaged(peutVoir);
        }
        if (currentPatientId <= 0) {
            if (lblBienvenue != null) lblBienvenue.setText("⛔ Aucun utilisateur connecté.");
            return;
        }
        if (role != null && role != Session.Role.USER && role != Session.Role.ADMIN) {
            if (lblBienvenue != null) lblBienvenue.setText("⛔ Accès réservé aux patients.");
            return;
        }
        if (sliderVolume != null)
            sliderVolume.valueProperty().addListener(
                    (obs, ov, nv) -> { if (mediaPlayer != null) mediaPlayer.setVolume(nv.doubleValue()); });

        configurerCombo();
        loadPatientData();
        chargerGraphiqueReel();
        afficherRecommandationTest();
        // ✅ Charger avatar personnalisé
        chargerAvatar();

        if (!rappelDejaVerifie) {
            rappelDejaVerifie = true;
            new Thread(() -> {
                try { new ServiceRappel().verifierEtEnvoyerRappels(); }
                catch (Exception e) { System.err.println("❌ Rappels : " + e.getMessage()); }
            }).start();
        }
    }

    public void setParentController(MindCareLayoutController parent) {
        this.parentController = parent;
    }

    private void loadPatientData() {
        try {
            String nom = (Session.getFullName() != null && !Session.getFullName().trim().isEmpty())
                    ? Session.getFullName() : "Patient #" + currentPatientId;
            if (lblBienvenue != null) lblBienvenue.setText("Hello " + nom + " ! ✨");
            updateScoresFromDB();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════
    // SCORES — LOGIQUE UNIFIÉE avec PassageQuizController
    //
    //   Bien-être : grand score = bien  → DIRECT
    //   Stress    : grand score = mal   → INVERSÉ  ✅
    //   Humeur    : grand score = bien  → DIRECT   ✅ (corrigé)
    // ══════════════════════════════════════════════════════════════
    private void updateScoresFromDB() {
        try {
            List<String> historique = serviceQuiz.getHistoriquePatient(currentPatientId);
            System.out.println("📋 " + historique.size() + " session(s) patient ID=" + currentPatientId);

            if (historique.isEmpty()) {
                if (scoreBienEtre != null) scoreBienEtre.setText("0/100");
                if (scoreStress   != null) scoreStress.setText("0/100");
                if (scoreHumeur   != null) scoreHumeur.setText("0/100");
                if (lblCoinsGagnes  != null) lblCoinsGagnes.setText("0 coins");
                if (lblSessionCount != null) lblSessionCount.setText("0 session");
                animerArc(arcBienEtre, 0, "#A78BFA");
                animerArc(arcStress,   0, "#FF6B9D");
                animerArc(arcHumeur,   0, "#4FACFE");
                setTrend(lblTrendBienEtre, 0, "#A78BFA");
                setTrend(lblTrendStress,   0, "#FF6B9D");
                setTrend(lblTrendHumeur,   0, "#4FACFE");
                if (lblConseil != null) lblConseil.setText("Commencez votre premier test ! 🚀");
                return;
            }

            final int SCORE_MAX = 6;
            List<Integer> scoresBE = new ArrayList<>();
            List<Integer> scoresST = new ArrayList<>();
            List<Integer> scoresHU = new ArrayList<>();

            // ✅ DEBUG — afficher les 5 premières lignes brutes
            System.out.println("🔎 Exemple lignes historique :");
            historique.stream().limit(5).forEach(l ->
                    System.out.println("   RAW: " + l));

            for (String ligne : historique) {
                int    score     = extraireScore(ligne);
                String titre     = extraireTitre(ligne).toLowerCase();
                int    scoreNorm = score > 6 ? score / 3 : score;

                System.out.println("  🔎 titre='" + titre
                        + "' score=" + score
                        + " scoreNorm=" + scoreNorm);

                if      (titre.contains("stress"))  scoresST.add(scoreNorm);
                else if (titre.contains("humeur"))  scoresHU.add(scoreNorm);
                else                                scoresBE.add(scoreNorm);
            }

            System.out.println("📊 BE:" + scoresBE.size() + " ST:" + scoresST.size() + " HU:" + scoresHU.size());
            System.out.println("📊 scoresBE=" + scoresBE);
            System.out.println("📊 scoresST=" + scoresST.subList(0, Math.min(5, scoresST.size())));
            System.out.println("📊 scoresHU=" + scoresHU);

            int totalGlobal = historique.stream().mapToInt(this::extraireScore).sum();
            int moyenneGlobale = totalGlobal / historique.size();

            int moyBE = scoresBE.isEmpty() ? 3 : (int) scoresBE.stream().mapToInt(i->i).average().orElse(3);
            int moyST = scoresST.isEmpty() ? 3 : (int) scoresST.stream().mapToInt(i->i).average().orElse(3);
            int moyHU = scoresHU.isEmpty() ? 3 : (int) scoresHU.stream().mapToInt(i->i).average().orElse(3);

            // ✅ Bien-être : DIRECT
            scoreBE = (int) Math.min(100, Math.max(0, (moyBE * 100.0) / SCORE_MAX));
            // ✅ Stress    : INVERSÉ
            scoreST = (int) Math.min(100, Math.max(0, (moyST * 100.0) / SCORE_MAX));
            // ✅ Humeur    : DIRECT (corrigé — plus d'inversion)
            scoreHU = (int) Math.min(100, Math.max(0, (moyHU * 100.0) / SCORE_MAX));

            System.out.println("📊 BE:" + scoreBE + "% ST:" + scoreST + "% HU:" + scoreHU + "%");

            // ✅ Tendances — HU non inversée, ST inversée, BE non inversée
            // ✅ Grand score = bien pour TOUS les tests → inverse=false partout
            int trendBE = calculerTendance(scoresBE, SCORE_MAX, false);
            int trendST = calculerTendance(scoresST, SCORE_MAX, false);
            int trendHU = calculerTendance(scoresHU, SCORE_MAX, false);

            if (scoreBienEtre != null) scoreBienEtre.setText(scoreBE + "/100");
            if (scoreStress   != null) scoreStress.setText(scoreST + "/100");
            if (scoreHumeur   != null) scoreHumeur.setText(scoreHU + "/100");

            animerArc(arcBienEtre, scoreBE, "#A78BFA");
            animerArc(arcStress,   scoreST, "#FF6B9D");
            animerArc(arcHumeur,   scoreHU, "#4FACFE");

            setTrend(lblTrendBienEtre, trendBE, "#A78BFA");
            setTrend(lblTrendStress,   trendST, "#FF6B9D");
            setTrend(lblTrendHumeur,   trendHU, "#4FACFE");

            // ✅ Calcul coins : sessions × 150 + score moyen × 10
            int coins = historique.size() * 150 + moyenneGlobale * 10;
            if (lblCoinsGagnes != null)
                lblCoinsGagnes.setText("🪙 " + coins + " coins");
            if (lblSessionCount != null) {
                int nb = historique.size();
                lblSessionCount.setText(nb + " session" + (nb > 1 ? "s" : "")
                        + " complétée" + (nb > 1 ? "s" : ""));
            }

            // ✅ Badges par seuil de coins
            afficherBadgesCoins(coins);

            afficherConseil(scoreBE, scoreST, scoreHU, historique.size());
            afficherHistorique(historique);

            // ✅ Mettre à jour avatar selon nouveaux scores
            if (prefsAvatar != null)
                Platform.runLater(this::mettreAJourAvatarUI);

        } catch (SQLException e) {
            System.err.println("❌ Scores : " + e.getMessage());
        }
    }

    private int calculerTendance(List<Integer> scores, int scoreMax, boolean inverse) {
        if (scores.size() < 2) return 0;
        int milieu = scores.size() / 2;
        double moyAncien = scores.subList(0, milieu).stream().mapToInt(i->i).average().orElse(0);
        double moyRecent = scores.subList(milieu, scores.size()).stream().mapToInt(i->i).average().orElse(0);
        double pctAncien = (moyAncien * 100.0) / scoreMax;
        double pctRecent = (moyRecent * 100.0) / scoreMax;
        if (inverse) { pctAncien = 100 - pctAncien; pctRecent = 100 - pctRecent; }
        int delta = (int) Math.round(pctRecent - pctAncien);
        return Math.max(-99, Math.min(99, delta));
    }

    // ══════════════════════════════════════════════════════════════
    // 🏅 SYSTÈME DE BADGES PAR COINS
    //
    //  Paliers :
    //    500  coins → 🧘 Gestion parfaite
    //   1500  coins → 🌤️ Stabilité émotionnelle
    //   3000  coins → 💪 Résilience
    //   5000  coins → 🔥 Mental fort
    //   8000  coins → 🌈 Progression continue
    // ══════════════════════════════════════════════════════════════
    private static final int[][] PALIERS = {
            { 500,  0},
            {1500,  1},
            {3000,  2},
            {5000,  3},
            {8000,  4}
    };
    private static final String[] BADGE_EMOJIS  = {"🧘","🌤️","💪","🔥","🌈"};
    private static final String[] BADGE_TITRES  = {
            "Gestion parfaite",
            "Stabilité émotionnelle",
            "Résilience",
            "Mental fort",
            "Progression continue"
    };
    private static final String[] BADGE_COULEURS = {
            "#6366f1","#0ea5e9","#f97316","#ef4444","#8b5cf6"
    };
    private static final String[] BADGE_BG = {
            "#ede9fe","#e0f2fe","#fff7ed","#fef2f2","#f5f3ff"
    };

    private void afficherBadgesCoins(int coins) {
        if (badgesBox == null) return;
        Platform.runLater(() -> {
            badgesBox.getChildren().clear();

            // ── Titre section ──────────────────────────────────────
            javafx.scene.layout.HBox titreLigne =
                    new javafx.scene.layout.HBox(10);
            titreLigne.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label ico = new Label("🏅");
            ico.setStyle("-fx-font-size:18px;");

            Label lblTitre = new Label("Mes Badges");
            lblTitre.setStyle(
                    "-fx-font-size:15px; -fx-font-weight:900;"
                            + "-fx-text-fill:#1e293b;");

            // Compteur badges débloqués
            long nbDebloque = java.util.Arrays.stream(PALIERS)
                    .filter(p -> coins >= p[0]).count();
            Label lblCount = new Label(nbDebloque + "/5");
            lblCount.setStyle(
                    "-fx-font-size:11px; -fx-font-weight:bold;"
                            + "-fx-text-fill:#6366f1;"
                            + "-fx-background-color:#ede9fe;"
                            + "-fx-background-radius:20;"
                            + "-fx-padding:3 10 3 10;");

            javafx.scene.layout.Pane sp = new javafx.scene.layout.Pane();
            javafx.scene.layout.HBox.setHgrow(sp,
                    javafx.scene.layout.Priority.ALWAYS);
            titreLigne.getChildren().addAll(ico, lblTitre, sp, lblCount);
            badgesBox.getChildren().add(titreLigne);

            // ── Barre progression globale ──────────────────────────
            int prochainSeuil = -1;
            for (int[] p : PALIERS) {
                if (coins < p[0]) { prochainSeuil = p[0]; break; }
            }
            int seuilPrecedent = 0;
            for (int[] p : PALIERS) {
                if (coins >= p[0]) seuilPrecedent = p[0];
            }
            double pctBarre = prochainSeuil > 0
                    ? (double)(coins - seuilPrecedent)
                    / (prochainSeuil - seuilPrecedent)
                    : 1.0;

            javafx.scene.layout.StackPane barreContainer =
                    new javafx.scene.layout.StackPane();
            barreContainer.setMaxWidth(Double.MAX_VALUE);
            barreContainer.setPrefHeight(8);
            barreContainer.setStyle(
                    "-fx-background-color:#f1f5f9;"
                            + "-fx-background-radius:4;");
            javafx.scene.layout.Pane barreFill =
                    new javafx.scene.layout.Pane();
            barreFill.setPrefHeight(8);
            String cBarre = nbDebloque >= 4 ? "#6366f1"
                    : nbDebloque >= 2 ? "#0ea5e9" : "#94a3b8";
            barreFill.setStyle(
                    "-fx-background-color:" + cBarre + ";"
                            + "-fx-background-radius:4;");
            javafx.scene.layout.StackPane.setAlignment(
                    barreFill, javafx.geometry.Pos.CENTER_LEFT);
            barreFill.setPrefWidth(0);
            barreContainer.getChildren().add(barreFill);
            badgesBox.getChildren().add(barreContainer);

            // Label prochain palier
            if (prochainSeuil > 0) {
                int reste = prochainSeuil - coins;
                Label lblProchain = new Label(
                        "🎯 Plus que " + reste + " coins pour le prochain badge !");
                lblProchain.setStyle(
                        "-fx-font-size:11px; -fx-text-fill:#64748b;"
                                + "-fx-font-style:italic;");
                badgesBox.getChildren().add(lblProchain);
            } else {
                Label lblMax = new Label("🏆 Tous les badges débloqués !");
                lblMax.setStyle(
                        "-fx-font-size:11px; -fx-font-weight:bold;"
                                + "-fx-text-fill:#6366f1;");
                badgesBox.getChildren().add(lblMax);
            }

            // ── Grille badges ──────────────────────────────────────
            javafx.scene.layout.TilePane grille =
                    new javafx.scene.layout.TilePane();
            grille.setHgap(10); grille.setVgap(10);
            grille.setPrefColumns(2);
            grille.setMaxWidth(Double.MAX_VALUE);

            for (int i = 0; i < PALIERS.length; i++) {
                boolean debloque = coins >= PALIERS[i][0];
                String couleur   = BADGE_COULEURS[i];
                String bgColor   = debloque ? BADGE_BG[i] : "#f8fafc";
                String borderCol = debloque ? couleur + "55" : "#e2e8f0";
                String opacity   = debloque ? "1.0" : "0.45";

                javafx.scene.layout.HBox item = new javafx.scene.layout.HBox(10);
                item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                item.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
                item.setStyle(
                        "-fx-background-color:" + bgColor + ";"
                                + "-fx-background-radius:14;"
                                + "-fx-border-color:" + borderCol + ";"
                                + "-fx-border-radius:14;"
                                + "-fx-border-width:1.5;"
                                + "-fx-opacity:" + opacity + ";");

                // Cercle emoji
                javafx.scene.layout.StackPane cercle =
                        new javafx.scene.layout.StackPane();
                cercle.setMinSize(44, 44); cercle.setMaxSize(44, 44);
                String circleBg = debloque
                        ? couleur + "22" : "#e2e8f0";
                cercle.setStyle(
                        "-fx-background-color:" + circleBg + ";"
                                + "-fx-background-radius:22;");
                Label lblEmoji = new Label(
                        debloque ? BADGE_EMOJIS[i] : "🔒");
                lblEmoji.setStyle("-fx-font-size:22px;");
                cercle.getChildren().add(lblEmoji);

                // Textes
                javafx.scene.layout.VBox textes =
                        new javafx.scene.layout.VBox(2);
                Label lblNom = new Label(BADGE_TITRES[i]);
                lblNom.setStyle(
                        "-fx-font-size:12px; -fx-font-weight:900;"
                                + "-fx-text-fill:" + (debloque
                                ? couleur : "#94a3b8") + ";");
                // Seuil en coins
                Label lblSeuil = new Label(
                        debloque
                                ? "✅ Débloqué — " + PALIERS[i][0] + " coins"
                                : "🔐 " + PALIERS[i][0] + " coins requis");
                lblSeuil.setStyle(
                        "-fx-font-size:10px;"
                                + "-fx-text-fill:" + (debloque
                                ? "#64748b" : "#94a3b8") + ";");
                textes.getChildren().addAll(lblNom, lblSeuil);

                // Badge débloqué → petite animation pulse
                if (debloque) {
                    javafx.animation.ScaleTransition pulse =
                            new javafx.animation.ScaleTransition(
                                    javafx.util.Duration.millis(600), cercle);
                    pulse.setFromX(0.8); pulse.setFromY(0.8);
                    pulse.setToX(1.0);   pulse.setToY(1.0);
                    pulse.setInterpolator(
                            javafx.animation.Interpolator.EASE_OUT);
                    pulse.play();
                }

                item.getChildren().addAll(cercle, textes);
                grille.getChildren().add(item);
            }

            badgesBox.getChildren().add(grille);

            // Animation barre après layout
            Platform.runLater(() -> {
                double maxW = barreContainer.getWidth() > 0
                        ? barreContainer.getWidth() : 340;
                javafx.animation.Timeline anim =
                        new javafx.animation.Timeline(
                                new javafx.animation.KeyFrame(
                                        javafx.util.Duration.ZERO,
                                        new javafx.animation.KeyValue(
                                                barreFill.prefWidthProperty(), 0)),
                                new javafx.animation.KeyFrame(
                                        javafx.util.Duration.millis(900),
                                        new javafx.animation.KeyValue(
                                                barreFill.prefWidthProperty(),
                                                maxW * pctBarre,
                                                javafx.animation.Interpolator.EASE_OUT)));
                anim.play();
            });
        });
    }

    // ══════════════════════════════════════════════════════════════
    // 🖼️ AVATAR — DiceBear API + Accompagnement émotionnel
    // ══════════════════════════════════════════════════════════════
    private void chargerAvatar() {
        if (currentPatientId <= 0) return;

        prefsAvatar = avatarService.chargerPrefs(currentPatientId);

        // Pseudo par défaut = nom session
        if (prefsAvatar.pseudo.startsWith("Patient #")
                && Session.getFullName() != null
                && !Session.getFullName().isBlank()) {
            prefsAvatar.pseudo = Session.getFullName();
        }

        mettreAJourAvatarUI();
    }

    private void mettreAJourAvatarUI() {
        if (imgAvatar == null || prefsAvatar == null) return;

        // ── Pseudo ────────────────────────────────────────────
        if (lblAvatarPseudo != null)
            lblAvatarPseudo.setText(prefsAvatar.pseudo);

        // ── État émotionnel adaptatif ──────────────────────────
        AvatarService.EtatEmotionnel etat =
                avatarService.calculerEtat(scoreBE, scoreST, scoreHU);

        if (lblAvatarEtat != null) {
            lblAvatarEtat.setText(etat.emoji + " " + etat.titre);
            lblAvatarEtat.setStyle(
                    "-fx-font-size:12px; -fx-font-weight:bold;"
                            + "-fx-text-fill:" + etat.couleur + ";"
                            + "-fx-background-color:" + etat.couleurBg + ";"
                            + "-fx-background-radius:20; -fx-padding:4 12;");
        }
        if (lblAvatarMessage != null) {
            lblAvatarMessage.setText(etat.message);
            lblAvatarMessage.setStyle(
                    "-fx-font-size:11px; -fx-text-fill:#64748b;"
                            + "-fx-wrap-text:true;");
        }
        if (avatarEtatBox != null) {
            avatarEtatBox.setStyle(
                    "-fx-background-color:" + etat.couleurBg + ";"
                            + "-fx-background-radius:14; -fx-padding:12;");
        }

        // ── Image avatar — URL émotionnelle ───────────────────
        String url = avatarService.getAvatarUrlEmotionnel(
                prefsAvatar, scoreBE, scoreST, scoreHU);

        new Thread(() -> {
            try {
                javafx.scene.image.Image img =
                        new javafx.scene.image.Image(
                                url, 120, 120, true, true, false);
                Platform.runLater(() -> {
                    if (!img.isError()) {
                        imgAvatar.setImage(img);
                        imgAvatar.setFitWidth(120);
                        imgAvatar.setFitHeight(120);

                        // Animation apparition
                        javafx.animation.FadeTransition ft =
                                new javafx.animation.FadeTransition(
                                        javafx.util.Duration.millis(500),
                                        imgAvatar);
                        ft.setFromValue(0); ft.setToValue(1);
                        ft.play();

                        javafx.animation.ScaleTransition st2 =
                                new javafx.animation.ScaleTransition(
                                        javafx.util.Duration.millis(500),
                                        imgAvatar);
                        st2.setFromX(0.6); st2.setFromY(0.6);
                        st2.setToX(1.0);   st2.setToY(1.0);
                        st2.setInterpolator(
                                javafx.animation.Interpolator.EASE_OUT);
                        st2.play();
                    }
                    System.out.println("✅ Avatar chargé : " + prefsAvatar.pseudo);
                });
            } catch (Exception e) {
                System.err.println("❌ Avatar : " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void personnaliserAvatar() {
        AvatarPersonnalisationController.ouvrir(
                currentPatientId,
                avatarService,
                () -> {
                    // Recharger après sauvegarde
                    prefsAvatar = avatarService.chargerPrefs(
                            currentPatientId);
                    mettreAJourAvatarUI();
                }
        );
    }

    private void afficherRecommandationTest() {
        new Thread(() -> {
            String test   = serviceGroq.recommanderProchainTest(scoreBE, scoreST, scoreHU);
            String raison = serviceGroq.getRaisonRecommandation(scoreBE, scoreST, scoreHU);
            testRecommande = test;
            Platform.runLater(() -> {
                System.out.println("🎯 " + test + " — " + raison);
                if (lblTestRecommande != null)
                    lblTestRecommande.setText("🎯 " + test + " recommandé\n" + raison);
                if (btnPasserTestRecommande != null) {
                    btnPasserTestRecommande.setText("▶ Passer " + test);
                    btnPasserTestRecommande.setVisible(true);
                    btnPasserTestRecommande.setManaged(true);
                }
            });
        }).start();
    }

    private void afficherConseil(int be, int st, int hu, int nbSessions) {
        if (lblConseil == null) return;
        lblConseil.setText("🤖 Conseil personnalisé en cours...");
        if (lblTitre1 != null) lblTitre1.setText("Chargement...");
        if (lblTitre2 != null) lblTitre2.setText("Chargement...");
        if (lblTitre3 != null) lblTitre3.setText("Chargement...");
        new Thread(() -> {
            String         conseil = serviceGroq.genererConseil(be, st, hu, nbSessions);
            List<String[]> trois   = serviceGroq.genererTroisConseils(be, st, hu);
            Platform.runLater(() -> {
                if (lblConseil != null)
                    lblConseil.setText(conseil != null ? conseil : "Continuez vos efforts !");
                if (trois != null && trois.size() > 0 && trois.get(0).length >= 3) {
                    if (lblEmoji1 != null) lblEmoji1.setText(trois.get(0)[0]);
                    if (lblTitre1 != null) lblTitre1.setText(trois.get(0)[1]);
                    if (lblDesc1  != null) lblDesc1.setText(trois.get(0)[2]);
                }
                if (trois != null && trois.size() > 1 && trois.get(1).length >= 3) {
                    if (lblEmoji2 != null) lblEmoji2.setText(trois.get(1)[0]);
                    if (lblTitre2 != null) lblTitre2.setText(trois.get(1)[1]);
                    if (lblDesc2  != null) lblDesc2.setText(trois.get(1)[2]);
                }
                if (trois != null && trois.size() > 2 && trois.get(2).length >= 3) {
                    if (lblEmoji3 != null) lblEmoji3.setText(trois.get(2)[0]);
                    if (lblTitre3 != null) lblTitre3.setText(trois.get(2)[1]);
                    if (lblDesc3  != null) lblDesc3.setText(trois.get(2)[2]);
                }
            });
        }).start();
    }

    // ══════════════════════════════════════════════════════════════
    // HISTORIQUE — logique unifiée
    //   Stress    → INVERSÉ
    //   Bien-être → DIRECT
    //   Humeur    → DIRECT  ✅ (corrigé)
    // ══════════════════════════════════════════════════════════════
    private void afficherHistorique(List<String> historique) {
        if (historyBox == null) return;
        historyBox.getChildren().clear();
        int debut = Math.max(0, historique.size() - 5);
        for (int i = historique.size() - 1; i >= debut; i--) {
            String ligne     = historique.get(i);
            int    scoreBrut = extraireScore(ligne);
            String date      = extraireDate(ligne);
            String titre     = extraireTitre(ligne);
            String titreLow  = titre.toLowerCase();
            int    scorePct;

            // ✅ Grand score = bien pour TOUS les tests
            scorePct = (int) Math.min(100, (scoreBrut * 100.0) / 6);

            String emoji = scorePct >= 70 ? "✅" : scorePct >= 40 ? "🟡" : "🔴";
            Label entry = new Label(date + "  ·  " + titre
                    + "  →  " + scorePct + "/100  " + emoji);
            entry.setStyle("-fx-font-size:12px; -fx-text-fill:#374151;"
                    + "-fx-font-weight:600; -fx-padding:6 0;");
            historyBox.getChildren().add(entry);
            historyBox.getChildren().add(new javafx.scene.control.Separator());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // GRAPHIQUE — logique unifiée
    //   Stress    → INVERSÉ
    //   Bien-être → DIRECT
    //   Humeur    → DIRECT  ✅ (corrigé)
    // ══════════════════════════════════════════════════════════════
    private void chargerGraphiqueReel() {
        if (evolutionChart == null) return;
        evolutionChart.getData().clear();
        evolutionChart.setAnimated(true);
        try {
            List<String> historique = serviceQuiz.getHistoriquePatient(currentPatientId);
            if (historique.isEmpty()) return;

            XYChart.Series<String, Number> seriesBE = new XYChart.Series<>(); seriesBE.setName("Bien-être");
            XYChart.Series<String, Number> seriesST = new XYChart.Series<>(); seriesST.setName("Stress");
            XYChart.Series<String, Number> seriesHU = new XYChart.Series<>(); seriesHU.setName("Humeur");

            LocalDateTime limite = LocalDateTime.now().minusDays(getSelectedDays());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

            for (String ligne : historique) {
                LocalDateTime dt = extraireDateTime(ligne);
                if (dt.isBefore(limite)) continue;
                int    score = extraireScore(ligne);
                String date  = dt.format(fmt);
                String titre = extraireTitre(ligne).toLowerCase();
                int vBE, vST, vHU;

                if (titre.contains("stress")) {
                    // ✅ Grand score = bien géré → DIRECT
                    vST = (int) Math.min(100, (score * 100.0) / 6);
                    vBE = Math.min(100, vST + 10);
                    vHU = Math.min(100, vST + 5);
                } else if (titre.contains("humeur")) {
                    // ✅ DIRECT
                    vHU = (int) Math.min(100, (score * 100.0) / 6);
                    vBE = Math.min(100, vHU + 8);
                    vST = Math.max(0, vHU - 10);
                } else {
                    // ✅ DIRECT
                    vBE = (int) Math.min(100, (score * 100.0) / 6);
                    vST = Math.max(0, vBE - 5);
                    vHU = Math.min(100, vBE - 5);
                }

                seriesBE.getData().add(new XYChart.Data<>(date, vBE));
                seriesST.getData().add(new XYChart.Data<>(date, vST));
                seriesHU.getData().add(new XYChart.Data<>(date, vHU));
            }

            evolutionChart.getData().addAll(seriesBE, seriesST, seriesHU);
            Platform.runLater(this::applyStyling);

        } catch (SQLException e) {
            System.err.println("❌ Graphique : " + e.getMessage());
        }
    }

    @FXML private void testerGroq() {
        setResultat("⏳ Test Groq en cours...", "#78350F", "#FEF3C7");
        new Thread(() -> {
            try {
                String conseil = serviceGroq.genererConseil(scoreBE, scoreST, scoreHU, 5);
                String test    = serviceGroq.recommanderProchainTest(scoreBE, scoreST, scoreHU);
                Platform.runLater(() -> {
                    if (conseil != null && !conseil.isEmpty())
                        setResultat("✅ GROQ OK !\n💬 " + conseil + "\n🎯 " + test, "#065F46", "#ECFDF5");
                    else
                        setResultat("❌ Groq — réponse vide", "#991B1B", "#FEF2F2");
                });
            } catch (Exception e) {
                Platform.runLater(() -> setResultat("❌ " + e.getMessage(), "#991B1B", "#FEF2F2"));
            }
        }).start();
    }

    @FXML private void testerEmail() {
        setResultat("⏳ Envoi email...", "#78350F", "#FEF3C7");
        new Thread(() -> {
            try {
                new services.ServiceEmail().envoyerEmail(
                        "mindcare.notifications@gmail.com", "🧪 Test MindCare", "<h2>✅ Email OK !</h2>");
                Platform.runLater(() -> setResultat("✅ EMAIL OK !\n📬 Vérifie ta boîte !", "#065F46", "#ECFDF5"));
            } catch (Exception e) {
                Platform.runLater(() -> setResultat("❌ " + e.getMessage(), "#991B1B", "#FEF2F2"));
            }
        }).start();
    }

    private void setResultat(String texte, String cTexte, String cFond) {
        if (lblTestResultat == null) return;
        lblTestResultat.setText(texte);
        lblTestResultat.setStyle("-fx-font-size:11px; -fx-font-weight:600;"
                + "-fx-text-fill:" + cTexte + "; -fx-background-color:" + cFond + ";"
                + "-fx-background-radius:8; -fx-padding:10;");
    }

    @FXML private void ouvrirChat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/chatquiz.fxml"));
            Node view = loader.load();
            VBox contentArea = (VBox) lblBienvenue.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void ouvrirEspacePraticien() {
        var role = Session.getRoleConnecte();
        if (role != Session.Role.PSYCHOLOGUE && role != Session.Role.ADMIN) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EspacePraticien.fxml"));
            Node view = loader.load();
            VBox contentArea = (VBox) lblBienvenue.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML public void retourSuivie() {
        if (parentController != null) parentController.loadAccueil();
    }

    @FXML private void passerTestRecommande() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PasserTests.fxml"));
            Node view = loader.load();
            VBox contentArea = (VBox) lblBienvenue.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void animerArc(Arc arc, int score, String couleurHex) {
        if (arc == null) return;
        double targetLength = -(score / 100.0) * 360.0;
        arc.setStroke(Color.web(couleurHex));
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setFill(Color.TRANSPARENT);
        final int[] frame = {0};
        new javafx.animation.AnimationTimer() {
            @Override public void handle(long now) {
                frame[0]++;
                double p = Math.min(1.0, frame[0] / 60.0);
                arc.setLength(targetLength * (1 - Math.pow(1 - p, 3)));
                if (frame[0] >= 60) stop();
            }
        }.start();
    }

    private void setTrend(Label lbl, int delta, String couleur) {
        if (lbl == null) return;
        String signe  = delta > 0 ? "↑ +" : delta < 0 ? "↓ " : "→ ";
        String tFill  = delta >= 0 ? "#065F46" : "#9D174D";
        String bgFill = delta >= 0 ? "rgba(16,185,129,0.1)" : "rgba(239,68,68,0.1)";
        lbl.setText(signe + delta + "%");
        lbl.setStyle("-fx-font-size:11px; -fx-font-weight:700;"
                + "-fx-text-fill:" + tFill + "; -fx-background-color:" + bgFill + ";"
                + "-fx-background-radius:20; -fx-padding:3 10 3 10;");
    }

    private void applyStyling() {
        if (evolutionChart == null) return;
        evolutionChart.setStyle("-fx-background-color:transparent;");
        styleArea(evolutionChart, 0, "rgba(167,139,250,0.25)", "#A78BFA");
        styleArea(evolutionChart, 1, "rgba(255,107,157,0.2)",  "#FF6B9D");
        styleArea(evolutionChart, 2, "rgba(79,172,254,0.2)",   "#4FACFE");
        Node plot = evolutionChart.lookup(".chart-plot-background");
        if (plot != null) plot.setStyle("-fx-background-color:#FAFBFC;");
    }

    private void styleArea(AreaChart<?, ?> chart, int index,
                           String fillColor, String strokeColor) {
        Node fill = chart.lookup(".default-color" + index + ".chart-series-area-fill");
        Node line = chart.lookup(".default-color" + index + ".chart-series-area-line");
        if (fill != null) fill.setStyle("-fx-fill:" + fillColor + ";");
        if (line != null) line.setStyle("-fx-stroke:" + strokeColor + "; -fx-stroke-width:2.5px;");
        chart.lookupAll(".default-color" + index + ".chart-area-symbol")
                .forEach(n -> n.setStyle("-fx-background-color:" + strokeColor + ",white;"));
    }

    private int extraireScore(String ligne) {
        try {
            int s = ligne.indexOf("Score: ") + 7;
            int e = ligne.indexOf(" |", s);
            return Integer.parseInt(ligne.substring(s, e).trim());
        } catch (Exception e) { return 0; }
    }

    private LocalDateTime extraireDateTime(String ligne) {
        try {
            int s = ligne.indexOf("Date: ") + 6;
            return LocalDateTime.parse(ligne.substring(s).trim());
        } catch (Exception e) { return LocalDateTime.now(); }
    }

    private String extraireDate(String ligne) {
        try { return extraireDateTime(ligne).format(DateTimeFormatter.ofPattern("dd/MM")); }
        catch (Exception e) { return "--/--"; }
    }

    private String extraireTitre(String ligne) {
        try {
            int s = ligne.indexOf("Quiz: ") + 6;
            int e = ligne.indexOf(" |", s);
            return ligne.substring(s, e).trim();
        } catch (Exception e) { return "Quiz"; }
    }

    private void configurerCombo() {
        if (comboPeriode == null) return;
        comboPeriode.getItems().addAll("7 jours", "30 jours", "90 jours");
        comboPeriode.getSelectionModel().select("30 jours");
        comboPeriode.setOnAction(e -> chargerGraphiqueReel());
    }

    private int getSelectedDays() {
        if (comboPeriode == null) return 30;
        switch (comboPeriode.getSelectionModel().getSelectedItem()) {
            case "7 jours":  return 7;
            case "90 jours": return 90;
            default:         return 30;
        }
    }

    @FXML private void chargerMusique() {
        if (btnChargerMusique != null) { btnChargerMusique.setText("⏳ Chargement..."); btnChargerMusique.setDisable(true); }
        if (lblChargementPistes != null) lblChargementPistes.setText("🤖 IA analyse votre état...");
        String nom = Session.getFullName() != null ? Session.getFullName() : "Patient";
        new Thread(() -> {
            ServiceMusique.MusiqueParams params = serviceMusique.calculerParams(scoreBE, scoreST, scoreHU, nom);
            List<ServiceMusique.Piste> pistes   = serviceMusique.chercherPistes(params);
            pistesChargees = pistes; pisteActuelle = 0;
            Platform.runLater(() -> {
                if (lblMusiqueMessage != null) lblMusiqueMessage.setText(params.message + " • BPM: " + params.bpm);
                afficherListePistes(pistes);
                if (btnChargerMusique != null) { btnChargerMusique.setText("🔄 Recharger"); btnChargerMusique.setDisable(false); }
                if (!pistes.isEmpty()) jouerPiste(pistes.get(0));
            });
        }).start();
    }

    private void afficherListePistes(List<ServiceMusique.Piste> pistes) {
        if (listePistesBox == null) return;
        listePistesBox.getChildren().removeIf(n -> n.getUserData() != null && n.getUserData().equals("piste"));
        if (lblChargementPistes != null) lblChargementPistes.setVisible(false);
        for (int i = 0; i < pistes.size(); i++) {
            ServiceMusique.Piste piste = pistes.get(i);
            final int idx = i;
            HBox ligne = new HBox(10);
            ligne.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ligne.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
            ligne.setUserData("piste");
            ligne.setStyle("-fx-background-color:" + (i == pisteActuelle ? "rgba(124,58,237,0.10)" : "transparent")
                    + "; -fx-background-radius:10; -fx-cursor:hand;");
            Label emoji = new Label(piste.emoji); emoji.setStyle("-fx-font-size:16px;");
            VBox infos = new VBox(2); HBox.setHgrow(infos, javafx.scene.layout.Priority.ALWAYS);
            Label nomLbl = new Label(piste.nom); nomLbl.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#1F2937;");
            Label dureeLbl = new Label(piste.duree); dureeLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#9CA3AF;");
            infos.getChildren().addAll(nomLbl, dureeLbl);
            Label playIco = new Label(i == pisteActuelle && enLecture ? "▶" : "○");
            playIco.setStyle("-fx-font-size:14px; -fx-text-fill:#7C3AED;");
            ligne.getChildren().addAll(emoji, infos, playIco);
            ligne.setOnMouseClicked(e -> { pisteActuelle = idx; jouerPiste(piste); afficherListePistes(pistesChargees); });
            ligne.setOnMouseEntered(ev -> ligne.setStyle("-fx-background-color:rgba(124,58,237,0.08); -fx-background-radius:10; -fx-cursor:hand;"));
            ligne.setOnMouseExited(ev  -> ligne.setStyle("-fx-background-color:" + (idx == pisteActuelle ? "rgba(124,58,237,0.10)" : "transparent") + "; -fx-background-radius:10; -fx-cursor:hand;"));
            listePistesBox.getChildren().add(ligne);
        }
    }

    private void jouerPiste(ServiceMusique.Piste piste) {
        if (lblPisteNom   != null) lblPisteNom.setText(piste.nom);
        if (lblPisteDuree != null) lblPisteDuree.setText(piste.duree);
        if (lblPisteEmoji != null) lblPisteEmoji.setText(piste.emoji);
        if (lblEtatLecture!= null) lblEtatLecture.setText("▶");
        if (btnPlayPause  != null) btnPlayPause.setText("⏸");
        if (piste.url == null) { if (lblPisteNom != null) lblPisteNom.setText(piste.nom + " (aperçu non disponible)"); enLecture = false; return; }
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
        try {
            javafx.scene.media.Media media = new javafx.scene.media.Media(piste.url);
            mediaPlayer = new javafx.scene.media.MediaPlayer(media);
            double vol = sliderVolume != null ? sliderVolume.getValue() : 0.8;
            mediaPlayer.setVolume(vol);
            mediaPlayer.setOnEndOfMedia(() -> {
                pisteActuelle = (pisteActuelle + 1) % pistesChargees.size();
                Platform.runLater(() -> { jouerPiste(pistesChargees.get(pisteActuelle)); afficherListePistes(pistesChargees); });
            });
            mediaPlayer.play(); enLecture = true;
            System.out.println("▶️ Lecture : " + piste.nom);
        } catch (Exception e) {
            System.err.println("❌ Lecture : " + e.getMessage());
            if (lblPisteNom != null) lblPisteNom.setText(piste.nom + " ⚠️");
            enLecture = false;
        }
    }

    @FXML private void togglePlayPause() {
        if (mediaPlayer == null) { if (!pistesChargees.isEmpty()) jouerPiste(pistesChargees.get(pisteActuelle)); return; }
        if (enLecture) { mediaPlayer.pause(); enLecture = false; if (btnPlayPause != null) btnPlayPause.setText("▶"); if (lblEtatLecture != null) lblEtatLecture.setText("⏸"); }
        else           { mediaPlayer.play();  enLecture = true;  if (btnPlayPause != null) btnPlayPause.setText("⏸"); if (lblEtatLecture != null) lblEtatLecture.setText("▶"); }
    }

    @FXML private void pisteSuivante() {
        if (pistesChargees.isEmpty()) return;
        pisteActuelle = (pisteActuelle + 1) % pistesChargees.size();
        jouerPiste(pistesChargees.get(pisteActuelle)); afficherListePistes(pistesChargees);
    }

    @FXML private void pistePrecedente() {
        if (pistesChargees.isEmpty()) return;
        pisteActuelle = (pisteActuelle - 1 + pistesChargees.size()) % pistesChargees.size();
        jouerPiste(pistesChargees.get(pisteActuelle)); afficherListePistes(pistesChargees);
    }

    @FXML private void stopMusique() {
        if (mediaPlayer != null) { mediaPlayer.stop(); enLecture = false; }
        if (btnPlayPause  != null) btnPlayPause.setText("▶");
        if (lblEtatLecture!= null) lblEtatLecture.setText("⏹");
        if (lblPisteNom   != null) lblPisteNom.setText("Aucune piste en lecture");
    }
}