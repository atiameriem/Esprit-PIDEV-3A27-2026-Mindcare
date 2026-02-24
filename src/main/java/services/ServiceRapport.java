package services;

import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ServiceRapport {

    private final ServiceEmail serviceEmail = new ServiceEmail();

    // ══════════════════════════════════════════════════════════════
    // Envoyer rapports mensuels à tous les patients actifs
    // ══════════════════════════════════════════════════════════════
    public void envoyerRapportsMensuels() {
        System.out.println("📄 Génération des rapports mensuels...");
        try {
            List<PatientStats> patients = getStatsPatientsMoisPrecedent();
            System.out.println("👥 " + patients.size()
                    + " patient(s) à traiter");

            for (PatientStats p : patients) {
                if (rapportDejaEnvoye(p.idPatient)) {
                    System.out.println("⏭️ Rapport déjà envoyé pour "
                            + p.nomComplet);
                    continue;
                }
                try {
                    String html = templateRapportMensuel(p);
                    serviceEmail.envoyerEmail(
                            p.email,
                            "📊 Votre rapport mensuel MindCare — "
                                    + LocalDate.now().format(
                                    DateTimeFormatter.ofPattern("MMMM yyyy")),
                            html
                    );
                    marquerRapportEnvoye(p.idPatient);
                    System.out.println("✅ Rapport envoyé à " + p.email);
                } catch (Exception e) {
                    System.err.println("❌ Erreur rapport pour "
                            + p.nomComplet + " : " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur globale rapports : "
                    + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Récupérer stats patients du mois précédent
    // ══════════════════════════════════════════════════════════════
    private List<PatientStats> getStatsPatientsMoisPrecedent()
            throws SQLException {

        List<PatientStats> liste = new ArrayList<>();
        String sql = "SELECT "
                + "  u.id, "
                + "  CONCAT(u.nom, ' ', u.prenom) AS nom_complet, "
                + "  u.email, "
                + "  COUNT(hq.id) AS nb_tests, "
                + "  AVG(hq.score_total) AS score_moyen, "
                + "  MIN(hq.score_total) AS score_min, "
                + "  MAX(hq.score_total) AS score_max "
                + "FROM users u "
                + "JOIN historique_quiz hq ON hq.id_patient = u.id "
                + "WHERE u.role = 'USER' "
                + "  AND hq.alerte_envoyee = 0 "
                + "  AND MONTH(hq.date_passage) = MONTH(NOW() - INTERVAL 1 MONTH) "
                + "  AND YEAR(hq.date_passage)  = YEAR(NOW()  - INTERVAL 1 MONTH) "
                + "GROUP BY u.id, u.nom, u.prenom, u.email "
                + "HAVING COUNT(hq.id) >= 1";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                PatientStats p = new PatientStats();
                p.idPatient  = rs.getInt("id");
                p.nomComplet = rs.getString("nom_complet");
                p.email      = rs.getString("email");
                p.nbTests    = rs.getInt("nb_tests");
                p.scoreMoyen = rs.getDouble("score_moyen");
                p.scoreMin   = rs.getInt("score_min");
                p.scoreMax   = rs.getInt("score_max");
                p.tendance   = calculerTendanceTexte(p.scoreMoyen);
                p.badge      = calculerBadge(p.nbTests, p.scoreMoyen);
                p.coins      = p.nbTests * 150
                        + (int)(p.scoreMoyen * 10);
                liste.add(p);
            }
        }
        return liste;
    }

    // ══════════════════════════════════════════════════════════════
    // Anti-spam — vérifier si rapport déjà envoyé
    // ══════════════════════════════════════════════════════════════
    private boolean rapportDejaEnvoye(int idPatient) {
        String sql = "SELECT COUNT(*) FROM historique_quiz "
                + "WHERE id_patient = ? "
                + "  AND alerte_envoyee = 1 "
                + "  AND MONTH(date_passage) = "
                + "      MONTH(NOW() - INTERVAL 1 MONTH) "
                + "  AND YEAR(date_passage) = "
                + "      YEAR(NOW()  - INTERVAL 1 MONTH)";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPatient);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Marquer rapport comme envoyé
    // ══════════════════════════════════════════════════════════════
    private void marquerRapportEnvoye(int idPatient) {
        String sql = "UPDATE historique_quiz "
                + "SET alerte_envoyee = 1 "
                + "WHERE id_patient = ? "
                + "  AND alerte_envoyee = 0 "
                + "  AND MONTH(date_passage) = "
                + "      MONTH(NOW() - INTERVAL 1 MONTH) "
                + "  AND YEAR(date_passage) = "
                + "      YEAR(NOW()  - INTERVAL 1 MONTH)";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPatient);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ marquerRapport : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Calculs
    // ══════════════════════════════════════════════════════════════
    private String calculerTendanceTexte(double scoreMoyen) {
        if      (scoreMoyen >= 20) return "📈 En progression";
        else if (scoreMoyen >= 10) return "➡️ Stable";
        else                       return "📉 À améliorer";
    }

    private String calculerBadge(int nbTests, double scoreMoyen) {
        if      (nbTests >= 10 && scoreMoyen >= 20) return "🏆 Champion";
        else if (nbTests >= 5  && scoreMoyen >= 12) return "⭐ Régulier";
        else if (nbTests >= 1)                      return "🌱 Débutant";
        else                                        return "🆕 Nouveau";
    }

    private String getNiveauLabel(double score) {
        if      (score >= 20) return "Excellent";
        else if (score >= 10) return "Bien";
        else                  return "À améliorer";
    }

    private String getCouleurNiveau(double score) {
        if      (score >= 20) return "#10B981";
        else if (score >= 10) return "#F59E0B";
        else                  return "#EF4444";
    }

    // ══════════════════════════════════════════════════════════════
    // Template HTML rapport mensuel
    // ══════════════════════════════════════════════════════════════
    private String templateRapportMensuel(PatientStats p) {
        String mois = LocalDate.now().minusMonths(1)
                .format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        String couleurNiveau = getCouleurNiveau(p.scoreMoyen);
        String niveauLabel   = getNiveauLabel(p.scoreMoyen);
        int    scorePct      = (int) Math.min(100,
                (p.scoreMoyen * 100.0) / 30);

        return "<!DOCTYPE html><html lang='fr'><head>"
                + "<meta charset='UTF-8'/>"
                + "<style>"
                + "body{margin:0;padding:0;background:#F0F4F8;"
                + "font-family:Arial,sans-serif;}"
                + ".container{max-width:600px;margin:30px auto;"
                + "background:white;border-radius:20px;"
                + "overflow:hidden;"
                + "box-shadow:0 10px 40px rgba(0,0,0,0.1);}"
                + ".header{background:linear-gradient("
                + "135deg,#2c4a6e,#7C3AED);"
                + "padding:40px 30px;text-align:center;color:white;}"
                + ".header h1{margin:0;font-size:28px;font-weight:900;}"
                + ".header p{margin:8px 0 0;opacity:.85;font-size:15px;}"
                + ".badge-container{text-align:center;"
                + "padding:24px 0 0;}"
                + ".badge{font-size:42px;}"
                + ".badge-label{font-size:18px;font-weight:700;"
                + "color:#7C3AED;margin-top:6px;}"
                + ".stats-grid{display:grid;"
                + "grid-template-columns:1fr 1fr;"
                + "gap:16px;padding:24px 30px;}"
                + ".stat-card{background:#F8F9FF;"
                + "border-radius:14px;padding:18px;"
                + "text-align:center;"
                + "border:1.5px solid #E5E7EB;}"
                + ".stat-label{font-size:11px;color:#9CA3AF;"
                + "font-weight:700;text-transform:uppercase;"
                + "letter-spacing:.5px;margin-bottom:6px;}"
                + ".stat-value{font-size:24px;font-weight:900;"
                + "color:#1F2937;}"
                + ".stat-sub{font-size:12px;color:#6B7280;margin-top:4px;}"
                + ".niveau-bar-container{padding:0 30px 24px;}"
                + ".niveau-bar-bg{background:#E5E7EB;"
                + "border-radius:10px;height:12px;overflow:hidden;}"
                + ".niveau-bar-fill{height:100%;border-radius:10px;"
                + "background:" + couleurNiveau + ";width:" + scorePct + "%;}"
                + ".tendance-box{margin:0 30px 24px;"
                + "background:" + couleurNiveau + "18;"
                + "border:1.5px solid " + couleurNiveau + ";"
                + "border-radius:14px;padding:16px 20px;"
                + "display:flex;align-items:center;gap:12px;}"
                + ".tendance-icon{font-size:24px;}"
                + ".tendance-text{font-size:14px;font-weight:700;"
                + "color:#1F2937;}"
                + ".coins-box{margin:0 30px 24px;"
                + "background:linear-gradient(135deg,"
                + "rgba(124,58,237,.08),rgba(44,74,110,.08));"
                + "border-radius:14px;padding:18px 20px;"
                + "text-align:center;}"
                + ".coins-title{font-size:12px;color:#9CA3AF;"
                + "font-weight:700;margin-bottom:6px;}"
                + ".coins-value{font-size:28px;font-weight:900;"
                + "color:#7C3AED;}"
                + ".footer{background:#F8F9FF;"
                + "padding:24px 30px;text-align:center;"
                + "border-top:1px solid #E5E7EB;}"
                + ".footer p{margin:0;font-size:12px;"
                + "color:#9CA3AF;line-height:1.6;}"
                + "</style></head><body>"
                + "<div class='container'>"

                // ── Header ──────────────────────────────────────────
                + "<div class='header'>"
                + "<h1>📊 Rapport Mensuel MindCare</h1>"
                + "<p>Votre bilan de santé mentale — " + mois + "</p>"
                + "</div>"

                // ── Badge ────────────────────────────────────────────
                + "<div class='badge-container'>"
                + "<div class='badge'>"
                + p.badge.split(" ")[0] + "</div>"
                + "<div class='badge-label'>" + p.badge + "</div>"
                + "<p style='font-size:13px;color:#6B7280;"
                + "margin:4px 0 0;'>Bonjour " + p.nomComplet + " !</p>"
                + "</div>"

                // ── Stats grid ───────────────────────────────────────
                + "<div class='stats-grid'>"

                + "<div class='stat-card'>"
                + "<div class='stat-label'>Tests complétés</div>"
                + "<div class='stat-value'>" + p.nbTests + "</div>"
                + "<div class='stat-sub'>ce mois-ci</div>"
                + "</div>"

                + "<div class='stat-card'>"
                + "<div class='stat-label'>Score moyen</div>"
                + "<div class='stat-value' style='color:"
                + couleurNiveau + ";'>"
                + String.format("%.1f", p.scoreMoyen) + "</div>"
                + "<div class='stat-sub'>" + niveauLabel + "</div>"
                + "</div>"

                + "<div class='stat-card'>"
                + "<div class='stat-label'>Score minimum</div>"
                + "<div class='stat-value' style='color:#EF4444;'>"
                + p.scoreMin + "</div>"
                + "<div class='stat-sub'>point bas du mois</div>"
                + "</div>"

                + "<div class='stat-card'>"
                + "<div class='stat-label'>Score maximum</div>"
                + "<div class='stat-value' style='color:#10B981;'>"
                + p.scoreMax + "</div>"
                + "<div class='stat-sub'>meilleure session</div>"
                + "</div>"

                + "</div>"

                // ── Barre niveau ──────────────────────────────────────
                + "<div class='niveau-bar-container'>"
                + "<p style='font-size:12px;color:#6B7280;"
                + "font-weight:700;margin:0 0 8px;'>"
                + "Niveau global : " + niveauLabel + " ("
                + scorePct + "%)</p>"
                + "<div class='niveau-bar-bg'>"
                + "<div class='niveau-bar-fill'></div>"
                + "</div></div>"

                // ── Tendance ──────────────────────────────────────────
                + "<div class='tendance-box'>"
                + "<div class='tendance-icon'>"
                + p.tendance.split(" ")[0] + "</div>"
                + "<div class='tendance-text'>" + p.tendance + "</div>"
                + "</div>"

                // ── Coins ─────────────────────────────────────────────
                + "<div class='coins-box'>"
                + "<div class='coins-title'>🏆 COINS GAGNÉS CE MOIS</div>"
                + "<div class='coins-value'>"
                + p.coins + " coins</div>"
                + "</div>"

                // ── Footer ────────────────────────────────────────────
                + "<div class='footer'>"
                + "<p>Ce rapport est généré automatiquement par MindCare.<br/>"
                + "Pour toute question, contactez votre psychologue.<br/>"
                + "<strong style='color:#7C3AED;'>MindCare</strong>"
                + " — Votre santé mentale, notre priorité. 💜</p>"
                + "</div>"

                + "</div></body></html>";
    }

    // ══════════════════════════════════════════════════════════════
    // Classe interne stats patient
    // ══════════════════════════════════════════════════════════════
    private static class PatientStats {
        int    idPatient;
        String nomComplet;
        String email;
        int    nbTests;
        double scoreMoyen;
        int    scoreMin;
        int    scoreMax;
        String tendance;
        String badge;
        int    coins;
    }
}