package test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.awt.Desktop;

public class mainrapport {

    public static void main(String[] args) {
        ServiceRapport service = new ServiceRapport();
        try {
            // Générer un rapport HTML pour le premier patient
            String html = service.genererRapportPourTest();

            if (html.isEmpty()) {
                System.out.println("⚠️ Aucun patient avec données ce mois-ci.");
                return;
            }

            // Créer un fichier temporaire
            File fichier = File.createTempFile("rapport_mensuel_", ".html");
            try (FileWriter fw = new FileWriter(fichier)) {
                fw.write(html);
            }

            // Ouvrir automatiquement dans le navigateur
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(fichier.toURI());
                System.out.println("✅ Rapport ouvert dans le navigateur !");
            } else {
                System.out.println("🌐 Fichier généré : " + fichier.getAbsolutePath());
            }

        } catch (SQLException | IOException e) {
            System.err.println("❌ Erreur lors du test du rapport : " + e.getMessage());
        }
    }
}