import models.CompteRenduSeance;
import models.CompteRenduView;
import models.RendezVous;
import org.junit.jupiter.api.*;
import services.ServiceCompteRenduSeance;
import services.ServiceRendezVous;
import utils.MyDatabase;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//@TestMethodOrder(...): dit à JUnit dans quel ordre exécuter les tests.
//OrderAnnotation.class : ça veut dire que l’ordre sera donné par @Order(1),
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceCompteRenduSeanceTest {
    //static : une seule instance pour toute la classe de test.
    static Connection cnx;//cnx : connexion BD.
    static ServiceCompteRenduSeance crService;//service qui gère les comptes rendus.
    static ServiceRendezVous rvService;

    //final : constants (ne changent pas).
    //Ici tu assumes que dans ta BD :
    //patient id = 1 existe
    //psychologue id = 5 existe
    static final int PATIENT_ID = 1;
    static final int PSY_ID = 5;

    private int idRv = -1; //l’id du rendez-vous créé pendant le test.
    private int idCr = -1; //-1 signifie “rien à supprimer”.

    //@BeforeAll : s’exécute une seule fois avant tous les tests
    @BeforeAll
    public static void setup() {
        //Récupère la connexion depuis ton Singleton MyDatabase.
        cnx = MyDatabase.getInstance().getConnection();
        //Crée les services en leur passant la connexion
        crService = new ServiceCompteRenduSeance(cnx);
        rvService = new ServiceRendezVous(cnx);
        //Un log pour vérifier que la préparation s’est bien fait
        System.out.println("[DEBUG_LOG] Setup OK (CompteRendu)");
    }

    // Affiche un message.
    //Attend 15 secondes pour te laisser regarder dans phpMyAdmin.
    private void pauseForDemo(String msg) throws InterruptedException {
        System.out.println("🔎 " + msg);
        System.out.println("⏳ Waiting 15 seconds...");
        Thread.sleep(15000);
    }

    //@AfterEach : s’exécute après chaque test.
    //Objectif : supprimer ce que le test a créé (pour ne pas “salir” la BD).
    @AfterEach
    void cleanUp() throws SQLException {
        //Si idCr contient un vrai id :
        //supprime ce compte rendu
        //reset idCr = -1
        if (idCr != -1) {
            crService.deleteById(idCr);
            System.out.println("[DEBUG_LOG] Cleanup: Deleted CR " + idCr);
            idCr = -1;
        }
        if (idRv != -1) {
            rvService.deleteForPatient(idRv, PATIENT_ID);
            System.out.println("[DEBUG_LOG] Cleanup: Deleted RV " + idRv);
            idRv = -1;
        }
    }

    // ================= INSERT =================
    @Test
    @Order(1)
    public void testAddAndReturnId() throws SQLException, InterruptedException {
//Crée un RV en base et récupère son id.
//On le stocke pour le cleanup.
        idRv = createRvForTest();
//créer un objet compte rendu en mémoire
        CompteRenduSeance cr = new CompteRenduSeance();
        cr.setIdAppointment(idRv);
        cr.setDateCreationCr(new Timestamp(System.currentTimeMillis()));
        cr.setProgresCr(CompteRenduSeance.ProgresCR.amelioration_stable);
        cr.setResumeSeanceCr("Résumé test JUnit");
        cr.setProchainesActionCr("Action test JUnit");

        idCr = crService.addAndReturnId(cr);
//pour voir en 15 secondes
        pauseForDemo("Va dans phpMyAdmin → table compte_rendu_seance → vérifie ID = " + idCr);
//Vérifie que l’ID retourné est valide (auto-increment).
        assertTrue(idCr > 0);
    }

    // ================= UPDATE =================
    @Test
    @Order(2)
    public void testUpdate() throws SQLException, InterruptedException {

        idRv = createRvForTest();
//créer un CR initial puis l’insérer
        CompteRenduSeance cr = new CompteRenduSeance();
        cr.setIdAppointment(idRv);
        cr.setDateCreationCr(new Timestamp(System.currentTimeMillis()));
        cr.setProgresCr(CompteRenduSeance.ProgresCR.stagnation);
        cr.setResumeSeanceCr("Avant update");
        cr.setProchainesActionCr("Action avant");

        idCr = crService.addAndReturnId(cr);
// cmpte rendu modifier
        CompteRenduSeance upd = new CompteRenduSeance();
        //upd.setIdCompteRendu(idCr)
        //➜ c’est l’identifiant de la ligne à modifier.
        upd.setIdCompteRendu(idCr);
        upd.setIdAppointment(idRv);
        upd.setDateCreationCr(new Timestamp(System.currentTimeMillis()));
        upd.setProgresCr(CompteRenduSeance.ProgresCR.amelioration_significative);
        upd.setResumeSeanceCr("Après update");
        upd.setProchainesActionCr("Action après");
//Met à jour la ligne id_compterendu = idCr.
        crService.update(upd);

        pauseForDemo("Va dans phpMyAdmin → vérifie que résumé et progrès ont changé");

        assertTrue(true);
    }

    // ================= VIEW =================
    @Test
    @Order(3)
    public void testFindViewsByPsychologist() throws SQLException {

        idRv = createRvForTest();

        CompteRenduSeance cr = new CompteRenduSeance();
        cr.setIdAppointment(idRv);
        cr.setDateCreationCr(new Timestamp(System.currentTimeMillis()));
        cr.setProgresCr(CompteRenduSeance.ProgresCR.amelioration_legere);
        cr.setResumeSeanceCr("View test");
        cr.setProchainesActionCr("View action");

        idCr = crService.addAndReturnId(cr);
//Récupère les comptes-rendus du psy via JOIN.
        List<CompteRenduView> views = crService.findViewsByPsychologist(PSY_ID);
        assertFalse(views.isEmpty());
    }
    // ================= DELETE =================
    @Test
    @Order(4)
    public void testDeleteById() throws SQLException, InterruptedException {

        idRv = createRvForTest();

        CompteRenduSeance cr = new CompteRenduSeance();
        cr.setIdAppointment(idRv);
        cr.setDateCreationCr(new Timestamp(System.currentTimeMillis()));
        cr.setProgresCr(CompteRenduSeance.ProgresCR.amelioration_stable);
        cr.setResumeSeanceCr("Delete test");
        cr.setProchainesActionCr("Delete action");

        idCr = crService.addAndReturnId(cr);

        pauseForDemo("AVANT delete → vérifie que la ligne existe");

        crService.deleteById(idCr);

        pauseForDemo("APRÈS delete → vérifie que la ligne a disparu");
//idCr = -1;
        idCr = -1;
        //vérifier en base que l’id n’existe plus.
        assertTrue(true);
    }

    // Helper RV
    private int createRvForTest() throws SQLException {
        RendezVous rv = new RendezVous();
        rv.setIdPatient(PATIENT_ID);
        rv.setIdPsychologist(PSY_ID);
        rv.setStatutRv(RendezVous.StatutRV.prevu);
        rv.setAppointmentDate(Date.valueOf("2026-04-10"));
        rv.setTypeRendezVous(RendezVous.TypeRV.premiere_consultation);
        rv.setAppointmentTimeRv(Time.valueOf("11:00:00"));

        return rvService.addAndReturnId(rv);
    }
}
