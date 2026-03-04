package controllers;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;

public class GoogleLoginWebViewController {

    @FXML
    private WebView webView;
    @FXML
    private ProgressIndicator progressIndicator;

    private String authorizationCode = null;
    private static final String REDIRECT_URI_PREFIX = "http://localhost:8888/Callback";

    public void loadUrl(String url) {
        WebEngine engine = webView.getEngine();

        // Pour contourner le blocage "Navigateur non sécurisé" de Google :
        // Google bloque les WebViews intégrées. Se faire passer pour Firefox permet 
        // généralement de contourner cette restriction de sécurité sur JavaFX.
        engine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0");

        engine.getLoadWorker().runningProperty().addListener((obs, oldVal, newVal) -> {
            progressIndicator.setVisible(newVal);
        });

        engine.locationProperty().addListener((obs, oldLocation, newLocation) -> {
            System.out.println("WebView Navigated to: " + newLocation);

            // On surveille si l'URL contient le code d'autorisation (après redirection vers
            // localhost)
            if (newLocation.startsWith(REDIRECT_URI_PREFIX) && newLocation.contains("code=")) {
                String code = extractCode(newLocation);
                if (code != null) {
                    this.authorizationCode = code;
                    closeWindow();
                }
            }
        });

        engine.load(url);
    }

    private String extractCode(String url) {
        try {
            String query = url.split("\\?")[1];
            for (String param : query.split("&")) {
                if (param.startsWith("code=")) {
                    return param.split("=")[1];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    private void closeWindow() {
        Stage stage = (Stage) webView.getScene().getWindow();
        stage.close();
    }
}
