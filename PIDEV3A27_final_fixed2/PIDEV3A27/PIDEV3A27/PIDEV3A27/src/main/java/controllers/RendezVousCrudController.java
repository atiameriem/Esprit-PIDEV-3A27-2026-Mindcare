package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.SVGPath;
import models.RendezVous;
import models.RendezVousView;
import services.ServiceRendezVous;
import services.ServiceCompteRenduSeance;
import utils.MyDatabase;
import utils.Session;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.ComboBox;
import java.util.Comparator;


/**
 * Patient : CRUD de ses rendez-vous avec popups + contrÃ´le de saisie.
 */
public class RendezVousCrudController {

    @FXML private TextField searchField;
    @FXML private TilePane rendezVousContainer;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ScrollPane listScroll;
    @FXML private Button patientCompteRenduButton;


    //objet qui fait les opÃ©rations SQL (add/update/delete/find)
    private ServiceRendezVous service;
    private final Connection cnx = MyDatabase.getInstance().getConnection();

    //Tu imposes un format pour :
    //Date : 2026-02-17
    //Heure : 14:30
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // â RÃ¨gles mÃ©tier date :
    // - pas dans le passÃ©
    // - pas trop loin (ex: max +6 mois)
    // (comme tu as demandÃ© : 2025-02-02 impossible (passÃ©) + 2030-02-02 impossible (trop loin))
    private static final int MAX_MONTHS_AHEAD = 6;

