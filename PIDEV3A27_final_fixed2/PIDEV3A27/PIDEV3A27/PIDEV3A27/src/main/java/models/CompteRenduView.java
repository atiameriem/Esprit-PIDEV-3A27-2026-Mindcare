package models;

import java.sql.Timestamp;
import java.sql.Date;
import java.sql.Time;

/**
 * DTO (vue) : compte-rendu + info du rendez-vous + noms (patient/psychologue).
 */

//C’est une classe faite pour l’affichage dans l’UI.
//PLUS des infos du rendez-vous (jointure)
//rvDate
//rvTime
//rvType
//rvStatut
//➕ PLUS des noms (jointure users/patient/psy)
//patientFullName
//psychologistFullName
public class CompteRenduView {

    private int idCompteRendu;
    private int idAppointment;

    private Timestamp dateCreationCr;
    private CompteRenduSeance.ProgresCR progresCr;
    private String resumeSeanceCr;
    private String prochainesActionCr;

    // infos RV
    private Date rvDate;
    private Time rvTime;
    private RendezVous.TypeRV rvType;
    private RendezVous.StatutRV rvStatut;

    // noms
    private String patientFullName;
    private String psychologistFullName;

    public int getIdCompteRendu() { return idCompteRendu; }
    public void setIdCompteRendu(int idCompteRendu) { this.idCompteRendu = idCompteRendu; }

    public int getIdAppointment() { return idAppointment; }
    public void setIdAppointment(int idAppointment) { this.idAppointment = idAppointment; }

    public Timestamp getDateCreationCr() { return dateCreationCr; }
    public void setDateCreationCr(Timestamp dateCreationCr) { this.dateCreationCr = dateCreationCr; }

    public CompteRenduSeance.ProgresCR getProgresCr() { return progresCr; }
    public void setProgresCr(CompteRenduSeance.ProgresCR progresCr) { this.progresCr = progresCr; }

    public String getResumeSeanceCr() { return resumeSeanceCr; }
    public void setResumeSeanceCr(String resumeSeanceCr) { this.resumeSeanceCr = resumeSeanceCr; }

    public String getProchainesActionCr() { return prochainesActionCr; }
    public void setProchainesActionCr(String prochainesActionCr) { this.prochainesActionCr = prochainesActionCr; }

    public Date getRvDate() { return rvDate; }
    public void setRvDate(Date rvDate) { this.rvDate = rvDate; }

    public Time getRvTime() { return rvTime; }
    public void setRvTime(Time rvTime) { this.rvTime = rvTime; }

    public RendezVous.TypeRV getRvType() { return rvType; }
    public void setRvType(RendezVous.TypeRV rvType) { this.rvType = rvType; }

    public RendezVous.StatutRV getRvStatut() { return rvStatut; }
    public void setRvStatut(RendezVous.StatutRV rvStatut) { this.rvStatut = rvStatut; }

    public String getPatientFullName() { return patientFullName; }
    public void setPatientFullName(String patientFullName) { this.patientFullName = patientFullName; }

    public String getPsychologistFullName() { return psychologistFullName; }
    public void setPsychologistFullName(String psychologistFullName) { this.psychologistFullName = psychologistFullName; }

}


