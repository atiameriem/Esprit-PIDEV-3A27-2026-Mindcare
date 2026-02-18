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
        cnx = MyDatabase.getInstance().getConnection();
        serviceReponse = new ServiceReponse();
    }

    // ══════════════════════════════════════════════════════════════
    //  CRUD
    // ══════════════════════════════════════════════════════════════

    @Override
    public void add(Question question) throws SQLException {
        String req = "INSERT INTO question (id_quiz, texte_question, ordre, type_question) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt   (1, question.getIdQuiz());
            pst.setString(2, question.getTexteQuestion());
            pst.setInt   (3, question.getOrdre());
            pst.setString(4, question.getTypeQuestion());

            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    question.setIdQuestion(rs.getInt(1));
                }
            }
        }
        System.out.println("Question ajoutée ID = " + question.getIdQuestion());
    }

    // Ajouter une question avec ses choix possibles en une seule opération
    public void addAvecChoix(Question question, List<Reponse> choix) throws SQLException {
        // 1. Ajouter la question
        add(question);

        // 2. Ajouter chaque choix (id_users = NULL car créé par psychologue)
        for (Reponse choixReponse : choix) {
            choixReponse.setIdQuiz    (question.getIdQuiz());
            choixReponse.setIdQuestion(question.getIdQuestion());
            choixReponse.setIdUsers   (null); // choix possible = pas de patient
            serviceReponse.add(choixReponse);
        }

        System.out.println("Question + " + choix.size() + " choix ajoutés.");
    }

    @Override
    public void update(Question question) throws SQLException {
        String req = "UPDATE question SET texte_question=?, ordre=?, type_question=? " +
                "WHERE id_question=?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, question.getTexteQuestion());
            pst.setInt   (2, question.getOrdre());
            pst.setString(3, question.getTypeQuestion());
            pst.setInt   (4, question.getIdQuestion());

            pst.executeUpdate();
        }
        System.out.println("Question modifiée ID = " + question.getIdQuestion());
    }

    @Override
    public void delete(Question question) throws SQLException {
        // Les réponses liées seront supprimées en CASCADE via id_quiz → quiz
        String req = "DELETE FROM question WHERE id_question=?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, question.getIdQuestion());
            pst.executeUpdate();
        }
        System.out.println("Question supprimée ID = " + question.getIdQuestion());
    }

    @Override
    public List<Question> getAll() throws SQLException {
        List<Question> questions = new ArrayList<>();
        String req = "SELECT * FROM question ORDER BY ordre ASC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                questions.add(mapResultSetToQuestion(rs));
            }
        }
        return questions;
    }

    // ══════════════════════════════════════════════════════════════
    //  MÉTHODES MÉTIER
    // ══════════════════════════════════════════════════════════════

    // Récupérer toutes les questions d'un quiz (sans les choix)
    public List<Question> getQuestionsByQuiz(int idQuiz) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String req = "SELECT * FROM question WHERE id_quiz = ? ORDER BY ordre ASC";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idQuiz);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }
        }
        return questions;
    }

    // Récupérer toutes les questions d'un quiz AVEC leurs choix possibles
    public List<Question> getQuestionsByQuizAvecChoix(int idQuiz) throws SQLException {
        List<Question> questions = getQuestionsByQuiz(idQuiz);

        // Pour chaque question, charger ses choix (id_users = NULL)
        for (Question question : questions) {
            List<Reponse> choix = serviceReponse.getChoixParQuestion(question.getIdQuestion());
            question.setReponses(choix);
        }

        return questions;
    }

    // Récupérer une question par son ID
    public Question getQuestionById(int idQuestion) throws SQLException {
        String req = "SELECT * FROM question WHERE id_question = ?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idQuestion);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Question question = mapResultSetToQuestion(rs);
                    // Charger les choix possibles
                    question.setReponses(
                            serviceReponse.getChoixParQuestion(idQuestion)
                    );
                    return question;
                }
            }
        }
        return null;
    }

    // Récupérer une question par son ID avec les réponses d'un patient
    public Question getQuestionAvecReponsesPatient(int idQuestion, int idPatient) throws SQLException {
        Question question = getQuestionById(idQuestion);

        if (question != null) {
            // Remplacer les choix par les réponses soumises par ce patient
            String req = "SELECT * FROM reponse WHERE id_question = ? AND id_users = ?";

            try (PreparedStatement pst = cnx.prepareStatement(req)) {
                pst.setInt(1, idQuestion);
                pst.setInt(2, idPatient);

                List<Reponse> reponsesPatient = new ArrayList<>();
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        Reponse r = new Reponse();
                        r.setIdReponse   (rs.getInt   ("id_reponse"));
                        r.setIdQuiz      (rs.getInt   ("id_quiz"));
                        r.setIdQuestion  (rs.getInt   ("id_question"));
                        r.setIdUsers     (idPatient);
                        r.setTexteReponse(rs.getString("texte_reponse"));
                        r.setValeur      (rs.getInt   ("valeur"));
                        reponsesPatient.add(r);
                    }
                }
                question.setReponses(reponsesPatient);
            }
        }
        return question;
    }

    // Compter le nombre de questions d'un quiz
    public int countQuestionsByQuiz(int idQuiz) throws SQLException {
        String req = "SELECT COUNT(*) AS total FROM question WHERE id_quiz = ?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idQuiz);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPER PRIVÉ
    // ══════════════════════════════════════════════════════════════

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