package controllers;

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
import javafx.stage.Stage;
import services.ServiceReponse;

import java.sql.SQLException;
import java.util.*;

public class ProfilControllerQuiz {

    @FXML private VBox   containerReponses;
    @FXML private Button btnRetour;
    @FXML private Label  lblTitrePatient;

    private int idPatient;
    private static final int SCORE_MAX = 6; // 3 questions × valeur max 2

    private final ServiceReponse serviceReponse = new ServiceReponse();

    public void setIdPatient(int idPatient) {
        this.idPatient = idPatient;
        afficherResultats();
    }

    @FXML
    private void handleRetour() {
        Stage stage = (Stage) btnRetour.getScene().getWindow();
        stage.close();
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
                lblVide.setStyle("-fx-font-size: 13px; -fx-text-fill: #9CA3AF; -fx-padding: 20;");
                containerReponses.getChildren().add(lblVide);
                return;
            }

            // ── Grouper les lignes par quiz ───────────────────────
            // Structure : Map<titreQuiz, List<ligne>>
            Map<String, List<String>> parQuiz = new LinkedHashMap<>();

            for (String ligne : details) {
                // Format : "Quiz: nom | Question: texte | Réponse: texte | Valeur: x"
                String[] parts   = ligne.split("\\|");
                String titreQuiz = parts[0].replace("Quiz:", "").trim();

                parQuiz.computeIfAbsent(titreQuiz, k -> new ArrayList<>()).add(ligne);
            }

            // ── Créer une carte par quiz ──────────────────────────
            for (Map.Entry<String, List<String>> entry : parQuiz.entrySet()) {
                containerReponses.getChildren().add(
                        creerCarteQuiz(entry.getKey(), entry.getValue())
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Carte d'un quiz complet avec toutes ses questions
    // ════════════════════════════════════════════════════════════
    private VBox creerCarteQuiz(String titreQuiz, List<String> lignes) {
        String titreLow = titreQuiz.toLowerCase();

        // Calculer score total du quiz
        int scoreBrut = 0;
        for (String ligne : lignes) {
            scoreBrut += extraireValeur(ligne);
        }

        // Conversion en pourcentage réel
        int scorePourcent;
        if (titreLow.contains("stress") || titreLow.contains("humeur")) {
            scorePourcent = (int) Math.max(0, 100 - (scoreBrut * 100.0) / SCORE_MAX);
        } else {
            scorePourcent = (int) Math.min(100, (scoreBrut * 100.0) / SCORE_MAX);
        }

        // Couleurs MindCare selon le type
        String couleur, couleurFond, emoji;
        if (titreLow.contains("stress")) {
            couleur = "#FF6B9D"; couleurFond = "#FFD4E5"; emoji = "🌸 ";
        } else if (titreLow.contains("humeur")) {
            couleur = "#4FACFE"; couleurFond = "#D4F1FF"; emoji = "💙 ";
        } else if (titreLow.contains("bien") || titreLow.contains("etre")) {
            couleur = "#A78BFA"; couleurFond = "#E9D5FF"; emoji = "🌿 ";
        } else {
            couleur = "#A78BFA"; couleurFond = "#E9D5FF"; emoji = "📝 ";
        }

        // ── Carte principale blanche ──────────────────────────────
        VBox carte = new VBox(12);
        carte.setPadding(new Insets(18));
        carte.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 2);"
        );

        // ── HEADER : arc + titre + score ──────────────────────────
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        // Arc de progression
        StackPane arcPane = new StackPane();
        arcPane.setPrefSize(72, 72);

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
        lblPct.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: " + couleur + ";");
        arcPane.getChildren().addAll(arcFond, arcScore, lblPct);

        // Titre + badge
        VBox titreBox = new VBox(5);
        HBox.setHgrow(titreBox, Priority.ALWAYS);

        Label lblTitre = new Label(emoji + titreQuiz);
        lblTitre.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: " + couleur + ";");

        HBox badgeBox = new HBox(8);
        badgeBox.setAlignment(Pos.CENTER_LEFT);

        Label lblBrut = new Label("Score : " + scoreBrut + "/" + SCORE_MAX);
        lblBrut.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF; -fx-font-weight: 600;");

        // Badge niveau
        String niveauTexte, niveauStyle;
        if (scorePourcent >= 70) {
            niveauTexte = "↑ Bon";
            niveauStyle = "-fx-background-color:rgba(16,185,129,0.1); -fx-text-fill:#065F46;";
        } else if (scorePourcent >= 40) {
            niveauTexte = "→ Moyen";
            niveauStyle = "-fx-background-color:rgba(245,158,11,0.1); -fx-text-fill:#92400E;";
        } else {
            niveauTexte = "↓ À suivre";
            niveauStyle = "-fx-background-color:rgba(239,68,68,0.1); -fx-text-fill:#991B1B;";
        }
        Label lblNiveau = new Label(niveauTexte);
        lblNiveau.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-padding:3 10 3 10;" +
                "-fx-background-radius:20;" + niveauStyle);

        badgeBox.getChildren().addAll(lblBrut, lblNiveau);
        titreBox.getChildren().addAll(lblTitre, badgeBox);
        header.getChildren().addAll(arcPane, titreBox);
        carte.getChildren().add(header);

        // ── SÉPARATEUR ────────────────────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #F3F4F6;");
        carte.getChildren().add(sep);

        // ── QUESTIONS / RÉPONSES ──────────────────────────────────
        for (String ligne : lignes) {
            String question = extraireQuestion(ligne);
            String reponse  = extraireReponse(ligne);
            int    valeur   = extraireValeur(ligne);

            HBox ligneBox = new HBox(10);
            ligneBox.setAlignment(Pos.CENTER_LEFT);
            ligneBox.setPadding(new Insets(4, 0, 4, 0));

            // Point coloré
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: " + couleur + "; -fx-font-size: 10px;");

            // Question
            Label lblQ = new Label(question);
            lblQ.setWrapText(true);
            lblQ.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151; -fx-font-weight: 600;");
            HBox.setHgrow(lblQ, Priority.ALWAYS);

            // Réponse + valeur
            Label lblR = new Label(reponse + "  (" + valeur + "pt)");
            lblR.setStyle("-fx-font-size: 12px; -fx-font-weight: 700;" +
                    "-fx-padding: 3 10 3 10; -fx-background-radius: 12;" +
                    "-fx-background-color: " + couleurFond + ";" +
                    "-fx-text-fill: " + couleur + ";");

            ligneBox.getChildren().addAll(dot, lblQ, lblR);
            carte.getChildren().add(ligneBox);
        }

        return carte;
    }

    // ════════════════════════════════════════════════════════════
    //  Extraction depuis les lignes
    //  Format : "Quiz: nom | Question: texte | Réponse: texte | Valeur: x"
    // ════════════════════════════════════════════════════════════
    private String extraireQuestion(String ligne) {
        try {
            int start = ligne.indexOf("Question:") + 9;
            int end   = ligne.indexOf("|", start);
            return ligne.substring(start, end).trim();
        } catch (Exception e) { return "Question inconnue"; }
    }

    private String extraireReponse(String ligne) {
        try {
            int start = ligne.indexOf("Réponse:") + 8;
            int end   = ligne.indexOf("|", start);
            return ligne.substring(start, end).trim();
        } catch (Exception e) { return "?"; }
    }

    private int extraireValeur(String ligne) {
        try {
            int start = ligne.indexOf("Valeur:") + 7;
            return Integer.parseInt(ligne.substring(start).trim());
        } catch (Exception e) { return 0; }
    }
}