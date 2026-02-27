package models;

import java.util.Date;

public class Participation {
    private int idParticipation;
    private int idUser;
    private int idFormation;
    private Date dateInscription;
    private String statut; // "en attente", "accepté"
    private int rating; // 0 à 5 stars

    private String titreFormation;
    private String imagePath;


    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String path) {
        this.imagePath = path;
    }

    public Participation() {
    }

    public Participation(int idUser, int idFormation, Date dateInscription, String statut) {
        this.idUser = idUser;
        this.idFormation = idFormation;
        this.dateInscription = dateInscription;
        this.statut = statut;
    }

    public int getIdParticipation() {
        return idParticipation;
    }

    public void setIdParticipation(int idParticipation) {
        this.idParticipation = idParticipation;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public int getIdFormation() {
        return idFormation;
    }

    public void setIdFormation(int idFormation) {
        this.idFormation = idFormation;
    }

    public Date getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(Date dateInscription) {
        this.dateInscription = dateInscription;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getTitreFormation() {
        return titreFormation;
    }

    public void setTitreFormation(String titreFormation) {
        this.titreFormation = titreFormation;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
