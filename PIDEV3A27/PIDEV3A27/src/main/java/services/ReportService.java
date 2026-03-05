package services;

import utils.MyDatabase;

import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportService {
    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    public static class ReportAgg {
        public long postId;
        public int total;
        public Timestamp lastAt;
        public List<String> reasons = new ArrayList<>();
    }

    public void reportPost(long postId, int idUsers, String reason, String details) throws SQLException {
        Connection cnx = cnx();
        String sql = "INSERT INTO reports (target_type, target_id, id_users, reason, details) VALUES ('POST',?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setInt(2, idUsers);
            ps.setString(3, reason);
            ps.setString(4, details);
            ps.executeUpdate();
        }
    }

    public void reportComment(long commentId, int idUsers, String reason, String details) throws SQLException {
        Connection cnx = cnx();
        String sql = "INSERT INTO reports (target_type, target_id, id_users, reason, details) VALUES ('COMMENT',?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            ps.setInt(2, idUsers);
            ps.setString(3, reason);
            ps.setString(4, details);
            ps.executeUpdate();
        }
    }

    /** Agrégation des signalements par POST (admin). */
    public List<ReportAgg> getReportedPostsAgg() throws SQLException {
        Connection cnx = cnx();
        String sql =
                "SELECT r.target_id AS post_id, COUNT(*) AS total, MAX(r.created_at) AS last_at, " +
                        "       GROUP_CONCAT(DISTINCT r.reason SEPARATOR ' | ') AS reasons " +
                        "FROM reports r " +
                        "WHERE r.target_type='POST' " +
                        "GROUP BY r.target_id " +
                        "ORDER BY last_at DESC";

        List<ReportAgg> out = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ReportAgg a = new ReportAgg();
                a.postId = rs.getLong("post_id");
                a.total = rs.getInt("total");
                a.lastAt = rs.getTimestamp("last_at");
                String reasons = rs.getString("reasons");
                if (reasons != null && !reasons.isBlank()) {
                    a.reasons = Arrays.asList(reasons.split("\\s\\|\\s"));
                }
                out.add(a);
            }
        }
        return out;
    }

    public void deleteReportsForPost(long postId) throws SQLException {
        Connection cnx = cnx();
        String sql = "DELETE FROM reports WHERE target_type='POST' AND target_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.executeUpdate();
        }
    }
}
