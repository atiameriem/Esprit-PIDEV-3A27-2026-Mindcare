package controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import models.RendezVous;
import models.RendezVousView;
import services.ServiceCompteRenduSeance;
import services.ServiceRecaptcha;
import services.ServiceRendezVous;
import utils.MyDatabase;
import utils.Session;

import java.sql.*; // ✅ IMPORTANT (PreparedStatement / ResultSet / etc.)
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;import java.util.stream.Collectors;

/**
 * Patient : CRUD de ses rendez-vous + pagination.
 */
public class RendezVousCrudController {

    // ===================== PAGINATION UI =====================
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label pageLabel;
    @FXML private ComboBox<Integer> pageSizeCombo;

    private int currentPage = 1;
    private int pageSize = 4;

    // ===================== BUSINESS RULES =====================
    //Définit l’heure de début de la journée de rendez-vous.
    //Ici : 08:00.
    private static final LocalTime SLOT_START = LocalTime.of(8, 0);
    private static final LocalTime SLOT_END = LocalTime.of(17, 0);
    private static final int SLOT_STEP_MIN = 15;
    private static final int APPOINTMENT_DURATION_MIN = 30;

    private static boolean overlaps(LocalTime start, LocalTime busyStart) {
        //Calcule l’heure de fin du nouveau RDV.
        LocalTime end = start.plusMinutes(APPOINTMENT_DURATION_MIN);
        //Calcule l’heure de fin du rendez-vous existant
        LocalTime busyEnd = busyStart.plusMinutes(APPOINTMENT_DURATION_MIN);
        return start.isBefore(busyEnd) && end.isAfter(busyStart);
    }

    @FXML private TextField searchField;
    @FXML private TilePane rendezVousContainer;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ScrollPane listScroll;
    @FXML private Button patientCompteRenduButton;

    private ServiceRendezVous service;
    private final Connection cnx = MyDatabase.getInstance().getConnection();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_MONTHS_AHEAD = 6;

