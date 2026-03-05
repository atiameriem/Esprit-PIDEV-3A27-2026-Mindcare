package services;

import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard admin (forum): stats + top users.
 */
public class AdminDashboardService {

    public static class DailyCount {
        public final LocalDate day;
        public final int count;

        public DailyCount(LocalDate day, int count) {
            this.day = day;
            this.count = count;
        }
    }

    public static class TopUser {
        public final int idUsers;
        public final String fullname;
        public final int points;

        public TopUser(int idUsers, String fullname, int points) {
            this.idUsers = idUsers;
            this.fullname = fullname;
            this.points = points;
        }
    }

    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    public int countPostsToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM post WHERE status='PUBLISHED' AND DATE(created_at)=CURDATE()";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<DailyCount> postsPerDay(int days) throws SQLException {
        int d = Math.max(1, days);
        String sql =
                "SELECT DATE(created_at) AS day, COUNT(*) AS c " +
                "FROM post WHERE status='PUBLISHED' AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                "GROUP BY DATE(created_at) ORDER BY day DESC";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, d - 1);
            try (ResultSet rs = ps.executeQuery()) {
                List<DailyCount> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new DailyCount(rs.getDate("day").toLocalDate(), rs.getInt("c")));
                }
                return out;
            }
        }
    }

    public int countReportsTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reports";
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<TopUser> topUsersByPoints(int limit) throws SQLException {
        String sql =
                "SELECT u.id_users, CONCAT(u.nom,' ',u.prenom) AS fullname, up.points " +
                        "FROM user_points up JOIN users u ON u.id_users=up.id_user " +
                        "ORDER BY up.points DESC LIMIT ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<TopUser> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new TopUser(rs.getInt("id_users"), rs.getString("fullname"), rs.getInt("points")));
                }
                return out;
            }
        }
    }
}
