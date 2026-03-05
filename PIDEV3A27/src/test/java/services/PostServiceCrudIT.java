package services;

import models.Post;
import org.junit.jupiter.api.*;
import utils.MyDatabase;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRUD tests "style workshop" (DB réelle).
 *
 * IMPORTANT:
 * - Ces tests utilisent la DB MySQL configurée dans utils.MyDatabase.
 * - Ils créent des données temporaires puis les nettoient en fin de test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostServiceCrudIT {

    private static final PostService postService = new PostService();
    private static long createdPostId;
    private static int createdUserId;

    private static Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        // create a dedicated user for tests
        String sql = "INSERT INTO users (nom, prenom, email, image, mot_de_passe, role, date_inscription) " +
                "VALUES (?,?,?,?,?,?,CURDATE())";
        try (PreparedStatement ps = cnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "JUnit");
            ps.setString(2, "Tester");
            ps.setString(3, "junit_post_" + System.currentTimeMillis() + "@test.com");
            ps.setString(4, "default.png");
            ps.setString(5, "1234");
            ps.setString(6, "patient");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                assertTrue(rs.next(), "Impossible de récupérer l'id user");
                createdUserId = rs.getInt(1);
            }
        }
    }

    @AfterAll
    static void afterAll() {
        // hard cleanup to keep DB clean
        try {
            if (createdPostId > 0) {
                try (PreparedStatement ps = cnx().prepareStatement("DELETE FROM post_images WHERE id_post=?")) {
                    ps.setLong(1, createdPostId);
                    ps.executeUpdate();
                }
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
    void create_post() throws Exception {
        Post p = new Post();
        p.setIdUsers(createdUserId);
        p.setTitle("Post CRUD JUnit");
        p.setContent("Contenu initial");
        p.setStatus("PUBLISHED");
        p.setLanguage("fr");

        createdPostId = postService.createPost(p);
        assertTrue(createdPostId > 0);

        Post loaded = postService.getPostById(createdPostId);
        assertNotNull(loaded);
        assertEquals("Post CRUD JUnit", loaded.getTitle());
        assertEquals("Contenu initial", loaded.getContent());
        assertEquals(createdUserId, loaded.getIdUsers());
    }

    @Test
    @Order(2)
    void read_post() throws Exception {
        Post loaded = postService.getPostById(createdPostId);
        assertNotNull(loaded);
        assertEquals(createdPostId, loaded.getId());
    }

    @Test
    @Order(3)
    void update_post() throws Exception {
        postService.updatePost(createdPostId, "Titre modifié", "Contenu modifié");
        Post loaded = postService.getPostById(createdPostId);
        assertNotNull(loaded);
        assertEquals("Titre modifié", loaded.getTitle());
        assertEquals("Contenu modifié", loaded.getContent());
    }

    @Test
    @Order(4)
    void delete_post_soft() throws Exception {
        postService.deletePost(createdPostId);

        // vérifier la colonne status
        try (PreparedStatement ps = cnx().prepareStatement("SELECT status FROM post WHERE id=?")) {
            ps.setLong(1, createdPostId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("DELETED", rs.getString(1));
            }
        }
    }
}
