package models;

import java.time.LocalDateTime;

public class SeanceGroupe {
    private int seanceId;
    private String titre;
    private int idFormation;
    private int idUsers;
    private LocalDateTime dateHeure;
    private int dureeMinutes;
    private String lienJitsi;
    private String statut;
    private String description;
    private int capaciteMax;
    private LocalDateTime createdAt;
    private String titreFormation; // Nom de la formation associée
    private String googleEventId; // ID de l'événement Google Calendar pour suppression/mise à jour
    private String psychoName; // Nom du psychologue créateur de la séance

    public SeanceGroupe() {
    }

    // Getters & Setters
    public String getTitreFormation() {
        return titreFormation;
    }

    public void setTitreFormation(String titreFormation) {
        this.titreFormation = titreFormation;
    }

    // Générer lien Jitsi unique
    public void genererLienJitsi() {
        String roomName = "MindCare-Session-" + ((int) (Math.random() * 100000));
        this.lienJitsi = "https://meet.ffmuc.net/" + roomName;
    }

    // Getters & Setters
    public int getSeanceId() {
        return seanceId;
    }

    public void setSeanceId(int seanceId) {
        this.seanceId = seanceId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public int getIdFormation() {
        return idFormation;
    }

    public void setIdFormation(int idFormation) {
        this.idFormation = idFormation;
    }

    public int getIdUsers() {
        return idUsers;
    }

    public void setIdUsers(int idUsers) {
        this.idUsers = idUsers;
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }

    public int getDureeMinutes() {
        return dureeMinutes;
    }

    public void setDureeMinutes(int dureeMinutes) {
        this.dureeMinutes = dureeMinutes;
    }

    public String getLienJitsi() {
        return lienJitsi;
    }

    public void setLienJitsi(String lienJitsi) {
        this.lienJitsi = lienJitsi;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(int capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    public String getGoogleEventId() {
        return googleEventId;
    }

    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }

    public String getPsychoName() {
        return psychoName;
    }

    public void setPsychoName(String psychoName) {
        this.psychoName = psychoName;
    }
}