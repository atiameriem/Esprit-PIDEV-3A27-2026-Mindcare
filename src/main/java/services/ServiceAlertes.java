package services;

import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ServiceAlertes {

    private final ServiceEmail serviceEmail = new ServiceEmail();

    // ══════════════════════════════════════════════════════════════
    // Vérifie scores critiques → email psychologue
    // ══════════════════════════════════════════════════════════════
    public void verifierScoresCritiques() {
        String sql = "SELECT h.id_users, u.nom, u.prenom, "
                + "AVG(h.score_total) as score_moyen, "
                + "COUNT(h.id_historique) as nb_tests, "
                + "ps.email as email_psy, "
                + "ps.nom as nom_psy, ps.prenom as prenom_psy "
                + "FROM historique_quiz h "
                + "JOIN users u  ON u.id_users = h.id_users "
                + "JOIN users ps ON ps.role = 'psychologue' "
                + "WHERE u.role = 'patient' "
                + "AND h.date_passage >= DATE_SUB(NOW(), INTERVAL 7 DAY) "
                + "AND h.alerte_envoyee = 0 "
                + "GROUP BY h.id_users, u.nom, u.prenom, "
                + "ps.email, ps.nom, ps.prenom "
                + "HAVING score_moyen < 5";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int    idPatient  = rs.getInt("id_users");
                String nomPatient = rs.getString("prenom")
                        + " " + rs.getString("nom");
                double scoreMoyen = rs.getDouble("score_moyen");
                int    nbTests    = rs.getInt("nb_tests");
                String emailPsy   = rs.getString("email_psy");
                String nomPsy     = rs.getString("prenom_psy")
                        + " " + rs.getString("nom_psy");

                envoyerAlertePsychologue(
                        emailPsy, nomPsy,
                        nomPatient, scoreMoyen, nbTests
                );
                marquerAlerteEnvoyee(idPatient);
                System.out.println("🚨 Alerte → " + nomPatient);
            }

        } catch (SQLException e) {
            System.err.println("❌ Alertes : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Marquer alerte_envoyee = 1 dans historique_quiz
    // ══════════════════════════════════════════════════════════════
    private void marquerAlerteEnvoyee(int idPatient) {
        String sql = "UPDATE historique_quiz SET alerte_envoyee = 1 "
                + "WHERE id_users = ? "
                + "AND date_passage >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPatient);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Marquer alerte : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Email alerte psychologue
    // ══════════════════════════════════════════════════════════════
    private void envoyerAlertePsychologue(String emailPsy, String nomPsy,
                                          String nomPatient,
                                          double scoreMoyen, int nbTests) {
        String niveau = scoreMoyen < 3 ? "⛔ TRÈS ÉLEVÉ" : "🔴 ÉLEVÉ";

        String html = "<!DOCTYPE html><html><body style='margin:0;padding:0;"
                + "background:#f8fafc;font-family:Segoe UI,Arial,sans-serif;'>"
                + "<div style='max-width:580px;margin:30px auto;background:white;"
                + "border-radius:20px;overflow:hidden;"
                + "box-shadow:0 4px 24px rgba(0,0,0,0.08);'>"

                + "<div style='background:linear-gradient(135deg,#DC2626,#EF4444);"
                + "padding:32px;text-align:center;'>"
                + "<div style='font-size:48px;'>🚨</div>"
                + "<h1 style='color:white;margin:8px 0 0;font-size:22px;"
                + "font-weight:900;'>Alerte Score Critique</h1>"
                + "</div>"

                + "<div style='padding:32px;'>"
                + "<p style='font-size:15px;color:#374151;'>Bonjour Dr. "
                + "<strong>" + nomPsy + "</strong>,</p>"
                + "<p style='font-size:14px;color:#6B7280;line-height:1.7;'>"
                + "Score critique détecté pour un de vos patients. "
                + "Une attention immédiate est recommandée.</p>"

                + "<div style='background:#FEF2F2;border:2px solid #FECACA;"
                + "border-radius:16px;padding:24px;margin:20px 0;'>"
                + "<h3 style='margin:0 0 16px;color:#DC2626;'>👤 "
                + nomPatient + "</h3>"

                + "<div style='display:flex;gap:12px;'>"
                + "<div style='flex:1;background:white;border-radius:10px;"
                + "padding:12px;text-align:center;'>"
                + "<div style='font-size:28px;font-weight:900;color:#DC2626;'>"
                + String.format("%.1f", scoreMoyen) + "</div>"
                + "<div style='font-size:11px;color:#9CA3AF;'>Score moyen</div>"
                + "</div>"
                + "<div style='flex:1;background:white;border-radius:10px;"
                + "padding:12px;text-align:center;'>"
                + "<div style='font-size:28px;font-weight:900;color:#F59E0B;'>"
                + nbTests + "</div>"
                + "<div style='font-size:11px;color:#9CA3AF;'>Tests / semaine</div>"
                + "</div></div>"

                + "<div style='margin-top:12px;padding:10px;background:#DC2626;"
                + "border-radius:10px;text-align:center;color:white;"
                + "font-weight:700;'>Niveau de risque : " + niveau + "</div>"
                + "</div>"

                + "<div style='background:#F0FDF4;border-left:4px solid #22C55E;"
                + "border-radius:8px;padding:16px;'>"
                + "<p style='margin:0;font-size:13px;color:#15803D;font-weight:600;'>"
                + "💡 Contactez ce patient dans les 24h et envisagez "
                + "une consultation rapprochée.</p>"
                + "</div></div>"

                + "<div style='background:#F9FAFB;padding:16px;text-align:center;"
                + "border-top:1px solid #F3F4F6;'>"
                + "<p style='font-size:12px;color:#9CA3AF;margin:0;'>"
                + "MindCare — Système d'alerte automatique 🧠</p>"
                + "</div></div></body></html>";

        serviceEmail.envoyerEmail(
                emailPsy,
                "🚨 MindCare — Score critique : " + nomPatient,
                html
        );
    }
}