package models;

public class ProgressionModule {
    private int id;
    private int id_user;
    private int id_module;
    private double taux_completion;
    private int temps_passe; // temps passé en minutes

    public ProgressionModule() {
    }

    public ProgressionModule(int id, int id_user, int id_module, double taux_completion, int temps_passe) {
        this.id = id;
        this.id_user = id_user;
        this.id_module = id_module;
        this.taux_completion = taux_completion;
        this.temps_passe = temps_passe;
    }

    public ProgressionModule(int id_user, int id_module, double taux_completion, int temps_passe) {
        this.id_user = id_user;
        this.id_module = id_module;
        this.taux_completion = taux_completion;
        this.temps_passe = temps_passe;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public int getId_module() {
        return id_module;
    }

    public void setId_module(int id_module) {
        this.id_module = id_module;
    }

    public double getTaux_completion() {
        return taux_completion;
    }

    public void setTaux_completion(double taux_completion) {
        this.taux_completion = taux_completion;
    }

    public int getTemps_passe() {
        return temps_passe;
    }

    public void setTemps_passe(int temps_passe) {
        this.temps_passe = temps_passe;
    }
}
