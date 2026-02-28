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

    // ══════════════════════════════════════════════════════════════
    //  CRUD
    // ══════════════════════════════════════════════════════════════

    @Override
    public void add(Reponse reponse) throws SQLException {
        String req = "INSERT INTO reponse (id_quiz, id_question, id_users, texte_reponse, valeur, date_reponse) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt   (1, reponse.getIdQuiz());
            pst.setInt   (2, reponse.getIdQuestion());

            // id_users nullable : NULL si psychologue crée un choix
            if (reponse.getIdUsers() == null) {
                pst.setNull(3, Types.INTEGER);
            } else {
                pst.setInt (3, reponse.getIdUsers());
            }

            pst.setString   (4, reponse.getTexteReponse());
            pst.setInt      (5, reponse.getValeur());
            pst.setTimestamp(6, Timestamp.valueOf(
                    reponse.getDateReponse() != null ? reponse.getDateReponse() : LocalDateTime.now()
            ));

            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    reponse.setIdReponse(rs.getInt(1));
                }
            }
        }
        System.out.println("Réponse ajoutée ID = " + reponse.getIdReponse());
    }

    @Override
    public void update(Reponse reponse) throws SQLException {
        String req = "UPDATE reponse SET id_question=?, id_users=?, texte_reponse=?, valeur=? " +
                "WHERE id_reponse=?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, reponse.getIdQuestion());

            if (reponse.getIdUsers() == null) {
                pst.setNull(2, Types.INTEGER);
            } else {
                pst.setInt (2, reponse.getIdUsers());
            }

            pst.setString(3, reponse.getTexteReponse());
            pst.setInt   (4, reponse.getValeur());
            pst.setInt   (5, reponse.getIdReponse());

            pst.executeUpdate();
        }
        System.out.println("Réponse modifiée ID = " + reponse.getIdReponse());
    }

    @Override
    public void delete(Reponse reponse) throws SQLException {
        String req = "DELETE FROM reponse WHERE id_reponse=?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, reponse.getIdReponse());
            pst.executeUpdate();
        }
        System.out.println("Réponse supprimée ID = " + reponse.getIdReponse());
    }

    @Override
    public List<Reponse> getAll() throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        String req = "SELECT * FROM reponse";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                reponses.add(mapResultSetToReponse(rs));
            }
        }
        return reponses;
    }

    // ══════════════════════════════════════════════════════════════
    //  MÉTHODES MÉTIER
    // ══════════════════════════════════════════════════════════════

    // Récupérer les choix possibles d'un quiz (créés par le psychologue, id_users = NULL)
    public List<Reponse> getChoixParQuiz(int idQuiz) throws SQLException {
        List<Reponse> choix = new ArrayList<>();
        String req = "SELECT * FROM reponse WHERE id_quiz = ? AND id_users IS NULL";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idQuiz);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    choix.add(mapResultSetToReponse(rs));
                }
            }
        }
        return choix;
    }

    // Récupérer les choix possibles d'une question (id_users = NULL)
    public List<Reponse> getChoixParQuestion(int idQuestion) throws SQLException {
        List<Reponse> choix = new ArrayList<>();
        String req = "SELECT * FROM reponse WHERE id_question = ? AND id_users IS NULL";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idQuestion);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    choix.add(mapResultSetToReponse(rs));
                }
            }
        }
        return choix;
    }

    // Récupérer les réponses soumises par un patient pour un quiz
    public List<Reponse> getReponsesParQuiz(int idQuiz) throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        // ← CORRIGÉ : on filtre uniquement les réponses patients (id_users NOT NULL)
        String req = "SELECT * FROM reponse WHERE id_quiz = ? AND id_users IS NOT NULL";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idQuiz);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    reponses.add(mapResultSetToReponse(rs));
                }
            }
        }
        return reponses;
    }

    // Récupérer les réponses d'un patient spécifique pour un quiz
    public List<Reponse> getReponsesParPatientEtQuiz(int idPatient, int idQuiz) throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        // ← CORRIGÉ : filtre direct sur id_users dans reponse
        String req = "SELECT * FROM reponse WHERE id_users = ? AND id_quiz = ?";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPatient);
            pst.setInt(2, idQuiz);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    reponses.add(mapResultSetToReponse(rs));
                }
            }
        }
        return reponses;
    }

    // Récupérer toutes les réponses soumises par un patient (tous quiz confondus)
    public List<String> getDetailsReponsesPatient(int idPatient) throws SQLException {
        List<String> details = new ArrayList<>();

        String req = "SELECT q.titre AS titre_quiz, qu.texte_question, r.texte_reponse, r.valeur, r.date_reponse " +
                "FROM reponse r " +
                "JOIN quiz q ON r.id_quiz = q.id_quiz " +
                "JOIN question qu ON r.id_question = qu.id_question " +
                "WHERE r.id_users = ? " +
                "ORDER BY r.date_reponse ASC";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String ligne = "Quiz: " + rs.getString("titre_quiz") +
                            " | Question: " + rs.getString("texte_question") +
                            " | Réponse: " + rs.getString("texte_reponse") +
                            " | Valeur: " + rs.getInt("valeur");
                    details.add(ligne);
                }
            }
        }

        return details;
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPER PRIVÉ
    // ══════════════════════════════════════════════════════════════

    private Reponse mapResultSetToReponse(ResultSet rs) throws SQLException {
        Reponse r = new Reponse();
        r.setIdReponse  (rs.getInt   ("id_reponse"));
        r.setIdQuiz     (rs.getInt   ("id_quiz"));
        r.setIdQuestion (rs.getInt   ("id_question"));
        r.setTexteReponse(rs.getString("texte_reponse"));
        r.setValeur     (rs.getInt   ("valeur"));

        // id_users nullable
        int idUsers = rs.getInt("id_users");
        r.setIdUsers(rs.wasNull() ? null : idUsers);

        Timestamp ts = rs.getTimestamp("date_reponse");
        r.setDateReponse(ts != null ? ts.toLocalDateTime() : null);

        return r;
    }
    // Calculer le score en pourcentage pour un patient et un quiz
    public int calculerScorePourcent(int idPatient, int idQuiz) throws SQLException {
        List<Reponse> reponses = getReponsesParPatientEtQuiz(idPatient, idQuiz);
        if (reponses.isEmpty()) return 0;

        int scoreTotal = 0;
        int scoreMax   = 0;

        for (Reponse r : reponses) {
            scoreTotal += r.getValeur();       // score obtenu
            scoreMax   += 5; // ← exemple : chaque question vaut max 5 points
        }

        return (int)((scoreTotal * 100.0) / scoreMax);
    }

    // Générer le conseil en fonction du score %
    public String getConseil(int scorePourcent) {
        if (scorePourcent >= 80) return "Excellent, continuez comme ça !";
        if (scorePourcent >= 60) return "Bien, mais quelques améliorations sont possibles.";
        return "Travaillez davantage sur ce sujet.";
    }

    public List<Reponse> getReponsesParTypes(int idPatient, List<String> typesQuiz) throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        if (typesQuiz.isEmpty()) return reponses;

        // Génération dynamique des ? pour la clause IN
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < typesQuiz.size(); i++) {
            inClause.append("?");
            if (i < typesQuiz.size() - 1) inClause.append(",");
        }

        // Correction : remplacer type_quiz par type_test
        String req = "SELECT r.* FROM reponse r " +
                "JOIN quiz q ON r.id_quiz = q.id_quiz " +
                "WHERE r.id_users = ? AND q.type_test IN (" + inClause + ")";

        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idPatient);
            for (int i = 0; i < typesQuiz.size(); i++) {
                pst.setString(i + 2, typesQuiz.get(i)); // indices commencent à 2
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    reponses.add(mapResultSetToReponse(rs));
                }
            }
        }

        return reponses;
    }

    // ══════════════════════════════════════════════════════════════
    public Map<Integer, String> getPatientsParPsychologue(int idPsy) throws SQLException {
        Map<Integer, String> patients = new LinkedHashMap<>();

        String sql = """
        SELECT DISTINCT rv.id_patient,
                        u.nom,
                        u.prenom
        FROM rendez_vous rv
        JOIN users u ON u.id_users = rv.id_patient
        WHERE rv.id_psychologist = ?
        ORDER BY u.nom, u.prenom
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idPsy);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int    id     = rs.getInt("id_patient");
                    String nom    = rs.getString("nom")    != null ? rs.getString("nom")    : "";
                    String prenom = rs.getString("prenom") != null ? rs.getString("prenom") : "";
                    String nomComplet = (prenom + " " + nom).trim();
                    patients.put(id, nomComplet);
                }
            }
        }
        return patients;
    }

    // ══════════════════════════════════════════════════════════════
    // Récupère les détails des réponses d'un patient (existant)
    // ══════════════════════════════════════════════════════════════


    // ══════════════════════════════════════════════════════════════
    // Labels utilitaires (utilisés dans les cartes RDV si besoin)
    // ══════════════════════════════════════════════════════════════
    public static String labelStatut(String statut) {
        if (statut == null) return "";
        return switch (statut) {
            case "termine"  -> "✅ Terminé";
            case "en_cours" -> "🔄 En cours";
            default         -> statut;
        };
    }

    public static String labelConfirmation(String conf) {
        if (conf == null) return "";
        return switch (conf) {
            case "confirme"   -> "✅ Confirmé";
            case "annule"     -> "❌ Annulé";
            case "en_attente" -> "⏳ En attente";
            default           -> conf;
        };
    }

    public static String labelType(String type) {
        if (type == null) return "";
        return switch (type) {
            case "premiere_consultation" -> "1ère consultation";
            case "suivi"                 -> "Suivi";
            case "urgence"               -> "🚨 Urgence";
            default                      -> type;
        };
    }

    public static String couleurConfirmation(String conf) {
        if (conf == null) return "#94a3b8";
        return switch (conf) {
            case "confirme"   -> "#10B981";
            case "annule"     -> "#EF4444";
            case "en_attente" -> "#F59E0B";
            default           -> "#94a3b8";
        };
    }
    // ── Méthode manquante utilisée dans le test ──
    public List<Reponse> getReponsesParQuestionEtUser(int idQuestion, int idUser) throws SQLException {
        List<Reponse> list = new ArrayList<>();
        String req = "SELECT * FROM reponse WHERE id_question=? AND id_users=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idQuestion);
            pst.setInt(2, idUser);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Reponse r = new Reponse();
                    r.setIdReponse(rs.getInt("id_reponse"));
                    r.setIdQuiz(rs.getInt("id_quiz"));
                    r.setIdQuestion(rs.getInt("id_question"));
                    r.setIdUsers(rs.getInt("id_users"));
                    r.setTexteReponse(rs.getString("texte_reponse"));
                    r.setValeur(rs.getInt("valeur"));
                    list.add(r);
                }
            }
        }
        return list;
    }





}