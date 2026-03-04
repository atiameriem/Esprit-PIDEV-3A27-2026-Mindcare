package models;

import java.sql.Date;

public class Reclamation {

    private int id;
    private int idUser;
    private String objet;
    private String urgence;
    private String description;
    private String statut;
    private Date date;
    private String reponse;
    private String categorie;
    private String resume;

    public Reclamation() {
    }

    public Reclamation(int id, int idUser, String objet, String urgence,
            String description, String statut, Date date, String reponse, String categorie, String resume) {
        this.id = id;
        this.idUser = idUser;
        this.objet = objet;
        this.urgence = urgence;
        this.description = description;
        this.statut = statut;
        this.date = date;
        this.reponse = reponse;
        this.categorie = categorie;
        this.resume = resume;
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

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public String getUrgence() {
        return urgence;
    }

    public void setUrgence(String urgence) {
        this.urgence = urgence;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    @Override
    public String toString() {
        return "Reclamation{id=" + id + ", idUser=" + idUser +
                ", objet='" + objet + "', urgence='" + urgence +
                "', statut='" + statut + "', date=" + date +
                ", description='" + description + "', reponse='" + reponse +
                "', categorie='" + categorie + "', resume='" + resume + "'}";
    }
}
