package models;

import java.util.List;

public class Question {
    private int idQuestion;
    private int idQuiz;
    private String texteQuestion;
    private int ordre;
    private String typeQuestion;
    private List<Reponse> reponses; // choix possibles (null) + réponses patients

    public Question() {}

    public Question(int idQuiz, String texteQuestion, int ordre, String typeQuestion) {
        this.idQuiz = idQuiz;
        this.texteQuestion = texteQuestion;
        this.ordre = ordre;
        this.typeQuestion = typeQuestion;
    }

    public int getIdQuestion() { return idQuestion; }
    public void setIdQuestion(int idQuestion) { this.idQuestion = idQuestion; }
    public int getIdQuiz() { return idQuiz; }
    public void setIdQuiz(int idQuiz) { this.idQuiz = idQuiz; }
    public String getTexteQuestion() { return texteQuestion; }
    public void setTexteQuestion(String texteQuestion) { this.texteQuestion = texteQuestion; }
    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
    public String getTypeQuestion() { return typeQuestion; }
    public void setTypeQuestion(String typeQuestion) { this.typeQuestion = typeQuestion; }
    public List<Reponse> getReponses() { return reponses; }
    public void setReponses(List<Reponse> reponses) { this.reponses = reponses; }

    @Override
    public String toString() {
        return "Question{" +
                "idQuestion=" + idQuestion +
                ", idQuiz=" + idQuiz +
                ", texteQuestion='" + texteQuestion + '\'' +
                ", ordre=" + ordre +
                ", typeQuestion='" + typeQuestion + '\'' +
                '}';
    }
}