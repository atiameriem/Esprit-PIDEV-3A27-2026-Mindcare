package services;

import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Scoring utilisateur.
 * +10 points par post
 * +2 points par commentaire
 */
public class PointsService {

    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    public void addPoints(int idUsers, int delta) throws SQLException {
        Connection cnx = cnx();

        // IMPORTANT:
        // - Always keep column 'badge' in sync with points.
        // - Use SQL to compute the badge after adding delta (works for insert + update).
        String sql = "INSERT INTO user_points (id_user, points, badge) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "points = points + VALUES(points), " +
                "badge = CASE " +
                "  WHEN (points + VALUES(points)) >= 1000 THEN 'Gold' " +
                "  WHEN (points + VALUES(points)) >= 500  THEN 'Silver' " +
                "  WHEN (points + VALUES(points)) >= 100  THEN 'Bronze' ELSE 'New' " +
                "END";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUsers);
            ps.setInt(2, delta);
            // For a new row, badge is computed from delta (starting points)
            ps.setString(3, badgeFromPoints(delta));
            ps.executeUpdate();
        }
    }

    public int getPoints(int idUsers) throws SQLException {
        Connection cnx = cnx();
        String sql = "SELECT points FROM user_points WHERE id_user=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUsers);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public String getBadge(int idUsers) throws SQLException {
        int p = getPoints(idUsers);
        return badgeFromPoints(p);
    }

    private String badgeFromPoints(int points) {
        if (points >= 1000) return "Gold";
        if (points >= 500) return "Silver";
        if (points >= 100) return "Bronze";
        return "New";
    }
}
