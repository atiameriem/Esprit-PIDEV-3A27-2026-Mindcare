import models.LocalRelaxation;
import org.junit.jupiter.api.*;
import services.LocalRelaxationService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LocalRelaxationServiceTest {

    private static LocalRelaxationService service;
    private int createdId = -1;

    @BeforeAll
    public static void setup() {
        service = new LocalRelaxationService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (createdId != -1) {
            service.delete(createdId); // ✅ delete(int)
            System.out.println("[DEBUG_LOG] Cleanup: Deleted Local id=" + createdId);
            createdId = -1;
        }
    }

    @Test
    @Order(1)
    void add_should_create_local() throws SQLException {
        String nom = "Local Test " + System.currentTimeMillis();

        LocalRelaxation l = new LocalRelaxation();
        l.setNom(nom);
        l.setDescription("Description Test");

        // ✅ Dans ton projet actuel: type/etat sont des String (pas enum)
        l.setType("YOGA");
        l.setCapacite(25);
        l.setEquipements("Tapis");
        l.setEtage(1);
        l.setDureeMaxSession(30);
        l.setTarifHoraire(new BigDecimal("20"));
        l.setEtat("ACTIF");
        l.setDisponible(true);

        // image peut être null -> service met "default.png"
        l.setImage(null);

        int id = service.add(l);   // ✅ add() retourne int
        createdId = id;

        assertTrue(id > 0);
        System.out.println("[DEBUG_LOG] Created Local id=" + id);
    }

    @Test
    @Order(2)
    void getAll_should_return_list() throws SQLException {
        List<LocalRelaxation> list = service.getAll();
        assertNotNull(list);
        // peut être vide selon ta base, donc pas assertFalse(list.isEmpty()) obligatoire
        System.out.println("[DEBUG_LOG] Total Locals: " + list.size());
    }

    @Test
    @Order(3)
    void update_should_update_fields() throws SQLException {
        // Create first
        LocalRelaxation l = new LocalRelaxation();
        l.setNom("Update Test " + System.currentTimeMillis());
        l.setDescription("Desc");
        l.setType("YOGA");
        l.setCapacite(20);
        l.setEquipements("Tapis");
        l.setEtage(1);
        l.setDureeMaxSession(30);
        l.setTarifHoraire(new BigDecimal("30"));
        l.setEtat("ACTIF");
        l.setDisponible(true);
        l.setImage("default.png");

        int id = service.add(l);
        createdId = id;

        // Update
        l.setIdLocal(id);
        l.setNom("Local Updated");
        l.setCapacite(10);
        l.setEtat("MAINTENANCE");

        service.update(l);

        LocalRelaxation updated = service.getById(id);
        assertNotNull(updated);
        assertEquals("Local Updated", updated.getNom());
        assertEquals(10, updated.getCapacite());
        assertEquals("MAINTENANCE", updated.getEtat());

        System.out.println("[DEBUG_LOG] Updated Local id=" + id);
    }

    @Test
    @Order(4)
    void delete_should_remove_local() throws SQLException {
        // Create first
        LocalRelaxation l = new LocalRelaxation();
        l.setNom("Delete Test " + System.currentTimeMillis());
        l.setDescription("Desc");
        l.setType("YOGA");
        l.setCapacite(20);
        l.setEquipements("Tapis");
        l.setEtage(1);
        l.setDureeMaxSession(30);
        l.setTarifHoraire(new BigDecimal("30"));
        l.setEtat("ACTIF");
        l.setDisponible(true);
        l.setImage("default.png");

        int id = service.add(l);

        // Delete
        service.delete(id);

        LocalRelaxation deleted = service.getById(id);
        assertNull(deleted);

        System.out.println("[DEBUG_LOG] Deleted Local id=" + id);
    }
}
