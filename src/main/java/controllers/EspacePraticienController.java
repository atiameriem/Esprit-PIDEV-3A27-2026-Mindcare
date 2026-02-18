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
import java.util.HashMap;
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
            VBox root = loader.load();

            // Passer l'id du patient au controller
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
            // Compter les patients distincts (simplification: nombre de users avec tests)
            // Dans un vrai système, ça viendrait d'une table users avec role=patient
            int totalPatients = 3; // mohamed, meriem, mariem
            lblTotalPatients.setText(String.valueOf(totalPatients));

            // Tests cette semaine (simplification: total passages cette semaine)
            lblTestsSemaine.setText("8");

            // Progression moyenne
            lblProgressionMoyenne.setText("+12%");

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
            // Récupérer tous les patients depuis la base
            Map<Integer, String> patients = serviceQuiz.getTousLesPatients();

            for (Map.Entry<Integer, String> entry : patients.entrySet()) {
                int idPatient  = entry.getKey();
                String nomPatient = entry.getValue();

                listePatients.getChildren().add(
                        creerCartePatient(idPatient, nomPatient)
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

        // ── Avatar + Nom ──────────────────────────────────────────
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
        try {
            List<String> historique = serviceQuiz.getHistoriquePatient(idPatient);

            VBox scoreBox = new VBox(4);
            scoreBox.setAlignment(Pos.CENTER);

            if (!historique.isEmpty()) {
                int dernierScore = extraireScore(historique.get(historique.size() - 1));

                Label lblScore = new Label(dernierScore + "%");
                lblScore.setStyle("-fx-font-size: 24px; -fx-font-weight: 900;" +
                        getCouleurScore(dernierScore));

                Label lblLabel = new Label("Dernier score");
                lblLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF; -fx-font-weight: 600;");

                scoreBox.getChildren().addAll(lblScore, lblLabel);
            } else {
                Label lblAucun = new Label("Aucun test");
                lblAucun.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF;");
                scoreBox.getChildren().add(lblAucun);
            }

            // ── Progression ───────────────────────────────────────
            VBox progBox = new VBox(4);
            progBox.setAlignment(Pos.CENTER);

            if (historique.size() >= 2) {
                int avant = extraireScore(historique.get(historique.size() - 2));
                int apres = extraireScore(historique.get(historique.size() - 1));
                int diff  = apres - avant;

                String texte = (diff > 0 ? "+" : "") + diff + "%";
                String couleur = diff > 0 ? "-fx-text-fill: #10B981;" :
                        diff < 0 ? "-fx-text-fill: #EF4444;" :
                                "-fx-text-fill: #9CA3AF;";

                Label lblDiff = new Label(texte);
                lblDiff.setStyle("-fx-font-size: 20px; -fx-font-weight: 900;" + couleur);

                Label lblLabelDiff = new Label("Évolution");
                lblLabelDiff.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF; -fx-font-weight: 600;");

                progBox.getChildren().addAll(lblDiff, lblLabelDiff);
            }

            // ── Bouton Voir détails ───────────────────────────────
            Button btnVoir = new Button("Voir détails →");
            btnVoir.setStyle("-fx-background-color: #667eea;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: 700;" +
                    "-fx-padding: 10 18 10 18;" +
                    "-fx-background-radius: 10;" +
                    "-fx-cursor: hand;");
            btnVoir.setOnAction(e -> voirDetailsPatient(idPatient, nomPatient));

            carte.getChildren().addAll(avatar, infos, scoreBox, progBox, btnVoir);

        } catch (SQLException e) {
            System.err.println("❌ Erreur carte patient : " + e.getMessage());
        }

        return carte;
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
        } catch (Exception e) {
            return 0;
        }
    }

    private String getCouleurScore(int score) {
        if (score >= 80) return "-fx-text-fill: #10B981;";
        if (score >= 60) return "-fx-text-fill: #F59E0B;";
        return "-fx-text-fill: #EF4444;";
    }

    // ══════════════════════════════════════════════════════════════
    // Actions
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