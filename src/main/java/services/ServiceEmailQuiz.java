package services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class ServiceEmailQuiz {

    // ⚠️ Remplacez par votre Gmail + App Password
    private static final String EXPEDITEUR   = "mindcare563@gmail.com";
    private static final String APP_PASSWORD = "key";


    // ══════════════════════════════════════════════════════════════
    // Envoi email HTML — retourne true si succès
    // ══════════════════════════════════════════════════════════════
    public boolean envoyerEmail(String destinataire, String sujet, String contenuHtml) {

        Properties props = new Properties();
        props.put("mail.smtp.host",              "smtp.gmail.com");
        props.put("mail.smtp.port",              "587");
        props.put("mail.smtp.auth",              "true");
        props.put("mail.smtp.starttls.enable",   "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust",         "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols",     "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EXPEDITEUR, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EXPEDITEUR));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject(sujet);

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(contenuHtml, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(htmlPart);
            message.setContent(multipart);

            Transport.send(message);
            System.out.println("✅ Email envoyé à : " + destinataire);
            return true;

        } catch (MessagingException e) {
            System.err.println("❌ Erreur email : " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Template — Nouveau Quiz assigné
    // ══════════════════════════════════════════════════════════════
    public String templateNouveauQuiz(String nomPatient, String titreQuiz,
                                      String description, String typeTest) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f4f4f4;padding:20px'>"
                + "<div style='max-width:500px;margin:auto;background:white;border-radius:12px;padding:30px'>"
                + "<h2 style='color:#2c4a6e'>🧠 MindCare</h2>"
                + "<p>Bonjour <strong>" + nomPatient + "</strong>,</p>"
                + "<p>Un nouveau test vous a été assigné :</p>"
                + "<div style='background:#dce8f0;border-radius:8px;padding:16px;margin:16px 0'>"
                + "<h3 style='margin:0;color:#2c4a6e'>" + titreQuiz + "</h3>"
                + "<p style='margin:8px 0;color:#555'>" + description + "</p>"
                + "<span style='background:#2c4a6e;color:white;padding:4px 12px;border-radius:20px;font-size:12px'>"
                + typeTest + "</span>"
                + "</div>"
                + "<p>Connectez-vous à MindCare pour passer votre test.</p>"
                + "<p style='color:#999;font-size:12px'>— L'équipe MindCare</p>"
                + "</div></body></html>";
    }

    // ══════════════════════════════════════════════════════════════
    // Template — Rappel Quiz non complété
    // ══════════════════════════════════════════════════════════════
    public String templateRappelQuiz(String nomPatient, int joursInactif) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f4f4f4;padding:20px'>"
                + "<div style='max-width:500px;margin:auto;background:white;border-radius:12px;padding:30px'>"
                + "<h2 style='color:#e67e22'>⏰ MindCare — Rappel</h2>"
                + "<p>Bonjour <strong>" + nomPatient + "</strong>,</p>"
                + "<p>Vous n'avez pas effectué de test depuis <strong>" + joursInactif + " jours</strong>.</p>"
                + "<div style='background:#fef9e7;border-radius:8px;padding:16px;margin:16px 0;border-left:4px solid #e67e22'>"
                + "<p style='margin:0;color:#7f6000'>Prenez quelques minutes pour compléter votre suivi.</p>"
                + "</div>"
                + "<p>Connectez-vous à MindCare pour passer votre test.</p>"
                + "<p style='color:#999;font-size:12px'>— L'équipe MindCare</p>"
                + "</div></body></html>";
    }

    // ══════════════════════════════════════════════════════════════
    // Template — Résultat après soumission du quiz
    // ══════════════════════════════════════════════════════════════
    public String templateResultatQuiz(String nomPatient, String titreQuiz,
                                       int score, int pourcentage, String conseil) {
        String couleur = pourcentage >= 70 ? "#27ae60" : pourcentage >= 40 ? "#f39c12" : "#e74c3c";
        String emoji   = pourcentage >= 70 ? "🏆" : pourcentage >= 40 ? "⭐" : "💪";

        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f4f4f4;padding:20px'>"
                + "<div style='max-width:500px;margin:auto;background:white;border-radius:12px;padding:30px'>"
                + "<h2 style='color:#2c4a6e'>🧠 MindCare — Résultat</h2>"
                + "<p>Bonjour <strong>" + nomPatient + "</strong>,</p>"
                + "<p>Voici le résultat de votre test <strong>" + titreQuiz + "</strong> :</p>"
                + "<div style='text-align:center;background:#f8f9fa;border-radius:12px;padding:24px;margin:16px 0'>"
                + "<div style='font-size:48px'>" + emoji + "</div>"
                + "<div style='font-size:52px;font-weight:900;color:" + couleur + "'>" + pourcentage + "%</div>"
                + "<div style='color:#7f8c8d;margin-top:8px'>Score : " + score + " pts</div>"
                + "</div>"
                + "<div style='background:#eaf4fb;border-radius:8px;padding:16px;margin:16px 0;border-left:4px solid #2c4a6e'>"
                + "<strong>💡 Conseil :</strong><br/>"
                + "<span style='color:#555'>" + (conseil != null && !conseil.isEmpty() ? conseil : "Continuez à pratiquer régulièrement.") + "</span>"
                + "</div>"
                + "<p style='color:#999;font-size:12px'>— L'équipe MindCare</p>"
                + "</div></body></html>";
    }
}