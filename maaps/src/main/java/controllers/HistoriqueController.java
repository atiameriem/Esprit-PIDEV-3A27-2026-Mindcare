package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.geometry.Insets;

import services.HistoryService;
import utils.UserSession;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoriqueController {

    @FXML private TilePane historyContainer;
    @FXML private TextField searchField;

    private List<String> allLines = Collections.emptyList();

    @FXML
    public void initialize() {
        // sécurité: réservé responsable/admin
        if (!(UserSession.isResponsableCentre() || UserSession.isAdmin())) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Historique réservé au responsable du centre.");
            if (historyContainer != null) {
                historyContainer.getChildren().setAll(new Label("Accès refusé."));
            }
            return;
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> render());
        }

        load();
        render();
    }

    private void load() {
        try {
            var path = HistoryService.getLogPath();
            if (Files.exists(path)) {
                allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            } else {
                allLines = new ArrayList<>();
            }
        } catch (Exception e) {
            allLines = new ArrayList<>();
            showAlert(Alert.AlertType.ERROR, "Historique", e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        load();
        render();
    }

    private void render() {
        if (historyContainer == null) return;
        historyContainer.getChildren().clear();

        String q = (searchField == null || searchField.getText() == null) ? "" : searchField.getText().trim().toLowerCase();

        List<String> lines = new ArrayList<>(allLines);
        // afficher les plus récents en haut
        Collections.reverse(lines);

        int shown = 0;
        for (String line : lines) {
            if (!q.isEmpty() && (line == null || !line.toLowerCase().contains(q))) continue;

            VBox card = new VBox(6);
            card.getStyleClass().addAll("modern-card");
            card.setPadding(new Insets(12));
            card.setPrefWidth(420);

            Label l = new Label(line);
            l.setWrapText(true);
            l.getStyleClass().add("muted");

            card.getChildren().add(l);
            historyContainer.getChildren().add(card);
            shown++;

            // éviter une page trop lourde
            if (shown >= 200) break;
        }

        if (shown == 0) {
            historyContainer.getChildren().add(new Label("Aucune entrée d'historique."));
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
