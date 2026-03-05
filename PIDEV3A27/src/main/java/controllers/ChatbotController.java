package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.geometry.Pos;
import models.PostDraft;
import models.Post;
import services.HuggingFaceChatService;
import services.PostService;
import services.PointsService;
import services.ActivityService;
import services.ModerationService;
import utils.Session;
import utils.forum.PopupUtil;

/**
 * Vue Chatbot: discussion + génération d'un brouillon de post.
 * - Aucun SQL ici.
 * - Appels API via services.
 */
public class ChatbotController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox messagesBox;
    @FXML private TextArea inputArea;
    @FXML private Button sendBtn;
    @FXML private Button draftBtn;
    @FXML private Label statusLabel;

    private final HuggingFaceChatService hf = new HuggingFaceChatService();
    private final PostService postService = new PostService();
    private final PointsService pointsService = new PointsService();
    private final ActivityService activityService = new ActivityService();
    private final ModerationService moderationService = new ModerationService();
    private final StringBuilder history = new StringBuilder();

    private Node typingNode;

    @FXML
    public void initialize() {
        // Styles sont définis dans forum.css (Chatbot.fxml)
        addBotMessage("Salut 🙂 Dis-moi ce que tu veux publier, et je t'aide à le formuler proprement.");

        sendBtn.setOnAction(e -> onSend());
        draftBtn.setOnAction(e -> onDraft());

        inputArea.setOnKeyPressed(ev -> {
            switch (ev.getCode()) {
                case ENTER -> {
                    if (ev.isControlDown()) onSend();
                }
            }
        });
    }

    private void onSend() {
        String msg = inputArea.getText();
        if (msg == null || msg.isBlank()) return;

        inputArea.clear();
        addUserMessage(msg);

        showTyping(true);
        statusLabel.setText("⏳ Réponse en cours...");

        // Appel API dans un thread pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                String reply = hf.chatReply(history.toString(), msg);
                Platform.runLater(() -> {
                    showTyping(false);
                    addBotMessage(reply == null || reply.isBlank() ? "(réponse vide)" : reply);
                    statusLabel.setText("");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showTyping(false);
                    statusLabel.setText("");
                    PopupUtil.showError(ex.getMessage() == null ? "Erreur chatbot" : ("Erreur chatbot: " + ex.getMessage()));
                });
            }
        }, "hf-chat").start();
    }

    private void onDraft() {
        String convo = history.toString();
        if (convo.isBlank()) {
            statusLabel.setText("Écris d'abord un message au chatbot 🙂");
            return;
        }
        showTyping(true);
        statusLabel.setText("⏳ Génération du post...");

        new Thread(() -> {
            try {
                PostDraft draft = hf.draftPost(convo);
                Platform.runLater(() -> {
                    showTyping(false);
                    statusLabel.setText("");
                    if (draft == null) {
                        statusLabel.setText("Impossible de générer le post.");
                        return;
                    }
                    addBotMessage("Voici une reformulation prête à publier 👇");
                    addDraftPanel(draft);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showTyping(false);
                    statusLabel.setText("");
                    PopupUtil.showError(ex.getMessage() == null ? "Erreur génération" : ("Erreur génération: " + ex.getMessage()));
                });
            }
        }, "hf-draft").start();
    }

    private void addDraftPanel(PostDraft draft) {
        // Draft card under the reformulation
        VBox card = new VBox(10);
        card.getStyleClass().add("draftCard");

        Label t = new Label(draft.getTitle() == null ? "" : draft.getTitle());
        t.getStyleClass().add("draftTitle");
        t.setWrapText(true);

        Label c = new Label(draft.getContent() == null ? "" : draft.getContent());
        c.getStyleClass().add("draftBody");
        c.setWrapText(true);

        Button del = new Button("Supprimer");
        del.getStyleClass().add("dangerBtn");

        Button copyTitle = new Button("Copier titre");
        copyTitle.getStyleClass().add("secondaryBtn");

        Button copyContent = new Button("Copier contenu");
        copyContent.getStyleClass().add("secondaryBtn");

        Button addPost = new Button("Ajouter post");
        addPost.getStyleClass().add("sendBtn");

        HBox actions = new HBox(10, del, copyTitle, copyContent, addPost);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(new Label("Titre"), t, new Label("Contenu"), c, actions);

        // Wrap in an HBox aligned left (like bot messages)
        HBox line = new HBox(card);
        line.setAlignment(Pos.CENTER_LEFT);
        messagesBox.getChildren().add(line);

        del.setOnAction(e -> messagesBox.getChildren().remove(line));
        copyTitle.setOnAction(e -> copyToClipboard(t.getText()));
        copyContent.setOnAction(e -> copyToClipboard(c.getText()));

        addPost.setOnAction(e -> {
            try {
                if (Session.idUsers <= 0) {
                    PopupUtil.showError("Session utilisateur invalide.");
                    return;
                }
                String title = (t.getText() == null ? "" : t.getText().trim());
                String content = (c.getText() == null ? "" : c.getText().trim());
                if (title.length() < 3) {
                    PopupUtil.showInfo("Le titre doit contenir au moins 3 caractères.");
                    return;
                }
                if (content.length() < 10) {
                    PopupUtil.showInfo("Le contenu doit contenir au moins 10 caractères.");
                    return;
                }

                // Use the same core DB logic as the forum
                moderationService.validatePost(Session.idUsers, title, content);

                Post p = new Post();
                p.setIdUsers(Session.idUsers);
                p.setTitle(title);
                p.setContent(content);
                p.setStatus("PUBLISHED");
                p.setLanguage("fr");

                long newId = postService.createPost(p);

                try { pointsService.addPoints(Session.idUsers, 10); } catch (Exception ignored) {}
                try { activityService.log(Session.idUsers, "CREATE_POST", "postId=" + newId + " (chatbot)"); } catch (Exception ignored) {}

                PopupUtil.showSuccess("Post ajouté ✅");
            } catch (Exception ex) {
                PopupUtil.showError(ex.getMessage() == null ? "Erreur" : ex.getMessage());
            }
        });

        Platform.runLater(() -> {
            if (scrollPane != null) scrollPane.setVvalue(1.0);
        });
    }

    private void copyToClipboard(String text) {
        if (text == null) text = "";
        final javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
        final javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(text);
        cb.setContent(cc);
        statusLabel.setText("Copié ✅");
    }

    private void addUserMessage(String msg) {
        history.append("Utilisateur: ").append(msg).append("\n");
        addMessageBubble(msg, true);
    }

    private void addBotMessage(String msg) {
        history.append("Assistant: ").append(msg).append("\n");
        addMessageBubble(msg, false);
    }

    private void addMessageBubble(String msg, boolean isUser) {
        Label lbl = new Label(msg);
        lbl.setWrapText(true);
        lbl.setMaxWidth(520);

        lbl.getStyleClass().addAll("bubble", isUser ? "bubbleUser" : "bubbleBot");

        HBox line = new HBox(lbl);
        line.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messagesBox.getChildren().add(line);

        // Auto-scroll en bas à chaque nouveau message
        Platform.runLater(() -> {
            if (scrollPane != null) scrollPane.setVvalue(1.0);
        });
    }

    private void showTyping(boolean show) {
        if (show) {
            if (typingNode != null) return;
            Label dots = new Label("···");
            dots.getStyleClass().addAll("bubble", "bubbleBot", "typingDots");
            HBox line = new HBox(dots);
            line.setAlignment(Pos.CENTER_LEFT);
            typingNode = line;
            messagesBox.getChildren().add(line);
            Platform.runLater(() -> {
                if (scrollPane != null) scrollPane.setVvalue(1.0);
            });
        } else {
            if (typingNode != null) {
                messagesBox.getChildren().remove(typingNode);
                typingNode = null;
            }
        }
    }
}
