package services;

import models.Quiz;
import models.Question;
import models.Reponse;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceQuiz implements IService<Quiz> {

    private Connection cnx;
    private ServiceQuestion serviceQuestion;

    public ServiceQuiz() {
        try {
            cnx = MyDatabase.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        serviceQuestion = new ServiceQuestion();
    }

    // ══════════════════════════════════════════════════════════════
    //  CRUD
    // ══════════════════════════════════════════════════════════════

    @Override
    public void add(Quiz quiz) throws SQLException {
        String req = "INSERT INTO quiz (id_users, cree_par, titre, description, type_test, actif, date_creation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt    (1, quiz.getIdUsers());   // 0 = global, >0 = patient spécifique
            pst.setInt    (2, quiz.getCreePar());
            pst.setString (3, quiz.getTitre());
            pst.setString (4, quiz.getDescription());
            pst.setString (5, quiz.getTypeTest());
            pst.setBoolean(6, quiz.isActif());
            pst.setTimestamp(7, Timestamp.valueOf(
                    quiz.getDateCreation() != null ? quiz.getDateCreation() : LocalDateTime.now()
            ));
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) quiz.setIdQuiz(rs.getInt(1));
            }
        }
        System.out.println("Quiz ajouté avec ID = " + quiz.getIdQuiz()
                + (quiz.getIdUsers() == 0 ? " [GLOBAL — tous les patients]" : " [Patient ID=" + quiz.getIdUsers() + "]"));
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
        System.out.println("Quiz modifié ID = " + quiz.getIdQuiz());
    }

    @Override
    public void delete(Quiz quiz) throws SQLException {
        String req = "DELETE FROM quiz WHERE id_quiz=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, quiz.getIdQuiz());
            pst.executeUpdate();
        }
        System.out.println("Quiz supprimé ID = " + quiz.getIdQuiz());
    }

    /**
     * Retourne TOUS les quiz (globaux + spécifiques à un patient).
     * Utilisé par le psychologue pour voir l'ensemble des quiz.
     */
    @Override
    public List<Quiz> getAll() throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();
        String req = "SELECT q.*, u.nom, u.prenom " +
                "FROM quiz q " +
                "JOIN users u ON q.cree_par = u.id_users " +
                "ORDER BY q.date_creation DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) quizzes.add(mapResultSetToQuiz(rs));
        }
        return quizzes;
    }

    // ══════════════════════════════════════════════════════════════
    //  MÉTHODES MÉTIER
    // ══════════════════════════════════════════════════════════════

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

    // ── Psychologue ───────────────────────────────────────────────
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

    // ✅ Alias pour PasserTestsController
    public List<Quiz> getQuizByPsychologue(int idPsychologue) throws SQLException {
        return getQuizParPsychologue(idPsychologue);
    }

    // ── Patient ───────────────────────────────────────────────────
    /**
     * Retourne les quiz visibles par un patient :
     *  - Les quiz globaux (id_users = 0) → pour tous les patients
     *  - Les quiz spécifiquement assignés à ce patient (id_users = idPatient)
     */
    public List<Quiz> getQuizParPatient(int idPatient) throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();
        String req = "SELECT * FROM quiz " +
                "WHERE id_users = ? OR id_users = 0 " +
                "ORDER BY date_creation DESC";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) quizzes.add(mapResultSetToQuiz(rs));
            }
        }
        return quizzes;
    }

    // ✅ Alias pour PasserTestsController
    public List<Quiz> getQuizByPatient(int idPatient) throws SQLException {
        return getQuizParPatient(idPatient);
    }

    /**
     * Retourne uniquement les quiz globaux (id_users = 0).
     */
    public List<Quiz> getQuizGlobaux() throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();
        String req = "SELECT * FROM quiz WHERE id_users = 0 ORDER BY date_creation DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) quizzes.add(mapResultSetToQuiz(rs));
        }
        return quizzes;
    }

    // ── Score & Historique ────────────────────────────────────────
    public String calculerEtSauvegarderScore(int idQuiz, int idPatient) throws SQLException {
        String req = "SELECT SUM(valeur) AS score_total FROM reponse WHERE id_quiz = ? AND id_users = ?";
        int scoreTotal = 0;
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idQuiz);
            pst.setInt(2, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) scoreTotal = rs.getInt("score_total");
            }
        }

        String niveau, conseils;
        if (scoreTotal < 5) {
            niveau   = "faible";
            conseils = "Tout va bien, continuez comme ça !";
        } else if (scoreTotal < 10) {
            niveau   = "moyen";
            conseils = "Quelques exercices de relaxation peuvent aider.";
        } else {
            niveau   = "élevé";
            conseils = "Consultez votre psychologue, des exercices réguliers sont recommandés.";
        }

        String reqHisto = "INSERT INTO historique_quiz (id_quiz, id_users, score_total, date_passage) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(reqHisto)) {
            pst.setInt      (1, idQuiz);
            pst.setInt      (2, idPatient);
            pst.setInt      (3, scoreTotal);
            pst.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pst.executeUpdate();
        }

        return "Score: " + scoreTotal + " | Niveau: " + niveau + " | Conseils: " + conseils;
    }

    public List<String> getHistoriquePatient(int idPatient) throws SQLException {
        List<String> historique = new ArrayList<>();
        String req =
                "SELECT h.score_total, h.date_passage, q.titre, " +
                        "(SELECT COUNT(DISTINCT qst.id_question) * MAX(r.valeur) " +
                        " FROM question qst " +
                        " JOIN reponse r ON r.id_question = qst.id_question " +
                        " WHERE qst.id_quiz = q.id_quiz) AS score_max " +
                        "FROM historique_quiz h " +
                        "JOIN quiz q ON h.id_quiz = q.id_quiz " +
                        "WHERE h.id_users = ? " +
                        "ORDER BY h.date_passage ASC";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int scoreMax = rs.getInt("score_max");
                    if (scoreMax <= 0) scoreMax = 6;

                    historique.add(
                            "Quiz: "   + rs.getString("titre") +
                                    " | Score: " + rs.getInt("score_total") +
                                    " | Max: "   + scoreMax +
                                    " | Date: "  + rs.getTimestamp("date_passage").toLocalDateTime()
                    );
                }
            }
        }
        return historique;
    }

    // ── Patients ──────────────────────────────────────────────────
    public Map<Integer, String> getTousLesPatients() throws SQLException {
        Map<Integer, String> patients = new LinkedHashMap<>();
        String req = "SELECT id_users, nom, prenom FROM users WHERE LOWER(role) = 'patient'";
        try (PreparedStatement pst = cnx.prepareStatement(req);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_users");
                String nomComplet = rs.getString("nom") +
                        (rs.getString("prenom") != null ? " " + rs.getString("prenom") : "");
                patients.put(id, nomComplet);
            }
        }
        return patients;
    }

    // ── Helpers privés ────────────────────────────────────────────
    private Quiz mapResultSetToQuiz(ResultSet rs) throws SQLException {
        Quiz quiz = new Quiz();
        quiz.setIdQuiz      (rs.getInt      ("id_quiz"));
        quiz.setIdUsers     (rs.getInt      ("id_users"));   // 0 = global
        quiz.setCreePar     (rs.getInt      ("cree_par"));
        quiz.setTitre       (rs.getString   ("titre"));
        quiz.setDescription (rs.getString   ("description"));
        quiz.setTypeTest    (rs.getString   ("type_test"));
        quiz.setActif       (rs.getBoolean  ("actif"));
        quiz.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
        return quiz;
    }

    private boolean userExiste(int idUser) throws SQLException {
        String req = "SELECT id_users FROM users WHERE id_users = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idUser);
            try (ResultSet rs = pst.executeQuery()) { return rs.next(); }
        }
    }

    private boolean estPsychologue(int idUser) throws SQLException {
        String req = "SELECT role FROM users WHERE id_users = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idUser);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return "PSYCHOLOGUE".equalsIgnoreCase(rs.getString("role"));
            }
        }
        return false;
    }
}