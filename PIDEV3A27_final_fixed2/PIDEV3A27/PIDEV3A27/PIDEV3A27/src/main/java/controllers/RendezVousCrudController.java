package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import models.RendezVous;
import models.RendezVousView;
import services.ServiceRendezVous;
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
 * Patient : CRUD de ses rendez-vous avec popups + contrôle de saisie.
 */
public class RendezVousCrudController {

    @FXML private TextField searchField;
    @FXML private VBox rendezVousContainer;
    @FXML private ComboBox<String> sortCombo;


    //objet qui fait les opérations SQL (add/update/delete/find)
    private ServiceRendezVous service;
    private final Connection cnx = MyDatabase.getInstance().getConnection();

    //Tu imposes un format pour :
    //Date : 2026-02-17
    //Heure : 14:30
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    //Crée le service
    //Charge la liste au début
    //Ajoute un listener : à chaque lettre tapée dans search → recharge la liste filtrée
    @FXML
    public void initialize() {
        service = new ServiceRendezVous(cnx);
        loadRendezVous();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> loadRendezVous());
        }
        if (sortCombo != null) {
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
        //➜ popup en mode ajout (car existing = null)
        Optional<RendezVous> created = showRendezVousDialog(null);
        created.ifPresent(rv -> {
            try {
                //insertion DB
                //retourne l’ID généré
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
                //le service doit vérifier id_patient = Session.getUserId()
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
                    //✅ Ici aussi, “ForPatient” = sécurité (ne supprimer que ses RDV).
                    service.deleteForPatient(rv.getIdRv(), Session.getUserId());
                    loadRendezVous();
                } catch (SQLException e) {
                    showError("Erreur Suppression", e);
                }
            }
        });
    }

    private void loadRendezVous() {
        try {
            List<RendezVousView> list = service.findViewsByPatient(Session.getUserId());

            String kw = (searchField == null) ? "" : searchField.getText().toLowerCase().trim();
            if (!kw.isEmpty()) {
                list = list.stream().filter(rv ->
                        String.valueOf(rv.getIdRv()).contains(kw)
                                || String.valueOf(rv.getIdPsychologist()).contains(kw)
                                || (rv.getStatutRv() != null && rv.getStatutRv().name().toLowerCase().contains(kw))
                                || (rv.getTypeRendezVous() != null && rv.getTypeRendezVous().name().toLowerCase().contains(kw))
                                || (rv.getAppointmentDate() != null && rv.getAppointmentDate().toString().toLowerCase().contains(kw))
                                || (rv.getAppointmentTimeRv() != null && rv.getAppointmentTimeRv().toString().toLowerCase().contains(kw))
                ).collect(java.util.stream.Collectors.toList());
            }
            // ✅ TRI (date croissant / décroissant)
            String sort = (sortCombo == null || sortCombo.getValue() == null) ? "" : sortCombo.getValue();

            //On déclare un comparateur byDateTime
            Comparator<RendezVousView> byDateTime = Comparator
                    //on trie d’abord selon la date
                    // getAppo méthode utilisée pour prendre la date de chaque rendez-vous.
                    //tri naturel croissant
                    //nullsLast(...) = si la date est null,
                    // on met cet élément à la fin (pour éviter erreur).
                    //si deux rendez-vous ont la même date, alors compare aussi l’heure
                    .comparing(RendezVousView::getAppointmentDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(RendezVousView::getAppointmentTimeRv, Comparator.nullsLast(Comparator.naturalOrder()));

            if ("Date ↑ (croissant)".equals(sort)) {
                list.sort(byDateTime);
            } else if ("Date ↓ (décroissant)".equals(sort)) {
                list.sort(byDateTime.reversed());
            }
// si rien choisi → on laisse l’ordre SQL par défaut

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
                row.getChildren().add(buildCard(list.get(i)));
            }

        } catch (SQLException e) {
            showError("Erreur chargement", e);
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

        TextField dateField = new TextField();
        dateField.setPromptText("yyyy-MM-dd");

        TextField timeField = new TextField();
        timeField.setPromptText("HH:mm (ex: 14:30)");

        ComboBox<RendezVous.StatutRV> statutCombo =
                new ComboBox<>(FXCollections.observableArrayList(RendezVous.StatutRV.values()));

        ComboBox<RendezVous.TypeRV> typeCombo =
                new ComboBox<>(FXCollections.observableArrayList(RendezVous.TypeRV.values()));

        Label hint = new Label("");
        hint.setStyle("-fx-text-fill:#DC2626; -fx-font-size: 11px;");

        if (isEdit) {
            selectPsychologist(psyCombo, existing.getIdPsychologist());
if (existing.getAppointmentDate() != null) dateField.setText(existing.getAppointmentDate().toLocalDate().format(DATE_FMT));
            if (existing.getAppointmentTimeRv() != null) timeField.setText(existing.getAppointmentTimeRv().toLocalTime().format(TIME_FMT));
            statutCombo.setValue(existing.getStatutRv());
            typeCombo.setValue(existing.getTypeRendezVous());
        } else {
            dateField.setText(LocalDate.now().format(DATE_FMT));
            timeField.setText(LocalTime.now().withSecond(0).withNano(0).format(TIME_FMT));
            statutCombo.setValue(RendezVous.StatutRV.prevu);
            typeCombo.setValue(RendezVous.TypeRV.premiere_consultation);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        grid.add(new Label("Psychologue"), 0, 0);
        grid.add(psyCombo, 1, 0);

        grid.add(new Label("Date"), 0, 1);
        grid.add(dateField, 1, 1);

        grid.add(new Label("Heure"), 0, 2);
        grid.add(timeField, 1, 2);

        grid.add(new Label("Statut"), 0, 3);
        grid.add(statutCombo, 1, 3);

        grid.add(new Label("Type"), 0, 4);
        grid.add(typeCombo, 1, 4);

        grid.add(hint, 1, 5);

        GridPane.setHgrow(psyCombo, Priority.ALWAYS);
        GridPane.setHgrow(dateField, Priority.ALWAYS);
        GridPane.setHgrow(timeField, Priority.ALWAYS);
        GridPane.setHgrow(statutCombo, Priority.ALWAYS);
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

            // date parse
            LocalDate d;
            try {
                d = LocalDate.parse(dateField.getText().trim(), DATE_FMT);
            } catch (Exception e) {
                saveBtn.setDisable(true);
                return;
            }

            // time parse
            try {
                LocalTime.parse(timeField.getText().trim(), TIME_FMT);
            } catch (Exception e) {
                saveBtn.setDisable(true);
                return;
            }

            ok = ok && statutCombo.getValue() != null && typeCombo.getValue() != null;

            if (!ok) {
                saveBtn.setDisable(true);
                return;
            }

            // vérifier psy existe
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
        dateField.textProperty().addListener((a,b,c)-> validate.run());
        timeField.textProperty().addListener((a,b,c)-> validate.run());
        statutCombo.valueProperty().addListener((a,b,c)-> validate.run());
        typeCombo.valueProperty().addListener((a,b,c)-> validate.run());
        validate.run();

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                PsyItem selPsy = psyCombo.getValue();
                int psyId = (selPsy == null) ? -1 : selPsy.getId();
                LocalDate d = LocalDate.parse(dateField.getText().trim(), DATE_FMT);
                LocalTime t = LocalTime.parse(timeField.getText().trim(), TIME_FMT);

                return new RendezVous(
                        Session.getUserId(),
                        psyId,
                        statutCombo.getValue(),
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

        card.setStyle("""
                -fx-background-color: #F9F9F9;
                -fx-padding: 20;
                -fx-background-radius: 10;
                -fx-border-radius: 10;
                -fx-border-color: #E7E7E7;
                """);

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

        Label title = new Label("Rendez-vous");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill:#111;");

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
        badges.getChildren().addAll(buildStatutBadge(rv.getStatutRv()), buildTypeBadge(rv.getTypeRendezVous()));


        // Informations (patient connecté)
        String info = "Patient : " + (Session.getFullName() == null ? "" : Session.getFullName());
        VBox details = buildSection("INFORMATIONS", info, "ℹ️");

        HBox btns = new HBox(10);
        Button editBtn = new Button("✏️");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2563EB; -fx-font-size: 16px; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEdit(toEntity(rv)));

        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DC2626; -fx-font-size: 16px; -fx-cursor: hand;");
        delBtn.setOnAction(e -> handleDelete(rv));

        btns.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(dateRow, title, psyRow, badges, details, btns);
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
                case prevu -> { text = "Prévu"; style = "-fx-background-color:#DBEAFE; -fx-text-fill:#1D4ED8;"; }
                case termine -> { text = "Terminé"; style = "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A;"; }
                case annule -> { text = "Annulé"; style = "-fx-background-color:#FEE2E2; -fx-text-fill:#DC2626;"; }
                default -> { text = statut.name(); style = "-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;"; }
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
        r.setAppointmentDate(v.getAppointmentDate());
        r.setTypeRendezVous(v.getTypeRendezVous());
        r.setAppointmentTimeRv(v.getAppointmentTimeRv());
        return r;
    }

}
