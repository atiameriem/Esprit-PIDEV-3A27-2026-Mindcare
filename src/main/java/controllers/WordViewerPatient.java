package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import services.GroqServiceF;
import utils.MyDatabase;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * WordViewerPatient — Affichage Word en lecture seule pour les patients
 * Inclut : traduction et explication des termes difficiles via IA Groq
 */
public class WordViewerPatient {

    @FXML
    private VBox contentContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Label lblTitre;
    @FXML
    private Button btnExpliquerIA;
    @FXML
    private VBox panneauExplication;
    @FXML
    private Label lblMotSelectionne;
    @FXML
    private Label lblExplication;
    @FXML
    private Label lblTraductionAr;
    @FXML
    private Label lblExemple;
    @FXML
    private ProgressIndicator loadingExplication;

    @FXML
    private javafx.scene.control.TextField fieldSaisieMot;

    private String texteComplet;
    private int idPatient;
    private int idContenu;
    private boolean modeEdition = false;
    private final List<Object> paragraphsControls = new ArrayList<>(); // Track controls to retrieve text

    // ─────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────
    public void initAvecVBox(VBox contentContainer, ScrollPane scrollPane,
            Button btnExpliquerIA, VBox panneauExplication,
            Label lblMotSelectionne, Label lblExplication,
            Label lblTraductionAr, Label lblExemple,
            ProgressIndicator loadingExplication,
            javafx.scene.control.TextField fieldSaisieMot,
            int idPatient, int idContenu) {
        this.contentContainer = contentContainer;
        this.scrollPane = scrollPane;
        this.btnExpliquerIA = btnExpliquerIA;
        this.panneauExplication = panneauExplication;
        this.lblMotSelectionne = lblMotSelectionne;
        this.lblExplication = lblExplication;
        this.lblTraductionAr = lblTraductionAr;
        this.lblExemple = lblExemple;
        this.loadingExplication = loadingExplication;
        this.fieldSaisieMot = fieldSaisieMot;
        this.idPatient = idPatient;
        this.idContenu = idContenu;

        panneauExplication.setVisible(false);
        // Bouton explication toujours activé pour la nouvelle recherche
        btnExpliquerIA.setDisable(false);

        // Auto-scroll
        contentContainer.heightProperty().addListener((obs, old, newH) -> scrollPane.setVvalue(0));
    }

    public void setModeEdition(boolean mode) {
        this.modeEdition = mode;
    }

    public void charger(String chemin) {
        chargerWord(chemin);
    }

    // ─────────────────────────────────────────────
    // Charger et afficher le Word (lecture seule via WebView)
    // ─────────────────────────────────────────────
    private void chargerWord(String chemin) {
        contentContainer.getChildren().clear();
        paragraphsControls.clear();
        StringBuilder texteBuilderHtml = new StringBuilder();
        StringBuilder texteBuilderBrut = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(chemin);
                XWPFDocument doc = new XWPFDocument(fis)) {

            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                String texte = paragraph.getText().trim();
                if (texte.isEmpty())
                    continue;
                texteBuilderHtml.append("<p>").append(texte).append("</p>\n");
                texteBuilderBrut.append(texte).append("\n\n");
            }

            texteComplet = texteBuilderBrut.toString().trim(); // Version brute pour l'IA

            javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
            webView.setPrefHeight(600);
            VBox.setVgrow(webView, Priority.ALWAYS);

            String html = """
                    <html>
                    <head>
                    <style>
                        body {
                            font-family: 'Segoe UI', Arial;
                            font-size: 15px;
                            color: #1F2A33;
                            line-height: 1.75;
                            padding: 16px;
                            margin: 0;
                            user-select: text;
                        }
                        ::selection {
                            background: #B3D9FF;   /* ✅ surlignage bleu */
                            color: #1F2A33;
                        }
                    </style>
                    </head>
                    <body>%s</body>
                    </html>
                    """.formatted(texteBuilderHtml.toString());

            javafx.scene.web.WebEngine engine = webView.getEngine();
            engine.loadContent(html);

            // Optional: You could still listen for selection to pre-fill the TextField
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    engine.executeScript("""
                                document.addEventListener('mouseup', function() {
                                    var sel = window.getSelection().toString().trim();
                                    if (sel.length > 1) {
                                        window.location.hash = 'sel=' + encodeURIComponent(sel);
                                    }
                                });
                            """);

