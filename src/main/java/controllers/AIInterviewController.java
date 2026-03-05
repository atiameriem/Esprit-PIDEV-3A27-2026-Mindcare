package controllers;

import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.scene.input.KeyCode;
import services.GroqServiceF;
import services.GeneratorServiceF;
import services.ModuleServiceF;
import java.util.*;

/**
 * AIInterviewController — Chat conversationnel IA ↔ Psychologue
 * L'IA pose des questions une par une pour générer un programme thérapeutique
 */
public class AIInterviewController {

    @FXML
    private VBox chatContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextArea tfMessage;
    @FXML
    private Button btnEnvoyer;
    @FXML
    private ProgressIndicator loading;
    @FXML
    private Button btnGenererCours;
    @FXML
    private Label lblStatut;

    // Historique complet de la conversation
    private final List<Map<String, String>> historique = new ArrayList<>();
    private boolean interviewTerminee = false;
    private int idFormationSelectionnee = -1; // Pour sauvegarder le cours dans la bonne formation
    private FormationController parentController; // Référence pour la navigation
    private final ModuleServiceF moduleServiceF = new ModuleServiceF();

    public void initAvecVBox(VBox chatContainer, ScrollPane scrollPane, TextArea tfMessage,
            Button btnEnvoyer, Button btnGenererCours, ProgressIndicator loading,
            Label lblStatut) {
        this.chatContainer = chatContainer;
        this.scrollPane = scrollPane;
        this.tfMessage = tfMessage;
        this.btnEnvoyer = btnEnvoyer;
        this.btnGenererCours = btnGenererCours;
        this.loading = loading;
        this.lblStatut = lblStatut;

        initialize(); // Call setup logic
    }

    public void setIdFormationSelectionnee(int idFormation) {
        this.idFormationSelectionnee = idFormation;
    }

    /**
     * Définit le contrôleur parent pour permettre la navigation inverse
     */
    public void setParentController(FormationController parent) {
        this.parentController = parent;
    }

    // ─────────────────────────────────────────────
    // SYSTEM PROMPT — Contexte MindCare Psychologue
    // ─────────────────────────────────────────────
    private static final String SYSTEM_PROMPT = """
            Tu es un assistant spécialisé en santé mentale qui aide les psychologues
            à créer des programmes thérapeutiques et modules de bien-être sur MindCare.

            Pose les questions dans cet ordre, UNE SEULE à la fois :
            1. Quel est le titre de votre module thérapeutique ?
            2. À quel type de patients s'adresse ce programme ?
               (anxiété, dépression, stress, développement personnel, autre...)
            3. Quelle est la durée prévue de ce programme ? (ex: 6 séances, 1 mois...)
            4. Quels sont les 3 objectifs thérapeutiques principaux ?
            5. Quelles techniques souhaitez-vous utiliser ?
               (TCC, pleine conscience, ACT, EMDR, relaxation...)
            6. Quels sont les grands thèmes des séances ?
            7. Quel type d'exercices ou activités thérapeutiques inclure ?
            8. Y a-t-il des contre-indications ou points de vigilance particuliers ?

            Règles STRICTES :
            - Pose UNE seule question à la fois, jamais deux
            - Sois naturel, bienveillant et professionnel
            - Utilise un vocabulaire clinique adapté
            - Après la 8ème réponse, dis EXACTEMENT cette phrase :
              "Parfait Docteur ! J'ai toutes les informations nécessaires pour rédiger l'article. Cliquez sur 'Générer l'article' pour obtenir le module thérapeutique formaté pour votre patient. 🧠"
            - Réponds toujours en français
            - Ne génère PAS le cours toi-même, attends le bouton
            """;

    // ─────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        btnGenererCours.setVisible(false);
        loading.setVisible(false);

        // Message de bienvenue
        String messageAccueil = "Bonjour Docteur ! 👋\n\n" +
                "Je suis votre assistant pédagogique MindCare.\n" +
                "Je vais vous poser quelques questions pour créer " +
                "votre programme thérapeutique personnalisé.\n\n" +
                "Commençons ! Quel est le titre de votre module thérapeutique ?";

        afficherMessageIA(messageAccueil);

        // Ajouter la 1ère question à l'historique
        historique.add(Map.of(
                "role", "assistant",
                "content", "Quel est le titre de votre module thérapeutique ?"));

