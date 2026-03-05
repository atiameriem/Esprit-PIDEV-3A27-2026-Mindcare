package services;

import models.Reponse;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceReponse implements IService<Reponse> {

    private Connection cnx;

    public ServiceReponse() {
        try {
            cnx = MyDatabase.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int create(Reponse reponse) throws SQLException {
        String req = "INSERT INTO reponse (id_quiz, id_question, id_users, texte_reponse, valeur, date_reponse) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, reponse.getIdQuiz());
            pst.setInt(2, reponse.getIdQuestion());
            if (reponse.getIdUsers() == null) pst.setNull(3, Types.INTEGER);
            else pst.setInt(3, reponse.getIdUsers());
            pst.setString   (4, reponse.getTexteReponse());
            pst.setInt      (5, reponse.getValeur());
            pst.setTimestamp(6, Timestamp.valueOf(
                    reponse.getDateReponse() != null ? reponse.getDateReponse() : LocalDateTime.now()));
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    reponse.setIdReponse(rs.getInt(1));
                    System.out.println("Reponse creee ID = " + reponse.getIdReponse());
                    return reponse.getIdReponse();
                }
            }
        }
        return -1;
    }

    @Override
    public void update(Reponse reponse) throws SQLException {
        String req = "UPDATE reponse SET id_question=?, id_users=?, texte_reponse=?, valeur=? WHERE id_reponse=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, reponse.getIdQuestion());
            if (reponse.getIdUsers() == null) pst.setNull(2, Types.INTEGER);
            else pst.setInt(2, reponse.getIdUsers());
            pst.setString(3, reponse.getTexteReponse());
            pst.setInt   (4, reponse.getValeur());
            pst.setInt   (5, reponse.getIdReponse());
            pst.executeUpdate();
        }
        System.out.println("Reponse modifiee ID = " + reponse.getIdReponse());
    }

    @Override
    public void delete(int id) throws SQLException {
        String req = "DELETE FROM reponse WHERE id_reponse=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
        System.out.println("Reponse supprimee ID = " + id);
    }

    @Override
    public List<Reponse> read() throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM reponse")) {
            while (rs.next()) reponses.add(mapResultSetToReponse(rs));
        }
        return reponses;
    }

    // ── Methodes metier ───────────────────────────────────────────

    public List<Reponse> getChoixParQuiz(int idQuiz) throws SQLException {
        List<Reponse> choix = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM reponse WHERE id_quiz = ? AND id_users IS NULL")) {
            pst.setInt(1, idQuiz);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) choix.add(mapResultSetToReponse(rs));
            }
        }
        return choix;
    }

    public List<Reponse> getChoixParQuestion(int idQuestion) throws SQLException {
        List<Reponse> choix = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM reponse WHERE id_question = ? AND id_users IS NULL")) {
            pst.setInt(1, idQuestion);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) choix.add(mapResultSetToReponse(rs));
            }
        }
        return choix;
    }

    public List<Reponse> getReponsesParQuiz(int idQuiz) throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM reponse WHERE id_quiz = ? AND id_users IS NOT NULL")) {
            pst.setInt(1, idQuiz);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) reponses.add(mapResultSetToReponse(rs));
            }
        }
        return reponses;
    }

    public List<Reponse> getReponsesParPatientEtQuiz(int idPatient, int idQuiz) throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM reponse WHERE id_users = ? AND id_quiz = ?")) {
            pst.setInt(1, idPatient); pst.setInt(2, idQuiz);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) reponses.add(mapResultSetToReponse(rs));
            }
        }
        return reponses;
    }

    public List<String> getDetailsReponsesPatient(int idPatient) throws SQLException {
        List<String> details = new ArrayList<>();
        String req = "SELECT q.titre AS titre_quiz, qu.texte_question, r.texte_reponse, r.valeur "
                + "FROM reponse r JOIN quiz q ON r.id_quiz = q.id_quiz "
                + "JOIN question qu ON r.id_question = qu.id_question "
                + "WHERE r.id_users = ? ORDER BY r.date_reponse ASC";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    details.add("Quiz: " + rs.getString("titre_quiz")
                            + " | Question: " + rs.getString("texte_question")
                            + " | Reponse: "  + rs.getString("texte_reponse")
                            + " | Valeur: "   + rs.getInt("valeur"));
                }
            }
        }
        return details;
    }

    public int calculerScorePourcent(int idPatient, int idQuiz) throws SQLException {
        List<Reponse> reponses = getReponsesParPatientEtQuiz(idPatient, idQuiz);
        if (reponses.isEmpty()) return 0;
        int scoreTotal = 0, scoreMax = 0;
        for (Reponse r : reponses) { scoreTotal += r.getValeur(); scoreMax += 5; }
        return (int)((scoreTotal * 100.0) / scoreMax);
    }

    public String getConseil(int scorePourcent) {
        if (scorePourcent >= 80) return "Excellent, continuez !";
        if (scorePourcent >= 60) return "Bien, quelques ameliorations possibles.";
        return "Travaillez davantage sur ce sujet.";
    }

    public List<Reponse> getReponsesParTypes(int idPatient, List<String> typesQuiz) throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        if (typesQuiz.isEmpty()) return reponses;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < typesQuiz.size(); i++) { in.append("?"); if (i < typesQuiz.size()-1) in.append(","); }
        String req = "SELECT r.* FROM reponse r JOIN quiz q ON r.id_quiz = q.id_quiz "
                + "WHERE r.id_users = ? AND q.type_test IN (" + in + ")";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPatient);
            for (int i = 0; i < typesQuiz.size(); i++) pst.setString(i + 2, typesQuiz.get(i));
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) reponses.add(mapResultSetToReponse(rs));
            }
        }
        return reponses;
    }

    public Map<Integer, String> getPatientsParPsychologue(int idPsy) throws SQLException {
        Map<Integer, String> patients = new LinkedHashMap<>();
        String sql = "SELECT DISTINCT rv.id_patient, u.nom, u.prenom "
                + "FROM rendez_vous rv JOIN users u ON u.id_users = rv.id_patient "
                + "WHERE rv.id_psychologist = ? ORDER BY u.nom, u.prenom";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idPsy);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nom    = rs.getString("nom")    != null ? rs.getString("nom")    : "";
                    String prenom = rs.getString("prenom") != null ? rs.getString("prenom") : "";
                    patients.put(rs.getInt("id_patient"), (prenom + " " + nom).trim());
                }
            }
        }
        return patients;
    }

    public List<Reponse> getReponsesParQuestionEtUser(int idQuestion, int idUser) throws SQLException {
        List<Reponse> list = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM reponse WHERE id_question=? AND id_users=?")) {
            pst.setInt(1, idQuestion); pst.setInt(2, idUser);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToReponse(rs));
            }
        }
        return list;
    }

    public int getScoreMaxQuiz(String titreQuiz) throws SQLException {
        String sql = "SELECT COALESCE(SUM(max_val), 0) FROM ("
                + "SELECT q.id_question, MAX(r.valeur) AS max_val "
                + "FROM question q "
                + "JOIN reponse r  ON r.id_question = q.id_question AND r.id_users IS NULL "
                + "JOIN quiz    qz ON qz.id_quiz    = q.id_quiz "
                + "WHERE qz.titre = ? GROUP BY q.id_question) AS max_par_question";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, titreQuiz);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // Labels utilitaires statiques
    public static String labelStatut(String s)      { return s == null ? "" : switch(s) { case "termine" -> "Termine"; case "en_cours" -> "En cours"; default -> s; }; }
    public static String labelConfirmation(String s){ return s == null ? "" : switch(s) { case "confirme" -> "Confirme"; case "annule" -> "Annule"; case "en_attente" -> "En attente"; default -> s; }; }
    public static String labelType(String s)        { return s == null ? "" : switch(s) { case "premiere_consultation" -> "1ere consultation"; case "suivi" -> "Suivi"; case "urgence" -> "Urgence"; default -> s; }; }
    public static String couleurConfirmation(String s){ return s == null ? "#94a3b8" : switch(s) { case "confirme" -> "#10B981"; case "annule" -> "#EF4444"; case "en_attente" -> "#F59E0B"; default -> "#94a3b8"; }; }

    private Reponse mapResultSetToReponse(ResultSet rs) throws SQLException {
        Reponse r = new Reponse();
        r.setIdReponse  (rs.getInt   ("id_reponse"));
        r.setIdQuiz     (rs.getInt   ("id_quiz"));
        r.setIdQuestion (rs.getInt   ("id_question"));
        r.setTexteReponse(rs.getString("texte_reponse"));
        r.setValeur     (rs.getInt   ("valeur"));
        int idUsers = rs.getInt("id_users");
        r.setIdUsers(rs.wasNull() ? null : idUsers);
        Timestamp ts = rs.getTimestamp("date_reponse");
        r.setDateReponse(ts != null ? ts.toLocalDateTime() : null);
        return r;
    }
}