package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import models.LocalRelaxation;
import services.LocalRelaxationService;
import utils.UserSession;

import java.sql.SQLException;
import java.util.List;

public class LocauxController {

    @FXML private TilePane locauxContainer;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> etageCombo;
    @FXML private TextField searchField;
    @FXML private Button btnAddLocal;

    private final LocalRelaxationService localService = new LocalRelaxationService();

    // ✅ injecté depuis MindCareLayoutController.loadLocaux()
    private MindCareLayoutController layoutController;
    public void setLayoutController(MindCareLayoutController layoutController) {
        this.layoutController = layoutController;
    }

    @FXML
    public void initialize() {

        // ✅ patient: cacher le bouton Ajouter local
        if (btnAddLocal != null && UserSession.isPatient()) {
            btnAddLocal.setVisible(false);
            btnAddLocal.setManaged(false);
        }

        if (typeCombo != null) {
            typeCombo.getItems().setAll("Tous", "MEDITATION", "THERAPIE_GROUPE", "YOGA", "RESPIRATION", "AUTRE");
            typeCombo.getSelectionModel().selectFirst();
            typeCombo.setOnAction(e -> refresh());
        }

        if (etageCombo != null) {
            etageCombo.getItems().setAll("Tous", "0", "1", "2", "3", "4", "5");
            etageCombo.getSelectionModel().selectFirst();
            etageCombo.setOnAction(e -> refresh());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> refresh());
        }

