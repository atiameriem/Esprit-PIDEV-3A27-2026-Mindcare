package controllers;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import services.CalendarService;
import services.ReservationService;
import utils.IcsUtil;
import utils.UserSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Calendar UI using FullCalendar in a WebView.
 * Events are loaded from the local DB table calendar_event (via CalendarService).
 */
public class CalendarController {

    @FXML private DatePicker datePicker;
    @FXML private Button btnRefresh;
    @FXML private Button btnExportIcs;
    @FXML private Button btnCancelReservation;
    @FXML private Label infoLabel;
    @FXML private WebView calendarWeb;

    private final CalendarService calendarService = new CalendarService();
    private final ReservationService reservationService = new ReservationService();

    private WebEngine engine;
    private Integer selectedReservationId = null;

    @FXML
    public void initialize() {

        // Date picker + refresh
        if (datePicker != null) {
            datePicker.setValue(LocalDate.now());
            datePicker.valueProperty().addListener((obs, o, n) -> goToDate(n));
        }
        if (btnRefresh != null) btnRefresh.setOnAction(e -> goToDate(datePicker.getValue()));

        // Export ICS
        if (btnExportIcs != null) btnExportIcs.setOnAction(e -> exportSelected());

        // Cancel reservation (only responsable/admin)
        if (btnCancelReservation != null) {
            boolean canCancel = UserSession.canManageReservationsStatus();
            btnCancelReservation.setVisible(canCancel);
            btnCancelReservation.setManaged(canCancel);
            btnCancelReservation.setOnAction(e -> cancelSelected());
        }

        // Init web calendar
        initWebCalendar();
    }

    private void initWebCalendar() {
        if (calendarWeb == null) return;

        engine = calendarWeb.getEngine();
        engine.load(getClass().getResource("/web/calendar.html").toExternalForm());

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("calendarBridge", new CalendarBridge());
                goToDate(datePicker != null ? datePicker.getValue() : LocalDate.now());
            }
        });
    }

    private void goToDate(LocalDate d) {
        if (d == null || engine == null) return;
        String iso = d.format(DateTimeFormatter.ISO_DATE);
        Platform.runLater(() -> engine.executeScript("goToDate('" + iso + "')"));
    }

    // Bridge JS <-> Java (calendar.html must call calendarBridge.getEventsJson(...) etc.)
    public class CalendarBridge {

        public String getEventsJson(String startStr, String endStr) {
            try {
                // FullCalendar passes ISO strings. We use the date part.
                LocalDate start = LocalDate.parse(startStr.substring(0, 10));
                LocalDate end = LocalDate.parse(endStr.substring(0, 10));
                LocalDateTime s = start.atStartOfDay();
                LocalDateTime e = end.atStartOfDay();

                List<CalendarService.CalendarEventDTO> list = calendarService.listForRange(s, e);
                Platform.runLater(() -> {
                    if (infoLabel != null) infoLabel.setText("Événements: " + list.size());
                });

                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (int i = 0; i < list.size(); i++) {
                    var ev = list.get(i);
                    sb.append("{")
                            .append("\"reservationId\":").append(ev.reservationId).append(",")
                            .append("\"title\":").append(json(ev.title)).append(",")
                            .append("\"start\":").append(json(ev.start.toString())).append(",")
                            .append("\"end\":").append(json(ev.end.toString())).append(",")
                            .append("\"status\":").append(json(ev.status))
                            .append("}");
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                return sb.toString();

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (infoLabel != null) infoLabel.setText("Erreur: " + ex.getMessage());
                });
                return "[]";
            }
        }

        // called when user clicks an event in calendar.html
        public void onSelectReservation(int reservationId) {
            selectedReservationId = reservationId;
        }
    }

    private void exportSelected() {
        if (selectedReservationId == null) {
            showAlert(Alert.AlertType.WARNING, "Calendrier", "Cliquez sur un événement dans le calendrier.");
            return;
        }

        try {
            LocalDate d = (datePicker == null || datePicker.getValue() == null) ? LocalDate.now() : datePicker.getValue();
            LocalDateTime s = d.minusMonths(1).atStartOfDay();
            LocalDateTime e = d.plusMonths(2).atStartOfDay();

            var ev = calendarService.listForRange(s, e).stream()
                    .filter(x -> x.reservationId == selectedReservationId)
                    .findFirst().orElse(null);

            if (ev == null) {
                showAlert(Alert.AlertType.WARNING, "Calendrier", "Événement introuvable.");
                return;
            }

            // Reminder 3 days before
            int reminderMin = 3 * 24 * 60;
            String ics = IcsUtil.buildIcs(ev.title, "Réservation #" + ev.reservationId, ev.start, ev.end, reminderMin);

            Path outDir = Paths.get("exports", "calendar");
            Files.createDirectories(outDir);
            Path out = outDir.resolve("reservation_" + ev.reservationId + ".ics");
            Files.writeString(out, ics);

            showAlert(Alert.AlertType.INFORMATION, "ICS exporté", "Fichier créé: " + out.toAbsolutePath());

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur export", ex.getMessage());
        }
    }

    private void cancelSelected() {
        if (selectedReservationId == null) {
            showAlert(Alert.AlertType.WARNING, "Calendrier", "Cliquez sur un événement dans le calendrier.");
            return;
        }
        if (!UserSession.canManageReservationsStatus()) {
            showAlert(Alert.AlertType.ERROR, "Accès refusé", "Seul admin/responsable peut annuler.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Annuler la réservation #" + selectedReservationId + " ?",
                ButtonType.OK, ButtonType.CANCEL);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    reservationService.updateStatus(selectedReservationId, "Annuler");
                    calendarService.cancelForReservation(selectedReservationId);
                    goToDate(datePicker != null ? datePicker.getValue() : LocalDate.now());
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                }
            }
        });
    }

    private String json(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}