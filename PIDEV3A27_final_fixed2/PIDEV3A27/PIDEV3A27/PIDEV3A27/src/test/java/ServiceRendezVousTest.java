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
    static ServiceRendezVous srv; //srv : service des rendez-vous à tester.

    // ✅ change ces IDs pour correspondre à ta DB (doivent exister)
    static final int PATIENT_ID = 1;
    static final int PSY_ID = 5;

    //idRv garde l’id du rendez-vous créé dans un test.
    //-1 = rien à supprimer.
    private int idRv = -1;

    @BeforeAll
    public static void setup() {
        //récupère la connexion à la base.
        cnx = MyDatabase.getInstance().getConnection();
        //crée le service rendez-vous avec cette connexion.
        srv = new ServiceRendezVous(cnx);
        System.out.println("[DEBUG_LOG] Setup OK");
    }
//@AfterEach : s’exécute après chaque test.
    //Objectif : supprimer ce qui a été créé pendant le test.
    @AfterEach
    void cleanUp() throws SQLException {
        if (idRv != -1) {
            // au cas où un CR existe encore
            //supprime tous les comptes rendus dont id_appointment = idRv
            try (PreparedStatement pst = cnx.prepareStatement(
                    "DELETE FROM compte_rendu_seance WHERE id_appointment=?")) {
                pst.setInt(1, idRv);
                pst.executeUpdate();
            }
            //Supprimer ensuite le rendez-vous
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

        //insert en base.
        //idRv = id pour cleanup après test.
        int id = srv.addAndReturnId(rv);
        idRv = id;

        System.out.println("[DEBUG_LOG] Created RV with ID: " + id);
        pauseForDemo("Va dans phpMyAdmin → table rendez_vous et trouve l'ID = " + id);
//vérifie que l’id auto-incrémenté est valide.
        assertTrue(id > 0);

//récupère la liste des RV du patient.
//vérifie qu’elle n’est pas vide.
        List<RendezVous> list = srv.findByPatient(PATIENT_ID);
        assertFalse(list.isEmpty());

        //cherche dans la liste si l’id créé est présent.
        //si absent → test échoue.
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
        //setIdRv(id) :→ indique quel RV modifier.
        upd.setIdRv(id);
        upd.setIdPatient(PATIENT_ID);
        upd.setIdPsychologist(PSY_ID);
        upd.setStatutRv(RendezVous.StatutRV.termine);
        upd.setAppointmentDate(Date.valueOf("2026-04-05"));
        upd.setTypeRendezVous(RendezVous.TypeRV.suivi);
        upd.setAppointmentTimeRv(Time.valueOf("12:00:00"));

        //update avec contrôle :
        // seulement si le RV appartient au patient donné
        srv.updateForPatient(upd, PATIENT_ID);

        System.out.println("[DEBUG_LOG] Updated RV ID: " + id);
        pauseForDemo("Va dans phpMyAdmin → table rendez_vous → ID = " + id +
                " et vérifie: type=suivi, statut=termine, time=12:00:00, date=2026-04-05");

        //Tu lis la liste et tu vérifies que la ligne a bien changé :
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

        //doit retourner des RV du psy avec noms.
        List<RendezVousView> views = srv.findViewsByPsychologist(PSY_ID);
        assertFalse(views.isEmpty());

        //trouver le RV qu’on vient d’ajouter
        RendezVousView v = views.stream().filter(x -> x.getIdRv() == id).findFirst().orElse(null);
        assertNotNull(v);

        //vérifie que la jointure users a bien ramené les noms.
        //vérifier noms non vides
        assertNotNull(v.getPatientFullName());
        assertFalse(v.getPatientFullName().isBlank());

        assertNotNull(v.getPsychologistFullName());
        assertFalse(v.getPsychologistFullName().isBlank());
    }

    @Test
    @Order(4)
    public void testDeleteForPatientAlsoDeletesCompteRendu() throws SQLException, InterruptedException {
        // create RV
        RendezVous rv = new RendezVous();
        rv.setIdPatient(PATIENT_ID);
        rv.setIdPsychologist(PSY_ID);
        rv.setStatutRv(RendezVous.StatutRV.prevu);
        rv.setAppointmentDate(Date.valueOf("2026-04-07"));
        rv.setTypeRendezVous(RendezVous.TypeRV.suivi);
        rv.setAppointmentTimeRv(Time.valueOf("15:00:00"));
        int id = srv.addAndReturnId(rv);
        idRv = id;

        // insert CR linked to RV (minimal)
        String sql = """
            INSERT INTO compte_rendu_seance (id_appointment, date_creationcr, progrescr, resumeseancecr, prochainesactioncr)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            // ✅ safe enum value (adapt if your enum differs)
            pst.setString(3, CompteRenduSeance.ProgresCR.amelioration_stable.name());
            pst.setString(4, "Résumé test JUnit");
            pst.setString(5, "Action test JUnit");
            pst.executeUpdate();
        }

        //vérifier existence avant delete
        assertTrue(exists("SELECT 1 FROM compte_rendu_seance WHERE id_appointment=?", id));
        assertTrue(exists("SELECT 1 FROM rendez_vous WHERE id_rv=?", id));

        pauseForDemo("AVANT delete: vérifie dans phpMyAdmin que RV(ID=" + id +
                ") existe ET qu'il y a un compte rendu dans compte_rendu_seance(id_appointment=" + id + ")");

        // delete should remove CR + RV
        srv.deleteForPatient(id, PATIENT_ID);
        idRv = -1; // already deleted

        pauseForDemo("APRÈS delete: vérifie que RV(ID=" + id +
                ") n'existe plus ET que compte_rendu_seance(id_appointment=" + id + ") n'existe plus");

        //confirme suppression.
        assertFalse(exists("SELECT 1 FROM compte_rendu_seance WHERE id_appointment=?", id));
        assertFalse(exists("SELECT 1 FROM rendez_vous WHERE id_rv=?", id));
    }

    //pour vérifier existence dune ligne
    private boolean exists(String sql, int id) throws SQLException {
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }
}
