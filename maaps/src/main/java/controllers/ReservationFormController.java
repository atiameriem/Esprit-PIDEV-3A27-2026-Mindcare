package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import models.Reservation;
import services.AiSessionRecommenderService;
import services.ReservationService;
import utils.UserSession;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationFormController {

    @FXML private Label titleLabel;
    @FXML private Label localLabel;
    @FXML private DatePicker datePicker;

    // ✅ Heures : TextField + bouton ⏰
    @FXML private TextField heureDebutField;
    @FXML private TextField heureFinField;
    @FXML private Button btnHeureDebut;
    @FXML private Button btnHeureFin;

    // ✅ Type session ComboBox
    @FXML private ComboBox<String> typeSessionCombo;

    @FXML private TextField motifField;
    @FXML private Label errorLabel;

    // "AI" suggestion
    @FXML private Label aiSuggestionLabel;
    @FXML private Button applyAiButton;
    @FXML private TextArea aiWhyArea;

    private ContextMenu menuHeureDebut;
    private ContextMenu menuHeureFin;

    private final ReservationService reservationService = new ReservationService();
    private final AiSessionRecommenderService aiService = new AiSessionRecommenderService();

    private AiSessionRecommenderService.RecommendationResult latestAi;

    private int idLocal;
    private String localNom;
    private Reservation editing;
    private Runnable onDone;

    public void setOnDone(Runnable onDone) { this.onDone = onDone; }

    public void setLocal(int idLocal, String localNom) {
        this.idLocal = idLocal;
        this.localNom = localNom;

        if (localLabel != null) localLabel.setText(localNom == null ? "-" : localNom);
        if (titleLabel != null) titleLabel.setText("➕ Ajouter une réservation");

        refreshAiSuggestion();
    }

    public void setEditingReservation(Reservation r) {
        this.editing = r;
        this.idLocal = r.getIdLocal();
        this.localNom = r.getLocalNom();

        if (titleLabel != null) titleLabel.setText("✏️ Modifier une réservation");
        if (localLabel != null) localLabel.setText(localNom == null ? "-" : localNom);

        if (datePicker != null) datePicker.setValue(r.getDateReservation());

        if (heureDebutField != null && r.getHeureDebut() != null) {
            // LocalTime.toString() peut donner HH:MM ou HH:MM:SS -> on garde HH:MM
            String s = r.getHeureDebut().toString();
            heureDebutField.setText(s.length() >= 5 ? s.substring(0, 5) : s);
        }

        if (heureFinField != null && r.getHeureFin() != null) {
            String s = r.getHeureFin().toString();
            heureFinField.setText(s.length() >= 5 ? s.substring(0, 5) : s);
        }

        if (typeSessionCombo != null) {
            typeSessionCombo.getSelectionModel().select(r.getTypeSession() == null ? null : r.getTypeSession());
        }

        if (motifField != null) motifField.setText(r.getMotif() == null ? "" : r.getMotif());

        refreshAiSuggestion();
    }

    @FXML
    private void initialize() {
        // ✅ Menus popup (liste heures)
        menuHeureDebut = createTimeMenu(heureDebutField);
        menuHeureFin = createTimeMenu(heureFinField);

        // ✅ Valeurs par défaut
        if (heureDebutField != null) heureDebutField.setText("08:00");
        if (heureFinField != null) heureFinField.setText("08:30");

        // ✅ Type session
        if (typeSessionCombo != null) {
            typeSessionCombo.getItems().setAll(
                    "THERAPIE_INDIVIDUELLE",
                    "MEDITATION",
                    "YOGA",
                    "THERAPIE_GROUPE",
                    "REPETITION",
                    "RESPIRATION",
                    "AUTRE"
            );
            typeSessionCombo.getSelectionModel().selectFirst();
        }

        // AI: listeners
        if (applyAiButton != null) applyAiButton.setDisable(true);

        if (motifField != null) {
            motifField.textProperty().addListener((obs, o, n) -> refreshAiSuggestion());
        }
        if (typeSessionCombo != null) {
            typeSessionCombo.valueProperty().addListener((obs, o, n) -> refreshAiSuggestion());
        }

        refreshAiSuggestion();

        // Bonus : si on change heure début, on propose automatiquement fin = +30min (optionnel)
        if (heureDebutField != null) {
            heureDebutField.textProperty().addListener((obs, oldV, newV) -> {
                if (newV != null && newV.matches("^\\d{2}:\\d{2}$")) {
                    try {
                        LocalTime t = LocalTime.parse(newV + ":00");
                        LocalTime fin = t.plusMinutes(30);
                        if (heureFinField != null) heureFinField.setText(fin.toString().substring(0,5));
                    } catch (Exception ignored) { }
                }
            });
        }
    }

    private void refreshAiSuggestion() {
        try {
            latestAi = null;

            if (aiSuggestionLabel != null) aiSuggestionLabel.setText("-");
            if (applyAiButton != null) applyAiButton.setDisable(true);
            if (aiWhyArea != null) aiWhyArea.setText("");

            if (!UserSession.isLoggedIn() || UserSession.getCurrentUser() == null) return;

            // suggestion uniquement utile pour patient
            if (!UserSession.isPatient()) return;

            String motif = (motifField == null) ? "" : motifField.getText();
            latestAi = aiService.analyzeAndRecommend(UserSession.getCurrentUser().getIdUsers(), motif);
            if (latestAi == null || latestAi.recommended == null) {
                if (aiSuggestionLabel != null) aiSuggestionLabel.setText("Aucun quiz trouvé pour ce patient.");
                return;
            }

            String current = (typeSessionCombo == null) ? null : typeSessionCombo.getValue();
            boolean different = current == null || !current.equalsIgnoreCase(latestAi.recommended);

            if (aiSuggestionLabel != null) {
                String alt = (latestAi.alternative == null) ? "" : (" | Alt: " + latestAi.alternative);
                aiSuggestionLabel.setText(
                        "Score analysé: " + latestAi.scoreUsed + " (" + latestAi.severity + ") | Motif: " + (motif == null ? "" : motif) +
                                " → Recommandé: " + latestAi.recommended + alt +
                                " | Confiance: " + (int) Math.round(latestAi.confidence * 100) + "%"
                );
            }
            if (aiWhyArea != null) aiWhyArea.setText(latestAi.why == null ? "" : latestAi.why);
            if (applyAiButton != null) applyAiButton.setDisable(!different);

        } catch (Exception e) {
            // Ne pas bloquer l'UI si une erreur BD arrive
            if (aiSuggestionLabel != null) aiSuggestionLabel.setText("Suggestion indisponible (" + e.getMessage() + ")");
            if (applyAiButton != null) applyAiButton.setDisable(true);
        }
    }

    @FXML
    private void applyAiRecommendation() {
        if (latestAi != null && latestAi.recommended != null && typeSessionCombo != null) {
            typeSessionCombo.getSelectionModel().select(latestAi.recommended);
            refreshAiSuggestion();
        }
    }

    // ✅ Boutons ⏰
    @FXML
    private void showHeureDebutMenu() {
        if (menuHeureDebut != null) {
            if (menuHeureDebut.isShowing()) menuHeureDebut.hide();
            menuHeureDebut.show(btnHeureDebut, Side.BOTTOM, 0, 0);
        }
    }

    @FXML
    private void showHeureFinMenu() {
        if (menuHeureFin != null) {
            if (menuHeureFin.isShowing()) menuHeureFin.hide();
            menuHeureFin.show(btnHeureFin, Side.BOTTOM, 0, 0);
        }
    }

    @FXML
    private void save() {
        if (errorLabel != null) errorLabel.setText("");

        try {
            if (!UserSession.isLoggedIn())
                throw new IllegalArgumentException("Aucun utilisateur en session.");

            if (idLocal <= 0)
                throw new IllegalArgumentException("Local non sélectionné.");

            LocalDate d = (datePicker == null) ? null : datePicker.getValue();
            if (d == null)
                throw new IllegalArgumentException("Veuillez choisir une date.");

            LocalTime hd = parseTime(heureDebutField == null ? null : heureDebutField.getText());
            LocalTime hf = parseTime(heureFinField == null ? null : heureFinField.getText());

            if (!hf.isAfter(hd))
                throw new IllegalArgumentException("Heure fin doit être après heure début.");

            String ts = (typeSessionCombo == null) ? null : typeSessionCombo.getValue();
            if (ts == null || ts.isBlank())
                throw new IllegalArgumentException("Veuillez choisir un type de session.");

            boolean isEdit = (editing != null && editing.getIdReservation() > 0);
            if (!isEdit) editing = new Reservation();

            editing.setIdLocal(idLocal);
            editing.setIdUtilisateur(UserSession.getCurrentUser().getIdUsers());
            editing.setDateReservation(d);
            editing.setHeureDebut(hd);
            editing.setHeureFin(hf);
            editing.setTypeSession(ts);
            editing.setMotif(motifField == null ? "" : motifField.getText().trim());

            // FK vers le dernier quiz du patient (utilisé par la recommandation)
            if (UserSession.isPatient()) {
                try {
                    AiSessionRecommenderService.RecommendationResult r = aiService.analyzeAndRecommend(UserSession.getCurrentUser().getIdUsers(),
                            motifField == null ? "" : motifField.getText());
                    editing.setIdHistoriqueUtilise(r == null ? null : r.idHistoriqueUtilise);
                } catch (Exception ignored) {
                    editing.setIdHistoriqueUtilise(null);
                }
            }

            // PATIENT
            if (UserSession.isPatient()) {
                editing.setStatut("EN_ATTENTE");
                editing.setIdResponsableCentre(null);
            } else {
                if (editing.getStatut() == null || editing.getStatut().isBlank())
                    editing.setStatut("EN_ATTENTE");
                if (editing.getIdResponsableCentre() == null)
                    editing.setIdResponsableCentre(UserSession.getCurrentUser().getIdUsers());
            }

            if (isEdit) reservationService.update(editing);
            else reservationService.add(editing);

            showPopup(isEdit ? "Modification avec succès" : "Ajout avec succès");

            if (onDone != null) onDone.run();

        } catch (SQLException ex) {
            if (errorLabel != null) errorLabel.setText("Erreur BD: " + ex.getMessage());
        } catch (Exception ex) {
            if (errorLabel != null) errorLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void cancel() {
        clearFields();
        if (onDone != null) onDone.run();
    }

    private void showPopup(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void clearFields() {
        if (datePicker != null) datePicker.setValue(null);
        if (heureDebutField != null) heureDebutField.clear();
        if (heureFinField != null) heureFinField.clear();
        if (typeSessionCombo != null) typeSessionCombo.getSelectionModel().clearSelection();
        if (motifField != null) motifField.clear();
        if (errorLabel != null) errorLabel.setText("");
    }

    private LocalTime parseTime(String s) {
        if (s == null) throw new IllegalArgumentException("Veuillez choisir une heure.");
        String t = s.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Veuillez choisir une heure.");

        // accepter HH:MM
        if (t.matches("^\\d{1,2}:\\d{2}$")) {
            if (t.length() == 4) t = "0" + t; // 8:00 -> 08:00
            t = t + ":00"; // -> HH:MM:SS
        }

        try {
            return LocalTime.parse(t);
        } catch (Exception e) {
            throw new IllegalArgumentException("Format heure invalide (HH:MM).");
        }
    }

    // ✅ Popup menu heures
    private ContextMenu createTimeMenu(TextField targetField) {
        ContextMenu menu = new ContextMenu();

        for (String t : generateTimes("08:00", "18:00", 30)) {
            // ✅ interdire la pause 12:00-13:00
            if (t.compareTo("12:00") >= 0 && t.compareTo("13:00") < 0) continue;
            MenuItem item = new MenuItem(t);
            item.setOnAction(e -> targetField.setText(t));
            menu.getItems().add(item);
        }
        return menu;
    }

    private List<String> generateTimes(String start, String end, int stepMinutes) {
        List<String> times = new ArrayList<>();

        int startH = Integer.parseInt(start.substring(0, 2));
        int startM = Integer.parseInt(start.substring(3, 5));
        int endH = Integer.parseInt(end.substring(0, 2));
        int endM = Integer.parseInt(end.substring(3, 5));

        int startTotal = startH * 60 + startM;
        int endTotal = endH * 60 + endM;

        for (int m = startTotal; m <= endTotal; m += stepMinutes) {
            int hh = m / 60;
            int mm = m % 60;
            times.add(String.format("%02d:%02d", hh, mm));
        }
        return times;
    }
}