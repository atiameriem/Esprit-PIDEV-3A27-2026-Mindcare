package services;

import org.apache.poi.xwpf.usermodel.*;
import utils.MyDatabase;
import java.io.*;
import java.sql.*;

/**
 * WordGeneratorService — Génère et écrit les fichiers Word (.docx)
 * Utilisé par AIInterviewController après l'interview IA
 */
public class GeneratorServiceF {

    private static final String SAVE_PATH = "src/main/resources/cours/";

    // ─────────────────────────────────────────────
    // Écrire le Word depuis le contenu généré par l'IA
    // ─────────────────────────────────────────────
    public static String ecrireWord(String titre, String contenuIA) throws IOException {

        new File(SAVE_PATH).mkdirs();
        String nomFichier = SAVE_PATH + titre.replaceAll("\\s+", "_") + ".docx";

        try (XWPFDocument doc = new XWPFDocument()) {

            // ═══ TITRE PRINCIPAL ═══
            XWPFParagraph titrePara = doc.createParagraph();
            titrePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titreRun = titrePara.createRun();
            titreRun.setText(titre);
            titreRun.setBold(true);
            titreRun.setFontSize(24);
            titreRun.setColor("1A6B3C"); // Vert MindCare
            titreRun.setFontFamily("Arial");
            titreRun.addBreak();

            // Ligne séparatrice
            XWPFParagraph separateur = doc.createParagraph();
            separateur.setBorderBottom(Borders.SINGLE);

            // ═══ PARSER ET ÉCRIRE LE CONTENU ═══
            String[] lignes = contenuIA.split("\n");

            for (String ligne : lignes) {
                ligne = ligne.trim();
                if (ligne.isEmpty())
                    continue;

                if (ligne.startsWith("INTRODUCTION:")) {
                    ajouterTitreSection(doc, "Introduction", "2980B9");
                    ajouterContenu(doc, ligne.replace("INTRODUCTION:", "").trim());

                } else if (ligne.startsWith("OBJECTIFS:")) {
                    ajouterTitreSection(doc, "Objectifs Thérapeutiques", "27AE60");
                    // Traiter liste numérotée
                    String objectifs = ligne.replace("OBJECTIFS:", "").trim();
                    for (String obj : objectifs.split("\\d+\\.")) {
                        if (!obj.trim().isEmpty()) {
                            ajouterListItem(doc, obj.trim());
                        }
                    }

                } else if (ligne.contains("_TITRE:")) {
                    String titreSection = ligne.split("_TITRE:")[1].trim();
                    ajouterTitreSection(doc, titreSection, "8E44AD");

                } else if (ligne.contains("_CONTENU:")) {
                    String contenu = ligne.split("_CONTENU:")[1].trim();
                    ajouterContenu(doc, contenu);

                } else if (ligne.startsWith("EXERCICES:")) {
                    ajouterTitreSection(doc, "Exercices Thérapeutiques", "E67E22");
                    ajouterContenu(doc, ligne.replace("EXERCICES:", "").trim());

                } else if (ligne.startsWith("CONCLUSION:")) {
                    ajouterTitreSection(doc, "Conclusion", "2C3E50");
                    ajouterContenu(doc, ligne.replace("CONCLUSION:", "").trim());
                }
            }

            // ═══ PIED DE PAGE ═══
            ajouterPiedDePage(doc, titre);

            // Sauvegarder le fichier
            try (FileOutputStream out = new FileOutputStream(nomFichier)) {
                doc.write(out);
            }
        }

        System.out.println("✅ Word généré : " + nomFichier);
        return nomFichier;
    }

    // ─────────────────────────────────────────────
    // Helpers — Formatage
    // ─────────────────────────────────────────────

    private static void ajouterTitreSection(XWPFDocument doc, String titre, String couleur) {
        XWPFParagraph para = doc.createParagraph();
        para.setSpacingBefore(300);
        para.setSpacingAfter(100);
        XWPFRun run = para.createRun();
        run.setText("▌ " + titre);
        run.setBold(true);
        run.setFontSize(15);
        run.setColor(couleur);
        run.setFontFamily("Arial");
    }

