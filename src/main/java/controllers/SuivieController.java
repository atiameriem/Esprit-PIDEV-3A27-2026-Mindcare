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
    @FXML private Button  btnEspacePsy;
    @FXML private Label   lblTestResultat;

    @FXML private Label lblEmoji1;
    @FXML private Label lblTitre1;
    @FXML private Label lblDesc1;
    @FXML private Label lblEmoji2;
    @FXML private Label lblTitre2;
    @FXML private Label lblDesc2;
    @FXML private Label lblEmoji3;
    @FXML private Label lblTitre3;
    @FXML private Label lblDesc3;

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

    private int scoreBE = 0, scoreST = 0, scoreHU = 0;
    static boolean rappelDejaVerifie = false;

    private javafx.scene.media.MediaPlayer mediaPlayer;
    private List<ServiceMusique.Piste>     pistesChargees = new ArrayList<>();
    private int     pisteActuelle = 0;
    private boolean enLecture     = false;

    // ══════════════════════════════════════════════════════════════
    // Rafraîchissement statique
    // ══════════════════════════════════════════════════════════════
    public static void rafraichir() {
        if (instance != null) {
            Platform.runLater(() -> {
                instance.updateScoresFromDB();
                instance.chargerGraphiqueReel();
                System.out.println("🔄 SuivieController rafraîchi !");
            });
        }
    }

    // ══════════════════════════════════════════════════════════════
    // INITIALIZE
    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        instance = this;

        var role = Session.getRoleConnecte();
        currentPatientId = Session.getUserId();

        System.out.println("👤 Patient connecté ID = " + currentPatientId
                + " | Nom = " + Session.getFullName()
                + " | Role = " + role);

        if (btnEspacePsy != null) {
            boolean peutVoir = (role == Session.Role.PSYCHOLOGUE
                    || role == Session.Role.ADMIN);
            btnEspacePsy.setVisible(peutVoir);
            btnEspacePsy.setManaged(peutVoir);
        }

        if (currentPatientId <= 0) {
            if (lblBienvenue != null)
                lblBienvenue.setText("⛔ Aucun utilisateur connecté.");
            return;
        }

        if (role != null
                && role != Session.Role.USER
                && role != Session.Role.ADMIN) {
            if (lblBienvenue != null)
                lblBienvenue.setText("⛔ Accès réservé aux patients.");
            return;
        }

        if (sliderVolume != null) {
            sliderVolume.valueProperty().addListener(
                    (obs, ov, nv) -> {
                        if (mediaPlayer != null)
                            mediaPlayer.setVolume(nv.doubleValue());
                    });
        }

        configurerCombo();
        loadPatientData();
        chargerGraphiqueReel();
        afficherRecommandationTest();

        if (!rappelDejaVerifie) {
            rappelDejaVerifie = true;
            new Thread(() -> {
                try {
                    new ServiceRappel().verifierEtEnvoyerRappels();
                } catch (Exception e) {
                    System.err.println("❌ Rappels : " + e.getMessage());
                }
            }).start();
        }
    }

    public void setParentController(MindCareLayoutController parent) {
        this.parentController = parent;
    }

    // ══════════════════════════════════════════════════════════════
    // CHARGEMENT DONNÉES
    // ══════════════════════════════════════════════════════════════
    private void loadPatientData() {
        try {
            String nom = (Session.getFullName() != null
                    && !Session.getFullName().trim().isEmpty())
                    ? Session.getFullName()
                    : "Patient #" + currentPatientId;

            if (lblBienvenue != null)
                lblBienvenue.setText("Hello " + nom + " ! ✨");

            updateScoresFromDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ EXTRACTION — Score, Max, Date, Titre
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
            return 6; // fallback pour compatibilité
        }
    }

    /**
     * ✅ Normalise en ratio [0.0 – 1.0] avec le vrai max du quiz.
     *    score=6 / max=6  → 1.0  (très mauvais pour stress/humeur)
     *    score=6 / max=18 → 0.33 (modéré)
     *    score=0 / max=6  → 0.0  (parfait pour stress/humeur)
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
            return extraireDateTime(ligne)
                    .format(DateTimeFormatter.ofPattern("dd/MM"));
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
    // ✅ SCORES + CERCLES
    // ══════════════════════════════════════════════════════════════
    private void updateScoresFromDB() {
        try {
            List<String> historique =
                    serviceQuiz.getHistoriquePatient(currentPatientId);

            System.out.println("📋 " + historique.size()
                    + " session(s) pour patient ID=" + currentPatientId);

            if (historique.isEmpty()) {
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
                if (lblConseil != null)
                    lblConseil.setText("Commencez votre premier test ! 🚀");
                return;
            }

            // ✅ Stocker les RATIOS (0.0–1.0) — le max vient directement de la ligne
            List<Double> ratiosBE = new ArrayList<>();
            List<Double> ratiosST = new ArrayList<>();
            List<Double> ratiosHU = new ArrayList<>();

            for (String ligne : historique) {
                String titre = extraireTitre(ligne).toLowerCase();
                double ratio = normaliserEnRatio(ligne);

                System.out.println("📌 " + titre
                        + " | score=" + extraireScore(ligne)
                        + " | max=" + extraireScoreMax(ligne)
                        + " | ratio=" + String.format("%.2f", ratio));

                if      (titre.contains("stress"))  ratiosST.add(ratio);
                else if (titre.contains("humeur"))  ratiosHU.add(ratio);
                else                                ratiosBE.add(ratio);
            }

            System.out.println("📊 Sessions — BE:" + ratiosBE.size()
                    + " ST:" + ratiosST.size()
                    + " HU:" + ratiosHU.size());

            // ✅ Ratio neutre 0.5 (50%) si aucune donnée pour la catégorie
            double moyRatioBE = ratiosBE.isEmpty() ? 0.5
                    : ratiosBE.stream().mapToDouble(d -> d).average().orElse(0.5);
            double moyRatioST = ratiosST.isEmpty() ? 0.5
                    : ratiosST.stream().mapToDouble(d -> d).average().orElse(0.5);
            double moyRatioHU = ratiosHU.isEmpty() ? 0.5
                    : ratiosHU.stream().mapToDouble(d -> d).average().orElse(0.5);

            // Bien-être  : ratio élevé = bon   → score élevé
            scoreBE = (int) Math.round(moyRatioBE * 100);
            // Stress     : ratio élevé = mauvais → inverser
            scoreST = (int) Math.round((1.0 - moyRatioST) * 100);
            // Humeur     : ratio élevé = mauvais → inverser
            scoreHU = (int) Math.round((1.0 - moyRatioHU) * 100);

            // Clamp 0–100
            scoreBE = Math.max(0, Math.min(100, scoreBE));
            scoreST = Math.max(0, Math.min(100, scoreST));
            scoreHU = Math.max(0, Math.min(100, scoreHU));

            System.out.println("📊 Scores finaux — BE:"
                    + scoreBE + "% ST:" + scoreST + "% HU:" + scoreHU + "%");

            // Tendances
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
            int totalGlobal    = historique.stream().mapToInt(this::extraireScore).sum();
            int moyenneGlobale = totalGlobal / historique.size();
            int coins          = historique.size() * 150 + moyenneGlobale * 10;
            if (lblCoinsGagnes != null)
                lblCoinsGagnes.setText(coins + " coins");

            if (lblSessionCount != null) {
                int nb = historique.size();
                lblSessionCount.setText(nb + " session"
                        + (nb > 1 ? "s" : "")
                        + " complétée" + (nb > 1 ? "s" : ""));
            }

            afficherConseil(scoreBE, scoreST, scoreHU, historique.size());
            afficherHistorique(historique);

        } catch (SQLException e) {
            System.err.println("❌ Scores : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ TENDANCE sur ratios [0.0–1.0]
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
    // RECOMMANDATION TEST
    // ══════════════════════════════════════════════════════════════
    private void afficherRecommandationTest() {
        new Thread(() -> {
            String test   = serviceGroq.recommanderProchainTest(scoreBE, scoreST, scoreHU);
            String raison = serviceGroq.getRaisonRecommandation(scoreBE, scoreST, scoreHU);
            Platform.runLater(() ->
                    System.out.println("🎯 Test recommandé : " + test + " — " + raison));
        }).start();
    }

    // ══════════════════════════════════════════════════════════════
    // CONSEILS IA
    // ══════════════════════════════════════════════════════════════
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
    // ✅ HISTORIQUE
    // ══════════════════════════════════════════════════════════════
    private void afficherHistorique(List<String> historique) {
        if (historyBox == null) return;
        historyBox.getChildren().clear();
//
        int debut = Math.max(0, historique.size() - 5);
        for (int i = historique.size() - 1; i >= debut; i--) {
            String ligne    = historique.get(i);
            String date     = extraireDate(ligne);
            String titre    = extraireTitre(ligne);
            String titreLow = titre.toLowerCase();

            // ✅ normaliserEnRatio(ligne) lit le vrai Max depuis la ligne
            double ratio    = normaliserEnRatio(ligne);
            int    scorePct;

            if (titreLow.contains("stress") || titreLow.contains("humeur")) {
                // ratio élevé = mauvais état → inverser pour afficher bien-être
                scorePct = (int) Math.round((1.0 - ratio) * 100);
            } else {
                // ratio élevé = bon état → garder
                scorePct = (int) Math.round(ratio * 100);
            }

            String emoji = scorePct >= 70 ? "✅" : scorePct >= 40 ? "🟡" : "🔴";
            Label entry = new Label(
                    date + "  ·  " + titre + "  →  " + scorePct + "/100  " + emoji);
            entry.setStyle(
                    "-fx-font-size:12px; -fx-text-fill:#374151;"
                            + "-fx-font-weight:600; -fx-padding:6 0;");
            historyBox.getChildren().add(entry);
            historyBox.getChildren().add(new javafx.scene.control.Separator());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ✅ GRAPHIQUE
    // ══════════════════════════════════════════════════════════════
    private void chargerGraphiqueReel() {
        if (evolutionChart == null) return;
        evolutionChart.getData().clear();
        evolutionChart.setAnimated(true);

        try {
            List<String> historique =
                    serviceQuiz.getHistoriquePatient(currentPatientId);

            if (historique.isEmpty()) return;

            XYChart.Series<String, Number> seriesBE = new XYChart.Series<>();
            seriesBE.setName("Bien-être");
            XYChart.Series<String, Number> seriesST = new XYChart.Series<>();
            seriesST.setName("Stress");
            XYChart.Series<String, Number> seriesHU = new XYChart.Series<>();
            seriesHU.setName("Humeur");

            LocalDateTime     limite = LocalDateTime.now().minusDays(getSelectedDays());
            DateTimeFormatter fmt    = DateTimeFormatter.ofPattern("dd/MM");

            for (String ligne : historique) {
                LocalDateTime dt = extraireDateTime(ligne);
                if (dt.isBefore(limite)) continue;

                String date  = dt.format(fmt);
                String titre = extraireTitre(ligne).toLowerCase();

                // ✅ normaliserEnRatio(ligne) — plus de heuristique score > 6
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

    // ══════════════════════════════════════════════════════════════
    // 🧪 ZONE TEST
    // ══════════════════════════════════════════════════════════════
    @FXML
    private void testerGroq() {
        setResultat("⏳ Test Groq en cours...", "#78350F", "#FEF3C7");
        new Thread(() -> {
            try {
                String conseil = serviceGroq.genererConseil(scoreBE, scoreST, scoreHU, 5);
                String test    = serviceGroq.recommanderProchainTest(scoreBE, scoreST, scoreHU);
                Platform.runLater(() -> {
                    if (conseil != null && !conseil.isEmpty()) {
                        setResultat("✅ GROQ OK !\n💬 " + conseil + "\n🎯 " + test,
                                "#065F46", "#ECFDF5");
                    } else {
                        setResultat("❌ Groq — réponse vide", "#991B1B", "#FEF2F2");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        setResultat("❌ " + e.getMessage(), "#991B1B", "#FEF2F2"));
            }
        }).start();
    }

    @FXML
    private void testerEmail() {
        setResultat("⏳ Envoi email...", "#78350F", "#FEF3C7");
        new Thread(() -> {
            try {
                new services.ServiceEmail().envoyerEmail(
                        "mindcare.notifications@gmail.com",
                        "🧪 Test MindCare",
                        "<h2>✅ Email OK !</h2>");
                Platform.runLater(() ->
                        setResultat("✅ EMAIL OK !\n📬 Vérifie ta boîte !",
                                "#065F46", "#ECFDF5"));
            } catch (Exception e) {
                Platform.runLater(() ->
                        setResultat("❌ " + e.getMessage(), "#991B1B", "#FEF2F2"));
            }
        }).start();
    }

    private void setResultat(String texte, String cTexte, String cFond) {
        if (lblTestResultat == null) return;
        lblTestResultat.setText(texte);
        lblTestResultat.setStyle(
                "-fx-font-size:11px; -fx-font-weight:600;"
                        + "-fx-text-fill:" + cTexte + ";"
                        + "-fx-background-color:" + cFond + ";"
                        + "-fx-background-radius:8; -fx-padding:10;");
    }

    // ══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════
    @FXML
    private void ouvrirChat() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/chatquiz.fxml"));
            Node view = loader.load();
            VBox contentArea = (VBox) lblBienvenue.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void ouvrirEspacePraticien() {
        var role = Session.getRoleConnecte();
        if (role != Session.Role.PSYCHOLOGUE && role != Session.Role.ADMIN) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/EspacePraticien.fxml"));
            Node view = loader.load();
            VBox contentArea = (VBox) lblBienvenue.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void retourSuivie() {
        if (parentController != null) parentController.loadAccueil();
    }

    // ══════════════════════════════════════════════════════════════
    // ARC ANIMÉ
    // ══════════════════════════════════════════════════════════════
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
        lbl.setStyle(
                "-fx-font-size:11px; -fx-font-weight:700;"
                        + "-fx-text-fill:" + tFill + ";"
                        + "-fx-background-color:" + bgFill + ";"
                        + "-fx-background-radius:20;"
                        + "-fx-padding:3 10 3 10;");
    }

    // ══════════════════════════════════════════════════════════════
    // GRAPHIQUE STYLING
    // ══════════════════════════════════════════════════════════════
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
        if (line != null) line.setStyle("-fx-stroke:" + strokeColor
                + "; -fx-stroke-width:2.5px;");
        chart.lookupAll(".default-color" + index + ".chart-area-symbol")
                .forEach(n -> n.setStyle(
                        "-fx-background-color:" + strokeColor + ",white;"));
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
    // 🎵 MUSICOTHÉRAPIE
    // ══════════════════════════════════════════════════════════════
    @FXML
    private void chargerMusique() {
        if (btnChargerMusique != null) {
            btnChargerMusique.setText("⏳ Chargement...");
            btnChargerMusique.setDisable(true);
        }
        if (lblChargementPistes != null)
            lblChargementPistes.setText("🤖 IA analyse votre état...");

        String nom = Session.getFullName() != null ? Session.getFullName() : "Patient";

        new Thread(() -> {
            ServiceMusique.MusiqueParams params =
                    serviceMusique.calculerParams(scoreBE, scoreST, scoreHU, nom);
            List<ServiceMusique.Piste> pistes = serviceMusique.chercherPistes(params);

            pistesChargees = pistes;
            pisteActuelle  = 0;

            Platform.runLater(() -> {
                if (lblMusiqueMessage != null)
                    lblMusiqueMessage.setText(params.message + " • BPM: " + params.bpm);

                afficherListePistes(pistes);

                if (btnChargerMusique != null) {
                    btnChargerMusique.setText("🔄 Recharger");
                    btnChargerMusique.setDisable(false);
                }
                if (!pistes.isEmpty()) jouerPiste(pistes.get(0));
            });
        }).start();
    }

    private void afficherListePistes(List<ServiceMusique.Piste> pistes) {
        if (listePistesBox == null) return;

        listePistesBox.getChildren().removeIf(
                n -> n.getUserData() != null && n.getUserData().equals("piste"));

        if (lblChargementPistes != null) lblChargementPistes.setVisible(false);

        for (int i = 0; i < pistes.size(); i++) {
            ServiceMusique.Piste piste = pistes.get(i);
            final int idx = i;

            HBox ligne = new HBox(10);
            ligne.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ligne.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
            ligne.setUserData("piste");
            ligne.setStyle(
                    "-fx-background-color:"
                            + (i == pisteActuelle ? "rgba(124,58,237,0.10)" : "transparent")
                            + "; -fx-background-radius:10; -fx-cursor:hand;");

            Label emoji = new Label(piste.emoji);
            emoji.setStyle("-fx-font-size:16px;");

            VBox infos = new VBox(2);
            HBox.setHgrow(infos, javafx.scene.layout.Priority.ALWAYS);
            Label nomLbl = new Label(piste.nom);
            nomLbl.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#1F2937;");
            Label dureeLbl = new Label(piste.duree);
            dureeLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#9CA3AF;");
            infos.getChildren().addAll(nomLbl, dureeLbl);

            Label playIco = new Label(i == pisteActuelle && enLecture ? "▶" : "○");
            playIco.setStyle("-fx-font-size:14px; -fx-text-fill:#7C3AED;");

            ligne.getChildren().addAll(emoji, infos, playIco);

            ligne.setOnMouseClicked(e -> {
                pisteActuelle = idx;
                jouerPiste(piste);
                afficherListePistes(pistesChargees);
            });
            ligne.setOnMouseEntered(ev -> ligne.setStyle(
                    "-fx-background-color:rgba(124,58,237,0.08);"
                            + "-fx-background-radius:10; -fx-cursor:hand;"));
            ligne.setOnMouseExited(ev -> ligne.setStyle(
                    "-fx-background-color:"
                            + (idx == pisteActuelle ? "rgba(124,58,237,0.10)" : "transparent")
                            + "; -fx-background-radius:10; -fx-cursor:hand;"));

            listePistesBox.getChildren().add(ligne);
        }
    }

    private void jouerPiste(ServiceMusique.Piste piste) {
        if (lblPisteNom    != null) lblPisteNom.setText(piste.nom);
        if (lblPisteDuree  != null) lblPisteDuree.setText(piste.duree);
        if (lblPisteEmoji  != null) lblPisteEmoji.setText(piste.emoji);
        if (lblEtatLecture != null) lblEtatLecture.setText("▶");
        if (btnPlayPause   != null) btnPlayPause.setText("⏸");

        if (piste.url == null) {
            if (lblPisteNom != null)
                lblPisteNom.setText(piste.nom + " (aperçu non disponible)");
            enLecture = false;
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        try {
            javafx.scene.media.Media media =
                    new javafx.scene.media.Media(piste.url);
            mediaPlayer = new javafx.scene.media.MediaPlayer(media);

            double vol = sliderVolume != null ? sliderVolume.getValue() : 0.8;
            mediaPlayer.setVolume(vol);

            mediaPlayer.setOnEndOfMedia(() -> {
                pisteActuelle = (pisteActuelle + 1) % pistesChargees.size();
                Platform.runLater(() -> {
                    jouerPiste(pistesChargees.get(pisteActuelle));
                    afficherListePistes(pistesChargees);
                });
            });

            mediaPlayer.play();
            enLecture = true;
            System.out.println("▶️ Lecture : " + piste.nom);

        } catch (Exception e) {
            System.err.println("❌ Lecture : " + e.getMessage());
            if (lblPisteNom != null) lblPisteNom.setText(piste.nom + " ⚠️");
            enLecture = false;
        }
    }

    @FXML
    private void togglePlayPause() {
        if (mediaPlayer == null) {
            if (!pistesChargees.isEmpty()) jouerPiste(pistesChargees.get(pisteActuelle));
            return;
        }
        if (enLecture) {
            mediaPlayer.pause();
            enLecture = false;
            if (btnPlayPause   != null) btnPlayPause.setText("▶");
            if (lblEtatLecture != null) lblEtatLecture.setText("⏸");
        } else {
            mediaPlayer.play();
            enLecture = true;
            if (btnPlayPause   != null) btnPlayPause.setText("⏸");
            if (lblEtatLecture != null) lblEtatLecture.setText("▶");
        }
    }

    @FXML
    private void pisteSuivante() {
        if (pistesChargees.isEmpty()) return;
        pisteActuelle = (pisteActuelle + 1) % pistesChargees.size();
        jouerPiste(pistesChargees.get(pisteActuelle));
        afficherListePistes(pistesChargees);
    }

    @FXML
    private void pistePrecedente() {
        if (pistesChargees.isEmpty()) return;
        pisteActuelle = (pisteActuelle - 1 + pistesChargees.size()) % pistesChargees.size();
        jouerPiste(pistesChargees.get(pisteActuelle));
        afficherListePistes(pistesChargees);
    }

    @FXML
    private void stopMusique() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            enLecture = false;
        }
        if (btnPlayPause   != null) btnPlayPause.setText("▶");
        if (lblEtatLecture != null) lblEtatLecture.setText("⏹");
        if (lblPisteNom    != null) lblPisteNom.setText("Aucune piste en lecture");
    }
}