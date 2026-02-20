package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import models.CompteRenduSeance;
import models.CompteRenduView;
import models.RendezVousView;
import services.ServiceCompteRenduSeance;
import services.ServiceRendezVous;
import utils.MyDatabase;
import utils.Session;
import utils.ValidationUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * ROLE PSYCHOLOGUE :
 * - Affiche uniquement les comptes-rendus des RDV du psy connecté
 * - Ajout/Edit/Supp via POPUP
 * - Ajout : choisir d'abord le rendez-vous (RDV du psy), ensuite créer le compte-rendu
 */
public class CompteRenduController {

    //@FXML signifie : “ce champ existe dans le fichier FXML et JavaFX va l’injecter ici”.
    //Les champs @FXML (liés au FXML)
    @FXML private TextField searchField; //searchField : champ où l’utilisateur tape un mot clé
    @FXML private VBox compteRenduContainer; //compteRenduContainer : conteneur principal où tu ajoutes les cards
    @FXML private Button newCompteRenduButton; //bouton “Nouveau Compte-rendu”

    private final Connection cnx = MyDatabase.getInstance().getConnection(); //une connexion SQL récupérée depuis MyDatabase (Singleton)
    //singleton pour assurer une seule instance pour la connexion a ala base de donnee
    private ServiceCompteRenduSeance crService; //service qui gère les comptes-rendus
    private ServiceRendezVous rvService; //service qui gère les rendez-vous

    //Une seule fois au début se faite  ,,lance le premier chargement ,,JavaFX l’appelle automatiquement
    @FXML
    public void initialize() {
        crService = new ServiceCompteRenduSeance(cnx); //tu crées le service CR avec la connexion DB.
        rvService = new ServiceRendezVous(cnx);

        loadCompteRendus(); //tu charges et affiches tout de suite la liste.

        //écoute la saisie clavier
        //à chaque changement du texte, tu rappelles loadCompteRendus()
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> loadCompteRendus());
        }
    }

    // ── Bouton "Nouveau Compte-rendu" ──────────────────────────────────────
    @FXML
    private void handleNewCompteRendu() {
        try {
            //tu récupères l’id du psy connecté depuis la Session pour affiche ses rendez vous li tebaainuu
            int idPsy = Session.getUserId();
            //tu récupères les rendez-vous du psy
            List<RendezVousView> rdvs = rvService.findViewsByPsychologist(idPsy);

            // ✅ Règle métier : compte-rendu seulement si RDV = confirmé + terminé
            rdvs = rdvs.stream().filter(rv ->
                    rv.getConfirmationStatus() == models.RendezVous.ConfirmationStatus.confirme
                            && rv.getStatutRv() == models.RendezVous.StatutRV.termine
            ).toList();

            //Si aucun rendez-vous terminé :
            if (rdvs.isEmpty()) {
                info("Aucune consultation terminée", "Le compte-rendu est disponible uniquement pour les rendez-vous confirmés et terminés.");
                return;
            }
            //choisir un rendez
            RendezVousView selected = chooseRendezVous(rdvs);
            //Si l’utilisateur annule → null → on sort.
            if (selected == null) return;

            // sécurité (le rdv appartient au psy)
            if (!crService.appointmentBelongsToPsychologist(selected.getIdRv(), idPsy)) {
                error("Accès refusé", "Ce rendez-vous ne vous appartient pas.");
                return;
            }
            //Ouvrir le popup en mode ajout :
            //toEdit = null → donc c’est un ajout
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

        Optional<RendezVousView> res = dialog.showAndWait();
        return res.orElse(null);
    }


    // ── Chargement / filtre ───────────────────────────────────────────────
    //fonction de recherche du compte rendu
    private void loadCompteRendus() {

        try {
            //Ici tu récupères les CR du psy
            List<CompteRenduView> list = crService.findViewsByPsychologist(Session.getUserId());
            //si pas de searchField → kw = ""(pas de recherche)
            String kw = (searchField == null) ? "" : searchField.getText().toLowerCase().trim();
            //Tu gardes un compte-rendu si au moins un champ contient le mot clé :
            //nom patient
            //résumé
            //prochaines actions
            if (!kw.isEmpty()) {
                list = list.stream().filter(cr ->
                        (cr.getPatientFullName() != null && cr.getPatientFullName().toLowerCase().contains(kw))
                                || (cr.getResumeSeanceCr() != null && cr.getResumeSeanceCr().toLowerCase().contains(kw))
                                || (cr.getProchainesActionCr() != null && cr.getProchainesActionCr().toLowerCase().contains(kw))
                                || (cr.getProgresCr() != null && cr.getProgresCr().name().toLowerCase().contains(kw))
                                || String.valueOf(cr.getIdAppointment()).contains(kw)
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

            //Affichage en grille de 3 colonnes
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

    // ══════════════════════════════════════════════════════════════════════
    // Construction d’une Card
    // ══════════════════════════════════════════════════════════════════════
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

        // Sections
        VBox resumeBox = buildSection("RÉSUMÉ DE SÉANCE", cr.getResumeSeanceCr(), "#111");
        VBox actionsBox = buildSection("PROCHAINES ACTIONS", cr.getProchainesActionCr(), "#111");

        // Buttons (edit/delete)
        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = iconButton("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z", "#2563EB");
        editBtn.setOnAction(e -> showCompteRenduDialog("Modifier Compte-rendu", cr, null));

        Button delBtn = iconButton("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zm3.46-7.12 1.41-1.41L12 11.59l1.12-1.12 1.41 1.41L13.41 13l1.12 1.12-1.41 1.41L12 14.41l-1.12 1.12-1.41-1.41L10.59 13 9.46 11.88zM15.5 4l-1-1h-5l-1 1H5v2h14V4z", "#DC2626");
        delBtn.setOnAction(e -> handleDelete(cr));

        btns.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(dateRow, title, patientRow, badge, resumeBox, actionsBox, btns);
        return card;
    }
    //popup confirmation et puis supp
    private void handleDelete(CompteRenduView cr) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
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

    // ══════════════════════════════════════════════════════════════════════
    // POPUP Add/Edit
    // ══════════════════════════════════════════════════════════════════════
    private void showCompteRenduDialog(String title, CompteRenduView toEdit, RendezVousView selectedRvForAdd) {

        boolean isEdit = (toEdit != null);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);

        ButtonType saveBtnType = new ButtonType(isEdit ? "Mettre à jour" : "Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        // Form
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

        if (isEdit) {
            progres.getSelectionModel().select(toEdit.getProgresCr());
            resume.setText(toEdit.getResumeSeanceCr());
            actions.setText(toEdit.getProchainesActionCr());
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
        grid.add(error, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Validation on save
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
            if (isEdit) {
                CompteRenduSeance upd = new CompteRenduSeance(
                        toEdit.getIdCompteRendu(),
                        appointmentId,
                        new Timestamp(System.currentTimeMillis()),
                        progres.getValue(),
                        resume.getText().trim(),
                        actions.getText().trim()
                );
                crService.update(upd);
            } else {
                CompteRenduSeance add = new CompteRenduSeance(
                        appointmentId,
                        new Timestamp(System.currentTimeMillis()),
                        progres.getValue(),
                        resume.getText().trim(),
                        actions.getText().trim()
                );
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
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
