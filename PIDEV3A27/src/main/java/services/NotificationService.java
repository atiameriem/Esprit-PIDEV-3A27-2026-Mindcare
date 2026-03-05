package services;

import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Notifications (DB):
 * notifications(id, id_user, type, message, entity_type, entity_id, is_read, created_at)
 */
public class NotificationService {

    public static class Notif {
        public final long id;
        public final String type;
        public final String message;
        public final String entityType;
        public final Long entityId;
        public final boolean isRead;
        public final Timestamp createdAt;

        public Notif(long id, String type, String message, String entityType, Long entityId, boolean isRead, Timestamp createdAt) {
            this.id = id;
            this.type = type;
            this.message = message;
            this.entityType = entityType;
            this.entityId = entityId;
            this.isRead = isRead;
            this.createdAt = createdAt;
        }
    }

    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Crée une notification.
     * @param receiverId utilisateur qui reçoit
     */
    public void create(int receiverId, String type, String message, String entityType, Integer entityId) {
        if (receiverId <= 0 || message == null || message.isBlank()) return;
        try {
            String sql = "INSERT INTO notifications (id_user, type, message, entity_type, entity_id) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = cnx().prepareStatement(sql)) {
                ps.setInt(1, receiverId);
                ps.setString(2, type == null ? "INFO" : type);
                ps.setString(3, message);
                ps.setString(4, entityType);
                if (entityId == null) ps.setNull(5, Types.BIGINT);
                else ps.setInt(5, entityId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[NOTIF] " + e.getMessage());
        }
    }

    public int countUnread(int receiverId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE id_user=? AND is_read=0";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Retourne les dernières notifications non lues.
     */
    public List<Notif> listUnread(int receiverId, int limit) throws SQLException {
        String sql = "SELECT id, type, message, entity_type, entity_id, is_read, created_at " +
                "FROM notifications WHERE id_user=? AND is_read=0 ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<Notif> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Notif(
                            rs.getLong("id"),
                            rs.getString("type"),
                            rs.getString("message"),
                            rs.getString("entity_type"),
                            rs.getObject("entity_id") == null ? null : rs.getLong("entity_id"),
                            rs.getInt("is_read") == 1,
                            rs.getTimestamp("created_at")
                    ));
                }
                return out;
            }
        }
    }

    public void markRead(long notifId) throws SQLException {
        String sql = "UPDATE notifications SET is_read=1 WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, notifId);
            ps.executeUpdate();
        }
    }

    public void markAllRead(int receiverId) throws SQLException {
        String sql = "UPDATE notifications SET is_read=1 WHERE id_user=? AND is_read=0";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ps.executeUpdate();
        }
    }
}
