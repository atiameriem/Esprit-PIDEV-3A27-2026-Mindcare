package models;

import java.time.LocalDateTime;
import java.util.List;

public class Quiz {
    private int idQuiz;
    private int idUsers;    // l'utilisateur lié au quiz
    private int creePar;    // ← AJOUTÉ : correspond à cree_par en DB
    //   (le psychologue qui a créé le quiz)
    private String titre;
    private String description;
    private String typeTest;
    private boolean actif;
    private LocalDateTime dateCreation;
    private List<Question> questions;

    public Quiz() {}

    // ── Psychologue crée un quiz ─────────────────────────────────────────
    public Quiz(int idUsers, int creePar, String titre,
                String description, String typeTest, boolean actif) {
        this.idUsers     = idUsers;
        this.creePar     = creePar;  // id du psychologue créateur
        this.titre       = titre;
        this.description = description;
        this.typeTest    = typeTest;
        this.actif       = actif;
        this.dateCreation = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────
    public int getIdQuiz() { return idQuiz; }
    public void setIdQuiz(int idQuiz) { this.idQuiz = idQuiz; }

    public int getIdUsers() { return idUsers; }
    public void setIdUsers(int idUsers) { this.idUsers = idUsers; }

    public int getCreePar() { return creePar; }        // ← AJOUTÉ
    public void setCreePar(int creePar) { this.creePar = creePar; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTypeTest() { return typeTest; }
    public void setTypeTest(String typeTest) { this.typeTest = typeTest; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    @Override
    public String toString() {
        return "Quiz{" +
                "idQuiz=" + idQuiz +
                ", idUsers=" + idUsers +
                ", creePar=" + creePar +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", typeTest='" + typeTest + '\'' +
                ", actif=" + actif +
                ", dateCreation=" + dateCreation +
                '}';
    }
}