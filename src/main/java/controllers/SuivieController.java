package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.layout.StackPane;
import services.ServiceQuiz;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SuivieController {

    private MindCareLayoutController parentController;
    private static int currentPatientId = 4;

    // ── Labels ──────────────────────────────────────────────────────
    @FXML private Label lblBienvenue;
    @FXML private Label lblCoinsGagnes;
    @FXML private Label lblSessionCount;

    // ── Scores ──────────────────────────────────────────────────────
    @FXML private Label scoreBienEtre;
    @FXML private Label scoreStress;
    @FXML private Label scoreHumeur;

    // ── Arcs de progression (remplacent les Circle) ─────────────────
    @FXML private Arc arcBienEtre;
    @FXML private Arc arcStress;
    @FXML private Arc arcHumeur;

    // ── Trends ──────────────────────────────────────────────────────
    @FXML private Label lblTrendBienEtre;
    @FXML private Label lblTrendStress;
    @FXML private Label lblTrendHumeur;

    // ── Graphique ───────────────────────────────────────────────────
    @FXML private ComboBox<String> comboPeriode;
    @FXML private AreaChart<String, Number> evolutionChart;

    // ── Conseils ────────────────────────────────────────────────────
    @FXML private Label lblConseil;

    // ── Historique ──────────────────────────────────────────────────
    @FXML private VBox historyBox;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();

    // Scores calculés (sauvegardés pour les tendances)
    private int scoreBE = 0, scoreST = 0, scoreHU = 0;

    // ================================================================
    //  INIT
    // ================================================================
    @FXML
    public void initialize() {
        configurerCombo();
        loadPatientData(currentPatientId);
        chargerGraphiqueReel();
    }

    public void setParentController(MindCareLayoutController parent) {
        this.parentController = parent;
    }

    public static void ouvrirSuivie(int patientId) {
        currentPatientId = patientId;
    }

    // ================================================================
    //  CHARGEMENT DONNÉES PATIENT
    // ================================================================
    private void loadPatientData(int patientId) {
        try {
            String nom = getPatientName(patientId);
            lblBienvenue.setText("Hello " + nom + " ! ✨");
            updateScoresFromDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateScoresFromDB() {
        try {
            List<String> historique = serviceQuiz.getHistoriquePatient(currentPatientId);

            if (historique.isEmpty()) {
                scoreBienEtre.setText("0/100");
                scoreStress.setText("0/100");
                scoreHumeur.setText("0/100");
                lblCoinsGagnes.setText("0 coins");
                if (lblSessionCount != null) lblSessionCount.setText("0 session");
                animerArc(arcBienEtre, 0, "#A78BFA");
                animerArc(arcStress,   0, "#FF6B9D");
                animerArc(arcHumeur,   0, "#4FACFE");
                return;
            }

            // ── Calcul du score moyen réel ───────────────────────────
            // ── Calcul réel depuis la DB ─────────────────────────────
            // Chaque question vaut max 3 points (GAD-7 : 0-3 par question)
            // Adapter SCORE_MAX selon ton quiz :
            //   GAD-7 (stress)   : 7 questions × 3 = 21 max
            //   PHQ-9 (humeur)   : 9 questions × 3 = 27 max
            //   WHOQOL (bien-être): variable, ajuster si besoin
            final int SCORE_MAX = 6; // 3 questions × valeur max 2 (Jamais=0, Parfois=1, Souvent=2)

            // Séparer les scores par type de quiz
            int totalBE = 0, countBE = 0;
            int totalST = 0, countST = 0;
            int totalHU = 0, countHU = 0;

            for (String ligne : historique) {
                int score = extraireScore(ligne);
                String titre = extraireTitre(ligne).toLowerCase();

                if (titre.contains("stress")) {
                    totalST += score; countST++;
                } else if (titre.contains("humeur")) {
                    totalHU += score; countHU++;
                } else {
                    // BIEN_ETRE / psychologique / cognitif / comportemental / émotionnel
                    totalBE += score; countBE++;
                }
            }

            // Si un type n'a pas de données, utiliser la moyenne globale
            int totalGlobal = 0;
            for (String ligne : historique) totalGlobal += extraireScore(ligne);
            int moyenneGlobale = totalGlobal / historique.size();

            int moyBE = countBE > 0 ? totalBE / countBE : moyenneGlobale;
            int moyST = countST > 0 ? totalST / countST : moyenneGlobale;
            int moyHU = countHU > 0 ? totalHU / countHU : moyenneGlobale;

            // ── Conversion en pourcentage réel (0-100) ───────────────
            // Bien-être : score élevé = bon → conversion directe
            scoreBE = (int) Math.min(100, (moyBE * 100.0) / SCORE_MAX);

            // Stress : score élevé = mauvais → on INVERSE
            // Ex: score brut 18/21 → stress élevé = 14% de bien-être
            //     score brut  3/21 → stress faible = 86% (bon signe)
            scoreST = (int) Math.max(0, 100 - (moyST * 100.0) / SCORE_MAX);

            // Humeur : score élevé = symptômes dépressifs = mauvais → INVERSER
            scoreHU = (int) Math.max(0, 100 - (moyHU * 100.0) / SCORE_MAX);

            // ── Mise à jour des labels ───────────────────────────────
            scoreBienEtre.setText(scoreBE + "/100");
            scoreStress.setText(scoreST   + "/100");
            scoreHumeur.setText(scoreHU   + "/100");

            // ── Animation des arcs ───────────────────────────────────
            animerArc(arcBienEtre, scoreBE, "#A78BFA");
            animerArc(arcStress,   scoreST, "#FF6B9D");
            animerArc(arcHumeur,   scoreHU, "#4FACFE");

            // ── Tendances (simulées) ─────────────────────────────────
            setTrend(lblTrendBienEtre, +8,  "#A78BFA");
            setTrend(lblTrendStress,   -5,  "#FF6B9D");
            setTrend(lblTrendHumeur,   +12, "#4FACFE");

            // ── Coins ────────────────────────────────────────────────
            int coins = historique.size() * 150 + moyenneGlobale * 10;
            lblCoinsGagnes.setText(coins + " coins");

            // ── Compteur sessions ────────────────────────────────────
            if (lblSessionCount != null) {
                lblSessionCount.setText(historique.size() + " session" +
                        (historique.size() > 1 ? "s" : "") + " complétée" +
                        (historique.size() > 1 ? "s" : ""));
            }

            // ── Conseils personnalisés ───────────────────────────────
            afficherConseil(scoreBE, scoreST, scoreHU);

            // ── Historique visuel ────────────────────────────────────
            afficherHistorique(historique);

        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement scores : " + e.getMessage());
        }
    }

    // ================================================================
    //  ARC ANIMÉ — remplace les Circle JavaFX (bug stroke-dasharray)
    // ================================================================
    /**
     * Anime un Arc JavaFX de 0° → (score/100 * 360°) en 60 frames.
     * L'Arc doit être déclaré dans le FXML avec :
     *   radiusX="40" radiusY="40" startAngle="90" length="0"
     *   type="OPEN" strokeWidth="8" fill="TRANSPARENT"
     */
    private void animerArc(Arc arc, int score, String couleurHex) {
        if (arc == null) return;
        double targetLength = -(score / 100.0) * 360.0; // négatif = sens horaire
        arc.setStroke(Color.web(couleurHex));
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setFill(Color.TRANSPARENT);

        final int[] frame = {0};
        final int totalFrames = 60;
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                frame[0]++;
                double progress = Math.min(1.0, frame[0] / (double) totalFrames);
                // Ease out cubic
                double ease = 1 - Math.pow(1 - progress, 3);
                arc.setLength(targetLength * ease);
                if (frame[0] >= totalFrames) stop();
            }
        };
        timer.start();
    }

    // ================================================================
    //  TENDANCE
    // ================================================================
    private void setTrend(Label lbl, int delta, String couleur) {
        if (lbl == null) return;
        String signe = delta >= 0 ? "↑ +" : "↓ ";
        lbl.setText(signe + delta + "%");
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; " +
                "-fx-text-fill: " + (delta >= 0 ? "#065F46" : "#9D174D") + ";");
    }

    // ================================================================
    //  CONSEILS PERSONNALISÉS
    // ================================================================
    private void afficherConseil(int be, int st, int hu) {
        if (lblConseil == null) return;
        String conseil;
        if (st > 60) {
            conseil = "😮‍💨 Ton niveau de stress est élevé. Essaie la respiration 4-7-8 avant 18h.";
        } else if (be >= 70) {
            conseil = "🧘 Excellent bien-être ! Continue la cohérence cardiaque le matin.";
        } else if (hu < 50) {
            conseil = "🌙 Ton humeur est basse. Objectif : 7h de sommeil avant minuit.";
        } else {
            conseil = "✅ Tes scores sont équilibrés. Maintiens ta routine actuelle !";
        }
        lblConseil.setText(conseil);
    }

    // ================================================================
    //  HISTORIQUE VISUEL
    // ================================================================
    // SCORE_MAX pour l'historique (même valeur que updateScoresFromDB)
    private static final int SCORE_MAX_HISTORIQUE = 6; // 3 questions × valeur max 2

    private void afficherHistorique(List<String> historique) {
        if (historyBox == null) return;
        historyBox.getChildren().clear();

        // Afficher les 5 dernières sessions
        int debut = Math.max(0, historique.size() - 5);
        for (int i = historique.size() - 1; i >= debut; i--) {
            String ligne     = historique.get(i);
            int scoreBrut    = extraireScore(ligne);   // score brut DB (ex: 4)
            String date      = extraireDate(ligne);
            String titre     = extraireTitre(ligne);

            // ✅ Convertir le score brut en pourcentage réel
            String titreLow = titre.toLowerCase();
            int scorePourcent;
            if (titreLow.contains("stress") || titreLow.contains("humeur")) {
                // Score élevé = mauvais → inverser
                scorePourcent = (int) Math.max(0, 100 - (scoreBrut * 100.0) / SCORE_MAX_HISTORIQUE);
            } else {
                // Bien-être / autres → direct
                scorePourcent = (int) Math.min(100, (scoreBrut * 100.0) / SCORE_MAX_HISTORIQUE);
            }

            // Emoji selon le résultat
            String emoji = scorePourcent >= 70 ? "✅" : scorePourcent >= 40 ? "🟡" : "🔴";

            Label entry = new Label(date + "  ·  " + titre + "  →  " + scorePourcent + "/100  " + emoji);
            entry.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151; " +
                    "-fx-font-weight: 600; -fx-padding: 6 0;");
            historyBox.getChildren().add(entry);

            // Séparateur
            javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
            sep.setStyle("-fx-background-color: #F3F4F6;");
            historyBox.getChildren().add(sep);
        }
    }

    // ================================================================
    //  GRAPHIQUE — 3 séries distinctes avec styling Platform.runLater
    // ================================================================
    private void chargerGraphiqueReel() {
        evolutionChart.getData().clear();
        evolutionChart.setAnimated(true);

        try {
            List<String> historique = serviceQuiz.getHistoriquePatient(currentPatientId);
            if (historique.isEmpty()) return;

            XYChart.Series<String, Number> seriesBE = new XYChart.Series<>();
            seriesBE.setName("Bien-être");
            XYChart.Series<String, Number> seriesST = new XYChart.Series<>();
            seriesST.setName("Stress");
            XYChart.Series<String, Number> seriesHU = new XYChart.Series<>();
            seriesHU.setName("Humeur");

            final int SCORE_MAX_GRAPH = 6; // même base que SCORE_MAX
            LocalDateTime limite = LocalDateTime.now().minusDays(getSelectedDays());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

            for (String ligne : historique) {
                LocalDateTime dateTime = extraireDateTime(ligne);
                if (dateTime.isBefore(limite)) continue;

                int score = extraireScore(ligne);
                String date = dateTime.format(fmt);
                String titre = extraireTitre(ligne).toLowerCase();

                // Conversion en % selon le type de quiz (basé sur le titre extrait)
                // Types réels en DB : STRESS, BIEN_ETRE, HUMEUR, psychologique, cognitif...
                int vBE, vST, vHU;
                if (titre.contains("stress")) {
                    vST = (int) Math.max(0, 100 - (score * 100.0) / SCORE_MAX_GRAPH);
                    vBE = Math.min(100, vST + 10);
                    vHU = Math.min(100, vST + 5);
                } else if (titre.contains("humeur") || titre.contains("bien_etre") || titre.contains("bien-etre")) {
                    vHU = (int) Math.max(0, 100 - (score * 100.0) / SCORE_MAX_GRAPH);
                    vBE = Math.min(100, vHU + 8);
                    vST = Math.max(0, 100 - vHU - 10);
                } else {
                    // Quiz psychologique / cognitif / comportemental / émotionnel → bien-être direct
                    vBE = (int) Math.min(100, (score * 100.0) / SCORE_MAX_GRAPH);
                    vST = Math.max(0, 100 - vBE - 5);
                    vHU = Math.min(100, vBE - 5);
                }

                seriesBE.getData().add(new XYChart.Data<>(date, vBE));
                seriesST.getData().add(new XYChart.Data<>(date, vST));
                seriesHU.getData().add(new XYChart.Data<>(date, vHU));
            }

            evolutionChart.getData().addAll(seriesBE, seriesST, seriesHU);

            // ⚠️  FIX CRITIQUE : applyStyling DOIT être appelé après le rendu
            Platform.runLater(() -> applyStyling());

        } catch (SQLException e) {
            System.err.println("❌ Erreur graphique : " + e.getMessage());
        }
    }

    // ================================================================
    //  STYLING DU GRAPHIQUE (appelé via Platform.runLater)
    // ================================================================
    private void applyStyling() {
        evolutionChart.setStyle("-fx-background-color: transparent;");

        // Bien-être — violet #A78BFA
        styleArea(evolutionChart, 0, "rgba(167,139,250,0.25)", "#A78BFA");
        // Stress — rose #FF6B9D
        styleArea(evolutionChart, 1, "rgba(255,107,157,0.2)",  "#FF6B9D");
        // Humeur — bleu #4FACFE
        styleArea(evolutionChart, 2, "rgba(79,172,254,0.2)",   "#4FACFE");

        // Fond du graphique
        Node plot = evolutionChart.lookup(".chart-plot-background");
        if (plot != null) plot.setStyle("-fx-background-color: #FAFBFC;");
    }

    private void styleArea(AreaChart<?, ?> chart, int index, String fillColor, String strokeColor) {
        String fill = ".default-color" + index + ".chart-series-area-fill";
        String line = ".default-color" + index + ".chart-series-area-line";
        String sym  = ".default-color" + index + ".chart-area-symbol";

        Node fillNode = chart.lookup(fill);
        Node lineNode = chart.lookup(line);

        if (fillNode != null) fillNode.setStyle("-fx-fill: " + fillColor + ";");
        if (lineNode != null) lineNode.setStyle("-fx-stroke: " + strokeColor + "; -fx-stroke-width: 2.5px;");

        // Colorier aussi les points de données
        chart.lookupAll(sym).forEach(node ->
                node.setStyle("-fx-background-color: " + strokeColor + ", white;")
        );
    }

    // ================================================================
    //  EXTRACTION DEPUIS LES LIGNES D'HISTORIQUE
    // ================================================================
    private int extraireScore(String ligne) {
        try {
            int start = ligne.indexOf("Score: ") + 7;
            int end   = ligne.indexOf(" |", start);
            return Integer.parseInt(ligne.substring(start, end).trim());
        } catch (Exception e) { return 0; }
    }

    private LocalDateTime extraireDateTime(String ligne) {
        try {
            int start = ligne.indexOf("Date: ") + 6;
            return LocalDateTime.parse(ligne.substring(start).trim());
        } catch (Exception e) { return LocalDateTime.now(); }
    }

    private String extraireDate(String ligne) {
        try {
            LocalDateTime dt = extraireDateTime(ligne);
            return dt.format(DateTimeFormatter.ofPattern("dd/MM"));
        } catch (Exception e) { return "--/--"; }
    }

    private String extraireTitre(String ligne) {
        try {
            int start = ligne.indexOf("Quiz: ") + 6;
            int end   = ligne.indexOf(" |", start);
            return ligne.substring(start, end).trim();
        } catch (Exception e) { return "Quiz"; }
    }

    // ================================================================
    //  COMBOBOX PÉRIODE
    // ================================================================
    private void configurerCombo() {
        comboPeriode.getItems().addAll("7 jours", "30 jours", "90 jours");
        comboPeriode.getSelectionModel().select("30 jours");
        comboPeriode.setOnAction(e -> chargerGraphiqueReel());
    }

    private int getSelectedDays() {
        switch (comboPeriode.getSelectionModel().getSelectedItem()) {
            case "7 jours":  return 7;
            case "90 jours": return 90;
            default:         return 30;
        }
    }

    // ================================================================
    //  NOMS PATIENTS
    // ================================================================
    private String getPatientName(int id) {
        switch (id) {
            case 1: return "Mohamed";
            case 4: return "Meriem";
            case 3: return "Mariem";
            default: return "Patient";
        }
    }

    // ================================================================
    //  NAVIGATION
    // ================================================================
    @FXML
    private void ouvrirEspacePraticien() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EspacePraticien.fxml"));
            Node view = loader.load();
            VBox contentArea = (VBox) lblBienvenue.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void retourSuivie() {
        if (parentController != null) parentController.loadAccueil();
    }
}