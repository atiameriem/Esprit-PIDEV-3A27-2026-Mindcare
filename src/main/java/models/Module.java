package models;

import java.util.ArrayList;
import java.util.List;

public class Module {

    private int id;
    private String titre;
    private String description;
    private int formationId;
    private List<Contenu> contenus = new ArrayList<>();

    public Module() {
    }

    public Module(String titre, String description) {
        this.titre = titre;
        this.description = description;
    }

    public Module(String titre, String description, int formationId) {
        this.titre = titre;
        this.description = description;
        this.formationId = formationId;
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

    public int getFormationId() {
        return formationId;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public void ajouterContenu(Contenu contenu) {
        contenus.add(contenu);
    }

    public List<Contenu> getContenus() {
        return contenus;
    }

    public void setContenus(List<Contenu> contenus) {
        this.contenus = contenus != null ? contenus : new ArrayList<>();
    }
}
