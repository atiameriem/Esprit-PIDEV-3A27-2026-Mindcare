package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import services.ServiceQuiz;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SuivieController {

    private MindCareLayoutController parentController;
    private static int currentPatientId = 2;

    @FXML private Label lblBienvenue;
    @FXML private Label lblCoinsGagnes;
    @FXML private Circle circleFrancais;
    @FXML private Label scoreFrancais;
    @FXML private Circle circleAnglais;
    @FXML private Label scoreAnglais;
    @FXML private Circle circleMaths;
    @FXML private Label scoreMaths;
    @FXML private ComboBox<String> comboPeriode;
    @FXML private AreaChart<String, Number> evolutionChart;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();

    @FXML
    public void initialize() {
        loadPatientData(currentPatientId);
        configurerCombo();
        chargerGraphiqueReel();
    }

    public void setParentController(MindCareLayoutController parent) {
        this.parentController = parent;
    }

    public static void ouvrirSuivie(int patientId) {
        currentPatientId = patientId;
    }

    // ────────────── Chargement des données patient ──────────────
    private void loadPatientData(int patientId) {
        try {
            String patientName = getPatientName(patientId);
            lblBienvenue.setText("Hello " + patientName + " ! ✨");
            updateScoresFromDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateScoresFromDB() {
        try {
            List<String> historique = serviceQuiz.getHistoriquePatient(currentPatientId);

            if (historique.isEmpty()) {
                scoreFrancais.setText("0/100");
                scoreAnglais.setText("0/100");
                scoreMaths.setText("0/100");
                lblCoinsGagnes.setText("0 coins");
                updateCircle(circleFrancais, 0);
                updateCircle(circleAnglais, 0);
                updateCircle(circleMaths, 0);
                return;
            }

            int totalScore = 0;
            for (String ligne : historique) totalScore += extraireScore(ligne);
            int scoreMoyen = totalScore / historique.size();

            Random rand = new Random(currentPatientId);
            int scoreBienEtre = Math.min(100, scoreMoyen + rand.nextInt(15));
            int scoreStress = Math.max(0, scoreMoyen - rand.nextInt(20));
            int scoreHumeur = Math.min(100, scoreMoyen + rand.nextInt(10));

            scoreFrancais.setText(scoreBienEtre + "/100");
            scoreAnglais.setText(scoreStress + "/100");
            scoreMaths.setText(scoreHumeur + "/100");

            updateCircle(circleFrancais, scoreBienEtre);
            updateCircle(circleAnglais, scoreStress);
            updateCircle(circleMaths, scoreHumeur);

            int coins = historique.size() * 150 + scoreMoyen * 10;
            lblCoinsGagnes.setText(coins + " coins");

        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement scores : " + e.getMessage());
        }
    }

    // ────────────── Graphique évolutif ──────────────
    private void chargerGraphiqueReel() {
        evolutionChart.getData().clear();

        try {
            List<String> historique = serviceQuiz.getHistoriquePatient(currentPatientId);
            if (historique.isEmpty()) return;

            XYChart.Series<String, Number> seriesBienEtre = new XYChart.Series<>();
            seriesBienEtre.setName("Bien-être");
            XYChart.Series<String, Number> seriesStress = new XYChart.Series<>();
            seriesStress.setName("Stress");
            XYChart.Series<String, Number> seriesHumeur = new XYChart.Series<>();
            seriesHumeur.setName("Humeur");

            Random rand = new Random(currentPatientId);
            LocalDateTime limite = LocalDateTime.now().minusDays(getSelectedDays());

            for (String ligne : historique) {
                LocalDateTime dateTime = extraireDateTime(ligne);
                if (dateTime.isBefore(limite)) continue;

                int score = extraireScore(ligne);
                String date = dateTime.format(DateTimeFormatter.ofPattern("dd/MM"));

                seriesBienEtre.getData().add(new XYChart.Data<>(date, Math.min(100, score + rand.nextDouble() * 10)));
                seriesStress.getData().add(new XYChart.Data<>(date, Math.max(0, score - rand.nextDouble() * 15)));
                seriesHumeur.getData().add(new XYChart.Data<>(date, Math.min(100, score + rand.nextDouble() * 8)));
            }

            evolutionChart.getData().addAll(seriesBienEtre, seriesStress, seriesHumeur);
            applyStyling();

        } catch (SQLException e) {
            System.err.println("❌ Erreur graphique : " + e.getMessage());
        }
    }

    private void applyStyling() {
        evolutionChart.setStyle("-fx-background-color: transparent;");
        evolutionChart.applyCss();
        evolutionChart.layout();

        try {
            // Couleurs cohérentes avec les cercles
            Node fillVert = evolutionChart.lookup(".default-color0.chart-series-area-fill");
            Node lineVert = evolutionChart.lookup(".default-color0.chart-series-area-line");
            if (fillVert != null) fillVert.setStyle("-fx-fill: rgba(167,139,250,0.3);");
            if (lineVert != null) lineVert.setStyle("-fx-stroke: #A78BFA; -fx-stroke-width: 3px;");

            Node fillRose = evolutionChart.lookup(".default-color1.chart-series-area-fill");
            Node lineRose = evolutionChart.lookup(".default-color1.chart-series-area-line");
            if (fillRose != null) fillRose.setStyle("-fx-fill: rgba(255,107,157,0.3);");
            if (lineRose != null) lineRose.setStyle("-fx-stroke: #FF6B9D; -fx-stroke-width: 3px;");

            Node fillBleu = evolutionChart.lookup(".default-color2.chart-series-area-fill");
            Node lineBleu = evolutionChart.lookup(".default-color2.chart-series-area-line");
            if (fillBleu != null) fillBleu.setStyle("-fx-fill: rgba(79,172,254,0.3);");
            if (lineBleu != null) lineBleu.setStyle("-fx-stroke: #4FACFE; -fx-stroke-width: 3px;");

            Node plot = evolutionChart.lookup(".chart-plot-background");
            if (plot != null) plot.setStyle("-fx-background-color: #FAFBFC;");
        } catch (Exception ignored) {}
    }

    // ────────────── Extraction score et date ──────────────
    private int extraireScore(String ligne) {
        try {
            int start = ligne.indexOf("Score: ") + 7;
            int end = ligne.indexOf(" |", start);
            return Integer.parseInt(ligne.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private LocalDateTime extraireDateTime(String ligne) {
        try {
            int start = ligne.indexOf("Date: ") + 6;
            String dateStr = ligne.substring(start).trim();
            return LocalDateTime.parse(dateStr);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    // ────────────── Cercles de progression ──────────────
    private void updateCircle(Circle circle, int score) {
        double percentage = score / 100.0;
        double radius = circle.getRadius();
        double circumference = 2 * Math.PI * radius;
        double dashOffset = circumference * (1 - percentage);
        circle.setStyle("-fx-stroke-dasharray: " + circumference + "; -fx-stroke-dashoffset: " + dashOffset + ";");
    }

    // ────────────── ComboBox Période ──────────────
    private void configurerCombo() {
        comboPeriode.getItems().addAll("7 jours", "30 jours", "90 jours");
        comboPeriode.getSelectionModel().select("30 jours");
        comboPeriode.setOnAction(e -> chargerGraphiqueReel());
    }

    private int getSelectedDays() {
        String sel = comboPeriode.getSelectionModel().getSelectedItem();
        switch (sel) {
            case "7 jours": return 7;
            case "30 jours": return 30;
            case "90 jours": return 90;
            default: return 30;
        }
    }

    // ────────────── Noms fictifs des patients ──────────────
    private String getPatientName(int patientId) {
        switch (patientId) {
            case 1: return "Mohamed";
            case 2: return "Meriem";
            case 3: return "Mariem";
            default: return "Patient";
        }
    }

    // ────────────── Navigation vers espace psychologue ──────────────
    @FXML
    private void ouvrirEspacePraticien() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EspacePraticien.fxml"));
            Node view = loader.load();
            VBox contentArea = (VBox) lblBienvenue.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void retourSuivie() {
        if (parentController != null) parentController.loadAccueil();
    }
}