package utils;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Timer;
import java.util.TimerTask;

public class JitsiMeetWindow {

    private static final String C_BG = "#0D1117";
    private static final String C_CARD = "#161B22";
    private static final String C_TEAL = "#00C9A7";
    private static final String C_BLUE = "#4A90D9";
    private static final String C_WHITE = "#E6EDF3";
    private static final String C_MUTED = "#8B949E";
    private static final String C_RED = "#F85149";
    private static final String C_BORDER = "#21262D";

    private static CefApp sharedCefApp = null;
    private static Timer sessionTimer = null;
    private static int elapsedSeconds = 0;
    private static Label timerLabel;

    // ── Points d'entrée ───────────────────────────────────────────────────────

    public static void open(int appointmentId, String doctorName, String patientName) {
        SwingUtilities.invokeLater(() -> initAndShow(appointmentId, doctorName, patientName));
    }

    public static void openUrl(String url, String participantName) {
        SwingUtilities.invokeLater(() -> initAndShowWithUrl(url, participantName));
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private static void initAndShow(int appointmentId, String doctorName, String patientName) {
        try {
            if (sharedCefApp == null)
                sharedCefApp = buildCefApp();
            CefClient client = sharedCefApp.createClient();
            String url = buildJitsiUrl(appointmentId, doctorName);
            CefBrowser browser = client.createBrowser(url, false, false);
            Component ui = activateNatively(browser);
            Platform.runLater(() -> showWindow("Consultation #" + appointmentId,
                    doctorName, patientName, ui, browser));
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showError(e.getMessage()));
        }
    }

