package utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Création automatique des tables "Forum avancé" si elles n'existent pas.
 * Objectif: rendre le projet exécutable même si tu n'as pas encore appliqué le script SQL.
 *
 * IMPORTANT: Cette version est alignée avec le script SQL conseillé:
 *  - user_points(id_user, points, badge, updated_at)
 *  - user_activity(id_user, action_type, entity_type, entity_id, meta, created_at)
 *  - notifications(id_user, type, message, entity_type, entity_id, is_read, created_at)
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {}

    public static void ensureForumExtraTables(Connection cnx) throws SQLException {
        if (cnx == null) return;

        try (Statement st = cnx.createStatement()) {

            // 1) Scoring utilisateur
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS user_points (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "id_user INT NOT NULL," +
                            "points INT NOT NULL DEFAULT 0," +
                            "badge VARCHAR(50) NULL," +
                            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "UNIQUE KEY uq_user_points (id_user)," +
                            "INDEX idx_user_points_points (points)," +
                            "CONSTRAINT fk_user_points_users FOREIGN KEY (id_user) REFERENCES users(id_users) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 2) Historique d'activité
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS user_activity (" +
                            "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "id_user INT NOT NULL," +
                            "action_type VARCHAR(50) NOT NULL," +
                            "entity_type VARCHAR(50) NULL," +
                            "entity_id BIGINT UNSIGNED NULL," +
                            "meta TEXT NULL," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "INDEX idx_user_activity_user (id_user)," +
                            "INDEX idx_user_activity_date (created_at)," +
                            "CONSTRAINT fk_user_activity_users FOREIGN KEY (id_user) REFERENCES users(id_users) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 3) Notifications
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS notifications (" +
                            "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "id_user INT NOT NULL," +
                            "type VARCHAR(50) NOT NULL," +
                            "message VARCHAR(255) NOT NULL," +
                            "entity_type VARCHAR(50) NULL," +
                            "entity_id BIGINT UNSIGNED NULL," +
                            "is_read TINYINT(1) NOT NULL DEFAULT 0," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "INDEX idx_notifications_user (id_user)," +
                            "INDEX idx_notifications_read (is_read)," +
                            "CONSTRAINT fk_notifications_users FOREIGN KEY (id_user) REFERENCES users(id_users) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 4) Mots interdits (optionnel)
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS banned_words (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "word VARCHAR(100) NOT NULL," +
                            "is_active TINYINT(1) NOT NULL DEFAULT 1," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "UNIQUE KEY uq_banned_word (word)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 5) Rate limit commentaires (optionnel)
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS comment_rate_limit (" +
                            "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "id_user INT NOT NULL," +
                            "window_start DATETIME NOT NULL," +
                            "count_in_window INT NOT NULL DEFAULT 0," +
                            "INDEX idx_rate_user_window (id_user, window_start)," +
                            "CONSTRAINT fk_rate_limit_users FOREIGN KEY (id_user) REFERENCES users(id_users) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        }
    }
}
