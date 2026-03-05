package models;

import java.time.LocalDateTime;

public class LoginHistory {

    private int id;
    private int idUser;
    private LocalDateTime loginDate;
    private String deviceName;
    private String deviceType; // "Desktop", "Laptop", "Mobile", "Tablet", "Unknown"
    private String osName;
    private String ipAddress;

    public LoginHistory() {
    }

    public LoginHistory(int idUser, LocalDateTime loginDate, String deviceName,
            String deviceType, String osName, String ipAddress) {
        this.idUser = idUser;
        this.loginDate = loginDate;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.osName = osName;
        this.ipAddress = ipAddress;
    }

    // ===== GETTERS & SETTERS =====

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public LocalDateTime getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(LocalDateTime loginDate) {
        this.loginDate = loginDate;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Returns the emoji icon corresponding to the device type.
     */
    public String getDeviceIcon() {
        if (deviceType == null)
            return "🖥️";
        return switch (deviceType) {
            case "Laptop" -> "💻";
            case "Mobile" -> "📱";
            case "Tablet" -> "📲";
            default -> "🖥️";
        };
    }

    @Override
    public String toString() {
        return "LoginHistory{id=" + id + ", idUser=" + idUser +
                ", loginDate=" + loginDate + ", device='" + deviceName +
                "', type='" + deviceType + "', os='" + osName + "'}";
    }
}
