package utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.embed.swing.JFXPanel;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import services.SeanceGroupeServiceF;

import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class JitsiMeetWindowF {

    private static CefApp sharedCefApp = null;
    private static Timer sessionTimer = null;
    private static int elapsedSeconds = 0;
    private static Label timerLabel;
    private static boolean isCefInitialized = false;
    private static boolean isOpen = false;
    private static int currentSeanceId = -1;
    private static int currentUserId = -1;
    private static final SeanceGroupeServiceF seanceService = new SeanceGroupeServiceF();
    private static javafx.stage.Stage mainAppStage;

    // ── Variables pour les participants ──────────────────────────────────────
    private static volatile String currentHost = "En attente...";
    private static final java.util.List<String> participants = java.util.Collections
            .synchronizedList(new java.util.ArrayList<>());
    private static javafx.scene.control.Label hostLabel;
    private static javafx.scene.control.Label countLabel;
    private static javafx.scene.layout.VBox participantListBox;

    // ── Points d'entrée ───────────────────────────────────────────────────────

    public static void open(int appointmentId, String doctorName, String patientName) {
        String url = buildJitsiUrl(appointmentId, doctorName);
        openUrl(url, doctorName, "Séance en direct", true);
    }

    public static void openUrl(String url, String participantName) {
        openUrl(url, participantName, "Vidéo MindCare", false);
    }

    // ── Initialisation CEF ────────────────────────────────────────────────────

    // ── Initialisation CEF ────────────────────────────────────────────────────
    private static synchronized void prepareCef() throws Exception {
        if (isCefInitialized && sharedCefApp != null)
            return;

        CefApp.CefAppState state = CefApp.getState();
        System.out.println("[MindCare JCEF] Tentative d'initialisation... État actuel : " + state);

        if (state == CefApp.CefAppState.INITIALIZATION_FAILED) {
            String errorMsg = "Le moteur vidéo JCEF a échoué. Veuillez fermer l'application et supprimer le dossier .mindcare dans "
                    + System.getProperty("user.home");
            System.err.println("[MindCare JCEF] " + errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (state == CefApp.CefAppState.TERMINATED) {
            throw new RuntimeException("Le moteur vidéo a été arrêté.");
        }

        if (state == CefApp.CefAppState.NONE) {
            System.out.println("[MindCare JCEF] Démarrage de CefApp...");
            sharedCefApp = buildCefApp();
        } else {
            sharedCefApp = CefApp.getInstance();
        }

        if (sharedCefApp == null) {
            throw new RuntimeException("L'instance CefApp est null après initialisation.");
        }

        isCefInitialized = true;
        System.out.println("[MindCare JCEF] ✅ Initialisation réussie !");
    }

    // ── Fenêtre AWT séparée ───────────────────────────────────────────────────

    public static void openUrl(String url, String participantName, String customTitle, boolean isLive) {
        SwingUtilities.invokeLater(() -> {
            try {
                prepareCef();

                if (CefApp.getState() == CefApp.CefAppState.TERMINATED) {
                    Platform.runLater(() -> showError(
                            "Le moteur vidéo a été arrêté. Veuillez redémarrer l'application."));
                    return;
                }

                CefClient client = sharedCefApp.createClient();

                // ── Injecter le design MindCare après chargement ──────────────
                client.addLoadHandler(new org.cef.handler.CefLoadHandlerAdapter() {
                    @Override
                    public void onLoadEnd(CefBrowser browser,
                            org.cef.browser.CefFrame frame, int httpStatusCode) {
                        if (frame.isMain()) {
                            browser.executeJavaScript(
                                    buildMindCareStyle(), frame.getURL(), 0);
                        }
                    }
                });

                final CefBrowser browser = client.createBrowser(url, false, false);
                final Component browserUI = browser.getUIComponent();

                final Frame awtFrame = new Frame();
                awtFrame.setLayout(new BorderLayout());
                awtFrame.setTitle("MindCare — " + customTitle);
                awtFrame.setSize(1280, 800);
                awtFrame.setBackground(Color.BLACK);
                awtFrame.setLocationRelativeTo(null);

                final JFXPanel toolbarPanel = new JFXPanel();
                toolbarPanel.setPreferredSize(new Dimension(1280, 60));

                Panel browserPanel = new Panel(new BorderLayout());
                browserPanel.setBackground(Color.BLACK);
                browserPanel.add(browserUI, BorderLayout.CENTER);

                awtFrame.add(toolbarPanel, BorderLayout.NORTH);
                awtFrame.add(browserPanel, BorderLayout.CENTER);

                browserUI.revalidate();
                browserUI.repaint();
                awtFrame.setVisible(true);

                Platform.runLater(() -> buildToolbar(
                        toolbarPanel, customTitle, isLive, browser, awtFrame));

                awtFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        if (isLive)
                            stopTimer();
                        SwingUtilities.invokeLater(() -> {
                            browser.close(true);
                            awtFrame.dispose();
                        });
                    }
                });

                scheduleRepaint(browserUI);

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showErrorWithDetails(
                        "Erreur JCEF : " + e.getMessage(),
                        "État CEF : " + CefApp.getState()));
            }
        });
    }

    // ── Intégration dans un Pane JavaFX ──────────────────────────────────────
    public static void openOverlay(javafx.stage.Stage primaryStage, int seanceId, int userId, String url, String title,
            String subtitle, boolean showSidebar, String moderatorName) {
        if (isOpen)
            return;
        isOpen = true;
        currentSeanceId = seanceId;
        currentUserId = userId;
        mainAppStage = primaryStage;
        currentHost = "En attente...";
        participants.clear();

        // Ne PAS verrouiller l'interface JavaFX pour permettre la navigation
        Platform.runLater(() -> {
            if (mainAppStage != null && mainAppStage.getScene() != null) {
                System.out.println("[MINDCARE] Fenêtre d'appel ouverte (non-modale).");
            }
        });

        // Enregistrer en DB
        if (userId > 0) {
            try {
                System.out.println("[MINDCARE] Insertion participant " + userId + " pour séance " + seanceId);
                seanceService.ajouterParticipant(seanceId, userId);
                System.out.println("[MINDCARE] ✅ Succès DB.");
            } catch (Exception e) {
                System.err.println("[MINDCARE] ❌ Échec DB : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("[MINDCARE] ⚠️ ID Utilisateur invalide (" + userId + "), non enregistré en DB.");
        }

        // Charger participants existants en DB
        participants.addAll(seanceService.getNomsParticipants(seanceId));

        SwingUtilities.invokeLater(() -> {
            try {
                prepareCef();
                CefClient client = sharedCefApp.createClient();

                client.addDisplayHandler(new org.cef.handler.CefDisplayHandlerAdapter() {
                    @Override
                    public boolean onConsoleMessage(CefBrowser browser, org.cef.CefSettings.LogSeverity level,
                            String message, String source, int line) {
                        if (message.startsWith("MINDCARE_HOST:")) {
                            String h = message.replace("MINDCARE_HOST:", "").trim();
                            if (moderatorName == null || moderatorName.isEmpty() || h.equalsIgnoreCase(moderatorName)) {
                                currentHost = h;
                                Platform.runLater(() -> {
                                    if (hostLabel != null)
                                        hostLabel.setText("👑 Host: " + currentHost);
                                });
                            }
                        } else if (message.startsWith("MINDCARE_JOIN:")) {
                            String name = message.replace("MINDCARE_JOIN:", "").trim();
                            if (!participants.contains(name)) {
                                participants.add(name);
                                Platform.runLater(JitsiMeetWindowF::updateParticipantUI);
                            }
                        } else if (message.startsWith("MINDCARE_LEFT:")) {
                            String name = message.replace("MINDCARE_LEFT:", "").trim();
                            participants.remove(name);
                            Platform.runLater(JitsiMeetWindowF::updateParticipantUI);
                        }
                        return false;
                    }
                });

                client.addLoadHandler(new org.cef.handler.CefLoadHandlerAdapter() {
                    @Override
                    public void onLoadEnd(CefBrowser browser, org.cef.browser.CefFrame frame, int httpStatusCode) {
                        if (frame.isMain()) {
                            browser.executeJavaScript(buildMindCareStyle(), frame.getURL(), 0);
                            browser.executeJavaScript(buildParticipantTracker(), frame.getURL(), 0);
                        }
                    }
                });

                final CefBrowser browser = client.createBrowser(url, false, false);
                final Component browserUI = browser.getUIComponent();

                final int[] bounds = new int[4];
                try {
                    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    Platform.runLater(() -> {
                        bounds[0] = (int) primaryStage.getX();
                        bounds[1] = (int) primaryStage.getY();
                        bounds[2] = (int) primaryStage.getWidth();
                        bounds[3] = (int) primaryStage.getHeight();
                        latch.countDown();
                    });
                    latch.await();
                } catch (Exception e) {
                    bounds[0] = 100;
                    bounds[1] = 50;
                    bounds[2] = 1280;
                    bounds[3] = 800;
                }

                Frame overlay = new Frame();
                overlay.setUndecorated(true);
                overlay.setLayout(new BorderLayout());
                overlay.setBackground(Color.decode("#0F1923"));
                overlay.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);

                Panel toolbarContainer = new Panel(new BorderLayout());
                toolbarContainer.setBackground(Color.decode("#161B22"));
                toolbarContainer.setPreferredSize(new Dimension(bounds[2], 60));

                JFXPanel fxToolbar = new JFXPanel();
                fxToolbar.setPreferredSize(new Dimension(bounds[2], 60));
                toolbarContainer.add(fxToolbar, BorderLayout.CENTER);

                JFXPanel fxSidebar = new JFXPanel();
                fxSidebar.setPreferredSize(new Dimension(240, bounds[3] - 60));
                fxSidebar.setBackground(Color.decode("#0F1923"));

                Panel videoPanel = new Panel(new BorderLayout());
                videoPanel.setBackground(Color.BLACK);
                videoPanel.add(browserUI, BorderLayout.CENTER);

                Panel mainContent = new Panel(new BorderLayout());
                mainContent.add(videoPanel, BorderLayout.CENTER);
                if (showSidebar) {
                    mainContent.add(fxSidebar, BorderLayout.EAST);
                }

                overlay.add(toolbarContainer, BorderLayout.NORTH);
                overlay.add(mainContent, BorderLayout.CENTER);
                overlay.setAlwaysOnTop(false);
                overlay.setVisible(true);

                primaryStage.xProperty().addListener((obs, oldV, newV) -> {
                    if (isOpen && overlay.isVisible())
                        overlay.setLocation(newV.intValue(), (int) primaryStage.getY());
                });
                primaryStage.yProperty().addListener((obs, oldV, newV) -> {
                    if (isOpen && overlay.isVisible())
                        overlay.setLocation((int) primaryStage.getX(), newV.intValue());
                });
                primaryStage.widthProperty().addListener((obs, oldV, newV) -> {
                    if (isOpen && overlay.isVisible())
                        overlay.setSize(newV.intValue(), (int) primaryStage.getHeight());
                });
                primaryStage.heightProperty().addListener((obs, oldV, newV) -> {
                    if (isOpen && overlay.isVisible())
                        overlay.setSize((int) primaryStage.getWidth(), newV.intValue());
                });

                Platform.runLater(() -> {
                    HBox bar = new HBox(20);
                    bar.setAlignment(Pos.CENTER_LEFT);
                    bar.setPadding(new Insets(0, 25, 0, 15));
                    bar.setStyle(
                            "-fx-background-color: #161B22; -fx-border-color: #1E293B; -fx-border-width: 0 0 1 0;");

                    Button quitBtn = new Button("← Quitter");
                    quitBtn.setStyle(
                            "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 18; -fx-cursor: hand; -fx-font-size: 13px;");
                    quitBtn.setOnAction(e -> closeOverlay(overlay, browser));

                    VBox titleBox = new VBox(2);
                    Label titleLbl = new Label("🧠 " + title);
                    titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #00C9A7;");
                    Label subLbl = new Label(subtitle);
                    subLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
                    titleBox.getChildren().addAll(titleLbl, subLbl);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    hostLabel = new Label("👑 En attente...");
                    hostLabel.setStyle(
                            "-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: #292310; -fx-padding: 5 12; -fx-background-radius: 15;");

                    countLabel = new Label("👥 " + participants.size());
                    countLabel.setStyle(
                            "-fx-text-fill: #00C9A7; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: #0D2818; -fx-padding: 5 12; -fx-background-radius: 15;");

                    Label live = new Label("⬤ EN DIRECT");
                    live.setStyle(
                            "-fx-text-fill: #F85149; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-color: #2D1016; -fx-padding: 5 12; -fx-background-radius: 15;");

                    bar.getChildren().addAll(quitBtn, titleBox, spacer, hostLabel, countLabel, live);
                    fxToolbar.setScene(new Scene(bar, bounds[2], 60));

                    VBox sidebar = new VBox(0);
                    sidebar.setStyle("-fx-background-color: #0D1520;");
                    sidebar.setPrefWidth(240);
                    Label sideTitle = new Label("Participants");
                    sideTitle.setStyle(
                            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #CBD5E1; -fx-padding: 15 15 10 15; -fx-border-color: #1E293B; -fx-border-width: 0 0 1 0;");
                    sideTitle.setMaxWidth(Double.MAX_VALUE);
                    participantListBox = new VBox(4);
                    participantListBox.setStyle("-fx-padding: 10 10 10 10;");
                    ScrollPane scroll = new ScrollPane(participantListBox);
                    scroll.setFitToWidth(true);
                    scroll.setStyle(
                            "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
                    VBox.setVgrow(scroll, Priority.ALWAYS);
                    sidebar.getChildren().addAll(sideTitle, scroll);
                    fxSidebar.setScene(new Scene(sidebar, 240, bounds[3] - 60));

                    updateParticipantUI();
                    scroll.getStylesheets().add(
                            "data:text/css,.scroll-bar{-fx-background-color:#1E293B;}.scroll-bar .thumb{-fx-background-color:#334155;-fx-background-radius:4px;}");
                });

                scheduleRepaint(browserUI);
            } catch (Exception e) {
                isOpen = false;
                e.printStackTrace();
                Platform.runLater(() -> showError("Erreur JCEF : " + e.getMessage()));
            }
        });
    }

    private static void updateParticipantUI() {
        if (countLabel != null)
            countLabel.setText("👥 " + participants.size());

        if (participantListBox != null) {
            participantListBox.getChildren().clear();
            for (String name : participants) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 8; " +
                        "-fx-padding: 8 12;");

                Label avatar = new Label(String.valueOf(name.charAt(0)).toUpperCase());
                avatar.setStyle("-fx-background-color: #00C9A7; -fx-text-fill: #0F1923; " +
                        "-fx-font-weight: bold; -fx-font-size: 12px; " +
                        "-fx-min-width: 30; -fx-min-height: 30; " +
                        "-fx-background-radius: 15; -fx-alignment: center;");

                Label nameLbl = new Label(name);
                nameLbl.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 12px;");

                row.getChildren().addAll(avatar, nameLbl);
                participantListBox.getChildren().add(row);
            }
        }
    }

    private static String buildParticipantTracker() {
        return "var __mc_check = setInterval(function() {" +
                "  try {" +
                "    if (typeof APP !== 'undefined' && APP.conference && APP.conference._room) {" +
                "      clearInterval(__mc_check);" +

                // Participants déjà présents
                "      var parts = APP.conference.listMembers();" +
                "      parts.forEach(function(p) {" +
                "        console.log('MINDCARE_JOIN:' + (p.getDisplayName() || 'Inconnu'));" +
                "        if (p.getRole() === 'moderator') {" +
                "          console.log('MINDCARE_HOST:' + (p.getDisplayName() || 'Inconnu'));" +
                "        }" +
                "      });" +

                // Moi-même
                "      var myName = APP.conference.getLocalDisplayName() || 'Moi';" +
                "      console.log('MINDCARE_JOIN:' + myName);" +
                "      if (APP.conference.isModerator() || window.location.hash.includes('role=%22Psychologue%22') || window.location.hash.includes('role=Psychologue')) {"
                +
                "        console.log('MINDCARE_HOST:' + myName);" +
                "      }" +

                // Nouveaux participants
                "      APP.conference.on('conference.userJoined', function(id, user) {" +
                "        var n = user.getDisplayName() || 'Inconnu';" +
                "        console.log('MINDCARE_JOIN:' + n);" +
                "      });" +

                // Participants qui partent
                "      APP.conference.on('conference.userLeft', function(id, user) {" +
                "        var n = (user && user.getDisplayName()) ? user.getDisplayName() : id;" +
                "        console.log('MINDCARE_LEFT:' + n);" +
                "      });" +

                // Changement de rôle (host)
                "      APP.conference.on('conference.participantRoleChanged', function(id, role) {" +
                "        if (role === 'moderator') {" +
                "          var p = APP.conference.getParticipantById(id);" +
                "          var n = p ? (p.getDisplayName() || 'Inconnu') : 'Inconnu';" +
                "          console.log('MINDCARE_HOST:' + n);" +
                "        }" +
                "      });" +
                "    }" +
                "  } catch(e) {}" +
                "}, 1500);";
    }

    private static void closeOverlay(Frame overlay, CefBrowser browser) {
        isOpen = false;

        // Déverrouiller l'interface JavaFX
        Platform.runLater(() -> {
            if (mainAppStage != null && mainAppStage.getScene() != null) {
                mainAppStage.getScene().getRoot().setDisable(false);
                System.out.println("[MINDCARE] Application JavaFX déverrouillée.");
            }
            mainAppStage = null;
        });

        // Retirer de la DB
        try {
            if (currentSeanceId != -1 && currentUserId != -1) {
                seanceService.retirerParticipant(currentSeanceId, currentUserId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        currentHost = "En attente...";
        participants.clear();
        hostLabel = null;
        countLabel = null;
        participantListBox = null;
        currentSeanceId = -1;
        currentUserId = -1;
        SwingUtilities.invokeLater(() -> {
            browser.close(true);
            overlay.dispose();
        });
    }

    // ── Construction CefApp ───────────────────────────────────────────────────

    private static CefApp buildCefApp() throws Exception {
        CefAppBuilder builder = new CefAppBuilder();

        // Dossier d'installation local et persistant
        File installDir = new File(System.getProperty("user.home"), ".mindcare/jcef_bundle");
        if (!installDir.exists())
            installDir.mkdirs();

        builder.setInstallDir(installDir);
        builder.getCefSettings().windowless_rendering_enabled = false;
        builder.getCefSettings().command_line_args_disabled = false;

        // Cache pour les sessions Jitsi (unique pour éviter le lock
        // INITIALIZATION_FAILED)
        File cacheDir = new File(installDir, "cache_" + System.currentTimeMillis());
        if (!cacheDir.exists())
            cacheDir.mkdirs();
        builder.getCefSettings().root_cache_path = cacheDir.getAbsolutePath();
        builder.getCefSettings().cache_path = cacheDir.getAbsolutePath();

        builder.addJcefArgs(
                "--use-fake-ui-for-media-stream",
                "--enable-media-stream",
                "--no-sandbox",
                "--disable-gpu",
                "--disable-gpu-sandbox",
                "--disable-gpu-compositing",
                "--disable-web-security",
                "--autoplay-policy=no-user-gesture-required",
                "--ignore-certificate-errors",
                "--no-proxy-server",
                "--disable-site-isolation-trials",
                "--disable-features=IsolateOrigins,site-per-process",
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

        builder.setAppHandler(new MavenCefAppHandlerAdapter() {
            @Override
            public void stateHasChanged(CefApp.CefAppState state) {
                System.out.println("[MindCare JCEF State] " + state);
                if (state == CefApp.CefAppState.INITIALIZATION_FAILED) {
                    Platform.runLater(() -> showError("Erreur critique : L'initialisation du moteur vidéo a échoué. " +
                            "Vérifiez qu'aucune autre instance de MindCare n'est ouverte."));
                }
            }
        });

        System.out.println("[MindCare JCEF] Création du bundle dans : " + installDir.getAbsolutePath());
        return builder.build();
    }

    // ── URL Jitsi ─────────────────────────────────────────────────────────────

    private static String buildJitsiUrl(int seanceId, String displayName) {
        try {
            String name = java.net.URLEncoder.encode(displayName, StandardCharsets.UTF_8.name());
            return "https://meet.ffmuc.net/MindCare-" + seanceId
                    + "#userInfo.displayName=\"" + name + "\"";
        } catch (Exception e) {
            return "about:blank";
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private static void startTimer() {
        elapsedSeconds = 0;
        sessionTimer = new Timer(true);
        sessionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                elapsedSeconds++;
                String t = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60);
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

    private static void buildToolbar(JFXPanel panel, String title, boolean isLive,
            CefBrowser browser, Frame frame) {
        HBox toolbar = new HBox(20);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 25, 0, 25));
        toolbar.setStyle("-fx-background-color: #161B22; -fx-min-height: 60;");

        Label logo = new Label("🧠 MindCare  ·  " + title);
        logo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00C9A7;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        timerLabel = new Label("00:00");
        timerLabel.setStyle(
                "-fx-font-family: 'Courier New'; -fx-font-weight: bold; "
                        + "-fx-font-size: 15px; -fx-text-fill: #00C9A7; "
                        + "-fx-background-color: #0D2818; -fx-padding: 5 15; "
                        + "-fx-background-radius: 20;");
        timerLabel.setVisible(isLive);
        timerLabel.setManaged(isLive);

        Label live = new Label("⬤  LIVE");
        live.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #F85149; "
                        + "-fx-background-color: #2D1016; -fx-padding: 5 14; "
                        + "-fx-background-radius: 20;");
        live.setVisible(isLive);
        live.setManaged(isLive);

        Button fsBtn = new Button("⛶  Plein écran");
        fsBtn.setStyle(
                "-fx-background-color: #21262D; -fx-text-fill: white; "
                        + "-fx-font-weight: bold; -fx-background-radius: 8; "
                        + "-fx-padding: 7 18; -fx-cursor: hand;");
        fsBtn.setOnAction(e -> SwingUtilities.invokeLater(() -> {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (gd.getFullScreenWindow() == null)
                gd.setFullScreenWindow(frame);
            else
                gd.setFullScreenWindow(null);
        }));

        Button endBtn = new Button("📵  Quitter");
        endBtn.setStyle(
                "-fx-background-color: #F85149; -fx-text-fill: white; "
                        + "-fx-font-weight: bold; -fx-background-radius: 8; "
                        + "-fx-padding: 7 18; -fx-cursor: hand;");
        endBtn.setOnAction(e -> {
            if (isLive)
                stopTimer();
            SwingUtilities.invokeLater(() -> {
                browser.close(true);
                frame.dispose();
            });
        });

        toolbar.getChildren().addAll(logo, spacer, timerLabel, live, fsBtn, endBtn);
        panel.setScene(new Scene(toolbar, 1280, 60));

        if (isLive)
            startTimer();
    }

    private static void scheduleRepaint(Component ui) {
        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            int count = 0;

            @Override
            public void run() {
                if (count++ > 8)
                    cancel();
                SwingUtilities.invokeLater(() -> {
                    ui.revalidate();
                    ui.repaint();
                });
            }
        }, 500, 500);
    }

    private static String buildMindCareStyle() {
        return
        // ── Supprimer le watermark Jitsi ──────────────────────────────────────
        "var style = document.createElement('style');"
                + "style.innerHTML = `"
                + "body, #app, #videoconference_page { background: #0F1923 !important; }"
                + ".header, #new-toolbox, .new-toolbox { background: #161B22 !important; border-bottom: 1px solid #1E293B !important; }"
                + ".toolbox-button, .toolbox-icon { background: #1E293B !important; border-radius: 50% !important; border: none !important; }"
                + ".toolbox-button:hover, .toolbox-icon:hover { background: #2D3F50 !important; }"
                + ".toolbox-button--hangup, .hangup-button, [data-testid='toolbar.leaveButton'], button[aria-label='Leave the meeting'], button[aria-label='Quitter la réunion'] { display: none !important; }"
                + ".videocontainer, .video-thumbnail { border-radius: 16px !important; border: 2px solid #1E293B !important; }"
                + ".videocontainer--dominant { border: 2px solid #00C9A7 !important; }"
                + ".displayname, .participant-name { background: rgba(0,0,0,0.6) !important; border-radius: 20px !important; color: white !important; font-family: 'Segoe UI' !important; }"
                + ".chat-panel, #chat_container { background: #1E293B !important; border-left: 1px solid #334155 !important; }"
                + ".chat-input, .chat-input-container { background: #0F1923 !important; border: 1px solid #334155 !important; color: white !important; border-radius: 10px !important; }"
                + ".chat-message { background: #0F1923 !important; border-radius: 12px !important; }"
                + ".chat-message .display-name { color: #00C9A7 !important; font-weight: 700 !important; }"
                + ".watermark, .leftwatermark, .rightwatermark, #header_logo, .jitsi-logo { display: none !important; }"
                + ".video-thumbnail--without-video { background: #1E293B !important; }"
                + "#new-toolbox { background: linear-gradient(to top, #161B22, transparent) !important; }"
                + ".notification-container { background: #1E293B !important; border: 1px solid #00C9A7 !important; border-radius: 12px !important; }"
                + "::-webkit-scrollbar { width: 4px; }"
                + "::-webkit-scrollbar-track { background: #0F1923; }"
                + "::-webkit-scrollbar-thumb { background: #334155; border-radius: 4px; }"
                + "`;"
                + "document.head.appendChild(style);"
                + "setTimeout(function() {"
                + "  var existing = document.getElementById('mindcare-logo');"
                + "  if (existing) return;"
                + "  var logo = document.createElement('div');"
                + "  logo.id = 'mindcare-logo';"
                + "  logo.style.cssText = 'position:fixed; top:10px; left:15px; z-index:9999; background:#161B22; padding:8px 16px; border-radius:20px; border:1px solid #1E293B; font-family:Segoe UI,sans-serif; font-size:14px; font-weight:700; color:#00C9A7; display:flex; align-items:center; gap:8px;';"
                + "  logo.innerHTML = 'MindCare';"
                + "  document.body.appendChild(logo);"
                + "}, 2000);";
    }

    private static void showError(String msg) {
        showErrorWithDetails(msg, null);
    }

    private static void showErrorWithDetails(String header, String details) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("MindCare — Erreur vidéo");
        a.setHeaderText(header);
        a.setContentText(details != null ? details : "Veuillez vérifier votre connexion internet.");
        a.showAndWait();
    }
}