package controllers;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
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
import services.TranslateService;
import services.SpeechToTextService;
import services.AdminReportService;
import utils.Session;
import utils.forum.ImageStorage;
import utils.forum.Permissions;
import utils.forum.UiFactory;
import utils.forum.PopupUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


/**
 * Controller = UI + events (les règles/SQL sont dans utils/services).
 */
public class ForumController {

    // Top bar
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button notifButton;
    @FXML private Label notifBadge;
    @FXML private Button addPostButton;

    // Tabs
    @FXML private ToggleGroup tabsGroup;
    @FXML private ToggleButton tabAllPosts;
    @FXML private ToggleButton tabMyPosts;
    @FXML private ToggleButton tabReports; // admin uniquement
    @FXML private ToggleButton tabDashboard; // admin uniquement

    // Content
    @FXML private VBox postsContainer;
    @FXML private VBox recommendedContainer;

    private final PostService postService = new PostService();
    private final CommentService commentService = new CommentService();
    private final LikeService likeService = new LikeService();
    private final ReportService reportService = new ReportService();

    // Nouveaux modules (forum only)
    private final services.ModerationService moderationService = new services.ModerationService();
    private final services.PointsService pointsService = new services.PointsService();
    private final services.ActivityService activityService = new services.ActivityService();
    private final services.NotificationService notificationService = new services.NotificationService();
    private final services.AdminDashboardService adminDashboardService = new services.AdminDashboardService();
    // APIs (utilisées uniquement pour l'affichage / saisie côté Forum)
    private final TranslateService translateService = new TranslateService();
    private final SpeechToTextService sttService = new SpeechToTextService();

    // Admin Reports (logique email + SQL hors controller)
    private final AdminReportService adminReportService = new AdminReportService();

    // UI state: langue sélectionnée par post
    private final Map<Long, String> postLang = new HashMap<>();
    // cache badges (évite une requête DB par carte)
    private final Map<Integer, String> badgeCache = new HashMap<>();
    // cache simple de traduction (clé = refType:id:lang)
    private final Map<String, String> translationCache = new HashMap<>();
    private boolean translationErrorShown = false;

    private final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String formatDate(LocalDateTime dt) {
        return dt == null ? "" : DT.format(dt);
    }
    // -------------------------
    // -------------------------
    // Traduction (à l'affichage)
    // -------------------------
    private String getSelectedLang(long postId) {
        return postLang.getOrDefault(postId, ""); // "" = original
    }

    private String mapLangDisplayToCode(String display) {
        if (display == null) return "";
        String v = display.trim();
        if (v.equalsIgnoreCase("Français") || v.equalsIgnoreCase("Francais")) return "fr";
        if (v.equalsIgnoreCase("Anglais") || v.equalsIgnoreCase("English")) return "en";
        if (v.equalsIgnoreCase("Deutsch") || v.equalsIgnoreCase("Allemand") || v.equalsIgnoreCase("German")) return "de";
        if (v.equalsIgnoreCase("Italien") || v.equalsIgnoreCase("Italiano") || v.equalsIgnoreCase("Italian")) return "it";
        if (v.equalsIgnoreCase("Espagnol") || v.equalsIgnoreCase("Español") || v.equalsIgnoreCase("Spanish")) return "es";
        if (v.equalsIgnoreCase("Original")) return "";
        return "";
    }

    private String mapCodeToDisplay(String code) {
        if (code == null) return "Original";
        String c = code.trim().toLowerCase();
        return switch (c) {
            case "fr" -> "Français";
            case "en" -> "Anglais";
            case "de" -> "Deutsch";
            case "it" -> "Italien";
            case "es" -> "Espagnol";
            default -> "Original";
        };
    }


    private String cacheKey(String refType, long refId, String lang) {
        return refType + ":" + refId + ":" + (lang == null ? "" : lang);
    }