    @FXML
    public void initialize() {
        service = new ServiceRendezVous(cnx);

        if (searchField != null) searchField.setText("");

        if (listScroll != null && rendezVousContainer != null) {
            rendezVousContainer.prefTileWidthProperty()
                    .bind(listScroll.widthProperty().subtract(60).divide(2));
            rendezVousContainer.setPrefColumns(2);
        }

        if (sortCombo != null) {
            sortCombo.getItems().addAll(
                    "Date ↑ (croissant)",
                    "Date ↓ (décroissant)"
            );
            sortCombo.getSelectionModel().select("Date ↓ (décroissant)");
        }

        // ✅ Pagination init
        if (pageSizeCombo != null) {
            pageSizeCombo.setItems(FXCollections.observableArrayList(4, 6, 8, 10, 12));
            pageSizeCombo.getSelectionModel().select(Integer.valueOf(pageSize));
            pageSizeCombo.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) {
                    pageSize = newV;
                    currentPage = 1;
                    loadRendezVous();
                }
            });
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> {
                currentPage = 1; // ✅ reset page
                loadRendezVous();
            });
        }

        if (sortCombo != null) {
            sortCombo.valueProperty().addListener((obs, o, n) -> {
                currentPage = 1; // ✅ reset page
                loadRendezVous();
            });
        }

        // First load
        loadRendezVous();
    }

    // ===================== Pagination actions (called by FXML) =====================
    @FXML
    private void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadRendezVous();
        }
    }

    @FXML
    private void nextPage() {
        currentPage++;
        loadRendezVous();
    }

    // ===================== Add RendezVous =====================
    @FXML
    private void handleNewRendezVous() {
        Optional<RendezVous> created = showRendezVousDialog(null);
        created.ifPresent(this::startCaptchaThenInsert);
    }

    private void startCaptchaThenInsert(RendezVous rv) {
        var owner = rendezVousContainer != null && rendezVousContainer.getScene() != null
                ? rendezVousContainer.getScene().getWindow()
                : null;

        ServiceRecaptcha.verify(owner, ok -> {
            if (ok) {
                try {
                    int id = service.addAndReturnId(rv);
                    System.out.println("Inserted rendez_vous id=" + id);
                    showInfo("Validé", "Vérification réussie ✅\nVotre rendez-vous a été ajouté.");
                    currentPage = 1;
                    loadRendezVous();
                } catch (SQLException e) {
                    showError("Erreur Ajout", e);
                }
            } else {
                Alert a = new Alert(Alert.AlertType.WARNING);
                stylePopup(a.getDialogPane());
                if (owner != null) a.initOwner(owner);
                a.setTitle("Échoué");
                a.setHeaderText("Vérification reCAPTCHA échouée");
                a.setContentText("Le rendez-vous n'a pas été ajouté.\n\nVoulez-vous réessayer ?");
                ButtonType retry = new ButtonType("Réessayer", ButtonBar.ButtonData.OK_DONE);
                a.getButtonTypes().setAll(retry, ButtonType.CANCEL);
                a.showAndWait().ifPresent(bt -> {
                    if (bt == retry) startCaptchaThenInsert(rv);
                });
            }
        });
    }

    // ===================== Edit / Delete =====================
    private void handleEdit(RendezVous existing) {
        Optional<RendezVous> updated = showRendezVousDialog(existing);
        updated.ifPresent(rv -> {
            try {
                rv.setIdRv(existing.getIdRv());
                service.updateForPatient(rv, Session.getUserId());
                loadRendezVous();
            } catch (SQLException e) {
                showError("Erreur Modification", e);
            }
        });
    }

    private void handleDelete(RendezVousView rv) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        stylePopup(alert.getDialogPane());
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer ce rendez-vous ?");
        alert.setContentText("Cette action est irréversible.");

        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    service.deleteForPatient(rv.getIdRv(), Session.getUserId());
                    // si page devient vide, on recale
                    loadRendezVous();
                } catch (SQLException e) {
                    showError("Erreur Suppression", e);
                }
            }
        });
    }

    private void handleDelete(RendezVous rv) {
        if (rv == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        stylePopup(alert.getDialogPane());
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer ce rendez-vous ?");
        alert.setContentText("Cette action est irréversible.");

        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    service.deleteForPatient(rv.getIdRv(), Session.getUserId());
                    loadRendezVous();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Erreur", "Impossible de supprimer le rendez-vous.");
                }
            }
        });
    }

    // ===================== LOAD + FILTER + SORT + PAGINATION =====================
    private void loadRendezVous() {
        try {
            List<RendezVousView> list = service.findViewsByPatient(Session.getUserId());

            // Button "Mes comptes-rendus"
            if (patientCompteRenduButton != null) {
                try {
                    ServiceCompteRenduSeance crService = new ServiceCompteRenduSeance(cnx);
                    boolean hasAny = !crService.findViewsByPatient(Session.getUserId()).isEmpty();
                    patientCompteRenduButton.setVisible(hasAny);
                    patientCompteRenduButton.setManaged(hasAny);
                } catch (SQLException ignore) {
                    patientCompteRenduButton.setVisible(false);
                    patientCompteRenduButton.setManaged(false);
                }
            }

            // Filter
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
                                || (rv.getPsychologistFullName() != null && rv.getPsychologistFullName().toLowerCase().contains(kw))
                ).collect(Collectors.toList());
            }

            // Sort
            String sort = (sortCombo == null || sortCombo.getValue() == null) ? "" : sortCombo.getValue();

            Comparator<RendezVousView> byDateTime = Comparator
                    .comparing(RendezVousView::getAppointmentDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(RendezVousView::getAppointmentTimeRv, Comparator.nullsLast(Comparator.naturalOrder()));

            if ("Date ↑ (croissant)".equals(sort)) list.sort(byDateTime);
            else if ("Date ↓ (décroissant)".equals(sort)) list.sort(byDateTime.reversed());

            // ---------------- PAGINATION ----------------
            int totalItems = list.size();
            int totalPages = (int) Math.ceil(totalItems / (double) pageSize);
            if (totalPages == 0) totalPages = 1;

            if (currentPage > totalPages) currentPage = totalPages;
            if (currentPage < 1) currentPage = 1;

            int fromIndex = (currentPage - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, totalItems);

            List<RendezVousView> pageList =
                    totalItems == 0 ? Collections.emptyList() : list.subList(fromIndex, toIndex);

            if (pageLabel != null) pageLabel.setText("Page " + currentPage + "/" + totalPages);
            if (btnPrev != null) btnPrev.setDisable(currentPage <= 1);
            if (btnNext != null) btnNext.setDisable(currentPage >= totalPages);

            // show only pageList
            list = pageList;
            // --------------------------------------------

            rendezVousContainer.getChildren().clear();

            if (list.isEmpty()) {
                Label empty = new Label("Aucun rendez-vous trouvé.");
                empty.setStyle("-fx-text-fill:#666; -fx-font-size: 14px; -fx-padding: 10 0 0 0;");
                rendezVousContainer.getChildren().add(empty);
                return;
            }

            for (RendezVousView rv : list) {
                rendezVousContainer.getChildren().add(buildCard(rv));
            }

        } catch (SQLException e) {
            showError("Erreur chargement", e);
        }
    }

    // ===================== Navigation =====================
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

    // ===================== Dialog (Add/Edit) =====================
    private Optional<RendezVous> showRendezVousDialog(RendezVous existing) {
        boolean isEdit = existing != null;

        Dialog<RendezVous> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier Rendez-vous" : "Nouveau Rendez-vous");
        dialog.setHeaderText(isEdit ? "Modifier les informations" : "Saisir les informations");

        // ✅ Modern popup theme
        stylePopup(dialog.getDialogPane());

        ButtonType saveType = new ButtonType(isEdit ? "Enregistrer" : "Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<PsyItem> psyCombo = new ComboBox<>();
        psyCombo.setPromptText("Choisir psychologue");
        psyCombo.setPrefWidth(260);
        psyCombo.setMaxWidth(Double.MAX_VALUE);
        psyCombo.setItems(FXCollections.observableArrayList(loadPsychologists()));

        DatePicker datePicker = new DatePicker();

        var availableSlots = FXCollections.<LocalTime>observableArrayList();
        var selectedTime = new SimpleObjectProperty<LocalTime>();

        TextField timeField = new TextField();
        timeField.setPromptText("Choisir une heure");
        timeField.setEditable(false);
        timeField.setFocusTraversable(false);
        timeField.setPrefWidth(180);
        timeField.setMaxWidth(Double.MAX_VALUE);
        timeField.getStyleClass().add("time-field");

        Button timePickBtn = new Button("🕒");
        timePickBtn.setFocusTraversable(false);
        timePickBtn.setMinWidth(42);
        timePickBtn.setPrefWidth(42);
        timePickBtn.setMaxWidth(42);
        timePickBtn.getStyleClass().add("icon-btn");
        timePickBtn.setDisable(true);

        selectedTime.addListener((obs, oldV, newV) -> {
            timeField.setText(newV == null ? "" : String.format("%02d:%02d", newV.getHour(), newV.getMinute()));
        });

        timePickBtn.setOnAction(evt -> {
            if (availableSlots.isEmpty()) return;
            LocalDate d = datePicker.getValue();
            LocalTime current = selectedTime.get();
            LocalTime chosen = showTimeSlotDialog(d, availableSlots, current);
            if (chosen != null) selectedTime.set(chosen);
        });

        HBox timeBox = new HBox(10, timePickBtn, timeField);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<RendezVous.TypeRV> typeCombo =
                new ComboBox<>(FXCollections.observableArrayList(RendezVous.TypeRV.values()));

        Label hint = new Label("");
        hint.setStyle("-fx-text-fill:#DC2626; -fx-font-size: 11px;");

        if (isEdit) {
            selectPsychologist(psyCombo, existing.getIdPsychologist());
            if (existing.getAppointmentDate() != null) datePicker.setValue(existing.getAppointmentDate().toLocalDate());
            if (existing.getAppointmentTimeRv() != null) selectedTime.set(existing.getAppointmentTimeRv().toLocalTime());
            typeCombo.setValue(existing.getTypeRendezVous());
        } else {
            datePicker.setValue(LocalDate.now());
            selectedTime.set(null);
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

        grid.add(new Label("Type"), 0, 3);
        grid.add(typeCombo, 1, 3);

        grid.add(hint, 1, 4);

        GridPane.setHgrow(psyCombo, Priority.ALWAYS);
        GridPane.setHgrow(datePicker, Priority.ALWAYS);
        GridPane.setHgrow(typeCombo, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(saveType);
        saveBtn.setDisable(true);

        Runnable refreshSlots = () -> {
            PsyItem sel = psyCombo.getValue();
            int idPsy = (sel == null) ? -1 : sel.getId();
            LocalDate d = datePicker.getValue();

            availableSlots.clear();
            selectedTime.set(null);
            timePickBtn.setDisable(true);

            if (idPsy <= 0 || d == null) {
                hint.setText("Choisissez d'abord un psychologue et une date.");
                return;
            }

            timePickBtn.setDisable(false);

            try {
                if (!service.isPsychologistUser(idPsy)) {
                    hint.setText("Psychologue invalide : doit exister avec role='psychologue'.");
                    timePickBtn.setDisable(true);
                    return;
                }

                var reserved = service.getReservedTimes(idPsy, d, isEdit ? existing.getIdRv() : null);

                LocalTime lastStart = SLOT_END.minusMinutes(APPOINTMENT_DURATION_MIN);

                LocalTime t = SLOT_START;
                LocalTime now = LocalTime.now().withSecond(0).withNano(0);
                while (!t.isAfter(lastStart)) {
                    if (!d.equals(LocalDate.now()) || !t.isBefore(now)) {
                        boolean overlapsExisting = false;
                        for (LocalTime busyStart : reserved) {
                            if (overlaps(t, busyStart)) {
                                overlapsExisting = true;
                                break;
                            }
                        }
                        if (!overlapsExisting) availableSlots.add(t);
                    }
                    t = t.plusMinutes(SLOT_STEP_MIN);
                }

                LocalTime old = null;
                if (isEdit && existing.getAppointmentTimeRv() != null) {
                    old = existing.getAppointmentTimeRv().toLocalTime();
                    if (!availableSlots.contains(old)) {
                        boolean overlapsExisting = false;
                        for (LocalTime busyStart : reserved) {
                            if (overlaps(old, busyStart)) {
                                overlapsExisting = true;
                                break;
                            }
                        }
                        if (!overlapsExisting) availableSlots.add(old);
                    }
                }

                availableSlots.sort(Comparator.naturalOrder());

                if (availableSlots.isEmpty()) {
                    hint.setText("Aucun créneau disponible (08:00 → 17:00) pour cette date.");
                    timePickBtn.setDisable(true);
                    return;
                }

                if (old != null && availableSlots.contains(old)) selectedTime.set(old);
                if (selectedTime.get() == null) selectedTime.set(availableSlots.get(0));

                hint.setText("");

            } catch (Exception ex) {
                ex.printStackTrace();
                hint.setText("Erreur lors du chargement des créneaux.");
                timePickBtn.setDisable(true);
            }
        };

        Runnable validate = () -> {
            hint.setText("");

            PsyItem selPsy = psyCombo.getValue();
            int idPsy = (selPsy == null) ? -1 : selPsy.getId();

            LocalDate d = datePicker.getValue();
            if (d == null) { saveBtn.setDisable(true); return; }

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

            LocalTime selected = selectedTime.get();
            if (selected == null) { saveBtn.setDisable(true); return; }

            if (typeCombo.getValue() == null) { saveBtn.setDisable(true); return; }

            try {
                if (!service.isPsychologistUser(idPsy)) {
                    hint.setText("ID psychologue invalide (role=psychologue).");
                    saveBtn.setDisable(true);
                    return;
                }
            } catch (SQLException ex) {
                hint.setText("Erreur validation : " + ex.getMessage());
                saveBtn.setDisable(true);
                return;
            }

            saveBtn.setDisable(false);
        };

        psyCombo.valueProperty().addListener((a,b,c)-> { refreshSlots.run(); validate.run(); });
        datePicker.valueProperty().addListener((a,b,c)-> { refreshSlots.run(); validate.run(); });
        selectedTime.addListener((a,b,c)-> validate.run());
        typeCombo.valueProperty().addListener((a,b,c)-> validate.run());

        refreshSlots.run();
        validate.run();

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                PsyItem selPsy = psyCombo.getValue();
                int psyId = (selPsy == null) ? -1 : selPsy.getId();
                LocalDate d = datePicker.getValue();
                LocalTime t = selectedTime.get();

                return new RendezVous(
                        Session.getUserId(),
                        psyId,
                        null,
                        RendezVous.ConfirmationStatus.en_attente,
                        Date.valueOf(d),
                        typeCombo.getValue(),
                        Time.valueOf(t)
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    // ===================== Card UI =====================
    private VBox buildCard(RendezVousView rv) {
        VBox card = new VBox(12);
        HBox.setHgrow(card, Priority.ALWAYS);

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

        HBox psyRow = new HBox(8);
        psyRow.setAlignment(Pos.CENTER_LEFT);

        Region psyIcon = svgIcon(
                "M12 12c2.761 0 5-2.239 5-5S14.761 2 12 2 7 4.239 7 7s2.239 5 5 5zm0 2c-4.418 0-8 2.239-8 5v1h16v-1c0-2.761-3.582-5-8-5z",
                "#2563EB", 14, 14
        );

        Label psyLabel = new Label(psyName);
        psyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill:#222;");
        psyRow.getChildren().addAll(psyIcon, psyLabel);

        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);
        Label typeBadge = buildTypeBadge(rv.getTypeRendezVous());
        Label statutBadge = buildStatutBadge(rv.getStatutRv());
        if (typeBadge != null) badges.getChildren().add(typeBadge);
        if (statutBadge != null) badges.getChildren().add(statutBadge);

        String fullName = (Session.getFullName() == null ? "" : Session.getFullName());
        VBox details = buildSection("INFORMATIONS", "Patient : " + fullName, "ℹ️");

        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = iconButton(
                svgIcon("M4 21h4l11-11-4-4L4 17v4z M14 6l4 4", "#FFFFFF", 16, 16),
                "Modifier", "#2563EB"
        );
        if (!locked) editBtn.setOnAction(e -> handleEdit(toEntity(rv)));
        else {
            editBtn.setDisable(true);
            editBtn.setMouseTransparent(true);
            editBtn.setOpacity(0.45);
        }

        Button delBtn = iconButton(
                svgIcon("M6 7h12l-1 14H7L6 7zm3-3h6l1 2H8l1-2z", "#FFFFFF", 16, 16),
                "Annuler", "#DC2626"
        );
        if (!locked) delBtn.setOnAction(e -> handleDelete(toEntity(rv)));
        else {
            delBtn.setDisable(true);
            delBtn.setMouseTransparent(true);
            delBtn.setOpacity(0.45);
        }

        btns.getChildren().addAll(editBtn, delBtn);

        Label lockedHint = new Label("🔒 Rendez-vous verrouillé (déjà traité par le psychologue)");
        lockedHint.setStyle("-fx-text-fill:#64748B; -fx-font-size:12px; -fx-padding: 4 0 0 0;");
        lockedHint.setVisible(locked);
        lockedHint.setManaged(locked);

        card.getChildren().addAll(
                dateRow,
                buildTopStatusRow(rv.getConfirmationStatus(), locked),
                title,
                psyRow,
                badges,
                details,
                btns,
                lockedHint
        );
        return card;
    }

    private Label buildStatutBadge(RendezVous.StatutRV statut) {
        if (statut == null) return null;

        String text;
        String style;
        switch (statut) {
            case termine -> { text = "Terminé"; style = "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A;"; }
            case en_cours -> { text = "En cours"; style = "-fx-background-color:#DBEAFE; -fx-text-fill:#1D4ED8;"; }
            default -> { text = statut.name(); style = "-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;"; }
        }
        Label badge = new Label("📌  " + text);
        badge.setStyle(style + "-fx-padding: 6 12; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        return badge;
    }

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
        if (locked) pill.setOpacity(0.85);

        row.getChildren().add(pill);
        return row;
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

    private Button iconButton(Region icon, String tooltipText, String bgColor) {
        Button b = new Button();
        b.setGraphic(icon);
        b.setMinSize(36, 36);
        b.setPrefSize(36, 36);
        b.setMaxSize(36, 36);
        b.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; -fx-cursor: hand;");
        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip.install(b, new Tooltip(tooltipText));
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
        stylePopup(a.getDialogPane());
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(e.getMessage());
        a.showAndWait();
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        stylePopup(a.getDialogPane());
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        stylePopup(a.getDialogPane());
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(message);
        a.showAndWait();
    }

    // ===================== Psychologists helpers =====================
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
        List<PsyItem> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_users");
                String fullName = rs.getString("prenom") + " " + rs.getString("nom");
                out.add(new PsyItem(id, fullName));
            }
        } catch (SQLException e) {
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

    /**
     * Sélecteur d'heure "chips"
     */
    private LocalTime showTimeSlotDialog(LocalDate date, List<LocalTime> slots, LocalTime current) {
        Dialog<LocalTime> dialog = new Dialog<>();
        dialog.setTitle("Choisir une heure");
        dialog.setHeaderText(date == null ? null : ("Créneaux disponibles - " + date.format(DATE_FMT)));

        // ✅ Modern popup theme
        stylePopup(dialog.getDialogPane());

        ButtonType okType = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        ToggleGroup group = new ToggleGroup();
        FlowPane grid = new FlowPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.setPrefWrapLength(420);

        for (LocalTime t : slots) {
            ToggleButton b = new ToggleButton(String.format("%02d:%02d", t.getHour(), t.getMinute()));
            b.setUserData(t);
            b.setToggleGroup(group);
            b.setFocusTraversable(false);

            b.setStyle(
                    "-fx-background-radius: 999;" +
                            "-fx-border-radius: 999;" +
                            "-fx-padding: 8 14;" +
                            "-fx-font-size: 12.5px;" +
                            "-fx-background-color: rgba(15, 118, 110, 0.10);" +
                            "-fx-border-color: rgba(15, 118, 110, 0.35);" +
                            "-fx-text-fill: #0f172a;"
            );

            b.selectedProperty().addListener((obs, was, is) -> {
                if (is) {
                    b.setStyle(
                            "-fx-background-radius: 999;" +
                                    "-fx-border-radius: 999;" +
                                    "-fx-padding: 8 14;" +
                                    "-fx-font-size: 12.5px;" +
                                    "-fx-background-color: rgba(15, 118, 110, 0.95);" +
                                    "-fx-border-color: rgba(15, 118, 110, 0.95);" +
                                    "-fx-text-fill: white;"
                    );
                } else {
                    b.setStyle(
                            "-fx-background-radius: 999;" +
                                    "-fx-border-radius: 999;" +
                                    "-fx-padding: 8 14;" +
                                    "-fx-font-size: 12.5px;" +
                                    "-fx-background-color: rgba(15, 118, 110, 0.10);" +
                                    "-fx-border-color: rgba(15, 118, 110, 0.35);" +
                                    "-fx-text-fill: #0f172a;"
                    );
                }
            });

            if (current != null && current.equals(t)) b.setSelected(true);

            grid.getChildren().add(b);
        }

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(260);
        sp.setStyle("-fx-background-color: transparent;");

        dialog.getDialogPane().setContent(sp);

        Node okBtn = dialog.getDialogPane().lookupButton(okType);
        okBtn.setDisable(group.getSelectedToggle() == null);

        group.selectedToggleProperty().addListener((obs, old, sel) -> okBtn.setDisable(sel == null));

        dialog.setResultConverter(btn -> {
            if (btn == okType) {
                Toggle t = group.getSelectedToggle();
                return t == null ? null : (LocalTime) t.getUserData();
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
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