    private static void initAndShowWithUrl(String url, String participantName) {
        try {
            if (sharedCefApp == null)
                sharedCefApp = buildCefApp();
            CefClient client = sharedCefApp.createClient();
            CefBrowser browser = client.createBrowser(url, false, false);
            Component ui = activateNatively(browser);
            Platform.runLater(() -> showWindow("Séance MindCare",
                    participantName, "", ui, browser));
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showError(e.getMessage()));
        }
    }

    // ── Construction CefApp ───────────────────────────────────────────────────

    private static CefApp buildCefApp() throws Exception {
        CefAppBuilder builder = new CefAppBuilder();

        File installDir = new File(
                System.getProperty("user.home") + "/AppData/Local/MindCare/jcef116");
        installDir.mkdirs();
        builder.setInstallDir(installDir);

        builder.getCefSettings().windowless_rendering_enabled = false;
        builder.getCefSettings().command_line_args_disabled = false;

        String cacheDir = installDir.getAbsolutePath() + "/cache";
        new File(cacheDir).mkdirs();
        builder.getCefSettings().root_cache_path = cacheDir;
        builder.getCefSettings().cache_path = cacheDir;

        // ✅ Configuration des flags Chromium (compatible avec
        // MavenCefAppHandlerAdapter)
        builder.addJcefArgs("--use-fake-ui-for-media-stream");
        builder.addJcefArgs("--enable-media-stream");
        builder.addJcefArgs("--no-sandbox");
        builder.addJcefArgs("--disable-gpu-sandbox");
        builder.addJcefArgs("--disable-web-security");
        builder.addJcefArgs("--autoplay-policy=no-user-gesture-required");
        builder.addJcefArgs("--ignore-certificate-errors");

        builder.setAppHandler(new MavenCefAppHandlerAdapter() {
            @Override
            public void stateHasChanged(CefApp.CefAppState state) {
                System.out.println("[MindCare JCEF] " + state);
            }
        });

        return builder.build();
    }

    private static void writeChromiumPermissions(String cacheDir) {
        try {
            File defaultDir = new File(cacheDir + "/Default");
            defaultDir.mkdirs();
            File prefsFile = new File(defaultDir, "Preferences");

            String prefs = "{\n" +
                    "  \"profile\": {\n" +
                    "    \"default_content_setting_values\": {\n" +
                    "      \"media_stream_camera\": 1,\n" +
                    "      \"media_stream_mic\": 1\n" +
                    "    },\n" +
                    "    \"content_settings\": {\n" +
                    "      \"exceptions\": {\n" +
                    "        \"media_stream_camera\": {\n" +
                    "          \"https://meet.jit.si:443,*\": {\"setting\": 1},\n" +
                    "          \"*\": {\"setting\": 1}\n" +
                    "        },\n" +
                    "        \"media_stream_mic\": {\n" +
                    "          \"https://meet.jit.si:443,*\": {\"setting\": 1},\n" +
                    "          \"*\": {\"setting\": 1}\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            Files.write(prefsFile.toPath(), prefs.getBytes(StandardCharsets.UTF_8));
            System.out.println("[MindCare JCEF] Permissions Chromium écrites ✅");

        } catch (IOException e) {
            System.out.println("[MindCare JCEF] writeChromiumPermissions warning: " + e.getMessage());
        }
    }

    // ── Activation native (évite écran blanc) ─────────────────────────────────

    private static Component activateNatively(CefBrowser browser) throws InterruptedException {
        Component ui = browser.getUIComponent();
        Frame hidden = new Frame();
        hidden.setUndecorated(true);
        hidden.add(ui);
        hidden.setSize(1280, 780);
        hidden.setVisible(true);
        Thread.sleep(1500);
        hidden.remove(ui);
        hidden.dispose();
        return ui;
    }

    // ── URL Jitsi ─────────────────────────────────────────────────────────────

    private static String buildJitsiUrl(int appointmentId, String displayName) {
        try {
            String name = java.net.URLEncoder.encode(displayName, StandardCharsets.UTF_8.name());
            return "https://meet.jit.si/MindCare-Session-" + appointmentId
                    + "#config.prejoinPageEnabled=false"
                    + "&config.startWithAudioMuted=false"
                    + "&config.startWithVideoMuted=false"
                    + "&config.disableDeepLinking=true"
                    + "&config.resolution=720"
                    + "&userInfo.displayName=" + name;
        } catch (Exception e) {
            return "https://meet.jit.si/MindCare-Session-" + appointmentId
                    + "#config.prejoinPageEnabled=false";
        }
    }

    // ── Fenêtre JavaFX ────────────────────────────────────────────────────────

    private static void showWindow(String sessionTitle, String doctorName,
            String patientName, Component browserUI, CefBrowser browser) {

        Stage stage = new Stage();
        stage.setTitle("MindCare — " + sessionTitle);
        stage.initStyle(StageStyle.DECORATED);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + C_BG + ";");
        root.setTop(buildHeader(doctorName, patientName));

        SwingNode swingNode = new SwingNode();
        root.setCenter(swingNode);
        root.setBottom(buildFooter(stage, browser));

        Scene scene = new Scene(root, 1280, 780);
        scene.setFill(Color.web(C_BG));
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            stopTimer();
            SwingUtilities.invokeLater(() -> browser.close(true));
        });

        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(awtColor(C_BG));
            panel.add(browserUI, BorderLayout.CENTER);
            panel.revalidate();
            swingNode.setContent(panel);
            Platform.runLater(() -> {
                stage.show();
                startTimer();

                // ✅ Forcer repaint après 500ms pour éviter l'écran noir
                new Timer(true).schedule(new TimerTask() {
                    @Override
                    public void run() {
                        SwingUtilities.invokeLater(() -> {
                            panel.revalidate();
                            panel.repaint();
                        });
                    }
                }, 500);
            });
        });
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private static HBox buildHeader(String doctorName, String patientName) {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 24, 12, 24));
        header.setStyle(
                "-fx-background-color: " + C_CARD + ";" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-width: 0 0 1 0;");

        Label logo = new Label("🧠 MindCare");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        logo.setTextFill(Color.web(C_TEAL));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        timerLabel = new Label("00:00");
        timerLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 15));
        timerLabel.setTextFill(Color.web(C_TEAL));
        timerLabel.setStyle(
                "-fx-background-color: #0D2818;" +
                        "-fx-padding: 5 14 5 14;" +
                        "-fx-background-radius: 20;");

        Label live = new Label("⬤  EN DIRECT");
        live.setFont(Font.font("System", FontWeight.BOLD, 11));
        live.setTextFill(Color.web(C_RED));
        live.setStyle(
                "-fx-background-color: #2D1016;" +
                        "-fx-padding: 5 12 5 12;" +
                        "-fx-background-radius: 20;");

        header.getChildren().addAll(
                logo,
                lbl("·  Consultation vidéo", C_MUTED, false, 14),
                spacer,
                buildParticipants(doctorName, patientName),
                timerLabel,
                live);
        return header;
    }

    private static HBox buildParticipants(String doctor, String patient) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
                "-fx-background-color: " + C_BORDER + ";" +
                        "-fx-padding: 6 16 6 16;" +
                        "-fx-background-radius: 20;");
        if (doctor != null && !doctor.isBlank()) {
            box.getChildren().add(avatar(getInitials(doctor), C_BLUE));
            box.getChildren().add(lbl("Dr. " + doctor, C_WHITE, true, 13));
        }
        if (patient != null && !patient.isBlank()) {
            box.getChildren().add(lbl("↔", C_MUTED, false, 13));
            box.getChildren().add(avatar(getInitials(patient), C_TEAL));
            box.getChildren().add(lbl(patient, C_WHITE, true, 13));
        }
        return box;
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private static HBox buildFooter(Stage stage, CefBrowser browser) {
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(12, 24, 12, 24));
        footer.setStyle(
                "-fx-background-color: " + C_CARD + ";" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-width: 1 0 0 0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label btnFS = clickableLabel("⛶  Plein écran", C_BORDER, C_WHITE);
        btnFS.setOnMouseClicked(e -> stage.setFullScreen(!stage.isFullScreen()));

        Label btnEnd = clickableLabel("📵  Terminer", C_RED, "#FFFFFF");
        btnEnd.setOnMouseClicked(e -> {
            stopTimer();
            SwingUtilities.invokeLater(() -> browser.close(true));
            stage.close();
        });

        footer.getChildren().addAll(
                lbl("🔒  Consultation chiffrée · Propulsé par Jitsi Meet", C_MUTED, false, 12),
                spacer,
                btnFS,
                btnEnd);
        return footer;
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private static void startTimer() {
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

    private static void stopTimer() {
        if (sessionTimer != null) {
            sessionTimer.cancel();
            sessionTimer = null;
        }
    }

    // ── Utilitaires UI ────────────────────────────────────────────────────────

    private static StackPane avatar(String initials, String hex) {
        Circle c = new Circle(15);
        c.setFill(Color.web(hex));
        Label l = new Label(initials);
        l.setFont(Font.font("System", FontWeight.BOLD, 11));
        l.setTextFill(Color.WHITE);
        return new StackPane(c, l);
    }

    private static Label lbl(String text, String color, boolean bold, double size) {
        Label l = new Label(text);
        l.setFont(Font.font("System",
                bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(Color.web(color));
        return l;
    }

    private static Label clickableLabel(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 13));
        l.setTextFill(Color.web(fg));
        l.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-padding: 9 20 9 20;" +
                        "-fx-background-radius: 8;");
        l.setCursor(Cursor.HAND);
        l.setOnMouseEntered(e -> l.setOpacity(0.8));
        l.setOnMouseExited(e -> l.setOpacity(1.0));
        return l;
    }

    private static void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("MindCare — Erreur vidéo");
        a.setHeaderText("Impossible de démarrer la consultation");
        a.setContentText(msg != null ? msg : "Erreur inconnue");
        a.showAndWait();
    }

    private static java.awt.Color awtColor(String hex) {
        try {
            return java.awt.Color.decode(hex);
        } catch (Exception e) {
            return java.awt.Color.BLACK;
        }
    }

    private static String getInitials(String name) {
        if (name == null || name.isBlank())
            return "?";
        String[] p = name.trim().split("\\s+");
        return p.length == 1
                ? String.valueOf(p[0].charAt(0)).toUpperCase()
                : (String.valueOf(p[0].charAt(0))
                        + String.valueOf(p[p.length - 1].charAt(0))).toUpperCase();
    }
}