        // Enter pour envoyer (Shift+Enter pour saut de ligne)
        tfMessage.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                e.consume(); // Empêche de créer une nouvelle ligne
                onEnvoyer();
            }
        });

        // Auto-scroll vers le bas
        chatContainer.heightProperty().addListener((obs, old, newH) -> scrollPane.setVvalue(1.0));
    }

    // ─────────────────────────────────────────────
    // Envoi d'un message
    // ─────────────────────────────────────────────
    @FXML
    public void onEnvoyer() {
        String message = tfMessage.getText().trim();
        if (message.isEmpty() || interviewTerminee)
            return;

        // Afficher message du psychologue (droite)
        afficherMessagePsychologue(message);
        tfMessage.clear();

        // Désactiver pendant l'appel IA
        tfMessage.setDisable(true);
        btnEnvoyer.setDisable(true);
        loading.setVisible(true);
        lblStatut.setText("L'IA réfléchit...");

        // Ajouter à l'historique
        historique.add(Map.of("role", "user", "content", message));

        // Appel Groq dans un thread séparé
        new Thread(() -> {
            String reponseIA = GroqServiceF.appelerAvecHistorique(
                    historique,
                    GroqServiceF.MODEL_PUISSANT,
                    SYSTEM_PROMPT);

            javafx.application.Platform.runLater(() -> {
                loading.setVisible(false);
                tfMessage.setDisable(false);
                btnEnvoyer.setDisable(false);
                tfMessage.requestFocus();
                lblStatut.setText("");

                if (reponseIA != null) {
                    afficherMessageIA(reponseIA);
                    historique.add(Map.of("role", "assistant", "content", reponseIA));

                    // Détecter fin de l'interview
                    if (reponseIA.contains("Générer l'article") ||
                            reponseIA.contains("toutes les informations")) {
                        interviewTerminee = true;
                        tfMessage.setDisable(true);
                        tfMessage.setPromptText("✅ Interview terminée");
                        btnEnvoyer.setDisable(true);
                        btnGenererCours.setText("🧠 Générer l'article pour le patient");
                        btnGenererCours.setVisible(true);
                        btnGenererCours.setManaged(true);
                    }
                } else {
                    afficherMessageIA("❌ Erreur de connexion. Vérifiez votre clé Groq.");
                }
            });
        }).start();
    }

    // ─────────────────────────────────────────────
    // Générer le cours Word depuis l'historique
    // ─────────────────────────────────────────────
    @FXML
    public void onGenererCours() {
        btnGenererCours.setDisable(true);
        btnGenererCours.setText("⏳ Génération en cours...");
        loading.setVisible(true);

        new Thread(() -> {
            try {
                // Construire le prompt final avec tout l'historique
                StringBuilder contexte = new StringBuilder();
                contexte.append("""
                        En tant qu'expert en psychologie clinique, génère un programme
                        thérapeutique complet et structuré basé sur cet interview :

                        """);

                for (Map<String, String> msg : historique) {
                    if (msg.get("role").equals("assistant")) {
                        contexte.append("Question IA : ").append(msg.get("content")).append("\n");
                    } else if (msg.get("role").equals("user")) {
                        contexte.append("Psychologue : ").append(msg.get("content")).append("\n");
                    }
                }

                contexte.append(
                        """

                                En tant qu'expert en psychologie et rédacteur spécialisé dans le bien-être,
                                rédige un ARTICLE thérapeutique captivant, bienveillant et facile à lire pour un PATIENT.
                                Utilise le "Vous" pour t'adresser directement au lecteur.

                                RÈGLE ABSOLUE : L'article doit contenir EXACTEMENT 3 SECTIONS avec 3 TITRES.
                                Ne génère pas plus de 3 sections.
                                L'article doit être structuré de la manière suivante avec ce format STRICT (une ligne par tag) :
                                ARTICLE_TITRE: [Un titre accrocheur et motivant pour le module]
                                INTRO_CONTENU: [Une intro chaleureuse qui explique pourquoi cet article va aider le patient]
                                SECTION1_TITRE: [Titre de la 1ère thématique]
                                SECTION1_CONTENU: [Contenu fluide, conseils pratiques]
                                SECTION2_TITRE: [Titre de la 2ème thématique]
                                SECTION2_CONTENU: [Contenu de la deuxième partie]
                                SECTION3_TITRE: [Titre de la 3ème et dernière thématique]
                                SECTION3_CONTENU: [Contenu formaté]
                                CONCLUSION_CONTENU: [Une conclusion positive et encourageante]
                                """);

                String contenuIA = GroqServiceF.appeler(contexte.toString());
                String titre = extraireTitre();

                if (contenuIA != null) {
                    javafx.application.Platform.runLater(() -> {
                        loading.setVisible(false);

                        // 1. CRÉER LE POPUP DE PRÉVIEW
                        Dialog<String> previewDialog = new Dialog<>();
                        previewDialog.setTitle("Aperçu du programme généré");
                        previewDialog.setHeaderText("Relisez et modifiez le programme avant de l'ajouter au module :");

                        ButtonType publishButtonType = new ButtonType("Postuler (Publier)",
                                ButtonBar.ButtonData.OK_DONE);
                        previewDialog.getDialogPane().getButtonTypes().addAll(publishButtonType, ButtonType.CANCEL);

                        TextArea textArea = new TextArea(contenuIA);
                        textArea.setWrapText(true);
                        textArea.setPrefWidth(600);
                        textArea.setPrefHeight(400);
                        textArea.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");

                        previewDialog.getDialogPane()
                                .setContent(new VBox(10, new Label("Contenu généré par l'IA :"), textArea));
                        previewDialog.setResultConverter(dialogButton -> {
                            if (dialogButton == publishButtonType)
                                return textArea.getText();
                            return null;
                        });

                        Optional<String> resultText = previewDialog.showAndWait();

                        // 2. SAUVEGARDER SI CONFIRMÉ
                        if (resultText.isPresent()) {
                            String finalContent = resultText.get();
                            try {
                                int targetFormationId = idFormationSelectionnee != -1 ? idFormationSelectionnee : 1;
                                int moduleId = obtenirOuCreerModuleId(titre, targetFormationId);

                                if (moduleId != -1) {
                                    // Sauvegarder en tant que DESCRIPTION du module (pas un PDF/Word comme demandé)
                                    models.Module m = moduleServiceF.findById(moduleId);
                                    if (m != null) {
                                        m.setDescription(finalContent);
                                        moduleServiceF.update(m);
                                        afficherMessageIA(
                                                "✅ Programme publié avec succès dans la description du module !");

                                        // On peut quand même générer un Word local par sécurité, mais l'utilisateur ne
                                        // le voit pas "postulé"
                                        GeneratorServiceF.ecrireWord(titre, finalContent);
                                    }
                                } else {
                                    afficherMessageIA("❌ Impossible de créer le module dans la base de données.");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                afficherMessageIA("⚠️ Erreur lors de la publication : " + e.getMessage());
                            }
                        } else {
                            afficherMessageIA("Publication annulée. Le texte n'a pas été ajouté au module.");
                        }
                        btnGenererCours.setText("✨ Générer une autre version");
                        btnGenererCours.setDisable(false); // Enable it to allow generating again
                    });
                } else {
                    javafx.application.Platform.runLater(() -> {
                        loading.setVisible(false);
                        afficherMessageIA("❌ Erreur lors de la génération. Réessayez.");
                        btnGenererCours.setDisable(false);
                        btnGenererCours.setText("🧠 Générer le programme");
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    loading.setVisible(false);
                    afficherMessageIA("❌ Erreur : " + e.getMessage());
                    btnGenererCours.setDisable(false);
                });
            }
        }).start();
    }

    // Extraire le titre (1ère réponse du psychologue)
    private String extraireTitre() {
        for (Map<String, String> msg : historique) {
            if (msg.get("role").equals("user")) {
                return msg.get("content").replaceAll("[^a-zA-ZÀ-ÿ0-9\\s]", "").trim();
            }
        }
        return "Programme_" + System.currentTimeMillis();
    }

    private int obtenirOuCreerModuleId(String titre, int formationId) {
        // Logique pour insérer un nouveau module s'il n'existe pas et retourner son ID
        String checkSql = "SELECT id FROM module WHERE titre = ? AND id_formation = ?";
        String insertSql = "INSERT INTO module (titre, description, id_formation) VALUES (?, 'Généré par IA', ?)";

        try (java.sql.Connection conn = utils.MyDatabase.getInstance().getConnection()) {
            // Vérifier s'il existe
            try (java.sql.PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setString(1, titre);
                psCheck.setInt(2, formationId);
                try (java.sql.ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }
            // Sinon le créer
            try (java.sql.PreparedStatement psInsert = conn.prepareStatement(insertSql,
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                psInsert.setString(1, titre);
                psInsert.setInt(2, formationId);
                psInsert.executeUpdate();
                try (java.sql.ResultSet rsKeys = psInsert.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        return rsKeys.getInt(1);
                    }
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("❌ Erreur obtenirOuCreerModuleId : " + e.getMessage());
            e.printStackTrace();
        }
        return -1; // Retourner -1 au lieu de 1
    }

    // ─────────────────────────────────────────────
    // UI — Bulles de chat
    // ─────────────────────────────────────────────

    // Bulle IA (gauche) — style bleu clair
    private void afficherMessageIA(String texte) {
        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(5, 60, 5, 10));

        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 22px;");

        Label bulle = new Label(texte);
        bulle.setWrapText(true);
        bulle.setMaxWidth(420);
        bulle.setStyle(
                "-fx-background-color: #EEF2FF;" +
                        "-fx-background-radius: 0 15 15 15;" +
                        "-fx-padding: 12 16 12 16;" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: #2C3E50;" +
                        "-fx-border-color: #C7D2FE;" +
                        "-fx-border-radius: 0 15 15 15;");

        hbox.getChildren().addAll(avatar, bulle);
        chatContainer.getChildren().add(hbox);
    }

    // Bulle Psychologue (droite) — style vert foncé
    private void afficherMessagePsychologue(String texte) {
        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.setPadding(new Insets(5, 10, 5, 60));

        Label bulle = new Label(texte);
        bulle.setWrapText(true);
        bulle.setMaxWidth(420);
        bulle.setStyle(
                "-fx-background-color: linear-gradient(to right, #3A6B7E, #5C98A8);" +
                        "-fx-background-radius: 15 0 15 15;" +
                        "-fx-padding: 12 16 12 16;" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: white;");

        Label avatar = new Label("👨‍⚕️");
        avatar.setStyle("-fx-font-size: 22px;");

        hbox.getChildren().addAll(bulle, avatar);
        chatContainer.getChildren().add(hbox);
    }
}
