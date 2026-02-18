package models;

public class Contenu {

    private int id;
    private String type;
    private String chemin;
    private int moduleId;

    public Contenu() {
    }

    public Contenu(String type, String chemin) {
        this.type = type;
        this.chemin = chemin;
    }

    public Contenu(String type, String chemin, int moduleId) {
        this.type = type;
        this.chemin = chemin;
        this.moduleId = moduleId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getChemin() {
        return chemin;
    }

    public void setChemin(String chemin) {
        this.chemin = chemin;
    }

    public int getModuleId() {
        return moduleId;
    }

    public void setModuleId(int moduleId) {
        this.moduleId = moduleId;
    }
}
