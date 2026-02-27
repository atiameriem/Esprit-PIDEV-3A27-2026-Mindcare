package models;
import java.time.LocalDateTime;

public class SeanceContenu {
    private int idC;
    private int seanceId;
    private int idUsers;
    private String type;
    private String chemin;
    private String nomFichier;
    private long tailleOctets;
    private int moduleId;
    private LocalDateTime partageAt;

    public SeanceContenu() {}

    public SeanceContenu(int seanceId, int idUsers, String type,
                         String chemin, String nomFichier, long tailleOctets) {
        this.seanceId = seanceId;
        this.idUsers = idUsers;
        this.type = type;
        this.chemin = chemin;
        this.nomFichier = nomFichier;
        this.tailleOctets = tailleOctets;
        this.partageAt = LocalDateTime.now();
    }

    // Icône selon type fichier
    public String getIcone() {
        if (type == null) return "📁";
        return switch (type.toLowerCase()) {
            case "pdf"              -> "📄";
            case "jpg","jpeg","png" -> "🖼️";
            case "mp4","avi"        -> "🎥";
            case "mp3","wav"        -> "🎵";
            case "docx","doc"       -> "📝";
            case "xlsx","xls"       -> "📊";
            case "pptx","ppt"       -> "📊";
            default                 -> "📁";
        };
    }

    // Taille lisible
    public String getTailleFormatee() {
        if (tailleOctets < 1024) return tailleOctets + " B";
        if (tailleOctets < 1024 * 1024) return (tailleOctets / 1024) + " KB";
        return (tailleOctets / (1024 * 1024)) + " MB";
    }

    // Getters & Setters
    public int getIdC() { return idC; }
    public void setIdC(int idC) { this.idC = idC; }
    public int getSeanceId() { return seanceId; }
    public void setSeanceId(int seanceId) { this.seanceId = seanceId; }
    public int getIdUsers() { return idUsers; }
    public void setIdUsers(int idUsers) { this.idUsers = idUsers; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getChemin() { return chemin; }
    public void setChemin(String chemin) { this.chemin = chemin; }
    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }
    public long getTailleOctets() { return tailleOctets; }
    public void setTailleOctets(long tailleOctets) { this.tailleOctets = tailleOctets; }
    public int getModuleId() { return moduleId; }
    public void setModuleId(int moduleId) { this.moduleId = moduleId; }
    public LocalDateTime getPartageAt() { return partageAt; }
    public void setPartageAt(LocalDateTime partageAt) { this.partageAt = partageAt; }
}