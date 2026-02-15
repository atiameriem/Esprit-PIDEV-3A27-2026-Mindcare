import entities.CompteRenduSeance;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import services.ServiceCompteRenduSeance;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
public class ServiceCompteRenduSeanceTest {

    static ServiceCompteRenduSeance scr;
    private int idCr = -1;

    @BeforeAll
    public static void setup() {
        scr = new ServiceCompteRenduSeance();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idCr != -1) {
            CompteRenduSeance toDelete = new CompteRenduSeance();
            toDelete.setIdCompteRendu(idCr);
            scr.delete(toDelete);
            idCr = -1;
        }
    }

    @Test
    @Order(1)
    public void testAddCompteRendu() {

        int idAppointment = 1;

        Timestamp ts = Timestamp.valueOf("2026-02-15 12:00:00");

        CompteRenduSeance cr = new CompteRenduSeance();
        cr.setIdAppointment(idAppointment);
        cr.setDateCreationCr(ts);
        cr.setProgresCr(CompteRenduSeance.ProgresCR.amelioration_legere);
        cr.setResumeSeanceCr("resume test junit");
        cr.setProchainesActionCr("actions test junit");

        try {
            scr.add(cr);

            List<CompteRenduSeance> list = scr.getAll();
            assertFalse(list.isEmpty());

            CompteRenduSeance found = list.stream()
                    .filter(x -> x.getIdAppointment() == idAppointment
                            && x.getDateCreationCr().equals(ts)
                            && x.getProgresCr() == CompteRenduSeance.ProgresCR.amelioration_legere
                            && "resume test junit".equals(x.getResumeSeanceCr())
                            && "actions test junit".equals(x.getProchainesActionCr()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(found);
            idCr = found.getIdCompteRendu();
            assertTrue(idCr > 0);

        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    @Order(2)
    public void testUpdateCompteRenduFields() throws SQLException {

        int idAppointment = 1;

        Timestamp ts = Timestamp.valueOf("2026-02-15 13:00:00");

        CompteRenduSeance cr = new CompteRenduSeance();
        cr.setIdAppointment(idAppointment);
        cr.setDateCreationCr(ts);
        cr.setProgresCr(CompteRenduSeance.ProgresCR.stagnation);
        cr.setResumeSeanceCr("avant update");
        cr.setProchainesActionCr("avant update actions");

        scr.add(cr);

        CompteRenduSeance created = scr.getAll().stream()
                .filter(x -> x.getIdAppointment() == idAppointment
                        && x.getDateCreationCr().equals(ts)
                        && "avant update".equals(x.getResumeSeanceCr()))
                .findFirst()
                .orElseThrow();

        idCr = created.getIdCompteRendu();

        scr.updateFields(
                idCr,
                CompteRenduSeance.ProgresCR.amelioration_significative,
                "apres update",
                "apres update actions"
        );

        boolean ok = scr.getAll().stream().anyMatch(x ->
                x.getIdCompteRendu() == idCr
                        && x.getProgresCr() == CompteRenduSeance.ProgresCR.amelioration_significative
                        && "apres update".equals(x.getResumeSeanceCr())
                        && "apres update actions".equals(x.getProchainesActionCr())
        );

        assertTrue(ok);
    }
}
