
        package controllers;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import models.RendezVous;
import models.RendezVousView;
import services.ServiceRendezVous;
import utils.MyDatabase;
import utils.Session;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Psychologue : lecture + actions + pagination.
 */
public class RendezVousController {

    @FXML private TextField searchField;
    @FXML private TilePane rendezVousContainer;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button terminatedConsultationsButton;
    @FXML private ScrollPane listScroll;

    // Pagination UI
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label pageLabel;
    @FXML private ComboBox<Integer> pageSizeCombo;

    private int currentPage = 1;
    private int pageSize = 4;

    private ServiceRendezVous service;
    private final Connection cnx = MyDatabase.getInstance().getConnection();

    // ===================== TWILIO =====================
    private static final String ACCOUNT_SID = ""; // TODO: fill
    private static final String AUTH_TOKEN  = ""; // TODO: fill
    private static final String TWILIO_FROM = "";

    //to num ptient
    //body = contenu du SMS
    private void sendSmsTwilio(String to, String body) {
        //Initialise Twilio avec tes identifiants (clé + token).
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        //Prépare et envoie réellement le SMS.
        Message message = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(TWILIO_FROM),
                body
        ).create ();
        System.out.println("SMS sent! SID: " + message.getSid());
    }
    // ================================================

    // ===================== UX Annulation "Smart" =====================
    private void openSmartCancelDialog(RendezVousView rv) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        stylePopup1(alert.getDialogPane());

        alert.setTitle("Annuler la demande ?");
        alert.setHeaderText("Annuler la demande ?");
        alert.setContentText(
                "Ce rendez-vous n’est pas encore confirmé. Vous pouvez modifier l’heure, " +
                        "le déplacer à la même heure la semaine prochaine, ou l’annuler définitivement."
        );

        ButtonType btModify = new ButtonType("Modifier l’heure (±2h)");
        ButtonType btNextWeek = new ButtonType("Même jour semaine suivante (+7 jours)");
        ButtonType btCancelDef = new ButtonType("Annuler définitivement", ButtonBar.ButtonData.OK_DONE);
        ButtonType btBack = new ButtonType("Retour", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btModify, btNextWeek, btCancelDef, btBack);

        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() == btBack) return;

        try {
            String patientPhone = service.getPatientPhoneByRdvId(rv.getIdRv());
            if (patientPhone == null || patientPhone.isBlank()) {
                showError("Téléphone manquant", new Exception("Le patient n'a pas de numéro dans users.telephone"));
                return;
            }

            LocalDate oldDate = rv.getAppointmentDate().toLocalDate();
            LocalTime oldTime = rv.getAppointmentTimeRv().toLocalTime();

            DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

            // 1) Modifier l'heure (±2h)
            if (choice.get() == btModify) {
                //Génère une liste d’heures possibles autour de oldTime (±2h
                List<String> options = buildTimeOptionsPlusMinus2h(oldTime);
                if (options.isEmpty()) return;
                //Ouvre un ChoiceDialog pour choisir la nouvelle heure
                ChoiceDialog<String> cd = new ChoiceDialog<>(options.get(0), options);
                stylePopup1(alert.getDialogPane());

                cd.setTitle("Modifier l’heure");
                cd.setHeaderText("Choisissez une nouvelle heure (±2h)");
                cd.setContentText("Nouvelle heure :");

                Optional<String> picked = cd.showAndWait();
                if (picked.isEmpty()) return;

                LocalTime newTime = LocalTime.parse(picked.get(), tf);
//Met à jour la DB
                service.rescheduleForPsychologist(
                        rv.getIdRv(),
                        Session.getUserId(),
                        java.sql.Date.valueOf(oldDate),
                        Time.valueOf(newTime)
                );

                String msg = "Bonjour, votre rendez-vous a été MODIFIÉ ⏰\n" +
                        "Ancien: " + oldDate + " à " + oldTime.format(tf) + "\n" +
                        "Nouveau: " + oldDate + " à " + newTime.format(tf) + "\n" +
                        "Psychologue: " + Session.getFullName();
                sendSmsTwilio(patientPhone, msg);

                loadRendezVous();
                return;
            }

            // 2) Même jour semaine suivante (+7 jours)
            // 2) +7 jours
            if (choice.get() == btNextWeek) {
    //le psy connecté doit être le psy du RDV
                int sessionPsy = Session.getUserId();
                int psyFromRv  = rv.getIdPsychologist();

                // ✅ garde-fou
                if (sessionPsy != psyFromRv) {
                    throw new SQLException(
                            "Session psy (" + sessionPsy + ") ≠ RDV psy (" + psyFromRv + "). " +
                                    "Ton login/Session ou tes données DB ne correspondent pas."
                    );
                }

                // Déplace le RDV à oldDate + 7 jours (même heure)
                service.moveToNextWeekSameTimeForPsychologist(rv.getIdRv(), psyFromRv);

                // ✅ puis confirmer
                service.updateConfirmationStatusForPsychologist(
                        rv.getIdRv(),
                        psyFromRv,
                        RendezVous.ConfirmationStatus.confirme
                );

                LocalDate newDate = oldDate.plusDays(7);

                String msg = "Bonjour, votre rendez-vous a été DÉPLACÉ 📅\n" +
                        "Ancien: " + oldDate + " à " + oldTime.format(tf) + "\n" +
                        "Nouveau: " + newDate + " à " + oldTime.format(tf) + "\n" +
                        "Psychologue: " + Session.getFullName();

                sendSmsTwilio(patientPhone, msg);
                loadRendezVous();
                return;
            }
            // 3) Annuler définitivement
            //Met le statut à annulé
            //Envoie SMS “ANNULÉ”
            //Recharge l’UI
            if (choice.get() == btCancelDef) {
                service.updateConfirmationStatusForPsychologist(
                        rv.getIdRv(),
                        Session.getUserId(),
                        RendezVous.ConfirmationStatus.annule
                );

                String msg = "Bonjour, votre rendez-vous a été ANNULÉ ❌\n" +
                        "Date: " + oldDate + " à " + oldTime.format(tf) + "\n" +
                        "Psychologue: " + Session.getFullName();
                sendSmsTwilio(patientPhone, msg);

                loadRendezVous();
            }

        } catch (Exception ex) {
            showError("Erreur annulation / reprogrammation / SMS", ex);
        }
    }

    /**
     * Construit des choix d'heure autour de l'heure actuelle (±2h) par pas de 30 min,
     * en restant dans une plage raisonnable (08:00 à 17:00).
     */
    private List<String> buildTimeOptionsPlusMinus2h(LocalTime base) {
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime min = base.minusHours(2);
        LocalTime max = base.plusHours(2);

        // clamp dans 08:00 - 17:00
        LocalTime dayStart = LocalTime.of(8, 0);
        LocalTime dayEnd = LocalTime.of(17, 0);
        if (min.isBefore(dayStart)) min = dayStart;
        if (max.isAfter(dayEnd)) max = dayEnd;

        List<String> opts = new ArrayList<>();
        // Arrondir min au prochain pas de 30 minutes
        int minute = min.getMinute();
        int rounded = (minute % 30 == 0) ? minute : (minute < 30 ? 30 : 0);
        LocalTime t = min.withMinute(rounded).withSecond(0).withNano(0);
        if (minute > 30) t = t.plusHours(1); // ex: 10:40 -> 11:00

        while (!t.isAfter(max)) {
            opts.add(t.format(tf));
            t = t.plusMinutes(30);
        }

        // si base est valide et absent (rare), l'ajouter en tête
        String baseStr = base.format(tf);
        if (!opts.contains(baseStr)) {
            opts.add(0, baseStr);
        }
        return opts;
    }
    // =========================================================

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
            pageSizeCombo.setItems(javafx.collections.FXCollections.observableArrayList(4, 6, 8, 10, 12));
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
                currentPage = 1;
                loadRendezVous();
            });
        }

        if (sortCombo != null) {
            sortCombo.valueProperty().addListener((obs, o, n) -> {
                currentPage = 1;
                loadRendezVous();
            });
        }

        loadRendezVous();
    }

    // Pagination actions
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

    private void loadRendezVous() {
        try {
            int psyId = Session.getUserId();
            List<RendezVousView> list = service.findViewsByPsychologist(psyId);

            if (terminatedConsultationsButton != null) {
                boolean hasTermine = list.stream().anyMatch(rv ->
                        rv.getConfirmationStatus() == RendezVous.ConfirmationStatus.confirme
                                && rv.getStatutRv() == RendezVous.StatutRV.termine
                );
                terminatedConsultationsButton.setVisible(hasTermine);
                terminatedConsultationsButton.setManaged(hasTermine);
            }

            // Filter
            String kw = (searchField == null) ? "" : searchField.getText().toLowerCase().trim();
            if (!kw.isEmpty()) {
                list = list.stream().filter(rv ->
                        String.valueOf(rv.getIdRv()).contains(kw)
                                || String.valueOf(rv.getIdPatient()).contains(kw)
                                || (rv.getPatientFullName() != null && rv.getPatientFullName().toLowerCase().contains(kw))
                                || (rv.getStatutRv() != null && rv.getStatutRv().name().toLowerCase().contains(kw))
                                || (rv.getConfirmationStatus() != null && rv.getConfirmationStatus().name().toLowerCase().contains(kw))
                                || (rv.getTypeRendezVous() != null && rv.getTypeRendezVous().name().toLowerCase().contains(kw))
                                || (rv.getAppointmentDate() != null && rv.getAppointmentDate().toString().toLowerCase().contains(kw))
                                || (rv.getAppointmentTimeRv() != null && rv.getAppointmentTimeRv().toString().toLowerCase().contains(kw))
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
            showError("Erreur chargement rendez-vous", e);
        }
    }

    private VBox buildCard(RendezVousView rv) {
        VBox card = new VBox(12);
        HBox.setHgrow(card, Priority.ALWAYS);

        card.setStyle("""
                -fx-background-color: #FFFFFF;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);
                -fx-padding: 20;
                -fx-background-radius: 10;
                -fx-border-radius: 10;
                -fx-border-color: #E7E7E7;
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

        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = buildTypeBadge(rv.getTypeRendezVous());
        Label statutBadge = buildStatutBadge(rv.getStatutRv());

        if (typeBadge != null) badges.getChildren().add(typeBadge);
        if (statutBadge != null) badges.getChildren().add(statutBadge);

        String info = "Patient : " + (rv.getPatientFullName() == null ? "" : rv.getPatientFullName());
        VBox details = buildSection("INFORMATIONS", info, "ℹ️");

        HBox actions = buildPsychologistActions(rv);

        card.getChildren().addAll(
                dateRow,
                buildTopStatusRow(rv.getConfirmationStatus()),
                title,
                psyRow,
                badges,
                details,
                actions
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

    private HBox buildTopStatusRow(RendezVous.ConfirmationStatus status) {
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
        row.getChildren().add(pill);
        return row;
    }

    // Actions Psychologue + SMS quand confirmer
    private HBox buildPsychologistActions(RendezVousView rv) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);

        RendezVous.ConfirmationStatus cs = rv.getConfirmationStatus();

        if (cs == null || cs == RendezVous.ConfirmationStatus.en_attente) {
            Button confirmBtn = new Button("✅ Confirmer");
            confirmBtn.setStyle("-fx-background-color:#16A34A; -fx-text-fill:white; -fx-background-radius:8; -fx-cursor:hand;");
            confirmBtn.setOnAction(e -> {
                try {
                    service.updateConfirmationStatusForPsychologist(
                            rv.getIdRv(),
                            Session.getUserId(),
                            RendezVous.ConfirmationStatus.confirme
                    );

                    String patientPhone = service.getPatientPhoneByRdvId(rv.getIdRv());
                    if (patientPhone == null || patientPhone.isBlank()) {
                        showError("Téléphone manquant", new Exception("Le patient n'a pas de numéro dans users.telephone"));
                        loadRendezVous();
                        return;
                    }

                    String msg = "Bonjour, votre rendez-vous est CONFIRMÉ ✅ le "
                            + rv.getAppointmentDate() + " à " + rv.getAppointmentTimeRv()
                            + ". Psychologue: " + Session.getFullName();

                    sendSmsTwilio(patientPhone, msg);

                    loadRendezVous();

                } catch (Exception ex) {
                    showError("Erreur confirmation / SMS", ex);
                }
            });

            Button cancelBtn = new Button("❌ Annuler");
            cancelBtn.setStyle("-fx-background-color:#DC2626; -fx-text-fill:white; -fx-background-radius:8; -fx-cursor:hand;");
            cancelBtn.setOnAction(e -> {
                // ✅ Variante "smart" : 3 choix (modifier l'heure / +7 jours / annuler)
                openSmartCancelDialog(rv);
            });

            box.getChildren().addAll(confirmBtn, cancelBtn);
            return box;
        }

        if (cs == RendezVous.ConfirmationStatus.confirme) {
            RendezVous.StatutRV st = rv.getStatutRv();
            if (st == RendezVous.StatutRV.termine) return box;

            Button doneBtn = new Button("🟩 Terminé");
            doneBtn.setStyle("-fx-background-color:#16A34A; -fx-text-fill:white; -fx-background-radius:8; -fx-cursor:hand;");
            doneBtn.setOnAction(e -> {
                try {
                    service.updateStatutForPsychologist(rv.getIdRv(), Session.getUserId(), RendezVous.StatutRV.termine);
                    loadRendezVous();
                } catch (SQLException ex) {
                    showError("Erreur statut", ex);
                }
            });

            Label hint = new Label("(Compte-rendu possible quand Terminé)");
            hint.setStyle("-fx-text-fill:#64748B; -fx-font-size: 11px;");

            if (st == null) {
                Button inProgressBtn = new Button("🟦 En cours");
                inProgressBtn.setStyle("-fx-background-color:#2563EB; -fx-text-fill:white; -fx-background-radius:8; -fx-cursor:hand;");
                inProgressBtn.setOnAction(e -> {
                    try {
                        service.updateStatutForPsychologist(rv.getIdRv(), Session.getUserId(), RendezVous.StatutRV.en_cours);
                        loadRendezVous();
                    } catch (SQLException ex) {
                        showError("Erreur statut", ex);
                    }
                });

                box.getChildren().addAll(inProgressBtn, doneBtn, hint);
                return box;
            }

            box.getChildren().addAll(doneBtn, hint);
            return box;
        }

        return box;
    }

    @FXML
    private void openCompteRenduFromRendezVous() {
        try {
            // IMPORTANT: lookup("#contentArea") fonctionne uniquement si l'élément a un Node.id="contentArea"
            // (pas seulement fx:id). C'est maintenant garanti dans MindCareLayout.fxml.
            VBox contentArea = (VBox) rendezVousContainer.getScene().lookup("#contentArea");
            if (contentArea == null) {
                throw new IllegalStateException("Zone de contenu introuvable (contentArea)");
            }

            // Charger la bonne vue selon le rôle (sécurité)
            String fxml = utils.Session.isPsychologue() ? "CompteRendu.fxml" : "CompteRenduRead.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxml));
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            showError("Erreur navigation", e);
        }
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
        stylePopup(a.getDialogPane());
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(e.getMessage());
        a.showAndWait();
    }

    private void stylePopup(DialogPane pane) {
        try {
            if (pane == null) return;
            pane.getStyleClass().add("mc-dialog");
            var css = getClass().getResource("/popup.css");
            if (css != null) pane.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void openStats() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RendezVousStats.fxml"));
            Parent root = loader.load();

            Scene currentScene = searchField.getScene();

            RendezVousStatsController controller = loader.getController();
            controller.setPreviousScene(currentScene);

            Scene statsScene = new Scene(root);

            URL css = getClass().getResource("/views/stats.css");
            if (css != null) statsScene.getStylesheets().add(css.toExternalForm());

            Stage stage = (Stage) currentScene.getWindow();
            stage.setScene(statsScene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //pour css du popup de fonction annuler
    private void stylePopup1(DialogPane pane) {
        if (pane == null) return;

        // Classe CSS commune (comme tes autres popups)
        pane.getStyleClass().add("mc-dialog");

        // Charger popup.css depuis resources
        var css = getClass().getResource("/popup.css");
        if (css != null) {
            if (!pane.getStylesheets().contains(css.toExternalForm())) {
                pane.getStylesheets().add(css.toExternalForm());
            }
        } else {
            System.out.println("[WARN] popup.css introuvable dans resources !");
        }
    }
}