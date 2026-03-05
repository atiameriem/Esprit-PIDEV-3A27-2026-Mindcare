package services;

import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Modération automatique (simple et locale):
 * - mots interdits
 * - filtrage spam (liens, répétitions)
 * - limite commentaires / minute
 */
public class ModerationService {

    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    // Tu peux externaliser ce set dans un fichier plus tard.
    private static final Set<String> BANNED = new HashSet<>(Arrays.asList(
            "fuck", "shit", "bitch", "asshole", "idiot",
            "spam", "casino", "bet", "porn"
    ));

    private static final Pattern URL = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern MANY_SAME_CHAR = Pattern.compile("(.)\\1{9,}");

    /**
     * Validation d'un post (mots interdits + spam + anti-duplicata).
     * Règle: un utilisateur ne peut pas publier le même contenu pendant 7 jours.
     */
    public void validatePost(int idUsers, String title, String content) throws SQLException {
        checkForbiddenWords(title);
        checkForbiddenWords(content);
        checkSpam(content);
        checkDuplicatePostForWeek(idUsers, content);
    }

    /**
     * Ancienne signature conservée pour compatibilité.
     * (Sans règle anti-duplicata)
     */
    public void validatePost(String title, String content) throws SQLException {
        checkForbiddenWords(title);
        checkForbiddenWords(content);
        checkSpam(content);
    }

    /**
     * Validation d'un commentaire (mots interdits + spam + rate limit + anti-duplicata).
     * Règles:
     *  - max 3 commentaires / minute (global)
     *  - ne pas poster le même texte de commentaire (duplicata)
     */
    public void validateComment(int idUsers, String content, int maxPerMinute) throws SQLException {
        checkForbiddenWords(content);
        checkSpam(content);
        checkCommentRateLimit(idUsers, maxPerMinute);
        checkDuplicateComment(idUsers, content);
    }

    private void checkForbiddenWords(String text) throws SQLException {
        if (text == null) return;
        String low = text.toLowerCase();
        for (String w : BANNED) {
            if (low.contains(w)) {
                throw new SQLException("Contenu refusé (mot interdit détecté)");
            }
        }
    }

    private void checkSpam(String content) throws SQLException {
        if (content == null) return;
        // trop de liens
        int links = 0;
        var m = URL.matcher(content);
        while (m.find()) links++;
        if (links >= 3) throw new SQLException("Contenu refusé (trop de liens)");

        // répétitions (ex: aaaaaaaaaaa)
        if (MANY_SAME_CHAR.matcher(content).find()) {
            throw new SQLException("Contenu refusé (spam détecté)");
        }
    }

    private void checkCommentRateLimit(int idUsers, int maxPerMinute) throws SQLException {
        Connection cnx = cnx();
        String sql = "SELECT COUNT(*) FROM comments WHERE id_users=? AND created_at >= (NOW() - INTERVAL 1 MINUTE)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUsers);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                if (count >= maxPerMinute) {
                    throw new SQLException("Limite atteinte: " + maxPerMinute + " commentaires / minute");
                }
            }
        }
    }

    private void checkDuplicatePostForWeek(int idUsers, String content) throws SQLException {
        if (content == null) return;
        Connection cnx = cnx();
        String sql = "SELECT 1 FROM post WHERE id_users=? AND status='PUBLISHED' " +
                "AND created_at >= (NOW() - INTERVAL 7 DAY) " +
                "AND LOWER(TRIM(content)) = LOWER(TRIM(?)) LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUsers);
            ps.setString(2, content);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new SQLException("Spam: vous avez déjà publié le même post durant les 7 derniers jours");
                }
            }
        }
    }

    private void checkDuplicateComment(int idUsers, String content) throws SQLException {
        if (content == null) return;
        Connection cnx = cnx();
        String sql = "SELECT 1 FROM comments WHERE id_users=? AND status='PUBLISHED' " +
                "AND LOWER(TRIM(content)) = LOWER(TRIM(?)) LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUsers);
            ps.setString(2, content);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new SQLException("Spam: vous ne pouvez pas publier le même commentaire");
                }
            }
        }
    }
}
