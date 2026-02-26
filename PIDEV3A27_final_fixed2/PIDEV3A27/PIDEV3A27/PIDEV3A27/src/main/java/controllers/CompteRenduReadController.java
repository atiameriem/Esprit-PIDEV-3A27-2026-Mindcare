package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import models.CompteRenduSeance;
import models.CompteRenduView;
import services.PdfCompteRenduService;
import services.ServiceCompteRenduSeance;
import services.Paginator;
import utils.MyDatabase;
import utils.Session;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * ROLE PATIENT : lecture seulement des comptes-rendus de SES rendez-vous.
 */
public class CompteRenduReadController {

    @FXML private TextField searchField;
    @FXML private VBox compteRenduContainer;

    // ✅ Pagination UI
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label pageLabel;
    @FXML private ComboBox<Integer> pageSizeCombo;

    private final Paginator paginator = new Paginator();

    private final Connection cnx = MyDatabase.getInstance().getConnection();
    private ServiceCompteRenduSeance crService;

    @FXML
    public void initialize() {
        crService = new ServiceCompteRenduSeance(cnx);
        // ✅ Pagination setup (4,6,8,10)
        if (pageSizeCombo != null) {
            pageSizeCombo.setItems(FXCollections.observableArrayList(4, 6, 8, 10));
            pageSizeCombo.setValue(4);
            paginator.setPageSize(4);
            pageSizeCombo.valueProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    paginator.setPageSize(n);
                    loadCompteRendus();
                }
            });
        }

        loadCompteRendus();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> { paginator.first(); loadCompteRendus(); });
        }
    }

    // ── Pagination actions ───────────────────────────────────────────────
    @FXML
    private void prevPage() {
        if (paginator.canPrev()) {
            paginator.prev();
            loadCompteRendus();
        }
    }

    @FXML
    private void nextPage() {
        if (paginator.canNext()) {
            paginator.next();
            loadCompteRendus();
        }
    }

    private void updatePaginationUI() {
        int tp = paginator.getTotalPages();
        int p = paginator.getPage();

        if (tp <= 0) {
            if (pageLabel != null) pageLabel.setText("Page 1/1");
            if (btnPrev != null) btnPrev.setDisable(true);
            if (btnNext != null) btnNext.setDisable(true);
            return;
        }

        if (pageLabel != null) pageLabel.setText("Page " + p + "/" + tp);
        if (btnPrev != null) btnPrev.setDisable(!paginator.canPrev());
        if (btnNext != null) btnNext.setDisable(!paginator.canNext());
    }

    private void loadCompteRendus() {
        try {
            List<CompteRenduView> list = crService.findViewsByPatient(Session.getUserId());

            String kw = (searchField == null) ? "" : searchField.getText().toLowerCase().trim();
            if (!kw.isEmpty()) {
                list = list.stream().filter(cr ->
                        (cr.getPsychologistFullName() != null && cr.getPsychologistFullName().toLowerCase().contains(kw))
                                || (cr.getResumeSeanceCr() != null && cr.getResumeSeanceCr().toLowerCase().contains(kw))
                                || (cr.getProchainesActionCr() != null && cr.getProchainesActionCr().toLowerCase().contains(kw))
                                || (cr.getProgresCr() != null && cr.getProgresCr().name().toLowerCase().contains(kw))
                ).toList();
            }


            // ✅ Pagination (après filtre)
            paginator.setTotalItems(list.size());
            int from = paginator.getOffset();
            if (from >= list.size() && paginator.getTotalPages() > 0) {
                paginator.setPage(paginator.getTotalPages());
                from = paginator.getOffset();
            }
            int to = Math.min(from + paginator.getPageSize(), list.size());
            List<CompteRenduView> pageList = (from < to) ? list.subList(from, to) : java.util.Collections.emptyList();

            updatePaginationUI();

            compteRenduContainer.getChildren().clear();


            if (pageList.isEmpty()) {
                Label empty = new Label("Aucun compte-rendu trouvé.");
                empty.setStyle("-fx-text-fill:#666; -fx-font-size: 14px; -fx-padding: 10 0 0 0;");
                compteRenduContainer.getChildren().add(empty);
                return;
            }

            HBox row = null;
            for (int i = 0; i < pageList.size(); i++) {
                if (i % 2 == 0) {
                    row = new HBox(15);
                    compteRenduContainer.getChildren().add(row);
                }
                row.getChildren().add(buildCard(pageList.get(i)));
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
        String psyName = (cr.getPsychologistFullName() == null || cr.getPsychologistFullName().isBlank())
                ? "Psychologue"
                : cr.getPsychologistFullName();
        Label psyLabel = new Label(psyName);
        psyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill:#222;");
        psyRow.getChildren().addAll(userIcon, psyLabel);

        Label badge = buildProgressBadge(cr.getProgresCr());
        HBox ratingRow = buildRatingRow(cr);

        VBox resumeBox = buildSection("RÉSUMÉ DE SÉANCE", cr.getResumeSeanceCr());
        VBox actionsBox = buildSection("PROCHAINES ACTIONS", cr.getProchainesActionCr());

        // ✅ Icône export PDF (comme psy)
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button pdfBtn = iconButton(
                "M6 2h7l5 5v15a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm7 1.5V8h4.5",
                "#0F766E"
        );
        pdfBtn.setTooltip(new Tooltip("Exporter PDF"));
        pdfBtn.setOnAction(e -> handleExportPdf(cr));

        actionRow.getChildren().add(pdfBtn);

        card.getChildren().addAll(dateRow, title, psyRow, badge, ratingRow, resumeBox, actionsBox, actionRow);
        return card;
    }

    // ✅ EXPORT PDF
    private void handleExportPdf(CompteRenduView cr) {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Exporter le compte-rendu en PDF");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));

            String psyName = (cr.getPsychologistFullName() == null) ? "psychologue" : cr.getPsychologistFullName();
            psyName = psyName.replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replace(" ", "_");

            String fileName = "CompteRendu_" + psyName + "_" +
                    (cr.getRvDate() == null ? "" : cr.getRvDate().toString()) + ".pdf";

            fc.setInitialFileName(fileName);

            File file = fc.showSaveDialog(compteRenduContainer.getScene().getWindow());
            if (file == null) return;

            Path out = file.toPath();
            PdfCompteRenduService.exportCompteRendu(cr, out, "Patient");

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            stylePopup(success.getDialogPane());
            success.setTitle("Export réussi");
            success.setHeaderText("PDF exporté avec succès ✅");
            success.setContentText(file.getAbsolutePath());
            success.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur export PDF", ex);
        }
    }

    // ─────────────── Rating (garder comme ton code) ───────────────
    private HBox buildRatingRow(CompteRenduView cr) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("Votre note :");
        label.setStyle("-fx-font-size: 12px; -fx-text-fill:#334155; -fx-font-weight: 700;");

        Label stars = new Label(renderStars(cr.getRating()));
        stars.setStyle("-fx-font-size: 16px; -fx-text-fill:#F59E0B; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button rateBtn = new Button(cr.getRating() == null ? "⭐ Noter" : "⭐ Modifier");
        rateBtn.setStyle("-fx-background-color: rgba(245,158,11,0.12); -fx-text-fill:#B45309; -fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 6 10;");

        rateBtn.setOnAction(e -> {
            Integer newRating = showRatingDialog(cr.getRating());
            if (newRating == null) return;
            try {
                crService.updateRatingForPatient(cr.getIdCompteRendu(), Session.getUserId(), newRating);
                loadCompteRendus();
            } catch (Exception ex) {
                showError("Erreur rating", ex);
            }
        });

        row.getChildren().addAll(label, stars, spacer, rateBtn);
        return row;
    }

    private String renderStars(Integer rating) {
        int r = (rating == null) ? 0 : Math.max(0, Math.min(5, rating));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= r ? '★' : '☆');
        return sb.toString();
    }

    private Integer showRatingDialog(Integer currentRating) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Noter la séance");
        dialog.setHeaderText("Donnez une note de 1 à 5 étoiles");

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        HBox starsBox = new HBox(8);
        starsBox.setAlignment(Pos.CENTER_LEFT);
        starsBox.setStyle("-fx-padding: 10 0 0 0;");

        final int[] selected = { (currentRating == null) ? 0 : currentRating };

        Label[] stars = new Label[5];
        for (int i = 0; i < 5; i++) {
            Label star = new Label("☆");
            star.setStyle("-fx-font-size: 28px; -fx-text-fill:#F59E0B; -fx-cursor: hand;");
            final int val = i + 1;
            star.setOnMouseClicked(ev -> {
                selected[0] = val;
                for (int k = 0; k < 5; k++) stars[k].setText(k < selected[0] ? "★" : "☆");
            });
            stars[i] = star;
            starsBox.getChildren().add(star);
        }
        for (int k = 0; k < 5; k++) stars[k].setText(k < selected[0] ? "★" : "☆");

        VBox content = new VBox(8);
        Label hint = new Label("Cliquez sur les étoiles pour choisir la note.");
        hint.setStyle("-fx-text-fill:#475569; -fx-font-size: 12px;");
        content.getChildren().addAll(hint, starsBox);
        dialog.getDialogPane().setContent(content);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        saveButton.setDisable(selected[0] == 0);
        starsBox.setOnMouseClicked(ev -> saveButton.setDisable(selected[0] == 0));

        dialog.setResultConverter(btn -> btn == saveBtn ? (selected[0] == 0 ? null : selected[0]) : null);
        return dialog.showAndWait().orElse(null);
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

    private Button iconButton(String svgPath, String colorHex) {
        Region icon = svgIcon(svgPath, colorHex, 18, 18);
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        return btn;
    }

    private void showError(String title, Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        stylePopup(a.getDialogPane());
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(e.getMessage());
        a.showAndWait();
    }

    // ===================== Popup theme helper =====================
    private void stylePopup(DialogPane pane) {
        try {
            if (pane == null) return;
            pane.getStyleClass().add("mc-dialog");
            var css = getClass().getResource("/popup.css");
            if (css != null) pane.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {
        }
    }
}