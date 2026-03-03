package services;

import utils.UserSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Historique "sans table" : on journalise dans un fichier local.
 * Visible uniquement pour responsable_centre/admin via l'UI.
 */
public class HistoryService {

    private static final Path LOG_PATH = Paths.get("exports", "history", "historique.log");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Path getLogPath() {
        return LOG_PATH;
    }

    public static void log(String action, String message) {
        try {
            Files.createDirectories(LOG_PATH.getParent());

            String who = "anonymous";
            if (UserSession.isLoggedIn() && UserSession.getCurrentUser() != null) {
                who = UserSession.getCurrentUser().getRole() + "#" + UserSession.getCurrentUser().getIdUsers();
            }

            String line = String.format("[%s] [%s] [%s] %s%n",
                    LocalDateTime.now().format(TS),
                    who,
                    (action == null ? "ACTION" : action.trim()),
                    (message == null ? "" : message.trim()));

            Files.write(LOG_PATH, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // ne jamais bloquer l'app à cause de l'historique
            System.err.println("⚠️ HistoryService: " + e.getMessage());
        }
    }
}
