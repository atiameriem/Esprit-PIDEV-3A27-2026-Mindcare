package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import services.ServiceRendezVous;
import utils.MyDatabase;
import utils.Session;

import java.net.URL;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;

public class RendezVousStatsController implements Initializable {

    @FXML private Label lblConfirmed, lblPending, lblCancelled, lblFinished;
    @FXML private Label lblConfirmedPct, lblPendingPct, lblCancelledPct, lblFinishedPct;
    @FXML private ProgressBar pbConfirmed, pbPending, pbCancelled, pbFinished;

    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private Scene previousScene;
    private final Connection cnx = MyDatabase.getInstance().getConnection();
    private ServiceRendezVous service;

    private static final String[] MONTHS = {"Jan", "Fév", "Mar", "Avr", "Mai", "Juin"};

    public void setPreviousScene(Scene previousScene) {
        this.previousScene = previousScene;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        service = new ServiceRendezVous(cnx);
        loadStats();
        // Axe Y en entiers (1,2,3...)
        yAxis.setAutoRanging(false);     // on désactive l'auto pour contrôler les ticks
        yAxis.setLowerBound(0);          // départ à 0 (ou 1 si tu veux)
        yAxis.setUpperBound(10);         // ⚠️ ajuste selon ton max réel
        yAxis.setTickUnit(1);            // pas = 1
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override public String toString(Number object) {
                return String.valueOf(object.intValue()); // force affichage entier
            }
            @Override public Number fromString(String string) {
                return Integer.parseInt(string);
            }
        });
    }

    @FXML
    private void goBack() {
        if (previousScene != null) {
            Stage stage = (Stage) lblConfirmed.getScene().getWindow();
            stage.setScene(previousScene);
            stage.show();
        }
    }

    private void loadStats() {
        try {
            int psyId = Session.getUserId();

            ServiceRendezVous.RendezVousStats stats = service.getStatsForPsychologist(psyId);
            int total = Math.max(stats.total, 1);

            setKpi(lblConfirmed, pbConfirmed, lblConfirmedPct, stats.confirmed, total);
            setKpi(lblPending, pbPending, lblPendingPct, stats.pending, total);
            setKpi(lblCancelled, pbCancelled, lblCancelledPct, stats.cancelled, total);
            setKpi(lblFinished, pbFinished, lblFinishedPct, stats.finished, total);

            int year = LocalDate.now().getYear();
            loadTrendsJanToJun(psyId, year);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setKpi(Label valueLabel, ProgressBar bar, Label pctLabel, int count, int total) {
        valueLabel.setText(String.valueOf(count));
        double ratio = (double) count / (double) total;
        bar.setProgress(ratio);
        pctLabel.setText(Math.round(ratio * 100) + "%");
    }

    // ✅ Affiche seulement les mois existants (pas de 0)
    private void loadTrendsJanToJun(int psyId, int year) {
        barChart.getData().clear();
        barChart.setLegendVisible(true);
        barChart.setAnimated(false);

        xAxis.getCategories().setAll(MONTHS);

        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(true);

        // ✅ BON NOM ICI
        Map<String, Map<Integer, Integer>> data = service.getStatsByTypeForJanToJun(psyId, year);

        for (Map.Entry<String, Map<Integer, Integer>> entry : data.entrySet()) {
            String typeLabel = entry.getKey();
            Map<Integer, Integer> monthCounts = entry.getValue();

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(typeLabel);

            for (int m = 1; m <= 6; m++) {
                Integer value = monthCounts.get(m);
                if (value != null && value > 0) {
                    series.getData().add(new XYChart.Data<>(MONTHS[m - 1], value));
                }
            }

            if (!series.getData().isEmpty()) {
                barChart.getData().add(series);
            }
        }
    }
}