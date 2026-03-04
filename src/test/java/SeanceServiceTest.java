import models.Formation;
import models.SeanceGroupe;
import org.junit.jupiter.api.*;
import services.FormationService;
import services.SeanceGroupeServiceF;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SeanceServiceTest {

    static SeanceGroupeServiceF ss;
    static FormationService fs;
    private static int formationId;
    private static final int testUserId = 1; // Ajuster selon la DB
    private int seanceId = -1;

    @BeforeAll
    public static void setup() throws SQLException {
        ss = new SeanceGroupeServiceF();
        fs = new FormationService();

        // Créer une formation test
        List<Formation> formations = fs.read();
        Formation f = formations.stream()
                .filter(fo -> "Formation Seance Test".equals(fo.getTitre()))
                .findFirst()
                .orElse(null);

        if (f == null) {
            f = new Formation("Formation Seance Test", "Description test", "2h", "Intermédiaire", "test.png");
            formationId = fs.create(f);
            System.out.println("[SETUP] Formation test créée ID = " + formationId);
        } else {
            formationId = f.getId();
            System.out.println("[SETUP] Formation test existante ID = " + formationId);
        }
    }

    @AfterEach
    void cleanupSeance() throws SQLException {
        if (seanceId != -1) {
            ss.delete(seanceId);
            System.out.println("[CLEANUP] Séance supprimée ID = " + seanceId);
            seanceId = -1;
        }
    }

    @AfterAll
    public static void cleanupFormation() throws SQLException {
        fs.delete(formationId);
        System.out.println("[CLEANUP] Formation test supprimée ID = " + formationId);
    }

    @Test
    @Order(1)
    public void testCreateSeance() throws SQLException {
        SeanceGroupe s = new SeanceGroupe();
        s.setTitre("Séance de Test JUnit");
        s.setIdFormation(formationId);
        s.setIdUsers(testUserId);
        s.setDateHeure(LocalDateTime.now().plusDays(1));
        s.setDureeMinutes(60);
        s.setStatut("PLANIFIEE");
        s.setDescription("Description séance test");
        s.setCapaciteMax(10);

        seanceId = ss.create(s);
        assertTrue(seanceId != -1, "L'ID de la séance doit être généré");
        System.out.println("[TEST CREATE] Séance créée ID = " + seanceId);
    }

    @Test
    @Order(2)
    public void testReadSeances() throws SQLException {
        SeanceGroupe s = new SeanceGroupe();
        s.setTitre("Séance à Lister");
        s.setIdFormation(formationId);
        s.setIdUsers(testUserId);
        s.setDateHeure(LocalDateTime.now().plusDays(2));
        s.setDureeMinutes(45);
        s.setStatut("PLANIFIEE");

        seanceId = ss.create(s);

        List<SeanceGroupe> list = ss.read();
        assertFalse(list.isEmpty(), "La liste des séances ne doit pas être vide");

        boolean found = list.stream().anyMatch(se -> se.getSeanceId() == seanceId);
        assertTrue(found, "La séance créée doit être dans la liste");
        System.out.println("[TEST READ] Séance trouvée dans la liste");
    }

    @Test
    @Order(3)
    public void testFindById() throws SQLException {
        SeanceGroupe s = new SeanceGroupe();
        s.setTitre("Séance Spécifique");
        s.setIdFormation(formationId);
        s.setIdUsers(testUserId);
        s.setDateHeure(LocalDateTime.now().plusHours(5));
        s.setDureeMinutes(30);

        seanceId = ss.create(s);

        SeanceGroupe found = ss.findById(seanceId);
        assertNotNull(found, "La séance doit être retrouvée par son ID");
        assertEquals("Séance Spécifique", found.getTitre());
        System.out.println("[TEST FINDBYID] Séance retrouvée par ID = " + seanceId);
    }

    @Test
    @Order(4)
    public void testDeleteSeance() throws SQLException {
        SeanceGroupe s = new SeanceGroupe();
        s.setTitre("Séance à Supprimer");
        s.setIdFormation(formationId);
        s.setIdUsers(testUserId);
        s.setDateHeure(LocalDateTime.now().plusDays(3));

        seanceId = ss.create(s);
        assertNotEquals(-1, seanceId);

        ss.delete(seanceId);

        SeanceGroupe deleted = ss.findById(seanceId);
        assertNull(deleted, "La séance ne doit plus exister après suppression");
        System.out.println("[TEST DELETE] Séance supprimée avec succès");
        seanceId = -1; // Évite AfterEach car déjà supprimé
    }
}
