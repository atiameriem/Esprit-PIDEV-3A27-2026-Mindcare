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

    // id du RV créé dans @BeforeEach (à supprimer en @AfterEach)
    private int createdId = -1;

    @BeforeAll
    static void setup() {
        cnx = MyDatabase.getInstance().getConnection();
        srv = new ServiceRendezVous(cnx);
        System.out.println("[DEBUG_LOG] Setup OK");
    }

    // ✅ Pause demo prof (utilisée dans CHAQUE test)
    private void pauseForDemo(String msg) throws InterruptedException {
        System.out.println("🔎 [DEMO] " + msg);
        System.out.println("⏳ Waiting 15 seconds...");
        Thread.sleep(15000);
    }

    @BeforeEach
    void createRvForTest() throws SQLException, InterruptedException {
        RendezVous rv = new RendezVous();
        rv.setIdPatient(PATIENT_ID);
        rv.setIdPsychologist(PSY_ID);

        // ✅ patient ne choisit pas statut
        rv.setStatutRv(null);

        rv.setAppointmentDate(Date.valueOf("2026-04-04"));
        rv.setTypeRendezVous(RendezVous.TypeRV.premiere_consultation);
        rv.setAppointmentTimeRv(Time.valueOf("10:30:00"));

        // ✅ patient -> en_attente
        rv.setConfirmationStatus(RendezVous.ConfirmationStatus.en_attente);

        createdId = srv.addAndReturnId(rv);

        System.out.println("[DEBUG_LOG] Created RV for test with ID: " + createdId);
        pauseForDemo("AVANT TEST: phpMyAdmin → rendez_vous → vérifie ID=" + createdId +
                " (confirmation_status=en_attente)");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (createdId != -1) {
            // supprimer CR lié au RV (si existe)
            try (PreparedStatement pst = cnx.prepareStatement(
                    "DELETE FROM compte_rendu_seance WHERE id_appointment=?")) {
                pst.setInt(1, createdId);
                pst.executeUpdate();
            }

            // supprimer RV
            try (PreparedStatement pst = cnx.prepareStatement(
                    "DELETE FROM rendez_vous WHERE id_rv=?")) {
                pst.setInt(1, createdId);
                pst.executeUpdate();
            }

            System.out.println("[DEBUG_LOG] Cleanup: Deleted RV " + createdId);
            createdId = -1;
        }
    }

    @Test
    @Order(1)
    public void testAddAndReturnId() throws SQLException, InterruptedException {
        // Ici, l'add est déjà fait dans @BeforeEach
        assertTrue(createdId > 0);

        List<RendezVous> list = srv.findByPatient(PATIENT_ID);
        assertFalse(list.isEmpty());

        boolean found = list.stream().anyMatch(x ->
                x.getIdRv() == createdId &&
                        x.getConfirmationStatus() == RendezVous.ConfirmationStatus.en_attente
        );

        pauseForDemo("TEST 1: Vérifie que RV(ID=" + createdId + ") est bien présent " +
                "et confirmation_status=en_attente");

        assertTrue(found, "Created RV should exist in DB with confirmation_status=en_attente");
    }

    @Test
    @Order(2)
    public void testUpdateForPatient() throws SQLException, InterruptedException {
        RendezVous upd = new RendezVous();
        upd.setIdRv(createdId);
        upd.setIdPatient(PATIENT_ID);
        upd.setIdPsychologist(PSY_ID);

        // ✅ patient ne touche pas statutrv
        upd.setStatutRv(null);

        upd.setAppointmentDate(Date.valueOf("2026-04-05"));
        upd.setTypeRendezVous(RendezVous.TypeRV.suivi);
        upd.setAppointmentTimeRv(Time.valueOf("12:00:00"));

        // ✅ garder en_attente
        upd.setConfirmationStatus(RendezVous.ConfirmationStatus.en_attente);

        srv.updateForPatient(upd, PATIENT_ID);

        pauseForDemo("TEST 2 APRÈS UPDATE: phpMyAdmin → rendez_vous → ID=" + createdId +
                " vérifie: date=2026-04-05, time=12:00:00, type=suivi, confirmation_status=en_attente");

        List<RendezVous> list = srv.findByPatient(PATIENT_ID);

        boolean found = list.stream().anyMatch(x ->
                x.getIdRv() == createdId
                        && x.getTypeRendezVous() == RendezVous.TypeRV.suivi
                        && "12:00:00".equals(x.getAppointmentTimeRv().toString())
                        && x.getConfirmationStatus() == RendezVous.ConfirmationStatus.en_attente
        );

        assertTrue(found, "RV should be updated (patient) while still en_attente");
    }

    @Test
    @Order(3)
    public void testFindViewsByPsychologist() throws SQLException, InterruptedException {
        List<RendezVousView> views = srv.findViewsByPsychologist(PSY_ID);
        assertNotNull(views);
        assertFalse(views.isEmpty());

        RendezVousView v = views.stream()
                .filter(x -> x.getIdRv() == createdId)
                .findFirst()
                .orElse(null);

        pauseForDemo("TEST 3: phpMyAdmin → vérifie que le RV(ID=" + createdId + ") appartient au psy " +
                PSY_ID + " et que la jointure affiche les noms côté interface psy.");

        assertNotNull(v, "The created RV should appear in psychologist views");

        assertNotNull(v.getPatientFullName());
        assertFalse(v.getPatientFullName().isBlank());

        assertNotNull(v.getPsychologistFullName());
        assertFalse(v.getPsychologistFullName().isBlank());

        assertEquals(RendezVous.ConfirmationStatus.en_attente, v.getConfirmationStatus());
    }

    @Test
    @Order(4)
    public void testDeleteForPatientAlsoDeletesCompteRendu() throws SQLException, InterruptedException {

        // 1) créer un Compte Rendu lié au RV déjà créé par @BeforeEach
        String sql = """
            INSERT INTO compte_rendu_seance (id_appointment, date_creationcr, progrescr, resumeseancecr, prochainesactioncr)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, createdId);
            pst.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pst.setString(3, CompteRenduSeance.ProgresCR.amelioration_stable.name());
            pst.setString(4, "Résumé test JUnit");
            pst.setString(5, "Action test JUnit");
            pst.executeUpdate();
        }

        assertTrue(exists("SELECT 1 FROM compte_rendu_seance WHERE id_appointment=?", createdId));
        assertTrue(exists("SELECT 1 FROM rendez_vous WHERE id_rv=?", createdId));

        pauseForDemo("TEST 4 AVANT DELETE: phpMyAdmin → RV(ID=" + createdId + ") existe " +
                "ET compte_rendu_seance(id_appointment=" + createdId + ") existe");

        // 2) delete
        srv.deleteForPatient(createdId, PATIENT_ID);

        pauseForDemo("TEST 4 APRÈS DELETE: phpMyAdmin → RV(ID=" + createdId + ") supprimé " +
                "ET compte_rendu_seance(id_appointment=" + createdId + ") supprimé");

        assertFalse(exists("SELECT 1 FROM compte_rendu_seance WHERE id_appointment=?", createdId));
        assertFalse(exists("SELECT 1 FROM rendez_vous WHERE id_rv=?", createdId));

        // ✅ très important: pour éviter que @AfterEach tente de resupprimer
        createdId = -1;
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