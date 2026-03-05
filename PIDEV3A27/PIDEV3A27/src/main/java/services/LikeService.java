package services;

import utils.MyDatabase;

import java.sql.*;

public class LikeService {
    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    public boolean hasPostLike(long postId, int idUsers) throws SQLException {
        Connection cnx = cnx();
        String sql = "SELECT 1 FROM post_likes WHERE id_post=? AND id_users=? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setInt(2, idUsers);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * @return true si le post est maintenant liké, false si unliké
     */
    public boolean togglePostLike(long postId, int idUsers) throws SQLException {
        Connection cnx = cnx();
        if (hasPostLike(postId, idUsers)) {
            String del = "DELETE FROM post_likes WHERE id_post=? AND id_users=?";
            try (PreparedStatement ps = cnx.prepareStatement(del)) {
                ps.setLong(1, postId);
                ps.setInt(2, idUsers);
                ps.executeUpdate();
            }
            return false;
        } else {
            String ins = "INSERT INTO post_likes (id_post, id_users) VALUES (?,?)";
            try (PreparedStatement ps = cnx.prepareStatement(ins)) {
                ps.setLong(1, postId);
                ps.setInt(2, idUsers);
                ps.executeUpdate();
            }
            return true;
        }
    }

    public boolean hasCommentLike(long commentId, int idUsers) throws SQLException {
        Connection cnx = cnx();
        String sql = "SELECT 1 FROM comment_likes WHERE id_comment=? AND id_users=? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            ps.setInt(2, idUsers);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean toggleCommentLike(long commentId, int idUsers) throws SQLException {
        Connection cnx = cnx();
        if (hasCommentLike(commentId, idUsers)) {
            String del = "DELETE FROM comment_likes WHERE id_comment=? AND id_users=?";
            try (PreparedStatement ps = cnx.prepareStatement(del)) {
                ps.setLong(1, commentId);
                ps.setInt(2, idUsers);
                ps.executeUpdate();
            }
            return false;
        } else {
            String ins = "INSERT INTO comment_likes (id_comment, id_users) VALUES (?,?)";
            try (PreparedStatement ps = cnx.prepareStatement(ins)) {
                ps.setLong(1, commentId);
                ps.setInt(2, idUsers);
                ps.executeUpdate();
            }
            return true;
        }
    }
}
