package services;

import models.SeanceGroupe;
import utils.MyDatabase;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Service d'envoi d'emails (Gmail SMTP) pour notifier les participants
 * d'une séance de groupe avec le lien Jitsi.
 *
 * Configuration : renseignez EMAIL_SENDER et EMAIL_PASSWORD.
 * Pour Gmail, générez un "Mot de passe d'application" (App Password) :
 * https://myaccount.google.com/apppasswords
 */
public class EmailServiceF {

    // ======================================================
    // 🔧 CONFIGURATION — Modifiez ces deux lignes
    // ======================================================
    private static final String EMAIL_SENDER = "YOUR_SENDER_EMAIL@gmail.com";
    private static final String EMAIL_PASSWORD = "YOUR_APP_PASSWORD"; // Mot de passe d'application Gmail
    // ======================================================

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private final Session mailSession;

    public EmailServiceF() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        mailSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_SENDER, EMAIL_PASSWORD);
            }
        });
    }

    /**
     * Envoie une invitation de séance à tous les participants acceptés d'une
     * formation.
     * 
     * @param psyName  Nom du psychologue (expéditeur affiché)
     * @param psyEmail Email du psychologue (pour le Reply-To)
     */
    public int envoyerInvitationsSeance(SeanceGroupe seance, int formationId, String psyName, String psyEmail) {
        List<String[]> participants = getParticipantsForFormation(formationId);
        int sent = 0;

        String dateFormatee = seance.getDateHeure()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm"));

        for (String[] participant : participants) {
            String email = participant[0];
            String name = participant[1];
            try {
                envoyerEmail(email, name, seance, dateFormatee, psyName, psyEmail);
                sent++;
                System.out.println("✅ Email envoyé à : " + email);
            } catch (Exception e) {
                System.err.println("❌ Échec envoi email à " + email + " : " + e.getMessage());
            }
        }
        return sent;
    }

    /** Compatibilité */
    public int envoyerInvitationsSeance(SeanceGroupe seance, int formationId) {
        return envoyerInvitationsSeance(seance, formationId, "MindCare", EMAIL_SENDER);
    }

    // ─── Méthodes privées ──────────────────────────────────────────────────────

    private void envoyerEmail(String toEmail, String toName,
            SeanceGroupe seance, String dateFormatee, String psyName, String psyEmail) throws Exception {
        Message message = buildMessage(toEmail, psyName, psyEmail);
        message.setSubject("Invitation - Seance MindCare : " + seance.getTitre());

        // Corps HTML personnalisé avec le nom du psy
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(buildInvitationBody(toName, seance, dateFormatee, psyName), "text/html; charset=utf-8");

        // Pièce jointe .ics (iCalendar) — 1 clic pour ajouter au Google Calendar
        MimeBodyPart icsPart = new MimeBodyPart();
        String icsContent = buildIcsContent(seance, toEmail);
        icsPart.setContent(icsContent, "text/calendar; charset=utf-8; method=REQUEST");
        icsPart.setHeader("Content-Transfer-Encoding", "quoted-printable");
        icsPart.setFileName("invitation_mindcare.ics");

        // Email multipart = HTML + .ics
        MimeMultipart multipart = new MimeMultipart("mixed");
        multipart.addBodyPart(htmlPart);
        multipart.addBodyPart(icsPart);
        message.setContent(multipart);

        Transport.send(message);
    }

    /**
     * Génère le contenu iCalendar (.ics) pour l'événement.
     * Quand le destinataire ouvre la pièce jointe, son Google Calendar
     * lui propose d'ajouter l'événement automatiquement.
     */
    private String buildIcsContent(SeanceGroupe seance, String attendeeEmail) {
        DateTimeFormatter icsFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        // Convertir en UTC pour le format iCalendar
        String dtStart = seance.getDateHeure().toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC).format(icsFormat);
        String dtEnd = seance.getDateHeure().plusMinutes(seance.getDureeMinutes())
                .toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC).format(icsFormat);
        String dtStamp = java.time.OffsetDateTime.now(ZoneOffset.UTC).format(icsFormat);

        // IMPORTANT: Utiliser l'ID Google comme UID pour que l'annulation puisse le
        // retrouver
        String uid = (seance.getGoogleEventId() != null ? seance.getGoogleEventId() : UUID.randomUUID().toString())
                + "@mindcare";
        String summary = seance.getTitre() != null ? seance.getTitre() : "Séance MindCare";
        String location = seance.getLienJitsi() != null ? seance.getLienJitsi() : "";
        String desc = "Lien Jitsi pour rejoindre : " + location;

        return "BEGIN:VCALENDAR\r\n" +
                "VERSION:2.0\r\n" +
                "PRODID:-//MindCare//Esprit//FR\r\n" +
                "METHOD:REQUEST\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:" + uid + "\r\n" +
                "SEQUENCE:0\r\n" + // Version initiale
                "DTSTAMP:" + dtStamp + "\r\n" +
                "DTSTART:" + dtStart + "\r\n" +
                "DTEND:" + dtEnd + "\r\n" +
                "SUMMARY:" + summary + "\r\n" +
                "DESCRIPTION:" + desc + "\r\n" +
                "LOCATION:" + location + "\r\n" +
                "URL:" + location + "\r\n" +
                "ORGANIZER;CN=MindCare:mailto:" + EMAIL_SENDER + "\r\n" +
                "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=" + attendeeEmail
                + ":mailto:" + attendeeEmail + "\r\n" +
                "BEGIN:VALARM\r\n" +
                "TRIGGER:-PT30M\r\n" +
                "ACTION:DISPLAY\r\n" +
                "DESCRIPTION:Rappel séance MindCare dans 30 minutes\r\n" +
                "END:VALARM\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR\r\n";
    }

    /**
     * Génère un fichier ICS avec METHOD:CANCEL pour supprimer l'événement
     * des calendriers des participants.
     */
    private String buildCancelIcsContent(SeanceGroupe seance, String attendeeEmail) {
        DateTimeFormatter icsFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        String dtStart = seance.getDateHeure().toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC).format(icsFormat);
        String dtEnd = seance.getDateHeure().plusMinutes(seance.getDureeMinutes())
                .toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC).format(icsFormat);
        String dtStamp = java.time.OffsetDateTime.now(ZoneOffset.UTC).format(icsFormat);
        String summary = "ANNULÉ : " + (seance.getTitre() != null ? seance.getTitre() : "Séance MindCare");

        return "BEGIN:VCALENDAR\r\n" +
                "VERSION:2.0\r\n" +
                "PRODID:-//MindCare//Esprit//FR\r\n" +
                "METHOD:CANCEL\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:" + (seance.getGoogleEventId() != null ? seance.getGoogleEventId() : "unknown") + "@mindcare\r\n" +
                "SEQUENCE:2\r\n" + // On met une séquence plus haute pour forcer la suppression
                "STATUS:CANCELLED\r\n" +
                "DTSTAMP:" + dtStamp + "\r\n" +
                "DTSTART:" + dtStart + "\r\n" +
                "DTEND:" + dtEnd + "\r\n" +
                "SUMMARY:" + summary + "\r\n" +
                "ORGANIZER;CN=MindCare:mailto:" + EMAIL_SENDER + "\r\n" +
                "ATTENDEE;CN=" + attendeeEmail + ":mailto:" + attendeeEmail + "\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR\r\n";
    }

    private Message buildMessage(String toEmail, String fromName, String replyToEmail) throws Exception {
        Message message = new MimeMessage(mailSession);
        // Le From est techniquement EMAIL_SENDER, mais on affiche le nom du Psy
        message.setFrom(new InternetAddress(EMAIL_SENDER, fromName));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        // Si le patient répond, ça va au Psy, pas au compte admin
        if (replyToEmail != null) {
            message.setReplyTo(new Address[] { new InternetAddress(replyToEmail) });
        }
        return message;
    }

    private String buildInvitationBody(String name, SeanceGroupe seance, String dateFormatee, String psyName) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f4f7fb;margin:0;padding:20px'>"
                +
                "<div style='max-width:600px;margin:auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1)'>"
                +
                "<div style='background:linear-gradient(135deg,#5C98A8,#3a7a8e);padding:30px;text-align:center'>" +
                "<h1 style='color:white;margin:0;font-size:24px'>🧠 MindCare</h1>" +
                "<p style='color:rgba(255,255,255,0.85);margin:8px 0 0'>Plateforme de Santé Mentale</p></div>" +
                "<div style='padding:32px'>" +
                "<h2 style='color:#1F2A33'>Bonjour " + name + ",</h2>" +
                "<p style='color:#555;line-height:1.6'><strong>" + psyName
                + "</strong> vous invite à participer à une séance de groupe :</p>" +
                "<div style='background:#f0f7fa;border-left:4px solid #5C98A8;padding:20px;border-radius:8px;margin:20px 0'>"
                +
                "<p style='margin:6px 0'><strong>📌 Titre :</strong> " + seance.getTitre() + "</p>" +
                "<p style='margin:6px 0'><strong>📅 Date :</strong> " + dateFormatee + "</p>" +
                "<p style='margin:6px 0'><strong>⏱ Durée :</strong> " + seance.getDureeMinutes() + " minutes</p>" +
                "</div>" +
                "<div style='text-align:center;margin:28px 0'>" +
                "<a href='" + seance.getLienJitsi() + "' " +
                "style='background:linear-gradient(135deg,#5C98A8,#3a7a8e);color:white;padding:14px 32px;" +
                "border-radius:25px;text-decoration:none;font-weight:bold;font-size:16px;display:inline-block'>" +
                "🎥 Rejoindre la séance</a></div>" +
                "<p style='color:#888;font-size:13px;text-align:center'>Ps : Vous pouvez répondre directement à cet email pour contacter "
                + psyName + ".</p>" +
                "</div>" +
                "<div style='background:#f4f7fb;padding:16px;text-align:center'>" +
                "<p style='color:#aaa;font-size:12px;margin:0'>© 2026 MindCare – Esprit University</p></div>" +
                "</div></body></html>";
    }

    public int envoyerAnnulationSeance(SeanceGroupe seance, int formationId) {
        List<String[]> participants = getParticipantsForFormation(formationId);
        int sent = 0;

        String dateFormatee = seance.getDateHeure()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm"));

        for (String[] p : participants) {
            try {
                String toEmail = p[0];
                String toName = p[1];

                Message message = buildMessage(toEmail, "MindCare", null);
                message.setSubject("❌ Annulation – Séance MindCare : " + seance.getTitre());

                // Multipart: Corps HTML + Pièce jointe CANCEL .ics
                MimeMultipart multipart = new MimeMultipart("mixed");

                // 1. Corps HTML
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(buildAnnulationBody(toName, seance, dateFormatee), "text/html; charset=utf-8");
                multipart.addBodyPart(htmlPart);

                // 2. ICS CANCEL (Pour supprimer de leur calendrier Google/Outlook)
                MimeBodyPart icsPart = new MimeBodyPart();
                String icsContent = buildCancelIcsContent(seance, toEmail);
                icsPart.setContent(icsContent, "text/calendar; charset=utf-8; method=CANCEL");
                icsPart.setFileName("annulation_mindcare.ics");
                multipart.addBodyPart(icsPart);

                message.setContent(multipart);
                Transport.send(message);

                sent++;
            } catch (Exception e) {
                System.err.println("❌ Annulation email échec pour " + p[0] + " : " + e.getMessage());
            }
        }
        return sent;
    }

    // ─── Méthodes privées ──────────────────────────────────────────────────────

    private String buildAnnulationBody(String name, SeanceGroupe seance, String dateFormatee) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f4f7fb;padding:20px'>" +
                "<div style='max-width:600px;margin:auto;background:white;border-radius:12px;padding:32px;border-top:5px solid #ef4444'>"
                +
                "<h2>❌ Séance Annulée</h2>" +
                "<p>Bonjour <strong>" + name + "</strong>,</p>" +
                "<p>Nous vous informons que la séance <strong>\"" + seance.getTitre() + "\"</strong> prévue le <strong>"
                + dateFormatee
                + "</strong> a dû être annulée.</p>" +
                "<p>Nous nous excusons pour ce changement. N'hésitez pas à consulter vos autres séances ou formations disponibles.</p>"
                +
                "<div style='background:#fee2e2;color:#b91c1c;padding:15px;border-radius:8px;margin-top:20px;text-align:center'>"
                +
                "<strong>Cette séance n'est plus accessible.</strong>" +
                "</div>" +
                "</div></body></html>";
    }

    /**
     * Récupère les participants acceptés d'une formation : [email, "Prénom Nom"]
     */
    public List<String[]> getParticipantsForFormation(int formationId) {
        List<String[]> result = new ArrayList<>();
        String query = "SELECT u.email, u.prenom, u.nom FROM participation p " +
                "JOIN users u ON p.id_users = u.id_users " +
                "WHERE p.id_formation = ? AND p.statut = 'accepté' " +
                "AND u.email IS NOT NULL AND u.email != ''";
        try {
            Connection connection = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setInt(1, formationId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String email = rs.getString("email");
                        String fullName = rs.getString("prenom") + " " + rs.getString("nom");
                        result.add(new String[] { email.trim(), fullName.trim() });
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur récupération participants : " + e.getMessage());
        }
        return result;
    }

    /**
     * Test de connexion SMTP — utile pour vérifier la config email.
     * 
     * @return true si la connexion SMTP réussit
     */
    public boolean testConnection() {
        try (Transport transport = mailSession.getTransport("smtp")) {
            transport.connect(SMTP_HOST, EMAIL_SENDER, EMAIL_PASSWORD);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Test SMTP échoué : " + e.getMessage());
            return false;
        }
    }
}
