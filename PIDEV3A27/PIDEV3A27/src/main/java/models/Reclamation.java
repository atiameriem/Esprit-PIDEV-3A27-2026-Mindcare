package models;

import java.sql.Date;

public class Reclamation {

    private int id;
    private int idUser;
    private TypeReclamation type;
    private String description;
    private String statut;
    private Date date;

    public Reclamation() {
    }

    public Reclamation(int id, int idUser, TypeReclamation type,
            String description, String statut, Date date) {
        this.id = id;
        this.idUser = idUser;
        this.type = type;
        this.description = description;
        this.statut = statut;
        this.date = date;
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

    public TypeReclamation getType() {
        return type;
    }

    public void setType(TypeReclamation type) {
        this.type = type;
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

    @Override
    public String toString() {
        return "Reclamation{id=" + id + ", idUser=" + idUser +
                ", type=" + (type != null ? type.name() : "null") +
                ", statut='" + statut + "', date=" + date +

    }
}
