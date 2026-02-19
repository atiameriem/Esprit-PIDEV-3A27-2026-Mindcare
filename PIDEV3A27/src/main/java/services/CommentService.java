package services;

import models.Commentaire;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentService {
    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    public List<Commentaire> getCommentsByPost(long postId) throws SQLException {
        Connection cnx = cnx();
        String sql = "SELECT c.id, c.id_post, c.id_users, c.content, c.created_at, c.updated_at, " +
                "(SELECT COUNT(*) FROM comment_likes cl WHERE cl.id_comment=c.id) AS likesCount " +
                "FROM comments c WHERE c.id_post=? AND c.status='PUBLISHED' ORDER BY c.created_at ASC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Commentaire> out = new ArrayList<>();
                while (rs.next()) {
                    Commentaire c = new Commentaire();
                    c.setId(rs.getLong("id"));
                    c.setIdPost(rs.getLong("id_post"));
                    c.setIdUsers(rs.getInt("id_users"));
                    c.setContent(rs.getString("content"));
                    Timestamp created = rs.getTimestamp("created_at");
                    Timestamp updated = rs.getTimestamp("updated_at");
                    if (created != null) c.setCreatedAt(created.toLocalDateTime());
                    if (updated != null) c.setUpdatedAt(updated.toLocalDateTime());
                    c.setLikesCount(rs.getInt("likesCount"));
                    out.add(c);
                }
                return out;
            }
        }
    }

    public void updateComment(long commentId, String content) throws SQLException {
        Connection cnx = cnx();
        String sql = "UPDATE comments SET content=?, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, content);
            ps.setLong(2, commentId);
            ps.executeUpdate();
        }
    }

    public void addComment(long postId, int idUsers, String content) throws SQLException {
        Connection cnx = cnx();
        String sql = "INSERT INTO comments (id_post, id_users, content, status) VALUES (?,?,?, 'PUBLISHED')";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setInt(2, idUsers);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    public void deleteComment(long commentId) throws SQLException {
        Connection cnx = cnx();
        String sql = "UPDATE comments SET status='DELETED' WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            ps.executeUpdate();
        }
    }
}