    private void translateAsync(String refType, long refId, String originalText, String targetLang,
                                java.util.function.Consumer<String> onDone) {

        if (originalText == null) originalText = "";

        // Original = pas de traduction
        if (targetLang == null || targetLang.isBlank()) {
            onDone.accept(originalText);
            return;
        }

        String key = cacheKey(refType, refId, targetLang);
        String cached = translationCache.get(key);
        if (cached != null) {
            onDone.accept(cached);
            return;
        }

        final String textToTranslate = originalText;

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return translateService.translate(textToTranslate, "auto", targetLang);
            }
        };

        task.setOnSucceeded(e -> {
            String tr = task.getValue();
            if (tr == null || tr.isBlank()) tr = textToTranslate;

            translationCache.put(key, tr);
            onDone.accept(tr);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("[TRANSLATE ERROR] " + (ex == null ? "unknown" : ex.getMessage()));
            if (ex != null) ex.printStackTrace();

            // ✅ Popup 1 seule fois (pour éviter spam)
            if (!translationErrorShown) {
                translationErrorShown = true;
                String msg = (ex == null) ? "Erreur inconnue." : ex.getMessage();
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Traduction", "API traduction indisponible:\n" + msg)
                );
            }

            // fallback : original
            onDone.accept(textToTranslate);
        });

        Thread th = new Thread(task, "translate-" + refType + "-" + refId);
        th.setDaemon(true);
        th.start();
    }


    // -------------------------
    // Speech-to-Text (Vosk)
    // -------------------------
    private void bindSpeech(Button micBtn, TextInputControl target) {
        micBtn.setOnAction(e -> {
            if (sttService.isRunning()) {
                sttService.stop();
                micBtn.setText("🎤");
                return;
            }
            micBtn.setText("⏹");
            sttService.start(text -> {
                Platform.runLater(() -> {
                    if (text == null || text.isBlank()) return;
                    if (text.startsWith("[STT ERROR]")) {
                        showAlert(Alert.AlertType.ERROR, "STT", text);
                        micBtn.setText("🎤");
                        sttService.stop();
                        return;
                    }
                    if (target.getText() == null || target.getText().isBlank()) target.setText(text);
                    else target.appendText(" " + text);
                });
            });
        });
    }


    // Validation UI (pas de SQL ici)
    // -------------------------
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void showFieldError(Control field, Label errorLabel, String message) {
        if (field != null && !field.getStyleClass().contains("field-error")) {
            field.getStyleClass().add("field-error");
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void clearFieldError(Control field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("field-error");
        }
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
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

        refreshRecommended();
        refreshNotifBadge();
        refreshPosts();
    }

    private void refreshPosts() {
        if (postsContainer == null) return;

        // Right column:
        // - Tous les posts  => Recommandés
        // - Mes posts       => Profil
        // - Signalements/Dashboard => hidden
        if (Permissions.isAdmin()) {
            if (tabReports != null && tabReports.isSelected()) {
                applySidebarVisibility(false);
                if (recommendedContainer != null) recommendedContainer.getChildren().clear();
            } else if (tabDashboard != null && tabDashboard.isSelected()) {
                applySidebarVisibility(false);
                if (recommendedContainer != null) recommendedContainer.getChildren().clear();
            } else if (tabMyPosts != null && tabMyPosts.isSelected()) {
                applySidebarVisibility(true);
                refreshProfileSidebar();
            } else {
                // default: all posts
                applySidebarVisibility(true);
                refreshRecommended();
            }
        } else {
            if (tabMyPosts != null && tabMyPosts.isSelected()) {
                applySidebarVisibility(true);
                refreshProfileSidebar();
            } else {
                applySidebarVisibility(true);
                refreshRecommended();
            }
        }

        // Admin: onglet dashboard
        if (Permissions.isAdmin() && tabDashboard != null && tabDashboard.isSelected()) {
            refreshDashboard();
            return;
        }

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

        // sync badge notif (ne doit jamais bloquer)
        refreshNotifBadge();
    }

        private boolean shouldShowSidebar() {
        // Sidebar is hidden only for admin tabs (Signalements / Dashboard).
        if (Permissions.isAdmin()) {
            if (tabReports != null && tabReports.isSelected()) return false;
            if (tabDashboard != null && tabDashboard.isSelected()) return false;
        }
        return true;
    }

    private void applySidebarVisibility(boolean show) {
        // The right sidebar (recommended/profile) is hidden only for admin tabs.
        if (recommendedContainer != null) {
            recommendedContainer.setVisible(show);
            recommendedContainer.setManaged(show);
        }

        // Posts list must ALWAYS use the full available width of the left column.
        // (Previous versions were centering the list when the sidebar was visible.)
        if (postsContainer != null) {
            postsContainer.setFillWidth(true);
            postsContainer.setAlignment(Pos.TOP_LEFT);
        }
    }

    // -------------------------
    // Sidebar: Posts recommandés (Top score)
    // -------------------------
    private void refreshRecommended() {
        if (recommendedContainer == null) return;
        recommendedContainer.getChildren().clear();

        Label h = new Label("Recommandés");
        h.getStyleClass().add("sectionTitle");
        recommendedContainer.getChildren().add(h);

        try {
            List<Post> top = postService.findTopScoredPosts(2);
            if (top.isEmpty()) {
                Label empty = new Label("Aucun post");
                empty.getStyleClass().add("meta");
                recommendedContainer.getChildren().add(empty);
                return;
            }
            for (Post p : top) {
                recommendedContainer.getChildren().add(buildRecommendedCard(p));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label err = new Label("Erreur chargement");
            err.getStyleClass().add("meta");
            recommendedContainer.getChildren().add(err);
        }
    }

    private VBox buildRecommendedCard(Post p) {
        VBox card = new VBox(6);
        card.getStyleClass().add("sideCard");

        Label t = new Label(p.getTitle());
        t.getStyleClass().add("postTitle");
        t.setWrapText(true);

        Label author = new Label(p.getAuthorFullName());
        author.getStyleClass().add("meta");

        String raw = p.getContent() == null ? "" : p.getContent().trim();
        String excerpt = raw.length() > 120 ? raw.substring(0, 120) + "…" : raw;
        Label ex = new Label(excerpt);
        ex.getStyleClass().add("excerpt");
        ex.setWrapText(true);

        int score = (int) Math.round(p.getScore());
        Label sc = new Label("Score " + score);
        sc.getStyleClass().add("scoreBadge");

        Label stats = new Label("❤️ " + p.getLikesCount() + "   •   💬 " + p.getCommentsCount());
        stats.getStyleClass().add("meta");

        HBox row = new HBox(8, sc, stats);
        row.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(t, author, ex, row);
        return card;
    }

    // -------------------------
    // Profile sidebar (Mes posts)
    // -------------------------
    private void refreshProfileSidebar() {
        if (recommendedContainer == null) return;
        recommendedContainer.getChildren().clear();

        Label h = new Label("Profil");
        h.getStyleClass().add("sectionTitle");
        recommendedContainer.getChildren().add(h);

        // Name
        Label full = new Label(Session.fullname == null ? "" : Session.fullname);
        full.getStyleClass().add("profileName");
        recommendedContainer.getChildren().add(full);

        try {
            int uid = Session.idUsers;

            int postsCount = postService.countPostsByUser(uid);
            int commentsCount = commentService.countCommentsByUser(uid);

            Label stats = new Label("Posts: " + postsCount + "   •   Commentaires: " + commentsCount);
            stats.getStyleClass().add("meta");
            recommendedContainer.getChildren().add(stats);

            int points = pointsService.getPoints(uid);
            String rank = pointsService.getBadge(uid);

            String medal = medalForRank(rank);
            String rankText = (medal.isEmpty() ? "" : medal + " ") + rank;
            Label rankLbl = new Label("Rank: " + rankText);
            rankLbl.getStyleClass().add("rankLine");
            recommendedContainer.getChildren().add(rankLbl);

            int next = nextThreshold(points);
            Label ptsLbl = new Label("Points: " + points + " / " + next);
            ptsLbl.getStyleClass().add("meta");
            recommendedContainer.getChildren().add(ptsLbl);

            ProgressBar pb = new ProgressBar();
            pb.setMaxWidth(Double.MAX_VALUE);
            pb.getStyleClass().add("profileProgress");
            double progress = next <= 0 ? 1.0 : Math.min(1.0, points / (double) next);
            pb.setProgress(progress);

            Label pct = new Label((int)Math.round(progress * 100) + "%");
            pct.getStyleClass().add("meta");

            HBox progRow = new HBox(8, pb, pct);
            progRow.setAlignment(Pos.CENTER_LEFT);
            recommendedContainer.getChildren().add(progRow);

            Label scale = new Label("0–100 New   •   100–490 Bronze   •   500–1000 Silver   •   1000+ Gold (Top Contributor)");
            scale.getStyleClass().add("scaleHint");
            scale.setWrapText(true);
            recommendedContainer.getChildren().add(scale);

        } catch (SQLException e) {
            e.printStackTrace();
            Label err = new Label("Erreur chargement profil");
            err.getStyleClass().add("meta");
            recommendedContainer.getChildren().add(err);
        }
    }

    private String medalForRank(String rank) {
        if (rank == null) return "";
        String r = rank.trim().toLowerCase();
        // Accept both clean ranks and legacy text stored in DB
        if (r.contains("gold") || r.contains("top contributor")) return "🥇";
        if (r.contains("silver")) return "🥈";
        if (r.contains("bronze")) return "🥉";
        return "";
    }

    private Label buildRankBadge(String rank) {
        String r = rank == null ? "New" : rank.trim();
        String low = r.toLowerCase();

        String text;
        String variant;
        if (low.contains("gold")) {
            text = "Gold • Top Contributor";
            variant = "rankGold";
        } else if (low.contains("silver")) {
            text = "Silver";
            variant = "rankSilver";
        } else if (low.contains("bronze")) {
            text = "Bronze";
            variant = "rankBronze";
        } else {
            text = "New";
            variant = "rankNew";
        }

        Label b = new Label(text);
        b.getStyleClass().addAll("rankBadge", variant);
        return b;
    }

    private int nextThreshold(int points) {
        if (points < 100) return 100;
        if (points < 500) return 500;
        if (points < 1000) return 1000;
        return 1000;
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
        // Allow card to take the full available width of the column
        card.setMaxWidth(Double.MAX_VALUE);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("avatar");
        Label av = new Label("U");
        av.getStyleClass().add("avatarText");
        avatar.getChildren().add(av);

        VBox info = new VBox(2);
        Label name = new Label(p.getAuthorFullName());
        name.getStyleClass().add("name");

        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        nameRow.getChildren().add(name);

        // Rank badge pill (colored) next to the creator name
        try {
            int authorId = p.getIdUsers();
            String rank = badgeCache.computeIfAbsent(authorId, id -> {
                try {
                    return pointsService.getBadge(id);
                } catch (SQLException e) {
                    return "New";
                }
            });
            nameRow.getChildren().add(buildRankBadge(rank));
        } catch (Exception ignored) {
            nameRow.getChildren().add(buildRankBadge("New"));
        }

        Label created = new Label("Publié le " + formatDate(p.getCreatedAt()));
        created.getStyleClass().add("meta");
        info.getChildren().addAll(nameRow, created);

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

                        activityService.log(Session.idUsers, "DELETE_POST", "postId=" + p.getId());
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
        // Traduction (par post)
        ComboBox<String> langCombo = new ComboBox<>();
        langCombo.getItems().addAll("Anglais", "Français", "Deutsch", "Italien", "Espagnol", "Original");
        langCombo.setPrefWidth(170);
        langCombo.getStyleClass().addAll("combo", "langCombo", "translateCombo");
        langCombo.setPromptText("Traduction");

        // Afficher toujours "Traduction" sur le bouton (comme un menu)
        langCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText("Traduction");
            }
        });
        langCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }
        });

        String selected = getSelectedLang(p.getId()); // code ("fr","en","de","it","es") ou ""
        langCombo.getSelectionModel().select(mapCodeToDisplay(selected));

        // listener robuste (évite certains cas où setOnAction n'est pas déclenché)
        langCombo.valueProperty().addListener((obs, oldV, v) -> {
            String lang = mapLangDisplayToCode(v); // code ou ""
            postLang.put(p.getId(), lang);

            // traduire titre + contenu (uniquement à l'affichage)
            translateAsync("POST_TITLE", p.getId(), p.getTitle(), lang,
                    tr -> Platform.runLater(() -> title.setText(tr)));
            translateAsync("POST", p.getId(), p.getContent(), lang,
                    tr -> Platform.runLater(() -> content.setText(tr)));
        });

        // appliquer la langue actuelle dès l'affichage
        String currentLang = getSelectedLang(p.getId());
        if (currentLang != null && !currentLang.isBlank()) {
            translateAsync("POST_TITLE", p.getId(), p.getTitle(), currentLang,
                    tr -> Platform.runLater(() -> title.setText(tr)));
            translateAsync("POST", p.getId(), p.getContent(), currentLang,
                    tr -> Platform.runLater(() -> content.setText(tr)));
        }

        HBox translateBar = new HBox(8, langCombo);
        translateBar.setAlignment(Pos.CENTER_LEFT);
