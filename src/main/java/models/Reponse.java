package models;

import java.time.LocalDateTime;

public class Reponse {
    private int idReponse;
    private int idQuiz;
    private int idQuestion;
    private Integer idUsers;      // NULL = choix créé par psychologue
    // valeur = réponse soumise par patient
    private String texteReponse;
    private int valeur;
    private LocalDateTime dateReponse;

    public Reponse() {}

    // ── Psychologue crée un choix possible ──────────────────────────────
    // id_users = NULL
    public Reponse(int idQuiz, int idQuestion, String texteReponse, int valeur) {
        this.idQuiz       = idQuiz;
        this.idQuestion   = idQuestion;
        this.texteReponse = texteReponse;
        this.valeur       = valeur;
        this.idUsers      = null; // pas encore répondu par un patient
        this.dateReponse  = LocalDateTime.now();
    }

    // ── Patient soumet sa réponse ────────────────────────────────────────
    // id_users = id du patient
    public Reponse(int idQuiz, int idQuestion, int idUsers,
                   String texteReponse, int valeur) {
        this.idQuiz       = idQuiz;
        this.idQuestion   = idQuestion;
        this.idUsers      = idUsers;
        this.texteReponse = texteReponse;
        this.valeur       = valeur;
        this.dateReponse  = LocalDateTime.now();
    }

    // ── Méthode utilitaire ───────────────────────────────────────────────
    public boolean isChoixPsychologue() { return this.idUsers == null; }
    public boolean isReponsePatient()   { return this.idUsers != null; }

    // ── Getters & Setters ────────────────────────────────────────────────
    public int getIdReponse() { return idReponse; }
    public void setIdReponse(int idReponse) { this.idReponse = idReponse; }
    public int getIdQuiz() { return idQuiz; }
    public void setIdQuiz(int idQuiz) { this.idQuiz = idQuiz; }
    public int getIdQuestion() { return idQuestion; }
    public void setIdQuestion(int idQuestion) { this.idQuestion = idQuestion; }
    public Integer getIdUsers() { return idUsers; }
    public void setIdUsers(Integer idUsers) { this.idUsers = idUsers; }
    public String getTexteReponse() { return texteReponse; }
    public void setTexteReponse(String texteReponse) { this.texteReponse = texteReponse; }
    public int getValeur() { return valeur; }
    public void setValeur(int valeur) { this.valeur = valeur; }
    public LocalDateTime getDateReponse() { return dateReponse; }
    public void setDateReponse(LocalDateTime dateReponse) { this.dateReponse = dateReponse; }

    @Override
    public String toString() {
        return "Reponse{" +
                "idReponse=" + idReponse +
                ", idQuiz=" + idQuiz +
                ", idQuestion=" + idQuestion +
                ", idUsers=" + (idUsers == null ? "choix psychologue" : idUsers) +
                ", texteReponse='" + texteReponse + '\'' +
                ", valeur=" + valeur +
                ", dateReponse=" + dateReponse +
                '}';
    }
}