package models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Reservation {
    private int idReservation;
    private LocalDate dateReservation;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String statut; // EN_ATTENTE / CONFIRMER / ANNULER
    private String typeSession;
    private String motif;
    private int idLocal;
    private int idUtilisateur;
    private Integer idResponsableCentre; // optional (ancien id_therapeute)
    private Integer idHistoriqueUtilise; // FK -> historique_quiz.id_historique (peut être null)

    // joined fields
    private String localNom;
    private String localType;
    private boolean localDisponible;

    public int getIdReservation() { return idReservation; }
    public void setIdReservation(int idReservation) { this.idReservation = idReservation; }

    public LocalDate getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDate dateReservation) { this.dateReservation = dateReservation; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getTypeSession() { return typeSession; }
    public void setTypeSession(String typeSession) { this.typeSession = typeSession; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public int getIdLocal() { return idLocal; }
    public void setIdLocal(int idLocal) { this.idLocal = idLocal; }

    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public Integer getIdResponsableCentre() { return idResponsableCentre; }
    public void setIdResponsableCentre(Integer idResponsableCentre) { this.idResponsableCentre = idResponsableCentre; }

    public Integer getIdHistoriqueUtilise() { return idHistoriqueUtilise; }
    public void setIdHistoriqueUtilise(Integer idHistoriqueUtilise) { this.idHistoriqueUtilise = idHistoriqueUtilise; }

    public String getLocalNom() { return localNom; }
    public void setLocalNom(String localNom) { this.localNom = localNom; }

    public String getLocalType() { return localType; }
    public void setLocalType(String localType) { this.localType = localType; }

    public boolean isLocalDisponible() { return localDisponible; }
    public void setLocalDisponible(boolean localDisponible) { this.localDisponible = localDisponible; }

    public boolean isLocked() {
        return statut != null && (statut.equalsIgnoreCase("CONFIRMER") || statut.equalsIgnoreCase("ANNULER") || statut.equalsIgnoreCase("CHECKED_IN"));
    }

    public LocalDateTime getStartDateTime() {
        if (dateReservation == null || heureDebut == null) return null;
        return LocalDateTime.of(dateReservation, heureDebut);
    }

    public LocalDateTime getEndDateTime() {
        if (dateReservation == null || heureFin == null) return null;
        return LocalDateTime.of(dateReservation, heureFin);
    }
}
