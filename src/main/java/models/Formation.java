package models;

import java.util.ArrayList;
import java.util.List;

public class Formation {

    private int id;
    private String titre;
    private String description;
    private String duree;
    private String niveau;
    private String imagePath;
    private List<Module> modules = new ArrayList<>();

    public Formation() {
    }

    public Formation(String titre, String description,
                     String duree, String niveau) {
        this.titre = titre;
        this.description = description;
        this.duree = duree;
        this.niveau = niveau;
    }

    public Formation(String titre, String description,
                     String duree, String niveau, String imagePath) {
        this.titre = titre;
        this.description = description;
        this.duree = duree;
        this.niveau = niveau;
        this.imagePath = imagePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDuree() {
        return duree;
    }

    public void setDuree(String duree) {
        this.duree = duree;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void ajouterModule(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules != null ? modules : new ArrayList<>();
    }
}
