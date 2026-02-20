package models;

import java.sql.Date;
import java.sql.Time;

/**
 * DTO (vue) : rendez-vous + noms (patient/psychologue) via jointure users.
 * On garde les IDs pour les opérations, mais l'UI peut afficher uniquement les noms.
 */
public class RendezVousView {

    private int idRv;
    private int idPatient;
    private int idPsychologist;

    private RendezVous.StatutRV statutRv;
    // ✅ Nouveau champ (DB) : confirmation_status (confirme/annule/en_attente)
    private RendezVous.ConfirmationStatus confirmationStatus;
    private Date appointmentDate;
    private RendezVous.TypeRV typeRendezVous;
    private Time appointmentTimeRv;

    private String patientFullName;
    private String psychologistFullName;

    public int getIdRv() { return idRv; }
    public void setIdRv(int idRv) { this.idRv = idRv; }

    public int getIdPatient() { return idPatient; }
    public void setIdPatient(int idPatient) { this.idPatient = idPatient; }

    public int getIdPsychologist() { return idPsychologist; }
    public void setIdPsychologist(int idPsychologist) { this.idPsychologist = idPsychologist; }

    public RendezVous.StatutRV getStatutRv() { return statutRv; }
    public void setStatutRv(RendezVous.StatutRV statutRv) { this.statutRv = statutRv; }

    public RendezVous.ConfirmationStatus getConfirmationStatus() { return confirmationStatus; }
    public void setConfirmationStatus(RendezVous.ConfirmationStatus confirmationStatus) { this.confirmationStatus = confirmationStatus; }

    public Date getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(Date appointmentDate) { this.appointmentDate = appointmentDate; }

    public RendezVous.TypeRV getTypeRendezVous() { return typeRendezVous; }
    public void setTypeRendezVous(RendezVous.TypeRV typeRendezVous) { this.typeRendezVous = typeRendezVous; }

    public Time getAppointmentTimeRv() { return appointmentTimeRv; }
    public void setAppointmentTimeRv(Time appointmentTimeRv) { this.appointmentTimeRv = appointmentTimeRv; }

    public String getPatientFullName() { return patientFullName; }
    public void setPatientFullName(String patientFullName) { this.patientFullName = patientFullName; }

    public String getPsychologistFullName() { return psychologistFullName; }
    public void setPsychologistFullName(String psychologistFullName) { this.psychologistFullName = psychologistFullName; }

    public String formatForChoice() {
        String date = appointmentDate == null ? "" : appointmentDate.toString();
        String time = appointmentTimeRv == null ? "" : appointmentTimeRv.toString();
        String who  = patientFullName == null ? "" : patientFullName;
        String type = typeRendezVous == null ? "" : typeRendezVous.name();
        String conf = confirmationStatus == null ? "" : confirmationStatus.name();
        return date + " " + time + "  •  " + who + "  •  " + type + (conf.isBlank() ? "" : "  •  " + conf);
    }


    @Override
    public String toString() {
        // Pour ChoiceDialog/ComboBox : afficher lisiblement le rendez-vous
        return formatForChoice();
    }
}
