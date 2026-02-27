package controllers;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import services.AvatarService;
import services.AvatarService.*;
import utils.Session;

import java.util.concurrent.CompletableFuture;

/**
 * ══════════════════════════════════════════════════════════════
 *  AvatarPersonnalisationController — v4
 *
 *  Nouveauté : sauvegarderPrefs() écrit en DB + fichier local
 *  L'image base64 est téléchargée et stockée en DB au moment
 *  du clic sur "Sauvegarder" → affichage instantané au prochain login
 * ══════════════════════════════════════════════════════════════
 */
public class AvatarPersonnalisationController {

    private final AvatarService avatarService = new AvatarService();
    private PrefsAvatar prefs;
    private javafx.stage.Stage stage;
    private Runnable onSauvegardeCallback;

    // ══════════════════════════════════════════════════════════════
    // Point d'entrée
    // ══════════════════════════════════════════════════════════════
    public static void ouvrir(int userId,
                              AvatarService avatarService,
                              Runnable onSauvegarde) {
        Platform.runLater(() -> {
            AvatarPersonnalisationController ctrl =
                    new AvatarPersonnalisationController();
            ctrl.onSauvegardeCallback = onSauvegarde;

            // Charge depuis DB en priorité
            ctrl.prefs = avatarService.chargerPrefs(userId);

            // Pseudo = prénom réel
            String prenom = Session.getPrenom();
            if (prenom == null || prenom.isBlank())
                prenom = Session.getFullName();
            if (prenom != null && !prenom.isBlank()) {
                ctrl.prefs.pseudo = prenom;
            } else if (ctrl.prefs.pseudo.isBlank()
                    || ctrl.prefs.pseudo.startsWith("Patient #")) {
                ctrl.prefs.pseudo = "Mon Avatar";
            }

            ctrl.construireEtAfficher(avatarService);
        });
    }

