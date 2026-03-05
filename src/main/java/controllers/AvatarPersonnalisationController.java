package controllers;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import services.AvatarService;
import services.AvatarService.*;
import utils.Session;

import java.util.concurrent.CompletableFuture;

/**
 * AvatarPersonnalisationController — palette MindCare Teal
 * Toutes les couleurs violettes (#6366f1) remplacées par le teal MindCare
 *
 *  Teal foncé    : #2D6E7E   (boutons principaux, accents)
 *  Teal moyen    : #5C98A8   (bordures, icônes)
 *  Teal clair    : #D4EBF0   (fonds badges)
 *  Teal extra    : rgba(92,152,168,…)
 */
public class AvatarPersonnalisationController {

    // ── Palette MindCare ─────────────────────────────────────────
    private static final String TEAL_DARK   = "#2D6E7E";
    private static final String TEAL_HOVER  = "#225A69";
    private static final String TEAL_MED    = "#5C98A8";
    private static final String TEAL_LIGHT  = "#D4EBF0";
    private static final String TEAL_BG     = "rgba(92,152,168,0.08)";
    private static final String TEAL_BORDER = "rgba(92,152,168,0.25)";
    private static final String TEAL_SHADOW = "rgba(45,110,126,0.30)";

    private final AvatarService avatarService = new AvatarService();
    private PrefsAvatar prefs;
    private javafx.stage.Stage stage;
    private Runnable onSauvegardeCallback;

    // ════════════════════════════════════════════════════════════
    //  Point d'entrée
    // ════════════════════════════════════════════════════════════
    public static void ouvrir(int userId,
                              AvatarService avatarService,
                              Runnable onSauvegarde) {
        Platform.runLater(() -> {
            AvatarPersonnalisationController ctrl =
                    new AvatarPersonnalisationController();
            ctrl.onSauvegardeCallback = onSauvegarde;
            ctrl.prefs = avatarService.chargerPrefs(userId);

            String prenom = Session.getPrenom();
            if (prenom == null || prenom.isBlank()) prenom = Session.getFullName();
            if (prenom != null && !prenom.isBlank()) {
                ctrl.prefs.pseudo = prenom;
            } else if (ctrl.prefs.pseudo.isBlank()
                    || ctrl.prefs.pseudo.startsWith("Patient #")) {
                ctrl.prefs.pseudo = "Mon Avatar";
            }

            ctrl.construireEtAfficher(avatarService);
        });
    }

