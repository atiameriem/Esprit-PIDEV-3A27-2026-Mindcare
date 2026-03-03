package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.CheckInService;
import utils.UserSession;

public class CheckInController {

    @FXML private TextArea qrTextArea;
    @FXML private Button btnCheckIn;
    @FXML private Label resultLabel;

    private final CheckInService checkInService = new CheckInService();

    @FXML
    public void initialize() {
        if (resultLabel != null) resultLabel.setText("");
        if (btnCheckIn != null) {
            btnCheckIn.setOnAction(e -> doCheckIn());
        }
    }

    @FXML
    private void doCheckIn() {
        if (!UserSession.canManageReservationsStatus()) {
            setResult("Accès refusé: seul admin/responsable peut valider le check-in.", true);
            return;
        }
        String qr = qrTextArea == null ? "" : qrTextArea.getText();
        try {
            String msg = checkInService.checkInFromQr(qr);
            setResult(msg, false);
        } catch (Exception ex) {
            setResult("❌ " + ex.getMessage(), true);
        }
    }

    private void setResult(String msg, boolean isError) {
        if (resultLabel == null) return;
        resultLabel.setText(msg);
        resultLabel.setStyle(isError ? "-fx-text-fill:#C62828;" : "-fx-text-fill:#2E7D32;");
    }
}
