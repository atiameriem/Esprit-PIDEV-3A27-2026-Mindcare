package models;

import java.sql.Date;
import java.sql.Time;

public class RendezVous {
    private int idRv;
    private int idPatient;
    private int idPsychologist;
    private StatutRV statutRv;
    // ✅ Nouveau champ : confirmation du rendez-vous (fait par le psychologue)
    private ConfirmationStatus confirmationStatus;
    private Date appointmentDate;
    private TypeRV typeRendezVous;
    private Time appointmentTimeRv;

    public RendezVous() {
    }

    public RendezVous(int idPatient, int idPsychologist, StatutRV statutRv, Date appointmentDate, TypeRV typeRendezVous, Time appointmentTimeRv) {
        this.idPatient = idPatient;
        this.idPsychologist = idPsychologist;
        this.statutRv = statutRv;
        this.appointmentDate = appointmentDate;
        this.typeRendezVous = typeRendezVous;
        this.appointmentTimeRv = appointmentTimeRv;
    }

    // ✅ Constructeur pratique côté patient : on passe aussi confirmation_status
    //    (ex: en_attente) sans avoir un id_rv (car il est auto-incrémenté en base).
    public RendezVous(int idPatient, int idPsychologist, StatutRV statutRv, ConfirmationStatus confirmationStatus,
                      Date appointmentDate, TypeRV typeRendezVous, Time appointmentTimeRv) {
        this.idPatient = idPatient;
        this.idPsychologist = idPsychologist;
        this.statutRv = statutRv;
        this.confirmationStatus = confirmationStatus;
        this.appointmentDate = appointmentDate;
        this.typeRendezVous = typeRendezVous;
        this.appointmentTimeRv = appointmentTimeRv;
    }

    public RendezVous(int idRv, int idPatient, int idPsychologist, StatutRV statutRv, Date appointmentDate, TypeRV typeRendezVous, Time appointmentTimeRv) {
        this.idRv = idRv;
        this.idPatient = idPatient;
        this.idPsychologist = idPsychologist;
        this.statutRv = statutRv;
        this.appointmentDate = appointmentDate;
        this.typeRendezVous = typeRendezVous;
        this.appointmentTimeRv = appointmentTimeRv;
    }

    // ✅ Constructeur pratique quand on veut aussi passer confirmation_status
    public RendezVous(int idRv, int idPatient, int idPsychologist, StatutRV statutRv, ConfirmationStatus confirmationStatus,
                      Date appointmentDate, TypeRV typeRendezVous, Time appointmentTimeRv) {
        this.idRv = idRv;
        this.idPatient = idPatient;
        this.idPsychologist = idPsychologist;
        this.statutRv = statutRv;
        this.confirmationStatus = confirmationStatus;
        this.appointmentDate = appointmentDate;
        this.typeRendezVous = typeRendezVous;
        this.appointmentTimeRv = appointmentTimeRv;
    }

    public int getIdRv() {
        return this.idRv;
    }

    public void setIdRv(int idRv) {
        this.idRv = idRv;
    }

    public int getIdPatient() {
        return this.idPatient;
    }

    public void setIdPatient(int idPatient) {
        this.idPatient = idPatient;
    }

    public int getIdPsychologist() {
        return this.idPsychologist;
    }

    public void setIdPsychologist(int idPsychologist) {
        this.idPsychologist = idPsychologist;
    }

    public StatutRV getStatutRv() {
        return this.statutRv;
    }

    public void setStatutRv(StatutRV statutRv) {
        this.statutRv = statutRv;
    }

    public ConfirmationStatus getConfirmationStatus() {
        return confirmationStatus;
    }

    public void setConfirmationStatus(ConfirmationStatus confirmationStatus) {
        this.confirmationStatus = confirmationStatus;
    }

    public Date getAppointmentDate() {
        return this.appointmentDate;
    }

    public void setAppointmentDate(Date appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public TypeRV getTypeRendezVous() {
        return this.typeRendezVous;
    }

    public void setTypeRendezVous(TypeRV typeRendezVous) {
        this.typeRendezVous = typeRendezVous;
    }

    public Time getAppointmentTimeRv() {
        return this.appointmentTimeRv;
    }

    public void setAppointmentTimeRv(Time appointmentTimeRv) {
        this.appointmentTimeRv = appointmentTimeRv;
    }

    public String toString() {
        return "Rendez-vous | Confirmation=" + this.confirmationStatus
                + " | Statut=" + this.statutRv
                + " | Type=" + this.typeRendezVous
                + " | Date=" + this.appointmentDate
                + " | Heure=" + this.appointmentTimeRv;
    }

    public static enum StatutRV {
        termine,
        en_cours;

        private StatutRV() {
        }
    }

    public static enum TypeRV {
        premiere_consultation,
        suivi,
        urgence;

        private TypeRV() {
        }
    }

    // ✅ Confirmation du rendez-vous :
    // - en_attente : patient vient de créer/modifier (psy n'a pas encore répondu)
    // - confirme   : psy accepte (patient ne peut plus modifier/supprimer)
    // - annule     : psy refuse/annule
    public static enum ConfirmationStatus {
        confirme,
        annule,
        en_attente;

        private ConfirmationStatus() {
        }
    }
}
