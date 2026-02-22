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
import javafx.scene.shape.StrokeLineCap;
import services.ServiceQuiz;
import services.ServiceGemini;
import services.ServiceRappel;
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SuivieController {

    private MindCareLayoutController parentController;
    private static int currentPatientId = 3;

    @FXML private Label lblBienvenue;
    @FXML private Label lblCoinsGagnes;
    @FXML private Label lblSessionCount;
    @FXML private Label scoreBienEtre;
    @FXML private Label scoreStress;
    @FXML private Label scoreHumeur;
    @FXML private Arc arcBienEtre;
    @FXML private Arc arcStress;
    @FXML private Arc arcHumeur;
    @FXML private Label lblTrendBienEtre;
    @FXML private Label lblTrendStress;
    @FXML private Label lblTrendHumeur;
    @FXML private ComboBox<String> comboPeriode;
    @FXML private AreaChart<String, Number> evolutionChart;
    @FXML private Label lblConseil;
    @FXML private VBox historyBox;

    // Labels dynamiques 3 conseils IA
    @FXML private Label lblEmoji1;
    @FXML private Label lblTitre1;
    @FXML private Label lblDesc1;
    @FXML private Label lblEmoji2;
    @FXML private Label lblTitre2;
    @FXML private Label lblDesc2;
    @FXML private Label lblEmoji3;
    @FXML private Label lblTitre3;
    @FXML private Label lblDesc3;

    private final ServiceQuiz     serviceQuiz     = new ServiceQuiz();
    private final ServiceGemini   serviceGemini   = new ServiceGemini();
    private final ServiceRappel   serviceRappel   = new ServiceRappel();

    private int scoreBE = 0, scoreST = 0, scoreHU = 0;

    // ══════════════════════════════════════════════════════════════
    private static boolean rappelDejaVerifie = false;

    // ✅ Dans initialize() — vérification rôle
    @FXML
    public void initialize() {
        var role = Session.getRoleConnecte();
        if (role != null
                && role != utils.Session.Role.USER
                && role != utils.Session.Role.ADMIN) {
            if (lblBienvenue != null)
                lblBienvenue.setText("⛔ Accès réservé aux patients.");
            return;
        }

        // ✅ currentPatientId depuis la session
        if (Session.getUserId() > 0) currentPatientId = Session.getUserId();

        configurerCombo();
        loadPatientData(currentPatientId);
        chargerGraphiqueReel();

        if (!rappelDejaVerifie) {
            rappelDejaVerifie = true;
            new Thread(() -> {
                try { new services.ServiceRappel().verifierEtEnvoyerRappels(); }
                catch (Exception e) { System.err.println("❌ Erreur rappels : " + e.getMessage()); }
            }).start();
        }
    }


    public void setParentController(MindCareLayoutController parent) {
        this.parentController = parent;
    }

    public static void ouvrirSuivie(int patientId) {
        currentPatientId = patientId;
    }

    // ══════════════════════════════════════════════════════════════
    // CHARGEMENT DONNÉES PATIENT
    // ══════════════════════════════════════════════════════════════
    private void loadPatientData(int patientId) {
        try {
            String nom = getPatientName(patientId);
            lblBienvenue.setText("Hello " + nom + " ! ✨");
            updateScoresFromDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//new

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

            final int SCORE_MAX = 6;
            int totalBE = 0, countBE = 0;
            int totalST = 0, countST = 0;
            int totalHU = 0, countHU = 0;

            for (String ligne : historique) {
                int score = extraireScore(ligne);
                String titre = extraireTitre(ligne).toLowerCase();
                if      (titre.contains("stress"))  { totalST += score; countST++; }
                else if (titre.contains("humeur"))  { totalHU += score; countHU++; }
                else                                { totalBE += score; countBE++; }
            }

            int totalGlobal = 0;
            for (String ligne : historique) totalGlobal += extraireScore(ligne);
            int moyenneGlobale = totalGlobal / historique.size();

            int moyBE = countBE > 0 ? totalBE / countBE : moyenneGlobale;
            int moyST = countST > 0 ? totalST / countST : moyenneGlobale;
            int moyHU = countHU > 0 ? totalHU / countHU : moyenneGlobale;

            scoreBE = (int) Math.min(100, (moyBE * 100.0) / SCORE_MAX);
            scoreST = (int) Math.max(0, 100 - (moyST * 100.0) / SCORE_MAX);
            scoreHU = (int) Math.max(0, 100 - (moyHU * 100.0) / SCORE_MAX);

            scoreBienEtre.setText(scoreBE + "/100");
            scoreStress.setText(scoreST   + "/100");
            scoreHumeur.setText(scoreHU   + "/100");

            animerArc(arcBienEtre, scoreBE, "#A78BFA");
            animerArc(arcStress,   scoreST, "#FF6B9D");
            animerArc(arcHumeur,   scoreHU, "#4FACFE");

            setTrend(lblTrendBienEtre, +8,  "#A78BFA");
            setTrend(lblTrendStress,   -5,  "#FF6B9D");
            setTrend(lblTrendHumeur,   +12, "#4FACFE");

            int coins = historique.size() * 150 + moyenneGlobale * 10;
            lblCoinsGagnes.setText(coins + " coins");

            if (lblSessionCount != null) {
                lblSessionCount.setText(historique.size() + " session" +
                        (historique.size() > 1 ? "s" : "") + " complétée" +
                        (historique.size() > 1 ? "s" : ""));
            }

            afficherConseil(scoreBE, scoreST, scoreHU, historique.size());
            afficherHistorique(historique);

        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement scores : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // CONSEIL PRINCIPAL + 3 CONSEILS IA
    // ══════════════════════════════════════════════════════════════
    private void afficherConseil(int be, int st, int hu, int nbSessions) {
        if (lblConseil == null) return;

        lblConseil.setText("🤖 Conseil personnalisé en cours...");
        if (lblTitre1 != null) lblTitre1.setText("Chargement...");
        if (lblTitre2 != null) lblTitre2.setText("Chargement...");
        if (lblTitre3 != null) lblTitre3.setText("Chargement...");

        new Thread(() -> {
            String conseil = serviceGemini.genererConseil(be, st, hu, nbSessions);
            List<String[]> trois = serviceGemini.genererTroisConseils(be, st, hu);

            Platform.runLater(() -> {
                if (lblConseil != null) lblConseil.setText(conseil);

                if (trois.size() > 0) {
                    if (lblEmoji1 != null) lblEmoji1.setText(trois.get(0)[0]);
                    if (lblTitre1 != null) lblTitre1.setText(trois.get(0)[1]);
                    if (lblDesc1  != null) lblDesc1.setText(trois.get(0)[2]);
                }
                if (trois.size() > 1) {
                    if (lblEmoji2 != null) lblEmoji2.setText(trois.get(1)[0]);
                    if (lblTitre2 != null) lblTitre2.setText(trois.get(1)[1]);
                    if (lblDesc2  != null) lblDesc2.setText(trois.get(1)[2]);
                }
                if (trois.size() > 2) {
                    if (lblEmoji3 != null) lblEmoji3.setText(trois.get(2)[0]);
                    if (lblTitre3 != null) lblTitre3.setText(trois.get(2)[1]);
                    if (lblDesc3  != null) lblDesc3.setText(trois.get(2)[2]);
                }
            });
        }).start();
    }

    // ══════════════════════════════════════════════════════════════
    // OUVRIR CHAT IA
    // ══════════════════════════════════════════════════════════════
    @FXML
    private void ouvrirChat() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/chatquiz.fxml")
            );
            Node view = loader.load();
            VBox contentArea = (VBox) lblBienvenue.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        final int totalFrames = 60;
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            @Override public void handle(long now) {
                frame[0]++;
                double progress = Math.min(1.0, frame[0] / (double) totalFrames);
                double ease = 1 - Math.pow(1 - progress, 3);
                arc.setLength(targetLength * ease);
                if (frame[0] >= totalFrames) stop();
            }
        };
        timer.start();
    }

    private void setTrend(Label lbl, int delta, String couleur) {
        if (lbl == null) return;
        String signe = delta >= 0 ? "↑ +" : "↓ ";
        lbl.setText(signe + delta + "%");
        lbl.setStyle("-fx-font-size:11px; -fx-font-weight:700; " +
                "-fx-text-fill:" + (delta >= 0 ? "#065F46" : "#9D174D") + ";");
    }

    // ══════════════════════════════════════════════════════════════
    // HISTORIQUE
    // ══════════════════════════════════════════════════════════════
    private static final int SCORE_MAX_HISTORIQUE = 6;

    private void afficherHistorique(List<String> historique) {
        if (historyBox == null) return;
        historyBox.getChildren().clear();
        int debut = Math.max(0, historique.size() - 5);
        for (int i = historique.size() - 1; i >= debut; i--) {
            String ligne      = historique.get(i);
            int    scoreBrut  = extraireScore(ligne);
            String date       = extraireDate(ligne);
            String titre      = extraireTitre(ligne);
            String titreLow   = titre.toLowerCase();
            int    scorePct;

            if (titreLow.contains("stress") || titreLow.contains("humeur")) {
                scorePct = (int) Math.max(0,
                        100 - (scoreBrut * 100.0) / SCORE_MAX_HISTORIQUE);
            } else {
                scorePct = (int) Math.min(100,
                        (scoreBrut * 100.0) / SCORE_MAX_HISTORIQUE);
            }

            String emoji = scorePct >= 70 ? "✅" : scorePct >= 40 ? "🟡" : "🔴";
            Label entry = new Label(
                    date + "  ·  " + titre + "  →  " + scorePct + "/100  " + emoji
            );
            entry.setStyle("-fx-font-size:12px; -fx-text-fill:#374151;" +
                    "-fx-font-weight:600; -fx-padding:6 0;");
            historyBox.getChildren().add(entry);

            javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
            sep.setStyle("-fx-background-color:#F3F4F6;");
            historyBox.getChildren().add(sep);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // GRAPHIQUE
    // ══════════════════════════════════════════════════════════════
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

            final int SCORE_MAX_GRAPH = 6;
            LocalDateTime limite = LocalDateTime.now().minusDays(getSelectedDays());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

            for (String ligne : historique) {
                LocalDateTime dateTime = extraireDateTime(ligne);
                if (dateTime.isBefore(limite)) continue;

                int    score = extraireScore(ligne);
                String date  = dateTime.format(fmt);
                String titre = extraireTitre(ligne).toLowerCase();
                int vBE, vST, vHU;

                if (titre.contains("stress")) {
                    vST = (int) Math.max(0, 100 - (score * 100.0) / SCORE_MAX_GRAPH);
                    vBE = Math.min(100, vST + 10);
                    vHU = Math.min(100, vST + 5);
                } else if (titre.contains("humeur") || titre.contains("bien_etre")) {
                    vHU = (int) Math.max(0, 100 - (score * 100.0) / SCORE_MAX_GRAPH);
                    vBE = Math.min(100, vHU + 8);
                    vST = Math.max(0, 100 - vHU - 10);
                } else {
                    vBE = (int) Math.min(100, (score * 100.0) / SCORE_MAX_GRAPH);
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
            System.err.println("❌ Erreur graphique : " + e.getMessage());
        }
    }

    private void applyStyling() {
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
        if (line != null) line.setStyle("-fx-stroke:" + strokeColor +
                "; -fx-stroke-width:2.5px;");
        chart.lookupAll(".default-color" + index + ".chart-area-symbol")
                .forEach(n -> n.setStyle("-fx-background-color:" + strokeColor + ",white;"));
    }

    // ══════════════════════════════════════════════════════════════
    // EXTRACTION
    // ══════════════════════════════════════════════════════════════
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
            return extraireDateTime(ligne)
                    .format(DateTimeFormatter.ofPattern("dd/MM"));
        } catch (Exception e) { return "--/--"; }
    }

    private String extraireTitre(String ligne) {
        try {
            int start = ligne.indexOf("Quiz: ") + 6;
            int end   = ligne.indexOf(" |", start);
            return ligne.substring(start, end).trim();
        } catch (Exception e) { return "Quiz"; }
    }

    // ══════════════════════════════════════════════════════════════
    // COMBO PÉRIODE
    // ══════════════════════════════════════════════════════════════
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

    // ══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════
    @FXML
    private void ouvrirEspacePraticien() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/EspacePraticien.fxml")
            );
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
    // HELPER NOM PATIENT
    // ══════════════════════════════════════════════════════════════
    private String getPatientName(int id) {
        switch (id) {
            case 1:  return "Mohamed";
            case 4:  return "Meriem";
            case 3:  return "Mariam";
            default: return "Patient";
        }
    }
}