    // ════════════════════════════════════════════════════════════
    //  Construction UI
    // ════════════════════════════════════════════════════════════
    private void construireEtAfficher(AvatarService svc) {

        stage = new javafx.stage.Stage();
        stage.setTitle("🎨 Personnaliser mon Avatar");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        HBox root = new HBox(0);
        root.setStyle("-fx-background-color:#EAF3F5;");  // fond MindCare

        // ════════════════════════════════════════════════════════
        //  PANNEAU GAUCHE — Prévisualisation
        // ════════════════════════════════════════════════════════
        VBox panneauGauche = new VBox(18);
        panneauGauche.setAlignment(Pos.CENTER);
        panneauGauche.setPadding(new Insets(32, 28, 32, 28));
        panneauGauche.setPrefWidth(270);
        panneauGauche.setMinWidth(270);
        panneauGauche.setStyle(
                "-fx-background-color: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(45,110,126,0.10), 14, 0, 2, 0);");

        Label lblTitreG = new Label("🖼️ Aperçu en direct");
        lblTitreG.setStyle(
                "-fx-font-size:12px; -fx-font-weight:900;" +
                        "-fx-text-fill:" + TEAL_MED + ";" +
                        "-fx-background-color:" + TEAL_LIGHT + ";" +
                        "-fx-background-radius:20; -fx-padding:5 14 5 14;");

        // Cercle avatar — halo teal
        StackPane avatarPane = new StackPane();
        avatarPane.setMinSize(200, 200); avatarPane.setMaxSize(200, 200);
        Circle haloExt = new Circle(100, 100, 98);
        haloExt.setStyle("-fx-fill:rgba(92,152,168,0.10);");
        Circle haloInt = new Circle(100, 100, 84);
        haloInt.setStyle("-fx-fill:rgba(92,152,168,0.06);");
        Circle fondC = new Circle(100, 100, 76);
        fondC.setStyle(
                "-fx-fill:white;" +
                        "-fx-stroke:rgba(92,152,168,0.22);" +
                        "-fx-stroke-width:2;" +
                        "-fx-effect:dropshadow(gaussian,rgba(92,152,168,0.20),16,0,0,4);");
        ImageView avatarImg = new ImageView();
        avatarImg.setFitWidth(148); avatarImg.setFitHeight(148);
        avatarImg.setPreserveRatio(true);
        Label lblChargement = new Label("⏳");
        lblChargement.setStyle("-fx-font-size:32px;");
        avatarPane.getChildren().addAll(haloExt, haloInt, fondC, avatarImg, lblChargement);

        Label lblPseudo = new Label(prefs.pseudo);
        lblPseudo.setStyle(
                "-fx-font-size:17px; -fx-font-weight:900; -fx-text-fill:#1F2A33;");

        Label lblStyleActuel = new Label(prefs.style.label);
        lblStyleActuel.setStyle(
                "-fx-font-size:11px; -fx-font-weight:bold;" +
                        "-fx-text-fill:" + TEAL_DARK + ";" +
                        "-fx-background-color:" + TEAL_LIGHT + ";" +
                        "-fx-background-radius:20; -fx-padding:4 12 4 12;");

        // Bouton Régénérer — style teal ghost
        Button btnRegenerer = new Button("🔀 Régénérer l'avatar");
        btnRegenerer.setStyle(styleGhost());
        btnRegenerer.setOnMouseEntered(e -> btnRegenerer.setStyle(styleGhostHover()));
        btnRegenerer.setOnMouseExited(e  -> btnRegenerer.setStyle(styleGhost()));
        btnRegenerer.setOnAction(e -> {
            prefs.seed = svc.genererNouveauSeed(prefs.userId);
            rafraichirPreview(avatarImg, lblChargement, svc);
        });

        Label lblCredit = new Label("Propulsé par DiceBear API (gratuit)");
        lblCredit.setStyle("-fx-font-size:9px; -fx-text-fill:#8AA8B2; -fx-font-style:italic;");

        panneauGauche.getChildren().addAll(
                lblTitreG, avatarPane, lblPseudo,
                lblStyleActuel, btnRegenerer, lblCredit);

        // ════════════════════════════════════════════════════════
        //  PANNEAU DROIT — Options
        // ════════════════════════════════════════════════════════
        VBox contenuScroll = new VBox(20);
        contenuScroll.setPadding(new Insets(28, 28, 12, 28));
        contenuScroll.setStyle("-fx-background-color:#EAF3F5;");

        Label lblTitreApp  = new Label("🎨 Personnalise ton avatar");
        lblTitreApp.setStyle("-fx-font-size:20px; -fx-font-weight:900; -fx-text-fill:#1F2A33;");
        Label lblSousTitre = new Label("Crée ton identité virtuelle MindCare");
        lblSousTitre.setStyle("-fx-font-size:12px; -fx-text-fill:#6E8E9A;");

        // § Pseudo
        VBox sectionPseudo = creerSection("✏️  Ton pseudo");
        TextField fieldPseudo = new TextField(prefs.pseudo);
        fieldPseudo.setPromptText("Entre ton pseudo...");
        fieldPseudo.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:rgba(92,152,168,0.30);" +
                        "-fx-border-radius:12; -fx-background-radius:12;" +
                        "-fx-padding:10 14 10 14; -fx-font-size:13px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(92,152,168,0.06),4,0,0,1);");
        fieldPseudo.textProperty().addListener((obs, old, nv) -> {
            prefs.pseudo = nv.trim().isEmpty() ? "Mon Avatar" : nv.trim();
            lblPseudo.setText(prefs.pseudo);
        });
        sectionPseudo.getChildren().add(fieldPseudo);

        // § Styles d'avatar
        VBox sectionStyles = creerSection("🎭  Style d'avatar");
        TilePane grillStyles = new TilePane();
        grillStyles.setHgap(8); grillStyles.setVgap(8);
        grillStyles.setPrefColumns(4); grillStyles.setPrefTileWidth(95);
        ToggleGroup toggleStyle = new ToggleGroup();
        for (Style s : Style.values()) {
            ToggleButton btn = new ToggleButton(s.label);
            btn.setToggleGroup(toggleStyle);
            btn.setSelected(prefs.style == s);
            appliquerStyleToggle(btn, prefs.style == s);
            btn.setOnAction(e -> {
                prefs.style = s;
                lblStyleActuel.setText(s.label);
                toggleStyle.getToggles().forEach(t ->
                        appliquerStyleToggle((ToggleButton) t, t == btn));
                rafraichirPreview(avatarImg, lblChargement, svc);
            });
            grillStyles.getChildren().add(btn);
        }
        sectionStyles.getChildren().add(grillStyles);

