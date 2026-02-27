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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import services.ServiceChat;
import services.ServiceVoix;
import utils.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatController {

    @FXML private VBox       chatBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField  champMessage;
    @FXML private Button     btnVoix;   // bouton 🔊/🔇 dans le FXML

    private final ServiceChat serviceChat = new ServiceChat();
    private final List<String> historique = new ArrayList<>();

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

        // Met à jour l'icône du bouton voix selon l'état initial
        mettreAJourBoutonVoix();

        // Message d'accueil — lu à voix haute par l'avatar
        String bienvenue = "Bonjour ! Je suis ton assistant MindCare.\nComment te sens-tu aujourd'hui ?";
        ajouterBulle(bienvenue, false, true);
    }

    // ══════════════════════════════════════════════════════════
    // ENVOI MESSAGE
    // ══════════════════════════════════════════════════════════
    @FXML
    public void envoyerMessage() {
        String texte = champMessage.getText().trim();
        if (texte.isEmpty()) return;

        // Message utilisateur (pas de voix)
        ajouterBulle(texte, true, false);
        champMessage.clear();

        // Indicateur "en train de répondre..."
        Label loading = new Label("💬 En train de répondre...");
        loading.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:11px; -fx-padding:0 0 0 12;");
        chatBox.getChildren().add(loading);
        scrollerEnBas();

        final String question = texte;
        new Thread(() -> {
            String reponse = serviceChat.envoyerMessage(historique, question);
            historique.add(question);
            historique.add(reponse);

            Platform.runLater(() -> {
                chatBox.getChildren().remove(loading);
                // Réponse de l'avatar — lue à voix haute
                ajouterBulle(reponse, false, true);
                scrollerEnBas();
            });
        }).start();
    }

    // ══════════════════════════════════════════════════════════
    // BOUTON VOIX (🔊 / 🔇)
    // ══════════════════════════════════════════════════════════
    @FXML
    public void toggleVoix() {
        ServiceVoix.setVoixActive(!ServiceVoix.isVoixActive());
        mettreAJourBoutonVoix();
        if (!ServiceVoix.isVoixActive()) ServiceVoix.arreter();
    }

    private void mettreAJourBoutonVoix() {
        if (btnVoix == null) return;
        if (ServiceVoix.isVoixActive()) {
            btnVoix.setText("🔊");
            btnVoix.setStyle(
                    "-fx-background-color:#ede9fe; -fx-text-fill:#7c3aed;"
                            + "-fx-background-radius:20; -fx-font-size:14px;"
                            + "-fx-cursor:hand; -fx-border-color:#c4b5fd;"
                            + "-fx-border-radius:20; -fx-border-width:1.5;"
                            + "-fx-padding:6 12;");
            btnVoix.setTooltip(new javafx.scene.control.Tooltip("Voix activée — cliquer pour désactiver"));
        } else {
            btnVoix.setText("🔇");
            btnVoix.setStyle(
                    "-fx-background-color:#f1f5f9; -fx-text-fill:#94a3b8;"
                            + "-fx-background-radius:20; -fx-font-size:14px;"
                            + "-fx-cursor:hand; -fx-border-color:#e2e8f0;"
                            + "-fx-border-radius:20; -fx-border-width:1.5;"
                            + "-fx-padding:6 12;");
            btnVoix.setTooltip(new javafx.scene.control.Tooltip("Voix désactivée — cliquer pour activer"));
        }
    }

    // ══════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════
    @FXML
    public void retourSuivie() {
        ServiceVoix.arreter(); // coupe la voix avant de quitter
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/Suivie.fxml"));
            Node view = loader.load();
            VBox contentArea = (VBox) champMessage.getScene()
                    .lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════
    // BULLE DE CHAT
    // ══════════════════════════════════════════════════════════
    /**
     * @param texte         contenu du message
     * @param estUtilisateur true = bulle droite (user), false = bulle gauche (avatar)
     * @param lireVoix      true = lire le texte à voix haute (avatar uniquement)
     */
    private void ajouterBulle(String texte, boolean estUtilisateur, boolean lireVoix) {
        Label bulle = new Label(texte);
        bulle.setWrapText(true);
        bulle.setMaxWidth(340);

        if (estUtilisateur) {
            bulle.setStyle(
                    "-fx-background-color:#7C3AED; -fx-text-fill:white;"
                            + "-fx-background-radius:18 18 4 18; -fx-padding:10 14;"
                            + "-fx-font-size:13px; -fx-font-weight:600;"
            );
        } else {
            bulle.setStyle(
                    "-fx-background-color:white; -fx-text-fill:#1F2937;"
                            + "-fx-background-radius:18 18 18 4; -fx-padding:10 14;"
                            + "-fx-font-size:13px; -fx-font-weight:600;"
                            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,2);"
            );
            // L'avatar parle — petit délai pour laisser l'UI s'afficher d'abord
            if (lireVoix) {
                ServiceVoix.parler(texte);
            }
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