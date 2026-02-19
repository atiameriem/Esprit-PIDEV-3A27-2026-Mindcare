import models.Formation;
import org.junit.jupiter.api.*;
import services.FormationService;

import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FormationServiceTest  {

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
            try {
                sf.delete(idFormation);
                System.out.println("[CLEANUP] Formation supprimée ID = " + idFormation);
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                idFormation = -1;
            }
        }
    }

    // ---------------- TEST CREATE ----------------
    @Test
    @Order(1)
    public void testCreateFormation() throws SQLException {
        String titre = "Formation Test " + System.currentTimeMillis();
        Formation f = new Formation(titre, "Description test", "10h", "Débutant");
        f.setImagePath("image.png");

        sf.create(f);

        List<Formation> formations = sf.read();
        assertFalse(formations.isEmpty(), "La liste ne devrait pas être vide après une insertion");

        Formation created = formations.stream()
                .filter(form -> titre.equals(form.getTitre()))
                .findFirst()
                .orElse(null);

        assertTrue(created != null, "La formation créée doit être retrouvée");
        idFormation = created.getId();
        assertTrue(created.getTitre().startsWith("Formation Test"), "Le titre doit respecter le préfixe");

        System.out.println("[TEST CREATE] Succès pour ID = " + idFormation);
    }

    // ---------------- TEST UPDATE ----------------
    @Test
    @Order(2)
    public void testUpdateFormation() throws SQLException {
        String titre = "Formation Update " + System.currentTimeMillis();
        Formation f = new Formation(titre, "Description initiale", "10h", "Intermédiaire");
        f.setImagePath("image.png");

        sf.create(f);

        Formation created = sf.read().stream()
                .filter(form -> titre.equals(form.getTitre()))
                .findFirst()
                .orElse(null);

        assertTrue(created != null, "La formation doit exister pour pouvoir la mettre à jour");
        idFormation = created.getId();

        // Modification
        created.setDescription("Description modifiée");
        created.setDuree("20h");
        created.setNiveau("Avancé");
        created.setImagePath("nouvelle.png");

        sf.update(created);

        Formation updated = sf.findById(idFormation);
        assertTrue(updated != null, "La formation mise à jour doit être retrouvée");
        assertTrue("Description modifiée".equals(updated.getDescription()));
        assertTrue("Avancé".equals(updated.getNiveau()));
        assertTrue("nouvelle.png".equals(updated.getImagePath()));

        System.out.println("[TEST UPDATE] Formation mise à jour ID = " + idFormation);
    }

    // ---------------- TEST FIND BY ID ----------------
    @Test
    @Order(3)
    public void testFindById() throws SQLException {
        String titre = "Formation Find " + System.currentTimeMillis();
        Formation f = new Formation(titre, "Description find", "15h", "Débutant");
        f.setImagePath("faciale.jpg");

        sf.create(f);

        Formation created = sf.read().stream()
                .filter(form -> titre.equals(form.getTitre()))
                .findFirst()
                .orElse(null);

        assertTrue(created != null, "La formation doit exister pour le test findById");
        idFormation = created.getId();

        Formation found = sf.findById(idFormation);
        assertTrue(found != null, "La formation doit être trouvée par ID");
        assertTrue(found.getId() == idFormation);

        System.out.println("[TEST FIND] Formation trouvée ID = " + idFormation);
    }

}
