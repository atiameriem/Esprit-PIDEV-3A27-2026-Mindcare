package services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import utils.MyDatabase;

public class ServiceRappelQuiz {

    private final ServiceEmailQuiz serviceEmailQuiz = new ServiceEmailQuiz();

    // ════════════════════════════════════════════════════════
    // Vérifier tous les patients inactifs > 7 jours
    // ════════════════════════════════════════════════════════
    public void verifierEtEnvoyerRappels() {
        new Thread(() -> {
            try {
                List<PatientInactif> inactifs = getPatientInactifs(7);
                System.out.println("🔍 " + inactifs.size() + " patient(s) inactif(s) trouvé(s)");

                for (PatientInactif p : inactifs) {
                    String html = serviceEmailQuiz.templateRappelQuiz(p.nom, p.joursInactif);
                    boolean ok  = serviceEmailQuiz.envoyerEmail(p.email,
                            "🧠 MindCare — Votre suivi vous manque !", html);
                    if (ok) marquerRappelEnvoye(p.idUsers);
                }

            } catch (Exception e) {
                System.err.println("❌ Erreur rappels : " + e.getMessage());
            }
        }).start();
    }

    // ════════════════════════════════════════════════════════
    // Envoyer email résultat après quiz
    // ════════════════════════════════════════════════════════
    public void envoyerResultatParEmail(int idUsers, String titreQuiz,
                                        int score, int pourcentage, String conseil) {
        new Thread(() -> {
            try {
                String[] infos = getInfosPatient(idUsers);
                if (infos == null) return;

                String html = serviceEmailQuiz.templateResultatQuiz(
                        infos[0], titreQuiz, score, pourcentage, conseil
                );
                serviceEmailQuiz.envoyerEmail(infos[1],
                        "✅ Résultat de votre test — " + titreQuiz, html);

            } catch (Exception e) {
                System.err.println("❌ Erreur envoi résultat : " + e.getMessage());
            }
        }).start();
    }

    // ════════════════════════════════════════════════════════
    // Requêtes DB — connexion fraîche à chaque appel
    // ════════════════════════════════════════════════════════
    private List<PatientInactif> getPatientInactifs(int seuilJours) throws SQLException {
        List<PatientInactif> liste = new ArrayList<>();
        String sql = """
            SELECT u.id_users, u.nom, u.email,
                   DATEDIFF(NOW(), MAX(h.date_passage)) AS jours_inactif
            FROM users u
            LEFT JOIN historique_quiz h ON u.id_users = h.id_users
            WHERE u.role = 'patient'
            GROUP BY u.id_users, u.nom, u.email
            HAVING jours_inactif > ? OR jours_inactif IS NULL
            """;
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seuilJours);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                liste.add(new PatientInactif(
                        rs.getInt("id_users"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getInt("jours_inactif")
                ));
            }
        }
        return liste;
    }

    private String[] getInfosPatient(int idUsers) throws SQLException {
        String sql = "SELECT nom, email FROM users WHERE id_users = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsers);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{rs.getString("nom"), rs.getString("email")};
        }
        return null;
    }

    private void marquerRappelEnvoye(int idUsers) {
        System.out.println("📧 Rappel envoyé pour id_users=" + idUsers);
    }

    // Classe interne
    private static class PatientInactif {
        int idUsers, joursInactif;
        String nom, email;
        PatientInactif(int id, String nom, String email, int jours) {
            this.idUsers = id;
            this.nom     = nom;
            this.email   = email;
            this.joursInactif = jours;
        }
    }
}