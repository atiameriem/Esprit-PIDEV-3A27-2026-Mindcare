package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.ServiceQuiz;
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class EspacePraticienController {

    @FXML private Label      lblTotalPatients;
    @FXML private Label      lblTestsSemaine;
    @FXML private Label      lblProgressionMoyenne;
    @FXML private Label      lblNomPsy;
    @FXML private TextField  fieldRecherche;
    @FXML private ComboBox<String> comboFiltre;
    @FXML private VBox       listePatients;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private Map<Integer, String> tousLesPatients = new LinkedHashMap<>();
    private static final int SCORE_MAX = 6;

    @FXML
    public void initialize() {
        // ✅ Vérification rôle — réservé aux psychologues et admins
        var role = Session.getRoleConnecte();
        if (role != null
                && role != utils.Session.Role.PSYCHOLOGUE
                && role != utils.Session.Role.ADMIN) {
            if (listePatients != null) {
                Label lbl = new Label("⛔ Accès réservé aux psychologues.");
                lbl.setStyle("-fx-font-size:14px; -fx-text-fill:#e74c3c; -fx-padding:20;");
                listePatients.getChildren().add(lbl);
            }
            return;
        }

        if (lblNomPsy != null) {
            lblNomPsy.setText("Dr. " + (Session.getFullName() != null
                    ? Session.getFullName() : "Psychologue"));
        }
        configurerFiltres();
        chargerStatsGlobales();
        chargerPatients();
    }

    private void voirDetailsPatient(int idPatient, String nomPatient) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/Profilquiz.fxml")
            );
            Node vue = loader.load();
            ProfilControllerQuiz controller = loader.getController();
            controller.setIdPatient(idPatient);

            VBox contentArea = (VBox) listePatients.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(vue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configurerFiltres() {
        comboFiltre.setItems(FXCollections.observableArrayList(
                "Tous", "En progression", "Stables", "En difficulté", "Nouveaux"
        ));
        comboFiltre.getSelectionModel().selectFirst();
        comboFiltre.setOnAction(e -> filtrerEtAfficher());
        fieldRecherche.textProperty().addListener((obs, old, newVal) -> filtrerEtAfficher());
    }

    private void chargerStatsGlobales() {
        try {
            Map<Integer, String> patients = serviceQuiz.getTousLesPatients();
            lblTotalPatients.setText(String.valueOf(patients.size()));

            int totalTests = 0;
            int totalDiff  = 0;
            int countDiff  = 0;

            for (int idPatient : patients.keySet()) {
                List<String> historique = serviceQuiz.getHistoriquePatient(idPatient);
                totalTests += historique.size();

                if (historique.size() >= 2) {
                    String apresLigne = historique.get(historique.size() - 1);
                    String typeApres  = extraireTitre(apresLigne).toLowerCase();
                    String avantLigne = null;
                    for (int j = historique.size() - 2; j >= 0; j--) {
                        if (extraireTitre(historique.get(j)).toLowerCase().equals(typeApres)) {
                            avantLigne = historique.get(j);
                            break;
                        }
                    }
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
                lblProgressionMoyenne.setText((moyDiff >= 0 ? "+" : "") + moyDiff + "%");
            } else {
                lblProgressionMoyenne.setText("N/A");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur stats globales : " + e.getMessage());
        }
    }

    private void chargerPatients() {
        try {
            tousLesPatients = serviceQuiz.getTousLesPatients();
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement patients : " + e.getMessage());
            return;
        }
        filtrerEtAfficher();
    }

    private void filtrerEtAfficher() {
        listePatients.getChildren().clear();
        String recherche = fieldRecherche.getText().toLowerCase().trim();
        String filtre    = comboFiltre.getSelectionModel().getSelectedItem();

        for (Map.Entry<Integer, String> entry : tousLesPatients.entrySet()) {
            int    idPatient  = entry.getKey();
            String nomPatient = entry.getValue();

            if (!recherche.isEmpty() && !nomPatient.toLowerCase().contains(recherche)) continue;

            if (filtre != null && !filtre.equals("Tous")) {
                try {
                    List<String> historique = serviceQuiz.getHistoriquePatient(idPatient);
                    boolean inclure = switch (filtre) {
                        case "Nouveaux"       -> historique.isEmpty();
                        case "En progression" -> calculerProgression(historique) > 0;
                        case "Stables"        -> calculerProgression(historique) == 0 && !historique.isEmpty();
                        case "En difficulté"  -> calculerProgression(historique) < 0;
                        default               -> true;
                    };
                    if (!inclure) continue;
                } catch (SQLException e) {
                    System.err.println("Erreur filtre : " + e.getMessage());
                }
            }

            listePatients.getChildren().add(creerCartePatient(idPatient, nomPatient));
        }

        if (listePatients.getChildren().isEmpty()) {
            Label lblVide = new Label("Aucun patient trouvé.");
            lblVide.setStyle("-fx-font-size: 13px; -fx-text-fill: #9CA3AF; -fx-padding: 20;");
            listePatients.getChildren().add(lblVide);
        }
    }

    private int calculerProgression(List<String> historique) {
        if (historique.size() < 2) return 0;
        int scoreApres = extraireScore(historique.get(historique.size() - 1));
        int scoreAvant = extraireScore(historique.get(historique.size() - 2));
        return scoreApres - scoreAvant;
    }

    private VBox creerCartePatient(int idPatient, String nomPatient) {
        VBox carte = new VBox(12);
        carte.setPadding(new Insets(20));
        carte.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 16;
                -fx-border-color: #F3F4F6;
                -fx-border-radius: 16;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);
                """);

        HBox ligneHaut = new HBox(14);
        ligneHaut.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.setPrefSize(52, 52);
        avatar.setMinSize(52, 52);
        avatar.setStyle("-fx-background-color: #EDE9FE; -fx-background-radius: 26;");
        Label initiales = new Label(getInitiales(nomPatient));
        initiales.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #7C3AED;");
        avatar.getChildren().add(initiales);

        VBox infosNom = new VBox(3);
        HBox.setHgrow(infosNom, Priority.ALWAYS);
        Label nom = new Label(nomPatient);
        nom.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #1F2937;");
        Label idLabel = new Label("ID patient : " + idPatient);
        idLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        infosNom.getChildren().addAll(nom, idLabel);

        ligneHaut.getChildren().addAll(avatar, infosNom);

        HBox ligneStats = new HBox(0);
        ligneStats.setAlignment(Pos.CENTER);
        ligneStats.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 10;");

        try {
            List<String> historique = serviceQuiz.getHistoriquePatient(idPatient);

            if (historique.isEmpty()) {
                Label lblNouv = new Label("🆕  Aucun test effectué");
                lblNouv.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280; -fx-padding: 12;");
                ligneStats.getChildren().add(lblNouv);
            } else {
                String derniereLigne = historique.get(historique.size() - 1);
                int    scoreBrut     = extraireScore(derniereLigne);
                String titreLow      = extraireTitre(derniereLigne).toLowerCase();
                String derniereDate  = extraireDate(derniereLigne);
                int    dernierScore  = convertirEnPourcent(scoreBrut, titreLow);

                int diff = 0;
                if (historique.size() >= 2) {
                    String avantLigne = historique.get(historique.size() - 2);
                    int scoreAvant    = convertirEnPourcent(extraireScore(avantLigne),
                            extraireTitre(avantLigne).toLowerCase());
                    diff = dernierScore - scoreAvant;
                }

                ligneStats.getChildren().addAll(
                        creerBlocStat("📋", String.valueOf(historique.size()), "Sessions"),
                        separateurVertical(),
                        creerBlocStat("🎯", dernierScore + "%", "Dernier score",
                                dernierScore >= 70 ? "#10B981" : dernierScore >= 40 ? "#F59E0B" : "#EF4444"),
                        separateurVertical(),
                        creerBlocStat(diff >= 0 ? "📈" : "📉",
                                (diff >= 0 ? "+" : "") + diff + "%", "Évolution",
                                diff > 0 ? "#10B981" : diff < 0 ? "#EF4444" : "#9CA3AF"),
                        separateurVertical(),
                        creerBlocStat("📅", derniereDate, "Dernier test")
                );
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur carte : " + e.getMessage());
        }

        HBox ligneTags = new HBox(8);
        ligneTags.setAlignment(Pos.CENTER_LEFT);
        try {
            List<String> historique = serviceQuiz.getHistoriquePatient(idPatient);
            java.util.Set<String> types = new java.util.LinkedHashSet<>();
            for (String ligne : historique) {
                String t = extraireTitre(ligne).toUpperCase();
                if (t.contains("STRESS"))   types.add("STRESS");
                if (t.contains("HUMEUR"))   types.add("HUMEUR");
                if (t.contains("BIEN"))     types.add("BIEN-ÊTRE");
                if (t.contains("COGNITIF")) types.add("COGNITIF");
            }
            if (types.isEmpty()) {
                Label aucun = new Label("Aucun test passé");
                aucun.setStyle("-fx-font-size: 11px; -fx-text-fill: #D1D5DB;");
                ligneTags.getChildren().add(aucun);
            } else {
                for (String type : types) ligneTags.getChildren().add(creerTag(type));
            }
        } catch (SQLException e) {
            System.err.println("❌ " + e.getMessage());
        }

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnVoir = new Button("📊  Voir détails");
        btnVoir.setStyle("""
                -fx-background-color: #4F46E5;
                -fx-text-fill: white; -fx-font-size: 12px;
                -fx-font-weight: 700; -fx-padding: 10 18 10 18;
                -fx-background-radius: 10; -fx-cursor: hand;
                """);
        btnVoir.setOnMouseEntered(e -> btnVoir.setStyle("""
                -fx-background-color: #3730A3;
                -fx-text-fill: white; -fx-font-size: 12px;
                -fx-font-weight: 700; -fx-padding: 10 18 10 18;
                -fx-background-radius: 10; -fx-cursor: hand;
                """));
        btnVoir.setOnMouseExited(e -> btnVoir.setStyle("""
                -fx-background-color: #4F46E5;
                -fx-text-fill: white; -fx-font-size: 12px;
                -fx-font-weight: 700; -fx-padding: 10 18 10 18;
                -fx-background-radius: 10; -fx-cursor: hand;
                """));
        btnVoir.setOnAction(e -> voirDetailsPatient(idPatient, nomPatient));

        Button btnAssigner = new Button("➕  Assigner test");
        btnAssigner.setStyle("""
                -fx-background-color: #ECFDF5; -fx-text-fill: #065F46;
                -fx-font-size: 12px; -fx-font-weight: 700;
                -fx-padding: 10 18 10 18; -fx-background-radius: 10;
                -fx-cursor: hand; -fx-border-color: #6EE7B7; -fx-border-radius: 10;
                """);
        btnAssigner.setOnAction(e -> ouvrirNouveauTest(idPatient));

        actions.getChildren().addAll(btnAssigner, btnVoir);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #F3F4F6;");

        carte.getChildren().addAll(ligneHaut, ligneStats, ligneTags, sep, actions);
        return carte;
    }

    private void ouvrirNouveauTest(int idPatient) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/NouveauTest.fxml")
            );
            Node vue = loader.load();
            NouveauTestController ctrl = loader.getController();
            ctrl.setIdPatientCible(idPatient);

            VBox contentArea = (VBox) listePatients.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(vue);
        } catch (IOException e) {
            System.err.println("❌ Erreur ouverture NouveauTest : " + e.getMessage());
        }
    }

    private VBox creerBlocStat(String emoji, String valeur, String label) {
        return creerBlocStat(emoji, valeur, label, "#1F2937");
    }

    private VBox creerBlocStat(String emoji, String valeur, String label, String couleur) {
        VBox bloc = new VBox(4);
        bloc.setAlignment(Pos.CENTER);
        bloc.setPadding(new Insets(12, 16, 12, 16));
        HBox.setHgrow(bloc, Priority.ALWAYS);

        Label lblEmoji  = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 16px;");
        Label lblValeur = new Label(valeur);
        lblValeur.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + couleur + ";");
        Label lblLabel  = new Label(label);
        lblLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF; -fx-font-weight: 600;");

        bloc.getChildren().addAll(lblEmoji, lblValeur, lblLabel);
        return bloc;
    }

    private Region separateurVertical() {
        Region sep = new Region();
        sep.setPrefWidth(1); sep.setMinWidth(1);
        sep.setStyle("-fx-background-color: #E5E7EB;");
        VBox.setVgrow(sep, Priority.ALWAYS);
        return sep;
    }

    private Label creerTag(String texte) {
        String couleur = switch (texte) {
            case "STRESS"    -> "-fx-background-color:#FEE2E2; -fx-text-fill:#DC2626;";
            case "HUMEUR"    -> "-fx-background-color:#FEF3C7; -fx-text-fill:#D97706;";
            case "BIEN-ÊTRE" -> "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A;";
            case "COGNITIF"  -> "-fx-background-color:#EDE9FE; -fx-text-fill:#7C3AED;";
            default          -> "-fx-background-color:#F3F4F6; -fx-text-fill:#6B7280;";
        };
        Label tag = new Label(texte);
        tag.setStyle(couleur + "-fx-padding:3 10; -fx-background-radius:20;" +
                "-fx-font-size:10px; -fx-font-weight:700;");
        return tag;
    }

    private String getInitiales(String nom) {
        String[] parts = nom.split(" ");
        if (parts.length >= 2)
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
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

    private String extraireDate(String ligne) {
        try {
            int start = ligne.indexOf("Date: ") + 6;
            return ligne.substring(start).trim().substring(0, 10);
        } catch (Exception e) { return "--/--"; }
    }

    private int convertirEnPourcent(int scoreBrut, String titreLow) {
        if (titreLow.contains("stress") || titreLow.contains("humeur"))
            return (int) Math.max(0, 100 - (scoreBrut * 100.0) / SCORE_MAX);
        return (int) Math.min(100, (scoreBrut * 100.0) / SCORE_MAX);
    }

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