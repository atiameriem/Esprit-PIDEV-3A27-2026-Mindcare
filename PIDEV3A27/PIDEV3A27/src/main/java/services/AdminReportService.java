package services;

/**
 * Service "Admin Reports": regroupe la logique (SQL + email) hors controller.
 * - Récupère l'email via UserService (SQL)
 * - Envoie via EmailService (Mailjet)
 */
public class AdminReportService {

    private final UserService userService = new UserService();
    private final EmailService emailService = new EmailService();

    public void sendMailToUserId(int userId, String subject, String message) throws Exception {
        String to = userService.fetchEmailById(userId);
        if (to == null || to.isBlank()) {
            throw new IllegalStateException("Email introuvable pour l'utilisateur id=" + userId);
        }
        emailService.sendEmailToUser(to, subject, message);
    }
}
