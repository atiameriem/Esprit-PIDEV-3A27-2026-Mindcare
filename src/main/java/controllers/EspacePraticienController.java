package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import services.ServiceQuiz;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class EspacePraticienController {

    @FXML private Label      lblTotalPatients;
    @FXML private Label      lblTestsSemaine;
    @FXML private Label      lblProgressionMoyenne;
    @FXML private TextField  fieldRecherche;
    @FXML private ComboBox<String> comboFiltre;
    @FXML private VBox       listePatients;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();

    // SCORE_MAX : 3 questions × valeur max 2 = 6
    private static final int SCORE_MAX = 6;

    @FXML
    public void initialize() {
        configurerFiltres();
        chargerStatsGlobales();
        chargerPatients();
    }

    @FXML
    private void voirDetailsPatient(int idPatient, String nomPatient) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/Profilquiz.fxml")
            );
            Parent root = loader.load();
            ProfilControllerQuiz controller = loader.getController();
            controller.setIdPatient(idPatient);
            Stage stage = new Stage();
            stage.setTitle("Détails du patient : " + nomPatient);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Configuration
    // ══════════════════════════════════════════════════════════════
    private void configurerFiltres() {
        comboFiltre.setItems(FXCollections.observableArrayList(
                "Tous", "En progression", "Stables", "En difficulté", "Nouveaux"
        ));
        comboFiltre.getSelectionModel().selectFirst();
        comboFiltre.setOnAction(e -> chargerPatients());

        fieldRecherche.textProperty().addListener((obs, old, newVal) -> {
            // TODO: filtrer la liste en temps réel
        });
    }

    // ══════════════════════════════════════════════════════════════
    // Stats globales
    // ══════════════════════════════════════════════════════════════
    private void chargerStatsGlobales() {
        try {
            Map<Integer, String> patients = serviceQuiz.getTousLesPatients();

            // ── 1. Nombre réel de patients ───────────────────────
            lblTotalPatients.setText(String.valueOf(patients.size()));

            // ── 2. Nombre total de tests + 3. Progression moyenne ─
            int totalTests = 0;
            int totalDiff  = 0;
            int countDiff  = 0;

            for (int idPatient : patients.keySet()) {
                List<String> historique = serviceQuiz.getHistoriquePatient(idPatient);
                totalTests += historique.size();

                if (historique.size() >= 2) {
                    String apresLigne  = historique.get(historique.size() - 1);
                    String typeApres   = extraireTitre(apresLigne).toLowerCase();

                    // ✅ Chercher le dernier test DU MÊME TYPE avant celui-ci
                    String avantLigne = null;
                    for (int j = historique.size() - 2; j >= 0; j--) {
                        String typeJ = extraireTitre(historique.get(j)).toLowerCase();
                        if (typeJ.equals(typeApres)) {
                            avantLigne = historique.get(j);
                            break;
                        }
                    }

                    // Comparer seulement si même type trouvé
                    if (avantLigne != null) {
                        int scoreAvant = convertirEnPourcent(extraireScore(avantLigne), typeApres);
                        int scoreApres = convertirEnPourcent(extraireScore(apresLigne), typeApres);
                        totalDiff += (scoreApres - scoreAvant);
                        countDiff++;
                    }
                }
            }

            lblTestsSemaine.setText(String.valueOf(totalTests));

            if (countDiff > 0) {
                int moyDiff = totalDiff / countDiff;
                String signe = moyDiff >= 0 ? "+" : "";
                lblProgressionMoyenne.setText(signe + moyDiff + "%");
            } else {
                lblProgressionMoyenne.setText("N/A");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur stats globales : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Charger liste patients
    // ══════════════════════════════════════════════════════════════
    private void chargerPatients() {
        listePatients.getChildren().clear();
        try {
            Map<Integer, String> patients = serviceQuiz.getTousLesPatients();
            for (Map.Entry<Integer, String> entry : patients.entrySet()) {
                listePatients.getChildren().add(
                        creerCartePatient(entry.getKey(), entry.getValue())
                );
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement patients : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Créer une carte patient
    // ══════════════════════════════════════════════════════════════
    private HBox creerCartePatient(int idPatient, String nomPatient) {
        HBox carte = new HBox(16);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setPadding(new Insets(20));
        carte.setStyle("-fx-background-color: white;" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: #F3F4F6;" +
                "-fx-border-radius: 16;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        // ── Avatar ────────────────────────────────────────────────
        StackPane avatar = new StackPane();
        avatar.setPrefSize(56, 56);
        avatar.setStyle("-fx-background-color: linear-gradient(135deg, #A78BFA, #FF6B9D);" +
                "-fx-background-radius: 50%;");
        Label initiales = new Label(getInitiales(nomPatient));
        initiales.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: white;");
        avatar.getChildren().add(initiales);

        VBox infos = new VBox(4);
        Label nom = new Label(nomPatient);
        nom.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #1F2937;");
        Label meta = new Label("Dernier test: il y a 2 jours");
        meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF;");
        infos.getChildren().addAll(nom, meta);
        HBox.setHgrow(infos, Priority.ALWAYS);

        // ── Score dernier test ────────────────────────────────────
        VBox scoreBox = new VBox(4);
        scoreBox.setAlignment(Pos.CENTER);

        VBox progBox = new VBox(4);
        progBox.setAlignment(Pos.CENTER);

        try {
            List<String> historique = serviceQuiz.getHistoriquePatient(idPatient);

            if (!historique.isEmpty()) {
                String derniereLigne = historique.get(historique.size() - 1);
                int scoreBrut        = extraireScore(derniereLigne);
                String titreLow      = extraireTitre(derniereLigne).toLowerCase();

                // ✅ Conversion en pourcentage réel
                int dernierScore = convertirEnPourcent(scoreBrut, titreLow);

                Label lblScore = new Label(dernierScore + "%");
                lblScore.setStyle("-fx-font-size: 24px; -fx-font-weight: 900;" +
                        getCouleurScore(dernierScore));

                Label lblLabel = new Label("Dernier score");
                lblLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF; -fx-font-weight: 600;");

                scoreBox.getChildren().addAll(lblScore, lblLabel);

                // ── Progression ───────────────────────────────────
                if (historique.size() >= 2) {
                    String avantLigne = historique.get(historique.size() - 2);
                    int avantBrut     = extraireScore(avantLigne);
                    String avantTitre = extraireTitre(avantLigne).toLowerCase();

                    // ✅ Convertir les deux scores avant de calculer la différence
                    int scoreAvant = convertirEnPourcent(avantBrut, avantTitre);
                    int diff       = dernierScore - scoreAvant;

                    String texte  = (diff > 0 ? "+" : "") + diff + "%";
                    String couleur = diff > 0 ? "-fx-text-fill: #10B981;" :
                            diff < 0 ? "-fx-text-fill: #EF4444;" :
                                    "-fx-text-fill: #9CA3AF;";

                    Label lblDiff = new Label(texte);
                    lblDiff.setStyle("-fx-font-size: 20px; -fx-font-weight: 900;" + couleur);

                    Label lblLabelDiff = new Label("Évolution");
                    lblLabelDiff.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF; -fx-font-weight: 600;");

                    progBox.getChildren().addAll(lblDiff, lblLabelDiff);
                }

            } else {
                Label lblAucun = new Label("Aucun test");
                lblAucun.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF;");
                scoreBox.getChildren().add(lblAucun);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur carte patient : " + e.getMessage());
        }

        // ── Bouton Voir détails ───────────────────────────────────
        Button btnVoir = new Button("Voir détails →");
        btnVoir.setStyle("-fx-background-color: #667eea;" +
                "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 700;" +
                "-fx-padding: 10 18 10 18; -fx-background-radius: 10; -fx-cursor: hand;");
        btnVoir.setOnAction(e -> voirDetailsPatient(idPatient, nomPatient));

        carte.getChildren().addAll(avatar, infos, scoreBox, progBox, btnVoir);
        return carte;
    }

    // ══════════════════════════════════════════════════════════════
    // Conversion score brut → pourcentage réel
    // ══════════════════════════════════════════════════════════════
    /**
     * Stress et Humeur : score élevé = mauvais → on inverse
     * Bien-être et autres : score élevé = bon → conversion directe
     */
    private int convertirEnPourcent(int scoreBrut, String titreLow) {
        if (titreLow.contains("stress") || titreLow.contains("humeur")) {
            return (int) Math.max(0, 100 - (scoreBrut * 100.0) / SCORE_MAX);
        } else {
            return (int) Math.min(100, (scoreBrut * 100.0) / SCORE_MAX);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════
    private String getInitiales(String nom) {
        String[] parts = nom.split(" ");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return nom.substring(0, Math.min(2, nom.length())).toUpperCase();
    }

    private int extraireScore(String ligne) {
        try {
            int start = ligne.indexOf("Score: ") + 7;
            int end   = ligne.indexOf(" |", start);
            return Integer.parseInt(ligne.substring(start, end).trim());
        } catch (Exception e) { return 0; }
    }

    private String extraireTitre(String ligne) {
        try {
            int start = ligne.indexOf("Quiz: ") + 6;
            int end   = ligne.indexOf(" |", start);
            return ligne.substring(start, end).trim();
        } catch (Exception e) { return ""; }
    }

    private String getCouleurScore(int score) {
        if (score >= 70) return "-fx-text-fill: #10B981;"; // vert
        if (score >= 40) return "-fx-text-fill: #F59E0B;"; // orange
        return "-fx-text-fill: #EF4444;";                  // rouge
    }

    // ══════════════════════════════════════════════════════════════
    // Navigation
    // ══════════════════════════════════════════════════════════════
    @FXML
    private void retourSuivie() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/Suivie.fxml")
            );
            Node vue = loader.load();
            VBox parent = (VBox) listePatients.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (IOException e) {
            System.err.println("❌ Erreur retour : " + e.getMessage());
        }
    }
}