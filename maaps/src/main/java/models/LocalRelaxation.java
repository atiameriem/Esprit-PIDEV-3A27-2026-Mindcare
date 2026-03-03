package models;

import java.math.BigDecimal;

public class LocalRelaxation {
    private int idLocal;
    private String nom;
    private String description;
    private String type; // enum in DB
    private int capacite;
    private String equipements;
    private int etage;
    private int dureeMaxSession; // minutes
    private BigDecimal tarifHoraire;
    private String etat; // enum in DB
    private boolean disponible;
    private String image;

    public LocalRelaxation() {}

    public int getIdLocal() { return idLocal; }
    public void setIdLocal(int idLocal) { this.idLocal = idLocal; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public String getEquipements() { return equipements; }
    public void setEquipements(String equipements) { this.equipements = equipements; }

    public int getEtage() { return etage; }
    public void setEtage(int etage) { this.etage = etage; }

    public int getDureeMaxSession() { return dureeMaxSession; }
    public void setDureeMaxSession(int dureeMaxSession) { this.dureeMaxSession = dureeMaxSession; }

    public BigDecimal getTarifHoraire() { return tarifHoraire; }
    public void setTarifHoraire(BigDecimal tarifHoraire) { this.tarifHoraire = tarifHoraire; }

    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    @Override
    public String toString() {
        return (nom == null ? ("Local #" + idLocal) : nom) + (type == null ? "" : (" • " + type));
    }
}