HBox actions = new HBox(10);

        Button likeBtn = new Button("♡ J'aime (" + p.getLikesCount() + ")");
        likeBtn.getStyleClass().add("actionBtn");
        likeBtn.setOnAction(e -> {
            try {
                boolean likedNow = likeService.togglePostLike(p.getId(), Session.idUsers);

                // Notifications + activity
                if (likedNow && p.getIdUsers() != Session.idUsers) {
                    notificationService.create(
                            p.getIdUsers(),
                            "LIKE_POST",
                            Session.fullname + " a aimé ton post : " + p.getTitle(),
                            "POST",
                            Math.toIntExact(p.getId())
                    );
                    refreshNotifBadge();
                }
                activityService.log(Session.idUsers, likedNow ? "LIKE_POST" : "UNLIKE_POST", "postId=" + p.getId());
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

        VBox addBox = new VBox(4);

        HBox addRow = new HBox(10);
        addRow.setAlignment(Pos.CENTER_LEFT);

        TextField tf = new TextField();
        tf.setPromptText("Ajouter un commentaire...");
        tf.getStyleClass().add("commentField");
        HBox.setHgrow(tf, Priority.ALWAYS);

        Button publish = new Button("Publier");
        publish.getStyleClass().add("primaryBtnSmall");
        Button mic = new Button("🎤");
        mic.getStyleClass().add("actionBtn");
        bindSpeech(mic, tf);


        Label lblCommentError = new Label();
        lblCommentError.getStyleClass().add("error-text");
        lblCommentError.setVisible(false);
        lblCommentError.setManaged(false);

        tf.textProperty().addListener((obs, o, n) -> clearFieldError(tf, lblCommentError));

        publish.setOnAction(ev -> {
            String txt = tf.getText();
            clearFieldError(tf, lblCommentError);

            if (isBlank(txt)) {
                showFieldError(tf, lblCommentError, "Le commentaire est obligatoire.");
                return;
            }
            if (txt.trim().length() < 2) {
                showFieldError(tf, lblCommentError, "Le commentaire doit contenir au moins 2 caractères.");
                return;
            }

            try {
                // Modération + anti-spam + rate limit
                moderationService.validateComment(Session.idUsers, txt.trim(), 3);

                commentService.addComment(p.getId(), Session.idUsers, txt.trim());

                // Scoring + activity
                try { pointsService.addPoints(Session.idUsers, 2); } catch (SQLException e2) { System.err.println("[POINTS] " + e2.getMessage()); }
                activityService.log(Session.idUsers, "CREATE_COMMENT", "postId=" + p.getId());

                // Notification au propriétaire du post
                if (p.getIdUsers() != Session.idUsers) {
                    notificationService.create(
                            p.getIdUsers(),
                            "COMMENT_POST",
                            Session.fullname + " a commenté ton post : " + p.getTitle(),
                            "POST",
                            Math.toIntExact(p.getId())
                    );
                    refreshNotifBadge();
                }
                tf.clear();
                loadCommentsInto(list, p.getId());
                refreshPosts();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
            }
        });

        addRow.getChildren().addAll(tf, mic, publish);
        addBox.getChildren().addAll(addRow, lblCommentError);

        commentsBox.getChildren().addAll(list, addBox);

        commentBtn.setOnAction(e -> {
            boolean show = !commentsBox.isVisible();
            commentsBox.setVisible(show);
            commentsBox.setManaged(show);
            if (show) loadCommentsInto(list, p.getId());
        });

        left.getChildren().addAll(content, translateBar, actions, commentsBox);
        body.getChildren().add(left);

        if (p.getFirstImagePath() != null && !p.getFirstImagePath().isBlank()) {
            File f = new File(p.getFirstImagePath());
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString(), 150, 105, false, true));
                iv.getStyleClass().add("postImage");
                body.getChildren().add(iv);
            }
        }

        // translateBar est déjà dans "left" (avec le contenu) -> éviter le doublon
        card.getChildren().addAll(header, title, body);
        return card;
    }

    private Node buildDialogHeader(String title) {
        StackPane header = new StackPane();
        header.getStyleClass().add("mcDialogHeader");
        header.setPadding(new Insets(18, 18, 16, 18));

        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);

        Label lbl = new Label(title);
        lbl.getStyleClass().add("mcDialogTitle");

        Region underline = new Region();
        underline.getStyleClass().add("mcDialogUnderline");
        underline.setPrefHeight(3);
        underline.setMaxWidth(90);

        box.getChildren().addAll(lbl, underline);
        header.getChildren().add(box);
        return header;
    }

