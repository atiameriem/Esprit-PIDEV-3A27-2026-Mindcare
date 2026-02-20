package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.SVGPath;
import models.RendezVous;
import models.RendezVousView;
import services.ServiceRendezVous;
import utils.MyDatabase;
import utils.Session;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javafx.scene.control.ComboBox;
import java.util.Comparator;


/**
 * Psychologue : lecture seule de SES rendez-vous.
 */
public class RendezVousController {

    @FXML private TextField searchField;
    @FXML private TilePane rendezVousContainer;
    @FXML private ComboBox<String> sortCombo;
    // ✅ Bouton "Consultations terminées" (pour ouvrir la page Compte-rendu)
    @FXML private Button terminatedConsultationsButton;
    @FXML private ScrollPane listScroll;


    private ServiceRendezVous service;
    private final Connection cnx = MyDatabase.getInstance().getConnection();

    @FXML
    public void initialize() {
        service = new ServiceRendezVous(cnx);
        loadRendezVous();

        // ✅ Demandé : la barre de recherche ne doit pas contenir du texte au chargement.
        if (searchField != null) {
            searchField.setText("");
        }

        // ✅ Demandé : 2 cartes par ligne SANS scroll horizontal.
        if (listScroll != null && rendezVousContainer != null) {
            rendezVousContainer.prefTileWidthProperty()
                    .bind(listScroll.widthProperty().subtract(60).divide(2));
            rendezVousContainer.setPrefColumns(2);
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> loadRendezVous());

        }
        if (sortCombo != null) {
            sortCombo.getItems().addAll(
                    "Date ↑ (croissant)",
                    "Date ↓ (décroissant)"
            );
//par defaut decroi
            sortCombo.getSelectionModel().select("Date ↓ (décroissant)");

            sortCombo.valueProperty().addListener((obs, o, n) -> loadRendezVous());
        }

    }

    private void loadRendezVous() {
        try {
            int psyId = Session.getUserId();

            // ✅ ICI: utiliser la méthode VIEW (avec noms)
            List<RendezVousView> list = service.findViewsByPsychologist(psyId);

            // ✅ Afficher le bouton "Consultations terminées" seulement si on a au moins un RDV confirmé + terminé
            if (terminatedConsultationsButton != null) {
                boolean hasTermine = list.stream().anyMatch(rv ->
                        rv.getConfirmationStatus() == RendezVous.ConfirmationStatus.confirme
                                && rv.getStatutRv() == RendezVous.StatutRV.termine
                );
                terminatedConsultationsButton.setVisible(hasTermine);
                terminatedConsultationsButton.setManaged(hasTermine);
            }

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
                ).collect(java.util.stream.Collectors.toList());
            }
            // ✅ TRI (date croissant / décroissant)
            //On crée une variable sort (type String)
            // qui contient le choix de tri.
            //sortCombo == null → le ComboBox n’existe pas (sécurité)
            //sortCombo.getValue() == null → l’utilisateur n’a rien sélectionné
            //Si l’une des deux est vraie → sort = "" (chaîne vide)
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
                //le comparateur byDateTime.
                list.sort(byDateTime);
            } else if ("Date ↓ (décroissant)".equals(sort)) {
                //byDateTime.reversed() crée la version inverse du comparateur.
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

            // ✅ TilePane : on ajoute directement les cards (pas de HBox intermédiaire)
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

        // ✅ Deux cartes par ligne : la largeur est gérée par le TilePane (binding sur prefTileWidth)
        card.setMaxWidth(Double.MAX_VALUE);

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
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-font-weight: 700;");

        dateRow.getChildren().addAll(calIcon, dateLabel);

        // Title
        Label title = new Label("Rendez-vous");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill:#111;");
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

        Label typeBadge = buildTypeBadge(rv.getTypeRendezVous());
        Label statutBadge = buildStatutBadge(rv.getStatutRv());

        // ✅ Demandé : pas de doublon "Confirmé/En attente" ici (déjà affiché en haut)
        if (typeBadge != null) badges.getChildren().add(typeBadge);
        if (statutBadge != null) badges.getChildren().add(statutBadge);

// Details
        String info = "Patient : " + (rv.getPatientFullName()==null? "" : rv.getPatientFullName());
        VBox details = buildSection("INFORMATIONS", info, "ℹ️");

        // ✅ Actions psychologue :
        // - si en_attente : Confirmer / Annuler
        // - si confirmé : choisir En cours / Terminé
        // - si annulé : rien
        HBox actions = buildPsychologistActions(rv);

        card.getChildren().addAll(dateRow, buildTopStatusRow(rv.getConfirmationStatus()), title, psyRow, badges, details, actions);
        return card;
    }

    private Label buildStatutBadge(RendezVous.StatutRV statut) {
        String text;
        String style;

        if (statut == null) {
            // ✅ Demandé : ne pas afficher "Statut inconnu" (on masque le badge si null)
            return null;
        } else {
            switch (statut) {
                case termine -> {
                    text = "Terminé";
                    style = "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A;";
                }
                case en_cours -> {
                    text = "En cours";
                    style = "-fx-background-color:#DBEAFE; -fx-text-fill:#1D4ED8;";
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

    // ✅ Badge confirmation_status
    // ===================== NOUVEL AFFICHAGE : statut en haut =====================
// ✅ Affiche clairement l'état de confirmation du rendez-vous tout en haut.
    private HBox buildTopStatusRow(RendezVous.ConfirmationStatus status) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        String txt;
        String style;

        if (status == null || status == RendezVous.ConfirmationStatus.en_attente) {
            // ✅ Demandé : afficher فقط "EN ATTENTE" (sans 'à traiter')
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

    // ✅ UI + logique pour les actions du psychologue
    private HBox buildPsychologistActions(RendezVousView rv) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);

        RendezVous.ConfirmationStatus cs = rv.getConfirmationStatus();

        if (cs == null || cs == RendezVous.ConfirmationStatus.en_attente) {
            Button confirmBtn = new Button("✅ Confirmer");
            confirmBtn.setStyle("-fx-background-color:#16A34A; -fx-text-fill:white; -fx-background-radius:8; -fx-cursor:hand;");
            confirmBtn.setOnAction(e -> {
                try {
                    // 1) confirmation_status = confirme
                    service.updateConfirmationStatusForPsychologist(rv.getIdRv(), Session.getUserId(), RendezVous.ConfirmationStatus.confirme);
                    // ✅ Demandé : ne pas mettre automatiquement "en_cours".
                    // Le psychologue choisira ensuite "En cours" ou "Terminé".
                    loadRendezVous();
                } catch (SQLException ex) {
                    showError("Erreur confirmation", ex);
                }
            });

            Button cancelBtn = new Button("❌ Annuler");
            cancelBtn.setStyle("-fx-background-color:#DC2626; -fx-text-fill:white; -fx-background-radius:8; -fx-cursor:hand;");
            cancelBtn.setOnAction(e -> {
                try {
                    service.updateConfirmationStatusForPsychologist(rv.getIdRv(), Session.getUserId(), RendezVous.ConfirmationStatus.annule);
                    loadRendezVous();
                } catch (SQLException ex) {
                    showError("Erreur annulation", ex);
                }
            });

            box.getChildren().addAll(confirmBtn, cancelBtn);
            return box;
        }

        if (cs == RendezVous.ConfirmationStatus.confirme) {
            // ✅ Nouvelle règle demandée :
            // - Si statut = null      => le psy peut choisir En cours OU Terminé
            // - Si statut = en_cours  => le psy peut encore passer à Terminé plus tard
            // - Si statut = termine   => plus aucun bouton (verrouillé)
            RendezVous.StatutRV st = rv.getStatutRv();
            if (st == RendezVous.StatutRV.termine) {
                return box;
            }

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

            // ✅ Petit hint : le compte-rendu sera disponible uniquement quand c'est "Terminé"
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

            // st == en_cours => on affiche seulement "Terminé" (modifiable plus tard)
            box.getChildren().addAll(doneBtn, hint);
            return box;
        }

        // annule : pas d'action
        return box;
    }

    // ✅ Ouverture de la page Compte-rendu depuis l'écran rendez-vous (sans sidebar)
    @FXML
    private void openCompteRenduFromRendezVous() {
        try {
            VBox contentArea = (VBox) rendezVousContainer.getScene().lookup("#contentArea");
            if (contentArea == null) return;

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/views/CompteRendu.fxml"));
            javafx.scene.Node view = loader.load();
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


    // ✅ Petit bouton icône (plus lisible que texte, demandé)
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
}
