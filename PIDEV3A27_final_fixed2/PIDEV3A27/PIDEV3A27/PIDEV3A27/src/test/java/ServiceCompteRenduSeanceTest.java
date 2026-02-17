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

    private int idRv = -1;
    private int idCr = -1;

    @BeforeAll
    public static void setup() {
        cnx = MyDatabase.getInstance().getConnection();
        crService = new ServiceCompteRenduSeance(cnx);
        rvService = new ServiceRendezVous(cnx);
        System.out.println("[DEBUG_LOG] Setup OK (CompteRendu)");
    }

    // ✅ Pause démo prof
    private void pauseForDemo(String msg) throws InterruptedException {
        System.out.println("🔎 " + msg);
        System.out.println("⏳ Waiting 15 seconds...");
        Thread.sleep(15000);
    }

    @AfterEach
    void cleanUp() throws SQLException {
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

        idRv = createRvForTest();

        CompteRenduSeance cr = new CompteRenduSeance();
        cr.setIdAppointment(idRv);
        cr.setDateCreationCr(new Timestamp(System.currentTimeMillis()));
        cr.setProgresCr(CompteRenduSeance.ProgresCR.amelioration_stable);
        cr.setResumeSeanceCr("Résumé test JUnit");
        cr.setProchainesActionCr("Action test JUnit");

        idCr = crService.addAndReturnId(cr);

        pauseForDemo("Va dans phpMyAdmin → table compte_rendu_seance → vérifie ID = " + idCr);

        assertTrue(idCr > 0);
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