        // § Couleurs de fond
        VBox sectionCouleurs = creerSection("🎨  Couleur de fond");
        FlowPane grillCouleurs = new FlowPane(10, 10);
        grillCouleurs.setAlignment(Pos.CENTER_LEFT);
        for (CouleurFond c : CouleurFond.values()) {
            StackPane cercle = new StackPane();
            cercle.setMinSize(38, 38); cercle.setMaxSize(38, 38);
            cercle.setUserData(c);
            appliquerStyleCercle(cercle, c, prefs.fond == c);
            Label tick = new Label(prefs.fond == c ? "✓" : "");
            // ✅ Tick teal au lieu de violet
            tick.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + TEAL_DARK + ";");
            cercle.getChildren().add(tick);
            Tooltip.install(cercle, new Tooltip(c.label));
            cercle.setOnMouseClicked(e -> {
                prefs.fond = c;
                grillCouleurs.getChildren().forEach(n -> {
                    if (n instanceof StackPane sp) {
                        CouleurFond cf = (CouleurFond) sp.getUserData();
                        if (cf == null) return;
                        boolean sel = cf == c;
                        appliquerStyleCercle(sp, cf, sel);
                        ((Label) sp.getChildren().get(0)).setText(sel ? "✓" : "");
                    }
                });
                rafraichirPreview(avatarImg, lblChargement, svc);
            });
            grillCouleurs.getChildren().add(cercle);
        }
        sectionCouleurs.getChildren().add(grillCouleurs);

        // § Taille
        VBox sectionTaille = creerSection("📐  Taille de l'avatar");
        Label lblTailleVal = new Label(prefs.taille + " px");
        lblTailleVal.setStyle(
                "-fx-font-size:11px; -fx-font-weight:bold;" +
                        "-fx-text-fill:" + TEAL_DARK + ";" +
                        "-fx-background-color:" + TEAL_LIGHT + ";" +
                        "-fx-background-radius:20; -fx-padding:3 10 3 10;");
        Slider sliderTaille = new Slider(100, 300, prefs.taille);
        sliderTaille.setMajorTickUnit(50);
        sliderTaille.setShowTickLabels(true);
        sliderTaille.setSnapToTicks(true);
        // ✅ Slider teal
        sliderTaille.setStyle("-fx-accent:" + TEAL_MED + ";");
        sliderTaille.valueProperty().addListener((obs, old, nv) -> {
            prefs.taille = nv.intValue();
            lblTailleVal.setText(prefs.taille + " px");
        });
        HBox ligneTaille = new HBox(12, sliderTaille, lblTailleVal);
        ligneTaille.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sliderTaille, Priority.ALWAYS);
        sectionTaille.getChildren().add(ligneTaille);

        contenuScroll.getChildren().addAll(
                lblTitreApp, lblSousTitre,
                new Separator(),
                sectionPseudo, sectionStyles,
                sectionCouleurs, sectionTaille);

        ScrollPane scroll = new ScrollPane(contenuScroll);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background:transparent;" +
                        "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Boutons bas ───────────────────────────────────────────
        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle(styleGhost());
        btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(styleGhostHover()));
        btnAnnuler.setOnMouseExited(e  -> btnAnnuler.setStyle(styleGhost()));
        btnAnnuler.setOnAction(e -> stage.close());

        // ✅ Bouton Sauvegarder teal (plus de violet)
        Button btnSauvegarder = new Button("💾  Sauvegarder l'avatar");
        Platform.runLater(() -> {
            CornerRadii r = new CornerRadii(14);
            btnSauvegarder.setBackground(new Background(
                    new BackgroundFill(Color.web(TEAL_DARK), r, Insets.EMPTY)));
            btnSauvegarder.setBorder(Border.EMPTY);
            btnSauvegarder.setTextFill(Color.WHITE);
            btnSauvegarder.setStyle(
                    "-fx-font-size:13px; -fx-font-weight:800;" +
                            "-fx-padding:11 26 11 26; -fx-cursor:hand;" +
                            "-fx-background-insets:0; -fx-border-insets:0;" +
                            "-fx-effect:dropshadow(gaussian," + TEAL_SHADOW + ",10,0,0,3);");
            btnSauvegarder.setOnMouseEntered(ev -> btnSauvegarder.setBackground(new Background(
                    new BackgroundFill(Color.web(TEAL_HOVER), r, Insets.EMPTY))));
            btnSauvegarder.setOnMouseExited(ev  -> btnSauvegarder.setBackground(new Background(
                    new BackgroundFill(Color.web(TEAL_DARK), r, Insets.EMPTY))));
        });

        btnSauvegarder.setOnAction(e -> {
            btnSauvegarder.setDisable(true);
            btnSauvegarder.setText("⏳ Sauvegarde...");
            CompletableFuture.runAsync(() -> svc.sauvegarderPrefs(prefs))
                    .thenRun(() -> Platform.runLater(() -> {
                        btnSauvegarder.setDisable(false);
                        btnSauvegarder.setText("💾  Sauvegarder l'avatar");
                        stage.close();
                        if (onSauvegardeCallback != null) onSauvegardeCallback.run();
                    }));
        });

        HBox boutonsLigne = new HBox(12, btnAnnuler, btnSauvegarder);
        boutonsLigne.setAlignment(Pos.CENTER_RIGHT);

        VBox zoneBoutons = new VBox(10, new Separator(), boutonsLigne);
        zoneBoutons.setPadding(new Insets(10, 28, 20, 28));
        zoneBoutons.setStyle("-fx-background-color:#EAF3F5;");

        VBox panneauDroit = new VBox(0, scroll, zoneBoutons);
        panneauDroit.setStyle("-fx-background-color:#EAF3F5;");
        HBox.setHgrow(panneauDroit, Priority.ALWAYS);

        root.getChildren().addAll(panneauGauche, panneauDroit);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 720, 600);
        stage.setScene(scene);

        // Aperçu initial
        Image imgBase64 = svc.getImageDepuisBase64(prefs);
        if (imgBase64 != null && !imgBase64.isError()) {
            avatarImg.setImage(imgBase64);
            avatarImg.setVisible(true);
            lblChargement.setVisible(false);
        } else {
            rafraichirPreview(avatarImg, lblChargement, svc);
        }

        stage.showAndWait();
    }

    // ════════════════════════════════════════════════════════════
    //  Preview live asynchrone
    // ════════════════════════════════════════════════════════════
    private void rafraichirPreview(ImageView avatarImg,
                                   Label lblChargement,
                                   AvatarService svc) {
        lblChargement.setVisible(true);
        avatarImg.setVisible(false);
        String url = svc.getAvatarUrl(prefs);

        CompletableFuture.supplyAsync(() -> {
            try { return new Image(url, 200, 200, true, true, true); }
            catch (Exception ex) { return null; }
        }).thenAccept(img -> Platform.runLater(() -> {
            if (img != null && !img.isError()) {
                avatarImg.setImage(img);
                avatarImg.setVisible(true);
                javafx.animation.FadeTransition ft =
                        new javafx.animation.FadeTransition(
                                javafx.util.Duration.millis(400), avatarImg);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
                javafx.animation.ScaleTransition sc =
                        new javafx.animation.ScaleTransition(
                                javafx.util.Duration.millis(400), avatarImg);
                sc.setFromX(0.7); sc.setFromY(0.7);
                sc.setToX(1.0);   sc.setToY(1.0);
                sc.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
                sc.play();
            }
            lblChargement.setVisible(false);
        }));
    }

    // ════════════════════════════════════════════════════════════
    //  Helpers styles — palette teal
    // ════════════════════════════════════════════════════════════
    private VBox creerSection(String titre) {
        VBox s = new VBox(10);
        Label l = new Label(titre);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:900; -fx-text-fill:#1F2A33;");
        s.getChildren().add(l);
        return s;
    }

    /** ToggleButton style : teal quand sélectionné, blanc sinon */
    private void appliquerStyleToggle(ToggleButton btn, boolean sel) {
        btn.setStyle(
                "-fx-background-color:" + (sel ? TEAL_DARK : "white") + ";" +
                        "-fx-text-fill:"        + (sel ? "white"   : "#374151") + ";" +
                        "-fx-font-size:10px; -fx-font-weight:bold;" +
                        "-fx-background-radius:10; -fx-cursor:hand; -fx-padding:7 10 7 10;" +
                        "-fx-border-color:" + (sel ? TEAL_DARK : "rgba(92,152,168,0.25)") + ";" +
                        "-fx-border-radius:10; -fx-border-width:1.5;");
    }

    /** Cercle couleur de fond : bordure teal quand sélectionné */
    private void appliquerStyleCercle(StackPane cercle, CouleurFond c, boolean sel) {
        cercle.setStyle(
                "-fx-background-color:#" + c.hex + "; -fx-background-radius:19;" +
                        "-fx-border-color:" + (sel ? TEAL_DARK : "transparent") + ";" +
                        "-fx-border-radius:19; -fx-border-width:2.5; -fx-cursor:hand;");
    }

    /** Bouton ghost teal (Annuler / Régénérer) */
    private String styleGhost() {
        return "-fx-background-color:white;" +
                "-fx-text-fill:" + TEAL_MED + ";" +
                "-fx-font-size:12px; -fx-font-weight:700;" +
                "-fx-background-radius:14; -fx-cursor:hand;" +
                "-fx-padding:10 22 10 22;" +
                "-fx-border-color:rgba(92,152,168,0.35);" +
                "-fx-border-radius:14; -fx-border-width:1.5;";
    }

    private String styleGhostHover() {
        return "-fx-background-color:" + TEAL_LIGHT + ";" +
                "-fx-text-fill:" + TEAL_DARK + ";" +
                "-fx-font-size:12px; -fx-font-weight:700;" +
                "-fx-background-radius:14; -fx-cursor:hand;" +
                "-fx-padding:10 22 10 22;" +
                "-fx-border-color:" + TEAL_MED + ";" +
                "-fx-border-radius:14; -fx-border-width:1.5;";
    }
}