                    engine.locationProperty().addListener((o, oldLoc, newLoc) -> {
                        if (newLoc != null && newLoc.contains("#sel=")) {
                            String mot = newLoc.substring(newLoc.indexOf("#sel=") + 5);
                            try {
                                mot = java.net.URLDecoder.decode(mot, java.nio.charset.StandardCharsets.UTF_8);
                                onMotSelectionne(mot);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            });

            contentContainer.getChildren().add(webView);
            paragraphsControls.add(webView);

        } catch (Exception e) {
            e.printStackTrace();
            Label erreur = new Label("❌ Erreur chargement : " + e.getMessage());
            contentContainer.getChildren().add(erreur);
        }
    }

    private void onMotSelectionne(String mot) {
        // Préremplir le champ de saisie si l'utilisateur surligne un mot
        javafx.application.Platform.runLater(() -> {
            if (fieldSaisieMot != null) {
                fieldSaisieMot.setText(mot);
            }
        });
    }

    /**
     * Sauvegarder (Désactivé en mode WebView lecture seule)
     */
    public void sauvegarder(String chemin) {
        System.out.println("Mode WebView (Lecture seule) : La sauvegarde auto est désactivée.");
    }

    // ─────────────────────────────────────────────
    // Expliquer le mot sélectionné avec l'IA (définition seule, via Groq)
    // ─────────────────────────────────────────────
    @FXML
    public void onExpliquerIA() {
        String motSaisi = fieldSaisieMot != null ? fieldSaisieMot.getText() : null;
        if (motSaisi == null || motSaisi.trim().isEmpty()) {
            return;
        }

        panneauExplication.setVisible(true);
        loadingExplication.setVisible(true);
        lblExplication.setText("Analyse en cours...");
        btnExpliquerIA.setDisable(true);

        String motFinal = motSaisi.trim();
        String contexteFinal = texteComplet.length() > 500 ? texteComplet.substring(0, 500) : texteComplet;

        new Thread(() -> {
            String systemPrompt = """
                    Tu es un psychologue qui explique des mots, concepts ou expressions liés au domaine médical et psychologique.
                    Fournis UNE DÉFINITION simple et claire pour être compréhensible par un patient.
                    Ne fournis PAS de traduction, ni d'exemples complexes. Juste la définition.
                    """;

            String userPrompt = "Définis : \"" + motFinal + "\".\n\n" +
                    "Voici le contexte du document qu'il/elle est en train de lire :\n" + contexteFinal;

            String reponse = GroqServiceF.appeler(systemPrompt, userPrompt, GroqServiceF.MODEL_RAPIDE);

            javafx.application.Platform.runLater(() -> {
                loadingExplication.setVisible(false);
                btnExpliquerIA.setDisable(false);

                if (reponse != null && !reponse.isEmpty()) {
                    lblExplication.setText(reponse.trim());

                    // Sauvegarder uniquement la définition en DB
                    sauvegarderMotExplique(motFinal, reponse.trim());
                } else {
                    lblExplication.setText("❌ Erreur. Réessayez.");
                }
            });
        }).start();
    }

    // ─────────────────────────────────────────────
    // Fermer le panneau d'explication
    // ─────────────────────────────────────────────
    @FXML
    public void onFermerExplication() {
        if (fieldSaisieMot != null) {
            fieldSaisieMot.clear();
        }
        btnExpliquerIA.setText("Trouver la définition");
        btnExpliquerIA.setDisable(false);
    }

    // ─────────────────────────────────────────────
    // Sauvegarder en MySQL (table mots_expliques)
    // ─────────────────────────────────────────────
    private void sauvegarderMotExplique(String mot, String explication) {
        String sql = """
                INSERT INTO mots_expliques
                (id_patient, id_contenu, mot, explication, traduction_ar, exemple_clinique, date_consultation)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """;

        try (Connection conn = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPatient);
            ps.setInt(2, idContenu);
            ps.setString(3, mot);
            ps.setString(4, explication);
            ps.setString(5, null); // pas de traduction générée
            ps.setString(6, null); // pas d'exemple généré
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}