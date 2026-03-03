package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.text.FontWeight;
import models.Reservation;
import services.ReservationService;
import utils.UserSession;
import utils.QrPayloadUtil;
import utils.QrCodeUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import javafx.scene.Parent;
import java.time.LocalDateTime;


public class ReservationsController {

    @FXML
    private TilePane reservationsContainer;
    @FXML
    private Button btnAddReservation;
    @FXML
    private Button btnCheckIn;
    @FXML
    private Button btnHistorique;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;

    private List<Reservation> allReservations = java.util.Collections.emptyList();
    private final ReservationService reservationService = new ReservationService();

    // injecté depuis MindCareLayoutController.loadReservations()
    private MindCareLayoutController layoutController;

    public void setLayoutController(MindCareLayoutController layoutController) {
        this.layoutController = layoutController;
    }

    @FXML
    public void initialize() {

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFiltersAndSort());
        }

        if (sortCombo != null) {
            sortCombo.getItems().setAll(
                    "Trier: date (récentes)",
                    "Trier: date (anciennes)",
                    "Trier: local (A→Z)",
                    "Trier: statut (A→Z)",
                    "Trier: heure début"
            );
            sortCombo.getSelectionModel().selectFirst();
            sortCombo.setOnAction(e -> applyFiltersAndSort());
        }

        if (!UserSession.isLoggedIn()) {
            showEmpty("Veuillez choisir une session.");
            if (btnAddReservation != null) btnAddReservation.setDisable(true);
            return;
        }

        if (btnAddReservation != null) btnAddReservation.setDisable(false);

        // ✅ "Nouvelle réservation" : visible uniquement pour patient
        if (btnAddReservation != null) {
            boolean showAdd = UserSession.isPatient();
            btnAddReservation.setVisible(showAdd);
            btnAddReservation.setManaged(showAdd);
        }

        // ✅ Check-in visible uniquement pour responsable_centre (ou admin)
        if (btnCheckIn != null) {
            boolean show = UserSession.isResponsableCentre() || UserSession.isAdmin();
            btnCheckIn.setVisible(show);
            btnCheckIn.setManaged(show);
        }

        // ✅ Historique visible uniquement pour responsable_centre (ou admin)
        if (btnHistorique != null) {
            boolean show = UserSession.isResponsableCentre() || UserSession.isAdmin();
            btnHistorique.setVisible(show);
            btnHistorique.setManaged(show);
        }

        refreshData();
    }

    private void refreshData() {
        if (reservationsContainer != null) reservationsContainer.getChildren().clear();
        try {
            boolean adminLike = UserSession.isAdmin() || UserSession.isResponsableCentre();

            allReservations = reservationService.getAllWithLocalForUser(
                    UserSession.getCurrentUser().getIdUsers(),
                    adminLike
            );

            applyFiltersAndSort();

        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", ex.getMessage());
        }
    }

    private void applyFiltersAndSort() {
        if (reservationsContainer == null) return;

        reservationsContainer.getChildren().clear();

        String q = (searchField == null || searchField.getText() == null) ? "" : searchField.getText().trim().toLowerCase();
        String sort = (sortCombo == null || sortCombo.getSelectionModel().getSelectedItem() == null)
                ? "" : sortCombo.getSelectionModel().getSelectedItem();

        java.util.List<Reservation> filtered = new java.util.ArrayList<>();
        for (Reservation r : allReservations) {
            if (r == null) continue;
            if (q.isEmpty()) {
                filtered.add(r);
                continue;
            }
            String local = (r.getLocalNom() == null) ? "" : r.getLocalNom().toLowerCase();
            String statut = (r.getStatut() == null) ? "" : r.getStatut().toLowerCase();
            String date = (r.getDateReservation() == null) ? "" : r.getDateReservation().toString();
            String hd = (r.getHeureDebut() == null) ? "" : r.getHeureDebut().toString();
            String hf = (r.getHeureFin() == null) ? "" : r.getHeureFin().toString();
            String type = (r.getTypeSession() == null) ? "" : r.getTypeSession().toLowerCase();
            String motif = (r.getMotif() == null) ? "" : r.getMotif().toLowerCase();

            if (local.contains(q) || statut.contains(q) || date.contains(q) || hd.contains(q) || hf.contains(q) || type.contains(q) || motif.contains(q)) {
                filtered.add(r);
            }
        }

        // Tri
        java.util.Comparator<Reservation> cmp;
        if (sort != null && sort.contains("anciennes")) {
            cmp = java.util.Comparator.comparing(Reservation::getDateReservation, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        } else if (sort != null && sort.contains("local")) {
            cmp = java.util.Comparator.comparing(r -> (r.getLocalNom() == null ? "" : r.getLocalNom().toLowerCase()));
        } else if (sort != null && sort.contains("statut")) {
            cmp = java.util.Comparator.comparing(r -> (r.getStatut() == null ? "" : r.getStatut().toLowerCase()));
        } else if (sort != null && sort.contains("heure")) {
            cmp = java.util.Comparator.comparing(Reservation::getHeureDebut, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        } else {
            // défaut: date (récentes)
            cmp = java.util.Comparator.comparing(Reservation::getDateReservation, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed();
        }
        filtered.sort(cmp);

        for (Reservation r : filtered) {
            reservationsContainer.getChildren().add(createCard(r));
        }

        if (filtered.isEmpty()) {
            showEmpty("Aucune réservation trouvée.");
        }
    }

    // ✅ appelée depuis Reservations.fxml : onAction="#onResetFilters"
    @FXML
    private void onResetFilters() {
        if (searchField != null) searchField.clear();
        if (sortCombo != null && !sortCombo.getItems().isEmpty()) {
            sortCombo.getSelectionModel().selectFirst();
        }
        applyFiltersAndSort();
    }

    private VBox createCard(Reservation r) {

        VBox card = new VBox(10);
        card.setPrefWidth(360);
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-background-radius: 10;");

        VBox body = new VBox(8);
        body.setPadding(new Insets(12));

        Label title = new Label(r.getLocalNom() == null ? "Réservation" : r.getLocalNom());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label date = new Label("📅 " + (r.getDateReservation() == null ? "" : r.getDateReservation().toString()));
        Label heure = new Label("⏰ " +
                (r.getHeureDebut() == null ? "" : r.getHeureDebut().toString()) +
                " - " +
                (r.getHeureFin() == null ? "" : r.getHeureFin().toString()));

        Label statut = new Label(r.getStatut() == null ? "" : r.getStatut());

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button edit = new Button("✏");
        edit.setTooltip(new Tooltip("Modifier"));
        Button del = new Button("🗑");
        del.setTooltip(new Tooltip("Supprimer"));

        // ✅ Demande: responsable_centre ne doit pas voir modifier/supprimer
        boolean showEditDel = UserSession.isPatient() || UserSession.isAdmin();
        if (showEditDel) {
            actions.getChildren().addAll(edit, del);
        }

        // ✅ Actions (corrige le "bloqué" : handlers manquants)
        edit.setOnAction(e -> {
            if (layoutController == null) {
                showAlert(Alert.AlertType.ERROR, "Navigation", "LayoutController non injecté (loadReservations).");
                return;
            }
            layoutController.openReservationEdit(r);
        });

        del.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText(null);
            confirm.setContentText("Supprimer cette réservation ?");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    try {
                        reservationService.delete(r.getIdReservation());
                        refreshData();
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Suppression", ex.getMessage());
                    }
                }
            });
        });

        // ✅ Verrouillage UI si statut final (confirmé/annulé)
        if (r.isLocked()) {
            edit.setDisable(true);
            del.setDisable(true);
        }

        // --- Check-in / Retard (affichage sans QR) ---
        boolean alreadyCheckedIn = false;
        try {
            alreadyCheckedIn = reservationService.isAlreadyCheckedIn(r.getIdReservation());
        } catch (Exception ignore) {
            // best effort UI
        }

        boolean isOverdue = false;
        try {
            var end = r.getEndDateTime();
            if (end != null) isOverdue = end.isBefore(LocalDateTime.now());
        } catch (Exception ignore) {
            // best effort UI
        }

        // ✅ Responsable centre : si EN_ATTENTE => peut Confirmer ou Annuler
        // Une fois confirmé/annulé : interdit de modifier le statut (géré aussi côté service via isLocked)
        String st = r.getStatut() == null ? "" : r.getStatut().trim();
        boolean isConfirmed = ("CONFIRMER".equalsIgnoreCase(st) || "Confirmer".equalsIgnoreCase(st));
        boolean isPending = "EN_ATTENTE".equalsIgnoreCase(st) || "EN ATTENTE".equalsIgnoreCase(st);

        // ✅ Remplacer le texte du statut côté UI (Responsable + Patient)
        if (isConfirmed && alreadyCheckedIn) {
            statut.setText("✅ Check-in déjà");
            statut.setStyle("-fx-background-color:#2E7D32; -fx-text-fill:white; -fx-padding:6 10; -fx-background-radius:8; -fx-font-weight:bold;");
        } else if (isConfirmed && isOverdue) {
            statut.setText("⏰ En retard");
            statut.setStyle("-fx-background-color:#C62828; -fx-text-fill:white; -fx-padding:6 10; -fx-background-radius:8; -fx-font-weight:bold;");
        }
        if (UserSession.isResponsableCentre() && isPending && !r.isLocked()) {
            Button btnConfirm = new Button("✔ Confirmer");
            btnConfirm.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white;");

            Button btnCancel = new Button("✖ Annuler");
            btnCancel.setStyle("-fx-background-color: #C62828; -fx-text-fill: white;");

            btnConfirm.setOnAction(e -> {
                try {
                    reservationService.updateStatus(r.getIdReservation(), "Confirmer");
                    refreshData();
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Statut", ex.getMessage());
                }
            });

            btnCancel.setOnAction(e -> {
                try {
                    reservationService.updateStatus(r.getIdReservation(), "Annuler");
                    refreshData();
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Statut", ex.getMessage());
                }
            });

            actions.getChildren().addAll(btnConfirm, btnCancel);
        }

        // ✅ Patient: si confirmé -> QR seulement si pas encore check-in et pas en retard
        if (UserSession.isPatient() && isConfirmed) {
            if (alreadyCheckedIn) {
                Label ok = new Label("✅ Check-in déjà");
                ok.setStyle("-fx-background-color:#2E7D32; -fx-text-fill:white; -fx-padding:6 10; -fx-background-radius:8; -fx-font-weight:bold;");
                actions.getChildren().add(ok);
            } else if (isOverdue) {
                Label late = new Label("⏰ En retard");
                late.setStyle("-fx-background-color:#C62828; -fx-text-fill:white; -fx-padding:6 10; -fx-background-radius:8; -fx-font-weight:bold;");
                actions.getChildren().add(late);
            } else {
                Button qrBtn = new Button("Voir QR");
                qrBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white;");
                qrBtn.setOnAction(e -> showQrPopup(r));
                actions.getChildren().add(qrBtn);
            }
        }

        // ✅ Responsable: si confirmé -> afficher badge (check-in déjà / en retard) au lieu de laisser vide
        if (UserSession.isResponsableCentre() && isConfirmed) {
            if (alreadyCheckedIn) {
                Label ok = new Label("✅ Check-in déjà");
                ok.setStyle("-fx-background-color:#2E7D32; -fx-text-fill:white; -fx-padding:6 10; -fx-background-radius:8; -fx-font-weight:bold;");
                actions.getChildren().add(ok);
            } else if (isOverdue) {
                Label late = new Label("⏰ En retard");
                late.setStyle("-fx-background-color:#C62828; -fx-text-fill:white; -fx-padding:6 10; -fx-background-radius:8; -fx-font-weight:bold;");
                actions.getChildren().add(late);
            }
        }

        body.getChildren().addAll(title, date, heure, statut, actions);
        card.getChildren().add(body);

        return card;
    }

    private void showQrPopup(Reservation r) {
        try {
            int id = r.getIdReservation();

            Path qrPath = Paths.get("uploads", "qrcodes", "reservation_" + id + ".png");
            File file = qrPath.toFile();

            // ✅ si QR absent => le générer automatiquement
            if (!file.exists()) {
                String payload = QrPayloadUtil.buildPayload(r);
                QrCodeUtil.generatePng(payload, 320, qrPath);
            }

            ImageView imageView = new ImageView(new Image(file.toURI().toString()));
            imageView.setFitWidth(280);
            imageView.setPreserveRatio(true);

            VBox root = new VBox(15, imageView);
            root.setPadding(new Insets(20));
            root.setAlignment(Pos.CENTER);

            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle("QR Réservation #" + id);
            popup.setScene(new Scene(root));
            popup.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "QR Code", ex.getMessage());
        }
    }

    // ✅ appelée depuis Reservations.fxml : onAction="#openAddReservation"
    @FXML
    private void openAddReservation() {
        if (layoutController == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "LayoutController non injecté (loadReservations).");
            return;
        }
        layoutController.loadLocaux();
    }

    private void showEmpty(String msg) {
        if (reservationsContainer == null) return;
        reservationsContainer.getChildren().clear();
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill: #777; -fx-padding: 10; -fx-font-size: 14px;");
        reservationsContainer.getChildren().add(l);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ✅ Ouvrir la page Calendrier dans la scène (contentArea) — pas de Stage
    @FXML
    private void openCalendar() {
        if (layoutController == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "LayoutController non injecté (loadReservations).");
            return;
        }
        layoutController.loadCalendar();
    }

    @FXML
    private void openCheckIn() {
        // sécurité: même si le bouton est caché, on vérifie la session
        if (!(UserSession.isResponsableCentre() || UserSession.isAdmin())) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Le Check-in est réservé au responsable du centre.");
            return;
        }
        if (layoutController == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "LayoutController non injecté.");
            return;
        }
        layoutController.loadCheckIn(); // ✅ simple et fiable
    }

    @FXML
    private AnchorPane contentArea;   // ou mainPane / rootPane etc.

    @FXML
    private void openHistorique() {
        if (!(UserSession.isResponsableCentre() || UserSession.isAdmin())) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé",
                    "L'historique est réservé au responsable du centre.");
            return;
        }

        layoutController.loadHistorique();
    }


}
