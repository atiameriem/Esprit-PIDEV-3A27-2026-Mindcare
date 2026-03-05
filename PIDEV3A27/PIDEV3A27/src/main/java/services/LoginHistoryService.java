package services;

import models.LoginHistory;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoginHistoryService {

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    // ================= CREATE TABLE IF NOT EXISTS =================
    public void ensureTableExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS login_history (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    id_users INT NOT NULL,
                    login_date DATETIME NOT NULL,
                    device_name VARCHAR(255),
                    device_type VARCHAR(50),
                    os_name VARCHAR(100),
                    ip_address VARCHAR(50),
                    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
                )
                """;
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("❌ Erreur création table login_history: " + e.getMessage());
        }
    }

    // ================= RECORD LOGIN =================
    public void recordLogin(int userId) {
        ensureTableExists();

        // Detect device info from Java system properties
        String osName = System.getProperty("os.name", "Unknown");
        String hostname = getHostname();
        String deviceType = detectDeviceType(osName);
        String ipAddress = getLocalIp();

        String sql = "INSERT INTO login_history (id_users, login_date, device_name, device_type, os_name, ip_address) "
                +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, hostname);
            stmt.setString(4, deviceType);
            stmt.setString(5, osName);
            stmt.setString(6, ipAddress);
            stmt.executeUpdate();
            System.out.println("✅ Login enregistré pour userId=" + userId + " depuis " + hostname);
        } catch (SQLException e) {
            System.err.println("❌ Erreur enregistrement login: " + e.getMessage());
        }
    }

    // ================= GET BY USER =================
    public List<LoginHistory> getByUser(int userId) {
        ensureTableExists();
        List<LoginHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM login_history WHERE id_users = ? ORDER BY login_date DESC";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LoginHistory h = new LoginHistory();
                    h.setId(rs.getInt("id"));
                    h.setIdUser(rs.getInt("id_users"));
                    Timestamp ts = rs.getTimestamp("login_date");
                    h.setLoginDate(ts != null ? ts.toLocalDateTime() : null);
                    h.setDeviceName(rs.getString("device_name"));
                    h.setDeviceType(rs.getString("device_type"));
                    h.setOsName(rs.getString("os_name"));
                    h.setIpAddress(rs.getString("ip_address"));
                    list.add(h);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lecture login_history: " + e.getMessage());
        }
        return list;
    }

    // ================= CLEAR HISTORY FOR USER =================
    public void clearHistory(int userId) {
        String sql = "DELETE FROM login_history WHERE id_users = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression login_history: " + e.getMessage());
        }
    }

    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Appareil inconnu";
        }
    }

    private String getLocalIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String detectDeviceType(String osName) {
        if (osName == null)
            return "Desktop";
        String os = osName.toLowerCase();
        if (os.contains("android") || os.contains("ios") || os.contains("iphone"))
            return "Mobile";
        if (os.contains("ipad"))
            return "Tablet";
        // On Windows/Linux/Mac → Desktop or Laptop — we can't distinguish easily,
        // so return Desktop by default
        return "Desktop";
    }
}
