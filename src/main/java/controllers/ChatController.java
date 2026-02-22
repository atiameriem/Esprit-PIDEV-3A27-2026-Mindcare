package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import services.ServiceChat;
import utils.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatController {

    @FXML private VBox chatBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField champMessage;

    private final ServiceChat serviceChat = new ServiceChat();
    private final List<String> historique = new ArrayList<>();

    @FXML
    public void initialize() {
        // ✅ Vérification rôle — réservé aux patients
        var role = Session.getRoleConnecte();
        if (role != null
                && role != utils.Session.Role.USER
                && role != utils.Session.Role.ADMIN) {
            ajouterBulle("⛔ Accès réservé aux patients.", false);
            champMessage.setDisable(true);
            return;
        }
        ajouterBulle("Bonjour ! 👋 Je suis ton assistant MindCare.\nComment te sens-tu aujourd'hui ?", false);
    }

    @FXML
    public void envoyerMessage() {
        String texte = champMessage.getText().trim();
        if (texte.isEmpty()) return;

        ajouterBulle(texte, true);
        champMessage.clear();

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
                ajouterBulle(reponse, false);
                scrollerEnBas();
            });
        }).start();
    }

    @FXML
    public void retourSuivie() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Suivie.fxml"));
            Node view = loader.load();
            VBox contentArea = (VBox) champMessage.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ajouterBulle(String texte, boolean estUtilisateur) {
        Label bulle = new Label(texte);
        bulle.setWrapText(true);
        bulle.setMaxWidth(340);

        if (estUtilisateur) {
            bulle.setStyle(
                    "-fx-background-color:#7C3AED; -fx-text-fill:white;" +
                            "-fx-background-radius:18 18 4 18; -fx-padding:10 14;" +
                            "-fx-font-size:13px; -fx-font-weight:600;"
            );
        } else {
            bulle.setStyle(
                    "-fx-background-color:white; -fx-text-fill:#1F2937;" +
                            "-fx-background-radius:18 18 18 4; -fx-padding:10 14;" +
                            "-fx-font-size:13px; -fx-font-weight:600;" +
                            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,2);"
            );
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