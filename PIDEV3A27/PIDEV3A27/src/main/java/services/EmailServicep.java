package services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailServicep {

    // ══════════════════════════════════════════════════════
    // ⚠️ IMPORTANT : Mettez votre clé d'application Gmail ici
    // 1) Activez la vérification en 2 étapes sur Gmail
    // 2) Allez sur : https://myaccount.google.com/apppasswords
    // 3) Créez une clé pour "Mail" et mettez-la ci-dessous
    // ══════════════════════════════════════════════════════
    private final String username = "mindcareservicemdp@gmail.com";
    private final String password = "vpls ggsz tlvd lwrq"; // ← Clé d'application (16 caractères)

    /**
     * Envoie un email HTML via Gmail SMTP (TLS port 587)
     */
    public boolean sendEmail(String to, String subject, String htmlBody) {
        System.out.println("📧 Tentative d'envoi email à : " + to);
        System.out.println("   From : " + username);

        try {
            Properties prop = new Properties();
            prop.put("mail.smtp.host", "smtp.gmail.com");
            prop.put("mail.smtp.port", "587");
            prop.put("mail.smtp.auth", "true");
            prop.put("mail.smtp.starttls.enable", "true");
            prop.put("mail.smtp.starttls.required", "true");
            prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
            prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            prop.put("mail.debug", "false"); // passer à "true" pour voir les logs SMTP

            Session session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, "MindCare Support"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email envoyé avec succès à : " + to);
            return true;

        } catch (Exception e) {
            System.err.println("❌ ERREUR SMTP: " + e.getMessage());
            System.err.println("   → Vérifiez la clé d'application Gmail sur:");
            System.err.println("   → https://myaccount.google.com/apppasswords");
            return false;
        }
    }

    /**
     * Envoie le code de réinitialisation en HTML premium (dark theme)
     */
    public boolean sendVerificationCode(String to, String code) {
        String subject = "Code de réinitialisation MindCare";

        // Construire les cases chiffres
        StringBuilder digits = new StringBuilder();
        for (char c : code.toCharArray()) {
            digits.append(
                    "<td style='padding:0 6px;'>" +
                            "<div style='width:46px;height:56px;line-height:56px;" +
                            "background-color:#1E2328;border:1px solid #2D3139;border-radius:10px;" +
                            "font-size:28px;font-weight:800;color:#4DABF7;text-align:center;" +
                            "font-family:Consolas,monospace;'>" + c + "</div>" +
                            "</td>");
        }

        String html = "<!DOCTYPE html>" +
                "<html lang='fr'><head><meta charset='UTF-8'/>" +
                "<meta name='viewport' content='width=device-width'/></head>" +
                "<body style='margin:0;padding:0;background-color:#0F1114;" +
                "font-family:Segoe UI,Arial,sans-serif;'>" +

                "<table width='100%' cellpadding='0' cellspacing='0' border='0'>" +
                "<tr><td align='center' style='padding:40px 20px;'>" +

                // Card
                "<table width='440' cellpadding='0' cellspacing='0' border='0' " +
                "style='background-color:#181A1D;border-radius:22px;" +
                "border:1px solid #2D3139;overflow:hidden;'>" +

                // Logo
                "<tr><td align='center' style='padding:28px 30px 16px;'>" +
                "<span style='font-size:26px;'>&#129504;</span>" +
                "<span style='font-size:22px;font-weight:900;color:#ffffff;" +
                "margin-left:10px;vertical-align:middle;'>MindCare</span>" +
                "</td></tr>" +

                // Blue bar
                "<tr><td height='4' bgcolor='#1565C0'></td></tr>" +

                // Content
                "<tr><td style='padding:30px 40px 35px;text-align:center;'>" +

                "<h1 style='color:#ffffff;font-size:20px;margin:0 0 10px;font-weight:800;'>" +
                "Votre code de r&eacute;initialisation</h1>" +

                "<p style='color:#718096;font-size:13px;margin:0 0 16px;'>" +
                "Envoy&eacute; &agrave; " + to + "</p>" +

                "<p style='color:#A0AEC0;text-align:left;font-size:13px;" +
                "line-height:1.7;margin:0 0 20px;'>" +
                "Vous avez <b style='color:#E2E8F0;'>demand&eacute;</b> &agrave; " +
                "r&eacute;initialiser votre mot de passe pour votre compte MindCare.</p>" +

                "<p style='color:#ffffff;font-weight:700;font-size:14px;margin:0 0 15px;'>" +
                "Voici votre code :</p>" +

                // Digit table
                "<table cellpadding='0' cellspacing='0' border='0' align='center' " +
                "style='margin:0 auto 20px;'>" +
                "<tr>" + digits.toString() + "</tr></table>" +

                "<p style='color:#4A5568;font-size:12px;margin:20px 0 0;line-height:1.6;'>" +
                "Ce code est valable <b style='color:#CBD5E0;'>10 minutes</b>.<br/>" +
                "Si vous n'&ecirc;tes pas &agrave; l'origine de cette demande, ignorez cet email." +
                "</p></td></tr>" +

                // Footer
                "<tr><td style='padding:14px 40px;border-top:1px solid #2D3139;" +
                "text-align:center;font-size:11px;color:#4A5568;'>" +
                "&copy; 2026 MindCare &mdash; Ne pas partager ce code." +
                "</td></tr></table>" +

                "</td></tr></table>" +
                "</body></html>";

        return sendEmail(to, subject, html);
    }
}