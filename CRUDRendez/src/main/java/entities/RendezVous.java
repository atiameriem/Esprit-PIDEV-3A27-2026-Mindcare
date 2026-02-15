// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package entities;

import java.sql.Date;
import java.sql.Time;

public class RendezVous {
    private int idRv;
    private int idPatient;
    private int idPsychologist;
    private StatutRV statutRv;
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

    public RendezVous(int idRv, int idPatient, int idPsychologist, StatutRV statutRv, Date appointmentDate, TypeRV typeRendezVous, Time appointmentTimeRv) {
        this.idRv = idRv;
        this.idPatient = idPatient;
        this.idPsychologist = idPsychologist;
        this.statutRv = statutRv;
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
        return "Rendez-vous | Statut=" + this.statutRv + " | Type=" + this.typeRendezVous + " | Date=" + this.appointmentDate + " | Heure=" + this.appointmentTimeRv;
    }

    public static enum StatutRV {
        prevu,
        termine,
        annule;

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
}
