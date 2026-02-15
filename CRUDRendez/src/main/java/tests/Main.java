package tests;

import entities.CompteRenduSeance;
import entities.RendezVous;
import services.ServiceCompteRenduSeance;
import services.ServiceRendezVous;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Scanner;

public class Main {

    /* ===================== OUTILS DE CONTRÔLE ===================== */

    private static int lireId(Scanner sc, String message) {
        while (true) {
            System.out.print(message);
            String input = sc.nextLine().trim();

            if (!input.matches("\\d+")) {
                System.out.println("❌ Entrez uniquement des chiffres (pas de lettres).");
                continue;
            }

            int val = Integer.parseInt(input);
            if (val <= 0) {
                System.out.println("❌ Entrez un nombre > 0.");
                continue;
            }

            return val;
        }
    }

    private static String lireTexteNonVide(Scanner sc, String message) {
        while (true) {
            System.out.print(message);
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("❌ Champ obligatoire.");
        }
    }

    private static Date lireDate(Scanner sc, String message) {
        while (true) {
            System.out.print(message);
            try {
                return Date.valueOf(sc.nextLine().trim());
            } catch (IllegalArgumentException e) {
                System.out.println("❌ Format invalide. Exemple : 2026-02-20");
            }
        }
    }

    private static Time lireTime(Scanner sc, String message) {
        while (true) {
            System.out.print(message);
            try {
                String s = sc.nextLine().trim();
                if (s.matches("\\d{2}:\\d{2}")) s += ":00";
                return Time.valueOf(s);
            } catch (IllegalArgumentException e) {
                System.out.println("❌ Format invalide. Exemple : 10:30 ou 10:30:00");
            }
        }
    }

    private static RendezVous.StatutRV lireStatutRV(Scanner sc) {
        while (true) {
            System.out.print("• statut (prevu / termine / annule) : ");
            String s = sc.nextLine().toLowerCase();

            switch (s) {
                case "prevu":
                    return RendezVous.StatutRV.prevu;
                case "termine":
                    return RendezVous.StatutRV.termine;
                case "annule":
                    return RendezVous.StatutRV.annule;
                default:
                    System.out.println("❌ Statut invalide.");
            }
        }
    }

    private static RendezVous.TypeRV lireTypeRV(Scanner sc) {
        while (true) {
            System.out.print("• type (premiere_consultation / suivi / urgence) : ");
            String s = sc.nextLine().toLowerCase();

            switch (s) {
                case "premiere_consultation":
                    return RendezVous.TypeRV.premiere_consultation;
                case "suivi":
                    return RendezVous.TypeRV.suivi;
                case "urgence":
                    return RendezVous.TypeRV.urgence;
                default:
                    System.out.println("❌ Type invalide.");
            }
        }
    }

    private static CompteRenduSeance.ProgresCR lireProgresCR(Scanner sc) {
        while (true) {
            System.out.print("• progrès (stagnation / amelioration_legere / amelioration_significative / amelioration_stable) : ");
            String s = sc.nextLine().toLowerCase();

            switch (s) {
                case "stagnation":
                    return CompteRenduSeance.ProgresCR.stagnation;
                case "amelioration_legere":
                    return CompteRenduSeance.ProgresCR.amelioration_legere;
                case "amelioration_significative":
                    return CompteRenduSeance.ProgresCR.amelioration_significative;
                case "amelioration_stable":
                    return CompteRenduSeance.ProgresCR.amelioration_stable;
                default:
                    System.out.println("❌ Valeur invalide.");
            }
        }
    }

    /* ===================== MAIN ===================== */

