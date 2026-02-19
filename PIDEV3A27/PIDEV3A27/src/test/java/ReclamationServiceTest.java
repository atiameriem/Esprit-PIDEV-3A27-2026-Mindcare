/*package test;*/

/*import models.Reclamation;
import models.TypeReclamation;
import models.User;
import org.junit.jupiter.api.*;
import services.ReclamationService;
import services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReclamationServiceTest {

    static ReclamationService rs;
    static UserService us;
    static int idUser = -1;
    static int idReclamation = -1;

    @BeforeAll
    public static void setup() throws SQLException {

        rs = new ReclamationService();
        us = new UserService();

        // Create user for foreign key
        User u = new User(
                25,
                "TestUser",
                "ForReclamation",
                "rec@test.com",
                "11111111",
                LocalDate.of(2024, 1, 1),
                "password",
                User.Role.Patient
        );

         us.create(u);
    }

    @AfterAll
    public static void cleanUp() throws SQLException {

        if (idReclamation != -1) {
            rs.delete(idReclamation);
        }

        if (idUser != -1) {
            us.delete(idUser);
        }
    }

    @Test
    @Order(1)
    public void testCreateReclamation() throws SQLException {

        Reclamation r = new Reclamation();
        r.setIdUser(idUser);
        r.setType(TypeReclamation.Autre);
        r.setDescription("Impossible de se connecter");
        r.setStatut("EN_ATTENTE");

        rs.create(r);

        idReclamation = r.getId();

        assertTrue(idReclamation > 0, "Reclamation ID should be greater than 0");
    }

    @Test
    @Order(2)
    public void testReadReclamation() throws SQLException {

        List<Reclamation> list = rs.getAll();

        assertFalse(list.isEmpty());

        boolean found = list.stream()
                .anyMatch(r -> r.getId() == idReclamation);

        assertTrue(found, "Reclamation should exist in database");
    }

    @Test
    @Order(3)
    public void testUpdateReclamation() throws SQLException {

        Reclamation r = new Reclamation();
        r.setId(idReclamation);
        r.setIdUser(idUser);
        r.setType(TypeReclamation.Autre);
        r.setDescription("Problème résolu");
        r.setStatut("RESOLU");

        rs.update(r);

        List<Reclamation> list = rs.getAll();

        Reclamation updated = list.stream()
                .filter(rec -> rec.getId() == idReclamation)
                .findFirst()
                .orElse(null);

        assertNotNull(updated);
        assertEquals("RESOLU", updated.getStatut());
        assertEquals(TypeReclamation.Autre, updated.getType());
    }

    @Test
    @Order(4)
    public void testDeleteReclamation() throws SQLException {

        rs.delete(idReclamation);
        idReclamation = -1;

        List<Reclamation> list = rs.getAll();

        boolean found = list.stream()
                .anyMatch(r -> r.getId() == idReclamation);

        assertFalse(found, "Reclamation should be deleted");
    }
}*/
