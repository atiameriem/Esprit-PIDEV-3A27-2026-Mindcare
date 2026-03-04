import models.Participation;
import models.Formation;
import org.junit.jupiter.api.*;
import services.ParticipationServiceF;
import services.FormationService;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ParticipationTest {

    static ParticipationServiceF ps;
    static FormationService fs;
    private static int formationId; // ID réel généré pour la formation test
    private int participationId = -1;
    private final int testUserId = 1; // ID fixe pour utilisateur (table users si existante)

    @BeforeAll
    public static void setup() throws SQLException {
        ps = new ParticipationServiceF();
        fs = new FormationService();

        // Créer une formation test
        List<Formation> formations = fs.read();
        Formation f = formations.stream()
                .filter(fo -> "Formation Test".equals(fo.getTitre()))
                .findFirst()
                .orElse(null);

        if (f == null) {
            f = new Formation("Formation Test", "Description test", "10h", "Débutant", "image.png");
            formationId = fs.create(f);
            System.out.println("[SETUP] Formation test créée ID = " + formationId);
        } else {
            formationId = f.getId();
            System.out.println("[SETUP] Formation test existante ID = " + formationId);
        }
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (participationId != -1) {
            ps.delete(participationId);
            System.out.println("[CLEANUP] Participation supprimée ID = " + participationId);
            participationId = -1;
        }
    }

    @AfterAll
    public static void cleanupFormation() throws SQLException {
        fs.delete(formationId);
        System.out.println("[CLEANUP] Formation test supprimée ID = " + formationId);
    }

    // TEST CREATE PARTICIPATION
    @Test
    @Order(1)
    public void testCreateParticipation() throws SQLException {
        Participation p = new Participation();
        p.setIdUser(testUserId);
        p.setIdFormation(formationId);
        p.setDateInscription(new Date());
        p.setStatut("en attente");
        p.setTitreFormation("Formation Test");
        p.setImagePath("image.png");

        ps.create(p);
        participationId = p.getIdParticipation();

        assertTrue(participationId != -1, "L'ID de la participation doit être généré");
        System.out.println("[TEST CREATE] Participation créée ID = " + participationId);
    }

    // TEST FIND BY USER
    @Test
    @Order(2)
    public void testFindByUserId() throws SQLException {
        Participation p = new Participation();
        p.setIdUser(testUserId);
        p.setIdFormation(formationId);
        p.setDateInscription(new Date());
        p.setStatut("en attente");
        p.setTitreFormation("Formation Test 2");
        p.setImagePath("image2.png");

        ps.create(p);
        participationId = p.getIdParticipation();

        List<Participation> list = ps.findByUserId(testUserId);

        assertFalse(list.isEmpty(), "La liste de participations pour l'utilisateur ne doit pas être vide");

        boolean found = list.stream().anyMatch(part -> part.getIdParticipation() == participationId);
        assertTrue(found, "La participation créée doit être retrouvée dans la liste");

        System.out.println("[TEST FIND] Participation trouvée ID = " + participationId);
    }
}