        refresh();
    }

    public void refresh() {
        if (locauxContainer != null) locauxContainer.getChildren().clear();

        try {
            List<LocalRelaxation> locaux = localService.getAll();

            String typeFilter = (typeCombo == null) ? "Tous" : typeCombo.getValue();
            String etageFilter = (etageCombo == null) ? "Tous" : etageCombo.getValue();
            String q = (searchField == null || searchField.getText() == null) ? "" : searchField.getText().toLowerCase().trim();

            for (LocalRelaxation l : locaux) {
                if (typeFilter != null && !"Tous".equals(typeFilter) && !typeFilter.equalsIgnoreCase(l.getType())) continue;

                if (etageFilter != null && !"Tous".equals(etageFilter)) {
                    try {
                        int et = Integer.parseInt(etageFilter);
                        if (l.getEtage() != et) continue;
                    } catch (NumberFormatException ignored) {}
                }

                if (!q.isBlank()) {
                    String hay = (safe(l.getNom()) + " " + safe(l.getDescription()) + " " + safe(l.getType())).toLowerCase();
                    if (!hay.contains(q)) continue;
                }

                locauxContainer.getChildren().add(createCard(l));
            }

            if (locaux.isEmpty()) {
                Label empty = new Label("Aucun local trouvé.");
                empty.setStyle("-fx-text-fill:#777; -fx-padding: 10; -fx-font-size: 14px;");
                locauxContainer.getChildren().add(empty);
            }

        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", ex.getMessage());
        }
    }

    private VBox createCard(LocalRelaxation l) {
        // carte style "pro" (image + overlay actions + bouton en bas)
        VBox card = new VBox(10);
        card.setPrefWidth(360);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #E0E0E0;");

        // --- image + overlay ---
        StackPane top = new StackPane();
        top.setPrefHeight(200);
        top.setMaxHeight(200);
        top.setStyle("-fx-background-color:#ECEFF1; -fx-background-radius: 12 12 0 0; -fx-border-radius: 12 12 0 0;");

        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
        iv.setFitHeight(200);
        iv.setFitWidth(360);
        iv.setPreserveRatio(false);

        String imgName = (l.getImage() == null || l.getImage().isBlank()) ? "default.png" : l.getImage();
        java.io.File f = new java.io.File("uploads/locaux/" + imgName);
        if (f.exists()) {
            iv.setImage(new javafx.scene.image.Image(f.toURI().toString(), 360, 200, false, true));
        } else {
            var url = getClass().getResource("/images/default.png");
            if (url != null) iv.setImage(new javafx.scene.image.Image(url.toExternalForm(), 360, 200, false, true));
        }

        top.getChildren().add(iv);

        // actions overlay (edit/delete)
        boolean canManageLocaux = UserSession.isAdmin() || UserSession.isResponsableCentre();
        if (canManageLocaux) {
            HBox overlayBtns = new HBox(8);
            overlayBtns.setPadding(new javafx.geometry.Insets(10));
            Button edit = iconButton("✏", "Modifier",
                    "-fx-background-color: rgba(255,255,255,0.9); -fx-text-fill: #263238; -fx-background-radius: 8;",
                    () -> openLocalForm(l.getIdLocal()));

            Button del = iconButton("🗑", "Supprimer",
                    "-fx-background-color: rgba(255,255,255,0.9); -fx-text-fill: #B71C1C; -fx-background-radius: 8;",
                    () -> deleteLocal(l.getIdLocal()));
            overlayBtns.getChildren().addAll(edit, del);
            StackPane.setAlignment(overlayBtns, javafx.geometry.Pos.TOP_LEFT);
            top.getChildren().add(overlayBtns);
        }

        if (!l.isDisponible()) {
            Label dispo = new Label("Indisponible");
            dispo.setStyle("-fx-background-color: #E53935; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 14;");
            StackPane.setAlignment(dispo, javafx.geometry.Pos.TOP_RIGHT);
            StackPane.setMargin(dispo, new javafx.geometry.Insets(12));
            top.getChildren().add(dispo);
        }

        card.getChildren().add(top);

        VBox body = new VBox(8);
        body.setPadding(new javafx.geometry.Insets(12, 14, 14, 14));

        Label nameLabel = new Label(safe(l.getNom()));
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        body.getChildren().add(nameLabel);

        Label descLabel = new Label(safe(l.getDescription()));
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill:#546E7A;");
        body.getChildren().add(descLabel);

        body.getChildren().add(infoLine("📌", "Type:", safe(l.getType())));
        body.getChildren().add(infoLine("👥", "Capacité:", String.valueOf(l.getCapacite()) + " personnes"));
        body.getChildren().add(infoLine("🧰", "Équipements:", safe(l.getEquipements())));
        body.getChildren().add(infoLine("🏢", "Étage:", String.valueOf(l.getEtage())));
        body.getChildren().add(infoLine("⏱", "Durée max:", l.getDureeMaxSession() + " min"));
        body.getChildren().add(infoLine("💰", "Tarif:", (l.getTarifHoraire() == null ? "0" : l.getTarifHoraire().toPlainString()) + " DT / heure"));
        body.getChildren().add(infoLine("🟢", "État:", safe(l.getEtat())));

        // bouton réserver (patient uniquement)
        if (UserSession.isPatient()) {
            Button reserveBtn = new Button(l.isDisponible() ? "Réserver" : "Non disponible");
            reserveBtn.setMaxWidth(Double.MAX_VALUE);
            reserveBtn.setDisable(!l.isDisponible());
            reserveBtn.setStyle(l.isDisponible()
                    ? "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 12; -fx-background-radius: 10;"
                    : "-fx-background-color: #CFD8DC; -fx-text-fill: #455A64; -fx-font-weight: bold; -fx-padding: 10 12; -fx-background-radius: 10;");
            reserveBtn.setOnAction(e -> openReservationForm(l.getIdLocal(), l.getNom()));
            body.getChildren().add(new javafx.scene.layout.Region());
            body.getChildren().add(reserveBtn);
        }

        card.getChildren().add(body);
        return card;
    }

    private HBox infoLine(String icon, String label, String value) {
        Label ic = new Label(icon);
        ic.setMinWidth(22);
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold;");
        Label v = new Label(value == null ? "" : value);
        v.setWrapText(true);
        HBox line = new HBox(6, ic, l, v);
        line.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return line;
    }

    private Button iconButton(String icon, String tooltip, String style, Runnable action) {
        Button b = new Button(icon);
        b.setStyle(style);
        b.setMinWidth(34);
        b.setPrefWidth(34);
        b.setMaxWidth(34);
        b.setTooltip(new Tooltip(tooltip));
        b.setOnAction(e -> action.run());
        return b;
    }

    private String safe(String s) { return s == null ? "" : s; }

    @FXML
    private void addLocal() {
        if (!(UserSession.isAdmin() || UserSession.isResponsableCentre())) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Vous n'avez pas le droit d'ajouter un local.");
            return;
        }
        openLocalForm(0);
    }

    private void openLocalForm(int idLocal) {
        if (layoutController == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "LayoutController non injecté. Vérifie loadLocaux().");
            return;
        }
        layoutController.openLocalForm(idLocal);
    }

    private void openReservationForm(int idLocal, String localNom) {
        if (!UserSession.isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Session", "Aucun utilisateur en session.");
            return;
        }
        if (layoutController == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "LayoutController non injecté. Vérifie loadLocaux().");
            return;
        }
        // ✅ IMPORTANT : ouverture via layout => onDone sera défini => retour après save/cancel
        layoutController.openReservationForm(idLocal, localNom);
    }

    private void deleteLocal(int idLocal) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce local ?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    localService.delete(idLocal);
                    refresh();
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BD", ex.getMessage());
                }
            }
        });
    }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ✅ Ouvrir la page Maps dans la scène (contentArea) — pas de Stage
    @FXML
    private void openMaps() {
        if (layoutController == null) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "LayoutController non injecté.");
            return;
        }
        layoutController.loadMaps();
    }

}
