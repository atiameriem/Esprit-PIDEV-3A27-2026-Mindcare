package models;

import java.sql.Timestamp;

public class CompteRenduSeance {
    private int idCompteRendu;
    private int idAppointment;
    private Timestamp dateCreationCr;
    private ProgresCR progresCr;
    private String resumeSeanceCr;
    private String prochainesActionCr;

    // rating saisi par le patient (1..5), null si pas encore noté
    private Integer rating;
    //pour le crud ce class
    public CompteRenduSeance() {
    }
    //2 constructeurs :
//sans idCompteRendu (quand tu ajoutes, l’ID est généré par la DB)
//avec idCompteRendu (quand tu modifies/supprimes un existant)
    public CompteRenduSeance(int idAppointment, Timestamp dateCreationCr, ProgresCR progresCr, String resumeSeanceCr, String prochainesActionCr) {
        this.idAppointment = idAppointment;
        this.dateCreationCr = dateCreationCr;
        this.progresCr = progresCr;
        this.resumeSeanceCr = resumeSeanceCr;
        this.prochainesActionCr = prochainesActionCr;
    }

    public CompteRenduSeance(int idCompteRendu, int idAppointment, Timestamp dateCreationCr, ProgresCR progresCr, String resumeSeanceCr, String prochainesActionCr) {
        this.idCompteRendu = idCompteRendu;
        this.idAppointment = idAppointment;
        this.dateCreationCr = dateCreationCr;
        this.progresCr = progresCr;
        this.resumeSeanceCr = resumeSeanceCr;
        this.prochainesActionCr = prochainesActionCr;
    }

    public int getIdCompteRendu() {
        return this.idCompteRendu;
    }

    public void setIdCompteRendu(int idCompteRendu) {
        this.idCompteRendu = idCompteRendu;
    }

    public int getIdAppointment() {
        return this.idAppointment;
    }

    public void setIdAppointment(int idAppointment) {
        this.idAppointment = idAppointment;
    }

    public Timestamp getDateCreationCr() {
        return this.dateCreationCr;
    }

    public void setDateCreationCr(Timestamp dateCreationCr) {
        this.dateCreationCr = dateCreationCr;
    }

    public ProgresCR getProgresCr() {
        return this.progresCr;
    }

    public void setProgresCr(ProgresCR progresCr) {
        this.progresCr = progresCr;
    }

    public String getResumeSeanceCr() {
        return this.resumeSeanceCr;
    }

    public void setResumeSeanceCr(String resumeSeanceCr) {
        this.resumeSeanceCr = resumeSeanceCr;
    }

    public String getProchainesActionCr() {
        return this.prochainesActionCr;
    }

    public void setProchainesActionCr(String prochainesActionCr) {
        this.prochainesActionCr = prochainesActionCr;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String toString() {
        return "Compte rendu | Progrès=" + this.progresCr + " | Date=" + this.dateCreationCr + " | Résumé=\"" + this.resumeSeanceCr + "\"";
    }

    public static enum ProgresCR {
        amelioration_legere,
        amelioration_significative,
        stagnation,
        amelioration_stable;

        private ProgresCR() {
        }
    }
}
