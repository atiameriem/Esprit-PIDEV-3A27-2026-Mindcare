import models.Formation;
import models.Module;
import org.junit.jupiter.api.*;
import services.FormationService;
import services.ModuleService;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ModuleServiceTest {

    static ModuleService ms;
    static FormationService fs;
    private static int formationId; // Formation de test
    private int moduleId = -1;

    @BeforeAll
    public static void setup() throws SQLException {
        ms = new ModuleService();
        fs = new FormationService();

        // Créer une formation test
        List<Formation> formations = fs.read();
        Formation f = formations.stream()
                .filter(fo -> "Formation Module Test".equals(fo.getTitre()))
                .findFirst()
                .orElse(null);

        if (f == null) {
            f = new Formation("Formation Module Test", "Description test formation", "5h", "Débutant", "image.png");
            formationId = fs.create(f);
            System.out.println("[SETUP] Formation test créée ID = " + formationId);
        } else {
            formationId = f.getId();
            System.out.println("[SETUP] Formation test existante ID = " + formationId);
        }
    }

    @AfterEach
    void cleanupModule() throws SQLException {
        if (moduleId != -1) {
            ms.delete(moduleId);
            System.out.println("[CLEANUP] Module supprimé ID = " + moduleId);
            moduleId = -1;
        }
    }

    @AfterAll
    public static void cleanupFormation() throws SQLException {
        fs.delete(formationId);
        System.out.println("[CLEANUP] Formation test supprimée ID = " + formationId);
    }

    // TEST CREATE MODULE
    @Test
    @Order(1)
    public void testCreateModule() throws SQLException {
        Module m = new Module();
        m.setTitre("Module Test");
        m.setDescription("Description module test");
        m.setFormationId(formationId);

        moduleId = ms.create(m);

        assertTrue(moduleId != -1, "L'ID du module doit être généré");
        System.out.println("[TEST CREATE] Module créé ID = " + moduleId);
    }

    // TEST FIND MODULE BY FORMATION
    @Test
    @Order(2)
    public void testFindByFormationId() throws SQLException {
        Module m = new Module();
        m.setTitre("Module Test 2");
        m.setDescription("Description module 2");
        m.setFormationId(formationId);

        moduleId = ms.create(m);

        List<Module> list = ms.findByFormationId(formationId);
        assertFalse(list.isEmpty(), "La liste des modules pour la formation ne doit pas être vide");

        boolean found = list.stream().anyMatch(mod -> mod.getId() == moduleId);
        assertTrue(found, "Le module créé doit être retrouvé dans la liste");

        System.out.println("[TEST FIND] Module trouvé ID = " + moduleId);
    }

    // TEST UPDATE MODULE
    @Test
    @Order(3)
    public void testUpdateModule() throws SQLException {
        Module m = new Module();
        m.setTitre("Module Update Test");
        m.setDescription("Description avant update");
        m.setFormationId(formationId);

        moduleId = ms.create(m);

        // Modification
        m.setTitre("Module Mis à Jour");
        m.setDescription("Description après update");
        ms.update(m);

        Module updated = ms.read().stream().filter(mod -> mod.getId() == moduleId).findFirst().orElse(null);
        assertEquals("Module Mis à Jour", updated.getTitre());
        assertEquals("Description après update", updated.getDescription());

        System.out.println("[TEST UPDATE] Module mis à jour ID = " + moduleId);
    }

    // TEST DELETE MODULE
    @Test
    @Order(4)
    public void testDeleteModule() throws SQLException {
        Module m = new Module();
        m.setTitre("Module à Supprimer");
        m.setDescription("Description à supprimer");
        m.setFormationId(formationId);

        moduleId = ms.create(m);

        ms.delete(moduleId);

        Module deleted = ms.read().stream().filter(mod -> mod.getId() == moduleId).findFirst().orElse(null);
        assertTrue(deleted == null, "Le module devrait être nul après suppression");

        System.out.println("[TEST DELETE] Module supprimé ID = " + moduleId);
        moduleId = -1;
    }
}
