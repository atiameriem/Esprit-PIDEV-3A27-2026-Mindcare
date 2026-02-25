package services;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * ServiceRecaptcha
 *
 * Intégration Google reCAPTCHA (v2 checkbox) dans l'application JavaFX (WebView).
 *
 * Flow:
 * 1) L'app demande au backend local /captcha/start (Node) une session "state" et une URL captcha.html?state=...
 * 2) On ouvre une fenêtre JavaFX (Stage modal) avec WebView qui charge cette URL
 * 3) On interroge /captcha/status?state=... en boucle
 * 4) Si verified=true => callback(true) + fermeture de la fenêtre
 *    Sinon (Annuler/Timeout) => callback(false) + fermeture
 *
 * IMPORTANT:
 * - Le backend Node DOIT tourner (recaptcha-server -> npm start) et disposer de /captcha/start, /captcha/status, /captcha/verify
 * - Dans pom.xml, on force des options pour stabiliser WebView sur Windows (SW pipeline + désactivation MediaPlayer).
 */
public final class ServiceRecaptcha {

    /** Backend local fourni dans le projet : dossier /recaptcha-server (Node.js). */
    public static final String BASE_URL = "http://localhost:8085";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private ServiceRecaptcha() {}

    /**
     * Lance reCAPTCHA dans une fenêtre WebView (dans l'app) et retourne le résultat via callback.
     * done.accept(true) => Validé, done.accept(false) => Échoué/Annulé/Timeout.
     */
    public static void verify(Window owner, Consumer<Boolean> done) {
        Objects.requireNonNull(done, "done");

        CompletableFuture<StartResponse> startFuture = CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/captcha/start"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    throw new IOException("Backend /captcha/start failed: HTTP " + resp.statusCode());
                }
                return StartResponse.fromJson(resp.body());
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });

        startFuture.whenComplete((start, err) -> Platform.runLater(() -> {
            if (err != null) {
                showSimpleError(owner,
                        "Serveur reCAPTCHA introuvable",
                        "Assure-toi que le serveur Node tourne : recaptcha-server → npm start\n\nDétail: " + err.getCause());
                done.accept(false);
                return;
            }
            openWebViewAndPoll(owner, start, done);
        }));
    }

    private static void openWebViewAndPoll(Window owner, StartResponse start, Consumer<Boolean> done) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("Vérification reCAPTCHA");

        Label title = new Label("Vérification reCAPTCHA");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 800;");

        // NB: on échappe les guillemets pour éviter les erreurs de compilation
        Label subtitle = new Label("Cochez \"I'm not a robot\" pour continuer.");
        subtitle.setStyle("-fx-text-fill: #475569;");

        WebView webView = new WebView();
        webView.setPrefSize(520, 520);
        WebEngine engine = webView.getEngine();
        // Empêche JavaFX WebView d'ouvrir des popups / nouveaux onglets dans le navigateur.
        // (reCAPTCHA et certains liens utilisent window.open / target=_blank)
        engine.setCreatePopupHandler(popupFeatures -> engine);
        engine.load(start.url);

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(22, 22);

        Label status = new Label("En attente de validation…");
        status.setStyle("-fx-text-fill: #475569;");

        Button reload = new Button("Rafraîchir");
        Button cancel = new Button("Annuler");

        HBox actions = new HBox(10, pi, status, new HBox(10, reload, cancel));
        actions.setAlignment(Pos.CENTER_LEFT);

        // actions = [pi, status, rightBox] => index 2 (et non 3)
        HBox right = (HBox) actions.getChildren().get(2);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(right, javafx.scene.layout.Priority.ALWAYS);

        VBox root = new VBox(10, title, subtitle, webView, actions);
        root.setPadding(new Insets(12));

        stage.setScene(new Scene(root, 560, 650));

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "recaptcha-poll");
            t.setDaemon(true);
            return t;
        });

        final long startedAt = System.currentTimeMillis();
        final long timeoutMs = 2 * 60 * 1000L; // 2 minutes

        Runnable stop = () -> {
            try { scheduler.shutdownNow(); } catch (Exception ignored) {}
            if (stage.isShowing()) stage.close();
        };

        cancel.setOnAction(e -> {
            stop.run();
            done.accept(false);
        });

        reload.setOnAction(e -> engine.reload());

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (System.currentTimeMillis() - startedAt > timeoutMs) {
                    Platform.runLater(() -> {
                        status.setText("Timeout. Réessayez.");
                        stop.run();
                        done.accept(false);
                    });
                    return;
                }

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/captcha/status?state=" + start.state))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body().contains("\"verified\":true")) {
                    Platform.runLater(() -> {
                        status.setText("Validé ✅");
                        stop.run();
                        done.accept(true);
                    });
                }
            } catch (Exception ignored) {
                // continue polling
            }
        }, 0, 900, TimeUnit.MILLISECONDS);

        stage.show();
    }

    private static void showSimpleError(Window owner, String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        if (owner != null) a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static final class StartResponse {
        final String state;
        final String url;

        StartResponse(String state, String url) {
            this.state = state;
            this.url = url;
        }

        static StartResponse fromJson(String json) {
            String s = extract(json, "state");
            String u = extract(json, "url");
            if (s == null || u == null) throw new IllegalArgumentException("Invalid JSON: " + json);
            return new StartResponse(s, u);
        }

        private static String extract(String json, String key) {
            String needle = "\"" + key + "\"";
            int i = json.indexOf(needle);
            if (i < 0) return null;
            int colon = json.indexOf(':', i);
            if (colon < 0) return null;
            int q1 = json.indexOf('"', colon + 1);
            if (q1 < 0) return null;
            int q2 = json.indexOf('"', q1 + 1);
            if (q2 < 0) return null;
            return json.substring(q1 + 1, q2);
        }
    }
}
