package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;
import services.*;
import services.AvatarService.*;
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SuivieController {

    private MindCareLayoutController parentController;
    private static SuivieController instance;
    private int currentPatientId = -1;

    @FXML private ScrollPane scrollRacine;
    @FXML private VBox rootVBox;

    @FXML private Label   lblBienvenue;
    @FXML private Label   lblCoinsGagnes;
    @FXML private Label   lblSessionCount;
    @FXML private Label   scoreBienEtre;
    @FXML private Label   scoreStress;
    @FXML private Label   scoreHumeur;
    @FXML private Arc     arcBienEtre;
    @FXML private Arc     arcStress;
    @FXML private Arc     arcHumeur;
    @FXML private Label   lblTrendBienEtre;
    @FXML private Label   lblTrendStress;
    @FXML private Label   lblTrendHumeur;
    @FXML private ComboBox<String>          comboPeriode;
    @FXML private AreaChart<String, Number> evolutionChart;
    @FXML private Label   lblConseil;
    @FXML private VBox    historyBox;
    @FXML private VBox    badgesBox;
    @FXML private Button  btnEspacePsy;
    @FXML private Label   lblTestResultat;

    @FXML private Label lblEmoji1; @FXML private Label lblTitre1; @FXML private Label lblDesc1;
    @FXML private Label lblEmoji2; @FXML private Label lblTitre2; @FXML private Label lblDesc2;
    @FXML private Label lblEmoji3; @FXML private Label lblTitre3; @FXML private Label lblDesc3;

    @FXML private javafx.scene.image.ImageView imgAvatar;
    @FXML private Label  lblAvatarPseudo;
    @FXML private Label  lblAvatarEtat;
    @FXML private Label  lblAvatarMessage;
    @FXML private Button btnPersonnaliserAvatar;
    @FXML private Button btnVoixAvatar;
    @FXML private VBox   avatarEtatBox;

    @FXML private javafx.scene.image.ImageView imgTherapeutique;
    @FXML private Label     lblImageMessage;
    @FXML private Label     lblImageTheme;
    @FXML private StackPane paneImageLoading;
    @FXML private StackPane paneImageContainer;
    @FXML private Button    btnRegeneImage;

    @FXML private Label   lblMusiqueMessage;
    @FXML private Button  btnChargerMusique;
    @FXML private Button  btnPlayPause;
    @FXML private Label   lblPisteEmoji;
    @FXML private Label   lblPisteNom;
    @FXML private Label   lblPisteDuree;
    @FXML private Label   lblEtatLecture;
    @FXML private VBox    listePistesBox;
    @FXML private Label   lblChargementPistes;
    @FXML private Slider  sliderVolume;

    // Météo
    @FXML private Label  lblMeteoEmoji;
    @FXML private Label  lblMeteoTemp;
    @FXML private Label  lblMeteoDesc;
    @FXML private Label  lblMeteoDetails;
    @FXML private Label  lblMeteoConseil;
    @FXML private VBox   meteoBox;
    @FXML private Button btnRefreshMeteo;

    private final ServiceQuiz               serviceQuiz        = new ServiceQuiz();
    private final ServiceGroqQuiz           serviceGroqQuiz    = new ServiceGroqQuiz();
    private final ServiceMusiqueQuiz        serviceMusiqueQuiz = new ServiceMusiqueQuiz();
    private final AvatarService             avatarService      = new AvatarService();
    private final ServiceImageTherapeutique serviceImage       = new ServiceImageTherapeutique();

    private PrefsAvatar prefsAvatar;
    private int scoreBE = 0, scoreST = 0, scoreHU = 0;
    private EtatDetecte dernierEtatPrononce = null;
    static boolean rappelDejaVerifie = false;
    private Animation animationAvatar;
    private ServiceImageTherapeutique.ResultatImage dernierResultatImage = null;
    private boolean imageEnChargement = false;

    private javafx.scene.media.MediaPlayer mediaPlayer;
    private List<ServiceMusiqueQuiz.Piste> pistesChargees = new ArrayList<>();
    private int     pisteActuelle = 0;
    private boolean enLecture     = false;

    // Coordonnées Tunis
    private final double geoLat = 36.8190;
    private final double geoLon = 10.1658;

    // ══════════════════════════════════════════════════════════════
    public static void rafraichir() {
        if (instance != null) Platform.runLater(() -> {
            instance.updateScoresFromDB();
            instance.chargerGraphiqueReel();
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        instance = this;
        Session.Role role = Session.getRoleConnecte();
        currentPatientId  = Session.getUserId();

        // ✅ Psychologue/Admin → redirection automatique EspacePraticien
        if (role == Session.Role.PSYCHOLOGUE || role == Session.Role.ADMIN) {
            viderPageImmediatement();
            Platform.runLater(() -> {
                try {
                    FXMLLoader l = new FXMLLoader(getClass().getResource("/views/EspacepraticienQuiz.fxml"));
                    Node v = l.load();
                    VBox ca = null;
                    if (scrollRacine != null && scrollRacine.getScene() != null)
                        ca = (VBox) scrollRacine.getScene().lookup("#contentArea");
                    if (ca != null) ca.getChildren().setAll(v);
                } catch (IOException e) {
                    System.err.println("Redirection praticien : " + e.getMessage());
                }
            });
            return;
        }

        // ✅ Ni USER ni PSYCHOLOGUE → page vide
        if (role != Session.Role.USER) {
            viderPageImmediatement();
            return;
        }

        // ✅ Patient connecté
        if (btnEspacePsy != null) {
            btnEspacePsy.setVisible(false);
            btnEspacePsy.setManaged(false);
        }

        if (currentPatientId <= 0) {
            if (lblBienvenue != null) lblBienvenue.setText("Aucun utilisateur connecté.");
            return;
        }

        if (sliderVolume != null)
            sliderVolume.valueProperty().addListener(
                    (obs, o, n) -> { if (mediaPlayer != null) mediaPlayer.setVolume(n.doubleValue()); });

        if (meteoBox != null) {
            meteoBox.setVisible(true);
            meteoBox.setManaged(true);
            meteoBox.setOpacity(1);
        }

        mettreAJourBoutonVoix();
        configurerCombo();
        loadPatientData();
        chargerGraphiqueReel();
        chargerAvatar();
        chargerMeteo();

        if (!rappelDejaVerifie) {
            rappelDejaVerifie = true;
            new Thread(() -> {
                try { new ServiceRappelQuiz().verifierEtEnvoyerRappels(); }
                catch (Exception e) { System.err.println("Rappels : " + e.getMessage()); }
            }).start();
        }
    }
    public void setParentController(MindCareLayoutController p) { this.parentController = p; }

    // ══════════════════════════════════════════════════════════════
    //  VIDER PAGE
    // ══════════════════════════════════════════════════════════════
    private void viderPageImmediatement() {
        if (scrollRacine != null) {
            scrollRacine.setVisible(false);
            scrollRacine.setManaged(false);
            return;
        }
        if (rootVBox != null) {
            rootVBox.getChildren().clear();
            rootVBox.setSpacing(0);
            rootVBox.setPadding(new Insets(0));
            rootVBox.setStyle("-fx-background-color: transparent;");
            Platform.runLater(() -> {
                try {
                    javafx.scene.Parent parent = rootVBox.getParent();
                    if (parent != null) { parent.setVisible(false); parent.setManaged(false); }
                } catch (Exception e) { System.err.println("viderPage : " + e.getMessage()); }
            });
            return;
        }
        masquerTout();
        Platform.runLater(() -> {
            try {
                if (lblBienvenue == null) return;
                javafx.scene.Parent courant = lblBienvenue.getParent();
                while (courant != null) {
                    javafx.scene.Parent parent = courant.getParent();
                    if (courant instanceof ScrollPane sp) { sp.setVisible(false); sp.setManaged(false); return; }
                    if (parent instanceof ScrollPane sp) { sp.setVisible(false); sp.setManaged(false); return; }
                    if (parent instanceof BorderPane bp) { bp.setCenter(new VBox()); return; }
                    if (parent instanceof AnchorPane ap) { ap.getChildren().clear(); ap.setStyle("-fx-background-color:transparent;"); return; }
                    courant = parent;
                }
            } catch (Exception e) { System.err.println("viderPage runLater : " + e.getMessage()); }
        });
    }

    private void masquerTout() {
        Node[] noeuds = {
                lblBienvenue, lblCoinsGagnes, lblSessionCount,
                scoreBienEtre, scoreStress, scoreHumeur,
                arcBienEtre, arcStress, arcHumeur,
                lblTrendBienEtre, lblTrendStress, lblTrendHumeur,
                comboPeriode, evolutionChart, lblConseil,
                historyBox, badgesBox, btnEspacePsy, lblTestResultat,
                lblEmoji1, lblTitre1, lblDesc1,
                lblEmoji2, lblTitre2, lblDesc2,
                lblEmoji3, lblTitre3, lblDesc3,
                imgAvatar, lblAvatarPseudo, lblAvatarEtat, lblAvatarMessage,
                btnPersonnaliserAvatar, btnVoixAvatar, avatarEtatBox,
                imgTherapeutique, lblImageMessage, lblImageTheme,
                paneImageLoading, paneImageContainer, btnRegeneImage,
                lblMusiqueMessage, btnChargerMusique, btnPlayPause,
                lblPisteEmoji, lblPisteNom, lblPisteDuree, lblEtatLecture,
                listePistesBox, lblChargementPistes, sliderVolume,
                meteoBox, btnRefreshMeteo,
                lblMeteoEmoji, lblMeteoTemp, lblMeteoDesc,
                lblMeteoDetails, lblMeteoConseil
        };
        for (Node n : noeuds) masquer(n);
    }

    private void masquer(Node n) {
        if (n == null) return;
        n.setVisible(false);
        n.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════════
    //  MÉTÉO — FIX COMPLET
    // ══════════════════════════════════════════════════════════════
    @FXML
    public void chargerMeteo() {
        // ✅ FIX : réinitialiser UI + s'assurer que la box est visible AVANT de charger
        Platform.runLater(() -> {
            // ✅ Rendre la box toujours visible
            if (meteoBox != null) {
                meteoBox.setVisible(true);
                meteoBox.setManaged(true);
            }
            if (lblMeteoEmoji   != null) lblMeteoEmoji.setText("🌡️");
            if (lblMeteoTemp    != null) lblMeteoTemp.setText("...");
            if (lblMeteoDesc    != null) lblMeteoDesc.setText("Chargement météo...");
            if (lblMeteoDetails != null) lblMeteoDetails.setText("");
            if (lblMeteoConseil != null) lblMeteoConseil.setText("Conseil IA en cours...");
            if (btnRefreshMeteo != null) {
                btnRefreshMeteo.setDisable(true);
                btnRefreshMeteo.setText("⏳");
            }
        });

        new Thread(() -> {
            String   resumeMeteo = null;
            double[] donnees     = null;
            String   icone       = null;

            try {
                resumeMeteo = ServiceMeteo.getMeteo(geoLat, geoLon);
                System.out.println("✅ Météo : " + (resumeMeteo != null
                        ? resumeMeteo.substring(0, Math.min(40, resumeMeteo.length())) : "null"));
            } catch (Exception e) {
                System.err.println("❌ getMeteo : " + e.getMessage());
            }

            try {
                donnees = ServiceMeteo.getDonneesBrutes(geoLat, geoLon);
                System.out.println("✅ Données brutes : " + (donnees != null ? donnees[0] + "°C" : "null"));
            } catch (Exception e) {
                System.err.println("❌ getDonneesBrutes : " + e.getMessage());
            }

            try {
                icone = ServiceMeteo.getIcone(geoLat, geoLon);
                System.out.println("✅ Icône : " + icone);
            } catch (Exception e) {
                System.err.println("❌ getIcone : " + e.getMessage());
            }

            final String   resumeFinal = (resumeMeteo != null && !resumeMeteo.startsWith("❌"))
                    ? resumeMeteo : "🌡️ Météo temporairement indisponible";
            final double[] donFinal    = donnees;
            final String   icoFinal    = (icone != null && !icone.isEmpty()) ? icone : "01d";

            String[] lignes = resumeFinal.split("\n");
            String   ligne0 = lignes.length > 0 ? lignes[0].trim() : "";
            String   emojiM = "🌡️", descM = ligne0;
            int espIdx = ligne0.indexOf(' ');
            if (espIdx > 0) {
                emojiM = ligne0.substring(0, espIdx).trim();
                descM  = ligne0.substring(espIdx).trim();
            }

            String tempVal = extraireApres(lignes.length > 1 ? lignes[1] : "", ":");
            if (tempVal != null && tempVal.contains("  "))
                tempVal = tempVal.substring(0, tempVal.indexOf("  ")).trim();

            StringBuilder details = new StringBuilder();
            for (int i = 2; i < Math.min(lignes.length, 5); i++) {
                if (i > 2) details.append("  |  ");
                details.append(lignes[i].trim());
            }

            String conseilIA = null;
            try {
                conseilIA = genererConseilMeteoGroqInterne(donFinal, icoFinal, descM);
                System.out.println("✅ Conseil Groq OK");
            } catch (Exception e) {
                System.err.println("❌ Conseil Groq : " + e.getMessage());
            }
            if (conseilIA == null || conseilIA.isBlank()) {
                conseilIA = conseilFallback(donFinal, icoFinal);
                System.out.println("ℹ️ Conseil fallback utilisé");
            }

            final String fEmoji   = emojiM;
            final String fTemp    = (tempVal == null || tempVal.isEmpty()) ? "--°C" : tempVal;
            final String fDesc    = descM;
            final String fDetails = details.toString();
            final String fConseil = conseilIA;

            // ✅ Mise à jour UI — meteoBox toujours visible, pas de FadeTransition qui cache
            Platform.runLater(() -> {
                try {
                    if (lblMeteoEmoji   != null) lblMeteoEmoji.setText(fEmoji);
                    if (lblMeteoTemp    != null) lblMeteoTemp.setText(fTemp);
                    if (lblMeteoDesc    != null) lblMeteoDesc.setText(fDesc);
                    if (lblMeteoDetails != null) lblMeteoDetails.setText(fDetails);
                    if (lblMeteoConseil != null) lblMeteoConseil.setText(fConseil);

                    // ✅ FIX : setVisible AVANT la FadeTransition
                    if (meteoBox != null) {
                        meteoBox.setVisible(true);
                        meteoBox.setManaged(true);
                        meteoBox.setOpacity(0);
                        FadeTransition ft = new FadeTransition(Duration.millis(600), meteoBox);
                        ft.setFromValue(0);
                        ft.setToValue(1);
                        ft.setInterpolator(Interpolator.EASE_OUT);
                        // ✅ FIX : s'assurer que opacity=1 même si la transition est interrompue
                        ft.setOnFinished(e -> meteoBox.setOpacity(1));
                        ft.play();
                    }
                } catch (Exception e) {
                    System.err.println("❌ UI météo : " + e.getMessage());
                } finally {
                    // ✅ TOUJOURS réactiver le bouton
                    if (btnRefreshMeteo != null) {
                        btnRefreshMeteo.setDisable(false);
                        btnRefreshMeteo.setText("↺ Actualiser");
                    }
                }
            });

        }).start();
    }

    // ── Conseil Groq interne ──────────────────────────────────────
    private String genererConseilMeteoGroqInterne(double[] donnees, String icone, String descMeteo) {
        try {
            String emojiMeteo = ServiceMeteo.iconeVersEmoji(icone != null ? icone : "01d");
            String contexteMeteo = donnees != null
                    ? String.format("%s %s, %.0f degrés (ressenti %.0f), humidité %.0f%%, vent %.0f km/h",
                    emojiMeteo, descMeteo, donnees[0], donnees[1], donnees[2], donnees[3])
                    : emojiMeteo + " " + descMeteo;

            int heure = java.time.LocalTime.now().getHour();
            String momentJournee, conseilSommeil;
            if      (heure >= 22 || heure < 6)  { momentJournee = "nuit";       conseilSommeil = scoreST >= 60 ? "Le stress élevé perturbe le sommeil : évitez les écrans 1h avant de dormir." : "Bonne nuit ! Chambre entre 18 et 20 degrés pour un sommeil réparateur."; }
            else if (heure >= 18)                { momentJournee = "soirée";     conseilSommeil = scoreHU < 50  ? "En soirée avec humeur basse : réduisez la luminosité des écrans." : "Soirée idéale pour se relaxer. Évitez la caféine après 16h."; }
            else if (heure >= 14)                { momentJournee = "après-midi"; conseilSommeil = "Une micro-sieste de 10 à 20 min entre 13h et 15h améliore la concentration."; }
            else                                 { momentJournee = "matinée";    conseilSommeil = scoreST >= 60 ? "La lumière naturelle le matin régule votre horloge circadienne." : "La lumière naturelle dans l'heure suivant le réveil booste la sérotonine."; }

            String etatPsy;
            if      (scoreST >= 70)                   etatPsy = "stress très élevé - besoin urgent de décompression";
            else if (scoreST >= 50)                   etatPsy = "tension modérée - pause active recommandée";
            else if (scoreBE >= 75 && scoreHU >= 70)  etatPsy = "état épanoui - énergie positive";
            else if (scoreBE < 40  || scoreHU < 40)   etatPsy = "moral bas - besoin de motivation";
            else                                      etatPsy = "état équilibré";

            String prompt = "Tu es un psychologue bienveillant et un coach bien-être pour MindCare.\n\n"
                    + "Contexte du patient (" + momentJournee + ") :\n"
                    + "- Météo : " + contexteMeteo + "\n"
                    + "- Scores : Bien-être " + scoreBE + "%, Stress " + scoreST + "%, Humeur " + scoreHU + "%\n"
                    + "- État : " + etatPsy + "\n"
                    + "- Conseil sommeil : " + conseilSommeil + "\n\n"
                    + "Génère UN conseil intégré (3 phrases maximum) qui :\n"
                    + "1. Exploite la météo actuelle comme opportunité concrète\n"
                    + "2. Propose une activité adaptée à l'état psychologique\n"
                    + "3. Intègre naturellement le conseil sommeil\n"
                    + "Commence directement par le conseil. Ton chaleureux et encourageant. Uniquement en français.";

            return serviceGroqQuiz.envoyerPromptLibre(prompt);
        } catch (Exception e) {
            System.err.println("genererConseilMeteoGroqInterne : " + e.getMessage());
            return null;
        }
    }

    // ── Conseil fallback (sans IA) ────────────────────────────────
    private String conseilFallback(double[] donnees, String icone) {
        int heure = java.time.LocalTime.now().getHour();
        boolean soiree = heure >= 20, nuit = heure >= 22 || heure < 6;
        if (nuit)  return scoreST >= 60
                ? "Nuit de stress : respirez lentement (4s/6s) avant de dormir. Chambre à 18-20 degrés."
                : "Bonne nuit ! Chambre fraîche et sans écrans pour un sommeil réparateur.";
        if (icone != null && (icone.startsWith("01") || icone.startsWith("02"))) {
            double temp = donnees != null ? donnees[0] : 20;
            if (temp >= 14 && temp <= 30) return scoreST >= 60
                    ? "Beau temps : 20 min de marche réduisent le cortisol. Ce soir évitez les écrans 1h avant de dormir."
                    : "Conditions parfaites pour sortir. La nature booste la sérotonine.";
        }
        if (icone != null && (icone.startsWith("09") || icone.startsWith("10") || icone.startsWith("11")))
            return soiree
                    ? "Soirée pluvieuse : musique douce ou lecture préparent un sommeil de qualité."
                    : "Temps pluvieux : méditation ou yoga. 10 min de pleine conscience réduisent le stress.";
        return scoreST >= 60
                ? "Stress détecté : cohérence cardiaque (5s/5s, 5 min). Ce soir, couchez-vous à heure fixe."
                : "Continuez vos efforts ! Une routine sommeil régulière consolide vos progrès.";
    }

    private String extraireApres(String ligne, String sep) {
        int idx = ligne.lastIndexOf(sep); if (idx < 0) return "";
        String r = ligne.substring(idx + sep.length()).trim();
        int e = r.indexOf(' ');
        return e > 0 ? r.substring(0, e) : r;
    }

    // ══════════════════════════════════════════════════════════════
    //  VOIX AVATAR
    // ══════════════════════════════════════════════════════════════
    @FXML public void toggleVoixAvatar() {
        ServiceVoixQuiz.setVoixActive(!ServiceVoixQuiz.isVoixActive());
        mettreAJourBoutonVoix();
    }

    private void mettreAJourBoutonVoix() {
        if (btnVoixAvatar == null) return;
        if (ServiceVoixQuiz.isVoixActive()) {
            btnVoixAvatar.setText("ON");
            btnVoixAvatar.setStyle("-fx-background-color:rgba(92,152,168,0.15); -fx-text-fill:#5C98A8; -fx-background-radius:20; -fx-font-size:13px; -fx-cursor:hand; -fx-border-color:rgba(92,152,168,0.35); -fx-border-radius:20; -fx-border-width:1.5; -fx-padding:5 10;");
        } else {
            btnVoixAvatar.setText("OFF");
            btnVoixAvatar.setStyle("-fx-background-color:#f1f5f9; -fx-text-fill:#94a3b8; -fx-background-radius:20; -fx-font-size:13px; -fx-cursor:hand; -fx-border-color:#e2e8f0; -fx-border-radius:20; -fx-border-width:1.5; -fx-padding:5 10;");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DONNÉES PATIENT
    // ══════════════════════════════════════════════════════════════
    private void loadPatientData() {
        try {
            String nom = (Session.getFullName() != null && !Session.getFullName().trim().isEmpty())
                    ? Session.getFullName() : "Patient #" + currentPatientId;
            if (lblBienvenue != null) lblBienvenue.setText("Hello " + nom + " !");
            updateScoresFromDB();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private int detecterScoreMax(List<Integer> scores) {
        if (scores == null || scores.isEmpty()) return 10;
        int max = scores.stream().mapToInt(Integer::intValue).max().orElse(1);
        int[] paliers = {5,6,7,10,12,15,20,25,30};
        for (int p : paliers) if (max <= p) return p;
        return max;
    }

    private int scoreBrutEnPourcentage(int brut, int max) {
        if (max <= 0) return 0;
        return (int) Math.min(100, Math.max(0, brut * 100.0 / max));
    }

    private void updateScoresFromDB() {
        try {
            List<String> hist = serviceQuiz.getHistoriquePatient(currentPatientId);
            if (hist.isEmpty()) {
                if (scoreBienEtre   != null) scoreBienEtre.setText("0/100");
                if (scoreStress     != null) scoreStress.setText("0/100");
                if (scoreHumeur     != null) scoreHumeur.setText("0/100");
                if (lblCoinsGagnes  != null) lblCoinsGagnes.setText("0 coins");
                if (lblSessionCount != null) lblSessionCount.setText("0 session");
                animerArc(arcBienEtre, 0, "#A78BFA"); animerArc(arcStress, 0, "#FF6B9D"); animerArc(arcHumeur, 0, "#4FACFE");
                setTrend(lblTrendBienEtre, 0, "#A78BFA"); setTrend(lblTrendStress, 0, "#FF6B9D"); setTrend(lblTrendHumeur, 0, "#4FACFE");
                if (lblConseil != null) lblConseil.setText("Commencez votre premier test !");
                return;
            }
            List<Integer> rawBE = new ArrayList<>(), rawST = new ArrayList<>(), rawHU = new ArrayList<>();
            for (String l : hist) {
                int sc = extraireScore(l); if (sc <= 0) continue;
                String t = extraireTitre(l).toLowerCase();
                if (t.contains("stress")) rawST.add(sc);
                else if (t.contains("humeur")) rawHU.add(sc);
                else rawBE.add(sc);
            }
            int maxBE = detecterScoreMax(rawBE), maxST = detecterScoreMax(rawST), maxHU = detecterScoreMax(rawHU);
            double moyBE = rawBE.isEmpty() ? 0 : rawBE.stream().mapToInt(Integer::intValue).average().orElse(0);
            double moyST = rawST.isEmpty() ? 0 : rawST.stream().mapToInt(Integer::intValue).average().orElse(0);
            double moyHU = rawHU.isEmpty() ? 0 : rawHU.stream().mapToInt(Integer::intValue).average().orElse(0);
            if (rawBE.isEmpty() && rawST.isEmpty() && rawHU.isEmpty()) {
                List<Integer> tous = new ArrayList<>();
                for (String l : hist) { int sc = extraireScore(l); if (sc > 0) tous.add(sc); }
                int maxT = detecterScoreMax(tous); double m = tous.stream().mapToInt(Integer::intValue).average().orElse(0);
                scoreBE = scoreST = scoreHU = scoreBrutEnPourcentage((int) m, maxT);
            } else {
                if (rawBE.isEmpty()) moyBE = (moyST + moyHU) / 2.0;
                if (rawST.isEmpty()) moyST = (moyBE + moyHU) / 2.0;
                if (rawHU.isEmpty()) moyHU = (moyBE + moyST) / 2.0;
                scoreBE = scoreBrutEnPourcentage((int) Math.round(moyBE), maxBE);
                scoreST = scoreBrutEnPourcentage((int) Math.round(moyST), maxST);
                scoreHU = scoreBrutEnPourcentage((int) Math.round(moyHU), maxHU);
            }
            if (scoreBienEtre != null) scoreBienEtre.setText(scoreBE + "/100");
            if (scoreStress   != null) scoreStress.setText(scoreST + "/100");
            if (scoreHumeur   != null) scoreHumeur.setText(scoreHU + "/100");
            animerArc(arcBienEtre, scoreBE, "#A78BFA"); animerArc(arcStress, scoreST, "#FF6B9D"); animerArc(arcHumeur, scoreHU, "#4FACFE");
            setTrend(lblTrendBienEtre, tendance(rawBE, maxBE), "#A78BFA"); setTrend(lblTrendStress, tendance(rawST, maxST), "#FF6B9D"); setTrend(lblTrendHumeur, tendance(rawHU, maxHU), "#4FACFE");
            int coins = hist.size() * 150 + hist.stream().mapToInt(this::extraireScore).filter(s -> s > 0).sum() * 5;
            if (lblCoinsGagnes  != null) lblCoinsGagnes.setText("coins : " + coins);
            if (lblSessionCount != null) { int nb = hist.size(); lblSessionCount.setText(nb + " session" + (nb > 1 ? "s" : "") + " complétée" + (nb > 1 ? "s" : "")); }
            afficherBadgesCoins(coins);
            afficherConseil(scoreBE, scoreST, scoreHU, hist.size());
            afficherHistorique(hist, maxBE, maxST, maxHU);
            if (prefsAvatar != null) Platform.runLater(() -> {
                mettreAJourEtatEtAnimation();
                rafraichirImageEmotionnelle();
                chargerImageTherapeutique();
            });
        } catch (SQLException e) { System.err.println("Scores : " + e.getMessage()); }
    }

    private int tendance(List<Integer> raw, int max) {
        if (raw.size() < 2) return 0;
        int m = raw.size() / 2;
        double avant  = raw.subList(0, m).stream().mapToInt(Integer::intValue).average().orElse(0);
        double recent = raw.subList(m, raw.size()).stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.max(-99, Math.min(99, (int) Math.round((recent * 100.0 / max) - (avant * 100.0 / max))));
    }

    // ══════════════════════════════════════════════════════════════
    //  AVATAR
    // ══════════════════════════════════════════════════════════════
    private void chargerAvatar() {
        if (currentPatientId <= 0) return;
        prefsAvatar = avatarService.chargerPrefs(currentPatientId);
        String prenom = Session.getPrenom();
        if (prenom == null || prenom.isBlank()) prenom = Session.getFullName();
        if (prenom != null && !prenom.isBlank()) prefsAvatar.pseudo = prenom;
        else if (prefsAvatar.pseudo.isBlank() || prefsAvatar.pseudo.startsWith("Patient #")) prefsAvatar.pseudo = "Mon Avatar";
        if (lblAvatarPseudo != null) lblAvatarPseudo.setText(prefsAvatar.pseudo);
        mettreAJourEtatEtAnimation();
        rafraichirImageEmotionnelle();
        chargerImageTherapeutique();
    }

    private void mettreAJourEtatEtAnimation() {
        if (prefsAvatar == null) return;
        EtatDetecte etat = AvatarService.detecterEtat(scoreBE, scoreST, scoreHU);
        Expression  expr = avatarService.getExpressionCourante(scoreBE, scoreST, scoreHU);
        if (lblAvatarEtat    != null) { lblAvatarEtat.setText(expr.emoji + " " + expr.titreReaction); lblAvatarEtat.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + expr.couleurBandeau + "; -fx-background-color:" + expr.couleurBandeauBg + "; -fx-background-radius:20; -fx-padding:4 12;"); }
        if (lblAvatarMessage != null) { lblAvatarMessage.setText(expr.messageReaction); lblAvatarMessage.setStyle("-fx-font-size:11px; -fx-text-fill:#4A6470;"); }
        if (avatarEtatBox    != null) avatarEtatBox.setStyle("-fx-background-color:" + expr.couleurBandeauBg + "; -fx-background-radius:14; -fx-padding:12;");
        if (imgAvatar != null) jouerAnimation(expr.animation);
        if (etat != dernierEtatPrononce) { dernierEtatPrononce = etat; ServiceVoixQuiz.parlerAvatar(expr.messageReaction); }
    }

    private void rafraichirImageEmotionnelle() {
        if (prefsAvatar == null) return;
        String url = avatarService.getAvatarUrlEmotionnel(prefsAvatar, scoreBE, scoreST, scoreHU);
        new Thread(() -> {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(url, 120, 120, true, true, false);
                Platform.runLater(() -> { if (!img.isError()) { Expression expr = avatarService.getExpressionCourante(scoreBE, scoreST, scoreHU); afficherImageAvecAnimationEmotionnelle(img, expr.animation); } });
            } catch (Exception e) { System.err.println("Avatar : " + e.getMessage()); }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════
    //  IMAGE THÉRAPEUTIQUE
    // ══════════════════════════════════════════════════════════════
    private void chargerImageTherapeutique() {
        if (imgTherapeutique == null || imageEnChargement) return;
        imageEnChargement = true;
        Platform.runLater(() -> {
            if (paneImageContainer != null) imgTherapeutique.fitWidthProperty().bind(paneImageContainer.widthProperty());
            if (paneImageLoading   != null) paneImageLoading.setVisible(true);
            if (btnRegeneImage     != null) btnRegeneImage.setDisable(true);
            if (lblImageMessage    != null) lblImageMessage.setText("Génération en cours...");
            if (lblImageTheme      != null) lblImageTheme.setText("");
        });
        new Thread(() -> {
            ServiceImageTherapeutique.ResultatImage res = serviceImage.genererImage(scoreBE, scoreST, scoreHU);
            Platform.runLater(() -> {
                imageEnChargement = false;
                if (paneImageLoading != null) paneImageLoading.setVisible(false);
                if (btnRegeneImage   != null) btnRegeneImage.setDisable(false);
                if (!res.isSuccess()) { if (lblImageMessage != null) lblImageMessage.setText("Image indisponible"); return; }
                try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(res.imageBytes)) {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(bis);
                    if (!img.isError()) {
                        imgTherapeutique.setImage(img); imgTherapeutique.setOpacity(0);
                        FadeTransition ft = new FadeTransition(Duration.millis(800), imgTherapeutique);
                        ft.setFromValue(0); ft.setToValue(1); ft.setInterpolator(Interpolator.EASE_OUT); ft.play();
                        if (lblImageMessage != null) lblImageMessage.setText(res.message);
                        if (lblImageTheme   != null) { lblImageTheme.setText(res.theme); if (res.couleurPrimaire != null) lblImageTheme.setStyle("-fx-background-color:" + res.couleurPrimaire + "33; -fx-text-fill:" + res.couleurPrimaire + "; -fx-background-radius:20; -fx-padding:4 14; -fx-font-size:11px; -fx-font-weight:700;"); }
                        dernierResultatImage = res;
                    }
                } catch (Exception e) { if (lblImageMessage != null) lblImageMessage.setText("Erreur affichage"); }
            });
        }).start();
    }

    @FXML private void regenererImage() { ServiceImageTherapeutique.viderCache(); chargerImageTherapeutique(); }

    // ══════════════════════════════════════════════════════════════
    //  ANIMATIONS AVATAR
    // ══════════════════════════════════════════════════════════════
    private void jouerAnimation(AnimationType type) {
        if (imgAvatar == null) return;
        if (animationAvatar != null) { animationAvatar.stop(); imgAvatar.setScaleX(1); imgAvatar.setScaleY(1); imgAvatar.setOpacity(1); imgAvatar.setRotate(0); imgAvatar.setTranslateX(0); }
        switch (type) {
            case RESPIRATION: { ScaleTransition st = new ScaleTransition(Duration.millis(2000), imgAvatar); st.setFromX(0.95); st.setFromY(0.95); st.setToX(1.05); st.setToY(1.05); st.setCycleCount(Animation.INDEFINITE); st.setAutoReverse(true); st.setInterpolator(Interpolator.EASE_BOTH); animationAvatar = st; st.play(); break; }
            case FONDU_DOUX:  { FadeTransition ft = new FadeTransition(Duration.millis(2500), imgAvatar); ft.setFromValue(0.65); ft.setToValue(1.0); ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true); ft.setInterpolator(Interpolator.EASE_BOTH); animationAvatar = ft; ft.play(); break; }
            case SECOUSSE:    { TranslateTransition tt = new TranslateTransition(Duration.millis(80), imgAvatar); tt.setFromX(0); tt.setToX(-8); tt.setCycleCount(6); tt.setAutoReverse(true); tt.setInterpolator(Interpolator.EASE_BOTH); tt.setOnFinished(e -> { imgAvatar.setTranslateX(0); ScaleTransition calm = new ScaleTransition(Duration.millis(3000), imgAvatar); calm.setFromX(0.97); calm.setFromY(0.97); calm.setToX(1.03); calm.setToY(1.03); calm.setCycleCount(Animation.INDEFINITE); calm.setAutoReverse(true); animationAvatar = calm; calm.play(); }); animationAvatar = tt; tt.play(); break; }
            case BOUNCE_JOIE: { ScaleTransition b = new ScaleTransition(Duration.millis(300), imgAvatar); b.setFromX(0.7); b.setFromY(0.7); b.setToX(1.15); b.setToY(1.15); b.setInterpolator(Interpolator.EASE_OUT); b.setOnFinished(e -> { ScaleTransition r = new ScaleTransition(Duration.millis(150), imgAvatar); r.setToX(1.0); r.setToY(1.0); r.setOnFinished(e2 -> { RotateTransition rot = new RotateTransition(Duration.millis(1500), imgAvatar); rot.setFromAngle(-4); rot.setToAngle(4); rot.setCycleCount(Animation.INDEFINITE); rot.setAutoReverse(true); animationAvatar = rot; rot.play(); }); r.play(); }); animationAvatar = b; b.play(); break; }
            case PULSE:       { ScaleTransition p = new ScaleTransition(Duration.millis(800), imgAvatar); p.setFromX(1.0); p.setFromY(1.0); p.setToX(1.12); p.setToY(1.12); p.setCycleCount(4); p.setAutoReverse(true); p.setOnFinished(e -> { imgAvatar.setScaleX(1.0); imgAvatar.setScaleY(1.0); ScaleTransition d = new ScaleTransition(Duration.millis(2000), imgAvatar); d.setFromX(0.98); d.setFromY(0.98); d.setToX(1.04); d.setToY(1.04); d.setCycleCount(Animation.INDEFINITE); d.setAutoReverse(true); animationAvatar = d; d.play(); }); animationAvatar = p; p.play(); break; }
            default:          { FadeTransition ft = new FadeTransition(Duration.millis(500), imgAvatar); ft.setFromValue(0); ft.setToValue(1); ft.setInterpolator(Interpolator.EASE_OUT); animationAvatar = ft; ft.play(); break; }
        }
    }

    private void afficherImageAvecAnimationEmotionnelle(javafx.scene.image.Image img, AnimationType type) {
        imgAvatar.setImage(img); imgAvatar.setFitWidth(120); imgAvatar.setFitHeight(120); imgAvatar.setOpacity(0); imgAvatar.setScaleX(0.6); imgAvatar.setScaleY(0.6);
        FadeTransition  ft = new FadeTransition(Duration.millis(400), imgAvatar); ft.setToValue(1.0); ft.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition st = new ScaleTransition(Duration.millis(400), imgAvatar); st.setToX(1.0); st.setToY(1.0); st.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition appear = new ParallelTransition(ft, st); appear.setOnFinished(e -> jouerAnimation(type)); appear.play();
    }

    @FXML private void personnaliserAvatar() {
        AvatarPersonnalisationController.ouvrir(currentPatientId, avatarService, () -> {
            prefsAvatar = avatarService.chargerPrefs(currentPatientId);
            String prenom = Session.getPrenom(); if (prenom == null || prenom.isBlank()) prenom = Session.getFullName();
            if (prenom != null && !prenom.isBlank()) prefsAvatar.pseudo = prenom;
            if (lblAvatarPseudo != null) lblAvatarPseudo.setText(prefsAvatar.pseudo);
            dernierEtatPrononce = null; mettreAJourEtatEtAnimation(); rafraichirImageEmotionnelle();
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  BADGES
    // ══════════════════════════════════════════════════════════════
    private static final int[][]   PALIERS        = {{5000,0},{15000,1},{35000,2},{70000,3},{120000,4}};
    private static final String[]  BADGE_EMOJIS   = {"⭐","🏅","❤️","🌟","💎"};
    private static final String[]  BADGE_TITRES   = {"Gestion parfaite","Stabilité émotionnelle","Résilience","Mental fort","Progression continue"};
    private static final String[]  BADGE_COULEURS = {"#5C98A8","#4A90BE","#5E9E82","#2D6E7E","#7FB9C7"};
    private static final String[]  BADGE_BG       = {"#D4EBF0","#D9EDF8","#D4EFE4","#C5DDE4","#E0F0F5"};

    private void afficherBadgesCoins(int coins) {
        if (badgesBox == null) return;
        Platform.runLater(() -> {
            badgesBox.getChildren().clear();
            HBox tL = new HBox(10); tL.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label lT = new Label("Mes Badges"); lT.setStyle("-fx-font-size:15px; -fx-font-weight:900; -fx-text-fill:#1F2A33;");
            long nb = Arrays.stream(PALIERS).filter(p -> coins >= p[0]).count();
            Label lC = new Label(nb + "/5"); lC.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#5C98A8; -fx-background-color:rgba(92,152,168,0.12); -fx-background-radius:20; -fx-padding:3 10;");
            Pane sp = new Pane(); HBox.setHgrow(sp, Priority.ALWAYS); tL.getChildren().addAll(lT, sp, lC); badgesBox.getChildren().add(tL);
            int ps = -1; for (int[] p : PALIERS) if (coins < p[0]) { ps = p[0]; break; }
            int pp = 0; for (int[] p : PALIERS) if (coins >= p[0]) pp = p[0];
            double pct = ps > 0 ? (double)(coins - pp) / (ps - pp) : 1.0;
            StackPane barC = new StackPane(); barC.setMaxWidth(Double.MAX_VALUE); barC.setPrefHeight(8); barC.setStyle("-fx-background-color:#EAF3F5; -fx-background-radius:4;");
            Pane barF = new Pane(); barF.setPrefHeight(8);
            barF.setStyle("-fx-background-color:#5C98A8; -fx-background-radius:4;");
            StackPane.setAlignment(barF, javafx.geometry.Pos.CENTER_LEFT); barF.setPrefWidth(0); barC.getChildren().add(barF); badgesBox.getChildren().add(barC);
            Label lp = new Label(ps > 0 ? "Plus que " + (ps - coins) + " coins pour le prochain badge !" : "Tous les badges débloqués !"); lp.setStyle("-fx-font-size:11px; -fx-text-fill:#6E8E9A; -fx-font-style:italic;"); badgesBox.getChildren().add(lp);
            TilePane grille = new TilePane(); grille.setHgap(10); grille.setVgap(10); grille.setPrefColumns(2); grille.setMaxWidth(Double.MAX_VALUE);
            for (int i = 0; i < PALIERS.length; i++) {
                boolean d = coins >= PALIERS[i][0];
                HBox item = new HBox(10); item.setAlignment(javafx.geometry.Pos.CENTER_LEFT); item.setPadding(new Insets(10, 14, 10, 14));
                item.setStyle("-fx-background-color:" + (d ? BADGE_BG[i] : "#f8fafc") + "; -fx-background-radius:14; -fx-border-color:" + (d ? BADGE_COULEURS[i]+"55" : "#e2e8f0") + "; -fx-border-radius:14; -fx-border-width:1.5; -fx-opacity:" + (d ? "1.0" : "0.45") + ";");
                StackPane cercle = new StackPane(); cercle.setMinSize(44,44); cercle.setMaxSize(44,44); cercle.setStyle("-fx-background-color:" + (d ? BADGE_COULEURS[i]+"22" : "#e2e8f0") + "; -fx-background-radius:22;");
                Label le = new Label(d ? BADGE_EMOJIS[i] : "?"); le.setStyle("-fx-font-size:16px; -fx-font-weight:900;"); cercle.getChildren().add(le);
                if (d) { ScaleTransition a = new ScaleTransition(Duration.millis(600), cercle); a.setFromX(0.8); a.setFromY(0.8); a.setToX(1.0); a.setToY(1.0); a.setInterpolator(Interpolator.EASE_OUT); a.play(); }
                VBox textes = new VBox(2);
                Label ln = new Label(BADGE_TITRES[i]); ln.setStyle("-fx-font-size:12px; -fx-font-weight:900; -fx-text-fill:" + (d ? BADGE_COULEURS[i] : "#94a3b8") + ";");
                Label ls = new Label(d ? "Débloqué" : PALIERS[i][0] + " coins requis"); ls.setStyle("-fx-font-size:10px; -fx-text-fill:" + (d ? "#64748b" : "#94a3b8") + ";");
                textes.getChildren().addAll(ln, ls); item.getChildren().addAll(cercle, textes); grille.getChildren().add(item);
            }
            badgesBox.getChildren().add(grille);
            Platform.runLater(() -> { double mW = barC.getWidth() > 0 ? barC.getWidth() : 340; new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(barF.prefWidthProperty(), 0)), new KeyFrame(Duration.millis(900), new KeyValue(barF.prefWidthProperty(), mW * pct, Interpolator.EASE_OUT))).play(); });
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  CONSEILS GROQ
    // ══════════════════════════════════════════════════════════════
    private void afficherConseil(int be, int st, int hu, int nb) {
        if (lblConseil == null) return;
        lblConseil.setText("Conseil en cours...");
        new Thread(() -> {
            String c = serviceGroqQuiz.genererConseil(be, st, hu, nb);
            List<String[]> t = serviceGroqQuiz.genererTroisConseils(be, st, hu);
            Platform.runLater(() -> {
                if (lblConseil != null) lblConseil.setText(c != null ? c : "Continuez vos efforts !");
                if (t != null && t.size() > 0 && t.get(0).length >= 3) { if (lblEmoji1 != null) lblEmoji1.setText(t.get(0)[0]); if (lblTitre1 != null) lblTitre1.setText(t.get(0)[1]); if (lblDesc1 != null) lblDesc1.setText(t.get(0)[2]); }
                if (t != null && t.size() > 1 && t.get(1).length >= 3) { if (lblEmoji2 != null) lblEmoji2.setText(t.get(1)[0]); if (lblTitre2 != null) lblTitre2.setText(t.get(1)[1]); if (lblDesc2 != null) lblDesc2.setText(t.get(1)[2]); }
                if (t != null && t.size() > 2 && t.get(2).length >= 3) { if (lblEmoji3 != null) lblEmoji3.setText(t.get(2)[0]); if (lblTitre3 != null) lblTitre3.setText(t.get(2)[1]); if (lblDesc3 != null) lblDesc3.setText(t.get(2)[2]); }
            });
        }).start();
    }

    // ══════════════════════════════════════════════════════════════
    //  HISTORIQUE
    // ══════════════════════════════════════════════════════════════
    private void afficherHistorique(List<String> h, int maxBE, int maxST, int maxHU) {
        if (historyBox == null) return;
        historyBox.getChildren().clear();
        int debut = Math.max(0, h.size() - 5);
        for (int i = h.size() - 1; i >= debut; i--) {
            String ligne = h.get(i); int sc = extraireScore(ligne); String titre = extraireTitre(ligne).toLowerCase();
            int mx = titre.contains("stress") ? (maxST > 0 ? maxST : 10) : titre.contains("humeur") ? (maxHU > 0 ? maxHU : 10) : (maxBE > 0 ? maxBE : 10);
            int pct = scoreBrutEnPourcentage(sc, mx);
            String e = pct >= 70 ? "✅" : pct >= 40 ? "➡️" : "⚠️";
            Label l = new Label(extraireDate(ligne) + "  ·  " + extraireTitre(ligne) + "  →  " + pct + "/100  " + e);
            l.setStyle("-fx-font-size:12px; -fx-text-fill:#1F2A33; -fx-font-weight:600; -fx-padding:6 0;");
            historyBox.getChildren().addAll(l, new Separator());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GRAPHIQUE
    // ══════════════════════════════════════════════════════════════
    private void chargerGraphiqueReel() {
        if (evolutionChart == null) return;
        evolutionChart.getData().clear();
        try {
            List<String> hist = serviceQuiz.getHistoriquePatient(currentPatientId);
            if (hist.isEmpty()) return;
            List<Integer> rawBE = new ArrayList<>(), rawST = new ArrayList<>(), rawHU = new ArrayList<>();
            for (String l : hist) { int sc = extraireScore(l); if (sc <= 0) continue; String t = extraireTitre(l).toLowerCase(); if (t.contains("stress")) rawST.add(sc); else if (t.contains("humeur")) rawHU.add(sc); else rawBE.add(sc); }
            final int mBE = detecterScoreMax(rawBE), mST = detecterScoreMax(rawST), mHU = detecterScoreMax(rawHU);
            XYChart.Series<String, Number> sBE = new XYChart.Series<>(), sST = new XYChart.Series<>(), sHU = new XYChart.Series<>();
            sBE.setName("Bien-être"); sST.setName("Stress"); sHU.setName("Humeur");
            LocalDateTime lim = LocalDateTime.now().minusDays(getSelectedDays());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
            for (String l : hist) {
                LocalDateTime dt = extraireDateTime(l); if (dt.isBefore(lim)) continue;
                int sc = extraireScore(l); if (sc <= 0) continue;
                String d = dt.format(fmt), titre = extraireTitre(l).toLowerCase();
                if (titre.contains("stress")) { int p = scoreBrutEnPourcentage(sc, mST); sST.getData().add(new XYChart.Data<>(d,p)); sBE.getData().add(new XYChart.Data<>(d,Math.min(100,p+10))); sHU.getData().add(new XYChart.Data<>(d,Math.min(100,p+5))); }
                else if (titre.contains("humeur")) { int p = scoreBrutEnPourcentage(sc, mHU); sHU.getData().add(new XYChart.Data<>(d,p)); sBE.getData().add(new XYChart.Data<>(d,Math.min(100,p+8))); sST.getData().add(new XYChart.Data<>(d,Math.max(0,p-10))); }
                else { int p = scoreBrutEnPourcentage(sc, mBE); sBE.getData().add(new XYChart.Data<>(d,p)); sST.getData().add(new XYChart.Data<>(d,Math.max(0,p-5))); sHU.getData().add(new XYChart.Data<>(d,Math.min(100,p-5))); }
            }
            evolutionChart.getData().addAll(sBE, sST, sHU);
            Platform.runLater(this::applyStyling);
        } catch (SQLException e) { System.err.println("Graphique : " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════
    //  BOUTONS
    // ══════════════════════════════════════════════════════════════
    @FXML private void testerGroq() {
        setResultat("Test Groq...", "#78350F", "#FEF3C7");
        new Thread(() -> {
            try {
                String c = serviceGroqQuiz.genererConseil(scoreBE, scoreST, scoreHU, 5);
                String t = serviceGroqQuiz.recommanderProchainTest(scoreBE, scoreST, scoreHU);
                Platform.runLater(() -> setResultat(c != null && !c.isEmpty() ? "GROQ OK ! " + c + " | " + t : "Réponse vide", c != null ? "#065F46" : "#991B1B", c != null ? "#ECFDF5" : "#FEF2F2"));
            } catch (Exception e) { Platform.runLater(() -> setResultat(e.getMessage(), "#991B1B", "#FEF2F2")); }
        }).start();
    }

    @FXML private void testerEmail() {
        setResultat("Envoi email...", "#78350F", "#FEF3C7");
        new Thread(() -> {
            try { new ServiceEmailQuiz().envoyerEmail("mindcare.notifications@gmail.com", "Test MindCare", "<h2>Email OK</h2>"); Platform.runLater(() -> setResultat("EMAIL OK !", "#065F46", "#ECFDF5")); }
            catch (Exception e) { Platform.runLater(() -> setResultat(e.getMessage(), "#991B1B", "#FEF2F2")); }
        }).start();
    }

    private void setResultat(String t, String cT, String cF) {
        if (lblTestResultat == null) return;
        lblTestResultat.setText(t);
        lblTestResultat.setStyle("-fx-font-size:11px; -fx-font-weight:600; -fx-text-fill:" + cT + "; -fx-background-color:" + cF + "; -fx-background-radius:8; -fx-padding:10;");
    }

    @FXML private void ouvrirChat() {
        ServiceVoixQuiz.arreter();
        try { FXMLLoader l = new FXMLLoader(getClass().getResource("/views/chatquiz.fxml")); Node v = l.load(); VBox ca = (VBox) lblBienvenue.getScene().lookup("#contentArea"); if (ca != null) ca.getChildren().setAll(v); } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void ouvrirEspacePraticien() {
        Session.Role r = Session.getRoleConnecte(); if (r != Session.Role.PSYCHOLOGUE && r != Session.Role.ADMIN) return;
        ServiceVoixQuiz.arreter();
        try { FXMLLoader l = new FXMLLoader(getClass().getResource("/views/EspacepraticienQuiz.fxml")); Node v = l.load(); VBox ca = (VBox) lblBienvenue.getScene().lookup("#contentArea"); if (ca != null) ca.getChildren().setAll(v); } catch (IOException e) { e.printStackTrace(); }
    }

    // ✅ APRÈS — naviguer via la scène directement
    @FXML public void retourSuivie() {
        try {
            if (parentController != null) {
                // Essayer d'appeler via réflexion si la méthode existe
                java.lang.reflect.Method m = parentController.getClass()
                        .getDeclaredMethod("loadAccueil");
                m.setAccessible(true);
                m.invoke(parentController);
            }
        } catch (Exception e) {
            System.err.println("retourSuivie : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS VISUELS
    // ══════════════════════════════════════════════════════════════
    private void animerArc(Arc arc, int score, String hex) {
        if (arc == null) return;
        double tgt = -(score / 100.0) * 360.0;
        arc.setStroke(Color.web(hex)); arc.setStrokeLineCap(StrokeLineCap.ROUND); arc.setFill(Color.TRANSPARENT);
        final int[] f = {0};
        new AnimationTimer() { @Override public void handle(long n) { f[0]++; double p = Math.min(1.0, f[0]/60.0); arc.setLength(tgt*(1-Math.pow(1-p,3))); if (f[0]>=60) stop(); } }.start();
    }

    private void setTrend(Label l, int delta, String couleur) {
        if (l == null) return;
        String s = delta > 0 ? "↑ +" : delta < 0 ? "↓ " : "= ";
        String tF = delta >= 0 ? "#065F46" : "#9D174D";
        String bF = delta >= 0 ? "rgba(16,185,129,0.1)" : "rgba(239,68,68,0.1)";
        l.setText(s + delta + "%");
        l.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:" + tF + "; -fx-background-color:" + bF + "; -fx-background-radius:20; -fx-padding:3 10;");
    }

    private void applyStyling() {
        if (evolutionChart == null) return;
        evolutionChart.setStyle("-fx-background-color:transparent;");
        styleArea(evolutionChart, 0, "rgba(167,139,250,0.22)", "#A78BFA");
        styleArea(evolutionChart, 1, "rgba(255,107,157,0.18)", "#FF6B9D");
        styleArea(evolutionChart, 2, "rgba(79,172,254,0.18)", "#4FACFE");
        Node p = evolutionChart.lookup(".chart-plot-background");
        if (p != null) p.setStyle("-fx-background-color:#F5FAFB;");
    }

    private void styleArea(AreaChart<?, ?> ch, int i, String fill, String stroke) {
        Node f = ch.lookup(".default-color" + i + ".chart-series-area-fill");
        Node l = ch.lookup(".default-color" + i + ".chart-series-area-line");
        if (f != null) f.setStyle("-fx-fill:" + fill + ";");
        if (l != null) l.setStyle("-fx-stroke:" + stroke + "; -fx-stroke-width:2px;");
        ch.lookupAll(".default-color" + i + ".chart-area-symbol").forEach(n -> n.setStyle("-fx-background-color:" + stroke + ",white;"));
    }

    // ══════════════════════════════════════════════════════════════
    //  MUSIQUE
    // ══════════════════════════════════════════════════════════════
    @FXML private void chargerMusique() {
        if (btnChargerMusique != null) { btnChargerMusique.setText("Chargement..."); btnChargerMusique.setDisable(true); }
        if (lblChargementPistes != null) lblChargementPistes.setText("IA analyse votre état...");
        String nom = Session.getFullName() != null ? Session.getFullName() : "Patient";
        new Thread(() -> {
            ServiceMusiqueQuiz.MusiqueParams params = serviceMusiqueQuiz.calculerParams(scoreBE, scoreST, scoreHU, nom);
            List<ServiceMusiqueQuiz.Piste> pistes = serviceMusiqueQuiz.chercherPistes(params);
            pistesChargees = pistes; pisteActuelle = 0;
            Platform.runLater(() -> {
                if (lblMusiqueMessage != null) lblMusiqueMessage.setText(params.message + " BPM: " + params.bpm);
                afficherListePistes(pistes);
                if (btnChargerMusique != null) { btnChargerMusique.setText("Recharger"); btnChargerMusique.setDisable(false); }
                if (!pistes.isEmpty()) jouerPiste(pistes.get(0));
            });
        }).start();
    }

    private void afficherListePistes(List<ServiceMusiqueQuiz.Piste> pistes) {
        if (listePistesBox == null) return;
        listePistesBox.getChildren().removeIf(n -> n.getUserData() != null && n.getUserData().equals("piste"));
        if (lblChargementPistes != null) lblChargementPistes.setVisible(false);
        for (int i = 0; i < pistes.size(); i++) {
            ServiceMusiqueQuiz.Piste piste = pistes.get(i); final int idx = i; final ServiceMusiqueQuiz.Piste pf = piste;
            HBox ligne = new HBox(10); ligne.setAlignment(javafx.geometry.Pos.CENTER_LEFT); ligne.setPadding(new Insets(8,12,8,12)); ligne.setUserData("piste");
            ligne.setStyle("-fx-background-color:" + (i==pisteActuelle ? "rgba(92,152,168,0.12)" : "transparent") + "; -fx-background-radius:10; -fx-cursor:hand;");
            Label em = new Label(piste.emoji); em.setStyle("-fx-font-size:16px;");
            VBox inf = new VBox(2); HBox.setHgrow(inf, Priority.ALWAYS);
            Label nL = new Label(piste.nom); nL.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#1F2A33;");
            Label dL = new Label(piste.duree); dL.setStyle("-fx-font-size:10px; -fx-text-fill:#8AA8B2;");
            inf.getChildren().addAll(nL, dL);
            Label pl = new Label(i==pisteActuelle && enLecture ? "▶" : "○"); pl.setStyle("-fx-font-size:14px; -fx-text-fill:#5C98A8;");
            ligne.getChildren().addAll(em, inf, pl);
            ligne.setOnMouseClicked(e -> { pisteActuelle = idx; jouerPiste(pf); afficherListePistes(pistesChargees); });
            ligne.setOnMouseEntered(e -> ligne.setStyle("-fx-background-color:rgba(92,152,168,0.08); -fx-background-radius:10; -fx-cursor:hand;"));
            ligne.setOnMouseExited(e  -> ligne.setStyle("-fx-background-color:" + (idx==pisteActuelle ? "rgba(92,152,168,0.12)" : "transparent") + "; -fx-background-radius:10; -fx-cursor:hand;"));
            listePistesBox.getChildren().add(ligne);
        }
    }

    private void jouerPiste(ServiceMusiqueQuiz.Piste p) {
        if (lblPisteNom   != null) lblPisteNom.setText(p.nom);
        if (lblPisteDuree != null) lblPisteDuree.setText(p.duree);
        if (lblPisteEmoji != null) lblPisteEmoji.setText(p.emoji);
        if (lblEtatLecture!= null) lblEtatLecture.setText("▶");
        if (btnPlayPause  != null) btnPlayPause.setText("⏸");
        if (p.url == null) {
            System.err.println("❌ URL nulle pour : " + p.nom);
            enLecture = false;
            return;
        }
        System.out.println("🎵 Tentative lecture : " + p.url);
        if (mediaPlayer   != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
        try {
            javafx.scene.media.Media media = new javafx.scene.media.Media(p.url);
            mediaPlayer = new javafx.scene.media.MediaPlayer(media);
            // ✅ Handlers d'erreur ajoutés
            mediaPlayer.setOnError(() ->
                    System.err.println("❌ MediaPlayer error: " + mediaPlayer.getError()));
            media.setOnError(() ->
                    System.err.println("❌ Media error: " + media.getError()));
            double vol = sliderVolume != null ? sliderVolume.getValue() : 0.8;
            mediaPlayer.setVolume(vol);
            mediaPlayer.setOnEndOfMedia(() -> {
                pisteActuelle = (pisteActuelle+1) % pistesChargees.size();
                Platform.runLater(() -> { jouerPiste(pistesChargees.get(pisteActuelle)); afficherListePistes(pistesChargees); });
            });
            mediaPlayer.play();
            enLecture = true;
        } catch (Exception e) {
            System.err.println("❌ Lecture exception : " + e.getMessage());
            enLecture = false;
        }
    }

    @FXML private void togglePlayPause() { if (mediaPlayer == null) { if (!pistesChargees.isEmpty()) jouerPiste(pistesChargees.get(pisteActuelle)); return; } if (enLecture) { mediaPlayer.pause(); enLecture = false; if (btnPlayPause != null) btnPlayPause.setText("▶"); if (lblEtatLecture != null) lblEtatLecture.setText("⏸"); } else { mediaPlayer.play(); enLecture = true; if (btnPlayPause != null) btnPlayPause.setText("⏸"); if (lblEtatLecture != null) lblEtatLecture.setText("▶"); } }
    @FXML private void pisteSuivante()   { if (pistesChargees.isEmpty()) return; pisteActuelle = (pisteActuelle+1) % pistesChargees.size(); jouerPiste(pistesChargees.get(pisteActuelle)); afficherListePistes(pistesChargees); }
    @FXML private void pistePrecedente() { if (pistesChargees.isEmpty()) return; pisteActuelle = (pisteActuelle-1+pistesChargees.size()) % pistesChargees.size(); jouerPiste(pistesChargees.get(pisteActuelle)); afficherListePistes(pistesChargees); }
    @FXML private void stopMusique()     { if (mediaPlayer != null) { mediaPlayer.stop(); enLecture = false; } if (btnPlayPause != null) btnPlayPause.setText("▶"); if (lblEtatLecture != null) lblEtatLecture.setText("⏹"); if (lblPisteNom != null) lblPisteNom.setText("Aucune piste en lecture"); }

    // ══════════════════════════════════════════════════════════════
    //  PARSING
    // ══════════════════════════════════════════════════════════════
    private int           extraireScore(String l)     { try { int s = l.indexOf("Score: ")+7,   e = l.indexOf(" |",s);  return Integer.parseInt(l.substring(s,e).trim()); } catch (Exception e) { return 0; } }
    private LocalDateTime extraireDateTime(String l)  { try { int s = l.indexOf("Date: ")+6;    return LocalDateTime.parse(l.substring(s).trim()); } catch (Exception e) { return LocalDateTime.now(); } }
    private String        extraireDate(String l)      { try { return extraireDateTime(l).format(DateTimeFormatter.ofPattern("dd/MM")); } catch (Exception e) { return "--/--"; } }
    private String        extraireTitre(String l)     { try { int s = l.indexOf("Quiz: ")+6,    e = l.indexOf(" |",s);  return l.substring(s,e).trim(); } catch (Exception e) { return "Quiz"; } }

    private void configurerCombo() {
        if (comboPeriode == null) return;
        comboPeriode.getItems().addAll("7 jours","30 jours","90 jours");
        comboPeriode.getSelectionModel().select("30 jours");
        comboPeriode.setOnAction(e -> chargerGraphiqueReel());
    }
    private int getSelectedDays() {
        if (comboPeriode == null) return 30;
        String s = comboPeriode.getSelectionModel().getSelectedItem();
        if ("7 jours".equals(s))  return 7;
        if ("90 jours".equals(s)) return 90;
        return 30;
    }
}