private void openEditPostDialog(Post p) {
        if (p == null) return;
        if (!Permissions.canEdit(p.getIdUsers())) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Modifier le Post");
        UiFactory.styleDialog(dialog.getDialogPane());

        // Custom modern header (like "Gestion Utilisateur")
        dialog.getDialogPane().setHeader(null);
        dialog.getDialogPane().getStyleClass().add("mcDialogPane");

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField tfTitle = new TextField(p.getTitle());
        tfTitle.setPromptText("Titre");

        Label lblTitleError = new Label();
        lblTitleError.getStyleClass().add("error-text");
        lblTitleError.setVisible(false);
        lblTitleError.setManaged(false);

        TextArea taContent = new TextArea(p.getContent());
        taContent.setPromptText("Contenu");
        taContent.setPrefRowCount(6);

        Label lblContentError = new Label();
        lblContentError.getStyleClass().add("error-text");
        lblContentError.setVisible(false);
        lblContentError.setManaged(false);

        tfTitle.textProperty().addListener((obs, o, n) -> clearFieldError(tfTitle, lblTitleError));
        taContent.textProperty().addListener((obs, o, n) -> clearFieldError(taContent, lblContentError));

        // ---- Form layout (2 columns header style) ----
        VBox root = new VBox();
        root.getStyleClass().add("mcDialogRoot");

        Node header = buildDialogHeader("Modifier Post");
        VBox form = new VBox(14);
        form.getStyleClass().add("mcDialogForm");
        form.setPadding(new Insets(18, 22, 16, 22));

        Label l1 = new Label("Titre");
        l1.getStyleClass().add("mcFormLabel");
        Label l2 = new Label("Contenu");
        l2.getStyleClass().add("mcFormLabel");

        taContent.getStyleClass().add("mcTextArea");
        tfTitle.getStyleClass().add("mcTextField");

        form.getChildren().addAll(
                l1, tfTitle, lblTitleError,
                l2, taContent, lblContentError
        );

        root.getChildren().addAll(header, form);
        dialog.getDialogPane().setContent(root);

        Button btnSave = (Button) dialog.getDialogPane().lookupButton(saveType);
        btnSave.getStyleClass().add("primaryBtn");
        btnSave.addEventFilter(ActionEvent.ACTION, ev -> {
            boolean ok = true;

            clearFieldError(tfTitle, lblTitleError);
            clearFieldError(taContent, lblContentError);

            String title = tfTitle.getText();
            String content = taContent.getText();

            if (isBlank(title)) {
                showFieldError(tfTitle, lblTitleError, "Le titre est obligatoire.");
                ok = false;
            } else if (title.trim().length() < 3) {
                showFieldError(tfTitle, lblTitleError, "Le titre doit contenir au moins 3 caractères.");
                ok = false;
            }

            if (isBlank(content)) {
                showFieldError(taContent, lblContentError, "Le contenu est obligatoire.");
                ok = false;
            } else if (content.trim().length() < 10) {
                showFieldError(taContent, lblContentError, "Le contenu doit contenir au moins 10 caractères.");
                ok = false;
            }

            if (!ok) ev.consume();
        });

        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnCancel.getStyleClass().add("secondaryBtn");

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                try {
                    postService.updatePost(p.getId(), tfTitle.getText().trim(), taContent.getText().trim());
                    refreshPosts();
                    PopupUtil.showSuccess("Succès", "Post modifié ✅");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    PopupUtil.showError("Erreur", ex.getMessage());
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
                Label who = new Label(c.getAuthorFullName());
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

                String lang = getSelectedLang(postId);
                if (lang != null && !lang.isBlank()) {
                    translateAsync("COMMENT", c.getId(), c.getContent(), lang, tr -> Platform.runLater(() -> msg.setText(tr)));
                }


                textBox.getChildren().addAll(who, cCreated);
                if (cUpdated != null) textBox.getChildren().add(cUpdated);
                textBox.getChildren().add(msg);

                Button like = new Button("👍 " + c.getLikesCount());
                like.getStyleClass().add("actionBtn");
                like.setOnAction(e -> {
                    try {
                        boolean likedNow = likeService.toggleCommentLike(c.getId(), Session.idUsers);
                        activityService.log(Session.idUsers, likedNow ? "LIKE_COMMENT" : "UNLIKE_COMMENT", "commentId=" + c.getId());
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
                                activityService.log(Session.idUsers, "DELETE_COMMENT", "commentId=" + c.getId());
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

        dialog.getDialogPane().setHeader(null);
        dialog.getDialogPane().getStyleClass().add("mcDialogPane");

        ButtonType addType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        TextField tfTitle = new TextField();
        tfTitle.setPromptText("Titre");

        Label lblTitleError = new Label();
        lblTitleError.getStyleClass().add("error-text");
        lblTitleError.setVisible(false);
        lblTitleError.setManaged(false);

        TextArea taContent = new TextArea();
        taContent.setPromptText("Contenu");
        taContent.setPrefRowCount(6);

        // STT
        Button micTitle = new Button("🎤");
        micTitle.getStyleClass().addAll("actionBtn", "micBtn");
        bindSpeech(micTitle, tfTitle);

        Button micContent = new Button("🎤");
        micContent.getStyleClass().addAll("actionBtn", "micBtn");
        bindSpeech(micContent, taContent);

        HBox titleRow = new HBox(10, tfTitle, micTitle);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tfTitle, Priority.ALWAYS);

        HBox contentTools = new HBox(10, micContent);
        contentTools.setAlignment(Pos.CENTER_LEFT);

        Label lblContentError = new Label();
        lblContentError.getStyleClass().add("error-text");
        lblContentError.setVisible(false);
        lblContentError.setManaged(false);

        tfTitle.textProperty().addListener((obs, o, n) -> clearFieldError(tfTitle, lblTitleError));
        taContent.textProperty().addListener((obs, o, n) -> clearFieldError(taContent, lblContentError));

        Label imgLabel = new Label("Aucune image");
        imgLabel.getStyleClass().add("mcHint");
        Button chooseImg = new Button("Choisir une image...");
        chooseImg.getStyleClass().add("secondaryBtn");

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

        VBox root = new VBox();
        root.getStyleClass().add("mcDialogRoot");

        Node header = buildDialogHeader("Ajouter Post");

        VBox form = new VBox(14);
        form.getStyleClass().add("mcDialogForm");
        form.setPadding(new Insets(18, 22, 16, 22));

        Label l1 = new Label("Titre");
        l1.getStyleClass().add("mcFormLabel");
        Label l2 = new Label("Contenu");
        l2.getStyleClass().add("mcFormLabel");

        taContent.getStyleClass().add("mcTextArea");
        tfTitle.getStyleClass().add("mcTextField");

        HBox imgRow = new HBox(10, chooseImg, imgLabel);
        imgRow.setAlignment(Pos.CENTER_LEFT);

        form.getChildren().addAll(
                l1, titleRow, lblTitleError,
                l2, taContent, contentTools, lblContentError,
                imgRow
        );

        root.getChildren().addAll(header, form);
        dialog.getDialogPane().setContent(root);

        Button btnAdd = (Button) dialog.getDialogPane().lookupButton(addType);
        btnAdd.getStyleClass().add("primaryBtn");

        btnAdd.addEventFilter(ActionEvent.ACTION, ev -> {
            boolean ok = true;

            clearFieldError(tfTitle, lblTitleError);
            clearFieldError(taContent, lblContentError);

            String title = tfTitle.getText();
            String content = taContent.getText();

            if (isBlank(title)) {
                showFieldError(tfTitle, lblTitleError, "Le titre est obligatoire.");
                ok = false;
            } else if (title.trim().length() < 3) {
                showFieldError(tfTitle, lblTitleError, "Le titre doit contenir au moins 3 caractères.");
                ok = false;
            }

            if (isBlank(content)) {
                showFieldError(taContent, lblContentError, "Le contenu est obligatoire.");
                ok = false;
            } else if (content.trim().length() < 10) {
                showFieldError(taContent, lblContentError, "Le contenu doit contenir au moins 10 caractères.");
                ok = false;
            }

            if (!ok) ev.consume();
        });

        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        btnCancel.getStyleClass().add("secondaryBtn");

        dialog.setResultConverter(bt -> {
            if (bt == addType) {
                try {
                    String storedPath = null;
                    if (chosen[0] != null) {
                        storedPath = ImageStorage.copyToUploads(chosen[0]);
                    }

                    Post p = new Post();
                    p.setIdUsers(Session.idUsers);
                    p.setTitle(tfTitle.getText().trim());
                    p.setContent(taContent.getText().trim());
                    p.setStatus("PUBLISHED");
                    p.setLanguage("fr");

                    // Modération
                    moderationService.validatePost(Session.idUsers, p.getTitle(), p.getContent());

                    long newId = postService.createPostWithOptionalImage(p, storedPath);

                    // Scoring + activité
                    try { pointsService.addPoints(Session.idUsers, 10); } catch (SQLException e2) { System.err.println("[POINTS] " + e2.getMessage()); }
                    activityService.log(Session.idUsers, "CREATE_POST", "postId=" + newId);

                    refreshPosts();
                    PopupUtil.showSuccess("Succès", "Post ajouté ✅");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    PopupUtil.showError("Erreur", ex.getMessage());
                } catch (IOException ex) {
                    ex.printStackTrace();
                    PopupUtil.showError("Erreur", "Impossible de copier l'image: " + ex.getMessage());
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
                activityService.log(Session.idUsers, "REPORT_POST", "postId=" + postId + ", reason=" + reason);
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
                activityService.log(Session.idUsers, "REPORT_COMMENT", "commentId=" + commentId + ", reason=" + reason);
                showAlert(Alert.AlertType.INFORMATION, "OK", "Signalement envoyé.");
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
            }
        });
    }

    // -------------------------
    // Notifications UI
    // -------------------------

    private void refreshNotifBadge() {
        if (notifBadge == null) return;
        try {
            int unread = notificationService.countUnread(Session.idUsers);
            notifBadge.setText(String.valueOf(unread));
            boolean show = unread > 0;
            notifBadge.setVisible(show);
            notifBadge.setManaged(show);
        } catch (SQLException e) {
            // Ne pas bloquer l'UI
            notifBadge.setVisible(false);
            notifBadge.setManaged(false);
        }
    }

    
    @FXML
    private void onNotifClick(ActionEvent event) {
        if (notifButton == null) return;

        ContextMenu menu = new ContextMenu();
        try {
            var items = notificationService.listUnread(Session.idUsers, 15);
            if (items.isEmpty()) {
                MenuItem empty = new MenuItem("Aucune nouvelle notification");
                empty.setDisable(true);
                menu.getItems().add(empty);
            } else {
                for (var n : items) {
                    MenuItem mi = new MenuItem(n.message);
                    mi.setOnAction(e -> {
                        try {
                            notificationService.markRead(n.id);
                            refreshNotifBadge();
                            // Option: ouvrir le post si entity_type=POST
                            if ("POST".equalsIgnoreCase(n.entityType) && n.entityId != null) {
                                Post p = postService.getPostById(n.entityId.intValue());
                                if (p != null) openViewPostDialog(p);
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });
                    menu.getItems().add(mi);
                }

                menu.getItems().add(new SeparatorMenuItem());
                MenuItem markAll = new MenuItem("Tout marquer comme lu");
                markAll.setOnAction(e -> {
                    try {
                        notificationService.markAllRead(Session.idUsers);
                        refreshNotifBadge();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });
                menu.getItems().add(markAll);
            }
        } catch (SQLException e) {
            MenuItem err = new MenuItem("Erreur chargement notifications");
            err.setDisable(true);
            menu.getItems().add(err);
        }

        menu.show(notifButton, javafx.geometry.Side.BOTTOM, 0, 6);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        PopupUtil.Kind k = switch (type) {
            case ERROR -> PopupUtil.Kind.ERROR;
            case CONFIRMATION -> PopupUtil.Kind.CONFIRM;
            default -> PopupUtil.Kind.INFO;
        };
        PopupUtil.show(k, title, message);
    }

    // -------------------------
    // -------------------------
    // Admin - Dashboard (tab)
    // -------------------------

    private void refreshDashboard() {
        if (postsContainer == null) return;
        postsContainer.getChildren().clear();
        postsContainer.getChildren().add(buildAdminDashboard());
    }

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

    private Node buildAdminDashboard() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dashboard");

        Label title = new Label("📊 Dashboard Admin");
        title.getStyleClass().add("sectionTitle");

        HBox cards = new HBox(12);
        cards.setAlignment(Pos.CENTER_LEFT);

        VBox postsCard = new VBox(6);
        postsCard.getStyleClass().add("statCard");
        Label postsLbl = new Label("Posts / jour");
        postsLbl.getStyleClass().add("statTitle");
        Label postsVal = new Label("-");
        postsVal.getStyleClass().add("statValue");
        postsCard.getChildren().addAll(postsLbl, postsVal);

        VBox reportsCard = new VBox(6);
        reportsCard.getStyleClass().add("statCard");
        Label reportsLbl = new Label("Signalements");
        reportsLbl.getStyleClass().add("statTitle");
        Label reportsVal = new Label("-");
        reportsVal.getStyleClass().add("statValue");
        reportsCard.getChildren().addAll(reportsLbl, reportsVal);

        VBox topCard = new VBox(6);
        topCard.getStyleClass().add("statCard");
        Label topLbl = new Label("Top utilisateurs");
        topLbl.getStyleClass().add("statTitle");
        VBox topList = new VBox(4);
        topCard.getChildren().addAll(topLbl, topList);

        cards.getChildren().addAll(postsCard, reportsCard, topCard);

        VBox perDayBox = new VBox(8);
        Label perDayTitle = new Label("Posts sur les 7 derniers jours");
        perDayTitle.getStyleClass().add("subTitle");
        VBox perDayList = new VBox(6);
        perDayBox.getChildren().addAll(perDayTitle, perDayList);

        VBox activityBox = new VBox(8);
        Label activityTitle = new Label("Historique d'activité");
        activityTitle.getStyleClass().add("subTitle");
        VBox activityList = new VBox(6);
        ScrollPane sp = new ScrollPane(activityList);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(240);
        sp.getStyleClass().add("activityScroll");
        activityBox.getChildren().addAll(activityTitle, sp);

        try {
            postsVal.setText(String.valueOf(adminDashboardService.countPostsToday()));
            reportsVal.setText(String.valueOf(adminDashboardService.countReportsTotal()));

            topList.getChildren().clear();
            for (var u : adminDashboardService.topUsersByPoints(5)) {
                Label li = new Label("• " + u.fullname + "  (" + u.points + ")");
                li.getStyleClass().add("meta");
                topList.getChildren().add(li);
            }

            perDayList.getChildren().clear();
            for (var dc : adminDashboardService.postsPerDay(7)) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Label d = new Label(dc.day.toString());
                d.getStyleClass().add("meta");
                Label c = new Label(String.valueOf(dc.count));
                c.getStyleClass().add("pill");
                row.getChildren().addAll(d, c);
                perDayList.getChildren().add(row);
            }

            activityList.getChildren().clear();
            for (var a : activityService.latest(20)) {
                String when = a.createdAt == null ? "" : DT.format(a.createdAt);
                Label line = new Label("• " + when + "  —  " + a.fullname + "  —  " + a.actionType);
                line.getStyleClass().add("meta");
                line.setWrapText(true);
                activityList.getChildren().add(line);
            }

        } catch (SQLException e) {
            Label err = new Label("Dashboard indisponible: " + e.getMessage());
            err.getStyleClass().add("meta");
            root.getChildren().addAll(title, err);
            return root;
        }

        root.getChildren().addAll(title, cards, perDayBox, activityBox);
        return root;
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

        Button view = new Button("Voir Post");
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
        dialog.setTitle("Voir Post");
        UiFactory.styleDialog(dialog.getDialogPane());

        ButtonType closeType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);

        // Header (MindCare gradient)
        Label headerTitle = new Label("Voir Post");
        headerTitle.getStyleClass().add("dialogHeaderTitle");
        StackPane header = new StackPane(headerTitle);
        header.getStyleClass().add("dialogHeader");
        header.setMinHeight(56);
        dialog.getDialogPane().setHeader(header);

        Label t = new Label(p.getTitle());
        t.getStyleClass().addAll("postTitle", "viewPostTitle");

        String metaText = p.getAuthorFullName() + " • Publié le " + formatDate(p.getCreatedAt());
        if (p.getUpdatedAt() != null) {
            metaText += " • Modifié le " + formatDate(p.getUpdatedAt());
        }
        Label meta = new Label(metaText);
        meta.getStyleClass().add("meta");

        Label c = new Label(p.getContent());
        c.setWrapText(true);
        c.getStyleClass().addAll("postText", "viewPostText");

        Button mail = new Button(" Envoyer mail");
        mail.getStyleClass().add("mailBtn");
        mail.setOnAction(e -> openSendMailDialog(p.getIdUsers()));

        VBox content = new VBox(10, t, meta, c, mail);
        content.getStyleClass().add("viewPostCard");

        if (p.getFirstImagePath() != null && !p.getFirstImagePath().isBlank()) {
            File f = new File(p.getFirstImagePath());
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString(), 520, 300, true, true));
                iv.getStyleClass().add("postImage");
                content.getChildren().add(iv);
            }
        }

        VBox root = new VBox(content);
        root.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(root);

        Button closeBtn = (Button) dialog.getDialogPane().lookupButton(closeType);
        if (closeBtn != null) closeBtn.getStyleClass().add("secondaryBtn");

        dialog.showAndWait();
    }


    private void openSendMailDialog(int idUsers) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Envoyer un email à l'utilisateur");
        UiFactory.styleDialog(dlg.getDialogPane());

        ButtonType sendType = new ButtonType("Envoyer", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(sendType, ButtonType.CANCEL);

        TextField subject = new TextField("A propos de votre publication");
        TextArea body = new TextArea();
        body.setPromptText("Tape ton message...");
        body.setPrefRowCount(8);

        Label info = new Label("Le message sera envoyé à l'email enregistré dans la table users.");
        info.getStyleClass().add("meta");

        VBox box = new VBox(8, info, new Label("Sujet"), subject, new Label("Message"), body);
        box.setPadding(new Insets(10));
        dlg.getDialogPane().setContent(box);

        Button sendBtn = (Button) dlg.getDialogPane().lookupButton(sendType);
        sendBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            if (subject.getText() == null || subject.getText().trim().length() < 3) {
                showAlert(Alert.AlertType.INFORMATION, "Info", "Sujet trop court.");
                ev.consume();
                return;
            }
            if (body.getText() == null || body.getText().trim().length() < 5) {
                showAlert(Alert.AlertType.INFORMATION, "Info", "Message trop court.");
                ev.consume();
                return;
            }

            sendBtn.setDisable(true);
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    adminReportService.sendMailToUserId(idUsers, subject.getText().trim(), body.getText().trim());
                    return null;
                }
            };
            task.setOnSucceeded(e2 -> {
                dlg.close();
                PopupUtil.showSuccess("Mail envoyé ✅");
            });
            task.setOnFailed(e2 -> {
                sendBtn.setDisable(false);
                Throwable ex = task.getException();
                showAlert(Alert.AlertType.ERROR, "Email", ex == null ? "Erreur" : ex.getMessage());
            });
            Thread th = new Thread(task, "mailjet-send-mail");
            th.setDaemon(true);
            th.start();

            ev.consume();
        });

        dlg.showAndWait();
    }

    private void applyAdminTabVisibility() {
        boolean show = Permissions.isAdmin();
        System.out.println("[FORUM] admin? " + show + " | sessionRole=" + Session.role + " | id=" + Session.idUsers);

        if (tabReports != null) {
            tabReports.setVisible(show);
            tabReports.setManaged(show);
        }
        if (tabDashboard != null) {
            tabDashboard.setVisible(show);
            tabDashboard.setManaged(show);
        }

        if (!show) {
            if (tabReports != null && tabReports.isSelected() && tabAllPosts != null) tabAllPosts.setSelected(true);
            if (tabDashboard != null && tabDashboard.isSelected() && tabAllPosts != null) tabAllPosts.setSelected(true);
        }
    }
}