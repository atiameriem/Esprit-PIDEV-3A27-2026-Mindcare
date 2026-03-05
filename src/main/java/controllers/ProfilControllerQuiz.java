package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import services.ServiceReponse;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class ProfilControllerQuiz {

    @FXML private VBox   containerReponses;
    @FXML private Button btnRetour;
    @FXML private Label  lblTitrePatient;

    private int idPatient;

    // ── Palette MindCare EspacePraticien ─────────────────────────
    private static final String TEAL_DARK   = "#2D6E7E";
    private static final String TEAL_HOVER  = "#225A69";
    private static final String TEAL_MED    = "#5C98A8";
    private static final String TEAL_LIGHT  = "#D4EBF0";
    private static final String BG_PAGE     = "#EAF3F5";
    private static final String TEXT_DARK   = "#1F2A33";
    private static final String TEXT_GREY   = "#6E8E9A";

    // ── Couleurs par type de quiz ─────────────────────────────────
    private static final String[][] QUIZ_COLORS = {
            { "stress",  "#E07A7A", "#FAE5E5", "🌸 " },
            { "humeur",  "#4A90BE", "#D9EDF8", "💙 " },
            { "bien",    "#5E9E82", "#D4EFE4", "🌿 " },
            { "etre",    "#5E9E82", "#D4EFE4", "🌿 " },
            { "anxiete", "#9B7EC8", "#EDE5F8", "🧠 " },
            { "anxiété", "#9B7EC8", "#EDE5F8", "🧠 " },
            { "motiv",   "#D4822A", "#FAEBD7", "⚡ " },
    };

    private final ServiceReponse serviceReponse = new ServiceReponse();

    // ════════════════════════════════════════════════════════════
    //  initialize() — Platform.runLater force le style APRÈS
    //  que JavaFX applique son CSS Modena par défaut
    // ════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            CornerRadii radius = new CornerRadii(20);

            btnRetour.setBackground(new Background(
                    new BackgroundFill(Color.web(TEAL_DARK), radius, Insets.EMPTY)
            ));
            btnRetour.setBorder(Border.EMPTY);
            btnRetour.setTextFill(Color.WHITE);
            btnRetour.setStyle(
                    "-fx-font-size: 12px;" +
                            "-fx-font-weight: 800;" +
                            "-fx-padding: 10 20 10 20;" +
                            "-fx-cursor: hand;" +
                            "-fx-background-insets: 0;" +
                            "-fx-border-insets: 0;" +
                            "-fx-background-radius: 20;"
            );

            // Hover
            btnRetour.setOnMouseEntered(e -> btnRetour.setBackground(new Background(
                    new BackgroundFill(Color.web(TEAL_HOVER), radius, Insets.EMPTY))));
            btnRetour.setOnMouseExited(e -> btnRetour.setBackground(new Background(
                    new BackgroundFill(Color.web(TEAL_DARK), radius, Insets.EMPTY))));
        });
    }

    // ════════════════════════════════════════════════════════════
    //  Vérification rôle + chargement
    // ════════════════════════════════════════════════════════════
    public void setIdPatient(int idPatient) {
        var role = utils.Session.getRoleConnecte();
        if (role != null
                && role != utils.Session.Role.PSYCHOLOGUE
                && role != utils.Session.Role.ADMIN) {
            Label lbl = new Label("⛔ Accès réservé aux psychologues.");
            lbl.setStyle("-fx-font-size:14px; -fx-text-fill:#E07A7A; -fx-padding:20;");
            containerReponses.getChildren().add(lbl);
            return;
        }
        this.idPatient = idPatient;
        afficherResultats();
    }

    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/EspacepraticienQuiz.fxml")
            );
            Node vue = loader.load();
            VBox contentArea = (VBox) btnRetour.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(vue);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Afficher les résultats groupés par quiz
    // ════════════════════════════════════════════════════════════
    private void afficherResultats() {
        containerReponses.getChildren().clear();

        try {
            List<String> details = serviceReponse.getDetailsReponsesPatient(idPatient);

            if (details.isEmpty()) {
                Label lblVide = new Label("Aucun résultat disponible pour ce patient.");
                lblVide.setStyle("-fx-font-size:13px; -fx-text-fill:" + TEXT_GREY + "; -fx-padding:20;");
                containerReponses.getChildren().add(lblVide);
                return;
            }

            // Grouper par quiz
            Map<String, List<String>> parQuiz = new LinkedHashMap<>();
            for (String ligne : details) {
                String[] parts   = ligne.split("\\|");
                String titreQuiz = parts[0].replace("Quiz:", "").trim();
                parQuiz.computeIfAbsent(titreQuiz, k -> new ArrayList<>()).add(ligne);
            }

            for (Map.Entry<String, List<String>> entry : parQuiz.entrySet()) {
                int scoreMaxReel = serviceReponse.getScoreMaxQuiz(entry.getKey());
                containerReponses.getChildren().add(
                        creerCarteQuiz(entry.getKey(), entry.getValue(), scoreMaxReel)
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Carte quiz — palette EspacePraticien
    // ════════════════════════════════════════════════════════════
    private VBox creerCarteQuiz(String titreQuiz, List<String> lignes, int scoreMax) {
        String titreLow = titreQuiz.toLowerCase();

        // Score obtenu
        int scoreBrut = 0;
        for (String ligne : lignes) scoreBrut += extraireValeur(ligne);

        // Sécurité division par zéro
        if (scoreMax <= 0) scoreMax = Math.max(scoreBrut, 1);

        // Pourcentage
        int scorePourcent;
        if (titreLow.contains("stress") || titreLow.contains("humeur")) {
            scorePourcent = (int) Math.max(0, 100 - (scoreBrut * 100.0) / scoreMax);
        } else {
            scorePourcent = (int) Math.min(100, (scoreBrut * 100.0) / scoreMax);
        }

        int nombreQuestions = lignes.size();

        // Couleur selon type
        String couleur    = TEAL_MED;
        String couleurFond = TEAL_LIGHT;
        String emoji      = "📝 ";
        for (String[] c : QUIZ_COLORS) {
            if (titreLow.contains(c[0])) {
                couleur = c[1]; couleurFond = c[2]; emoji = c[3];
                break;
            }
        }

        // ── Carte blanche avec bordure teal ───────────────────────
        VBox carte = new VBox(12);
        carte.setPadding(new Insets(20));
        carte.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(92,152,168,0.22);" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1.2;" +
                        "-fx-effect: dropshadow(gaussian, rgba(45,110,126,0.08), 12, 0, 0, 3);"
        );

        // ── Header ────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        // Arc de progression
        StackPane arcPane = new StackPane();
        arcPane.setPrefSize(72, 72);
        arcPane.setMinSize(72, 72);

        Arc arcFond = new Arc(36, 36, 28, 28, 0, 360);
        arcFond.setType(ArcType.OPEN);
        arcFond.setStroke(Color.web(couleurFond));
        arcFond.setStrokeWidth(7);
        arcFond.setFill(Color.TRANSPARENT);

        Arc arcScore = new Arc(36, 36, 28, 28, 90, -(scorePourcent / 100.0) * 360);
        arcScore.setType(ArcType.OPEN);
        arcScore.setStroke(Color.web(couleur));
        arcScore.setStrokeWidth(7);
        arcScore.setFill(Color.TRANSPARENT);
        arcScore.setStrokeLineCap(StrokeLineCap.ROUND);

        Label lblPct = new Label(scorePourcent + "%");
        lblPct.setStyle("-fx-font-size:12px; -fx-font-weight:900; -fx-text-fill:" + couleur + ";");
        arcPane.getChildren().addAll(arcFond, arcScore, lblPct);

        // Titre + badges
        VBox titreBox = new VBox(6);
        HBox.setHgrow(titreBox, Priority.ALWAYS);

        Label lblTitre = new Label(emoji + titreQuiz);
        lblTitre.setStyle(
                "-fx-font-size:15px; -fx-font-weight:900; -fx-text-fill:" + TEXT_DARK + ";"
        );

        HBox badgeBox = new HBox(10);
        badgeBox.setAlignment(Pos.CENTER_LEFT);

        Label lblBrut = new Label("Score : " + scoreBrut + "/" + scoreMax);
        lblBrut.setStyle("-fx-font-size:11px; -fx-text-fill:" + TEXT_GREY + "; -fx-font-weight:600;");

        // Badge niveau
        String niveauTexte, niveauBg, niveauFg;
        if (scorePourcent >= 70) {
            niveauTexte = "↑ Bon";
            niveauBg = "rgba(94,158,130,0.15)"; niveauFg = "#2E7D5A";
        } else if (scorePourcent >= 40) {
            niveauTexte = "→ Moyen";
            niveauBg = "rgba(212,130,42,0.12)";  niveauFg = "#8A5A1A";
        } else {
            niveauTexte = "↓ À suivre";
            niveauBg = "rgba(224,122,122,0.15)"; niveauFg = "#8B3030";
        }
        Label lblNiveau = new Label(niveauTexte);
        lblNiveau.setStyle(
                "-fx-font-size:11px; -fx-font-weight:700;" +
                        "-fx-padding:4 12 4 12; -fx-background-radius:20;" +
                        "-fx-background-color:" + niveauBg + "; -fx-text-fill:" + niveauFg + ";"
        );

        Label lblNbQ = new Label(nombreQuestions + " question" + (nombreQuestions > 1 ? "s" : ""));
        lblNbQ.setStyle("-fx-font-size:10px; -fx-text-fill:" + TEXT_GREY + "; -fx-font-weight:500;");

        badgeBox.getChildren().addAll(lblBrut, lblNiveau, lblNbQ);
        titreBox.getChildren().addAll(lblTitre, badgeBox);
        header.getChildren().addAll(arcPane, titreBox);
        carte.getChildren().add(header);

        // ── Séparateur ────────────────────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(92,152,168,0.20);");
        carte.getChildren().add(sep);

        // ── Questions / Réponses ──────────────────────────────────
        for (String ligne : lignes) {
            String question = extraireQuestion(ligne);
            String reponse  = extraireReponse(ligne);
            int    valeur   = extraireValeur(ligne);

            HBox ligneBox = new HBox(12);
            ligneBox.setAlignment(Pos.CENTER_LEFT);
            ligneBox.setPadding(new Insets(5, 0, 5, 0));

            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill:" + couleur + "; -fx-font-size:10px;");

            Label lblQ = new Label(question);
            lblQ.setWrapText(true);
            lblQ.setStyle("-fx-font-size:12px; -fx-text-fill:" + TEXT_DARK + "; -fx-font-weight:600;");
            HBox.setHgrow(lblQ, Priority.ALWAYS);

            Label lblR = new Label(reponse + "  (" + valeur + "pt)");
            lblR.setStyle(
                    "-fx-font-size:12px; -fx-font-weight:700;" +
                            "-fx-padding:4 12 4 12; -fx-background-radius:14;" +
                            "-fx-background-color:" + couleurFond + ";" +
                            "-fx-text-fill:" + couleur + ";"
            );

            ligneBox.getChildren().addAll(dot, lblQ, lblR);
            carte.getChildren().add(ligneBox);
        }

        return carte;
    }

    // ════════════════════════════════════════════════════════════
    //  Extraction
    // ════════════════════════════════════════════════════════════
    private String extraireQuestion(String ligne) {
        try { int s = ligne.indexOf("Question:") + 9; return ligne.substring(s, ligne.indexOf("|", s)).trim(); }
        catch (Exception e) { return "Question inconnue"; }
    }
    private String extraireReponse(String ligne) {
        try { int s = ligne.indexOf("Réponse:") + 8; return ligne.substring(s, ligne.indexOf("|", s)).trim(); }
        catch (Exception e) { return "?"; }
    }
    private int extraireValeur(String ligne) {
        try { return Integer.parseInt(ligne.substring(ligne.indexOf("Valeur:") + 7).trim()); }
        catch (Exception e) { return 0; }
    }
}