    public static void main(String[] args) {

        ServiceRendezVous serviceRV = new ServiceRendezVous();
        ServiceCompteRenduSeance serviceCR = new ServiceCompteRenduSeance();
        Scanner sc = new Scanner(System.in);

        try {
            /* ===== CREATE RENDEZ-VOUS ===== */
            System.out.println("👉 AJOUT RENDEZ-VOUS");

            int idPatient;
            while (true) {
                idPatient = lireId(sc, "• id patient : ");
                if (serviceRV.userExiste(idPatient, "patient")) break;
                System.out.println("❌ Patient introuvable (id inexistant ou rôle ≠ patient).");
            }

            int idPsy;
            while (true) {
                idPsy = lireId(sc, "• id psychologue : ");
                if (serviceRV.userExiste(idPsy, "psychologue")) break;
                System.out.println("❌ Psychologue introuvable (id inexistant ou rôle ≠ psychologue).");
            }

            RendezVous.StatutRV statut = lireStatutRV(sc);
            RendezVous.TypeRV type = lireTypeRV(sc);
            Date date = lireDate(sc, "• date (YYYY-MM-DD) : ");
            Time time = lireTime(sc, "• heure (HH:MM ou HH:MM:SS) : ");

            serviceRV.add(new RendezVous(idPatient, idPsy, statut, date, type, time));
            System.out.println("✅ Rendez-vous ajouté");

            /* ===== READ RENDEZ-VOUS ===== */
            System.out.println("👉 LISTE DES RENDEZ-VOUS");
            serviceRV.getAll().forEach(System.out::println);

            /* ===== UPDATE RENDEZ-VOUS (SANS patient/psy) ===== */
            System.out.println("👉 MODIFIER RENDEZ-VOUS (statut + type + date + heure)");
            System.out.println("ℹ️ Consulte l'id dans MySQL");

            int idRvModif;
            while (true) {
                idRvModif = lireId(sc, "• id_rv à modifier : ");
                if (serviceRV.rendezVousExiste(idRvModif)) break;
                System.out.println("❌ Rendez-vous introuvable (id_rv n'existe pas).");
            }

            RendezVous.StatutRV newStatut = lireStatutRV(sc);
            RendezVous.TypeRV newType = lireTypeRV(sc);
            Date newDate = lireDate(sc, "• nouvelle date (YYYY-MM-DD) : ");
            Time newTime = lireTime(sc, "• nouvelle heure (HH:MM ou HH:MM:SS) : ");

            serviceRV.updateFields(idRvModif, newStatut, newType, newDate, newTime);

            /* ===== CREATE COMPTE RENDU ===== */
            System.out.println("👉 AJOUT COMPTE RENDU");

            int idRv = lireId(sc, "• id du rendez-vous : ");
            CompteRenduSeance.ProgresCR progres = lireProgresCR(sc);
            String resume = lireTexteNonVide(sc, "• résumé : ");
            String actions = lireTexteNonVide(sc, "• actions : ");

            serviceCR.add(new CompteRenduSeance(
                    idRv,
                    new Timestamp(System.currentTimeMillis()),
                    progres,
                    resume,
                    actions
            ));
            System.out.println("✅ Compte rendu ajouté");

            /* ===== READ COMPTE RENDU ===== */
            System.out.println("👉 LISTE DES COMPTES RENDUS");
            serviceCR.getAll().forEach(System.out::println);

            /* ===== UPDATE COMPTE RENDU (SANS changer id_rv) ===== */
            System.out.println("👉 MODIFIER COMPTE RENDU (progrès + résumé + actions)");
            System.out.println("ℹ️ Consulte l'id dans MySQL");

            int idCrModif;
            while (true) {
                idCrModif = lireId(sc, "• id_compterendu à modifier : ");
                if (serviceCR.compteRenduExiste(idCrModif)) break;
                System.out.println("❌ Compte rendu introuvable (id_compterendu n'existe pas).");
            }

            CompteRenduSeance.ProgresCR newProg = lireProgresCR(sc);
            String newResume = lireTexteNonVide(sc, "• nouveau résumé : ");
            String newActions = lireTexteNonVide(sc, "• nouvelles actions : ");

            serviceCR.updateFields(idCrModif, newProg, newResume, newActions);

            /* ===== DELETE RENDEZ-VOUS (CASCADE) ===== */
            System.out.println("👉 SUPPRIMER RENDEZ-VOUS (CASCADE)");
            System.out.println("ℹ️ Consulte l'id dans MySQL");

            int idRvSupp;
            while (true) {
                idRvSupp = lireId(sc, "• id_rv à supprimer : ");
                if (serviceRV.rendezVousExiste(idRvSupp)) break;
                System.out.println("❌ Rendez-vous introuvable (id_rv n'existe pas).");
            }

            RendezVous rvDel = new RendezVous();
            rvDel.setIdRv(idRvSupp);

            serviceRV.delete(rvDel);
            System.out.println("✅ Rendez-vous supprimé");
            System.out.println("ℹ️ Les comptes rendus liés sont supprimés automatiquement (ON DELETE CASCADE)");

            /* ===== ÉTAT FINAL ===== */
            System.out.println("👉 RENDEZ-VOUS FINAUX");
            serviceRV.getAll().forEach(System.out::println);

            System.out.println("\n👉 COMPTES RENDUS FINAUX");
            serviceCR.getAll().forEach(System.out::println);

            System.out.println("\n🎉 CRUD COMPLET TERMINÉ AVEC SUCCÈS");

        } catch (Exception e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        } finally {
            sc.close();
        }
    }
}
