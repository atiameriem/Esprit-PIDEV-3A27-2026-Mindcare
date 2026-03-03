import models.Reservation;
import org.junit.jupiter.api.*;
import services.ReservationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReservationServiceTest {

    private static ReservationService service;

    // ⚠️ Mets ici des IDs qui existent réellement dans ta BD
    private static final int LOCAL_ID = 1;
    private static final int USER_ID = 2;

    private int createdId = -1;

    @BeforeAll
    public static void setup() {
        service = new ReservationService();
    }

    @AfterEach
    void cleanup() throws SQLException {
        if (createdId != -1) {
            service.delete(createdId); // ✅ delete(int)
            System.out.println("[DEBUG_LOG] Cleanup: Deleted Reservation id=" + createdId);
            createdId = -1;
        }
    }

    @Test
    @Order(1)
    void add_should_create_reservation() throws SQLException {
        Reservation r = new Reservation();
        r.setIdLocal(LOCAL_ID);
        r.setIdUtilisateur(USER_ID);

        r.setDateReservation(LocalDate.of(2026, 2, 12));
        r.setHeureDebut(LocalTime.of(9, 0));
        r.setHeureFin(LocalTime.of(10, 0));

        r.setTypeSession("Reservation Test");
        r.setMotif("Test JUnit");
        r.setStatut("EN_ATTENTE"); // ✅ String (selon ton service)

        int id = service.add(r);     // ✅ add retourne int
        createdId = id;

        assertTrue(id > 0);
        System.out.println("[DEBUG_LOG] Created Reservation id=" + id);
    }

    @Test
    @Order(2)
    void getAllWithLocalForUser_should_return_list() throws SQLException {
        // on récupère les réservations du patient
        List<Reservation> list = service.getAllWithLocalForUser(USER_ID, false);

        assertNotNull(list);
        // peut être vide si pas de réservations, donc on ne force pas assertFalse(list.isEmpty())
        System.out.println("[DEBUG_LOG] Reservations for user " + USER_ID + ": " + list.size());
    }

    @Test
    @Order(3)
    void update_should_update_fields_when_not_locked() throws SQLException {
        // Create first
        Reservation r = new Reservation();
        r.setIdLocal(LOCAL_ID);
        r.setIdUtilisateur(USER_ID);
        r.setDateReservation(LocalDate.of(2026, 3, 1));
        r.setHeureDebut(LocalTime.of(14, 0));
        r.setHeureFin(LocalTime.of(15, 0));
        r.setTypeSession("Update Test");
        r.setMotif("Avant modification");
        r.setStatut("EN_ATTENTE");

        int id = service.add(r);
        createdId = id;

        // Update
        r.setIdReservation(id);
        r.setMotif("Après modification");
        r.setTypeSession("Update Test Modifié");

        service.update(r);

        Reservation updated = service.getById(id);
        assertNotNull(updated);
        assertEquals("Après modification", updated.getMotif());
        assertEquals("Update Test Modifié", updated.getTypeSession());

        System.out.println("[DEBUG_LOG] Updated Reservation id=" + id);
    }

    @Test
    @Order(4)
    void delete_should_remove_reservation() throws SQLException {
        // Create first
        Reservation r = new Reservation();
        r.setIdLocal(LOCAL_ID);
        r.setIdUtilisateur(USER_ID);
        r.setDateReservation(LocalDate.of(2026, 4, 1));
        r.setHeureDebut(LocalTime.of(10, 0));
        r.setHeureFin(LocalTime.of(11, 0));
        r.setTypeSession("Delete Test");
        r.setMotif("Suppression");
        r.setStatut("EN_ATTENTE");

        int id = service.add(r);

        // Delete
        service.delete(id);

        Reservation deleted = service.getById(id);
        assertNull(deleted);

        System.out.println("[DEBUG_LOG] Deleted Reservation id=" + id);
    }
}
