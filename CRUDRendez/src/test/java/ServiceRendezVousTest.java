import entities.RendezVous;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import services.ServiceRendezVous;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
public class ServiceRendezVousTest {

    static ServiceRendezVous srv;
    private int idRv = -1;

    @BeforeAll
    public static void setup() {
        srv = new ServiceRendezVous();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idRv != -1) {
            RendezVous toDelete = new RendezVous();
            toDelete.setIdRv(idRv);
            srv.delete(toDelete);
            idRv = -1;
        }
    }

    @Test
    @Order(1)
    public void testAddRendezVous() throws SQLException {

        int idPatient = 1;
        int idPsychologist = 2;

        Date date = Date.valueOf("2026-02-15");
        Time time = Time.valueOf("10:30:00");

        RendezVous rv = new RendezVous();
        rv.setIdPatient(idPatient);
        rv.setIdPsychologist(idPsychologist);
        rv.setStatutRv(RendezVous.StatutRV.prevu);
        rv.setAppointmentDate(date);
        rv.setTypeRendezVous(RendezVous.TypeRV.premiere_consultation);
        rv.setAppointmentTimeRv(time);

        srv.add(rv);

        List<RendezVous> list = srv.getAll();

        RendezVous found = list.stream()
                .filter(x ->
                        x.getIdPatient() == idPatient &&
                                x.getIdPsychologist() == idPsychologist &&
                                x.getAppointmentDate().equals(date) &&
                                x.getAppointmentTimeRv().equals(time) &&
                                x.getStatutRv() == RendezVous.StatutRV.prevu &&
                                x.getTypeRendezVous() == RendezVous.TypeRV.premiere_consultation
                )
                .findFirst()
                .orElse(null);

        assertNotNull(found);
        idRv = found.getIdRv();
        assertTrue(idRv > 0);
    }

    @Test
    @Order(2)
    public void testUpdateRendezVous() throws SQLException {

        int idPatient = 1;
        int idPsychologist = 2;

        Date date = Date.valueOf("2026-02-15");
        Time time = Time.valueOf("11:00:00");

        RendezVous rv = new RendezVous();
        rv.setIdPatient(idPatient);
        rv.setIdPsychologist(idPsychologist);
        rv.setStatutRv(RendezVous.StatutRV.prevu);
        rv.setAppointmentDate(date);
        rv.setTypeRendezVous(RendezVous.TypeRV.suivi);
        rv.setAppointmentTimeRv(time);

        srv.add(rv);

        RendezVous created = srv.getAll().stream()
                .filter(x ->
                        x.getIdPatient() == idPatient &&
                                x.getIdPsychologist() == idPsychologist &&
                                x.getAppointmentDate().equals(date) &&
                                x.getAppointmentTimeRv().equals(time) &&
                                x.getTypeRendezVous() == RendezVous.TypeRV.suivi
                )
                .findFirst()
                .orElseThrow();

        idRv = created.getIdRv();

        created.setStatutRv(RendezVous.StatutRV.termine);
        created.setTypeRendezVous(RendezVous.TypeRV.urgence);

        srv.update(created);

        boolean ok = srv.getAll().stream().anyMatch(x ->
                x.getIdRv() == idRv &&
                        x.getStatutRv() == RendezVous.StatutRV.termine &&
                        x.getTypeRendezVous() == RendezVous.TypeRV.urgence
        );

        assertTrue(ok);
    }
}
