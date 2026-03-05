package services;

import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Historique d'activité.
 * user_activity(id, id_user, action_type, entity_type, entity_id, meta, created_at)
 */
public class ActivityService {

    public static class ActivityItem {
        public final String fullname;
        public final String actionType;
        public final String entityType;
        public final Integer entityId;
        public final String meta;
        public final LocalDateTime createdAt;

        public ActivityItem(String fullname, String actionType, String entityType, Integer entityId, String meta, LocalDateTime createdAt) {
            this.fullname = fullname;
            this.actionType = actionType;
            this.entityType = entityType;
            this.entityId = entityId;
            this.meta = meta;
            this.createdAt = createdAt;
        }
    }

    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Log simple (compat): on mappe "details" vers meta.
     */
    public void log(int idUsers, String actionType, String details) {
        log(idUsers, actionType, null, null, details);
    }

    public void log(int idUsers, String actionType, String entityType, Integer entityId, String meta) {
        if (idUsers <= 0 || actionType == null || actionType.isBlank()) return;
        try {
            String sql = "INSERT INTO user_activity (id_user, action_type, entity_type, entity_id, meta) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = cnx().prepareStatement(sql)) {
                ps.setInt(1, idUsers);
                ps.setString(2, actionType);
                ps.setString(3, entityType);
                if (entityId == null) ps.setNull(4, Types.BIGINT);
                else ps.setInt(4, entityId);
                ps.setString(5, meta);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            // On ne bloque pas l'app sur un log
            System.err.println("[ACTIVITY] " + e.getMessage());
        }
    }

    public List<ActivityItem> latest(int limit) throws SQLException {
        String sql =
                "SELECT CONCAT(u.nom,' ',u.prenom) AS fullname, a.action_type, a.entity_type, a.entity_id, a.meta, a.created_at " +
                "FROM user_activity a JOIN users u ON u.id_users=a.id_user " +
                "ORDER BY a.created_at DESC LIMIT ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<ActivityItem> out = new ArrayList<>();
                while (rs.next()) {
                    var ts = rs.getTimestamp("created_at");
                    LocalDateTime dt = ts == null ? null : ts.toLocalDateTime();
                    Integer id = (Integer) rs.getObject("entity_id");
                    out.add(new ActivityItem(
                            rs.getString("fullname"),
                            rs.getString("action_type"),
                            rs.getString("entity_type"),
                            id,
                            rs.getString("meta"),
                            dt
                    ));
                }
                return out;
            }
        }
    }
}
