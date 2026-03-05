package services;

import models.Quiz;
import models.Question;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceQuiz implements IService<Quiz> {

    private Connection cnx; // ✅ FIXED: was missing — caused "cannot find symbol" compile error
    private ServiceQuestion serviceQuestion;

    private Connection getConnection() throws SQLException {
        return MyDatabase.getInstance().getConnection();
    }

    public ServiceQuiz() {
        try {
            cnx = MyDatabase.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        serviceQuestion = new ServiceQuestion();
    }

    @Override
    public int create(Quiz quiz) throws SQLException {
        String req = "INSERT INTO quiz (id_users, cree_par, titre, description, type_test, actif, date_creation) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt    (1, quiz.getIdUsers());
            pst.setInt    (2, quiz.getCreePar());
            pst.setString (3, quiz.getTitre());
            pst.setString (4, quiz.getDescription());
            pst.setString (5, quiz.getTypeTest());
            pst.setBoolean(6, quiz.isActif());
            pst.setTimestamp(7, Timestamp.valueOf(
                    quiz.getDateCreation() != null ? quiz.getDateCreation() : LocalDateTime.now()));
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    quiz.setIdQuiz(rs.getInt(1));
                    System.out.println("Quiz cree ID = " + quiz.getIdQuiz()
                            + (quiz.getIdUsers() == 0 ? " [GLOBAL]" : " [Patient ID=" + quiz.getIdUsers() + "]"));
                    return quiz.getIdQuiz();
                }
            }
        }
        return -1;
    }

    @Override
    public void update(Quiz quiz) throws SQLException {
        String req = "UPDATE quiz SET titre=?, description=?, type_test=?, actif=? WHERE id_quiz=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString (1, quiz.getTitre());
            pst.setString (2, quiz.getDescription());
            pst.setString (3, quiz.getTypeTest());
            pst.setBoolean(4, quiz.isActif());
            pst.setInt    (5, quiz.getIdQuiz());
            pst.executeUpdate();
        }
        System.out.println("Quiz modifie ID = " + quiz.getIdQuiz());
    }

    @Override
    public void delete(int id) throws SQLException {
        String req = "DELETE FROM quiz WHERE id_quiz=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
        System.out.println("Quiz supprime ID = " + id);
    }

    @Override
    public List<Quiz> read() throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();
        String req = "SELECT q.*, u.nom, u.prenom FROM quiz q "
                + "JOIN users u ON q.cree_par = u.id_users ORDER BY q.date_creation DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) quizzes.add(mapResultSetToQuiz(rs));
        }
        return quizzes;
    }

    // ── Methodes metier ───────────────────────────────────────────

    public Quiz getQuizById(int idQuiz) throws SQLException {
        String req = "SELECT * FROM quiz WHERE id_quiz=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idQuiz);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Quiz quiz = mapResultSetToQuiz(rs);
                    quiz.setQuestions(serviceQuestion.getQuestionsByQuiz(idQuiz));
                    return quiz;
                }
            }
        }
        return null;
    }

    public List<Quiz> getQuizParPsychologue(int idPsychologue) throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();
        String req = "SELECT * FROM quiz WHERE cree_par = ? ORDER BY date_creation DESC";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPsychologue);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) quizzes.add(mapResultSetToQuiz(rs));
            }
        }
        return quizzes;
    }

    public List<Quiz> getQuizByPsychologue(int id) throws SQLException { return getQuizParPsychologue(id); }

    public List<Quiz> getQuizParPatient(int idPatient) throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();
        String req = "SELECT * FROM quiz WHERE id_users = ? OR id_users = 0 ORDER BY date_creation DESC";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) quizzes.add(mapResultSetToQuiz(rs));
            }
        }
        return quizzes;
    }

    public List<Quiz> getQuizByPatient(int id) throws SQLException { return getQuizParPatient(id); }

    public List<Quiz> getQuizGlobaux() throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();
        String req = "SELECT * FROM quiz WHERE id_users = 0 ORDER BY date_creation DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) quizzes.add(mapResultSetToQuiz(rs));
        }
        return quizzes;
    }

    public String calculerEtSauvegarderScore(int idQuiz, int idPatient) throws SQLException {
        int scoreTotal = 0;
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT SUM(valeur) AS score_total FROM reponse WHERE id_quiz=? AND id_users=?")) {
            pst.setInt(1, idQuiz); pst.setInt(2, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) scoreTotal = rs.getInt("score_total");
            }
        }
        String niveau, conseils;
        if      (scoreTotal < 5)  { niveau = "faible"; conseils = "Tout va bien, continuez !"; }
        else if (scoreTotal < 10) { niveau = "moyen";  conseils = "Quelques exercices de relaxation peuvent aider."; }
        else                      { niveau = "eleve";  conseils = "Consultez votre psychologue."; }

        try (PreparedStatement pst = cnx.prepareStatement(
                "INSERT INTO historique_quiz (id_quiz, id_users, score_total, date_passage) VALUES (?, ?, ?, ?)")) {
            pst.setInt(1, idQuiz); pst.setInt(2, idPatient);
            pst.setInt(3, scoreTotal); pst.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pst.executeUpdate();
        }
        return "Score: " + scoreTotal + " | Niveau: " + niveau + " | Conseils: " + conseils;
    }

    public List<String> getHistoriquePatient(int idPatient) throws SQLException {
        List<String> historique = new ArrayList<>();
        String req = "SELECT h.score_total, h.date_passage, q.titre, "
                + "(SELECT COUNT(DISTINCT qst.id_question) * MAX(r.valeur) FROM question qst "
                + " JOIN reponse r ON r.id_question = qst.id_question WHERE qst.id_quiz = q.id_quiz) AS score_max "
                + "FROM historique_quiz h JOIN quiz q ON h.id_quiz = q.id_quiz "
                + "WHERE h.id_users = ? ORDER BY h.date_passage ASC";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int scoreMax = rs.getInt("score_max");
                    if (scoreMax <= 0) scoreMax = 6;
                    historique.add("Quiz: " + rs.getString("titre")
                            + " | Score: " + rs.getInt("score_total")
                            + " | Max: "   + scoreMax
                            + " | Date: "  + rs.getTimestamp("date_passage").toLocalDateTime());
                }
            }
        }
        return historique;
    }

    public Map<Integer, String> getTousLesPatients() throws SQLException {
        Map<Integer, String> patients = new LinkedHashMap<>();
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT id_users, nom, prenom FROM users WHERE LOWER(role)='patient'");
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                patients.put(rs.getInt("id_users"),
                        rs.getString("nom") + (rs.getString("prenom") != null ? " " + rs.getString("prenom") : ""));
            }
        }
        return patients;
    }

    private Quiz mapResultSetToQuiz(ResultSet rs) throws SQLException {
        Quiz quiz = new Quiz();
        quiz.setIdQuiz      (rs.getInt      ("id_quiz"));
        quiz.setIdUsers     (rs.getInt      ("id_users"));
        quiz.setCreePar     (rs.getInt      ("cree_par"));
        quiz.setTitre       (rs.getString   ("titre"));
        quiz.setDescription (rs.getString   ("description"));
        quiz.setTypeTest    (rs.getString   ("type_test"));
        quiz.setActif       (rs.getBoolean  ("actif"));
        quiz.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
        return quiz;
    }

    // ✅ Quiz globaux (id_users=0) + quiz assignés au patient
    public List<Quiz> getQuizDisponiblesPatient(int idPatient) throws SQLException {
        List<Quiz> liste = new ArrayList<>();
        String sql = "SELECT * FROM quiz WHERE actif = 1 AND (id_users = 0 OR id_users = ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idPatient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapResultSetToQuiz(rs));
                }
            }
        }
        return liste;
    }
}