package services;

import models.Post;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostService {
    // IMPORTANT: ne pas garder une connexion en champ, car elle peut être fermée puis recréée.
    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    public long createPost(Post p) throws SQLException {
        Connection cnx = cnx();
        String sql = "INSERT INTO post (id_users, title, content, status, language) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getIdUsers());
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getContent());
            ps.setString(4, p.getStatus() == null ? "PUBLISHED" : p.getStatus());
            ps.setString(5, p.getLanguage() == null ? "fr" : p.getLanguage());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Impossible de récupérer l'id du post");
                return rs.getLong(1);
            }
        }
    }

    public void attachImage(long postId, String path) throws SQLException {
        if (path == null || path.isBlank()) return;
        Connection cnx = cnx();
        String sql = "INSERT INTO post_images (id_post, path, sort_order) VALUES (?,?,0)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setString(2, path);
            ps.executeUpdate();
        }
    }

    public long createPostWithOptionalImage(Post p, String imagePathOrNull) throws SQLException {
        Connection cnx = cnx();
        cnx.setAutoCommit(false);
        try {
            long id = createPost(p);
            attachImage(id, imagePathOrNull);
            cnx.commit();
            return id;
        } catch (SQLException ex) {
            cnx.rollback();
            throw ex;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    public List<Post> findPosts(boolean onlyMine, int idUsers, String search, String sort) throws SQLException {
        Connection cnx = cnx();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT p.id, p.id_users, p.title, p.content, p.created_at, p.updated_at, ")
                .append("(SELECT pi.path FROM post_images pi WHERE pi.id_post=p.id ORDER BY pi.sort_order, pi.id LIMIT 1) AS img, ")
                .append("(SELECT COUNT(*) FROM post_likes pl WHERE pl.id_post=p.id) AS likesCount, ")
                .append("(SELECT COUNT(*) FROM comments c WHERE c.id_post=p.id AND c.status='PUBLISHED') AS commentsCount ")
                .append("FROM post p ")
                .append("WHERE p.status='PUBLISHED' ");

        List<Object> params = new ArrayList<>();
        if (onlyMine) {
            sb.append("AND p.id_users=? ");
            params.add(idUsers);
        }

        if (search != null && !search.isBlank()) {
            sb.append("AND (p.title LIKE ? OR p.content LIKE ?) ");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }

        // tri
        if ("Plus anciens".equals(sort)) sb.append("ORDER BY p.created_at ASC ");
        else if ("Plus aimés".equals(sort)) sb.append("ORDER BY likesCount DESC, p.created_at DESC ");
        else if ("Plus commentés".equals(sort)) sb.append("ORDER BY commentsCount DESC, p.created_at DESC ");
        else sb.append("ORDER BY p.created_at DESC ");

        try (PreparedStatement ps = cnx.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Post> out = new ArrayList<>();
                while (rs.next()) {
                    Post p = new Post();
                    p.setId(rs.getLong("id"));
                    p.setIdUsers(rs.getInt("id_users"));
                    p.setTitle(rs.getString("title"));
                    p.setContent(rs.getString("content"));
                    Timestamp created = rs.getTimestamp("created_at");
                    Timestamp updated = rs.getTimestamp("updated_at");
                    if (created != null) p.setCreatedAt(created.toLocalDateTime());
                    if (updated != null) p.setUpdatedAt(updated.toLocalDateTime());
                    p.setFirstImagePath(rs.getString("img"));
                    p.setLikesCount(rs.getInt("likesCount"));
                    p.setCommentsCount(rs.getInt("commentsCount"));
                    out.add(p);
                }
                return out;
            }
        }
    }

    public void updatePost(long postId, String title, String content) throws SQLException {
        Connection cnx = cnx();
        String sql = "UPDATE post SET title=?, content=?, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setLong(3, postId);
            ps.executeUpdate();
        }
    }

    public void deletePost(long postId) throws SQLException {
        Connection cnx = cnx();
        String sql = "UPDATE post SET status='DELETED' WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.executeUpdate();
        }
    }

    public Post getPostById(long postId) throws SQLException {
        Connection cnx = cnx();
        String sql =
                "SELECT p.id, p.id_users, p.title, p.content, p.created_at, p.updated_at, " +
                        "(SELECT pi.path FROM post_images pi WHERE pi.id_post=p.id ORDER BY pi.sort_order, pi.id LIMIT 1) AS img, " +
                        "(SELECT COUNT(*) FROM post_likes pl WHERE pl.id_post=p.id) AS likesCount, " +
                        "(SELECT COUNT(*) FROM comments c WHERE c.id_post=p.id AND c.status='PUBLISHED') AS commentsCount " +
                        "FROM post p WHERE p.id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Post p = new Post();
                p.setId(rs.getLong("id"));
                p.setIdUsers(rs.getInt("id_users"));
                p.setTitle(rs.getString("title"));
                p.setContent(rs.getString("content"));
                Timestamp created = rs.getTimestamp("created_at");
                Timestamp updated = rs.getTimestamp("updated_at");
                if (created != null) p.setCreatedAt(created.toLocalDateTime());
                if (updated != null) p.setUpdatedAt(updated.toLocalDateTime());
                p.setFirstImagePath(rs.getString("img"));
                p.setLikesCount(rs.getInt("likesCount"));
                p.setCommentsCount(rs.getInt("commentsCount"));
                return p;
            }
        }
    }
}
