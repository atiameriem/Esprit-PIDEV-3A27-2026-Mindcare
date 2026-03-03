package services;

import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * "AI" (règles + analyse) :
 * - utilise plusieurs quiz (dernier N) et calcule score moyen pondéré + tendance
 * - analyse le motif (mots-clés) pour déterminer le besoin (stress, dépression, sommeil...)
 * - propose un type_session + justification (pourquoi)
 *
 * Barème score_total (0..100) :
 *   0-20 = léger, 21-59 = normal, 60-100 = élevé
 */
public class AiSessionRecommenderService {

    private final Connection cnx;

    public AiSessionRecommenderService() {
        this.cnx = MyDatabase.getInstance().getConnection();
    }

    public AiSessionRecommenderService(Connection cnx) {
        this.cnx = cnx;
    }

    public static class QuizInfo {
        public final int idHistorique;
        public final int scoreTotal;

        public QuizInfo(int idHistorique, int scoreTotal) {
            this.idHistorique = idHistorique;
            this.scoreTotal = scoreTotal;
        }
    }

    public static class QuizEntry {
        public final int idHistorique;
        public final int scoreTotal;
        public final Instant datePassage;

        public QuizEntry(int idHistorique, int scoreTotal, Instant datePassage) {
            this.idHistorique = idHistorique;
            this.scoreTotal = scoreTotal;
            this.datePassage = datePassage;
        }
    }

    
    public static class QuizStats {
        public final int count;
        public final double avgScore;   // 0..100
        public final int sumScore;      // somme brute des score_total
        public final int minScore;
        public final int maxScore;

        public QuizStats(int count, double avgScore, int sumScore, int minScore, int maxScore) {
            this.count = count;
            this.avgScore = avgScore;
            this.sumScore = sumScore;
            this.minScore = minScore;
            this.maxScore = maxScore;
        }
    }

public static class RecommendationResult {
        public final String recommended;
        public final String alternative;
        public final double confidence; // 0..1
        public final int scoreUsed; // score "analysé" (moyenne pondérée arrondie)
        public final String severity; // LEGER / NORMAL / ELEVE
        public final String motifCategory;
        public final String why;
        public final Integer idHistoriqueUtilise; // quiz le plus récent (pour FK)

        public RecommendationResult(String recommended,
                                    String alternative,
                                    double confidence,
                                    int scoreUsed,
                                    String severity,
                                    String motifCategory,
                                    String why,
                                    Integer idHistoriqueUtilise) {
            this.recommended = recommended;
            this.alternative = alternative;
            this.confidence = confidence;
            this.scoreUsed = scoreUsed;
            this.severity = severity;
            this.motifCategory = motifCategory;
            this.why = why;
            this.idHistoriqueUtilise = idHistoriqueUtilise;
        }
    }

