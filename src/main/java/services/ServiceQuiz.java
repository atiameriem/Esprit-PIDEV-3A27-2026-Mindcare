package services;

import models.Quiz;
import models.Question;
import models.Reponse;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceQuiz implements IService<Quiz> {

    private Connection cnx;
    private ServiceQuestion serviceQuestion;

    public ServiceQuiz() {
        cnx = MyDatabase.getInstance().getConnection();
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
            pst.setInt    (1, quiz.getIdUsers());
            pst.setInt    (2, quiz.getCreePar());
            pst.setString (3, quiz.getTitre());
            pst.setString (4, quiz.getDescription());
            pst.setString (5, quiz.getTypeTest());
            pst.setBoolean(6, quiz.isActif());
            pst.setTimestamp(7, Timestamp.valueOf(
                    quiz.getDateCreation() != null ? quiz.getDateCreation() : LocalDateTime.now()
            ));

            pst.executeUpdate();

            // Récupérer l'ID généré
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    quiz.setIdQuiz(rs.getInt(1));
                }
            }
        }
        System.out.println("Quiz ajouté avec ID = " + quiz.getIdQuiz());
    }

    @Override
    public void update(Quiz quiz) throws SQLException {
        String req = "UPDATE quiz SET titre=?, description=?, type_test=?, actif=? " +
                "WHERE id_quiz=?";

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
        // Les questions et réponses seront supprimées en CASCADE
        String req = "DELETE FROM quiz WHERE id_quiz=?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, quiz.getIdQuiz());
            pst.executeUpdate();
        }
        System.out.println("Quiz supprimé ID = " + quiz.getIdQuiz());
    }

    @Override
    public List<Quiz> getAll() throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();

        String req = "SELECT q.*, u.nom, u.prenom " +
                "FROM quiz q " +
                "JOIN users u ON q.cree_par = u.id_users";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                quizzes.add(mapResultSetToQuiz(rs));
            }
        }
        return quizzes;
    }

    // ══════════════════════════════════════════════════════════════
    //  MÉTHODES MÉTIER
    // ══════════════════════════════════════════════════════════════

    // Récupérer un quiz par son ID (avec ses questions)
    public Quiz getQuizById(int idQuiz) throws SQLException {
        String req = "SELECT * FROM quiz WHERE id_quiz=?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idQuiz);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Quiz quiz = mapResultSetToQuiz(rs);
                    // Charger les questions liées
                    quiz.setQuestions(serviceQuestion.getQuestionsByQuiz(idQuiz));
                    return quiz;
                }
            }
        }
        return null;
    }

    // Récupérer tous les quiz créés par un psychologue
    public List<Quiz> getQuizParPsychologue(int idPsychologue) throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();

        String req = "SELECT * FROM quiz WHERE cree_par = ?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPsychologue);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    quizzes.add(mapResultSetToQuiz(rs));
                }
            }
        }
        return quizzes;
    }

    // Récupérer tous les quiz assignés à un patient
    public List<Quiz> getQuizParPatient(int idPatient) throws SQLException {
        List<Quiz> quizzes = new ArrayList<>();

        String req = "SELECT * FROM quiz WHERE id_users = ?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPatient);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    quizzes.add(mapResultSetToQuiz(rs));
                }
            }
        }
        return quizzes;
    }

    // Calculer le score d'un patient pour un quiz
    // et l'enregistrer dans historique_quiz
    public String calculerEtSauvegarderScore(int idQuiz, int idPatient) throws SQLException {

        // 1. Récupérer les réponses soumises par le patient
        String req = "SELECT SUM(valeur) AS score_total " +
                "FROM reponse " +
                "WHERE id_quiz = ? AND id_users = ?";

        int scoreTotal = 0;

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idQuiz);
            pst.setInt(2, idPatient);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    scoreTotal = rs.getInt("score_total");
                }
            }
        }

        // 2. Déterminer le niveau
        String niveau;
        String conseils;

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

        // 3. Sauvegarder dans historique_quiz
        String reqHisto = "INSERT INTO historique_quiz (id_quiz, id_users, score_total, date_passage) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(reqHisto)) {
            pst.setInt      (1, idQuiz);
            pst.setInt      (2, idPatient);
            pst.setInt      (3, scoreTotal);
            pst.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pst.executeUpdate();
        }

        return "Score: " + scoreTotal + " | Niveau: " + niveau + " | Conseils: " + conseils;
    }

    // Historique des passages d'un patient (depuis historique_quiz)
    public List<String> getHistoriquePatient(int idPatient) throws SQLException {
        List<String> historique = new ArrayList<>();

        String req = "SELECT h.score_total, h.date_passage, q.titre " +
                "FROM historique_quiz h " +
                "JOIN quiz q ON h.id_quiz = q.id_quiz " +
                "WHERE h.id_users = ? " +
                "ORDER BY h.date_passage ASC";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPatient);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String ligne = "Quiz: " + rs.getString("titre") +
                            " | Score: " + rs.getInt("score_total") +
                            " | Date: " + rs.getTimestamp("date_passage").toLocalDateTime();
                    historique.add(ligne);
                }
            }
        }
        return historique;
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPER PRIVÉ
    // ══════════════════════════════════════════════════════════════

    // Évite la duplication du mapping ResultSet → Quiz
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
    private boolean userExiste(int idUser) throws SQLException {
        String req = "SELECT id_users FROM users WHERE id_users = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idUser);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean estPsychologue(int idUser) throws SQLException {
        String req = "SELECT role FROM users WHERE id_users = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idUser);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return "PSYCHOLOGUE".equalsIgnoreCase(rs.getString("role"));
                }
            }
        }
        return false;
    }
    // Récupérer tous les patients (id + nom complet)
    public Map<Integer, String> getTousLesPatients() throws SQLException {
        Map<Integer, String> patients = new HashMap<>();

        String req = "SELECT id_users, nom, prenom FROM users WHERE role = 'PATIENT'";
        try (PreparedStatement pst = cnx.prepareStatement(req);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_users");
                String nomComplet = rs.getString("nom") + " " + rs.getString("prenom");
                patients.put(id, nomComplet);
            }
        }

        return patients;
    }


}