    // ══════════════════════════════════════════════════════════════
    // Construction UI
    // ══════════════════════════════════════════════════════════════
    private void construireEtAfficher(AvatarService svc) {

        stage = new javafx.stage.Stage();
        stage.setTitle("🎨 Personnaliser mon Avatar");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        HBox root = new HBox(0);
        root.setStyle("-fx-background-color:#f8fafc;");

        // ════════════════════════════════════════════════════════
        // PANNEAU GAUCHE — Prévisualisation
        // ════════════════════════════════════════════════════════
        VBox panneauGauche = new VBox(18);
        panneauGauche.setAlignment(Pos.CENTER);
        panneauGauche.setPadding(new Insets(32, 28, 32, 28));
        panneauGauche.setPrefWidth(270);
        panneauGauche.setMinWidth(270);
        panneauGauche.setStyle(
                "-fx-background-color:white;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,2,0);");

        Label lblTitreG = new Label("🖼️ Aperçu en direct");
        lblTitreG.setStyle(
                "-fx-font-size:12px; -fx-font-weight:900;" +
                        "-fx-text-fill:#64748b;" +
                        "-fx-background-color:#f1f5f9;" +
                        "-fx-background-radius:20; -fx-padding:5 14 5 14;");

        // Cercle avatar
        StackPane avatarPane = new StackPane();
        avatarPane.setMinSize(200, 200); avatarPane.setMaxSize(200, 200);
        Circle haloExt = new Circle(100, 100, 98);
        haloExt.setStyle("-fx-fill:rgba(99,102,241,0.08);");
        Circle haloInt = new Circle(100, 100, 84);
        haloInt.setStyle("-fx-fill:rgba(99,102,241,0.06);");
        Circle fondC = new Circle(100, 100, 76);
        fondC.setStyle("-fx-fill:white; -fx-stroke:rgba(99,102,241,0.18);" +
                "-fx-stroke-width:2; -fx-effect:dropshadow(gaussian," +
                "rgba(99,102,241,0.20),16,0,0,4);");
        ImageView avatarImg = new ImageView();
        avatarImg.setFitWidth(148); avatarImg.setFitHeight(148);
        avatarImg.setPreserveRatio(true);
        Label lblChargement = new Label("⏳");
        lblChargement.setStyle("-fx-font-size:32px;");
        avatarPane.getChildren().addAll(haloExt, haloInt, fondC, avatarImg, lblChargement);

        Label lblPseudo = new Label(prefs.pseudo);
        lblPseudo.setStyle(
                "-fx-font-size:17px; -fx-font-weight:900; -fx-text-fill:#1e293b;");

        Label lblStyleActuel = new Label(prefs.style.label);
        lblStyleActuel.setStyle(
                "-fx-font-size:11px; -fx-font-weight:bold;" +
                        "-fx-text-fill:#6366f1; -fx-background-color:#ede9fe;" +
                        "-fx-background-radius:20; -fx-padding:4 12 4 12;");

        Button btnRegenerer = new Button("🔀 Régénérer l'avatar");
        btnRegenerer.setStyle(
                "-fx-background-color:#f1f5f9; -fx-text-fill:#475569;" +
                        "-fx-font-size:11px; -fx-font-weight:700;" +
                        "-fx-background-radius:12; -fx-cursor:hand;" +
                        "-fx-padding:9 18 9 18;" +
                        "-fx-border-color:#e2e8f0; -fx-border-radius:12; -fx-border-width:1;");
        btnRegenerer.setOnAction(e -> {
            prefs.seed = svc.genererNouveauSeed(prefs.userId);
            rafraichirPreview(avatarImg, lblChargement, svc);
        });

        Label lblCredit = new Label("Propulsé par DiceBear API (gratuit)");
        lblCredit.setStyle(
                "-fx-font-size:9px; -fx-text-fill:#cbd5e1; -fx-font-style:italic;");

        panneauGauche.getChildren().addAll(
                lblTitreG, avatarPane, lblPseudo,
                lblStyleActuel, btnRegenerer, lblCredit);

        // ════════════════════════════════════════════════════════
        // PANNEAU DROIT — Contenu scrollable
        // ════════════════════════════════════════════════════════
        VBox contenuScroll = new VBox(18);
        contenuScroll.setPadding(new Insets(28, 28, 12, 28));
        contenuScroll.setStyle("-fx-background-color:#f8fafc;");

        Label lblTitreApp  = new Label("🎨 Personnalise ton avatar");
        lblTitreApp.setStyle(
                "-fx-font-size:20px; -fx-font-weight:900; -fx-text-fill:#1e293b;");
        Label lblSousTitre = new Label("Crée ton identité virtuelle MindCare");
        lblSousTitre.setStyle("-fx-font-size:12px; -fx-text-fill:#94a3b8;");

        // § Pseudo
        VBox sectionPseudo = creerSection("✏️  Ton pseudo");
        TextField fieldPseudo = new TextField(prefs.pseudo);
        fieldPseudo.setPromptText("Entre ton pseudo...");
        fieldPseudo.setStyle(
                "-fx-background-color:white; -fx-border-color:#e2e8f0;" +
                        "-fx-border-radius:12; -fx-background-radius:12;" +
                        "-fx-padding:10 14 10 14; -fx-font-size:13px;");
        fieldPseudo.textProperty().addListener((obs, old, nv) -> {
            prefs.pseudo = nv.trim().isEmpty() ? "Mon Avatar" : nv.trim();
            lblPseudo.setText(prefs.pseudo);
        });
        sectionPseudo.getChildren().add(fieldPseudo);

        // § Styles
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

        // § Couleurs
        VBox sectionCouleurs = creerSection("🎨  Couleur de fond");
        FlowPane grillCouleurs = new FlowPane(10, 10);
        grillCouleurs.setAlignment(Pos.CENTER_LEFT);
        for (CouleurFond c : CouleurFond.values()) {
            StackPane cercle = new StackPane();
            cercle.setMinSize(38, 38); cercle.setMaxSize(38, 38);
            cercle.setUserData(c);
            appliquerStyleCercle(cercle, c, prefs.fond == c);
            Label tick = new Label(prefs.fond == c ? "✓" : "");
            tick.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#6366f1;");
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
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#6366f1;" +
                        "-fx-background-color:#ede9fe; -fx-background-radius:20; -fx-padding:3 10 3 10;");
        Slider sliderTaille = new Slider(100, 300, prefs.taille);
        sliderTaille.setMajorTickUnit(50);
        sliderTaille.setShowTickLabels(true);
        sliderTaille.setSnapToTicks(true);
        sliderTaille.setStyle("-fx-accent:#6366f1;");
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
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;" +
                "-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Boutons fixes en bas ───────────────────────────────
        final String styleSave =
                "-fx-background-color:#6366f1; -fx-text-fill:white;" +
                        "-fx-font-size:13px; -fx-font-weight:800;" +
                        "-fx-background-radius:14; -fx-cursor:hand;" +
                        "-fx-padding:11 26 11 26;" +
                        "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.40),10,0,0,3);";
        final String styleSaveHover =
                "-fx-background-color:#4f46e5; -fx-text-fill:white;" +
                        "-fx-font-size:13px; -fx-font-weight:800;" +
                        "-fx-background-radius:14; -fx-cursor:hand; -fx-padding:11 26 11 26;";

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle(
                "-fx-background-color:#f1f5f9; -fx-text-fill:#64748b;" +
                        "-fx-font-size:13px; -fx-font-weight:700;" +
                        "-fx-background-radius:14; -fx-cursor:hand;" +
                        "-fx-padding:11 22 11 22;" +
                        "-fx-border-color:#e2e8f0; -fx-border-radius:14; -fx-border-width:1;");
        btnAnnuler.setOnAction(e -> stage.close());

        Button btnSauvegarder = new Button("💾  Sauvegarder l'avatar");
        btnSauvegarder.setStyle(styleSave);
        btnSauvegarder.setOnMouseEntered(e -> btnSauvegarder.setStyle(styleSaveHover));
        btnSauvegarder.setOnMouseExited(e  -> btnSauvegarder.setStyle(styleSave));
        btnSauvegarder.setOnAction(e -> {
            // Désactive pendant la sauvegarde
            btnSauvegarder.setDisable(true);
            btnSauvegarder.setText("⏳ Sauvegarde...");

            CompletableFuture.runAsync(() -> {
                // ✅ Sauvegarde en DB + fichier local + encode base64 en DB
                svc.sauvegarderPrefs(prefs);
            }).thenRun(() -> Platform.runLater(() -> {
                btnSauvegarder.setDisable(false);
                btnSauvegarder.setText("💾  Sauvegarder l'avatar");
                stage.close();
                if (onSauvegardeCallback != null)
                    onSauvegardeCallback.run();
            }));
        });

        HBox boutonsLigne = new HBox(12, btnAnnuler, btnSauvegarder);
        boutonsLigne.setAlignment(Pos.CENTER_RIGHT);

        VBox zoneBoutons = new VBox(10, new Separator(), boutonsLigne);
        zoneBoutons.setPadding(new Insets(10, 28, 20, 28));
        zoneBoutons.setStyle("-fx-background-color:#f8fafc;");

        VBox panneauDroit = new VBox(0, scroll, zoneBoutons);
        panneauDroit.setStyle("-fx-background-color:#f8fafc;");
        HBox.setHgrow(panneauDroit, Priority.ALWAYS);

        root.getChildren().addAll(panneauGauche, panneauDroit);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 720, 600);
        stage.setScene(scene);

