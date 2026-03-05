package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import services.ServiceChatQuiz;
import services.ServiceVoixQuiz;
import utils.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatQuizController {

    @FXML private VBox       chatBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField  champMessage;
    @FXML private Button     btnVoix;
    @FXML private Button     btnEnvoyer;   // ← ajouter fx:id="btnEnvoyer" dans FXML

    // ── Palette MindCare ─────────────────────────────────────────
    private static final String TEAL_DARK   = "#2D6E7E";
    private static final String TEAL_HOVER  = "#225A69";
    private static final String TEAL_MED    = "#5C98A8";
    private static final String TEAL_LIGHT  = "#D4EBF0";
    private static final String TEXT_DARK   = "#1F2A33";
    private static final String TEXT_GREY   = "#6E8E9A";

    private final ServiceChatQuiz serviceChat = new ServiceChatQuiz();
    private final List<String> historique = new ArrayList<>();

    // ════════════════════════════════════════════════════════════
    //  initialize()
    // ════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        var role = Session.getRoleConnecte();
        if (role != null
                && role != utils.Session.Role.USER
                && role != utils.Session.Role.ADMIN) {
            ajouterBulle("⛔ Accès réservé aux patients.", false, false);
            champMessage.setDisable(true);
            return;
        }

        // ✅ Fix couleur bouton envoi via Platform.runLater (bypass CSS Modena)
        Platform.runLater(() -> {
            if (btnEnvoyer != null) {
                CornerRadii rc = new CornerRadii(50);
                btnEnvoyer.setBackground(new Background(
                        new BackgroundFill(Color.web(TEAL_DARK), rc, Insets.EMPTY)));
                btnEnvoyer.setBorder(javafx.scene.layout.Border.EMPTY);
                btnEnvoyer.setTextFill(Color.WHITE);
                btnEnvoyer.setStyle(
                        "-fx-font-size:15px; -fx-font-weight:900;" +
                                "-fx-padding:11 18; -fx-cursor:hand;" +
                                "-fx-background-insets:0; -fx-border-insets:0;");
                btnEnvoyer.setOnMouseEntered(e -> btnEnvoyer.setBackground(new Background(
                        new BackgroundFill(Color.web(TEAL_HOVER), rc, Insets.EMPTY))));
                btnEnvoyer.setOnMouseExited(e  -> btnEnvoyer.setBackground(new Background(
                        new BackgroundFill(Color.web(TEAL_DARK), rc, Insets.EMPTY))));
            }
        });

        mettreAJourBoutonVoix();

        // Message d'accueil
        String bienvenue = "Bonjour ! Je suis ton assistant MindCare.\nComment te sens-tu aujourd'hui ?";
        ajouterBulle(bienvenue, false, true);
    }

    // ════════════════════════════════════════════════════════════
    //  Envoi message
    // ════════════════════════════════════════════════════════════
    @FXML
    public void envoyerMessage() {
        String texte = champMessage.getText().trim();
        if (texte.isEmpty()) return;

        ajouterBulle(texte, true, false);
        champMessage.clear();

        Label loading = new Label("💬 En train de répondre...");
        loading.setStyle("-fx-text-fill:" + TEXT_GREY + "; -fx-font-size:11px; -fx-padding:0 0 0 12;");
        chatBox.getChildren().add(loading);
        scrollerEnBas();

        final String question = texte;
        new Thread(() -> {
            String reponse = serviceChat.envoyerMessage(historique, question);
            historique.add(question);
            historique.add(reponse);

            Platform.runLater(() -> {
                chatBox.getChildren().remove(loading);
                ajouterBulle(reponse, false, true);
                scrollerEnBas();
            });
        }).start();
    }

    // ════════════════════════════════════════════════════════════
    //  Bouton voix — teal quand actif
    // ════════════════════════════════════════════════════════════
    @FXML
    public void toggleVoix() {
        ServiceVoixQuiz.setVoixActive(!ServiceVoixQuiz.isVoixActive());
        mettreAJourBoutonVoix();
        if (!ServiceVoixQuiz.isVoixActive()) ServiceVoixQuiz.arreter();
    }

    private void mettreAJourBoutonVoix() {
        if (btnVoix == null) return;
        if (ServiceVoixQuiz.isVoixActive()) {
            // ✅ Actif : teal clair (assortit avec l'interface)
            btnVoix.setText("🔊");
            btnVoix.setStyle(
                    "-fx-background-color:" + TEAL_LIGHT + ";" +
                            "-fx-text-fill:" + TEAL_DARK + ";" +
                            "-fx-background-radius:20; -fx-font-size:14px;" +
                            "-fx-cursor:hand; -fx-border-color:" + TEAL_MED + ";" +
                            "-fx-border-radius:20; -fx-border-width:1.5;" +
                            "-fx-padding:6 12;");
            btnVoix.setTooltip(new javafx.scene.control.Tooltip("Voix activée — cliquer pour désactiver"));
        } else {
            // Inactif : gris neutre
            btnVoix.setText("🔇");
            btnVoix.setStyle(
                    "-fx-background-color:#f1f5f9; -fx-text-fill:#94a3b8;" +
                            "-fx-background-radius:20; -fx-font-size:14px;" +
                            "-fx-cursor:hand; -fx-border-color:#e2e8f0;" +
                            "-fx-border-radius:20; -fx-border-width:1.5;" +
                            "-fx-padding:6 12;");
            btnVoix.setTooltip(new javafx.scene.control.Tooltip("Voix désactivée — cliquer pour activer"));
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Navigation
    // ════════════════════════════════════════════════════════════
    @FXML
    public void retourSuivie() {
        ServiceVoixQuiz.arreter();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/Suivie.fxml"));
            Node view = loader.load();
            VBox contentArea = (VBox) champMessage.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Bulles de chat — palette teal
    // ════════════════════════════════════════════════════════════
    /**
     * @param texte          contenu du message
     * @param estUtilisateur true = droite (user), false = gauche (IA)
     * @param lireVoix       true = lire à voix haute
     */
    private void ajouterBulle(String texte, boolean estUtilisateur, boolean lireVoix) {
        Label bulle = new Label(texte);
        bulle.setWrapText(true);
        bulle.setMaxWidth(340);

        if (estUtilisateur) {
            // ✅ Bulle user : teal foncé + texte blanc
            bulle.setStyle(
                    "-fx-background-color:" + TEAL_DARK + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 18 18 4 18;" +
                            "-fx-padding: 10 14;" +
                            "-fx-font-size: 13px; -fx-font-weight: 600;");
        } else {
            // ✅ Bulle IA : blanc + ombre teal douce
            bulle.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-text-fill:" + TEXT_DARK + ";" +
                            "-fx-background-radius: 18 18 18 4;" +
                            "-fx-padding: 10 14;" +
                            "-fx-font-size: 13px; -fx-font-weight: 600;" +
                            "-fx-effect: dropshadow(gaussian, rgba(45,110,126,0.10), 8, 0, 0, 2);");
            if (lireVoix) ServiceVoixQuiz.parler(texte);
        }

        HBox ligne = new HBox(bulle);
        ligne.setPadding(new Insets(4, 8, 4, 8));
        ligne.setAlignment(estUtilisateur ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chatBox.getChildren().add(ligne);
    }

    private void scrollerEnBas() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}