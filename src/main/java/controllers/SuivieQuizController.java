package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;
import services.*;
import services.AvatarService.*;
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SuivieQuizController {

    private MindCareLayoutController parentController;
    private static SuivieQuizController instance;
    private int currentPatientId = -1;

    // ── Scores ─────────────────────────────────────────────────
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
    @FXML private VBox    badgesBox;
    @FXML private Button  btnEspacePsy;
    @FXML private Label   lblTestResultat;

    // ── Conseils ───────────────────────────────────────────────
    @FXML private Label lblEmoji1; @FXML private Label lblTitre1; @FXML private Label lblDesc1;
    @FXML private Label lblEmoji2; @FXML private Label lblTitre2; @FXML private Label lblDesc2;
    @FXML private Label lblEmoji3; @FXML private Label lblTitre3; @FXML private Label lblDesc3;

    // ── Avatar ─────────────────────────────────────────────────
    @FXML private javafx.scene.image.ImageView imgAvatar;
    @FXML private Label  lblAvatarPseudo;
    @FXML private Label  lblAvatarEtat;
    @FXML private Label  lblAvatarMessage;
    @FXML private Button btnPersonnaliserAvatar;
    @FXML private Button btnVoixAvatar;
    @FXML private VBox   avatarEtatBox;

    // ── Musique ────────────────────────────────────────────────
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

    // ── Services ───────────────────────────────────────────────
    private final ServiceQuiz         serviceQuiz         = new ServiceQuiz();
    private final ServiceGroqQuiz     serviceGroqQuiz     = new ServiceGroqQuiz();
    private final ServiceMusiqueQuiz  serviceMusiqueQuiz  = new ServiceMusiqueQuiz();
    private final AvatarService       avatarService       = new AvatarService();

    // ── État ───────────────────────────────────────────────────
    private PrefsAvatar  prefsAvatar;
    private int scoreBE = 0, scoreST = 0, scoreHU = 0;
    private EtatDetecte  dernierEtatPrononce = null;

    static boolean rappelDejaVerifie = false;
    private Animation animationAvatar;

    // ── Musique ────────────────────────────────────────────────
    private javafx.scene.media.MediaPlayer     mediaPlayer;
    private List<ServiceMusiqueQuiz.Piste>     pistesChargees = new ArrayList<>();
    private int     pisteActuelle = 0;
    private boolean enLecture     = false;

    // ══════════════════════════════════════════════════════════════
    // Rafraîchissement statique
    // ══════════════════════════════════════════════════════════════
    public static void rafraichir() {
        if (instance != null) Platform.runLater(() -> {
            instance.updateScoresFromDB();
            instance.chargerGraphiqueReel();
        });
    }

    // ══════════════════════════════════════════════════════════════
    // INITIALIZE
    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        instance = this;
        Session.Role role = Session.getRoleConnecte();
        currentPatientId  = Session.getUserId();

        if (btnEspacePsy != null) {
            boolean v = role == Session.Role.PSYCHOLOGUE || role == Session.Role.ADMIN;
            btnEspacePsy.setVisible(v);
            btnEspacePsy.setManaged(v);
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
                    (obs, o, n) -> { if (mediaPlayer != null) mediaPlayer.setVolume(n.doubleValue()); });

        mettreAJourBoutonVoix();
        configurerCombo();
        loadPatientData();
        chargerGraphiqueReel();
        chargerAvatar();

        if (!rappelDejaVerifie) {
            rappelDejaVerifie = true;
            new Thread(() -> {
                try { new ServiceRappelQuiz().verifierEtEnvoyerRappels(); }
                catch (Exception e) { System.err.println("Rappels : " + e.getMessage()); }
            }).start();
        }
    }

    public void setParentController(MindCareLayoutController p) { this.parentController = p; }

    // ══════════════════════════════════════════════════════════════
    // BOUTON VOIX AVATAR  🔊 / 🔇
    // ══════════════════════════════════════════════════════════════
    @FXML
    public void toggleVoixAvatar() {
        ServiceVoixQuiz.setVoixActive(!ServiceVoixQuiz.isVoixActive());
        mettreAJourBoutonVoix();
    }

    private void mettreAJourBoutonVoix() {
        if (btnVoixAvatar == null) return;
        if (ServiceVoixQuiz.isVoixActive()) {
            btnVoixAvatar.setText("🔊");
            btnVoixAvatar.setStyle(
                    "-fx-background-color:#ede9fe; -fx-text-fill:#7c3aed;"
                            + "-fx-background-radius:20; -fx-font-size:13px; -fx-cursor:hand;"
                            + "-fx-border-color:#c4b5fd; -fx-border-radius:20;"
                            + "-fx-border-width:1.5; -fx-padding:5 10;");
            btnVoixAvatar.setTooltip(new Tooltip("Voix avatar activée — cliquer pour désactiver"));
        } else {
            btnVoixAvatar.setText("🔇");
            btnVoixAvatar.setStyle(
                    "-fx-background-color:#f1f5f9; -fx-text-fill:#94a3b8;"
                            + "-fx-background-radius:20; -fx-font-size:13px; -fx-cursor:hand;"
                            + "-fx-border-color:#e2e8f0; -fx-border-radius:20;"
                            + "-fx-border-width:1.5; -fx-padding:5 10;");
            btnVoixAvatar.setTooltip(new Tooltip("Voix avatar désactivée — cliquer pour activer"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DONNÉES
    // ══════════════════════════════════════════════════════════════
    private void loadPatientData() {
        try {
            String nom = (Session.getFullName() != null && !Session.getFullName().trim().isEmpty())
                    ? Session.getFullName() : "Patient #" + currentPatientId;
            if (lblBienvenue != null) lblBienvenue.setText("Hello " + nom + " ! ✨");
            updateScoresFromDB();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ EXTRACTION — Score, Max, Date, Titre  (identique SuivieController)
    // ══════════════════════════════════════════════════════════════
    private int extraireScore(String ligne) {
        try {
            int s = ligne.indexOf("Score: ") + 7;
            int e = ligne.indexOf(" |", s);
            return Integer.parseInt(ligne.substring(s, e).trim());
        } catch (Exception e) { return 0; }
    }

    /**
     * ✅ Lit le score_max calculé depuis la DB et injecté dans la ligne.
     * Format attendu : "... | Max: 18 | Date: ..."
     * Fallback = 6 si absent (anciennes lignes sans Max).
     */
    private int extraireScoreMax(String ligne) {
        try {
            int s = ligne.indexOf("Max: ") + 5;
            int e = ligne.indexOf(" |", s);
            int max = Integer.parseInt(ligne.substring(s, e).trim());
            return max > 0 ? max : 6;
        } catch (Exception e) {
            return 6; // fallback compatibilité
        }
    }

    /**
     * ✅ Normalise en ratio [0.0 – 1.0] avec le vrai max du quiz.
     *    score=6 / max=6  → 1.0
     *    score=6 / max=18 → 0.33
     *    score=0 / max=6  → 0.0
     */
    private double normaliserEnRatio(String ligne) {
        int score = extraireScore(ligne);
        int max   = extraireScoreMax(ligne);
        return Math.max(0.0, Math.min(1.0, (double) score / max));
    }

    private LocalDateTime extraireDateTime(String ligne) {
        try {
            int s = ligne.indexOf("Date: ") + 6;
            return LocalDateTime.parse(ligne.substring(s).trim());
        } catch (Exception e) { return LocalDateTime.now(); }
    }

    private String extraireDate(String ligne) {
        try {
            return extraireDateTime(ligne).format(DateTimeFormatter.ofPattern("dd/MM"));
        } catch (Exception e) { return "--/--"; }
    }

    private String extraireTitre(String ligne) {
        try {
            int s = ligne.indexOf("Quiz: ") + 6;
            int e = ligne.indexOf(" |", s);
            return ligne.substring(s, e).trim();
        } catch (Exception e) { return "Quiz"; }
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ SCORES + CERCLES  — normalisation ratio dynamique
    // ══════════════════════════════════════════════════════════════
    private void updateScoresFromDB() {
        try {
            List<String> hist = serviceQuiz.getHistoriquePatient(currentPatientId);

            System.out.println("📋 " + hist.size()
                    + " session(s) pour patient ID=" + currentPatientId);

            if (hist.isEmpty()) {
                if (scoreBienEtre   != null) scoreBienEtre.setText("0/100");
                if (scoreStress     != null) scoreStress.setText("0/100");
                if (scoreHumeur     != null) scoreHumeur.setText("0/100");
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

            // ✅ Stocker les RATIOS (0.0–1.0) — le max vient directement de la ligne
            List<Double> ratiosBE = new ArrayList<>();
            List<Double> ratiosST = new ArrayList<>();
            List<Double> ratiosHU = new ArrayList<>();

            for (String ligne : hist) {
                String titre = extraireTitre(ligne).toLowerCase();
                double ratio = normaliserEnRatio(ligne);

                System.out.println("📌 " + titre
                        + " | score=" + extraireScore(ligne)
                        + " | max="   + extraireScoreMax(ligne)
                        + " | ratio=" + String.format("%.2f", ratio));

                if      (titre.contains("stress")) ratiosST.add(ratio);
                else if (titre.contains("humeur")) ratiosHU.add(ratio);
                else                               ratiosBE.add(ratio);
            }

            System.out.println("📊 Sessions — BE:" + ratiosBE.size()
                    + " ST:" + ratiosST.size() + " HU:" + ratiosHU.size());

            // Ratio neutre 0.5 (50%) si aucune donnée pour la catégorie
            double moyRatioBE = ratiosBE.isEmpty() ? 0.5
                    : ratiosBE.stream().mapToDouble(d -> d).average().orElse(0.5);
            double moyRatioST = ratiosST.isEmpty() ? 0.5
                    : ratiosST.stream().mapToDouble(d -> d).average().orElse(0.5);
            double moyRatioHU = ratiosHU.isEmpty() ? 0.5
                    : ratiosHU.stream().mapToDouble(d -> d).average().orElse(0.5);

            // ✅ Bien-être  : ratio élevé = bon  → score élevé
            scoreBE = (int) Math.round(moyRatioBE * 100);
            // ✅ Stress     : ratio élevé = mauvais → inverser
            scoreST = (int) Math.round((1.0 - moyRatioST) * 100);
            // ✅ Humeur     : ratio élevé = mauvais → inverser
            scoreHU = (int) Math.round((1.0 - moyRatioHU) * 100);

            // Clamp 0–100
            scoreBE = Math.max(0, Math.min(100, scoreBE));
            scoreST = Math.max(0, Math.min(100, scoreST));
            scoreHU = Math.max(0, Math.min(100, scoreHU));

            System.out.println("📊 Scores finaux — BE:"
                    + scoreBE + "% ST:" + scoreST + "% HU:" + scoreHU + "%");

            // ✅ Tendances sur ratios
            int trendBE = calculerTendanceRatio(ratiosBE, false);
            int trendST = calculerTendanceRatio(ratiosST, true);
            int trendHU = calculerTendanceRatio(ratiosHU, true);

            if (scoreBienEtre != null) scoreBienEtre.setText(scoreBE + "/100");
            if (scoreStress   != null) scoreStress.setText(scoreST + "/100");
            if (scoreHumeur   != null) scoreHumeur.setText(scoreHU + "/100");

            animerArc(arcBienEtre, scoreBE, "#A78BFA");
            animerArc(arcStress,   scoreST, "#FF6B9D");
            animerArc(arcHumeur,   scoreHU, "#4FACFE");

            setTrend(lblTrendBienEtre, trendBE, "#A78BFA");
            setTrend(lblTrendStress,   trendST, "#FF6B9D");
            setTrend(lblTrendHumeur,   trendHU, "#4FACFE");

            // Coins : basé sur nb sessions + score moyen global
            int totalGlobal    = hist.stream().mapToInt(this::extraireScore).sum();
            int moyenneGlobale = totalGlobal / hist.size();
            int coins          = hist.size() * 150 + moyenneGlobale * 10;

            if (lblCoinsGagnes != null) lblCoinsGagnes.setText("🪙 " + coins + " coins");
            if (lblSessionCount != null) {
                int nb = hist.size();
                lblSessionCount.setText(nb + " session"
                        + (nb > 1 ? "s" : "") + " complétée" + (nb > 1 ? "s" : ""));
            }

            afficherBadgesCoins(coins);
            afficherConseil(scoreBE, scoreST, scoreHU, hist.size());
            afficherHistorique(hist);

            // ══ AVATAR DYNAMIQUE + VOIX ═══════════════════════
            if (prefsAvatar != null) {
                Platform.runLater(() -> {
                    mettreAJourEtatEtAnimation();
                    rafraichirImageEmotionnelle();
                });
            }

        } catch (SQLException e) { System.err.println("❌ Scores : " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ TENDANCE sur ratios [0.0–1.0]  (identique SuivieController)
    // ══════════════════════════════════════════════════════════════
    private int calculerTendanceRatio(List<Double> ratios, boolean inverse) {
        if (ratios.size() < 2) return 0;

        int    milieu    = ratios.size() / 2;
        double moyAncien = ratios.subList(0, milieu).stream()
                .mapToDouble(d -> d).average().orElse(0.5);
        double moyRecent = ratios.subList(milieu, ratios.size()).stream()
                .mapToDouble(d -> d).average().orElse(0.5);

        double pctAncien = moyAncien * 100.0;
        double pctRecent = moyRecent * 100.0;

        if (inverse) {
            pctAncien = 100.0 - pctAncien;
            pctRecent = 100.0 - pctRecent;
        }

        int delta = (int) Math.round(pctRecent - pctAncien);
        return Math.max(-99, Math.min(99, delta));
    }

    // ══════════════════════════════════════════════════════════════
    // AVATAR DYNAMIQUE
    // ══════════════════════════════════════════════════════════════
    private void chargerAvatar() {
        if (currentPatientId <= 0) return;
        prefsAvatar = avatarService.chargerPrefs(currentPatientId);

        String prenom = Session.getPrenom();
        if (prenom == null || prenom.isBlank()) prenom = Session.getFullName();
        if (prenom != null && !prenom.isBlank()) prefsAvatar.pseudo = prenom;
        else if (prefsAvatar.pseudo.isBlank() || prefsAvatar.pseudo.startsWith("Patient #"))
            prefsAvatar.pseudo = "Mon Avatar";

        if (lblAvatarPseudo != null) lblAvatarPseudo.setText(prefsAvatar.pseudo);
        mettreAJourEtatEtAnimation();
        rafraichirImageEmotionnelle();
    }

    private void mettreAJourEtatEtAnimation() {
        if (prefsAvatar == null) return;

        EtatDetecte etat = AvatarService.detecterEtat(scoreBE, scoreST, scoreHU);
        Expression  expr = avatarService.getExpressionCourante(scoreBE, scoreST, scoreHU);

        if (lblAvatarEtat != null) {
            lblAvatarEtat.setText(expr.emoji + " " + expr.titreReaction);
            lblAvatarEtat.setStyle(
                    "-fx-font-size:12px; -fx-font-weight:bold;"
                            + "-fx-text-fill:"        + expr.couleurBandeau   + ";"
                            + "-fx-background-color:" + expr.couleurBandeauBg + ";"
                            + "-fx-background-radius:20; -fx-padding:4 12;");
        }
        if (lblAvatarMessage != null) {
            lblAvatarMessage.setText(expr.messageReaction);
            lblAvatarMessage.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b; -fx-wrap-text:true;");
        }
        if (avatarEtatBox != null) {
            avatarEtatBox.setStyle(
                    "-fx-background-color:" + expr.couleurBandeauBg + ";"
                            + "-fx-background-radius:14; -fx-padding:12;");
        }
        if (imgAvatar != null) jouerAnimation(expr.animation);

        if (etat != dernierEtatPrononce) {
            dernierEtatPrononce = etat;
            ServiceVoixQuiz.parlerAvatar(expr.messageReaction);
        }
    }

    private void rafraichirImageEmotionnelle() {
        if (prefsAvatar == null) return;
        String url = avatarService.getAvatarUrlEmotionnel(prefsAvatar, scoreBE, scoreST, scoreHU);
        System.out.printf("🎭 Avatar → BE=%d ST=%d HU=%d | %s%n", scoreBE, scoreST, scoreHU, url);
        new Thread(() -> {
            try {
                javafx.scene.image.Image img =
                        new javafx.scene.image.Image(url, 120, 120, true, true, false);
                Platform.runLater(() -> {
                    if (!img.isError()) {
                        Expression expr = avatarService.getExpressionCourante(scoreBE, scoreST, scoreHU);
                        afficherImageAvecAnimationEmotionnelle(img, expr.animation);
                    } else {
                        System.err.println("❌ Image DiceBear en erreur : " + url);
                    }
                });
            } catch (Exception e) { System.err.println("❌ rafraichirImageEmotionnelle : " + e.getMessage()); }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════
    // ANIMATIONS
    // ══════════════════════════════════════════════════════════════
    private void jouerAnimation(AnimationType type) {
        if (imgAvatar == null) return;
        if (animationAvatar != null) {
            animationAvatar.stop();
            imgAvatar.setScaleX(1.0); imgAvatar.setScaleY(1.0);
            imgAvatar.setOpacity(1.0); imgAvatar.setRotate(0); imgAvatar.setTranslateX(0);
        }
        switch (type) {
            case RESPIRATION: {
                ScaleTransition st = new ScaleTransition(Duration.millis(2000), imgAvatar);
                st.setFromX(0.95); st.setFromY(0.95); st.setToX(1.05); st.setToY(1.05);
                st.setCycleCount(Animation.INDEFINITE); st.setAutoReverse(true);
                st.setInterpolator(Interpolator.EASE_BOTH); animationAvatar = st; st.play(); break;
            }
            case FONDU_DOUX: {
                FadeTransition ft = new FadeTransition(Duration.millis(2500), imgAvatar);
                ft.setFromValue(0.65); ft.setToValue(1.0);
                ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true);
                ft.setInterpolator(Interpolator.EASE_BOTH); animationAvatar = ft; ft.play(); break;
            }
            case SECOUSSE: {
                TranslateTransition tt = new TranslateTransition(Duration.millis(80), imgAvatar);
                tt.setFromX(0); tt.setToX(-8); tt.setCycleCount(6); tt.setAutoReverse(true);
                tt.setInterpolator(Interpolator.EASE_BOTH);
                tt.setOnFinished(e -> {
                    imgAvatar.setTranslateX(0);
                    ScaleTransition calm = new ScaleTransition(Duration.millis(3000), imgAvatar);
                    calm.setFromX(0.97); calm.setFromY(0.97); calm.setToX(1.03); calm.setToY(1.03);
                    calm.setCycleCount(Animation.INDEFINITE); calm.setAutoReverse(true);
                    calm.setInterpolator(Interpolator.EASE_BOTH); animationAvatar = calm; calm.play();
                });
                animationAvatar = tt; tt.play(); break;
            }
            case BOUNCE_JOIE: {
                ScaleTransition bounce = new ScaleTransition(Duration.millis(300), imgAvatar);
                bounce.setFromX(0.7); bounce.setFromY(0.7); bounce.setToX(1.15); bounce.setToY(1.15);
                bounce.setInterpolator(Interpolator.EASE_OUT);
                bounce.setOnFinished(e -> {
                    ScaleTransition ret = new ScaleTransition(Duration.millis(150), imgAvatar);
                    ret.setToX(1.0); ret.setToY(1.0); ret.setInterpolator(Interpolator.EASE_IN);
                    ret.setOnFinished(e2 -> {
                        RotateTransition rot = new RotateTransition(Duration.millis(1500), imgAvatar);
                        rot.setFromAngle(-4); rot.setToAngle(4);
                        rot.setCycleCount(Animation.INDEFINITE); rot.setAutoReverse(true);
                        rot.setInterpolator(Interpolator.EASE_BOTH); animationAvatar = rot; rot.play();
                    });
                    ret.play();
                });
                animationAvatar = bounce; bounce.play(); break;
            }
            case PULSE: {
                ScaleTransition pulse = new ScaleTransition(Duration.millis(800), imgAvatar);
                pulse.setFromX(1.0); pulse.setFromY(1.0); pulse.setToX(1.12); pulse.setToY(1.12);
                pulse.setCycleCount(4); pulse.setAutoReverse(true); pulse.setInterpolator(Interpolator.EASE_BOTH);
                pulse.setOnFinished(e -> {
                    imgAvatar.setScaleX(1.0); imgAvatar.setScaleY(1.0);
                    ScaleTransition doux = new ScaleTransition(Duration.millis(2000), imgAvatar);
                    doux.setFromX(0.98); doux.setFromY(0.98); doux.setToX(1.04); doux.setToY(1.04);
                    doux.setCycleCount(Animation.INDEFINITE); doux.setAutoReverse(true);
                    doux.setInterpolator(Interpolator.EASE_BOTH); animationAvatar = doux; doux.play();
                });
                animationAvatar = pulse; pulse.play(); break;
            }
            case STANDARD: {
                FadeTransition ft = new FadeTransition(Duration.millis(500), imgAvatar);
                ft.setFromValue(0); ft.setToValue(1); ft.setInterpolator(Interpolator.EASE_OUT);
                animationAvatar = ft; ft.play(); break;
            }
            default: animationAvatar = null; break;
        }
    }

    private void afficherImageAvecAnimationEmotionnelle(javafx.scene.image.Image img, AnimationType type) {
        imgAvatar.setImage(img); imgAvatar.setFitWidth(120); imgAvatar.setFitHeight(120);
        imgAvatar.setOpacity(0); imgAvatar.setScaleX(0.6); imgAvatar.setScaleY(0.6);
        FadeTransition  ft = new FadeTransition(Duration.millis(400), imgAvatar);
        ft.setToValue(1.0); ft.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition st = new ScaleTransition(Duration.millis(400), imgAvatar);
        st.setToX(1.0); st.setToY(1.0); st.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition appear = new ParallelTransition(ft, st);
        appear.setOnFinished(e -> jouerAnimation(type));
        appear.play();
    }

    @FXML
    private void personnaliserAvatar() {
        AvatarPersonnalisationController.ouvrir(currentPatientId, avatarService, () -> {
            prefsAvatar = avatarService.chargerPrefs(currentPatientId);
            String prenom = Session.getPrenom();
            if (prenom == null || prenom.isBlank()) prenom = Session.getFullName();
            if (prenom != null && !prenom.isBlank()) prefsAvatar.pseudo = prenom;
            if (lblAvatarPseudo != null) lblAvatarPseudo.setText(prefsAvatar.pseudo);
            dernierEtatPrononce = null;
            mettreAJourEtatEtAnimation();
            rafraichirImageEmotionnelle();
        });
    }

    // ══════════════════════════════════════════════════════════════
    // BADGES
    // ══════════════════════════════════════════════════════════════
    private static final int[][] PALIERS      = {{2000,0},{6000,1},{15000,2},{30000,3},{60000,4}};
    private static final String[] BADGE_EMOJIS   = {"🧘","🌤","💪","🔥","🌈"};
    private static final String[] BADGE_TITRES   = {"Gestion parfaite","Stabilité émotionnelle","Résilience","Mental fort","Progression continue"};
    private static final String[] BADGE_COULEURS = {"#6366f1","#0ea5e9","#f97316","#ef4444","#8b5cf6"};
    private static final String[] BADGE_BG       = {"#ede9fe","#e0f2fe","#fff7ed","#fef2f2","#f5f3ff"};

    private void afficherBadgesCoins(int coins) {
        if (badgesBox == null) return;
        Platform.runLater(() -> {
            badgesBox.getChildren().clear();
            HBox titreLigne = new HBox(10); titreLigne.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label ico = new Label("Badges"); ico.setStyle("-fx-font-size:18px; -fx-font-weight:900;");
            Label lT  = new Label("Mes Badges"); lT.setStyle("-fx-font-size:15px; -fx-font-weight:900; -fx-text-fill:#1e293b;");
            long nb = Arrays.stream(PALIERS).filter(p -> coins >= p[0]).count();
            Label lC = new Label(nb+"/5"); lC.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#6366f1; -fx-background-color:#ede9fe; -fx-background-radius:20; -fx-padding:3 10 3 10;");
            Pane sp = new Pane(); HBox.setHgrow(sp, Priority.ALWAYS);
            titreLigne.getChildren().addAll(ico, lT, sp, lC); badgesBox.getChildren().add(titreLigne);

            int ps = -1; for (int[] p : PALIERS) if (coins < p[0]) { ps = p[0]; break; }
            int pp = 0;  for (int[] p : PALIERS) if (coins >= p[0]) pp = p[0];
            double pct = ps > 0 ? (double)(coins-pp)/(ps-pp) : 1.0;

            StackPane barC = new StackPane(); barC.setMaxWidth(Double.MAX_VALUE); barC.setPrefHeight(8);
            barC.setStyle("-fx-background-color:#f1f5f9; -fx-background-radius:4;");
            Pane barF = new Pane(); barF.setPrefHeight(8);
            String cB = nb>=4?"#6366f1":nb>=2?"#0ea5e9":"#94a3b8";
            barF.setStyle("-fx-background-color:"+cB+"; -fx-background-radius:4;");
            StackPane.setAlignment(barF, javafx.geometry.Pos.CENTER_LEFT); barF.setPrefWidth(0);
            barC.getChildren().add(barF); badgesBox.getChildren().add(barC);

            if (ps > 0) { Label lp = new Label("Plus que "+(ps-coins)+" coins pour le prochain badge !"); lp.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b; -fx-font-style:italic;"); badgesBox.getChildren().add(lp); }
            else { Label lm = new Label("Tous les badges débloqués !"); lm.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#6366f1;"); badgesBox.getChildren().add(lm); }

            TilePane grille = new TilePane(); grille.setHgap(10); grille.setVgap(10); grille.setPrefColumns(2); grille.setMaxWidth(Double.MAX_VALUE);
            for (int i = 0; i < PALIERS.length; i++) {
                boolean d = coins >= PALIERS[i][0];
                HBox item = new HBox(10); item.setAlignment(javafx.geometry.Pos.CENTER_LEFT); item.setPadding(new javafx.geometry.Insets(10,14,10,14));
                item.setStyle("-fx-background-color:"+(d?BADGE_BG[i]:"#f8fafc")+"; -fx-background-radius:14; -fx-border-color:"+(d?BADGE_COULEURS[i]+"55":"#e2e8f0")+"; -fx-border-radius:14; -fx-border-width:1.5; -fx-opacity:"+(d?"1.0":"0.45")+";");
                StackPane cercle = new StackPane(); cercle.setMinSize(44,44); cercle.setMaxSize(44,44);
                cercle.setStyle("-fx-background-color:"+(d?BADGE_COULEURS[i]+"22":"#e2e8f0")+"; -fx-background-radius:22;");
                Label le = new Label(d?BADGE_EMOJIS[i]:"?"); le.setStyle("-fx-font-size:22px;"); cercle.getChildren().add(le);
                if (d) { ScaleTransition a = new ScaleTransition(Duration.millis(600),cercle); a.setFromX(0.8); a.setFromY(0.8); a.setToX(1.0); a.setToY(1.0); a.setInterpolator(Interpolator.EASE_OUT); a.play(); }
                VBox textes = new VBox(2);
                Label ln = new Label(BADGE_TITRES[i]); ln.setStyle("-fx-font-size:12px; -fx-font-weight:900; -fx-text-fill:"+(d?BADGE_COULEURS[i]:"#94a3b8")+";");
                Label ls = new Label(d?"Débloqué — "+PALIERS[i][0]+" coins":PALIERS[i][0]+" coins requis"); ls.setStyle("-fx-font-size:10px; -fx-text-fill:"+(d?"#64748b":"#94a3b8")+";");
                textes.getChildren().addAll(ln, ls); item.getChildren().addAll(cercle, textes); grille.getChildren().add(item);
            }
            badgesBox.getChildren().add(grille);
            Platform.runLater(() -> {
                double mW = barC.getWidth() > 0 ? barC.getWidth() : 340;
                new Timeline(
                        new KeyFrame(Duration.ZERO,         new KeyValue(barF.prefWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(900),   new KeyValue(barF.prefWidthProperty(), mW*pct, Interpolator.EASE_OUT))
                ).play();
            });
        });
    }

    // ══════════════════════════════════════════════════════════════
    // CONSEILS / HISTORIQUE / GRAPHIQUE
    // ══════════════════════════════════════════════════════════════
    private void afficherConseil(int be, int st, int hu, int nb) {
        if (lblConseil == null) return;
        lblConseil.setText("🤖 Conseil personnalisé en cours...");
        if (lblTitre1 != null) lblTitre1.setText("Chargement...");
        if (lblTitre2 != null) lblTitre2.setText("Chargement...");
        if (lblTitre3 != null) lblTitre3.setText("Chargement...");
        new Thread(() -> {
            String c = serviceGroqQuiz.genererConseil(be, st, hu, nb);
            List<String[]> t = serviceGroqQuiz.genererTroisConseils(be, st, hu);
            Platform.runLater(() -> {
                if (lblConseil != null) lblConseil.setText(c != null ? c : "Continuez vos efforts !");
                if (t!=null&&t.size()>0&&t.get(0).length>=3) { if(lblEmoji1!=null)lblEmoji1.setText(t.get(0)[0]); if(lblTitre1!=null)lblTitre1.setText(t.get(0)[1]); if(lblDesc1!=null)lblDesc1.setText(t.get(0)[2]); }
                if (t!=null&&t.size()>1&&t.get(1).length>=3) { if(lblEmoji2!=null)lblEmoji2.setText(t.get(1)[0]); if(lblTitre2!=null)lblTitre2.setText(t.get(1)[1]); if(lblDesc2!=null)lblDesc2.setText(t.get(1)[2]); }
                if (t!=null&&t.size()>2&&t.get(2).length>=3) { if(lblEmoji3!=null)lblEmoji3.setText(t.get(2)[0]); if(lblTitre3!=null)lblTitre3.setText(t.get(2)[1]); if(lblDesc3!=null)lblDesc3.setText(t.get(2)[2]); }
            });
        }).start();
    }

    // ✅ Historique — utilise normaliserEnRatio + inversion pour stress/humeur
    private void afficherHistorique(List<String> h) {
        if (historyBox == null) return;
        historyBox.getChildren().clear();
        int debut = Math.max(0, h.size() - 5);
        for (int i = h.size() - 1; i >= debut; i--) {
            String ligne    = h.get(i);
            String titreLow = extraireTitre(ligne).toLowerCase();
            double ratio    = normaliserEnRatio(ligne);
            int    scorePct;

            if (titreLow.contains("stress") || titreLow.contains("humeur")) {
                scorePct = (int) Math.round((1.0 - ratio) * 100);
            } else {
                scorePct = (int) Math.round(ratio * 100);
            }

            String emoji = scorePct >= 70 ? "✅" : scorePct >= 40 ? "🟡" : "🔴";
            Label l = new Label(extraireDate(ligne) + "  ·  " + extraireTitre(ligne)
                    + "  →  " + scorePct + "/100  " + emoji);
            l.setStyle("-fx-font-size:12px; -fx-text-fill:#374151; -fx-font-weight:600; -fx-padding:6 0;");
            historyBox.getChildren().addAll(l, new javafx.scene.control.Separator());
        }
    }

    // ✅ Graphique — utilise normaliserEnRatio + inversion (identique SuivieController)
    private void chargerGraphiqueReel() {
        if (evolutionChart == null) return;
        evolutionChart.getData().clear();
        evolutionChart.setAnimated(true);
        try {
            List<String> hist = serviceQuiz.getHistoriquePatient(currentPatientId);
            if (hist.isEmpty()) return;

            XYChart.Series<String,Number> sBE = new XYChart.Series<>();
            XYChart.Series<String,Number> sST = new XYChart.Series<>();
            XYChart.Series<String,Number> sHU = new XYChart.Series<>();
            sBE.setName("Bien-être"); sST.setName("Stress"); sHU.setName("Humeur");

            LocalDateTime     lim = LocalDateTime.now().minusDays(getSelectedDays());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

            for (String ligne : hist) {
                LocalDateTime dt = extraireDateTime(ligne);
                if (dt.isBefore(lim)) continue;

                String date  = dt.format(fmt);
                String titre = extraireTitre(ligne).toLowerCase();
                double ratio = normaliserEnRatio(ligne);
                int vBE, vST, vHU;

                if (titre.contains("stress")) {
                    vST = (int) Math.round((1.0 - ratio) * 100);
                    vBE = Math.min(100, vST + 10);
                    vHU = Math.min(100, vST + 5);
                } else if (titre.contains("humeur")) {
                    vHU = (int) Math.round((1.0 - ratio) * 100);
                    vBE = Math.min(100, vHU + 8);
                    vST = Math.max(0, 100 - vHU - 10);
                } else {
                    vBE = (int) Math.round(ratio * 100);
                    vST = Math.max(0, 100 - vBE - 5);
                    vHU = Math.min(100, vBE - 5);
                }

                sBE.getData().add(new XYChart.Data<>(date, vBE));
                sST.getData().add(new XYChart.Data<>(date, vST));
                sHU.getData().add(new XYChart.Data<>(date, vHU));
            }
            evolutionChart.getData().addAll(sBE, sST, sHU);
            Platform.runLater(this::applyStyling);
        } catch (SQLException e) { System.err.println("❌ Graphique : " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════
    // BOUTONS FXML
    // ══════════════════════════════════════════════════════════════
    @FXML private void testerGroq() {
        setResultat("⏳ Test Groq...", "#78350F", "#FEF3C7");
        new Thread(() -> {
            try {
                String c = serviceGroqQuiz.genererConseil(scoreBE, scoreST, scoreHU, 5);
                String t = serviceGroqQuiz.recommanderProchainTest(scoreBE, scoreST, scoreHU);
                Platform.runLater(() -> setResultat(
                        c!=null&&!c.isEmpty() ? "✅ GROQ OK !\n💬 "+c+"\n🎯 "+t : "❌ Réponse vide",
                        c!=null ? "#065F46" : "#991B1B",
                        c!=null ? "#ECFDF5" : "#FEF2F2"));
            } catch(Exception e) { Platform.runLater(() -> setResultat("❌ "+e.getMessage(),"#991B1B","#FEF2F2")); }
        }).start();
    }

    @FXML private void testerEmail() {
        setResultat("⏳ Envoi email...", "#78350F", "#FEF3C7");
        new Thread(() -> {
            try {
                new ServiceEmailQuiz().envoyerEmail(
                        "mindcare.notifications@gmail.com", "🧪 Test MindCare", "<h2>✅ Email OK !</h2>");
                Platform.runLater(() -> setResultat("✅ EMAIL OK ! 📬","#065F46","#ECFDF5"));
            } catch(Exception e) { Platform.runLater(() -> setResultat("❌ "+e.getMessage(),"#991B1B","#FEF2F2")); }
        }).start();
    }

    private void setResultat(String t, String cT, String cF) {
        if (lblTestResultat == null) return;
        lblTestResultat.setText(t);
        lblTestResultat.setStyle("-fx-font-size:11px; -fx-font-weight:600;"
                + "-fx-text-fill:"+cT+"; -fx-background-color:"+cF+";"
                + "-fx-background-radius:8; -fx-padding:10;");
    }

    @FXML private void ouvrirChat() {
        ServiceVoixQuiz.arreter();
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/views/chatquiz.fxml"));
            Node v = l.load();
            VBox ca = (VBox) lblBienvenue.getScene().lookup("#contentArea");
            if (ca != null) ca.getChildren().setAll(v);
        } catch(IOException e) { e.printStackTrace(); }
    }

    @FXML private void ouvrirEspacePraticien() {
        Session.Role r = Session.getRoleConnecte();
        if (r != Session.Role.PSYCHOLOGUE && r != Session.Role.ADMIN) return;
        ServiceVoixQuiz.arreter();
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/views/EspacepraticienQuiz.fxml"));
            Node v = l.load();
            VBox ca = (VBox) lblBienvenue.getScene().lookup("#contentArea");
            if (ca != null) ca.getChildren().setAll(v);
        } catch(IOException e) { e.printStackTrace(); }
    }

    @FXML public void retourSuivie() { if (parentController != null) parentController.loadAccueil(); }

    // ══════════════════════════════════════════════════════════════
    // HELPERS VISUELS
    // ══════════════════════════════════════════════════════════════
    private void animerArc(Arc arc, int score, String hex) {
        if (arc == null) return;
        double tgt = -(score / 100.0) * 360.0;
        arc.setStroke(Color.web(hex));
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setFill(Color.TRANSPARENT);
        final int[] f = {0};
        new javafx.animation.AnimationTimer() {
            @Override public void handle(long n) {
                f[0]++;
                double p = Math.min(1.0, f[0] / 60.0);
                arc.setLength(tgt * (1 - Math.pow(1 - p, 3)));
                if (f[0] >= 60) stop();
            }
        }.start();
    }

    private void setTrend(Label l, int delta, String couleur) {
        if (l == null) return;
        String s  = delta > 0 ? "↑ +" : delta < 0 ? "↓ " : "→ ";
        String tF = delta >= 0 ? "#065F46" : "#9D174D";
        String bF = delta >= 0 ? "rgba(16,185,129,0.1)" : "rgba(239,68,68,0.1)";
        l.setText(s + delta + "%");
        l.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:"+tF+";"
                + "-fx-background-color:"+bF+"; -fx-background-radius:20; -fx-padding:3 10 3 10;");
    }

    private void applyStyling() {
        if (evolutionChart == null) return;
        evolutionChart.setStyle("-fx-background-color:transparent;");
        styleArea(evolutionChart, 0, "rgba(167,139,250,0.25)", "#A78BFA");
        styleArea(evolutionChart, 1, "rgba(255,107,157,0.2)",  "#FF6B9D");
        styleArea(evolutionChart, 2, "rgba(79,172,254,0.2)",   "#4FACFE");
        Node p = evolutionChart.lookup(".chart-plot-background");
        if (p != null) p.setStyle("-fx-background-color:#FAFBFC;");
    }

    private void styleArea(AreaChart<?,?> ch, int i, String fill, String stroke) {
        Node f = ch.lookup(".default-color"+i+".chart-series-area-fill");
        Node l = ch.lookup(".default-color"+i+".chart-series-area-line");
        if (f != null) f.setStyle("-fx-fill:"+fill+";");
        if (l != null) l.setStyle("-fx-stroke:"+stroke+"; -fx-stroke-width:2.5px;");
        ch.lookupAll(".default-color"+i+".chart-area-symbol")
                .forEach(n -> n.setStyle("-fx-background-color:"+stroke+",white;"));
    }

    // ══════════════════════════════════════════════════════════════
    // COMBO PÉRIODE
    // ══════════════════════════════════════════════════════════════
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

    // ══════════════════════════════════════════════════════════════
    // MUSIQUE
    // ══════════════════════════════════════════════════════════════
    @FXML private void chargerMusique() {
        if (btnChargerMusique != null) { btnChargerMusique.setText("⏳ Chargement..."); btnChargerMusique.setDisable(true); }
        if (lblChargementPistes != null) lblChargementPistes.setText("🤖 IA analyse votre état...");
        String nom = Session.getFullName() != null ? Session.getFullName() : "Patient";
        new Thread(() -> {
            ServiceMusiqueQuiz.MusiqueParams params = serviceMusiqueQuiz.calculerParams(scoreBE, scoreST, scoreHU, nom);
            List<ServiceMusiqueQuiz.Piste> pistes   = serviceMusiqueQuiz.chercherPistes(params);
            pistesChargees = pistes; pisteActuelle = 0;
            Platform.runLater(() -> {
                if (lblMusiqueMessage != null) lblMusiqueMessage.setText(params.message + " • BPM: " + params.bpm);
                afficherListePistes(pistes);
                if (btnChargerMusique != null) { btnChargerMusique.setText("🔄 Recharger"); btnChargerMusique.setDisable(false); }
                if (!pistes.isEmpty()) jouerPiste(pistes.get(0));
            });
        }).start();
    }

    private void afficherListePistes(List<ServiceMusiqueQuiz.Piste> pistes) {
        if (listePistesBox == null) return;
        listePistesBox.getChildren().removeIf(n -> n.getUserData() != null && n.getUserData().equals("piste"));
        if (lblChargementPistes != null) lblChargementPistes.setVisible(false);
        for (int i = 0; i < pistes.size(); i++) {
            ServiceMusiqueQuiz.Piste piste = pistes.get(i);
            final int idx = i;
            HBox ligne = new HBox(10);
            ligne.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ligne.setPadding(new javafx.geometry.Insets(8,12,8,12));
            ligne.setUserData("piste");
            ligne.setStyle("-fx-background-color:"+(i==pisteActuelle?"rgba(124,58,237,0.10)":"transparent")+"; -fx-background-radius:10; -fx-cursor:hand;");
            Label em = new Label(piste.emoji); em.setStyle("-fx-font-size:16px;");
            VBox inf = new VBox(2); HBox.setHgrow(inf, Priority.ALWAYS);
            Label nL = new Label(piste.nom); nL.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#1F2937;");
            Label dL = new Label(piste.duree); dL.setStyle("-fx-font-size:10px; -fx-text-fill:#9CA3AF;");
            inf.getChildren().addAll(nL, dL);
            Label pl = new Label(i==pisteActuelle&&enLecture?"▶":"○"); pl.setStyle("-fx-font-size:14px; -fx-text-fill:#7C3AED;");
            ligne.getChildren().addAll(em, inf, pl);
            ligne.setOnMouseClicked(e -> { pisteActuelle=idx; jouerPiste(piste); afficherListePistes(pistesChargees); });
            ligne.setOnMouseEntered(e -> ligne.setStyle("-fx-background-color:rgba(124,58,237,0.08); -fx-background-radius:10; -fx-cursor:hand;"));
            ligne.setOnMouseExited(e  -> ligne.setStyle("-fx-background-color:"+(idx==pisteActuelle?"rgba(124,58,237,0.10)":"transparent")+"; -fx-background-radius:10; -fx-cursor:hand;"));
            listePistesBox.getChildren().add(ligne);
        }
    }

    private void jouerPiste(ServiceMusiqueQuiz.Piste p) {
        if (lblPisteNom    != null) lblPisteNom.setText(p.nom);
        if (lblPisteDuree  != null) lblPisteDuree.setText(p.duree);
        if (lblPisteEmoji  != null) lblPisteEmoji.setText(p.emoji);
        if (lblEtatLecture != null) lblEtatLecture.setText("▶");
        if (btnPlayPause   != null) btnPlayPause.setText("⏸");
        if (p.url == null) { if (lblPisteNom != null) lblPisteNom.setText(p.nom+" (aperçu non dispo)"); enLecture=false; return; }
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
        try {
            mediaPlayer = new javafx.scene.media.MediaPlayer(new javafx.scene.media.Media(p.url));
            double vol = sliderVolume != null ? sliderVolume.getValue() : 0.8;
            mediaPlayer.setVolume(vol);
            mediaPlayer.setOnEndOfMedia(() -> {
                pisteActuelle = (pisteActuelle + 1) % pistesChargees.size();
                Platform.runLater(() -> { jouerPiste(pistesChargees.get(pisteActuelle)); afficherListePistes(pistesChargees); });
            });
            mediaPlayer.play(); enLecture = true;
        } catch(Exception e) { System.err.println("❌ Lecture : "+e.getMessage()); enLecture=false; }
    }

    @FXML private void togglePlayPause() {
        if (mediaPlayer == null) { if (!pistesChargees.isEmpty()) jouerPiste(pistesChargees.get(pisteActuelle)); return; }
        if (enLecture) { mediaPlayer.pause(); enLecture=false; if(btnPlayPause!=null)btnPlayPause.setText("▶"); if(lblEtatLecture!=null)lblEtatLecture.setText("⏸"); }
        else           { mediaPlayer.play();  enLecture=true;  if(btnPlayPause!=null)btnPlayPause.setText("⏸"); if(lblEtatLecture!=null)lblEtatLecture.setText("▶"); }
    }

    @FXML private void pisteSuivante() {
        if (pistesChargees.isEmpty()) return;
        pisteActuelle = (pisteActuelle + 1) % pistesChargees.size();
        jouerPiste(pistesChargees.get(pisteActuelle));
        afficherListePistes(pistesChargees);
    }

    @FXML private void pistePrecedente() {
        if (pistesChargees.isEmpty()) return;
        pisteActuelle = (pisteActuelle - 1 + pistesChargees.size()) % pistesChargees.size();
        jouerPiste(pistesChargees.get(pisteActuelle));
        afficherListePistes(pistesChargees);
    }

    @FXML private void stopMusique() {
        if (mediaPlayer != null) { mediaPlayer.stop(); enLecture = false; }
        if (btnPlayPause   != null) btnPlayPause.setText("▶");
        if (lblEtatLecture != null) lblEtatLecture.setText("⏹");
        if (lblPisteNom    != null) lblPisteNom.setText("Aucune piste en lecture");
    }
}