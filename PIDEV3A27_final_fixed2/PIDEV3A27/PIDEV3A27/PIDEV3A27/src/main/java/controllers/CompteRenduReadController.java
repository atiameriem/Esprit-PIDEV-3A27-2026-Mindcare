package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import models.CompteRenduSeance;
import models.CompteRenduView;
import services.ServiceCompteRenduSeance;
import utils.MyDatabase;
import utils.Session;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * ROLE PATIENT : lecture seulement des comptes-rendus de SES rendez-vous.
 */
public class CompteRenduReadController {

    @FXML private TextField searchField;
    @FXML private VBox compteRenduContainer;

    private final Connection cnx = MyDatabase.getInstance().getConnection();
    private ServiceCompteRenduSeance crService;

    @FXML
    public void initialize() {
        crService = new ServiceCompteRenduSeance(cnx);
        loadCompteRendus();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> loadCompteRendus());
        }
    }

    private void loadCompteRendus() {
        try {
            //Donc le patient ne voit que ses propres comptes-rendus.
            List<CompteRenduView> list = crService.findViewsByPatient(Session.getUserId());

            String kw = (searchField == null) ? "" : searchField.getText().toLowerCase().trim();
            if (!kw.isEmpty()) {
                list = list.stream().filter(cr ->
                        (cr.getPsychologistFullName() != null && cr.getPsychologistFullName().toLowerCase().contains(kw))
                                || (cr.getResumeSeanceCr() != null && cr.getResumeSeanceCr().toLowerCase().contains(kw))
                                || (cr.getProchainesActionCr() != null && cr.getProchainesActionCr().toLowerCase().contains(kw))
                                || (cr.getProgresCr() != null && cr.getProgresCr().name().toLowerCase().contains(kw))
                ).collect(java.util.stream.Collectors.toList());
            }
            //On efface toutes les cards, puis on reconstruit.
            compteRenduContainer.getChildren().clear();

            if (list.isEmpty()) {
                Label empty = new Label("Aucun compte-rendu trouvé.");
                empty.setStyle("-fx-text-fill:#666; -fx-font-size: 14px; -fx-padding: 10 0 0 0;");
                compteRenduContainer.getChildren().add(empty);
                return;
            }

            HBox row = null;
            for (int i = 0; i < list.size(); i++) {
                if (i % 3 == 0) {
                    row = new HBox(15);
                    compteRenduContainer.getChildren().add(row);
                }
                row.getChildren().add(buildCard(list.get(i)));
            }

        } catch (SQLException e) {
            showError("Erreur chargement", e);
        }
    }

    private VBox buildCard(CompteRenduView cr) {
        VBox card = new VBox(12);
        HBox.setHgrow(card, Priority.ALWAYS);

        card.setStyle("""
                -fx-background-color: #FFFFFF;
                -fx-padding: 18;
                -fx-background-radius: 14;
                -fx-border-radius: 14;
                -fx-border-color: #E7E7E7;
                """);

        // Date RDV
        HBox dateRow = new HBox(8);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        Region calIcon = svgIcon(
                "M7 2v2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2V2h-2v2H9V2H7zm12 6H5v10h14V8z",
                "#2563EB", 14, 14
        );

        String d = cr.getRvDate() == null ? "" : cr.getRvDate().toString();
        String t = cr.getRvTime() == null ? "" : cr.getRvTime().toString();
        Label dateLabel = new Label(d + (t.isBlank() ? "" : "  ·  " + t));
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #111; -fx-font-weight: 600;");
        dateRow.getChildren().addAll(calIcon, dateLabel);

        Label title = new Label("Compte-rendu de séance");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill:#111;");

        // Psychologue
        HBox psyRow = new HBox(8);
        psyRow.setAlignment(Pos.CENTER_LEFT);
        Region userIcon = svgIcon(
                "M12 12c2.761 0 5-2.239 5-5S14.761 2 12 2 7 4.239 7 7s2.239 5 5 5zm0 2c-4.418 0-8 2.239-8 5v1h16v-1c0-2.761-3.582-5-8-5z",
                "#0F766E", 14, 14
        );
        String psyName = (cr.getPsychologistFullName() == null || cr.getPsychologistFullName().isBlank()) ? "Psychologue" : cr.getPsychologistFullName();
        Label psyLabel = new Label(psyName);
        psyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill:#222;");
        psyRow.getChildren().addAll(userIcon, psyLabel);

        Label badge = buildProgressBadge(cr.getProgresCr());

        VBox resumeBox = buildSection("RÉSUMÉ DE SÉANCE", cr.getResumeSeanceCr());
        VBox actionsBox = buildSection("PROCHAINES ACTIONS", cr.getProchainesActionCr());
//buildCard() : Il n’y a pas :
//bouton Ajouter
//bouton Modifier
//bouton Supprimer
//popup showCompteRenduDialog(...)
//handler handleDelete(...)
        card.getChildren().addAll(dateRow, title, psyRow, badge, resumeBox, actionsBox);
        return card;
    }

    private Label buildProgressBadge(CompteRenduSeance.ProgresCR progrescr) {
        String text, style;
        if (progrescr == null) {
            text = "Progression";
            style = "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B;";
        } else {
            switch (progrescr) {
                case amelioration_significative -> {
                    text  = "Amélioration Significative";
                    style = "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A;";
                }
                case amelioration_legere -> {
                    text  = "Amélioration Légère";
                    style = "-fx-background-color: #D1FAE5; -fx-text-fill: #059669;";
                }
                case amelioration_stable -> {
                    text  = "Amélioration Stable";
                    style = "-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8;";
                }
                case stagnation -> {
                    text  = "Stagnation";
                    style = "-fx-background-color: #FEF9C3; -fx-text-fill: #D97706;";
                }
                default -> {
                    text  = progrescr.name();
                    style = "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B;";
                }
            }
        }
        Label badge = new Label("✦  " + text);
        badge.setStyle(style + "-fx-padding: 6 12; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        return badge;
    }

    private VBox buildSection(String sectionTitle, String content) {
        VBox box = new VBox(4);
        Label title = new Label(sectionTitle);
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #111;");

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
