package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import models.Commentaire;
import models.Post;
import services.CommentService;
import services.LikeService;
import services.PostService;
import services.ReportService;
import utils.Session;
import utils.forum.ImageStorage;
import utils.forum.Permissions;
import utils.forum.UiFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller = UI + events (les règles/SQL sont dans utils/services).
 */
public class ForumController {

    // Top bar
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button addPostButton;

    // Tabs
    @FXML private ToggleGroup tabsGroup;
    @FXML private ToggleButton tabAllPosts;
    @FXML private ToggleButton tabMyPosts;
    @FXML private ToggleButton tabReports; // admin uniquement

    // Content
    @FXML private VBox postsContainer;

    private final PostService postService = new PostService();
    private final CommentService commentService = new CommentService();
    private final LikeService likeService = new LikeService();
    private final ReportService reportService = new ReportService();

    private final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String formatDate(LocalDateTime dt) {
        return dt == null ? "" : DT.format(dt);
    }

    private boolean isMyTab() {
        return tabMyPosts != null && tabMyPosts.isSelected();
    }

    @FXML
    public void initialize() {
        System.out.println("Vue Forum chargée");

        // Force une première synchronisation (role/admin) dès le chargement
        Permissions.isAdmin();

        applyAdminTabVisibility();

        if (sortCombo != null) {
            sortCombo.getItems().setAll("Plus récents", "Plus anciens", "Plus aimés", "Plus commentés");
            sortCombo.getSelectionModel().selectFirst();
            sortCombo.setOnAction(e -> refreshPosts());
        }

        if (tabsGroup != null) {
            tabsGroup.selectedToggleProperty().addListener((obs, o, n) -> {
                applyAdminTabVisibility();
                refreshPosts();
            });
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> refreshPosts());
        }

        if (addPostButton != null) {
            addPostButton.setOnAction(e -> openAddPostDialog());
        }

