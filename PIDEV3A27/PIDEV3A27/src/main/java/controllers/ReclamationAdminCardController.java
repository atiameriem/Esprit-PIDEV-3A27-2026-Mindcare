package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import models.Reclamation;
import models.User;
import services.ReclamationService;
import services.UserService;
import services.GrokService;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.sql.SQLException;

public class ReclamationAdminCardController {

    @FXML
    private Label userNameLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label urgencyLabel;
    @FXML
    private Label objetLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label statutLabel;
    @FXML
    private Label doneLabel;
    @FXML
    private Button repondreBtn;
    @FXML
    private VBox reponseContainerAdmin;
    @FXML
    private Label reponseLabelAdmin;
    @FXML
    private Label categorieLabel;
    @FXML
    private VBox resumeContainer;
    @FXML
    private Label resumeLabel;
    @FXML
    private HBox urgencyContainer;
    @FXML
    private HBox categorieContainer;
    @FXML
    private Region priorityStrip;

    private Reclamation reclamation;
    private AdminReclamationsController parentController;
    private String userName = "";
    private final UserService userService = new UserService();
    private final GrokService grokService = new GrokService();
    private final ReclamationService reclamationService = new ReclamationService();

    public void setData(Reclamation reclamation, AdminReclamationsController parentController) {
        this.reclamation = reclamation;
        this.parentController = parentController;

        // Fetch user name
        try {
            User user = userService.getById(reclamation.getIdUser());
            if (user != null) {
                userName = user.getNom() + " " + user.getPrenom();
            } else {
                userName = "Utilisateur #" + reclamation.getIdUser();
            }
        } catch (SQLException e) {
            userName = "Utilisateur #" + reclamation.getIdUser();
        }
        userNameLabel.setText(userName);

        dateLabel.setText(reclamation.getDate() != null ? reclamation.getDate().toString() : "N/A");
        objetLabel.setText(reclamation.getObjet());
        descriptionLabel.setText(reclamation.getDescription());
        urgencyLabel.setText(reclamation.getUrgence());
        statutLabel.setText(reclamation.getStatut());

        // Status UI
        boolean traite = "Traité".equalsIgnoreCase(reclamation.getStatut());
        repondreBtn.setVisible(!traite);
        repondreBtn.setManaged(!traite);
        doneLabel.setVisible(traite);
        doneLabel.setManaged(traite);
        statutLabel.setStyle(traite
                ? "-fx-text-fill: #16A34A; -fx-font-weight: bold;"
                : "-fx-text-fill: #D97706; -fx-font-weight: bold;");

        // Response display
        if (reponseContainerAdmin != null) {
            String reponse = reclamation.getReponse();
            boolean hasReponse = reponse != null && !reponse.trim().isEmpty();
            reponseContainerAdmin.setVisible(hasReponse);
            reponseContainerAdmin.setManaged(hasReponse);
            if (hasReponse && reponseLabelAdmin != null) {
                reponseLabelAdmin.setText(reponse);
            }
        }

        // Categorie section
        if (categorieContainer != null) {
            String cat = reclamation.getCategorie();
            boolean hasCat = cat != null && !cat.trim().isEmpty();
            categorieContainer.setVisible(hasCat);
            categorieContainer.setManaged(hasCat);
            if (hasCat && categorieLabel != null) {
                categorieLabel.setText(cat.toUpperCase());
                categorieContainer.setStyle(
                        "-fx-background-color: #E0E7FF; -fx-padding: 5 12; -fx-background-radius: 8; -fx-border-color: #C7D2FE; -fx-border-width: 1;");
                categorieLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #4338CA;");
            }
        }

        // Resume section
        if (resumeContainer != null) {
            String resume = reclamation.getResume();
            boolean hasResume = resume != null && !resume.trim().isEmpty() && !resume.contains("Aucun résumé");

            if (!hasResume && reclamation.getDescription() != null && reclamation.getDescription().length() > 20) {
                // Auto-generate if missing (Summary + Professional Suggestion)
                grokService.summarizeReclamation(reclamation.getObjet(), reclamation.getDescription())
                        .thenCombine(
                                grokService.suggestProfessional(reclamation.getObjet(), reclamation.getDescription()),
                                (summary, profession) -> {
                                    String completeResume = summary + "\n\n🎯 PROFESSIONNEL CONSEILLÉ : " + profession;
                                    reclamation.setResume(completeResume);
                                    return completeResume;
                                })
                        .thenAccept(completeResume -> {
                            try {
                                reclamationService.update(reclamation);
                                Platform.runLater(() -> {
                                    resumeLabel.setText(completeResume);
                                    resumeContainer.setVisible(true);
                                    resumeContainer.setManaged(true);
                                });
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        });
            } else if (hasResume) {
                resumeContainer.setVisible(true);
                resumeContainer.setManaged(true);
                resumeLabel.setText(resume);
            } else {
                resumeContainer.setVisible(false);
                resumeContainer.setManaged(false);
            }
        }

        // Urgency color & priority strip
        String urgence = reclamation.getUrgence() != null ? reclamation.getUrgence().toUpperCase() : "";
        switch (urgence) {
            case "HIGH":
                urgencyContainer
                        .setStyle("-fx-background-color: #FEE2E2; -fx-padding: 5 12; -fx-background-radius: 8;");
                urgencyLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #EF4444;");
                priorityStrip.setStyle("-fx-background-color: #EF4444; -fx-background-radius: 22 0 0 22;");
                break;
            case "MEDIUM":
                urgencyContainer
                        .setStyle("-fx-background-color: #FEF3C7; -fx-padding: 5 12; -fx-background-radius: 8;");
                urgencyLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #D97706;");
                priorityStrip.setStyle("-fx-background-color: #F59E0B; -fx-background-radius: 22 0 0 22;");
                break;
            default:
                urgencyContainer
                        .setStyle("-fx-background-color: #DCFCE7; -fx-padding: 5 12; -fx-background-radius: 8;");
                urgencyLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #16A34A;");
                priorityStrip.setStyle("-fx-background-color: #10B981; -fx-background-radius: 22 0 0 22;");
        }
    }

    @FXML
    private void handleRepondre() {
        // Open the admin respond dialog (FXML-based)
        AdminRepondreController.open(reclamation, userName, parentController);
    }
}
