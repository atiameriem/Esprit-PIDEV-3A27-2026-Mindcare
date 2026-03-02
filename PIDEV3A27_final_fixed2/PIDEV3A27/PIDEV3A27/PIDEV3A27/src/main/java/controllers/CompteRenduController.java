package controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import models.CompteRenduSeance;
import models.CompteRenduView;
import models.RendezVousView;
import services.LlmSummaryService;
import services.PdfCompteRenduService;
import services.Paginator;
import services.ServiceCompteRenduSeance;
import services.ServiceRendezVous;
import utils.MyDatabase;
import utils.Session;
import utils.ValidationUtils;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ROLE PSYCHOLOGUE :
 * - Affiche uniquement les comptes-rendus des RDV du psy connecté
 * - Ajout/Edit/Supp via POPUP
 * - Ajout : choisir d'abord le rendez-vous (RDV du psy), ensuite créer le compte-rendu
 */
public class CompteRenduController {

    @FXML private TextField searchField;
    @FXML private VBox compteRenduContainer;
    @FXML private Button newCompteRenduButton;

    // ✅ Pagination UI
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label pageLabel; //Page X/Y
    @FXML private ComboBox<Integer> pageSizeCombo; //taille page (4/6/8/10)

    //Création de l’objet paginator : garde page actuelle, taille, total items…
    private final Paginator paginator = new Paginator();

    private final Connection cnx = MyDatabase.getInstance().getConnection();
    private ServiceCompteRenduSeance crService;
    private ServiceRendezVous rvService;

