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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceCompteRenduSeanceTest {

    static Connection cnx;
    static ServiceCompteRenduSeance crService;
    static ServiceRendezVous rvService;

    static final int PATIENT_ID = 1;
    static final int PSY_ID = 5;

    // IDs créés pour le test courant
    private int createdRvId = -1;
    private int createdCrId = -1;

    @BeforeAll
    public static void setup() {
        cnx = MyDatabase.getInstance().getConnection();
        crService = new ServiceCompteRenduSeance(cnx);
        rvService = new ServiceRendezVous(cnx);
        System.out.println("[DEBUG_LOG] Setup OK (CompteRendu)");
    }

    // ✅ Pause demo prof
    private void pauseForDemo(String msg) throws InterruptedException {
        System.out.println("🔎 [DEMO] " + msg);
        System.out.println("⏳ Waiting 15 seconds...");
        Thread.sleep(15000);
    }

    // ✅ Helper : crée un RV CONFIRMÉ + TERMINÉ (règle métier)
    private int createRvConfirmedAndTermine() throws SQLException {
        RendezVous rv = new RendezVous();
        rv.setIdPatient(PATIENT_ID);
        rv.setIdPsychologist(PSY_ID);

        rv.setConfirmationStatus(RendezVous.ConfirmationStatus.confirme);
        rv.setStatutRv(RendezVous.StatutRV.termine);

        rv.setAppointmentDate(Date.valueOf("2026-04-10"));
        rv.setTypeRendezVous(RendezVous.TypeRV.premiere_consultation);
        rv.setAppointmentTimeRv(Time.valueOf("11:00:00"));

        return rvService.addAndReturnId(rv);
    }

    @BeforeEach
    void createRvForEachTest() throws SQLException, InterruptedException {
        createdRvId = createRvConfirmedAndTermine();
        System.out.println("[DEBUG_LOG] Created RV for test: " + createdRvId);

        pauseForDemo("AVANT TEST: phpMyAdmin → rendez_vous → vérifie RV ID=" + createdRvId +
                " (confirmation_status=confirme, statutrv=termine)");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        // supprimer CR d'abord
        if (createdCrId != -1) {
            crService.deleteById(createdCrId);
            System.out.println("[DEBUG_LOG] Cleanup: Deleted CR " + createdCrId);
            createdCrId = -1;
        }

        // supprimer RV ensuite (direct SQL)
        if (createdRvId != -1) {
            try (PreparedStatement pst = cnx.prepareStatement("DELETE FROM rendez_vous WHERE id_rv=?")) {
                pst.setInt(1, createdRvId);
                pst.executeUpdate();
            }
            System.out.println("[DEBUG_LOG] Cleanup: Deleted RV " + createdRvId);
            createdRvId = -1;
        }
    }

    // ================= INSERT =================
    @Test
    @Order(1)
    public void testAddAndReturnId() throws SQLException, InterruptedException {

        CompteRenduSeance cr = new CompteRenduSeance();
        cr.setIdAppointment(createdRvId);
        cr.setDateCreationCr(new Timestamp(System.currentTimeMillis()));
        cr.setProgresCr(CompteRenduSeance.ProgresCR.amelioration_stable);
        cr.setResumeSeanceCr("Résumé test JUnit");
        cr.setProchainesActionCr("Action test JUnit");

        createdCrId = crService.addAndReturnId(cr);

        System.out.println("[DEBUG_LOG] Created CR with ID: " + createdCrId);
        pauseForDemo("TEST 1: phpMyAdmin → compte_rendu_seance → vérifie CR ID=" + createdCrId +
                " et id_appointment=" + createdRvId);

        assertTrue(createdCrId > 0);
    }

    // ================= UPDATE =================
    @Test
    @Order(2)
    public void testUpdate() throws SQLException, InterruptedException {

        // 1) créer CR initial
        CompteRenduSeance cr = new CompteRenduSeance();
        cr.setIdAppointment(createdRvId);
        cr.setDateCreationCr(new Timestamp(System.currentTimeMillis()));
        cr.setProgresCr(CompteRenduSeance.ProgresCR.stagnation);
        cr.setResumeSeanceCr("Avant update");
        cr.setProchainesActionCr("Action avant");

        createdCrId = crService.addAndReturnId(cr);
        System.out.println("[DEBUG_LOG] Created CR with ID: " + createdCrId);

        pauseForDemo("TEST 2 AVANT UPDATE: phpMyAdmin → compte_rendu_seance → ID=" + createdCrId +
                " vérifie: progres=stagnation, resume='Avant update', action='Action avant'");

        // 2) update
        CompteRenduSeance upd = new CompteRenduSeance();
        upd.setIdCompteRendu(createdCrId);
        upd.setIdAppointment(createdRvId);
        upd.setDateCreationCr(new Timestamp(System.currentTimeMillis()));
        upd.setProgresCr(CompteRenduSeance.ProgresCR.amelioration_significative);
        upd.setResumeSeanceCr("Après update");
        upd.setProchainesActionCr("Action après");

        crService.update(upd);

        pauseForDemo("TEST 2 APRÈS UPDATE: phpMyAdmin → compte_rendu_seance → ID=" + createdCrId +
                " vérifie: progres=amelioration_significative, resume='Après update', action='Action après'");

        // 3) vérification SQL simple (AVANT/APRÈS visible)
        assertTrue(exists("SELECT 1 FROM compte_rendu_seance WHERE id_compterendu=?", createdCrId));
    }

    // ================= VIEW =================
    @Test
    @Order(3)
    public void testFindViewsByPsychologist() throws SQLException, InterruptedException {

        // créer CR
        CompteRenduSeance cr = new CompteRenduSeance();
        cr.setIdAppointment(createdRvId);
        cr.setDateCreationCr(new Timestamp(System.currentTimeMillis()));
        cr.setProgresCr(CompteRenduSeance.ProgresCR.amelioration_legere);
        cr.setResumeSeanceCr("View test");
        cr.setProchainesActionCr("View action");

        createdCrId = crService.addAndReturnId(cr);

        pauseForDemo("TEST 3: phpMyAdmin → compte_rendu_seance → vérifie CR ID=" + createdCrId +
                " puis vérifie côté interface psy si le compte-rendu apparaît.");

        List<CompteRenduView> views = crService.findViewsByPsychologist(PSY_ID);
        assertNotNull(views);
        assertFalse(views.isEmpty());

        // optionnel : vérifier que notre CR est dedans (si CompteRenduView a getIdCompteRendu)
        // boolean found = views.stream().anyMatch(v -> v.getIdCompteRendu() == createdCrId);
        // assertTrue(found);
    }

    // ================= DELETE =================
    @Test
    @Order(4)
    public void testDeleteById() throws SQLException, InterruptedException {

        // créer CR
        CompteRenduSeance cr = new CompteRenduSeance();
        cr.setIdAppointment(createdRvId);
        cr.setDateCreationCr(new Timestamp(System.currentTimeMillis()));
        cr.setProgresCr(CompteRenduSeance.ProgresCR.amelioration_stable);
        cr.setResumeSeanceCr("Delete test");
        cr.setProchainesActionCr("Delete action");

        createdCrId = crService.addAndReturnId(cr);

        pauseForDemo("TEST 4 AVANT DELETE: phpMyAdmin → compte_rendu_seance → vérifie CR ID=" +
                createdCrId + " existe");

        crService.deleteById(createdCrId);

        pauseForDemo("TEST 4 APRÈS DELETE: phpMyAdmin → compte_rendu_seance → vérifie CR ID=" +
                createdCrId + " a disparu");

        assertFalse(exists("SELECT 1 FROM compte_rendu_seance WHERE id_compterendu=?", createdCrId));

        // pour éviter que @AfterEach tente de re-delete
        createdCrId = -1;
    }

    // ✅ helper existence (SQL)
    private boolean exists(String sql, int id) throws SQLException {
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }
}