    /** Dernier quiz du patient (par date_passage puis id_historique). Retourne null si aucun quiz. */
    public QuizInfo getLatestQuizForUser(int userId) throws SQLException {
        String sql = "SELECT id_historique, score_total FROM historique_quiz " +
                "WHERE id_users=? ORDER BY date_passage DESC, id_historique DESC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new QuizInfo(rs.getInt("id_historique"), rs.getInt("score_total"));
                }
            }
        }
        return null;
    }

    /** Récupère les N derniers quiz du patient (le plus récent d'abord). */
    public List<QuizEntry> getRecentQuizzesForUser(int userId, int limit) throws SQLException {
        int lim = Math.max(1, Math.min(limit, 20));
        String sql = "SELECT id_historique, score_total, date_passage FROM historique_quiz " +
                "WHERE id_users=? ORDER BY date_passage DESC, id_historique DESC LIMIT " + lim;
        List<QuizEntry> out = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("date_passage");
                    Instant dt = (ts == null) ? null : ts.toInstant();
                    out.add(new QuizEntry(
                            rs.getInt("id_historique"),
                            rs.getInt("score_total"),
                            dt
                    ));
                }
            }
        }
        return out;
    }

    /** Statistiques globales (tous les quiz) pour un patient. */
    public QuizStats getQuizStatsForUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) AS c, AVG(score_total) AS a, SUM(score_total) AS s, " +
                "MIN(score_total) AS mi, MAX(score_total) AS ma " +
                "FROM historique_quiz WHERE id_users=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int c = rs.getInt("c");
                    double a = rs.getDouble("a");
                    int s = rs.getInt("s");
                    int mi = rs.getInt("mi");
                    int ma = rs.getInt("ma");
                    return new QuizStats(c, a, s, mi, ma);
                }
            }
        }
        return new QuizStats(0, 0.0, 0, 0, 0);
    }


    /**
     * Analyse + recommandation basée sur score (0..100) et motif.
     * Utilise plusieurs quiz (jusqu'à 5) : moyenne pondérée + tendance.
     */
    public RecommendationResult analyzeAndRecommend(int userId, String motif) throws SQLException {
        List<QuizEntry> quizzes = getRecentQuizzesForUser(userId, 5);
        if (quizzes.isEmpty()) {
            return new RecommendationResult(
                    null, null, 0.0, -1, "N/A", "N/A",
                    "Aucun quiz trouvé pour ce patient.",
                    null
            );
        }

        // Score analysé = moyenne de TOUS les quiz du patient (0..100)
        QuizStats stats = getQuizStatsForUser(userId);
        if (stats.count <= 0) {
            return new RecommendationResult(
                    null, null, 0.0, -1, "N/A", "N/A",
                    "Aucun quiz trouvé pour ce patient.",
                    null
            );
        }
        int scoreUsed = (int) Math.round(stats.avgScore);

        // tendance simple : plus récent - plus ancien (sur les N récupérés)
        int recent = quizzes.get(0).scoreTotal;
        int oldest = quizzes.get(quizzes.size() - 1).scoreTotal;
        int trend = recent - oldest;

        String severity = severityFromScore(scoreUsed);
        MotifAnalysis motifA = analyzeMotif(motif);

        // Score par type_session (moteur de choix)
        Map<String, Double> scores = new LinkedHashMap<>();
        for (String t : List.of(
                "THERAPIE_INDIVIDUELLE",
                "THERAPIE_GROUPE",
                "REPETITION",
                "RESPIRATION",
                "MEDITATION",
                "YOGA",
                "AUTRE"
        )) {
            scores.put(t, 0.0);
        }

        // 1) Base selon catégorie motif
        switch (motifA.category) {
            case "DEPRESSION" -> {
                add(scores, "THERAPIE_GROUPE", 3.0);
                add(scores, "THERAPIE_INDIVIDUELLE", 3.0);
                add(scores, "REPETITION", 2.0);
                add(scores, "MEDITATION", 1.0);
            }
            case "STRESS" -> {
                add(scores, "RESPIRATION", 3.0);
                add(scores, "YOGA", 2.5);
                add(scores, "MEDITATION", 2.0);
                add(scores, "THERAPIE_GROUPE", 1.0);
            }
            case "SOMMEIL" -> {
                add(scores, "MEDITATION", 3.0);
                add(scores, "RESPIRATION", 2.0);
                add(scores, "YOGA", 1.5);
            }
            case "COLERE" -> {
                add(scores, "RESPIRATION", 3.0);
                add(scores, "THERAPIE_GROUPE", 2.0);
                add(scores, "YOGA", 1.5);
            }
            case "CONCENTRATION" -> {
                add(scores, "MEDITATION", 3.0);
                add(scores, "YOGA", 1.5);
                add(scores, "RESPIRATION", 1.0);
            }
            default -> {
                // motif non reconnu
                add(scores, "MEDITATION", 1.0);
                add(scores, "RESPIRATION", 1.0);
            }
        }

        // 2) Ajustement selon gravité (0-20 léger, 21-59 normal, 60-100 élevé)
        if (scoreUsed <= 20) {
            add(scores, "YOGA", 0.75);
            add(scores, "MEDITATION", 0.75);
        } else if (scoreUsed <= 59) {
            add(scores, "THERAPIE_GROUPE", 0.75);
            add(scores, "REPETITION", 0.25);
        } else {
            add(scores, "THERAPIE_INDIVIDUELLE", 2.0);
            add(scores, "THERAPIE_GROUPE", 1.0);
            if (scoreUsed >= 85) add(scores, "THERAPIE_INDIVIDUELLE", 1.0);
        }

        // 3) Ajustement selon tendance (si ça s'aggrave)
        if (trend >= 10) {
            add(scores, "THERAPIE_INDIVIDUELLE", 1.0);
            add(scores, "THERAPIE_GROUPE", 0.5);
        } else if (trend <= -10) {
            add(scores, "MEDITATION", 0.5);
            add(scores, "YOGA", 0.5);
        }

        // Choisir top1/top2
        List<Map.Entry<String, Double>> ranked = new ArrayList<>(scores.entrySet());
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        String rec = ranked.get(0).getKey();
        String alt = ranked.size() > 1 ? ranked.get(1).getKey() : null;

        double top = ranked.get(0).getValue();
        double second = ranked.size() > 1 ? ranked.get(1).getValue() : 0.0;
        double confidence = clamp01(0.55 + (top - second) * 0.12);
        if (motifA.category.equals("AUTRE")) confidence = Math.min(confidence, 0.70);
        if (quizzes.size() == 1) confidence = Math.min(confidence, 0.75);

        Integer usedHistoriqueId = quizzes.get(0).idHistorique; // FK : le plus récent
        String why = buildWhy(motifA, scoreUsed, severity, stats, quizzes.size(), trend, rec, alt);
        return new RecommendationResult(rec, alt, confidence, scoreUsed, severity, motifA.category, why, usedHistoriqueId);
    }

    // ---- Helpers ----

    private String buildWhy(MotifAnalysis motifA, int scoreUsed, String severity, QuizStats stats, int recentQuizUsed, int trend,
                            String rec, String alt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyse basée sur ").append(stats.count).append(" quiz + le motif.\n");
        sb.append("• Motif détecté : ").append(motifA.humanLabel).append("\n");
        sb.append("• Score global (moyenne de tous les quiz) : ").append(scoreUsed)
                .append(" → ").append(severity).append("\n");
        sb.append("• Détails quiz : total=").append(stats.sumScore)
                .append(", min=").append(stats.minScore)
                .append(", max=").append(stats.maxScore).append("\n");
        if (trend != 0) {
            sb.append("• Tendance : ").append(trend > 0 ? "+" : "").append(trend)
                    .append(" (calculée sur les " + recentQuizUsed + " derniers quiz : récent - ancien)\n");
        }
        sb.append("⇒ Recommandation : ").append(rec);
        if (alt != null) sb.append(" | Alternative : ").append(alt);
        sb.append("\n");
        sb.append("Pourquoi : ");
        switch (motifA.category) {
            case "DEPRESSION" -> sb.append("le motif indique un état dépressif, donc une approche thérapeutique (groupe/individuelle) est prioritaire pour le soutien et le suivi.");
            case "STRESS" -> sb.append("le motif indique stress/anxiété, donc respiration/yoga/méditation aident à réguler le stress et calmer le système nerveux.");
            case "SOMMEIL" -> sb.append("le motif indique des difficultés de sommeil, donc méditation/respiration favorisent l'endormissement et la détente.");
            case "COLERE" -> sb.append("le motif indique colère/irritabilité, donc respiration et séances encadrées aident au contrôle émotionnel.");
            case "CONCENTRATION" -> sb.append("le motif indique un besoin de concentration, donc méditation et routines de focus améliorent l'attention.");
            default -> sb.append("motif non spécifique, donc une option bien-être est proposée.");
        }
        return sb.toString();
    }

    private static void add(Map<String, Double> map, String key, double delta) {
        map.put(key, map.getOrDefault(key, 0.0) + delta);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private String severityFromScore(int score) {
        if (score <= 20) return "LEGER";
        if (score <= 59) return "NORMAL";
        return "ELEVE";
    }

    private static class MotifAnalysis {
        final String category;
        final String humanLabel;

        MotifAnalysis(String category, String humanLabel) {
            this.category = category;
            this.humanLabel = humanLabel;
        }
    }

    private MotifAnalysis analyzeMotif(String motif) {
        String m = normalize(motif);
        if (containsAny(m, "deprime", "deprim", "depression", "triste", "vide", "sans envie", "moral bas", "abattu"))
            return new MotifAnalysis("DEPRESSION", "Dépression / tristesse");
        if (containsAny(m, "stress", "anx", "angoisse", "panique", "pression", "tension"))
            return new MotifAnalysis("STRESS", "Stress / anxiété");
        if (containsAny(m, "sommeil", "insomnie", "dorm", "endorm", "reveil", "fatigue"))
            return new MotifAnalysis("SOMMEIL", "Sommeil");
        if (containsAny(m, "colere", "enerv", "irrit", "agress"))
            return new MotifAnalysis("COLERE", "Colère / irritabilité");
        if (containsAny(m, "concentr", "attention", "focus", "memoire"))
            return new MotifAnalysis("CONCENTRATION", "Concentration / attention");
        return new MotifAnalysis("AUTRE", "Autre");
    }

    private String normalize(String s) {
        if (s == null) return "";
        String x = s.trim().toLowerCase(Locale.ROOT);
        return x
                .replace('é', 'e').replace('è', 'e').replace('ê', 'e')
                .replace('à', 'a').replace('â', 'a')
                .replace('î', 'i').replace('ï', 'i')
                .replace('ô', 'o')
                .replace('ù', 'u').replace('û', 'u')
                .replace('ç', 'c');
    }

    
    private boolean containsAny(String text, String... keys) {
        // Match exact substring OR fuzzy match (tolère fautes de frappe)
        if (text == null) return false;
        String cleaned = text.trim();
        if (cleaned.isEmpty()) return false;

        for (String k : keys) {
            if (k == null || k.isBlank()) continue;
            String kk = k.trim();
            if (cleaned.contains(kk)) return true;

            // fuzzy : comparer avec chaque token
            for (String token : tokenize(cleaned)) {
                if (token.isBlank()) continue;
                int d = levenshtein(token, kk);
                if (d <= 2) return true; // tolérance
            }
        }
        return false;
    }

    private List<String> tokenize(String s) {
        // garde lettres/chiffres, remplace le reste par espace
        String x = s.replaceAll("[^a-z0-9]+", " ").trim();
        if (x.isEmpty()) return Collections.emptyList();
        return Arrays.asList(x.split("\\s+"));
    }

    private int levenshtein(String a, String b) {
        // Levenshtein standard (petites chaînes)
        if (a.equals(b)) return 0;
        int la = a.length(), lb = b.length();
        if (la == 0) return lb;
        if (lb == 0) return la;

        int[] prev = new int[lb + 1];
        int[] cur = new int[lb + 1];
        for (int j = 0; j <= lb; j++) prev[j] = j;

        for (int i = 1; i <= la; i++) {
            cur[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= lb; j++) {
                int cost = (ca == b.charAt(j - 1)) ? 0 : 1;
                cur[j] = Math.min(
                        Math.min(cur[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[lb];
    }
}