    private static void ajouterContenu(XWPFDocument doc, String texte) {
        XWPFParagraph para = doc.createParagraph();
        para.setSpacingAfter(120);
        XWPFRun run = para.createRun();
        run.setText(texte);
        run.setFontSize(12);
        run.setColor("2C3E50");
        run.setFontFamily("Arial");
    }

    private static void ajouterListItem(XWPFDocument doc, String texte) {
        XWPFParagraph para = doc.createParagraph();
        para.setIndentationLeft(720);
        XWPFRun run = para.createRun();
        run.setText("• " + texte);
        run.setFontSize(12);
        run.setColor("2C3E50");
        run.setFontFamily("Arial");
    }

    private static void ajouterPiedDePage(XWPFDocument doc, String titre) {
        XWPFParagraph para = doc.createParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingBefore(400);
        para.setBorderTop(Borders.SINGLE);
        XWPFRun run = para.createRun();
        run.setText("MindCare — Programme généré par IA | " + titre);
        run.setFontSize(9);
        run.setColor("999999");
        run.setItalic(true);
    }

    // ─────────────────────────────────────────────
    // Sauvegarder en MySQL
    // ─────────────────────────────────────────────
    public static void sauvegarderEnDB(String titre, String chemin, int idPsychologue, int moduleId, int idFormation)
            throws SQLException {
        String sql = """
                INSERT INTO cours_genere
                (titre, chemin_word, id_psychologue, module_id, id_formation, statut, date_creation)
                VALUES (?, ?, ?, ?, ?, 'brouillon', NOW())
                """;

        try (Connection conn = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setString(2, chemin);
            ps.setInt(3, idPsychologue);
            ps.setInt(4, moduleId);
            ps.setInt(5, idFormation);
            ps.executeUpdate();
            System.out.println("✅ Cours sauvegardé avec module_id=" + moduleId + " id_formation=" + idFormation);
        }
    }

    // ─────────────────────────────────────────────
    // Lire le contenu d'un fichier Word pour l'aperçu
    // ─────────────────────────────────────────────
    public static String lireWord(String chemin) throws IOException {
        try (FileInputStream fis = new FileInputStream(chemin);
                XWPFDocument doc = new XWPFDocument(fis)) {
            StringBuilder sb = new StringBuilder();
            int sectionCount = 1;
            boolean introDone = false;

            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText().trim();
                if (text.isEmpty() || text.startsWith("MindCare —"))
                    continue;

                // Titre principal
                if (p.getAlignment() == ParagraphAlignment.CENTER && !sb.toString().contains("ARTICLE_TITRE:")) {
                    sb.append("ARTICLE_TITRE: ").append(text).append("\n");
                    continue;
                }

                if (text.startsWith("▌ ")) {
                    String cleanTitle = text.replace("▌ ", "").trim();
                    if (cleanTitle.equalsIgnoreCase("Introduction")) {
                        // ignore header
                    } else if (cleanTitle.equalsIgnoreCase("Conclusion")) {
                        // ignore header
                    } else {
                        sb.append("SECTION").append(sectionCount).append("_TITRE: ").append(cleanTitle).append("\n");
                    }
                    continue;
                }

                // Distribution du contenu
                if (!introDone) {
                    sb.append("INTRO_CONTENU: ").append(text).append("\n");
                    introDone = true;
                } else if (sb.toString().contains("SECTION" + sectionCount + "_TITRE:")
                        && !sb.toString().contains("SECTION" + sectionCount + "_CONTENU:")) {
                    sb.append("SECTION").append(sectionCount).append("_CONTENU: ").append(text).append("\n");
                    sectionCount++;
                } else if (text.length() > 0) {
                    if (p.equals(doc.getParagraphs().get(doc.getParagraphs().size() - 1))
                            || text.contains("encourageante")) {
                        sb.append("CONCLUSION_CONTENU: ").append(text).append("\n");
                    } else {
                        sb.append(text).append("\n");
                    }
                }
            }
            return sb.toString();
        }
    }
}
