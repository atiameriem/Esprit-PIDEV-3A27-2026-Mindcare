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

    @FXML private Label            lblTotalPatients;
    @FXML private Label            lblTestsSemaine;
    @FXML private Label            lblProgressionMoyenne;
    @FXML private Label            lblNomPsy;
    @FXML private TextField        fieldRecherche;
    @FXML private ComboBox<String> comboFiltre;
    @FXML private VBox             listePatients;
    @FXML private Button           btnTous;
    @FXML private Button           btnMesPatients;

    private final ServiceQuiz    serviceQuiz    = new ServiceQuiz();
    private final ServiceReponse serviceReponse = new ServiceReponse();

    private Map<Integer, String> tousLesPatients = new LinkedHashMap<>();
    private Map<Integer, String> mesPatients     = new LinkedHashMap<>();
    private String ongletActif   = "tous";
    private int    idPsychologue = -1;
    private static final int SCORE_MAX = 6;

    // ══════════════════════════════════════════════════════════════
    // PALETTE MINDCARE
    // ══════════════════════════════════════════════════════════════
    // Teal (sidebar / boutons principaux)
    private static final String C_TEAL        = "#4A7C8E";
    private static final String C_TEAL_LIGHT  = "#EDF6F9";
    private static final String C_TEAL_BORDER = "#C0DDE6";
    // Violet (Bien-être)
    private static final String C_VIOLET      = "#7C4DFF";
    private static final String C_VIOLET_PALE = "#F3EEFF";
    // Rose/Magenta (Stress)
    private static final String C_ROSE        = "#E91E8C";
    private static final String C_ROSE_PALE   = "#FFE8F5";
    // Cyan (Humeur)
    private static final String C_CYAN        = "#29B6D8";
    private static final String C_CYAN_PALE   = "#E4F6FF";
    // Amber (coins / trophée)
    private static final String C_AMBER       = "#F5A623";
    private static final String C_AMBER_PALE  = "#FFF8E4";
    // Vert (progression positive)
    private static final String C_GREEN       = "#4CAF50";
    private static final String C_GREEN_PALE  = "#EFF9F0";
    // Rouge (En difficulté)
    private static final String C_RED         = "#EF4444";
    // Texte
    private static final String C_DARK        = "#1E3A44";
    private static final String C_MID         = "#2C5F6E";
    private static final String C_GREY        = "#8AA8B2";
    // Fond
    private static final String C_BG          = "#EDF2F4";
    private static final String C_CARD_BG     = "white";

    // ══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        var role = Session.getRoleConnecte();
        if (role != null
                && role != utils.Session.Role.PSYCHOLOGUE
                && role != utils.Session.Role.ADMIN) {
            if (listePatients != null) {
                Label lbl = new Label("⛔ Accès réservé aux psychologues.");
                lbl.setStyle("-fx-font-size:14px; -fx-text-fill:" + C_RED + "; -fx-padding:20;");
                listePatients.getChildren().add(lbl);
            }
            return;
        }

        idPsychologue = Session.getUserId();
        if (lblNomPsy != null)
            lblNomPsy.setText("Dr. " + (Session.getFullName() != null
                    ? Session.getFullName() : "Psychologue"));

        configurerFiltres();
        try {
            tousLesPatients = serviceQuiz.getTousLesPatients();
            mesPatients     = serviceReponse.getPatientsParPsychologue(idPsychologue);
        } catch (SQLException e) {
            System.err.println("❌ " + e.getMessage());
        }

        chargerStatsGlobales();
        activerOnglet("tous");
        filtrerEtAfficher();
    }

    // ── Onglets ──────────────────────────────────────────────────
    @FXML private void afficherTousPatients() { ongletActif = "tous";         activerOnglet("tous");         filtrerEtAfficher(); }
    @FXML private void afficherMesPatients()  { ongletActif = "mes_patients"; activerOnglet("mes_patients"); filtrerEtAfficher(); }

    private void activerOnglet(String actif) {
        String on  = "-fx-background-color:" + C_TEAL + "; -fx-text-fill:white;"
                + "-fx-font-size:13px; -fx-font-weight:800;"
                + "-fx-padding:10 22; -fx-background-radius:22; -fx-cursor:hand;"
                + "-fx-effect:dropshadow(gaussian,rgba(74,124,142,0.35),10,0,0,3);";
        String off = "-fx-background-color:" + C_TEAL_LIGHT + "; -fx-text-fill:" + C_TEAL + ";"
                + "-fx-font-size:13px; -fx-font-weight:700;"
                + "-fx-padding:10 22; -fx-background-radius:22;"
                + "-fx-border-color:" + C_TEAL_BORDER + "; -fx-border-radius:22;"
                + "-fx-border-width:1; -fx-cursor:hand;";
        if (btnTous        != null) btnTous.setStyle("tous".equals(actif)           ? on : off);
        if (btnMesPatients != null) btnMesPatients.setStyle("mes_patients".equals(actif) ? on : off);
    }

    // ── Filtres ──────────────────────────────────────────────────
    private void configurerFiltres() {
        comboFiltre.setItems(FXCollections.observableArrayList(
                "Tous", "En progression", "Stables", "En difficulté", "Nouveaux"));
        comboFiltre.getSelectionModel().selectFirst();
        comboFiltre.setOnAction(e -> filtrerEtAfficher());
        fieldRecherche.textProperty().addListener((obs, o, n) -> filtrerEtAfficher());
    }

    // ── Stats ────────────────────────────────────────────────────
    private void chargerStatsGlobales() {
        try {
            if (lblTotalPatients != null)
                lblTotalPatients.setText(String.valueOf(tousLesPatients.size()));
            int totalTests = 0, totalDiff = 0, countDiff = 0;
            for (int id : tousLesPatients.keySet()) {
                List<String> h = serviceQuiz.getHistoriquePatient(id);
                totalTests += h.size();
                if (h.size() >= 2) {
                    String apres = h.get(h.size() - 1);
                    String type  = extraireTitre(apres).toLowerCase();
                    for (int j = h.size() - 2; j >= 0; j--) {
                        if (extraireTitre(h.get(j)).toLowerCase().equals(type)) {
                            totalDiff += convertirEnPourcent(extraireScore(apres), type)
                                    - convertirEnPourcent(extraireScore(h.get(j)), type);
                            countDiff++; break;
                        }
                    }
                }
            }
            if (lblTestsSemaine     != null) lblTestsSemaine.setText(String.valueOf(totalTests));
            if (lblProgressionMoyenne != null)
                lblProgressionMoyenne.setText(countDiff > 0
                        ? (totalDiff / countDiff >= 0 ? "+" : "") + totalDiff / countDiff + "%" : "N/A");
        } catch (Exception e) { System.err.println("❌ Stats : " + e.getMessage()); }
    }

    // ── Filtrage & affichage ─────────────────────────────────────
    private void filtrerEtAfficher() {
        listePatients.getChildren().clear();
        String recherche = fieldRecherche.getText().toLowerCase().trim();
        String filtre    = comboFiltre.getSelectionModel().getSelectedItem();
        Map<Integer, String> source = "mes_patients".equals(ongletActif) ? mesPatients : tousLesPatients;

        for (Map.Entry<Integer, String> e : source.entrySet()) {
            int id = e.getKey(); String nom = e.getValue();
            if (!recherche.isEmpty() && !nom.toLowerCase().contains(recherche)) continue;
            if (filtre != null && !filtre.equals("Tous")) {
                try {
                    List<String> h = serviceQuiz.getHistoriquePatient(id);
                    boolean ok = switch (filtre) {
                        case "Nouveaux"       -> h.isEmpty();
                        case "En progression" -> calculerProgression(h) > 0;
                        case "Stables"        -> calculerProgression(h) == 0 && !h.isEmpty();
                        case "En difficulté"  -> calculerProgression(h) < 0;
                        default               -> true;
                    };
                    if (!ok) continue;
                } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
            listePatients.getChildren().add(creerCartePatient(id, nom));
        }

        if (listePatients.getChildren().isEmpty()) {
            Label lbl = new Label("🔍  " + ("mes_patients".equals(ongletActif)
                    ? "Aucun patient n'a de rendez-vous avec vous."
                    : "Aucun patient trouvé."));
            lbl.setStyle("-fx-font-size:13px; -fx-text-fill:" + C_GREY + ";"
                    + "-fx-padding:24; -fx-font-style:italic;");
            lbl.setWrapText(true);
            listePatients.getChildren().add(lbl);
        }
    }

    private int calculerProgression(List<String> h) {
        if (h.size() < 2) return 0;
        return extraireScore(h.get(h.size() - 1)) - extraireScore(h.get(h.size() - 2));
    }

    // ══════════════════════════════════════════════════════════════
    // CARTE PATIENT — thème MindCare
    // ══════════════════════════════════════════════════════════════
    private VBox creerCartePatient(int idPatient, String nomPatient) {
        VBox carte = new VBox(14);
        carte.setPadding(new Insets(20));

        String styleBase = "-fx-background-color:white; -fx-background-radius:18;"
                + "-fx-border-color:#DDE8ED; -fx-border-radius:18; -fx-border-width:1.5;"
                + "-fx-effect:dropshadow(gaussian,rgba(74,124,142,0.08),16,0,0,3);";
        String styleHover = "-fx-background-color:white; -fx-background-radius:18;"
                + "-fx-border-color:" + C_TEAL_BORDER + "; -fx-border-radius:18; -fx-border-width:1.5;"
                + "-fx-effect:dropshadow(gaussian,rgba(74,124,142,0.18),20,0,0,5);";
        carte.setStyle(styleBase);
        carte.setOnMouseEntered(e -> carte.setStyle(styleHover));
        carte.setOnMouseExited(e  -> carte.setStyle(styleBase));

        // ── Avatar (couleurs cycliques MindCare) ─────────────────
        String[][] avatarThemes = {
                {"#EDE0FF", "#7C4DFF"},   // violet
                {"#FFD6EE", "#E91E8C"},   // rose
                {"#CCF2FB", "#0099BB"},   // cyan
                {"#FFF0CC", "#D4860A"},   // amber
                {"#D4EEF5", "#4A7C8E"},   // teal
        };
        int idx = Math.abs(nomPatient.hashCode()) % avatarThemes.length;
        String avBg = avatarThemes[idx][0];
        String avFg = avatarThemes[idx][1];

        StackPane avatar = new StackPane();
        avatar.setPrefSize(54, 54); avatar.setMinSize(54, 54);
        avatar.setStyle("-fx-background-color:" + avBg + "; -fx-background-radius:27;");
        Label initiales = new Label(getInitiales(nomPatient));
        initiales.setStyle("-fx-font-size:17px; -fx-font-weight:900; -fx-text-fill:" + avFg + ";");
        avatar.getChildren().add(initiales);

        VBox infosNom = new VBox(4);
        HBox.setHgrow(infosNom, Priority.ALWAYS);
        Label nom = new Label(nomPatient);
        nom.setStyle("-fx-font-size:15px; -fx-font-weight:800; -fx-text-fill:" + C_DARK + ";");
        infosNom.getChildren().add(nom);

        if ("mes_patients".equals(ongletActif) || mesPatients.containsKey(idPatient)) {
            Label badge = new Label("👨‍⚕️ Mon patient");
            badge.setStyle("-fx-font-size:10px; -fx-font-weight:700; -fx-text-fill:" + C_TEAL + ";"
                    + "-fx-background-color:" + C_TEAL_LIGHT + "; -fx-background-radius:20;"
                    + "-fx-padding:3 10;");
            infosNom.getChildren().add(badge);
        }

        HBox ligneHaut = new HBox(14);
        ligneHaut.setAlignment(Pos.CENTER_LEFT);
        ligneHaut.getChildren().addAll(avatar, infosNom);

        // ── Accent bar teal → cyan ────────────────────────────────
        Region accentBar = new Region();
        accentBar.setPrefHeight(3); accentBar.setMinHeight(3);
        accentBar.setStyle("-fx-background-color:linear-gradient(to right,"
                + C_TEAL + "," + C_CYAN + ",transparent); -fx-background-radius:2;");

        // ── Stats row ─────────────────────────────────────────────
        HBox ligneStats = new HBox(0);
        ligneStats.setAlignment(Pos.CENTER);
        ligneStats.setStyle("-fx-background-color:#F5FAFB; -fx-background-radius:12;"
                + "-fx-border-color:#DDE8ED; -fx-border-radius:12; -fx-border-width:1;");

        try {
            List<String> hist = serviceQuiz.getHistoriquePatient(idPatient);
            if (hist.isEmpty()) {
                HBox b = new HBox(8);
                b.setAlignment(Pos.CENTER);
                b.setPadding(new Insets(14));
                HBox.setHgrow(b, Priority.ALWAYS);
                Label lbl = new Label("🆕  Aucun test effectué");
                lbl.setStyle("-fx-font-size:12px; -fx-text-fill:" + C_TEAL + ";"
                        + "-fx-font-weight:700; -fx-background-color:" + C_TEAL_LIGHT + ";"
                        + "-fx-background-radius:8; -fx-padding:6 14;");
                b.getChildren().add(lbl);
                ligneStats.getChildren().add(b);
            } else {
                String last  = hist.get(hist.size() - 1);
                int    score = convertirEnPourcent(extraireScore(last), extraireTitre(last).toLowerCase());
                String date  = extraireDate(last);
                int    diff  = 0;
                if (hist.size() >= 2) {
                    String prev = hist.get(hist.size() - 2);
                    diff = score - convertirEnPourcent(extraireScore(prev), extraireTitre(prev).toLowerCase());
                }
                String scoreColor = score >= 70 ? C_VIOLET : score >= 40 ? C_AMBER : C_ROSE;
                String diffColor  = diff > 0 ? C_GREEN : diff < 0 ? C_ROSE : C_GREY;
                String diffEmoji  = diff >= 0 ? "📈" : "📉";

                ligneStats.getChildren().addAll(
                        creerBlocStat("📋", String.valueOf(hist.size()), "Sessions", C_TEAL),
                        separateurVertical(),
                        creerBlocStat("🎯", score + "%", "Score", scoreColor),
                        separateurVertical(),
                        creerBlocStat(diffEmoji, (diff >= 0 ? "+" : "") + diff + "%", "Évolution", diffColor),
                        separateurVertical(),
                        creerBlocStat("📅", date, "Dernier test", C_GREY));
            }
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }

        // ── Tags ──────────────────────────────────────────────────
        HBox ligneTags = new HBox(8);
        ligneTags.setAlignment(Pos.CENTER_LEFT);
        try {
            List<String> hist = serviceQuiz.getHistoriquePatient(idPatient);
            Set<String> types = new LinkedHashSet<>();
            for (String l : hist) {
                String t = extraireTitre(l).toUpperCase();
                if (t.contains("STRESS"))   types.add("STRESS");
                if (t.contains("HUMEUR"))   types.add("HUMEUR");
                if (t.contains("BIEN"))     types.add("BIEN-ÊTRE");
                if (t.contains("COGNITIF")) types.add("COGNITIF");
            }
            if (types.isEmpty()) {
                Label lbl = new Label("Aucun test passé");
                lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#D1D5DB; -fx-font-style:italic;");
                ligneTags.getChildren().add(lbl);
            } else {
                for (String t : types) ligneTags.getChildren().add(creerTag(t));
            }
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }

        // ── Séparateur ────────────────────────────────────────────
        Region sep = new Region();
        sep.setPrefHeight(1); sep.setMinHeight(1);
        sep.setStyle("-fx-background-color:#DDE8ED;");

        // ── Boutons ───────────────────────────────────────────────
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // Bouton "Voir détails" — teal MindCare
        Button btnVoir = new Button("📊  Voir détails");
        String bvBase = "-fx-background-color:" + C_TEAL + "; -fx-text-fill:white;"
                + "-fx-font-size:12px; -fx-font-weight:800;"
                + "-fx-padding:10 20; -fx-background-radius:22; -fx-cursor:hand;"
                + "-fx-effect:dropshadow(gaussian,rgba(74,124,142,0.35),10,0,0,3);";
        String bvHover = "-fx-background-color:#3A6878; -fx-text-fill:white;"
                + "-fx-font-size:12px; -fx-font-weight:800;"
                + "-fx-padding:10 20; -fx-background-radius:22; -fx-cursor:hand;"
                + "-fx-effect:dropshadow(gaussian,rgba(74,124,142,0.50),14,0,0,4);";
        btnVoir.setStyle(bvBase);
        btnVoir.setOnMouseEntered(e -> btnVoir.setStyle(bvHover));
        btnVoir.setOnMouseExited (e -> btnVoir.setStyle(bvBase));
        btnVoir.setOnAction(e -> voirDetailsPatient(idPatient, nomPatient));

        // Bouton "Assigner test" — violet MindCare
        Button btnAssigner = new Button("➕  Assigner test");
        String baBase  = "-fx-background-color:" + C_VIOLET_PALE + "; -fx-text-fill:" + C_VIOLET + ";"
                + "-fx-font-size:12px; -fx-font-weight:800;"
                + "-fx-padding:10 20; -fx-background-radius:22; -fx-cursor:hand;"
                + "-fx-border-color:#C9B8FF; -fx-border-radius:22; -fx-border-width:1.5;";
        String baHover = "-fx-background-color:#E6DDFF; -fx-text-fill:" + C_VIOLET + ";"
                + "-fx-font-size:12px; -fx-font-weight:800;"
                + "-fx-padding:10 20; -fx-background-radius:22; -fx-cursor:hand;"
                + "-fx-border-color:#A78BFA; -fx-border-radius:22; -fx-border-width:1.5;";
        btnAssigner.setStyle(baBase);
        btnAssigner.setOnMouseEntered(e -> btnAssigner.setStyle(baHover));
        btnAssigner.setOnMouseExited (e -> btnAssigner.setStyle(baBase));
        btnAssigner.setOnAction(e -> ouvrirNouveauTest(idPatient));

        actions.getChildren().addAll(btnAssigner, btnVoir);
        carte.getChildren().addAll(ligneHaut, accentBar, ligneStats, ligneTags, sep, actions);
        return carte;
    }

    // ── Navigation ───────────────────────────────────────────────
    private void voirDetailsPatient(int id, String nom) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/views/Profilquiz.fxml"));
            Node vue = l.load();
            ProfilControllerQuiz ctrl = l.getController();
            ctrl.setIdPatient(id);
            VBox area = (VBox) listePatients.getScene().lookup("#contentArea");
            if (area != null) area.getChildren().setAll(vue);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void ouvrirNouveauTest(int id) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/views/NouveauTestQuiz.fxml"));
            Node vue = l.load();
            NouveauTestQuizController ctrl = l.getController();
            ctrl.setIdPatientCible(id);
            VBox area = (VBox) listePatients.getScene().lookup("#contentArea");
            if (area != null) area.getChildren().setAll(vue);
        } catch (IOException e) { System.err.println("❌ " + e.getMessage()); }
    }

    @FXML
    private void retourSuivie() {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/views/Suivie.fxml"));
            Node vue = l.load();
            VBox area = (VBox) listePatients.getScene().lookup("#contentArea");
            if (area != null) area.getChildren().setAll(vue);
        } catch (IOException e) { System.err.println("❌ " + e.getMessage()); }
    }

    // ── Helpers UI ───────────────────────────────────────────────
    private VBox creerBlocStat(String emoji, String valeur, String label, String couleur) {
        VBox bloc = new VBox(5);
        bloc.setAlignment(Pos.CENTER);
        bloc.setPadding(new Insets(14, 10, 14, 10));
        HBox.setHgrow(bloc, Priority.ALWAYS);

        // Icône colorée (petit carré avec fond)
        String icBg, icFg;
        if (couleur.equals(C_TEAL)) {
            icBg = "#B2D8E8"; icFg = "#2C5F6E";
        } else if (couleur.equals(C_VIOLET)) {
            icBg = "#C9B0FF"; icFg = "#5B21B6";
        } else if (couleur.equals(C_GREEN)) {
            icBg = "#6EE7B7"; icFg = "#065F46";
        } else if (couleur.equals(C_ROSE)) {
            icBg = "#FCA5C0"; icFg = "#9B1239";
        } else {
            icBg = "#CBD5E1"; icFg = "#475569";
        }

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(22, 22); iconBox.setMinSize(22, 22);
        iconBox.setStyle("-fx-background-color:" + icBg + "; -fx-background-radius:6;");
        Label icLabel = new Label(emoji);
        icLabel.setStyle("-fx-font-size:11px; -fx-text-fill:" + icFg + "; -fx-font-weight:900;");
        iconBox.getChildren().add(icLabel);

        Label v = new Label(valeur);
        v.setStyle("-fx-font-size:17px; -fx-font-weight:900; -fx-text-fill:" + couleur + ";");
        Label l = new Label(label);
        l.setStyle("-fx-font-size:10px; -fx-text-fill:" + C_GREY + "; -fx-font-weight:600;");
        bloc.getChildren().addAll(iconBox, v, l);
        return bloc;
    }

    private Region separateurVertical() {
        Region r = new Region();
        r.setPrefWidth(1); r.setMinWidth(1);
        r.setStyle("-fx-background-color:#DDE8ED;");
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    private Label creerTag(String texte) {
        // Tags MindCare : violet=Bien-être, rose=Stress, cyan=Humeur, amber=Cognitif
        String style = switch (texte) {
            case "STRESS"    -> "-fx-background-color:" + C_ROSE_PALE   + "; -fx-text-fill:" + C_ROSE   + ";";
            case "HUMEUR"    -> "-fx-background-color:" + C_CYAN_PALE   + "; -fx-text-fill:" + C_CYAN   + ";";
            case "BIEN-ÊTRE" -> "-fx-background-color:" + C_VIOLET_PALE + "; -fx-text-fill:" + C_VIOLET + ";";
            case "COGNITIF"  -> "-fx-background-color:" + C_AMBER_PALE  + "; -fx-text-fill:" + C_AMBER  + ";";
            default          -> "-fx-background-color:#F0F4F6; -fx-text-fill:" + C_GREY + ";";
        };
        Label tag = new Label(texte);
        tag.setStyle(style + "-fx-padding:4 12; -fx-background-radius:20;"
                + "-fx-font-size:10px; -fx-font-weight:800;");
        return tag;
    }

    // ── Parsing ──────────────────────────────────────────────────
    private String getInitiales(String nom) {
        String[] p = nom.trim().split("\\s+");
        if (p.length >= 2) return ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase();
        return nom.substring(0, Math.min(2, nom.length())).toUpperCase();
    }

    private int extraireScore(String l) {
        try { int s = l.indexOf("Score: ") + 7, e = l.indexOf(" |", s); return Integer.parseInt(l.substring(s, e).trim()); }
        catch (Exception e) { return 0; }
    }

    private String extraireTitre(String l) {
        try { int s = l.indexOf("Quiz: ") + 6, e = l.indexOf(" |", s); return l.substring(s, e).trim(); }
        catch (Exception e) { return ""; }
    }

    private String extraireDate(String l) {
        try { int s = l.indexOf("Date: ") + 6; return l.substring(s).trim().substring(0, 10); }
        catch (Exception e) { return "--/--"; }
    }

    private int convertirEnPourcent(int score, String titre) {
        if (titre.contains("stress") || titre.contains("humeur"))
            return (int) Math.max(0, 100 - (score * 100.0) / SCORE_MAX);
        return (int) Math.min(100, (score * 100.0) / SCORE_MAX);
    }
}