        refreshPosts();
    }

    private void refreshPosts() {
        if (postsContainer == null) return;

        // Admin: onglet signalements
        if (Permissions.isAdmin() && tabReports != null && tabReports.isSelected()) {
            refreshReportedPosts();
            return;
        }

        String search = (searchField == null) ? "" : searchField.getText();
        String sort = (sortCombo == null || sortCombo.getValue() == null) ? "Plus récents" : sortCombo.getValue();

        try {
            List<Post> posts = postService.findPosts(isMyTab(), Session.idUsers, search, sort);
            postsContainer.getChildren().clear();

            for (Post p : posts) {
                postsContainer.getChildren().add(buildPostCard(p));
            }

            if (posts.isEmpty()) {
                postsContainer.getChildren().add(buildEmptyState());
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les posts.\n" + ex.getMessage());
        }
    }

    private Node buildEmptyState() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);
        Label icon = new Label("💬");
        icon.setStyle("-fx-font-size: 40px;");
        Label txt = new Label("Aucun post pour le moment");
        txt.setStyle("-fx-text-fill: #7a8795; -fx-font-size: 14px;");
        box.getChildren().addAll(icon, txt);
        return box;
    }

    private VBox buildPostCard(Post p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("avatar");
        Label av = new Label("U");
        av.getStyleClass().add("avatarText");
        avatar.getChildren().add(av);

        VBox info = new VBox(2);
        Label name = new Label("User #" + p.getIdUsers());
        name.getStyleClass().add("name");

        Label created = new Label("Publié le " + formatDate(p.getCreatedAt()));
        created.getStyleClass().add("meta");
        info.getChildren().addAll(name, created);

        if (p.getUpdatedAt() != null) {
            Label updated = new Label("Modifié le " + formatDate(p.getUpdatedAt()));
            updated.getStyleClass().add("meta");
            info.getChildren().add(updated);
        }

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        int ownerId = p.getIdUsers();

        boolean showEdit = Permissions.canEdit(ownerId);
        boolean enableEdit = showEdit;

        boolean showDelete = Permissions.canDelete(ownerId);
        boolean enableDelete = showDelete;

        boolean showRep = Permissions.canReport(ownerId);

        ContextMenu postMenu = UiFactory.buildIconMenu(
                showEdit, enableEdit,
                () -> openEditPostDialog(p),
                showDelete, enableDelete,
                () -> {
                    try {
                        postService.deletePost(p.getId());
                        // si admin supprime un post signalé, on nettoie les reports
                        if (Permissions.isAdmin()) {
                            reportService.deleteReportsForPost(p.getId());
                        }
                        refreshPosts();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                    }
                },
                showRep,
                () -> openReportDialogPost(p.getId())
        );

        Button dots = UiFactory.dotsButton(postMenu);
        header.getChildren().addAll(avatar, info, spacer, dots);

        // Title
        Label title = new Label(p.getTitle());
        title.getStyleClass().add("postTitle");

        // Body with optional image
        HBox body = new HBox(12);

        VBox left = new VBox(8);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label content = new Label(p.getContent());
        content.setWrapText(true);
        content.getStyleClass().add("postText");

        HBox actions = new HBox(10);

        Button likeBtn = new Button("♡ J'aime (" + p.getLikesCount() + ")");
        likeBtn.getStyleClass().add("actionBtn");
        likeBtn.setOnAction(e -> {
            try {
                likeService.togglePostLike(p.getId(), Session.idUsers);
                refreshPosts();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
            }
        });

        Button commentBtn = new Button("💬 Commenter (" + p.getCommentsCount() + ")");
        commentBtn.getStyleClass().add("actionBtn");

        actions.getChildren().addAll(likeBtn, commentBtn);

        // Comments area (collapsible)
        VBox commentsBox = new VBox(8);
        commentsBox.setPadding(new Insets(10, 0, 0, 0));
        commentsBox.setVisible(false);
        commentsBox.setManaged(false);

        VBox list = new VBox(8);

        HBox addRow = new HBox(10);
        addRow.setAlignment(Pos.CENTER_LEFT);
        TextField tf = new TextField();
        tf.setPromptText("Ajouter un commentaire...");
        tf.getStyleClass().add("commentField");
        HBox.setHgrow(tf, Priority.ALWAYS);

        Button publish = new Button("Publier");
        publish.getStyleClass().add("primaryBtnSmall");

        publish.setOnAction(ev -> {
            String txt = tf.getText();
            if (txt == null || txt.isBlank()) return;
            try {
                commentService.addComment(p.getId(), Session.idUsers, txt);
                tf.clear();
                loadCommentsInto(list, p.getId());
                refreshPosts();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
            }
        });

        addRow.getChildren().addAll(tf, publish);
        commentsBox.getChildren().addAll(list, addRow);

        commentBtn.setOnAction(e -> {
            boolean show = !commentsBox.isVisible();
            commentsBox.setVisible(show);
            commentsBox.setManaged(show);
            if (show) loadCommentsInto(list, p.getId());
        });

        left.getChildren().addAll(content, actions, commentsBox);
        body.getChildren().add(left);

        if (p.getFirstImagePath() != null && !p.getFirstImagePath().isBlank()) {
            File f = new File(p.getFirstImagePath());
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString(), 150, 105, false, true));
                iv.getStyleClass().add("postImage");
                body.getChildren().add(iv);
            }
        }

        card.getChildren().addAll(header, title, body);
        return card;
    }

    private void openEditPostDialog(Post p) {
        if (p == null) return;
        if (!Permissions.canEdit(p.getIdUsers())) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Modifier le Post");
        UiFactory.styleDialog(dialog.getDialogPane());

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField tfTitle = new TextField(p.getTitle());
        TextArea taContent = new TextArea(p.getContent());
        taContent.setPrefRowCount(6);

        VBox box = new VBox(10,
                new Label("Titre"), tfTitle,
                new Label("Contenu"), taContent
        );
        box.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                String title = tfTitle.getText();
                String content = taContent.getText();
                if (title == null || title.isBlank() || content == null || content.isBlank()) {
                    showAlert(Alert.AlertType.WARNING, "Champs obligatoires", "Titre et contenu sont obligatoires.");
                    return null;
                }
                try {
                    postService.updatePost(p.getId(), title, content);
                    refreshPosts();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void openEditCommentDialog(Commentaire c, long postId, VBox list) {
        if (c == null) return;
        if (!Permissions.canEdit(c.getIdUsers())) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Modifier le commentaire");
        UiFactory.styleDialog(dialog.getDialogPane());

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextArea ta = new TextArea(c.getContent());
        ta.setPrefRowCount(4);
        ta.setWrapText(true);

        VBox box = new VBox(10, new Label("Contenu"), ta);
        box.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                String content = ta.getText();
                if (content == null || content.isBlank()) return null;
                try {
                    commentService.updateComment(c.getId(), content);
                    loadCommentsInto(list, postId);
                    refreshPosts();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void loadCommentsInto(VBox list, long postId) {
        list.getChildren().clear();
        try {
            List<Commentaire> comments = commentService.getCommentsByPost(postId);
            if (comments.isEmpty()) {
                Label none = new Label("Aucun commentaire");
                none.getStyleClass().add("meta");
                list.getChildren().add(none);
                return;
            }

            for (Commentaire c : comments) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);

                StackPane av = new StackPane();
                av.getStyleClass().add("avatarSmall");
                Label t = new Label("U");
                t.getStyleClass().add("avatarTextSmall");
                av.getChildren().add(t);

                VBox textBox = new VBox(2);
                HBox.setHgrow(textBox, Priority.ALWAYS);
                Label who = new Label("User #" + c.getIdUsers());
                who.getStyleClass().add("name");

                Label cCreated = new Label("Publié le " + formatDate(c.getCreatedAt()));
                cCreated.getStyleClass().add("meta");

                Label cUpdated = null;
                if (c.getUpdatedAt() != null) {
                    cUpdated = new Label("Modifié le " + formatDate(c.getUpdatedAt()));
                    cUpdated.getStyleClass().add("meta");
                }

                Label msg = new Label(c.getContent());
                msg.setWrapText(true);
                msg.getStyleClass().add("commentText");

                textBox.getChildren().addAll(who, cCreated);
                if (cUpdated != null) textBox.getChildren().add(cUpdated);
                textBox.getChildren().add(msg);

                Button like = new Button("👍 " + c.getLikesCount());
                like.getStyleClass().add("actionBtn");
                like.setOnAction(e -> {
                    try {
                        likeService.toggleCommentLike(c.getId(), Session.idUsers);
                        loadCommentsInto(list, postId);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                    }
                });

                int ownerId = c.getIdUsers();
                boolean showEdit = Permissions.canEdit(ownerId);
                boolean enableEdit = showEdit;
                boolean showDelete = Permissions.canDelete(ownerId);
                boolean enableDelete = showDelete;
                boolean showRep = Permissions.canReport(ownerId);

                ContextMenu cMenu = UiFactory.buildIconMenu(
                        showEdit, enableEdit,
                        () -> openEditCommentDialog(c, postId, list),
                        showDelete, enableDelete,
                        () -> {
                            try {
                                commentService.deleteComment(c.getId());
                                loadCommentsInto(list, postId);
                                refreshPosts();
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                            }
                        },
                        showRep,
                        () -> openReportDialogComment(c.getId())
                );

                Button dots = UiFactory.dotsButton(cMenu);

                row.getChildren().addAll(av, textBox, like, dots);
                list.getChildren().add(row);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Label err = new Label("Erreur chargement commentaires: " + ex.getMessage());
            err.getStyleClass().add("meta");
            list.getChildren().add(err);
        }
    }

    private void openAddPostDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un Post");
        UiFactory.styleDialog(dialog.getDialogPane());

        ButtonType addType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        TextField tfTitle = new TextField();
        tfTitle.setPromptText("Titre");

        TextArea taContent = new TextArea();
        taContent.setPromptText("Contenu");
        taContent.setPrefRowCount(6);

        Label imgLabel = new Label("Aucune image");
        Button chooseImg = new Button("Choisir une image...");

        final Path[] chosen = {null};

        chooseImg.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
            );
            File file = fc.showOpenDialog(addPostButton.getScene().getWindow());
            if (file != null) {
                chosen[0] = file.toPath();
                imgLabel.setText(file.getName());
            }
        });

        VBox box = new VBox(10,
                new Label("Titre"), tfTitle,
                new Label("Contenu"), taContent,
                new HBox(10, chooseImg, imgLabel)
        );
        box.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(bt -> {
            if (bt == addType) {
                String title = tfTitle.getText();
                String content = taContent.getText();
                if (title == null || title.isBlank() || content == null || content.isBlank()) {
                    showAlert(Alert.AlertType.WARNING, "Champs obligatoires", "Titre et contenu sont obligatoires.");
                    return null;
                }

                try {
                    String storedPath = null;
                    if (chosen[0] != null) {
                        storedPath = ImageStorage.copyToUploads(chosen[0]);
                    }

                    Post p = new Post();
                    p.setIdUsers(Session.idUsers);
                    p.setTitle(title);
                    p.setContent(content);
                    p.setStatus("PUBLISHED");
                    p.setLanguage("fr");

                    postService.createPostWithOptionalImage(p, storedPath);
                    refreshPosts();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
                } catch (IOException ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de copier l'image: " + ex.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void openReportDialogPost(long postId) {
        // admin ne peut pas signaler
        if (Permissions.isAdmin()) {
            showAlert(Alert.AlertType.INFORMATION, "Interdit", "L'admin ne peut pas signaler.");
            return;
        }

        ChoiceDialog<String> dlg = new ChoiceDialog<>("SPAM", "SPAM", "ABUS", "HARCÈLEMENT", "AUTRE");
        dlg.setTitle("Signaler un post");
        dlg.setHeaderText("Choisir une raison");
        dlg.setContentText("Raison:");
        UiFactory.styleDialog(dlg.getDialogPane());

        dlg.showAndWait().ifPresent(reason -> {
            try {
                reportService.reportPost(postId, Session.idUsers, reason, null);
                showAlert(Alert.AlertType.INFORMATION, "OK", "Signalement envoyé.");
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
            }
        });
    }

    private void openReportDialogComment(long commentId) {
        if (Permissions.isAdmin()) {
            showAlert(Alert.AlertType.INFORMATION, "Interdit", "L'admin ne peut pas signaler.");
            return;
        }

        ChoiceDialog<String> dlg = new ChoiceDialog<>("SPAM", "SPAM", "ABUS", "HARCÈLEMENT", "AUTRE");
        dlg.setTitle("Signaler un commentaire");
        dlg.setHeaderText("Choisir une raison");
        dlg.setContentText("Raison:");
        UiFactory.styleDialog(dlg.getDialogPane());

        dlg.showAndWait().ifPresent(reason -> {
            try {
                reportService.reportComment(commentId, Session.idUsers, reason, null);
                showAlert(Alert.AlertType.INFORMATION, "OK", "Signalement envoyé.");
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        UiFactory.styleDialog(a.getDialogPane());
        a.showAndWait();
    }

    // -------------------------
    // Admin - Signalements
    // -------------------------

    private void refreshReportedPosts() {
        postsContainer.getChildren().clear();
        try {
            List<ReportService.ReportAgg> aggs = reportService.getReportedPostsAgg();
            if (aggs.isEmpty()) {
                Label none = new Label("Aucun signalement");
                none.getStyleClass().add("meta");
                VBox box = new VBox(8, none);
                box.setPadding(new Insets(20));
                postsContainer.getChildren().add(box);
                return;
            }

            for (ReportService.ReportAgg a : aggs) {
                Post p = postService.getPostById(a.postId);
                if (p == null) continue;
                postsContainer.getChildren().add(buildReportedPostCard(p, a));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    private VBox buildReportedPostCard(Post p, ReportService.ReportAgg agg) {
        VBox card = buildPostCard(p);

        Label badge = new Label("🚩 " + agg.total + " signalement(s)");
        badge.getStyleClass().add("reportBadge");

        String reasons = (agg.reasons == null || agg.reasons.isEmpty()) ? "" : String.join(", ", agg.reasons);
        Label reasonsLabel = new Label(reasons.isBlank() ? "" : ("Raisons: " + reasons));
        reasonsLabel.getStyleClass().add("meta");
        reasonsLabel.setWrapText(true);

        HBox top = new HBox(10, badge);
        top.setAlignment(Pos.CENTER_LEFT);
        if (!reasons.isBlank()) {
            VBox v = new VBox(4, top, reasonsLabel);
            card.getChildren().add(0, v);
        } else {
            card.getChildren().add(0, top);
        }

        Button view = new Button("Voir");
        view.getStyleClass().add("actionBtn");
        view.setOnAction(e -> openViewPostDialog(p));

        Button done = new Button("Marquer traité");
        done.getStyleClass().add("actionBtn");
        done.setOnAction(e -> {
            try {
                reportService.deleteReportsForPost(p.getId());
                refreshReportedPosts();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
            }
        });

        HBox actions = new HBox(10, view, done);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));
        card.getChildren().add(actions);

        return card;
    }

    private void openViewPostDialog(Post p) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Post signalé");
        UiFactory.styleDialog(dialog.getDialogPane());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label t = new Label(p.getTitle());
        t.getStyleClass().add("postTitle");

        Label meta = new Label("User #" + p.getIdUsers() + " • Publié le " + formatDate(p.getCreatedAt()));
        meta.getStyleClass().add("meta");

        if (p.getUpdatedAt() != null) {
            meta = new Label(meta.getText() + " • Modifié le " + formatDate(p.getUpdatedAt()));
            meta.getStyleClass().add("meta");
        }

        Label c = new Label(p.getContent());
        c.setWrapText(true);
        c.getStyleClass().add("postText");

        VBox box = new VBox(10, t, meta, c);
        if (p.getFirstImagePath() != null && !p.getFirstImagePath().isBlank()) {
            File f = new File(p.getFirstImagePath());
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString(), 420, 280, true, true));
                iv.getStyleClass().add("postImage");
                box.getChildren().add(iv);
            }
        }
        box.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
    }

    private void applyAdminTabVisibility() {
        boolean show = Permissions.isAdmin();
        System.out.println("[FORUM] admin? " + show + " | sessionRole=" + Session.role + " | id=" + Session.idUsers);

        if (tabReports != null) {
            tabReports.setVisible(show);
            tabReports.setManaged(show);

            if (!show && tabReports.isSelected() && tabAllPosts != null) {
                tabAllPosts.setSelected(true);
            }
        }
    }
}
