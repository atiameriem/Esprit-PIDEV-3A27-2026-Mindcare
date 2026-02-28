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
import services.ServiceReponse;
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class EspacePraticienQuizController {

    @FXML private Label           lblTotalPatients;
    @FXML private Label           lblTestsSemaine;
    @FXML private Label           lblProgressionMoyenne;
    @FXML private Label           lblNomPsy;
    @FXML private TextField       fieldRecherche;
    @FXML private ComboBox<String> comboFiltre;
    @FXML private VBox            listePatients;

    // ── Boutons onglets (à déclarer dans le FXML) ──────────────
    // <Button fx:id="btnTous"        onAction="#afficherTousPatients" text="Tous les patients"/>
    // <Button fx:id="btnMesPatients" onAction="#afficherMesPatients"  text="👨‍⚕️ Mes patients"/>
    @FXML private Button btnTous;
    @FXML private Button btnMesPatients;

    private final ServiceQuiz    serviceQuiz    = new ServiceQuiz();
    private final ServiceReponse serviceReponse = new ServiceReponse();

    // Tous les patients de la plateforme
    private Map<Integer, String> tousLesPatients = new LinkedHashMap<>();

    // ✅ Patients filtrés depuis rendez_vous WHERE id_psychologist = idPsy
    private Map<Integer, String> mesPatients = new LinkedHashMap<>();

    // "tous" | "mes_patients"
    private String ongletActif  = "tous";
    private int    idPsychologue = -1;

    private static final int SCORE_MAX = 6;

    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
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

        idPsychologue = Session.getUserId();

        if (lblNomPsy != null)
            lblNomPsy.setText("Dr. " + (Session.getFullName() != null
                    ? Session.getFullName() : "Psychologue"));

        configurerFiltres();

        // ✅ Charger les deux listes au démarrage
        try {
            tousLesPatients = serviceQuiz.getTousLesPatients();

            // Requête SQL : SELECT DISTINCT id_patient FROM rendez_vous WHERE id_psychologist = ?
            mesPatients = serviceReponse.getPatientsParPsychologue(idPsychologue);

        } catch (SQLException e) {
            System.err.println("❌ Chargement patients : " + e.getMessage());
        }

        chargerStatsGlobales();
        activerOnglet("tous");
        filtrerEtAfficher();
    }

    // ══════════════════════════════════════════════════════════════
    // ONGLETS
    // ══════════════════════════════════════════════════════════════

    /** Affiche tous les patients de la plateforme */
    @FXML
    private void afficherTousPatients() {
        ongletActif = "tous";
        activerOnglet("tous");
        filtrerEtAfficher();
    }

    /**
     * ✅ Affiche uniquement les patients qui ont un rendez-vous
     *    avec le psychologue connecté (filtre sur rendez_vous.id_psychologist)
     */
    @FXML
    private void afficherMesPatients() {
        ongletActif = "mes_patients";
        activerOnglet("mes_patients");
        filtrerEtAfficher();
    }

    private void activerOnglet(String actif) {
        String styleOn = "-fx-background-color:#4F46E5; -fx-text-fill:white;"
                + "-fx-font-size:13px; -fx-font-weight:800;"
                + "-fx-padding:10 24; -fx-background-radius:12; -fx-cursor:hand;";
        String styleOff = "-fx-background-color:#F3F4F6; -fx-text-fill:#6B7280;"
                + "-fx-font-size:13px; -fx-font-weight:700;"
                + "-fx-padding:10 24; -fx-background-radius:12; -fx-cursor:hand;";

        if (btnTous        != null) btnTous.setStyle("tous".equals(actif)          ? styleOn : styleOff);
        if (btnMesPatients != null) btnMesPatients.setStyle("mes_patients".equals(actif) ? styleOn : styleOff);
    }

    // ══════════════════════════════════════════════════════════════
    // Filtres
    // ══════════════════════════════════════════════════════════════
    private void configurerFiltres() {
        comboFiltre.setItems(FXCollections.observableArrayList(
                "Tous", "En progression", "Stables", "En difficulté", "Nouveaux"));
        comboFiltre.getSelectionModel().selectFirst();
        comboFiltre.setOnAction(e -> filtrerEtAfficher());
        fieldRecherche.textProperty().addListener((obs, old, nv) -> filtrerEtAfficher());
    }

    // ══════════════════════════════════════════════════════════════
    // Stats globales (basées sur TOUS les patients)
    // ══════════════════════════════════════════════════════════════
    private void chargerStatsGlobales() {
        try {
            if (lblTotalPatients != null)
                lblTotalPatients.setText(String.valueOf(tousLesPatients.size()));

            int totalTests = 0, totalDiff = 0, countDiff = 0;
            for (int idPatient : tousLesPatients.keySet()) {
                List<String> hist = serviceQuiz.getHistoriquePatient(idPatient);
                totalTests += hist.size();
                if (hist.size() >= 2) {
                    String apres = hist.get(hist.size() - 1);
                    String type  = extraireTitre(apres).toLowerCase();
                    for (int j = hist.size() - 2; j >= 0; j--) {
                        if (extraireTitre(hist.get(j)).toLowerCase().equals(type)) {
                            totalDiff += convertirEnPourcent(extraireScore(apres), type)
                                    - convertirEnPourcent(extraireScore(hist.get(j)), type);
                            countDiff++;
                            break;
                        }
                    }
                }
            }
            if (lblTestsSemaine != null)
                lblTestsSemaine.setText(String.valueOf(totalTests));
            if (lblProgressionMoyenne != null)
                lblProgressionMoyenne.setText(countDiff > 0
                        ? (totalDiff / countDiff >= 0 ? "+" : "") + totalDiff / countDiff + "%" : "N/A");

        } catch (Exception e) {
            System.err.println("❌ Stats : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Filtrage & affichage
    // ══════════════════════════════════════════════════════════════
    private void filtrerEtAfficher() {
        listePatients.getChildren().clear();
        String recherche = fieldRecherche.getText().toLowerCase().trim();
        String filtre    = comboFiltre.getSelectionModel().getSelectedItem();

        // ✅ Source selon onglet :
        //    "tous"        → tousLesPatients
        //    "mes_patients"→ mesPatients (filtrés depuis rendez_vous)
        Map<Integer, String> source = "mes_patients".equals(ongletActif)
                ? mesPatients
                : tousLesPatients;

        for (Map.Entry<Integer, String> entry : source.entrySet()) {
            int    idPatient  = entry.getKey();
            String nomPatient = entry.getValue();

            if (!recherche.isEmpty() && !nomPatient.toLowerCase().contains(recherche)) continue;

            if (filtre != null && !filtre.equals("Tous")) {
                try {
                    List<String> hist = serviceQuiz.getHistoriquePatient(idPatient);
                    boolean ok = switch (filtre) {
                        case "Nouveaux"       -> hist.isEmpty();
                        case "En progression" -> calculerProgression(hist) > 0;
                        case "Stables"        -> calculerProgression(hist) == 0 && !hist.isEmpty();
                        case "En difficulté"  -> calculerProgression(hist) < 0;
                        default               -> true;
                    };
                    if (!ok) continue;
                } catch (SQLException e) {
                    System.err.println("Erreur filtre : " + e.getMessage());
                }
            }

            listePatients.getChildren().add(creerCartePatient(idPatient, nomPatient));
        }

        if (listePatients.getChildren().isEmpty()) {
            String msg = "mes_patients".equals(ongletActif)
                    ? "Aucun patient n'a de rendez-vous avec vous pour l'instant."
                    : "Aucun patient trouvé.";
            Label lblVide = new Label(msg);
            lblVide.setStyle("-fx-font-size:13px; -fx-text-fill:#9CA3AF;"
                    + "-fx-padding:20; -fx-font-style:italic;");
            lblVide.setWrapText(true);
            listePatients.getChildren().add(lblVide);
        }
    }

    private int calculerProgression(List<String> hist) {
        if (hist.size() < 2) return 0;
        return extraireScore(hist.get(hist.size() - 1))
                - extraireScore(hist.get(hist.size() - 2));
    }

    // ══════════════════════════════════════════════════════════════
    // Carte patient — même affichage que "Tous les patients"
    // ══════════════════════════════════════════════════════════════
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

        // ── Avatar + Nom ──────────────────────────────────────────
        HBox ligneHaut = new HBox(14);
        ligneHaut.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.setPrefSize(52, 52);
        avatar.setMinSize(52, 52);
        avatar.setStyle("-fx-background-color:#EDE9FE; -fx-background-radius:26;");
        Label initiales = new Label(getInitiales(nomPatient));
        initiales.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:#7C3AED;");
        avatar.getChildren().add(initiales);

        VBox infosNom = new VBox(3);
        HBox.setHgrow(infosNom, Priority.ALWAYS);
        Label nom = new Label(nomPatient);
        nom.setStyle("-fx-font-size:15px; -fx-font-weight:800; -fx-text-fill:#1F2937;");

        // Badge "Mon patient" si dans mes consultations
        infosNom.getChildren().add(nom);
        if ("mes_patients".equals(ongletActif) || mesPatients.containsKey(idPatient)) {
            Label badge = new Label("👨‍⚕️ Mon patient");
            badge.setStyle("-fx-font-size:10px; -fx-font-weight:700; -fx-text-fill:#4F46E5;"
                    + "-fx-background-color:#EDE9FE; -fx-background-radius:20;"
                    + "-fx-padding:3 10 3 10;");
            infosNom.getChildren().add(badge);
        }

        ligneHaut.getChildren().addAll(avatar, infosNom);

        // ── Stats quiz ────────────────────────────────────────────
        HBox ligneStats = new HBox(0);
        ligneStats.setAlignment(Pos.CENTER);
        ligneStats.setStyle("-fx-background-color:#F9FAFB; -fx-background-radius:10;");

        try {
            List<String> hist = serviceQuiz.getHistoriquePatient(idPatient);

            if (hist.isEmpty()) {
                Label lblNouv = new Label("🆕  Aucun test effectué");
                lblNouv.setStyle("-fx-font-size:12px; -fx-text-fill:#6B7280; -fx-padding:12;");
                ligneStats.getChildren().add(lblNouv);
            } else {
                String derniereLigne = hist.get(hist.size() - 1);
                int    scoreBrut     = extraireScore(derniereLigne);
                String titreLow      = extraireTitre(derniereLigne).toLowerCase();
                String derniereDate  = extraireDate(derniereLigne);
                int    dernierScore  = convertirEnPourcent(scoreBrut, titreLow);

                int diff = 0;
                if (hist.size() >= 2) {
                    String avantLigne = hist.get(hist.size() - 2);
                    diff = dernierScore - convertirEnPourcent(
                            extraireScore(avantLigne),
                            extraireTitre(avantLigne).toLowerCase());
                }

                ligneStats.getChildren().addAll(
                        creerBlocStat("📋", String.valueOf(hist.size()), "Sessions"),
                        separateurVertical(),
                        creerBlocStat("🎯", dernierScore + "%", "Dernier score",
                                dernierScore >= 70 ? "#10B981"
                                        : dernierScore >= 40 ? "#F59E0B" : "#EF4444"),
                        separateurVertical(),
                        creerBlocStat(diff >= 0 ? "📈" : "📉",
                                (diff >= 0 ? "+" : "") + diff + "%", "Évolution",
                                diff > 0 ? "#10B981" : diff < 0 ? "#EF4444" : "#9CA3AF"),
                        separateurVertical(),
                        creerBlocStat("📅", derniereDate, "Dernier test"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur carte : " + e.getMessage());
        }

        // ── Tags types de tests ───────────────────────────────────
        HBox ligneTags = new HBox(8);
        ligneTags.setAlignment(Pos.CENTER_LEFT);
        try {
            List<String> hist = serviceQuiz.getHistoriquePatient(idPatient);
            Set<String> types = new LinkedHashSet<>();
            for (String ligne : hist) {
                String t = extraireTitre(ligne).toUpperCase();
                if (t.contains("STRESS"))   types.add("STRESS");
                if (t.contains("HUMEUR"))   types.add("HUMEUR");
                if (t.contains("BIEN"))     types.add("BIEN-ÊTRE");
                if (t.contains("COGNITIF")) types.add("COGNITIF");
            }
            if (types.isEmpty()) {
                Label aucun = new Label("Aucun test passé");
                aucun.setStyle("-fx-font-size:11px; -fx-text-fill:#D1D5DB;");
                ligneTags.getChildren().add(aucun);
            } else {
                for (String type : types) ligneTags.getChildren().add(creerTag(type));
            }
        } catch (SQLException e) {
            System.err.println("❌ " + e.getMessage());
        }

        // ── Boutons ───────────────────────────────────────────────
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnVoir = new Button("📊  Voir détails");
        btnVoir.setStyle("""
                -fx-background-color:#4F46E5; -fx-text-fill:white;
                -fx-font-size:12px; -fx-font-weight:700;
                -fx-padding:10 18; -fx-background-radius:10; -fx-cursor:hand;
                """);
        btnVoir.setOnMouseEntered(e -> btnVoir.setStyle("""
                -fx-background-color:#3730A3; -fx-text-fill:white;
                -fx-font-size:12px; -fx-font-weight:700;
                -fx-padding:10 18; -fx-background-radius:10; -fx-cursor:hand;
                """));
        btnVoir.setOnMouseExited(e -> btnVoir.setStyle("""
                -fx-background-color:#4F46E5; -fx-text-fill:white;
                -fx-font-size:12px; -fx-font-weight:700;
                -fx-padding:10 18; -fx-background-radius:10; -fx-cursor:hand;
                """));
        btnVoir.setOnAction(e -> voirDetailsPatient(idPatient, nomPatient));

        Button btnAssigner = new Button("➕  Assigner test");
        btnAssigner.setStyle("""
                -fx-background-color:#ECFDF5; -fx-text-fill:#065F46;
                -fx-font-size:12px; -fx-font-weight:700;
                -fx-padding:10 18; -fx-background-radius:10;
                -fx-cursor:hand; -fx-border-color:#6EE7B7; -fx-border-radius:10;
                """);
        btnAssigner.setOnAction(e -> ouvrirNouveauTest(idPatient));

        actions.getChildren().addAll(btnAssigner, btnVoir);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#F3F4F6;");

        carte.getChildren().addAll(ligneHaut, ligneStats, ligneTags, sep, actions);
        return carte;
    }

    // ══════════════════════════════════════════════════════════════
    // Navigation
    // ══════════════════════════════════════════════════════════════
    private void voirDetailsPatient(int idPatient, String nomPatient) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profilquiz.fxml"));
            Node vue = loader.load();
            ProfilControllerQuiz ctrl = loader.getController();
            ctrl.setIdPatient(idPatient);
            VBox contentArea = (VBox) listePatients.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(vue);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void ouvrirNouveauTest(int idPatient) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NouveauTestQuiz.fxml"));
            Node vue = loader.load();
            NouveauTestQuizController ctrl = loader.getController();
            ctrl.setIdPatientCible(idPatient);
            VBox contentArea = (VBox) listePatients.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(vue);
        } catch (IOException e) { System.err.println("❌ " + e.getMessage()); }
    }

    @FXML
    private void retourSuivie() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Suivie.fxml"));
            Node vue = loader.load();
            VBox parent = (VBox) listePatients.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (IOException e) { System.err.println("❌ " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════
    // Helpers UI
    // ══════════════════════════════════════════════════════════════
    private VBox creerBlocStat(String emoji, String valeur, String label) {
        return creerBlocStat(emoji, valeur, label, "#1F2937");
    }

    private VBox creerBlocStat(String emoji, String valeur, String label, String couleur) {
        VBox bloc = new VBox(4);
        bloc.setAlignment(Pos.CENTER);
        bloc.setPadding(new Insets(12, 16, 12, 16));
        HBox.setHgrow(bloc, Priority.ALWAYS);
        Label e = new Label(emoji); e.setStyle("-fx-font-size:16px;");
        Label v = new Label(valeur); v.setStyle("-fx-font-size:18px; -fx-font-weight:900; -fx-text-fill:" + couleur + ";");
        Label l = new Label(label); l.setStyle("-fx-font-size:10px; -fx-text-fill:#9CA3AF; -fx-font-weight:600;");
        bloc.getChildren().addAll(e, v, l);
        return bloc;
    }

    private Region separateurVertical() {
        Region sep = new Region();
        sep.setPrefWidth(1); sep.setMinWidth(1);
        sep.setStyle("-fx-background-color:#E5E7EB;");
        VBox.setVgrow(sep, Priority.ALWAYS);
        return sep;
    }

    private Label creerTag(String texte) {
        String style = switch (texte) {
            case "STRESS"    -> "-fx-background-color:#FEE2E2; -fx-text-fill:#DC2626;";
            case "HUMEUR"    -> "-fx-background-color:#FEF3C7; -fx-text-fill:#D97706;";
            case "BIEN-ÊTRE" -> "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A;";
            case "COGNITIF"  -> "-fx-background-color:#EDE9FE; -fx-text-fill:#7C3AED;";
            default          -> "-fx-background-color:#F3F4F6; -fx-text-fill:#6B7280;";
        };
        Label tag = new Label(texte);
        tag.setStyle(style + "-fx-padding:3 10; -fx-background-radius:20;"
                + "-fx-font-size:10px; -fx-font-weight:700;");
        return tag;
    }

    // ══════════════════════════════════════════════════════════════
    // Helpers parsing
    // ══════════════════════════════════════════════════════════════
    private String getInitiales(String nom) {
        String[] p = nom.trim().split("\\s+");
        if (p.length >= 2) return ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase();
        return nom.substring(0, Math.min(2, nom.length())).toUpperCase();
    }

    private int extraireScore(String ligne) {
        try { int s = ligne.indexOf("Score: ") + 7, e = ligne.indexOf(" |", s); return Integer.parseInt(ligne.substring(s, e).trim()); }
        catch (Exception e) { return 0; }
    }

    private String extraireTitre(String ligne) {
        try { int s = ligne.indexOf("Quiz: ") + 6, e = ligne.indexOf(" |", s); return ligne.substring(s, e).trim(); }
        catch (Exception e) { return ""; }
    }

    private String extraireDate(String ligne) {
        try { int s = ligne.indexOf("Date: ") + 6; return ligne.substring(s).trim().substring(0, 10); }
        catch (Exception e) { return "--/--"; }
    }

    private int convertirEnPourcent(int scoreBrut, String titreLow) {
        if (titreLow.contains("stress") || titreLow.contains("humeur"))
            return (int) Math.max(0, 100 - (scoreBrut * 100.0) / SCORE_MAX);
        return (int) Math.min(100, (scoreBrut * 100.0) / SCORE_MAX);
    }
}