    @FXML
    //Méthode automatique exécutée quand l’écran (FXML) est chargé.
    public void initialize() {
        //Création des services en passant la connexion DB.
        crService = new ServiceCompteRenduSeance(cnx);
        rvService = new ServiceRendezVous(cnx);

        // Vérifie que la ComboBox existe
        if (pageSizeCombo != null) {
            //Met les valeurs possibles dans la ComboBox.
            pageSizeCombo.setItems(FXCollections.observableArrayList(4, 6, 8, 10));
            pageSizeCombo.setValue(4);
            //Dit au paginator : 4 éléments par page.
            paginator.setPageSize(4);
            //Écouteur : si l’utilisateur change la valeur…
            pageSizeCombo.valueProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    paginator.setPageSize(n);
                    loadCompteRendus();
                }
            });
        }

        loadCompteRendus();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> {
                //Dès que le texte change…
                //Revient à la première page

                paginator.first();
                loadCompteRendus();
            });
        }
    }

    // ── Pagination actions ───────────────────────────────────────────────
    //Méthode appelée par le bouton “Précédent”.
    @FXML
    private void prevPage() {
        //Si on peut reculer (page > 1)
        if (paginator.canPrev()) {
            paginator.prev();
            loadCompteRendus();
        }
    }

    @FXML
    //Méthode appelée par bouton “Suivant”.
    //Si une page suivante existe…
    private void nextPage() {
        if (paginator.canNext()) {
            paginator.next();
            loadCompteRendus();
        }
    }

    private void updatePaginationUI() {
        //tp = total pages,
        // p = page actuelle.
        int tp = paginator.getTotalPages();
        int p = paginator.getPage();

        //Cas où il n’y a aucune donnée.
        //Affiche Page 1/1.
        //Désactive les boutons.
        if (tp <= 0) {
            if (pageLabel != null) pageLabel.setText("Page 1/1");
            if (btnPrev != null) btnPrev.setDisable(true);
            if (btnNext != null) btnNext.setDisable(true);
            return;
        }
        //Affiche “Page X/Y”.
        //Désactive/active selon possibilité.
        if (pageLabel != null) pageLabel.setText("Page " + p + "/" + tp);
        if (btnPrev != null) btnPrev.setDisable(!paginator.canPrev());
        if (btnNext != null) btnNext.setDisable(!paginator.canNext());
    }

    // ── Bouton "Nouveau Compte-rendu" ──────────────────────────────────────
    @FXML
    private void handleNewCompteRendu() {
        try {
            //Récupère l’id du psy connecté.
            int idPsy = Session.getUserId();
            //Charge ses RDV depuis DB.
            List<RendezVousView> rdvs = rvService.findViewsByPsychologist(idPsy);

            // Filtre : on garde seulement RDV confirmés ET terminés.
            rdvs = rdvs.stream().filter(rv ->
                    rv.getConfirmationStatus() == models.RendezVous.ConfirmationStatus.confirme
                            && rv.getStatutRv() == models.RendezVous.StatutRV.termine
            ).toList();

            if (rdvs.isEmpty()) {
                info("Aucune consultation terminée",
                        "Le compte-rendu est disponible uniquement pour les rendez-vous confirmés et terminés.");
                return;
            }

            RendezVousView selected = chooseRendezVous(rdvs);
            if (selected == null) return;

            if (!crService.appointmentBelongsToPsychologist(selected.getIdRv(), idPsy)) {
                error("Accès refusé", "Ce rendez-vous ne vous appartient pas.");
                return;
            }

            showCompteRenduDialog("Nouveau Compte-rendu", null, selected);

        } catch (SQLException e) {
            showError("Erreur", e);
        }
    }

    private RendezVousView chooseRendezVous(List<RendezVousView> rdvs) {
        ChoiceDialog<RendezVousView> dialog = new ChoiceDialog<>(rdvs.get(0), rdvs);
        dialog.setTitle("Choisir un rendez-vous");
        dialog.setHeaderText("Sélectionnez le rendez-vous pour créer le compte-rendu");
        dialog.setContentText("Rendez-vous :");
        stylePopup(dialog.getDialogPane());
        Optional<RendezVousView> res = dialog.showAndWait();
        return res.orElse(null);
    }

    // ── Chargement / filtre ───────────────────────────────────────────────
    private void loadCompteRendus() {
        try {
            List<CompteRenduView> list = crService.findViewsByPsychologist(Session.getUserId());

            String kw = (searchField == null) ? "" : searchField.getText().toLowerCase().trim();
            if (!kw.isEmpty()) {
                list = list.stream().filter(cr ->
                        (cr.getPatientFullName() != null && cr.getPatientFullName().toLowerCase().contains(kw))
                                || (cr.getResumeSeanceCr() != null && cr.getResumeSeanceCr().toLowerCase().contains(kw))
                                || (cr.getProchainesActionCr() != null && cr.getProchainesActionCr().toLowerCase().contains(kw))
                                || (cr.getProgresCr() != null && cr.getProgresCr().name().toLowerCase().contains(kw))
                                || String.valueOf(cr.getIdAppointment()).contains(kw)
                ).collect(Collectors.toList());
            }

            // On dit au paginator combien d’items total on a (après filtre).
            paginator.setTotalItems(list.size());
            //from = index de départ de la page actuelle.
            int from = paginator.getOffset();
            //Si on est sur une page qui n’existe plus
            //tu étais page 4 avant, puis tu filtres et il reste 1 page)
            if (from >= list.size() && paginator.getTotalPages() > 0) {
                //On remet la page à la dernière page valide.
                paginator.setPage(paginator.getTotalPages());
                //On recalcule offset avec la nouvelle page.(Offset = position de départ dans la liste)
                from = paginator.getOffset();
            }
            //Math.min empêche de dépasser la taille de la liste.
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
            //Pour chaque compte-rendu dans la page…
            //On crée une “card pour ajouter les contenus
            for (CompteRenduView item : pageList) {
                compteRenduContainer.getChildren().add(buildCard(item));
            }

        } catch (SQLException e) {
            showError("Erreur chargement", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Card
    // ══════════════════════════════════════════════════════════════════════
    private VBox buildCard(CompteRenduView cr) {
        VBox card = new VBox(12);
        card.setMaxWidth(Double.MAX_VALUE);

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

        // Title
        Label title = new Label("Compte-rendu de séance");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill:#111;");

        // Patient
        HBox patientRow = new HBox(8);
        patientRow.setAlignment(Pos.CENTER_LEFT);
        Region userIcon = svgIcon(
                "M12 12c2.761 0 5-2.239 5-5S14.761 2 12 2 7 4.239 7 7s2.239 5 5 5zm0 2c-4.418 0-8 2.239-8 5v1h16v-1c0-2.761-3.582-5-8-5z",
                "#0F766E", 14, 14
        );
        String patientName = (cr.getPatientFullName() == null || cr.getPatientFullName().isBlank()) ? "Patient" : cr.getPatientFullName();
        Label patientLabel = new Label(patientName);
        patientLabel.setStyle("-fx-font-size: 13px; -fx-text-fill:#222;");
        patientRow.getChildren().addAll(userIcon, patientLabel);

        // Badge progrès
        Label badge = buildProgressBadge(cr.getProgresCr());

        // Rating
        HBox ratingRow = buildRatingRow(cr);

        // Sections
        VBox resumeBox = buildSection("RÉSUMÉ DE SÉANCE", cr.getResumeSeanceCr(), "#111");
        VBox actionsBox = buildSection("PROCHAINES ACTIONS", cr.getProchainesActionCr(), "#111");

        // ✅ Section IA uniquement si présente ET non annulée
        VBox aiBox = null;
        boolean hasAi = cr.getAiResumeCr() != null && !cr.getAiResumeCr().isBlank() && !cr.isAiResumeCanceled();
        if (hasAi) {
            aiBox = buildSection("SYNTHÈSE IA", cr.getAiResumeCr(), "#2563EB");
        }

        // Buttons (edit/delete/pdf)
        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = iconButton(
                "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
                "#2563EB"
        );
        editBtn.setTooltip(new Tooltip("Modifier"));
        editBtn.setOnAction(e -> showCompteRenduDialog("Modifier Compte-rendu", cr, null));

        Button delBtn = iconButton(
                "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zm3.46-7.12 1.41-1.41L12 11.59l1.12-1.12 1.41 1.41L13.41 13l1.12 1.12-1.41 1.41L12 14.41l-1.12 1.12-1.41-1.41L10.59 13 9.46 11.88zM15.5 4l-1-1h-5l-1 1H5v2h14V4z",
                "#DC2626"
        );
        delBtn.setTooltip(new Tooltip("Supprimer"));
        delBtn.setOnAction(e -> handleDelete(cr));

        Button pdfBtn = iconButton(
                "M6 2h7l5 5v15a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm7 1.5V8h4.5",
                "#0F766E"
        );
        pdfBtn.setTooltip(new Tooltip("Exporter PDF"));
        pdfBtn.setOnAction(e -> handleExportPdf(cr));

        btns.getChildren().addAll(editBtn, delBtn, pdfBtn);

        if (aiBox != null) {
            card.getChildren().addAll(dateRow, title, patientRow, badge, ratingRow, resumeBox, actionsBox, aiBox, btns);
        } else {
            card.getChildren().addAll(dateRow, title, patientRow, badge, ratingRow, resumeBox, actionsBox, btns);
        }

        return card;
    }


    //Fonction Rating
    private HBox buildRatingRow(CompteRenduView cr) {
        //Crée un conteneur horizontal HBox.
        //Le 10 = espacement
        HBox row = new HBox(10);
        //Aligne les éléments au centre verticalement et collés à gauche horizontalement.
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("Note patient :");
        label.setStyle("-fx-font-size: 12px; -fx-text-fill:#334155; -fx-font-weight: 700;");

        //Récupère la note depuis l’objet cr
        Integer rating = cr.getRating();
        //Crée un label qui contient les étoiles.
        Label stars = new Label(renderStars(rating));
        stars.setStyle("-fx-font-size: 16px; -fx-text-fill:#F59E0B; -fx-font-weight: 800;");

        //Crée un texte à droite des étoiles :
        //Si rating == null → affiche “Non noté”
        //Sinon → affiche par exemple “4/5”
        Label txt = new Label(rating == null ? "Non noté" : (rating + "/5"));
        txt.setStyle("-fx-font-size: 12px; -fx-text-fill:#475569; -fx-font-weight: 700;");

        //Ajoute les 3 éléments dans la ligne, dans l’ordre :
        //“Note patient :”
        //étoiles ★★★☆☆
        //texte “3/5” ou “Non noté”
        row.getChildren().addAll(label, stars, txt);
        return row;
    }

    private String renderStars(Integer rating) {
        int r = (rating == null) ? 0 : Math.max(0, Math.min(5, rating));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= r ? '★' : '☆');
        return sb.toString();
    }

    private void handleDelete(CompteRenduView cr) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        stylePopup(alert.getDialogPane());
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer ce compte-rendu ?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    crService.deleteById(cr.getIdCompteRendu());
                    loadCompteRendus();
                } catch (SQLException e) {
                    showError("Erreur suppression", e);
                }
            }
        });
    }

    // Ouvrir une fenêtre “Enregistrer sous” (FileChooser)
    //Générer un nom de fichier propre
    //Appeler ton service PDF
    private void handleExportPdf(CompteRenduView cr) {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Exporter le compte-rendu en PDF");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));

            String patientName = (cr.getPatientFullName() == null) ? "patient" : cr.getPatientFullName();
            patientName = patientName.replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replace(" ", "_");

            String fileName = "CompteRendu_" + patientName + "_" +
                    (cr.getRvDate() == null ? "" : cr.getRvDate().toString()) + ".pdf";
            fc.setInitialFileName(fileName);

            File file = fc.showSaveDialog(compteRenduContainer.getScene().getWindow());
            if (file == null) return;

            Path out = file.toPath();

            // Psy: export normal (résumé/actions toujours). IA seulement si hasAi (Pdf service gère ça)
            PdfCompteRenduService.exportCompteRendu(cr, out, "Psychologue");

            info("Export PDF", "PDF exporté avec succès ✅\n" + file.getAbsolutePath());

        } catch (Exception ex) {
            ex.printStackTrace();
            error("Export PDF", "Erreur lors de l'export PDF : " + ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // POPUP Add/Edit (Dialog)
    // ══════════════════════════════════════════════════════════════════════
    private void showCompteRenduDialog(String title, CompteRenduView toEdit, RendezVousView selectedRvForAdd) {

        boolean isEdit = (toEdit != null);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        stylePopup(dialog.getDialogPane());

        ButtonType saveBtnType = new ButtonType(isEdit ? "Mettre à jour" : "Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        Label apptLabel = new Label("Rendez-vous :");
        apptLabel.setStyle("-fx-font-weight: 700;");

        String apptText;
        int appointmentId;

        if (isEdit) {
            appointmentId = toEdit.getIdAppointment();
            String d = toEdit.getRvDate() == null ? "" : toEdit.getRvDate().toString();
            String t = toEdit.getRvTime() == null ? "" : toEdit.getRvTime().toString();
            apptText = d + " " + t + "  •  " + (toEdit.getPatientFullName() == null ? "" : toEdit.getPatientFullName());
        } else {
            appointmentId = selectedRvForAdd.getIdRv();
            apptText = selectedRvForAdd.formatForChoice();
        }

        Label apptValue = new Label(apptText);
        apptValue.setStyle("-fx-text-fill:#111;");

        ComboBox<CompteRenduSeance.ProgresCR> progres = new ComboBox<>();
        progres.setItems(FXCollections.observableArrayList(CompteRenduSeance.ProgresCR.values()));
        progres.setPrefWidth(260);

        TextArea resume = new TextArea();
        resume.setPrefRowCount(4);
        resume.setWrapText(true);

        TextArea actions = new TextArea();
        actions.setPrefRowCount(4);
        actions.setWrapText(true);

        Label error = new Label("");
        error.setStyle("-fx-text-fill:#DC2626; -fx-font-weight: 700;");

        // ✅ AI textarea (LOCAL, not @FXML)
        TextArea aiSummaryArea = new TextArea();
        aiSummaryArea.setPrefRowCount(5);
        aiSummaryArea.setWrapText(true);
        aiSummaryArea.setEditable(true);
        aiSummaryArea.setStyle(
                "-fx-background-color: #F0FDF4;" +
                        "-fx-border-color: #86EFAC;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 12px;"
        );
        aiSummaryArea.setPromptText("Le résumé généré par l'IA apparaîtra ici...");

        CheckBox cancelAiBox = new CheckBox("Annuler l'affichage IA (ne s'affichera plus)");
        cancelAiBox.setStyle("-fx-text-fill:#B45309; -fx-font-weight: 700;");

        if (isEdit) {
            progres.getSelectionModel().select(toEdit.getProgresCr());
            resume.setText(toEdit.getResumeSeanceCr());
            actions.setText(toEdit.getProchainesActionCr());
            aiSummaryArea.setText(toEdit.getAiResumeCr() == null ? "" : toEdit.getAiResumeCr());
            cancelAiBox.setSelected(toEdit.isAiResumeCanceled());
        } else {
            progres.getSelectionModel().selectFirst();
        }

        grid.add(apptLabel, 0, 0);
        grid.add(apptValue, 1, 0);
        grid.add(new Label("Progrès :"), 0, 1);
        grid.add(progres, 1, 1);
        grid.add(new Label("Résumé :"), 0, 2);
        grid.add(resume, 1, 2);
        grid.add(new Label("Actions :"), 0, 3);
        grid.add(actions, 1, 3);

        Label aiSectionTitle = new Label("RÉSUMÉ AUTOMATIQUE (IA)");
        aiSectionTitle.setStyle(
                "-fx-font-size: 10px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-text-fill: #2563EB;" +
                        "-fx-padding: 8 0 2 0;"
        );

        Button generateAiBtn = new Button("✨  Générer résumé IA");
        generateAiBtn.setStyle(
                "-fx-background-color: #2563EB;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 6 14;"
        );

        generateAiBtn.setOnAction(e -> {
            String resumeText  = resume.getText() == null ? "" : resume.getText().trim();
            String actionsText = actions.getText() == null ? "" : actions.getText().trim();
            String progresText = progres.getValue() != null ? progres.getValue().name() : "non défini";

            if (resumeText.isBlank()) {
                aiSummaryArea.setText("⚠️  Veuillez d'abord saisir le résumé de séance.");
                return;
            }

            generateAiBtn.setDisable(true);
            generateAiBtn.setText("⏳  Génération en cours...");
            aiSummaryArea.setText("Génération en cours...");

            LlmSummaryService llmService = new LlmSummaryService();
            int idPsy = Session.getUserId();

            Task<String> task = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return llmService.generateSummaryWithRag(resumeText, actionsText, progresText, idPsy);
                }
            };

            task.setOnSucceeded(ev -> {
                aiSummaryArea.setText(task.getValue());
                generateAiBtn.setDisable(false);
                generateAiBtn.setText("✨  Générer résumé IA");
            });

            task.setOnFailed(ev -> {
                Throwable ex = task.getException();
                aiSummaryArea.setText("❌  Erreur : " + (ex != null ? ex.getMessage() : "inconnue"));
                generateAiBtn.setDisable(false);
                generateAiBtn.setText("✨  Générer résumé IA");
            });

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        });

        grid.add(aiSectionTitle, 1, 4);
        grid.add(generateAiBtn, 1, 5);
        grid.add(new Label("Synthèse :"), 0, 6);
        grid.add(aiSummaryArea, 1, 6);
        grid.add(cancelAiBox, 1, 7);
        grid.add(error, 1, 8);

        dialog.getDialogPane().setContent(grid);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String err;
            err = ValidationUtils.requiredText(resume.getText(), "Résumé");
            if (err == null) err = ValidationUtils.minLength(resume.getText(), "Résumé", 10);
            if (err == null) err = ValidationUtils.maxLength(resume.getText(), "Résumé", 1000);
            if (err == null) err = ValidationUtils.requiredText(actions.getText(), "Prochaines actions");
            if (err == null) err = ValidationUtils.minLength(actions.getText(), "Prochaines actions", 10);
            if (err == null) err = ValidationUtils.maxLength(actions.getText(), "Prochaines actions", 1000);

            if (err != null) {
                error.setText(err);
                ev.consume();
            }
        });

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != saveBtnType) return;

        try {
            String aiText = aiSummaryArea.getText() == null ? null : aiSummaryArea.getText().trim();
            if (aiText != null && aiText.isBlank()) aiText = null;

            Timestamp now = new Timestamp(System.currentTimeMillis());

            if (isEdit) {
                CompteRenduSeance upd = new CompteRenduSeance(
                        toEdit.getIdCompteRendu(),
                        appointmentId,
                        now,
                        progres.getValue(),
                        resume.getText().trim(),
                        actions.getText().trim()
                );
                upd.setAiResumeCr(aiText);
                upd.setAiResumeCanceled(cancelAiBox.isSelected());
                crService.update(upd);
            } else {
                CompteRenduSeance add = new CompteRenduSeance(
                        appointmentId,
                        now,
                        progres.getValue(),
                        resume.getText().trim(),
                        actions.getText().trim()
                );
                add.setAiResumeCr(aiText);
                add.setAiResumeCanceled(cancelAiBox.isSelected());
                crService.addAndReturnId(add);
            }

            loadCompteRendus();

        } catch (SQLException e) {
            showError("Erreur sauvegarde", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // UI helpers
    // ══════════════════════════════════════════════════════════════════════

    private Label buildProgressBadge(CompteRenduSeance.ProgresCR progrescr) {
        String text, style;
        if (progrescr == null) {
            text  = "Progression";
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

    private VBox buildSection(String sectionTitle, String content, String titleColor) {
        VBox box = new VBox(4);

        Label title = new Label(sectionTitle);
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + titleColor + ";");

        // ✅ Better for line breaks than Label only:
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
        error(title, e.getMessage());
    }

    private void error(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        stylePopup(a.getDialogPane());
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        stylePopup(a.getDialogPane());
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    // Popup theme helper
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