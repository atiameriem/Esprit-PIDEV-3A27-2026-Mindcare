package services;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import models.CompteRenduView;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class PdfCompteRenduService {

    //Constructeur privé : empêche de faire new PdfCompteRenduService().
    private PdfCompteRenduService() {}

    //Prend :
    //cr : les données du compte rendu
    //outputPdf : où enregistrer le PDF
    //viewerRoleLabel : rôle de la personne qui exporte (patient / psychologue)
    public static void exportCompteRendu(CompteRenduView cr, Path outputPdf, String viewerRoleLabel) throws Exception {
        if (cr == null) throw new IllegalArgumentException("CompteRenduView is null");
        if (outputPdf == null) throw new IllegalArgumentException("outputPdf is null");

        //Lit le fichier CSS
        String css = readResource("/pdf/compte_rendu.css");
        //Construit le HTML final + injecte le CSS + adapte selon le rôle.
        String html = buildHtml(cr, css, viewerRoleLabel); // ✅ viewerRoleLabel utilisé

        //Ouvre un flux d’écriture vers le fichier PDF.
        try (OutputStream os = Files.newOutputStream(outputPdf)) {
            //Objet qui va faire la conversion HTML → PDF.
            PdfRendererBuilder builder = new PdfRendererBuilder();
            //Active un mode rapide
            builder.useFastMode();
            //Donne le contenu HTML à convertir.
            //null = pas de base URL
            builder.withHtmlContent(html, null);
            //Dit : “le PDF généré écris-le dans os”.
            builder.toStream(os);
            builder.run();
        }
    }

    // ✅ PDF doit refléter l'affichage (patient vs psy)
    private static String buildHtml(CompteRenduView cr, String css, String viewerRoleLabel) {

        //Récupérer date/heure du RDV
        //Si la date/heure est null → on met "-".
        String date = cr.getRvDate() == null ? "-" : cr.getRvDate().toString();
        String time = cr.getRvTime() == null ? "-" : cr.getRvTime().toString();

        String patient = safe(cr.getPatientFullName(), "Patient");
        String psy = safe(cr.getPsychologistFullName(), "Psychologue");

        String progress = cr.getProgresCr() == null ? "-" : cr.getProgresCr().name();
        String resume = safe(cr.getResumeSeanceCr(), "—");
        String actions = safe(cr.getProchainesActionCr(), "—");

        //hasAi = vrai si :
        //texte IA existe
        //pas vide
        //pas “canceled”
        boolean hasAi = cr.getAiResumeCr() != null && !cr.getAiResumeCr().isBlank() && !cr.isAiResumeCanceled();
        String aiResume = hasAi ? cr.getAiResumeCr().trim() : null;

        String type = cr.getRvType() == null ? "-" : cr.getRvType().name();
        String statut = cr.getRvStatut() == null ? "-" : cr.getRvStatut().name();
 //rating de 0 a 5
        int rating = (cr.getRating() == null) ? 0 : Math.max(0, Math.min(5, cr.getRating()));

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // ✅ Détecter rôle (patient)
        boolean isPatient = viewerRoleLabel != null && viewerRoleLabel.toLowerCase().contains("patient");

        // Crée des petites icônes HTML <svg> (pas besoin d’images externes).
        //svg() fabrique le code
        String iconDoc = svg("M6 2h7l5 5v15a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm7 1.5V8h4.5", "#0F766E");
        String iconCal = svg("M7 2v2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2V2h-2v2H9V2H7zm12 6H5v10h14V8z", "#2563eb");
        String iconTag = svg("M20.59 13.41 11 3.83V3h-1v.83L3.41 13.41a2 2 0 0 0 0 2.83l3.35 3.35a2 2 0 0 0 2.83 0L20.59 16.24a2 2 0 0 0 0-2.83zM7.5 8A1.5 1.5 0 1 1 9 6.5 1.5 1.5 0 0 1 7.5 8z", "#2563eb");

        // ✅ Contenu exporté selon affichage:
        // - Patient: si IA existe -> exporter IA seulement (comme UI)
        // - Sinon: exporter contenu normal (psy)
        boolean exportAiOnlyForPatient = isPatient && hasAi;

        return "<html>"
                + "<head>"
                + "<meta charset='utf-8'/>"
                + "<style>" + css + "</style>"
                + "</head>"
                + "<body>"
                + "  <div class='page'>"

                + "    <div class='date-top'>"
                + "      " + iconCal + "<span> RDV : " + esc(date) + " · " + esc(time) + "</span><br/>"
                + "      <span>Généré le : " + esc(now) + "</span>"
                + "    </div>"

                + "    <div class='header'>"
                + "      <div class='brand'>"
                + "        <div class='logo'>" + iconDoc + "</div>"
                + "        <div>"
                + "          <h1>Compte-rendu de séance</h1>"
                + "          <p>MindCare</p>"
                + "        </div>"
                + "      </div>"
                + "    </div>"

                + "    <hr class='sep'/>"

                // ✅ DÉTAILS
                + "    <div class='card'>"
                + "      <h2>DÉTAILS</h2>"
                + "      <table class='kv'>"
                + "        <tr><td class='k'>Type</td><td class='v dark'>" + esc(type) + "</td></tr>"
                + "        <tr><td class='k'>Statut</td><td class='v dark'>" + esc(statut) + "</td></tr>"
                + "        <tr><td class='k'>Patient</td><td class='v dark'>: " + esc(patient) + "</td></tr>"
                + "        <tr><td class='k'>Psychologue</td><td class='v dark'>: " + esc(psy) + "</td></tr>"
                + "        <tr><td class='k'>Progrès</td><td class='v dark'>: " + esc(progress) + "</td></tr>"
                + "      </table>"
                + "    </div>"

                // NOTE PATIENT
                + "    <div class='card'>"
                + "      <h2>NOTE PATIENT</h2>"
                + "      <table class='kv'>"
                + "        <tr><td class='k'>Étoiles</td><td class='v stars-wrap'>" + renderStarsBoxes(rating) + "</td></tr>"
                + "      </table>"
                + "    </div>"

                // ✅ Affichage PDF selon UI
                + (exportAiOnlyForPatient
                ? (
                // Patient voit IA seulement
                "    <div class='section'>"
                        + "      <h3>" + iconTag + " RÉSUMÉ</h3>"
                        + "      <div class='box'>" + escNl(aiResume) + "</div>"
                        + "    </div>"
        )
                : (
                // Comportement normal (comme avant)
                "    <div class='section'>"
                        + "      <h3>" + iconTag + " RÉSUMÉ DE SÉANCE</h3>"
                        + "      <div class='box'>" + escNl(resume) + "</div>"
                        + "    </div>"

                        + "    <div class='section'>"
                        + "      <h3>" + iconTag + " PROCHAINES ACTIONS</h3>"
                        + "      <div class='box'>" + escNl(actions) + "</div>"
                        + "    </div>"

                        // Synthèse IA (si présente) : on garde pour psy, et pour patient seulement si tu veux (mais ton UI patient montre IA-only)
                        + ((!isPatient && hasAi)
                        ? ("    <div class='section'>"
                        + "      <h3>" + iconTag + " SYNTHÈSE IA (LIA)</h3>"
                        + "      <div class='box'>" + escNl(aiResume) + "</div>"
                        + "    </div>")
                        : "")
        )
        )

                + "  </div>"
                + "</body>"
                + "</html>";
    }

    // ✅ rating  crée les “étoiles” en HTML/CSS
    //Construit 5 petits blocs.
    private static String renderStarsBoxes(int rating) {
        //String améliorée” pour concaténer dans une boucle.
        StringBuilder sb = new StringBuilder();
        //Si i <= rating → classe on sinon off
        for (int i = 1; i <= 5; i++) {
            sb.append("<span class='starbox ").append(i <= rating ? "on" : "off").append("'></span>");
        }
        //Retourne le HTML final.
        return sb.toString();
    }
//éviter null ou vide
    private static String safe(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }
//échapper les caractères spéciaux HTML
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ✅ important: garder les retours à la ligne dans le PDF
    //Puis remplace \n par <br/> pour le PDF.
    private static String escNl(String s) {
        return esc(s).replace("\r\n", "\n").replace("\n", "<br/>");
    }

    //lire un fichier dans resources
    private static String readResource(String path) {
        //Lit une ressource dans le (dossier resources).
        try (InputStream is = PdfCompteRenduService.class.getResourceAsStream(path)) {
            if (is == null) return "";
            //Lit tout le fichier en texte UTF-8.
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
//fabrique une icône SVG HTML
    private static String svg(String path, String color) {
        return "<svg class='icon' viewBox='0 0 24 24' xmlns='http://www.w3.org/2000/svg'>"
                + "<path d='" + path + "' fill='" + color + "'/></svg>";
    }
}