package services;

import models.Question;
import models.Reponse;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceQuestion implements IService<Question> {

    private Connection cnx;
    private ServiceReponse serviceReponse;

    public ServiceQuestion() {
        try {
            cnx = MyDatabase.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        serviceReponse = new ServiceReponse();
    }

    @Override
    public int create(Question question) throws SQLException {
        String req = "INSERT INTO question (id_quiz, texte_question, ordre, type_question) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt   (1, question.getIdQuiz());
            pst.setString(2, question.getTexteQuestion());
            pst.setInt   (3, question.getOrdre());
            pst.setString(4, question.getTypeQuestion());
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    question.setIdQuestion(rs.getInt(1));
                    System.out.println("Question creee ID = " + question.getIdQuestion());
                    return question.getIdQuestion();
                }
            }
        }
        return -1;
    }

    // Cree une question + ses choix en une operation
    public void createAvecChoix(Question question, List<Reponse> choix) throws SQLException {
        create(question);
        for (Reponse c : choix) {
            c.setIdQuiz    (question.getIdQuiz());
            c.setIdQuestion(question.getIdQuestion());
            c.setIdUsers   (null); // choix psychologue
            serviceReponse.create(c);
        }
        System.out.println("Question + " + choix.size() + " choix crees.");
    }

    // Alias pour compatibilite avec NouveauTestQuizController
    public void addAvecChoix(Question question, List<Reponse> choix) throws SQLException {
        createAvecChoix(question, choix);
    }

    @Override
    public void update(Question question) throws SQLException {
        String req = "UPDATE question SET texte_question=?, ordre=?, type_question=? WHERE id_question=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, question.getTexteQuestion());
            pst.setInt   (2, question.getOrdre());
            pst.setString(3, question.getTypeQuestion());
            pst.setInt   (4, question.getIdQuestion());
            pst.executeUpdate();
        }
        System.out.println("Question modifiee ID = " + question.getIdQuestion());
    }

    @Override
    public void delete(int id) throws SQLException {
        String req = "DELETE FROM question WHERE id_question=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
        System.out.println("Question supprimee ID = " + id);
    }

    @Override
    public List<Question> read() throws SQLException {
        List<Question> questions = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM question ORDER BY ordre ASC")) {
            while (rs.next()) questions.add(mapResultSetToQuestion(rs));
        }
        return questions;
    }

    // ── Methodes metier ───────────────────────────────────────────

    public List<Question> getQuestionsByQuiz(int idQuiz) throws SQLException {
        List<Question> questions = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM question WHERE id_quiz = ? ORDER BY ordre ASC")) {
            pst.setInt(1, idQuiz);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) questions.add(mapResultSetToQuestion(rs));
            }
        }
        return questions;
    }

    public List<Question> getQuestionsByQuizAvecChoix(int idQuiz) throws SQLException {
        List<Question> questions = getQuestionsByQuiz(idQuiz);
        for (Question q : questions)
            q.setReponses(serviceReponse.getChoixParQuestion(q.getIdQuestion()));
        return questions;
    }

    public Question getQuestionById(int idQuestion) throws SQLException {
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM question WHERE id_question = ?")) {
            pst.setInt(1, idQuestion);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Question q = mapResultSetToQuestion(rs);
                    q.setReponses(serviceReponse.getChoixParQuestion(idQuestion));
                    return q;
                }
            }
        }
        return null;
    }

    public Question getQuestionAvecReponsesPatient(int idQuestion, int idPatient) throws SQLException {
        Question question = getQuestionById(idQuestion);
        if (question != null) {
            List<Reponse> reponsesPatient = new ArrayList<>();
            try (PreparedStatement pst = cnx.prepareStatement(
                    "SELECT * FROM reponse WHERE id_question = ? AND id_users = ?")) {
                pst.setInt(1, idQuestion); pst.setInt(2, idPatient);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        Reponse r = new Reponse();
                        r.setIdReponse(rs.getInt("id_reponse"));
                        r.setIdQuiz(rs.getInt("id_quiz"));
                        r.setIdQuestion(rs.getInt("id_question"));
                        r.setIdUsers(idPatient);
                        r.setTexteReponse(rs.getString("texte_reponse"));
                        r.setValeur(rs.getInt("valeur"));
                        reponsesPatient.add(r);
                    }
                }
            }
            question.setReponses(reponsesPatient);
        }
        return question;
    }

    public int countQuestionsByQuiz(int idQuiz) throws SQLException {
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT COUNT(*) AS total FROM question WHERE id_quiz = ?")) {
            pst.setInt(1, idQuiz);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        }
        return 0;
    }

    private Question mapResultSetToQuestion(ResultSet rs) throws SQLException {
        Question q = new Question();
        q.setIdQuestion  (rs.getInt   ("id_question"));
        q.setIdQuiz      (rs.getInt   ("id_quiz"));
        q.setTexteQuestion(rs.getString("texte_question"));
        q.setOrdre       (rs.getInt   ("ordre"));
        q.setTypeQuestion(rs.getString("type_question"));
        return q;
    }
}