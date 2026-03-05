package services;

import models.Post;
import org.junit.jupiter.api.*;
import utils.MyDatabase;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRUD tests "style workshop" (DB réelle) pour les commentaires.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommentServiceCrudIT {

    private static final CommentService commentService = new CommentService();
    private static final PostService postService = new PostService();

    private static int createdUserId;
    private static long createdPostId;
    private static long createdCommentId;

    private static Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        // user
        String sqlUser = "INSERT INTO users (nom, prenom, email, image, mot_de_passe, role, date_inscription) " +
                "VALUES (?,?,?,?,?,?,CURDATE())";
        try (PreparedStatement ps = cnx().prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "JUnit");
            ps.setString(2, "Commenter");
            ps.setString(3, "junit_comment_" + System.currentTimeMillis() + "@test.com");
            ps.setString(4, "default.png");
            ps.setString(5, "1234");
            ps.setString(6, "patient");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                assertTrue(rs.next());
                createdUserId = rs.getInt(1);
            }
        }

        // post
        Post p = new Post();
        p.setIdUsers(createdUserId);
        p.setTitle("Post pour commentaires");
        p.setContent("Contenu");
        p.setStatus("PUBLISHED");
        p.setLanguage("fr");
        createdPostId = postService.createPost(p);
        assertTrue(createdPostId > 0);
    }

    @AfterAll
    static void afterAll() {
        try {
            if (createdPostId > 0) {
                try (PreparedStatement ps = cnx().prepareStatement("DELETE FROM comments WHERE id_post=?")) {
                    ps.setLong(1, createdPostId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = cnx().prepareStatement("DELETE FROM post WHERE id=?")) {
                    ps.setLong(1, createdPostId);
                    ps.executeUpdate();
                }
            }

            if (createdUserId > 0) {
                try (PreparedStatement ps = cnx().prepareStatement("DELETE FROM users WHERE id_users=?")) {
                    ps.setInt(1, createdUserId);
                    ps.executeUpdate();
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @Order(1)
    void create_comment() throws Exception {
        commentService.addComment(createdPostId, createdUserId, "Commentaire initial");

        // récupérer l'id du commentaire créé
        String sql = "SELECT id FROM comments WHERE id_post=? AND id_users=? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, createdPostId);
            ps.setInt(2, createdUserId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Commentaire non inséré");
                createdCommentId = rs.getLong(1);
            }
        }
        assertTrue(createdCommentId > 0);
    }

    @Test
    @Order(2)
    void read_comments_by_post() throws Exception {
        var list = commentService.getCommentsByPost(createdPostId);
        assertNotNull(list);
        assertTrue(list.stream().anyMatch(c -> c.getId() == createdCommentId));
    }

    @Test
    @Order(3)
    void update_comment() throws Exception {
        commentService.updateComment(createdCommentId, "Commentaire modifié");
        String sql = "SELECT content FROM comments WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, createdCommentId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("Commentaire modifié", rs.getString(1));
            }
        }
    }

    @Test
    @Order(4)
    void delete_comment_soft() throws Exception {
        commentService.deleteComment(createdCommentId);
        String sql = "SELECT status FROM comments WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, createdCommentId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("DELETED", rs.getString(1));
            }
        }
    }
}