        // Aperçu initial — essaie base64 d'abord, sinon URL
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

    // ══════════════════════════════════════════════════════════════
    // Preview live asynchrone
    // ══════════════════════════════════════════════════════════════
    private void rafraichirPreview(ImageView avatarImg,
                                   Label lblChargement,
                                   AvatarService svc) {
        lblChargement.setVisible(true);
        avatarImg.setVisible(false);
        String url = svc.getAvatarUrl(prefs);
        System.out.println("🖼️ Preview : " + url);

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

    // ══════════════════════════════════════════════════════════════
    // Helpers visuels
    // ══════════════════════════════════════════════════════════════
    private VBox creerSection(String titre) {
        VBox s = new VBox(10);
        Label l = new Label(titre);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:900; -fx-text-fill:#374151;");
        s.getChildren().add(l);
        return s;
    }

    private void appliquerStyleToggle(ToggleButton btn, boolean sel) {
        btn.setStyle(
                "-fx-background-color:" + (sel ? "#6366f1" : "white") + ";" +
                        "-fx-text-fill:"        + (sel ? "white"   : "#475569") + ";" +
                        "-fx-font-size:10px; -fx-font-weight:bold;" +
                        "-fx-background-radius:10; -fx-cursor:hand; -fx-padding:7 10 7 10;" +
                        "-fx-border-color:" + (sel ? "#6366f1" : "#e2e8f0") + "; -fx-border-radius:10;");
    }

    private void appliquerStyleCercle(StackPane cercle, CouleurFond c, boolean sel) {
        cercle.setStyle(
                "-fx-background-color:#" + c.hex + "; -fx-background-radius:19;" +
                        "-fx-border-color:" + (sel ? "#6366f1" : "transparent") + ";" +
                        "-fx-border-radius:19; -fx-border-width:2.5; -fx-cursor:hand;");
    }
}