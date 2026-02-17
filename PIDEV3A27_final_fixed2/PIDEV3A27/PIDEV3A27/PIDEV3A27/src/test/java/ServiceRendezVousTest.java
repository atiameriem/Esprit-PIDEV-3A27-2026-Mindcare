import models.CompteRenduSeance;
import models.RendezVous;
import models.RendezVousView;
import org.junit.jupiter.api.*;
import services.ServiceRendezVous;
import utils.MyDatabase;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceRendezVousTest {

    static Connection cnx;
    static ServiceRendezVous srv;

    // ✅ change ces IDs pour correspondre à ta DB (doivent exister)
    static final int PATIENT_ID = 1;
    static final int PSY_ID = 5;

    private int idRv = -1;

    @BeforeAll
    public static void setup() {
        cnx = MyDatabase.getInstance().getConnection();
        srv = new ServiceRendezVous(cnx);
        System.out.println("[DEBUG_LOG] Setup OK");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idRv != -1) {
            // au cas où un CR existe encore
            try (PreparedStatement pst = cnx.prepareStatement(
                    "DELETE FROM compte_rendu_seance WHERE id_appointment=?")) {
                pst.setInt(1, idRv);
                pst.executeUpdate();
            }
            try (PreparedStatement pst = cnx.prepareStatement(
                    "DELETE FROM rendez_vous WHERE id_rv=?")) {
                pst.setInt(1, idRv);
                pst.executeUpdate();
            }
            System.out.println("[DEBUG_LOG] Cleanup: Deleted RV " + idRv);
            idRv = -1;
        }
    }

    // ✅ Pause demo prof
    private void pauseForDemo(String msg) throws InterruptedException {
        System.out.println("🔎 [DEMO] " + msg);
        System.out.println("⏳ Waiting 15 seconds...");
        Thread.sleep(15000);
    }

    @Test
    @Order(1)
    public void testAddAndReturnId() throws SQLException, InterruptedException {
        RendezVous rv = new RendezVous();
        rv.setIdPatient(PATIENT_ID);
        rv.setIdPsychologist(PSY_ID);
        rv.setStatutRv(RendezVous.StatutRV.prevu);
        rv.setAppointmentDate(Date.valueOf("2026-04-04"));
        rv.setTypeRendezVous(RendezVous.TypeRV.premiere_consultation);
        rv.setAppointmentTimeRv(Time.valueOf("10:30:00"));

        int id = srv.addAndReturnId(rv);
        idRv = id;

        System.out.println("[DEBUG_LOG] Created RV with ID: " + id);
        pauseForDemo("Va dans phpMyAdmin → table rendez_vous et trouve l'ID = " + id);

        assertTrue(id > 0);

        List<RendezVous> list = srv.findByPatient(PATIENT_ID);
        assertFalse(list.isEmpty());

        boolean found = list.stream().anyMatch(x -> x.getIdRv() == id);
        assertTrue(found, "Created RV should exist in DB");
    }

    @Test
    @Order(2)
    public void testUpdateForPatient() throws SQLException, InterruptedException {
        // create first
        RendezVous rv = new RendezVous();
        rv.setIdPatient(PATIENT_ID);
        rv.setIdPsychologist(PSY_ID);
        rv.setStatutRv(RendezVous.StatutRV.prevu);
        rv.setAppointmentDate(Date.valueOf("2026-04-04"));
        rv.setTypeRendezVous(RendezVous.TypeRV.premiere_consultation);
        rv.setAppointmentTimeRv(Time.valueOf("10:30:00"));
        int id = srv.addAndReturnId(rv);
        idRv = id;

        // update
        RendezVous upd = new RendezVous();
        upd.setIdRv(id);
        upd.setIdPatient(PATIENT_ID);
        upd.setIdPsychologist(PSY_ID);
        upd.setStatutRv(RendezVous.StatutRV.termine);
        upd.setAppointmentDate(Date.valueOf("2026-04-05"));
        upd.setTypeRendezVous(RendezVous.TypeRV.suivi);
        upd.setAppointmentTimeRv(Time.valueOf("12:00:00"));

        srv.updateForPatient(upd, PATIENT_ID);

        System.out.println("[DEBUG_LOG] Updated RV ID: " + id);
        pauseForDemo("Va dans phpMyAdmin → table rendez_vous → ID = " + id +
                " et vérifie: type=suivi, statut=termine, time=12:00:00, date=2026-04-05");

        List<RendezVous> list = srv.findByPatient(PATIENT_ID);
        boolean found = list.stream().anyMatch(x ->
                x.getIdRv() == id
                        && x.getTypeRendezVous() == RendezVous.TypeRV.suivi
                        && x.getStatutRv() == RendezVous.StatutRV.termine
                        && "12:00:00".equals(x.getAppointmentTimeRv().toString())
        );

        assertTrue(found, "RV should be updated");
    }

    @Test
    @Order(3)
    public void testFindViewsByPsychologist() throws SQLException {
        // create first
        RendezVous rv = new RendezVous();
        rv.setIdPatient(PATIENT_ID);
        rv.setIdPsychologist(PSY_ID);
        rv.setStatutRv(RendezVous.StatutRV.prevu);
        rv.setAppointmentDate(Date.valueOf("2026-04-06"));
        rv.setTypeRendezVous(RendezVous.TypeRV.urgence);
        rv.setAppointmentTimeRv(Time.valueOf("09:00:00"));
        int id = srv.addAndReturnId(rv);
        idRv = id;

        List<RendezVousView> views = srv.findViewsByPsychologist(PSY_ID);
        assertFalse(views.isEmpty());

        RendezVousView v = views.stream().filter(x -> x.getIdRv() == id).findFirst().orElse(null);
        assertNotNull(v);

        assertNotNull(v.getPatientFullName());
        assertFalse(v.getPatientFullName().isBlank());

        assertNotNull(v.getPsychologistFullName());
        assertFalse(v.getPsychologistFullName().isBlank());
    }



    private boolean exists(String sql, int id) throws SQLException {
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }
}
