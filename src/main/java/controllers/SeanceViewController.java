package controllers;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import models.SeanceGroupe;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Dimension;
import java.awt.Panel;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class SeanceViewController {

    @FXML
    private Label seanceTitreLabel;
    @FXML
    private Label formationTitreLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label heureLabel;
    @FXML
    private Label participantLabel;
    @FXML
    private VBox preparationView;
    @FXML
    private VBox jitsiContainer;
    @FXML
    private StackPane browserContainer;
    @FXML
    private Label timerLabel;
    @FXML
    private Button rejoindreBtn;

    private String jitsiUrl;
    private String userName;
    private Runnable onRetour;
    private CefApp cefApp;
    private CefBrowser browser;
    private Timer sessionTimer;
    private int elapsedSeconds = 0;

    public void initialize(SeanceGroupe seance, String url, String user, Runnable retourCallback) {
        this.jitsiUrl = url;
        this.userName = user;
        this.onRetour = retourCallback;

        seanceTitreLabel.setText(seance.getTitre());
        if (seance.getTitreFormation() != null)
            formationTitreLabel.setText("Formation : " + seance.getTitreFormation());
        if (seance.getDateHeure() != null) {
            dateLabel.setText(seance.getDateHeure().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            heureLabel.setText(seance.getDateHeure().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        participantLabel.setText(user);

        // 🚀 Ouverture automatique dès l'initialisation
        Platform.runLater(this::rejoindreSeance);
    }

    @FXML
    private void rejoindreSeance() {
        preparationView.setVisible(false);
        preparationView.setManaged(false);
        jitsiContainer.setVisible(true);

        // Récupérer la Stage JavaFX actuelle
        Stage currentStage = (Stage) preparationView.getScene().getWindow();

        SwingUtilities.invokeLater(() -> {
            try {
                // ✅ Gestion robuste du Singleton JCEF
                if (CefApp.getState() == CefApp.CefAppState.NONE) {
                    CefAppBuilder builder = new CefAppBuilder();
                    File installDir = new File(System.getProperty("user.home") + "/AppData/Local/MindCare/jcef116");
                    installDir.mkdirs();
                    builder.setInstallDir(installDir);
                    builder.getCefSettings().windowless_rendering_enabled = false;
                    builder.getCefSettings().command_line_args_disabled = false;

                    String cacheDir = installDir.getAbsolutePath() + "/cache";
                    new File(cacheDir).mkdirs();
                    builder.getCefSettings().root_cache_path = cacheDir;
                    builder.getCefSettings().cache_path = cacheDir;

                    builder.addJcefArgs("--use-fake-ui-for-media-stream", "--enable-media-stream", "--no-sandbox",
                            "--disable-gpu-sandbox", "--disable-web-security",
                            "--autoplay-policy=no-user-gesture-required",
                            "--ignore-certificate-errors");

                    builder.setAppHandler(new MavenCefAppHandlerAdapter() {
                        @Override
                        public void stateHasChanged(CefApp.CefAppState state) {
                            System.out.println("[MindCare JCEF] " + state);
                        }
                    });

                    cefApp = builder.build();
                } else if (CefApp.getState() == CefApp.CefAppState.TERMINATED) {
                    // Si le moteur est mort, on ne peut pas le relancer sans redémarrer le process
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur Jitsi");
                        alert.setHeaderText("Le moteur vidéo a été arrêté.");
                        alert.setContentText("Veuillez redémarrer l'application MindCare pour lancer un nouvel appel.");
                        alert.showAndWait();
                    });
                    return;
                } else {
                    cefApp = CefApp.getInstance();
                }

                // 🚀 Config Jitsi "Style API"
                String baseUri = jitsiUrl;
                if (baseUri.contains("#"))
                    baseUri = baseUri.substring(0, baseUri.indexOf("#"));

                String safeName = (userName != null) ? "\"" + userName.replace("\"", "") + "\"" : "\"Participant\"";
                String encodedName = java.net.URLEncoder.encode(safeName, "UTF-8");

                String apiJitsiUrl = baseUri + "#userInfo.displayName=" + encodedName
                        + "&config.prejoinPageEnabled=false"
                        + "&config.startWithAudioMuted=false"
                        + "&config.startWithVideoMuted=false"
                        + "&interfaceConfig.TOOLBAR_BUTTONS=[]"
                        + "&interfaceConfig.SHOW_JITSI_WATERMARK=false";

                CefClient client = cefApp.createClient();
                browser = client.createBrowser(apiJitsiUrl, false, false);
                Component browserUI = browser.getUIComponent();

                // Créer une vraie fenêtre AWT qui contient le browser JCEF
                // ET le toolbar JavaFX via JFXPanel
                Frame awtFrame = new Frame();
                awtFrame.setLayout(new BorderLayout());
                awtFrame.setTitle("MindCare — Séance");
                awtFrame.setSize(1280, 780);
                awtFrame.setUndecorated(false);

                // Toolbar en haut via JFXPanel (JavaFX dans Swing cette fois)
                javafx.embed.swing.JFXPanel toolbarPanel = new javafx.embed.swing.JFXPanel();
                toolbarPanel.setPreferredSize(new Dimension(1280, 50));

                // Browser en dessous
                Panel browserPanel = new Panel(new BorderLayout());
                browserPanel.add(browserUI, BorderLayout.CENTER);

                awtFrame.add(toolbarPanel, BorderLayout.NORTH);
                awtFrame.add(browserPanel, BorderLayout.CENTER);
                awtFrame.setVisible(true);

                // Forcer le rendu initial
                browserUI.revalidate();
                browserUI.repaint();

                // Construire le toolbar JavaFX dans le JFXPanel
                Platform.runLater(() -> {
                    javafx.scene.layout.HBox toolbar = new javafx.scene.layout.HBox(12);
                    toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    toolbar.setStyle("-fx-background-color: #161B22; -fx-padding: 8 20;");

                    Label logo = new Label("🧠 MindCare  ·  Séance en cours");
                    logo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #00C9A7;");

                    javafx.scene.layout.Pane spacer = new javafx.scene.layout.Pane();
                    javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                    timerLabel = new Label("00:00");
                    timerLabel.setStyle(
                            "-fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #00C9A7; -fx-background-color: #0D2818; -fx-padding: 4 14; -fx-background-radius: 20;");

                    Label live = new Label("⬤  EN DIRECT");
                    live.setStyle(
                            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #F85149; -fx-background-color: #2D1016; -fx-padding: 4 12; -fx-background-radius: 20;");

                    Button fullscreenBtn = new Button("⛶  Plein écran");
                    fullscreenBtn.setStyle(
                            "-fx-background-color: #21262D; -fx-text-fill: #E6EDF3; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 16; -fx-cursor: hand;");
                    fullscreenBtn.setOnAction(e -> SwingUtilities.invokeLater(() -> {
                        java.awt.GraphicsDevice gd = java.awt.GraphicsEnvironment
                                .getLocalGraphicsEnvironment().getDefaultScreenDevice();
                        if (gd.getFullScreenWindow() == null) {
                            gd.setFullScreenWindow(awtFrame);
                        } else {
                            gd.setFullScreenWindow(null);
                        }
                    }));

                    Button terminerBtn = new Button("📵  Terminer");
                    terminerBtn.setStyle(
                            "-fx-background-color: #F85149; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 16; -fx-cursor: hand;");
                    terminerBtn.setOnAction(e -> {
                        stopTimer();
                        SwingUtilities.invokeLater(() -> {
                            if (browser != null) {
                                browser.close(true);
                                browser = null;
                            }
                            awtFrame.dispose();

                            Platform.runLater(() -> {
                                if (currentStage != null)
                                    currentStage.show();
                                if (onRetour != null)
                                    onRetour.run();
                            });
                        });
                    });

                    toolbar.getChildren().addAll(logo, spacer, timerLabel, live, fullscreenBtn, terminerBtn);
                    toolbarPanel.setScene(new javafx.scene.Scene(toolbar, 1280, 50));
                    startTimer();
                });

                // Cacher la stage JavaFX originale
                Platform.runLater(() -> {
                    if (currentStage != null)
                        currentStage.hide();
                });

                // Quand la fenêtre AWT est fermée, réafficher la stage JavaFX
                awtFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        stopTimer();
                        SwingUtilities.invokeLater(() -> {
                            if (browser != null) {
                                browser.close(true);
                                browser = null;
                            }
                            awtFrame.dispose();
                        });
                        Platform.runLater(() -> {
                            if (currentStage != null)
                                currentStage.show();
                            if (onRetour != null)
                                onRetour.run();
                        });
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    preparationView.setVisible(true);
                    preparationView.setManaged(true);
                    jitsiContainer.setVisible(false);
                    if (currentStage != null)
                        currentStage.show();
                });
            }
        });
    }

    @FXML
    private void terminerSeance() {
        stopTimer();
        closeBrowser();
        if (onRetour != null)
            onRetour.run();
    }

    @FXML
    private void toggleFullscreen() {
        if (jitsiContainer.getScene() != null) {
            Stage stage = (Stage) jitsiContainer.getScene().getWindow();
            stage.setFullScreen(!stage.isFullScreen());
        }
    }

    @FXML
    private void retourSeances() {
        stopTimer();
        closeBrowser();
        if (onRetour != null)
            onRetour.run();
    }

    private void closeBrowser() {
        CefBrowser b = browser;
        browser = null;
        if (b != null) {
            SwingUtilities.invokeLater(() -> b.close(true));
        }

        Platform.runLater(() -> {
            browserContainer.getChildren().clear();
        });
    }

    private void startTimer() {
        elapsedSeconds = 0;
        sessionTimer = new Timer(true);
        sessionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                elapsedSeconds++;
                String t = String.format("%02d:%02d",
                        elapsedSeconds / 60, elapsedSeconds % 60);
                Platform.runLater(() -> {
                    if (timerLabel != null)
                        timerLabel.setText(t);
                });
            }
        }, 1000, 1000);
    }

    private void stopTimer() {
        if (sessionTimer != null) {
            sessionTimer.cancel();
            sessionTimer = null;
        }
    }
}