    //CrÃ©e le service
    //Charge la liste au dÃ©but
    //Ajoute un listener : Ã  chaque lettre tapÃ©e dans search â recharge la liste filtrÃ©e
    @FXML
    public void initialize() {
        service = new ServiceRendezVous(cnx);
        loadRendezVous();

        // â DemandÃ© : la barre de recherche ne doit pas contenir du texte au chargement.
        if (searchField != null) {
            searchField.setText("");
        }

        // â DemandÃ© : 2 cartes par ligne SANS scroll horizontal.
        // On force la largeur des "tiles" Ã  50% (moins les marges).
        if (listScroll != null && rendezVousContainer != null) {
            rendezVousContainer.prefTileWidthProperty()
                    .bind(listScroll.widthProperty().subtract(60).divide(2));
            rendezVousContainer.setPrefColumns(2);
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> loadRendezVous());
        }
        if (sortCombo != null) {
            // Affichage correct (UTF-8)
            sortCombo.getItems().addAll(
                    "Date ↑ (croissant)",
                    "Date ↓ (décroissant)"
            );

            sortCombo.getSelectionModel().select("Date ↓ (décroissant)");

            sortCombo.valueProperty().addListener((obs, o, n) -> loadRendezVous());
        }
    }
    //Ajouter un RDV
    @FXML
    private void handleNewRendezVous() {
        //showRendezVousDialog(null)
        //â popup en mode ajout (car existing = null)
        Optional<RendezVous> created = showRendezVousDialog(null);
        created.ifPresent(rv -> {
            try {
                //insertion DB
                //retourne lâID gÃ©nÃ©rÃ©
                int id = service.addAndReturnId(rv);
                System.out.println("Inserted rendez_vous id=" + id);
                //refresh affichage
                loadRendezVous();
            } catch (SQLException e) {
                showError("Erreur Ajout", e);
            }
        });
    }
    //Modifier un RDV
    private void handleEdit(RendezVous existing) {
        Optional<RendezVous> updated = showRendezVousDialog(existing);
        //Si le patient valide tu fais update
        updated.ifPresent(rv -> {
            try {
                //(jai choisis nom du psy)
                rv.setIdRv(existing.getIdRv());
                //le service doit vÃ©rifier id_patient = Session.getUserId()
                service.updateForPatient(rv, Session.getUserId());
                loadRendezVous();
            } catch (SQLException e) {
                showError("Erreur Modification", e);
            }
        });
    }

    private void handleDelete(RendezVousView rv) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer ce rendez-vous ?");
        alert.setContentText("Cette action est irréversible.");

        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    //â Ici aussi, âForPatientâ = sÃ©curitÃ© (ne supprimer que ses RDV).
                    service.deleteForPatient(rv.getIdRv(), Session.getUserId());
                    loadRendezVous();
                } catch (SQLException e) {
                    showError("Erreur Suppression", e);
                }
            }
        });
    }

    // â Overload : certains appels passent par l'entitÃ© (RendezVous) et non la vue (RendezVousView)
    //    On garde les deux signatures pour Ã©viter de casser le reste du code.
    private void handleDelete(RendezVous rv) {
        if (rv == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer ce rendez-vous ?");
        alert.setContentText("Cette action est irréversible.");

        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    //â SÃ©curitÃ© : ne supprimer que SES RDV (patient connectÃ©)
                    service.deleteForPatient(rv.getIdRv(), Session.getUserId());
                    loadRendezVous();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Erreur", "Impossible de supprimer le rendez-vous.");
                }
            }
        });
    }

    private void loadRendezVous() {
        try {
            List<RendezVousView> list = service.findViewsByPatient(Session.getUserId());

            // â Patient : afficher le bouton "Mes comptes-rendus" uniquement si le patient a au moins 1 CR.
            if (patientCompteRenduButton != null) {
                try {
                    ServiceCompteRenduSeance crService = new ServiceCompteRenduSeance(cnx);
                    boolean hasAny = !crService.findViewsByPatient(Session.getUserId()).isEmpty();
                    patientCompteRenduButton.setVisible(hasAny);
                    patientCompteRenduButton.setManaged(hasAny);
                } catch (SQLException ignore) {
                    // Si erreur CR, on cache le bouton sans casser l'Ã©cran
                    patientCompteRenduButton.setVisible(false);
                    patientCompteRenduButton.setManaged(false);
                }
            }

            String kw = (searchField == null) ? "" : searchField.getText().toLowerCase().trim();
            if (!kw.isEmpty()) {
                list = list.stream().filter(rv ->
                        String.valueOf(rv.getIdRv()).contains(kw)
                                || String.valueOf(rv.getIdPsychologist()).contains(kw)
                                || (rv.getStatutRv() != null && rv.getStatutRv().name().toLowerCase().contains(kw))
                                || (rv.getConfirmationStatus() != null && rv.getConfirmationStatus().name().toLowerCase().contains(kw))
                                || (rv.getTypeRendezVous() != null && rv.getTypeRendezVous().name().toLowerCase().contains(kw))
                                || (rv.getAppointmentDate() != null && rv.getAppointmentDate().toString().toLowerCase().contains(kw))
                                || (rv.getAppointmentTimeRv() != null && rv.getAppointmentTimeRv().toString().toLowerCase().contains(kw))
                ).collect(java.util.stream.Collectors.toList());
            }
            // â TRI (date croissant / dÃ©croissant)
            String sort = (sortCombo == null || sortCombo.getValue() == null) ? "" : sortCombo.getValue();

            //On dÃ©clare un comparateur byDateTime
            Comparator<RendezVousView> byDateTime = Comparator
                    //on trie dâabord selon la date
                    // getAppo mÃ©thode utilisÃ©e pour prendre la date de chaque rendez-vous.
                    //tri naturel croissant
                    //nullsLast(...) = si la date est null,
                    // on met cet Ã©lÃ©ment Ã  la fin (pour Ã©viter erreur).
                    //si deux rendez-vous ont la mÃªme date, alors compare aussi lâheure
                    .comparing(RendezVousView::getAppointmentDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(RendezVousView::getAppointmentTimeRv, Comparator.nullsLast(Comparator.naturalOrder()));

            if ("Date ↑ (croissant)".equals(sort)) {
                list.sort(byDateTime);
            } else if ("Date ↓ (décroissant)".equals(sort)) {
                list.sort(byDateTime.reversed());
            }
// si rien choisi â on laisse lâordre SQL par dÃ©faut

            rendezVousContainer.getChildren().clear();

            if (list.isEmpty()) {
                Label empty = new Label("Aucun rendez-vous trouvé.");
                empty.setStyle("-fx-text-fill:#666; -fx-font-size: 14px; -fx-padding: 10 0 0 0;");
                rendezVousContainer.getChildren().add(empty);
                return;
            }

            // â TilePane : on ajoute directement les cards (pas de HBox intermÃ©diaire)
            for (RendezVousView rv : list) {
                rendezVousContainer.getChildren().add(buildCard(rv));
            }

        } catch (SQLException e) {
            showError("Erreur chargement", e);
        }
    }

    // â Patient : ouvrir la page de lecture des comptes-rendus
    @FXML
    private void openCompteRenduRead() {
        try {
            VBox contentArea = (VBox) rendezVousContainer.getScene().lookup("#contentArea");
            if (contentArea == null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CompteRenduRead.fxml"));
            Node page = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (Exception e) {
            showError("Erreur ouverture compte-rendu", e);
        }
    }

    // ===== POPUP =====

    private Optional<RendezVous> showRendezVousDialog(RendezVous existing) {
        boolean isEdit = existing != null;

        Dialog<RendezVous> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier Rendez-vous" : "Nouveau Rendez-vous");
        dialog.setHeaderText(isEdit ? "Modifier les informations" : "Saisir les informations");

        ButtonType saveType = new ButtonType(isEdit ? "Enregistrer" : "Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<PsyItem> psyCombo = new ComboBox<>();
        psyCombo.setPromptText("Choisir psychologue");
        psyCombo.setPrefWidth(260);
        psyCombo.setMaxWidth(Double.MAX_VALUE);
        psyCombo.setItems(FXCollections.observableArrayList(loadPsychologists()));

        // â DatePicker = calendrier (plus pratique que TextField)
        DatePicker datePicker = new DatePicker();

        // â Time picker "innovant" : 2 spinners (heure + minute)
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 9);
        hourSpinner.setEditable(true);
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 55, 0, 5); // pas de 5 minutes
        minuteSpinner.setEditable(true);
        HBox timeBox = new HBox(8, hourSpinner, new Label(":"), minuteSpinner);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<RendezVous.TypeRV> typeCombo =
                new ComboBox<>(FXCollections.observableArrayList(RendezVous.TypeRV.values()));

        Label hint = new Label("");
        hint.setStyle("-fx-text-fill:#DC2626; -fx-font-size: 11px;");

        if (isEdit) {
            selectPsychologist(psyCombo, existing.getIdPsychologist());
            if (existing.getAppointmentDate() != null) datePicker.setValue(existing.getAppointmentDate().toLocalDate());
            if (existing.getAppointmentTimeRv() != null) {
                LocalTime lt = existing.getAppointmentTimeRv().toLocalTime();
                hourSpinner.getValueFactory().setValue(lt.getHour());
                minuteSpinner.getValueFactory().setValue((lt.getMinute() / 5) * 5);
            }
            typeCombo.setValue(existing.getTypeRendezVous());
        } else {
            datePicker.setValue(LocalDate.now());
            LocalTime now = LocalTime.now().withSecond(0).withNano(0);
            hourSpinner.getValueFactory().setValue(now.getHour());
            minuteSpinner.getValueFactory().setValue((now.getMinute() / 5) * 5);
            typeCombo.setValue(RendezVous.TypeRV.premiere_consultation);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        grid.add(new Label("Psychologue"), 0, 0);
        grid.add(psyCombo, 1, 0);

        grid.add(new Label("Date"), 0, 1);
        grid.add(datePicker, 1, 1);

        grid.add(new Label("Heure"), 0, 2);
        grid.add(timeBox, 1, 2);

        // â Le statut n'est plus choisi par le patient.
        // Il sera gÃ©rÃ© par le psychologue (termine / en_cours) aprÃ¨s confirmation.

        grid.add(new Label("Type"), 0, 3);
        grid.add(typeCombo, 1, 3);

        grid.add(hint, 1, 4);

        GridPane.setHgrow(psyCombo, Priority.ALWAYS);
        GridPane.setHgrow(datePicker, Priority.ALWAYS);
        GridPane.setHgrow(typeCombo, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(saveType);
        saveBtn.setDisable(true);

        Runnable validate = () -> {
            hint.setText("");
            boolean ok = true;

            int idPsy;
            try {
                PsyItem selPsy = psyCombo.getValue();
                idPsy = (selPsy == null) ? -1 : selPsy.getId();
            }
            catch (Exception e) {
                saveBtn.setDisable(true);
                return;
            }

            // â Date : obligatoire + rÃ¨gle (pas passÃ© + pas trop loin)
            LocalDate d = datePicker.getValue();
            if (d == null) {
                saveBtn.setDisable(true);
                return;
            }
            LocalDate today = LocalDate.now();
            LocalDate maxDate = today.plusMonths(MAX_MONTHS_AHEAD);
            if (d.isBefore(today)) {
                hint.setText("Date invalide : vous ne pouvez pas choisir une date passée.");
                saveBtn.setDisable(true);
                return;
            }
            if (d.isAfter(maxDate)) {
                hint.setText("Date invalide : max autorisé = " + maxDate.format(DATE_FMT) + ".");
                saveBtn.setDisable(true);
                return;
            }

            // â Time : spinners (heure/minute)
            Integer hh = hourSpinner.getValue();
            Integer mm = minuteSpinner.getValue();
            if (hh == null || mm == null) {
                saveBtn.setDisable(true);
                return;
            }
            ok = ok && typeCombo.getValue() != null;

            if (!ok) {
                saveBtn.setDisable(true);
                return;
            }

            // vÃ©rifier psy existe
            try {
                if (!service.isPsychologistUser(idPsy)) {
                    hint.setText("ID psychologue invalide (doit exister dans users avec role=psychologue). ");
                    saveBtn.setDisable(true);
                    return;
                }
            } catch (SQLException ex) {
                hint.setText("Erreur validation : " + ex.getMessage());
                saveBtn.setDisable(true);
                return;
            }

            // ok
            saveBtn.setDisable(false);
        };

        psyCombo.valueProperty().addListener((a,b,c)-> validate.run());
        datePicker.valueProperty().addListener((a,b,c)-> validate.run());
        hourSpinner.valueProperty().addListener((a,b,c)-> validate.run());
        minuteSpinner.valueProperty().addListener((a,b,c)-> validate.run());
        typeCombo.valueProperty().addListener((a,b,c)-> validate.run());
        validate.run();

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                PsyItem selPsy = psyCombo.getValue();
                int psyId = (selPsy == null) ? -1 : selPsy.getId();
                LocalDate d = datePicker.getValue();
                LocalTime t = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());

                return new RendezVous(
                        Session.getUserId(),
                        psyId,
                        null, // â statutrv = choisi plus tard par le psychologue
                        RendezVous.ConfirmationStatus.en_attente, // â ajout/Ã©dition cÃ´tÃ© patient = en_attente
                        Date.valueOf(d),
                        typeCombo.getValue(),
                        Time.valueOf(t)
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    // ===== CARD UI =====

    private VBox buildCard(RendezVousView rv) {
        VBox card = new VBox(12);
        HBox.setHgrow(card, Priority.ALWAYS);

        // â UI : si rendez-vous confirmÃ©/annulÃ© => le patient ne peut plus modifier/supprimer.
        boolean locked = rv.getConfirmationStatus() != null
                && rv.getConfirmationStatus() != RendezVous.ConfirmationStatus.en_attente;

        card.setStyle(locked ? """
                -fx-background-color: #F1F5F9;
                -fx-padding: 20;
                -fx-background-radius: 10;
                -fx-border-radius: 10;
                -fx-border-color: #CBD5E1;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);
                """ : """
                -fx-background-color: #FFFFFF;
                -fx-padding: 20;
                -fx-background-radius: 10;
                -fx-border-radius: 10;
                -fx-border-color: #E7E7E7;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);
                """);

        // â Deux cartes par ligne : la largeur est gÃ©rÃ©e par le TilePane (binding sur prefTileWidth)
        card.setMaxWidth(Double.MAX_VALUE);


        HBox dateRow = new HBox(8);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        Region calIcon = svgIcon(
                "M7 2v2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2V2h-2v2H9V2H7zm12 6H5v10h14V8z",
                "#1D4ED8", 14, 14
        );

        String dateTxt = rv.getAppointmentDate() == null ? "" : rv.getAppointmentDate().toString();
        String timeTxt = rv.getAppointmentTimeRv() == null ? "" : rv.getAppointmentTimeRv().toString();
        Label dateLabel = new Label(dateTxt + "  ·  " + timeTxt);
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-font-weight: 700;");

        dateRow.getChildren().addAll(calIcon, dateLabel);

        Label title = new Label("Rendez-vous");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill:#111;");

        String psyName = rv.getPsychologistFullName() == null || rv.getPsychologistFullName().isBlank()
                ? "Psychologue"
                : rv.getPsychologistFullName();
        Label psyLabel = new Label(psyName);
        psyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill:#222;");

        HBox psyRow = new HBox(8);
        psyRow.setAlignment(Pos.CENTER_LEFT);

        Region psyIcon = svgIcon(
                "M12 12c2.761 0 5-2.239 5-5S14.761 2 12 2 7 4.239 7 7s2.239 5 5 5zm0 2c-4.418 0-8 2.239-8 5v1h16v-1c0-2.761-3.582-5-8-5z",
                "#2563EB", 14, 14
        );

        psyRow.getChildren().addAll(psyIcon, psyLabel);


        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);
        Label typeBadge = buildTypeBadge(rv.getTypeRendezVous());
        Label statutBadge = buildStatutBadge(rv.getStatutRv());
        if (typeBadge != null) badges.getChildren().add(typeBadge);
        if (statutBadge != null) badges.getChildren().add(statutBadge);


        // Informations (patient connecté) — même affichage que côté psychologue
        String fullName = (Session.getFullName() == null ? "" : Session.getFullName());
        String info = "Patient : " + fullName;
        VBox details = buildSection("INFORMATIONS", info, "ℹ️");

        // â Actions (cÃ´tÃ© patient)
// - Tant que c'est "En attente" : le patient peut modifier / annuler (supprimer)
// - DÃ¨s que c'est confirmÃ© ou annulÃ© : on bloque les actions
        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER_LEFT);

// â Boutons icÃ´nes (modifier / annuler) demandÃ©s
        Button editBtn = iconButton(
                svgIcon("M4 21h4l11-11-4-4L4 17v4z M14 6l4 4", "#FFFFFF", 16, 16),
                "Modifier", "#2563EB"
        );
        if (!locked) {
            editBtn.setOnAction(e -> handleEdit(toEntity(rv)));
        } else {
            // â DemandÃ© : quand c'est confirmÃ©/annulÃ© => aucune action possible (pas cliquable)
            editBtn.setDisable(true);
            editBtn.setMouseTransparent(true);
            editBtn.setOpacity(0.45);
        }

        Button delBtn = iconButton(
                svgIcon("M6 7h12l-1 14H7L6 7zm3-3h6l1 2H8l1-2z", "#FFFFFF", 16, 16),
                "Annuler", "#DC2626"
        );
        if (!locked) {
            delBtn.setOnAction(e -> handleDelete(toEntity(rv)));
        } else {
            delBtn.setDisable(true);
            delBtn.setMouseTransparent(true);
            delBtn.setOpacity(0.45);
        }

        btns.getChildren().addAll(editBtn, delBtn);

        // â Hint visuel quand le rendez-vous est verrouillÃ© (confirmÃ©/annulÃ©)
        Label lockedHint = new Label("🔒 Rendez-vous verrouillé (déjà traité par le psychologue)");
        lockedHint.setStyle("-fx-text-fill:#64748B; -fx-font-size:12px; -fx-padding: 4 0 0 0;");
        lockedHint.setVisible(locked);
        lockedHint.setManaged(locked);


        card.getChildren().addAll(dateRow, buildTopStatusRow(rv.getConfirmationStatus(), locked), title, psyRow, badges, details, btns, lockedHint);
        return card;
    }

    private Label buildStatutBadge(RendezVous.StatutRV statut) {
        String text;
        String style;
        if (statut == null) {
            // â DemandÃ© : ne pas afficher "Statut inconnu" (on masque le badge si null)
            return null;
        } else {
            switch (statut) {
                case termine -> { text = "Terminé"; style = "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A;"; }
                case en_cours -> { text = "En cours"; style = "-fx-background-color:#DBEAFE; -fx-text-fill:#1D4ED8;"; }
                default -> { text = statut.name(); style = "-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;"; }
            }
        }
        Label badge = new Label("📌  " + text);
        badge.setStyle(style + "-fx-padding: 6 12; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        return badge;
    }

    // â Badge confirmation_status (visible patient + psy)
    // ===================== NOUVEL AFFICHAGE : statut en haut =====================
// ✅ Affiche "EN ATTENTE / CONFIRMÉ / ANNULÉ" tout en haut de la carte
//    (plus visible que les petits badges).
    private HBox buildTopStatusRow(RendezVous.ConfirmationStatus status, boolean locked) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        String txt;
        String style;

        if (status == null || status == RendezVous.ConfirmationStatus.en_attente) {
            txt = "⏳  EN ATTENTE";
            style = "-fx-background-color:#FEF3C7; -fx-text-fill:#B45309;";
        } else if (status == RendezVous.ConfirmationStatus.confirme) {
            txt = "✅  CONFIRMÉ";
            style = "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A;";
        } else {
            txt = "❌  ANNULÉ";
            style = "-fx-background-color:#FEE2E2; -fx-text-fill:#DC2626;";
        }

        Label pill = new Label(txt);
        pill.setStyle(style + "-fx-padding: 6 12; -fx-background-radius: 999; -fx-font-size: 12px; -fx-font-weight: 800;");

        // ✅ Si verrouillé : on diminue un peu l'opacité pour indiquer "read-only"
        if (locked) pill.setOpacity(0.85);

        row.getChildren().add(pill);
        return row;
    }

    private Label buildConfirmationBadge(RendezVous.ConfirmationStatus status) {
        String text;
        String style;
        if (status == null) {
            text = "En attente";
            style = "-fx-background-color:#FEF3C7; -fx-text-fill:#B45309;";
        } else {
            switch (status) {
                case en_attente -> { text = "En attente"; style = "-fx-background-color:#FEF3C7; -fx-text-fill:#B45309;"; }
                case confirme -> { text = "Confirmé"; style = "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A;"; }
                case annule -> { text = "Annulé"; style = "-fx-background-color:#FEE2E2; -fx-text-fill:#DC2626;"; }
                default -> { text = status.name(); style = "-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;"; }
            }
        }
        Label badge = new Label("⏳  " + text);
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
                case premiere_consultation -> { text = "Première consultation"; style = "-fx-background-color:#FEF9C3; -fx-text-fill:#B45309;"; }
                case suivi -> { text = "Suivi"; style = "-fx-background-color:#E0E7FF; -fx-text-fill:#3730A3;"; }
                case urgence -> { text = "Urgence"; style = "-fx-background-color:#FFEDD5; -fx-text-fill:#C2410C;"; }
                default -> { text = type.name(); style = "-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;"; }
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


    // â Petit bouton icÃ´ne (plus lisible que texte, demandÃ©)
    private Button iconButton(Region icon, String tooltipText, String bgColor) {
        Button b = new Button();
        b.setGraphic(icon);
        b.setMinSize(36, 36);
        b.setPrefSize(36, 36);
        b.setMaxSize(36, 36);
        b.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; -fx-cursor: hand;");
        if (tooltipText != null && !tooltipText.isBlank()) {
            javafx.scene.control.Tooltip.install(b, new javafx.scene.control.Tooltip(tooltipText));
        }
        return b;
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

    // â Overload simple : afficher une erreur avec un message (sans Exception)
    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(message);
        a.showAndWait();
    }


    // ===== Helpers psychologues (pour afficher le nom au lieu de l'ID) =====

    private static class PsyItem {
        private final int id;
        private final String fullName;

        PsyItem(int id, String fullName) {
            this.id = id;
            this.fullName = fullName;
        }

        int getId() { return id; }

        @Override
        public String toString() {
            return fullName;
        }
    }

    private List<PsyItem> loadPsychologists() {
        String sql = "SELECT id_users, prenom, nom FROM users WHERE role = 'psychologue' ORDER BY prenom, nom";
        List<PsyItem> out = new java.util.ArrayList<>();
        try (java.sql.PreparedStatement pst = cnx.prepareStatement(sql);
             java.sql.ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_users");
                String fullName = rs.getString("prenom") + " " + rs.getString("nom");
                out.add(new PsyItem(id, fullName));
            }
        } catch (SQLException e) {
            // si la liste ne charge pas, on laisse le combo vide mais on affiche l'erreur
            showError("Erreur chargement psychologues", e);
        }
        return out;
    }

    private void selectPsychologist(ComboBox<PsyItem> combo, int idPsy) {
        if (combo == null) return;
        for (PsyItem it : combo.getItems()) {
            if (it.getId() == idPsy) {
                combo.getSelectionModel().select(it);
                return;
            }
        }
    }

    private RendezVous toEntity(RendezVousView v) {
        RendezVous r = new RendezVous();
        r.setIdRv(v.getIdRv());
        r.setIdPatient(v.getIdPatient());
        r.setIdPsychologist(v.getIdPsychologist());
        r.setStatutRv(v.getStatutRv());
        r.setConfirmationStatus(v.getConfirmationStatus());
        r.setAppointmentDate(v.getAppointmentDate());
        r.setTypeRendezVous(v.getTypeRendezVous());
        r.setAppointmentTimeRv(v.getAppointmentTimeRv());
        return r;
    }

}
