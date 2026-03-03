package services;

import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Local calendar storage (DB) + ICS export with VALARM.
 * This avoids heavy Google Calendar OAuth while still giving a "calendar" module.
 */
public class CalendarService {

    private final Connection cnx;

    public CalendarService() {
        this.cnx = MyDatabase.getInstance().getConnection();
        ensureTable();
    }

    public CalendarService(Connection cnx) {
        this.cnx = cnx;
        ensureTable();
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS calendar_event (" +
                "id_event INT AUTO_INCREMENT PRIMARY KEY, " +
                "reservation_id INT UNIQUE, " +
                "title VARCHAR(255) NOT NULL, " +
                "start_dt DATETIME NOT NULL, " +
                "end_dt DATETIME NOT NULL, " +
                "status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE', " +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur création table calendar_event: " + e.getMessage(), e);
        }
    }

    public void upsertForReservation(int reservationId, String title, LocalDateTime start, LocalDateTime end, String status) throws SQLException {
        if (status == null || status.isBlank()) status = "ACTIVE";
        String sql = "INSERT INTO calendar_event(reservation_id, title, start_dt, end_dt, status) VALUES(?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE title=VALUES(title), start_dt=VALUES(start_dt), end_dt=VALUES(end_dt), status=VALUES(status)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setString(2, title);
            ps.setTimestamp(3, Timestamp.valueOf(start));
            ps.setTimestamp(4, Timestamp.valueOf(end));
            ps.setString(5, status);
            ps.executeUpdate();
        }
    }

    public void cancelForReservation(int reservationId) throws SQLException {
        String sql = "UPDATE calendar_event SET status='CANCELLED' WHERE reservation_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.executeUpdate();
        }
    }

    public List<CalendarEventDTO> listForDate(LocalDate date) throws SQLException {
        List<CalendarEventDTO> list = new ArrayList<>();
        String sql = "SELECT id_event, reservation_id, title, start_dt, end_dt, status FROM calendar_event WHERE DATE(start_dt)=? ORDER BY start_dt";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CalendarEventDTO(
                            rs.getInt("id_event"),
                            rs.getInt("reservation_id"),
                            rs.getString("title"),
                            rs.getTimestamp("start_dt").toLocalDateTime(),
                            rs.getTimestamp("end_dt").toLocalDateTime(),
                            rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Used by the WebView calendar (month/week/day views).
     * Returns events whose start_dt is within [start, end).
     */
    public List<CalendarEventDTO> listForRange(LocalDateTime start, LocalDateTime end) throws SQLException {
        List<CalendarEventDTO> list = new ArrayList<>();
        String sql = "SELECT id_event, reservation_id, title, start_dt, end_dt, status " +
                "FROM calendar_event WHERE start_dt >= ? AND start_dt < ? ORDER BY start_dt";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CalendarEventDTO(
                            rs.getInt("id_event"),
                            rs.getInt("reservation_id"),
                            rs.getString("title"),
                            rs.getTimestamp("start_dt").toLocalDateTime(),
                            rs.getTimestamp("end_dt").toLocalDateTime(),
                            rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    public static class CalendarEventDTO {
        public final int idEvent;
        public final int reservationId;
        public final String title;
        public final LocalDateTime start;
        public final LocalDateTime end;
        public final String status;

        public CalendarEventDTO(int idEvent, int reservationId, String title, LocalDateTime start, LocalDateTime end, String status) {
            this.idEvent = idEvent;
            this.reservationId = reservationId;
            this.title = title;
            this.start = start;
            this.end = end;
            this.status = status;
        }

        @Override public String toString() {
            return "[" + status + "] " + title + " (" + start.toLocalTime() + " - " + end.toLocalTime() + ") #res=" + reservationId;
        }
    }
}
