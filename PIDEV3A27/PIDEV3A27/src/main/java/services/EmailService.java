package services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    // Configurez votre email et votre clé d'application ici
    private final String username = "eyabd81@gmail.com";
    private final String password = "rqab gfny sevx lnim"; // Votre clé Google fournie

    public boolean sendEmail(String to, String subject, String body) {
        if (username.equals("votre.email@gmail.com")) {
            System.err.println("ERREUR: Vous devez configurer votre email Gmail dans EmailService.java");
            System.out.println("[TEST MODE] Code : " + body);
            return false;
        }

        try {
            Properties prop = new Properties();
            prop.put("mail.smtp.host", "smtp.gmail.com");
            prop.put("mail.smtp.port", "587");
            prop.put("mail.smtp.auth", "true");
            prop.put("mail.smtp.starttls.enable", "true");
            prop.put("mail.smtp.ssl.protocols", "TLSv1.2");

            Session session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Email envoyé avec succès !");
            return true;
        } catch (MessagingException e) {
            System.err.println("ERREUR SMTP: " + e.getMessage());
            // Fallback console pour le test
            System.out.println("[TEST MODE] Code : " + body);
            return false;
        }
    }

    public boolean sendVerificationCode(String to, String code) {
        String subject = "Code de réinitialisation MindCare";
        String body = "Votre code de réinitialisation est : " + code;
        return sendEmail(to, subject, body);
    }
}
