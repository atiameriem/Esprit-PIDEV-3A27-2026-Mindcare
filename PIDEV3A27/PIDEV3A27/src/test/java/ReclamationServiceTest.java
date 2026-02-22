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
    private static int idUser = -1;
    private static int idReclamation = -1;

    @BeforeAll
    public static void setup() throws SQLException {
        rs = new ReclamationService();
        us = new UserService();

        // Création de l'utilisateur pour la clé étrangère
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

        idUser = us.create(u); // récupérer l'ID généré
        System.out.println("[DEBUG_LOG] User créé pour Reclamation ID = " + idUser);
    }

    @AfterAll
    public static void cleanUp() {
        try {
            if (idReclamation != -1) {
                rs.delete(idReclamation);
                System.out.println("[DEBUG_LOG] Cleanup: Reclamation supprimée ID = " + idReclamation);
                idReclamation = -1;
            }

            if (idUser != -1) {
                us.delete(idUser);
                System.out.println("[DEBUG_LOG] Cleanup: User supprimé ID = " + idUser);
                idUser = -1;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR_LOG] Cleanup échoué: " + e.getMessage());
        }
    }

    // ---------------- TEST CREATE ----------------
    @Test
    @Order(1)
    public void testCreateReclamation() throws SQLException {
        Reclamation r = new Reclamation();
        r.setIdUser(idUser);
        r.setType(TypeReclamation.Autre);
        r.setDescription("Impossible de se connecter");
        r.setStatut("EN_ATTENTE");

        idReclamation = rs.create(r); // récupérer l'ID généré
        assertTrue(idReclamation > 0, "Reclamation ID should be greater than 0");

        System.out.println("[DEBUG_LOG][TEST CREATE] Reclamation créée ID = " + idReclamation);
    }

    // ---------------- TEST READ ----------------
    @Test
    @Order(2)
    public void testReadReclamation() throws SQLException {
        if (idReclamation == -1) {
            testCreateReclamation();
        }

        List<Reclamation> list = rs.getAll();
        assertFalse(list.isEmpty(), "La liste des réclamations ne doit pas être vide");

        boolean found = list.stream()
                .anyMatch(r -> r.getId() == idReclamation);

        assertTrue(found, "La réclamation doit exister dans la base");
        System.out.println("[DEBUG_LOG][TEST READ] Reclamation trouvée ID = " + idReclamation);
    }

    // ---------------- TEST UPDATE ----------------
    @Test
    @Order(3)
    public void testUpdateReclamation() throws SQLException {
        if (idReclamation == -1) {
            testCreateReclamation();
        }

        Reclamation r = new Reclamation();
        r.setId(idReclamation);
        r.setIdUser(idUser);
        r.setType(TypeReclamation.Autre);
        r.setDescription("Problème résolu");
        r.setStatut("RESOLU");

        rs.update(r);

        Reclamation updated = rs.findById(idReclamation);
        assertNotNull(updated, "La réclamation mise à jour doit être retrouvée");
        assertEquals("RESOLU", updated.getStatut());
        assertEquals(TypeReclamation.Autre, updated.getType());

        System.out.println("[DEBUG_LOG][TEST UPDATE] Reclamation mise à jour ID = " + idReclamation);
    }

    // ---------------- TEST DELETE ----------------
    @Test
    @Order(4)
    public void testDeleteReclamation() throws SQLException {
        if (idReclamation == -1) {
            testCreateReclamation();
        }

        rs.delete(idReclamation);
        System.out.println("[DEBUG_LOG][TEST DELETE] Reclamation supprimée ID = " + idReclamation);
        idReclamation = -1;

        List<Reclamation> list = rs.getAll();
        boolean found = list.stream()
                .anyMatch(r -> r.getId() == idReclamation);

        assertFalse(found, "La réclamation doit être supprimée de la base");
    }
}
*/