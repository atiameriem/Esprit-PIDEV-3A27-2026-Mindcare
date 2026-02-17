package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import models.RendezVous;
import models.RendezVousView;
import services.ServiceRendezVous;
import utils.MyDatabase;
import utils.Session;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Psychologue : lecture seule de SES rendez-vous.
 */
public class RendezVousController {

    @FXML private TextField searchField;
    @FXML private VBox rendezVousContainer;

    private ServiceRendezVous service;
    private final Connection cnx = MyDatabase.getInstance().getConnection();

    @FXML
    public void initialize() {
        service = new ServiceRendezVous(cnx);
        loadRendezVous();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> loadRendezVous());
        }
    }

    private void loadRendezVous() {
        try {
            int psyId = Session.getUserId();

            // ✅ ICI: utiliser la méthode VIEW (avec noms)
            List<RendezVousView> list = service.findViewsByPsychologist(psyId);

            String kw = (searchField == null) ? "" : searchField.getText().toLowerCase().trim();
            if (!kw.isEmpty()) {
                list = list.stream().filter(rv ->
                        String.valueOf(rv.getIdRv()).contains(kw)
                                || String.valueOf(rv.getIdPatient()).contains(kw)
                                || (rv.getPatientFullName() != null && rv.getPatientFullName().toLowerCase().contains(kw))
                                || (rv.getStatutRv() != null && rv.getStatutRv().name().toLowerCase().contains(kw))
                                || (rv.getTypeRendezVous() != null && rv.getTypeRendezVous().name().toLowerCase().contains(kw))
                                || (rv.getAppointmentDate() != null && rv.getAppointmentDate().toString().toLowerCase().contains(kw))
                                || (rv.getAppointmentTimeRv() != null && rv.getAppointmentTimeRv().toString().toLowerCase().contains(kw))
                ).collect(java.util.stream.Collectors.toList());
            }

            rendezVousContainer.getChildren().clear();

            if (list.isEmpty()) {
                Label empty = new Label("Aucun rendez-vous trouvé.");
                empty.setStyle("-fx-text-fill:#666; -fx-font-size: 14px; -fx-padding: 10 0 0 0;");
                rendezVousContainer.getChildren().add(empty);
                return;
            }

            HBox row = null;
            for (int i = 0; i < list.size(); i++) {
                if (i % 3 == 0) {
                    row = new HBox(15);
                    rendezVousContainer.getChildren().add(row);
                }
                // ✅ ICI: list.get(i) est maintenant RendezVousView
                row.getChildren().add(buildCard(list.get(i)));
            }

        } catch (SQLException e) {
            showError("Erreur chargement rendez-vous", e);
        }
    }


    private VBox buildCard(RendezVousView rv) {
        VBox card = new VBox(12);
        HBox.setHgrow(card, Priority.ALWAYS);

        card.setStyle("""
                -fx-background-color: #F9F9F9;
                -fx-padding: 20;
                -fx-background-radius: 10;
                -fx-border-radius: 10;
                -fx-border-color: #E7E7E7;
                """);

        // Date + time row
        HBox dateRow = new HBox(8);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        Region calIcon = svgIcon(
                "M7 2v2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2V2h-2v2H9V2H7zm12 6H5v10h14V8z",
                "#1D4ED8", 14, 14
        );

        String dateTxt = rv.getAppointmentDate() == null ? "" : rv.getAppointmentDate().toString();
        String timeTxt = rv.getAppointmentTimeRv() == null ? "" : rv.getAppointmentTimeRv().toString();
        Label dateLabel = new Label(dateTxt + "  ·  " + timeTxt);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #444; -fx-font-weight: 600;");

        dateRow.getChildren().addAll(calIcon, dateLabel);

        // Title
        Label title = new Label("Rendez-vous");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill:#111;");
        // Psychologue row (afficher le nom du psy)
        HBox psyRow = new HBox(8);
        psyRow.setAlignment(Pos.CENTER_LEFT);

        Region psyIcon = svgIcon(
                "M12 12c2.761 0 5-2.239 5-5S14.761 2 12 2 7 4.239 7 7s2.239 5 5 5zm0 2c-4.418 0-8 2.239-8 5v1h16v-1c0-2.761-3.582-5-8-5z",
                "#2563EB", 14, 14
        );

        String psyName = (rv.getPsychologistFullName() == null || rv.getPsychologistFullName().isBlank())
                ? Session.getFullName()
                : rv.getPsychologistFullName();

        Label psyLabel = new Label(psyName);
        psyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill:#222;");

        psyRow.getChildren().addAll(psyIcon, psyLabel);


        // Badges
        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label statutBadge = buildStatutBadge(rv.getStatutRv());
        Label typeBadge = buildTypeBadge(rv.getTypeRendezVous());

        badges.getChildren().addAll(statutBadge, typeBadge);

        // Details
        String info = "Patient : " + (rv.getPatientFullName()==null? "" : rv.getPatientFullName());
        VBox details = buildSection("INFORMATIONS", info, "ℹ️");

        card.getChildren().addAll(dateRow, title, psyRow, badges, details);
        return card;
    }

    private Label buildStatutBadge(RendezVous.StatutRV statut) {
        String text;
        String style;

        if (statut == null) {
            text = "Statut inconnu";
            style = "-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;";
        } else {
            switch (statut) {
                case prevu -> {
                    text = "Prévu";
                    style = "-fx-background-color:#DBEAFE; -fx-text-fill:#1D4ED8;";
                }
                case termine -> {
                    text = "Terminé";
                    style = "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A;";
                }
                case annule -> {
                    text = "Annulé";
                    style = "-fx-background-color:#FEE2E2; -fx-text-fill:#DC2626;";
                }
                default -> {
                    text = statut.name();
                    style = "-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;";
                }
            }
        }

        Label badge = new Label("📌  " + text);
        badge.setStyle(style + "-fx-padding: 6 12; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        return badge;
    }

    private Label buildTypeBadge(RendezVous.TypeRV type) {
        String text;
        String style;

        if (type == null) {
            text = "Type inconnu";
            style = "-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;";
        } else {
            switch (type) {
                case premiere_consultation -> {
                    text = "Première consultation";
                    style = "-fx-background-color:#FEF9C3; -fx-text-fill:#B45309;";
                }
                case suivi -> {
                    text = "Suivi";
                    style = "-fx-background-color:#E0E7FF; -fx-text-fill:#3730A3;";
                }
                case urgence -> {
                    text = "Urgence";
                    style = "-fx-background-color:#FFEDD5; -fx-text-fill:#C2410C;";
                }
                default -> {
                    text = type.name();
                    style = "-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;";
                }
            }
        }

        Label badge = new Label("🏷️  " + text);
        badge.setStyle(style + "-fx-padding: 6 12; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        return badge;
    }

    private VBox buildSection(String titleText, String content, String emoji) {
        VBox box = new VBox(4);

        Label title = new Label(emoji + "  " + titleText);
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #000;");

        Label body = new Label(content == null ? "" : content);
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: #222; -fx-font-size: 13px;");

        box.getChildren().addAll(title, body);
        return box;
    }

    private Region svgIcon(String path, String colorHex, double w, double h) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setStyle("-fx-fill: " + colorHex + ";");

        StackPane wrap = new StackPane(svg);
        wrap.setMinSize(w, h);
        wrap.setPrefSize(w, h);
        wrap.setMaxSize(w, h);
        wrap.setAlignment(Pos.CENTER);
        return wrap;
    }

    private void showError(String title, Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(e.getMessage());
        a.showAndWait();
    }
}
