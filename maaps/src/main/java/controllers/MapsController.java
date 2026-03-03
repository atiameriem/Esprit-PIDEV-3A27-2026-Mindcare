package controllers;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import models.LocalRelaxation;
import netscape.javascript.JSObject;
import services.LocalRelaxationService;

import java.sql.SQLException;
import java.util.List;

/**
 * "Maps" page using OpenStreetMap + Leaflet (via WebView).
 *
 * Features:
 *  - Displays all locaux as markers (geocoded in the embedded page).
 *  - Search a start address and compute the fastest route to a selected local (OSRM).
 *  - Hover a local in the ListView -> show route from the searched address to that local.
 *
 * NOTE: LocalRelaxation doesn't contain latitude/longitude in this project,
 * so coordinates are obtained by geocoding the local name and the searched address.
 */
public class MapsController {

    @FXML private TextField addressField;
    @FXML private Button btnSearch;
    @FXML private ListView<LocalRelaxation> locauxList;
    @FXML private WebView mapWeb;
    @FXML private Label infoLabel;

    private final LocalRelaxationService localService = new LocalRelaxationService();
    private WebEngine engine;

    @FXML
    public void initialize() {
        if (btnSearch != null) btnSearch.setOnAction(e -> search());
        if (addressField != null) addressField.setOnAction(e -> search());

        if (locauxList != null) {
            // sélection -> zoom + popup (via JS)
            locauxList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                if (n != null) selectLocal(n);
            });

            // ✅ hover -> route depuis l’adresse vers le local
            locauxList.setCellFactory(lv -> {
                ListCell<LocalRelaxation> cell = new ListCell<>() {
                    @Override
                    protected void updateItem(LocalRelaxation item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            // texte affiché dans la liste
                            setText(item.getNom() + " • " + item.getType());
                        }
                    }
                };

                cell.setOnMouseEntered(e -> {
                    LocalRelaxation l = cell.getItem();
                    if (l == null || engine == null) return;

                    // 🧠 Comme on n’a pas lat/lng en DB, on route via nom -> JS géocode le local
                    engine.executeScript("routeToLocalName(" + json(l.getNom()) + ")");
                });

                return cell;
            });
        }

        initMap();
    }

    private void initMap() {
        if (mapWeb == null) return;
        engine = mapWeb.getEngine();
        engine.load(getClass().getResource("/web/map.html").toExternalForm());

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("mapsBridge", new MapsBridge());

                loadLocaux();

                // default start point
                engine.executeScript("setStartFromQuery(" + json("Tunis") + ")");
            }
        });
    }

    private void loadLocaux() {
        if (locauxList == null) return;
        try {
            List<LocalRelaxation> list = localService.getAll();
            locauxList.getItems().setAll(list);
            if (infoLabel != null) infoLabel.setText("Locaux: " + list.size());

            if (engine != null) {
                // envoie au JS la liste des locaux (nom + type)
                engine.executeScript("setLocaux(JSON.parse(" + json(buildLocauxJson(list)) + "))");
            }
        } catch (SQLException ex) {
            if (infoLabel != null) infoLabel.setText("Erreur BD: " + ex.getMessage());
        }
    }

    private void search() {
        String q = addressField == null ? "" : addressField.getText();
        if (q == null || q.isBlank()) q = "Tunis";
        if (engine != null) {
            // JS : géocode l’adresse et mémorise userPos
            engine.executeScript("setStartFromQuery(" + json(q) + ")");
        }
    }

    private void selectLocal(LocalRelaxation local) {
        if (local == null || engine == null) return;

        // JS : focus marker + popup + (optionnel) route
        engine.executeScript("selectLocalByName(" + json(local.getNom()) + ")");
    }

    // Called from JS
    public class MapsBridge {

        // route summary
        public void onRouteInfo(String summary) {
            if (infoLabel != null && summary != null && !summary.isBlank()) {
                infoLabel.setText(summary);
            }
        }

        // nearest local detected in JS
        public void onNearestLocal(String localName) {
            if (localName == null || localName.isBlank() || locauxList == null) return;

            for (LocalRelaxation l : locauxList.getItems()) {
                if (l != null && localName.equalsIgnoreCase(l.getNom())) {
                    locauxList.getSelectionModel().select(l);
                    locauxList.scrollTo(l);
                    break;
                }
            }
        }
    }

    private String buildLocauxJson(List<LocalRelaxation> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            LocalRelaxation l = list.get(i);
            sb.append("{")
                    .append("\"id\":").append(l.getIdLocal()).append(",")
                    // ⚠️ map.html utilise "name" dans ton projet
                    .append("\"name\":").append(jsonRaw(l.getNom())).append(",")
                    .append("\"type\":").append(jsonRaw(l.getType()))
                    .append("}");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String json(String s) {
        return jsonRaw(s == null ? "" : s);
    }

    private String jsonRaw(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}