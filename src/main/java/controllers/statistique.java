package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import models.ProgressionModule;
import services.GeminiService;
import services.ProgressionService;
import utils.UserSession;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrôleur pour la vue Statistiques.
 * Calcule le temps passé et la progression réelle de l'utilisateur.
 */
public class statistique {

    @FXML
    private Label timeSpentLabel;
    @FXML
    private Label progressLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label geminiInsightLabel;

    private final ProgressionService progressionService = new ProgressionService();
    private final GeminiService geminiService = new GeminiService();
    private int currentUserId;

    @FXML
    public void initialize() {
        if (UserSession.getInstance().getUser() != null) {
            currentUserId = UserSession.getInstance().getUser().getId();
            loadStats();
        }
    }

    private void loadStats() {
        try {
            // 1. Temps Total
            int totalMinutes = progressionService.getTotalTimeSpent(currentUserId);
            timeSpentLabel.setText(totalMinutes + " min");

            // 2. Progression Globale
            List<ProgressionModule> progs = progressionService.getAllForUser(currentUserId);
            if (!progs.isEmpty()) {
                double avg = progs.stream().mapToDouble(ProgressionModule::getTaux_completion).average().orElse(0.0);
                progressBar.setProgress(avg);
                progressLabel.setText((int) (avg * 100) + "%");

                // 3. Gemini Insight
                refreshAiInsight();
            } else {
                geminiInsightLabel.setText("Commencez votre première formation pour recevoir des suggestions !");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void refreshAiInsight() {
        geminiInsightLabel.setText("L'IA analyse votre parcours...");

        new Thread(() -> {
            try {
                int totalMinutes = progressionService.getTotalTimeSpent(currentUserId);
                List<ProgressionModule> progs = progressionService.getAllForUser(currentUserId);
                double avg = progs.stream().mapToDouble(ProgressionModule::getTaux_completion).average().orElse(0.0);

                String insight = geminiService.getAiInsights(totalMinutes, avg);

                Platform.runLater(() -> geminiInsightLabel.setText(insight));
            } catch (Exception e) {
                Platform.runLater(() -> geminiInsightLabel.setText("L'IA MindCare vous encourage à continuer !"));
            }
        }).start();
    }
}
