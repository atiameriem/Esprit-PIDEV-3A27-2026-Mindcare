import models.Formation;
import org.junit.jupiter.api.*;
import services.FormationService;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FormationServiceTest {

    static FormationService sf;
    private int idFormation = -1;

    @BeforeAll
    public static void setup() {
        sf = new FormationService();
        System.out.println("Setup FormationService");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idFormation != -1) {
            sf.delete(idFormation);
            System.out.println("[CLEANUP] Formation supprimée ID = " + idFormation);
            idFormation = -1;
        }
    }

    // ===================== TEST CREATE =====================
    @Test
    @Order(1)
    public void testCreateFormation() throws SQLException {
        String titre = "Formation Test " + System.currentTimeMillis();
        Formation f = new Formation(
                titre,
                "Description test",
                "10h",
                "Débutant"
        );
        f.setImagePath("image.png");

        sf.create(f);

        List<Formation> formations = sf.read();
        assertFalse(formations.isEmpty(), "La liste des formations ne doit pas être vide");

        Formation created = formations.stream()
                .filter(form -> titre.equals(form.getTitre()))
                .findFirst()
                .orElse(null);

        assertNotNull(created, "La formation créée doit exister");
        idFormation = created.getId();

        System.out.println("[TEST CREATE] Formation créée ID = " + idFormation);
    }

    // ===================== TEST UPDATE =====================
    @Test
    @Order(2)
    public void testUpdateFormation() throws SQLException {
        String titre = "Formation Update " + System.currentTimeMillis();
        Formation f = new Formation(
                titre,
                "Description initiale",
                "10h",
                "Intermédiaire"
        );
        f.setImagePath("image.png");
        sf.create(f);

        Formation created = sf.read().stream()
                .filter(form -> titre.equals(form.getTitre()))
                .findFirst()
                .orElse(null);

        assertNotNull(created);
        idFormation = created.getId();

        // Modifier certains champs
        created.setDescription("Description modifiée");
        created.setDuree("20h");
        created.setNiveau("Avancé");
        created.setImagePath("nouvelle.png");

        sf.update(created);

        Formation updated = sf.findById(idFormation);
        assertNotNull(updated);
        assertEquals("Description modifiée", updated.getDescription());
        assertEquals("Avancé", updated.getNiveau());
        assertEquals("nouvelle.png", updated.getImagePath());

        System.out.println("[TEST UPDATE] Formation mise à jour ID = " + idFormation);
    }

    // ===================== 3️⃣ TEST FIND BY ID =====================
    @Test
    @Order(3)
    public void testFindById() throws SQLException {
        Formation f = new Formation(
                "Formation Find",
                "Description find",
                "15h",
                "Débutant"
        );
        f.setImagePath("faciale.jpg");

        sf.create(f);

        Formation created = sf.read().stream()
                .filter(form -> "Formation Find".equals(form.getTitre()))
                .findFirst()
                .orElse(null);

        assertNotNull(created);
        idFormation = created.getId();

        Formation found = sf.findById(idFormation);
        assertNotNull(found);
        assertEquals(idFormation, found.getId());

        System.out.println("[TEST FIND] Formation trouvée ID = " + idFormation);
    }
}
