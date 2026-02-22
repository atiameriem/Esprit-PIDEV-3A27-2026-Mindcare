import models.Quiz;
import models.Question;
import models.Reponse;
import org.junit.jupiter.api.*;
import services.ServiceQuiz;
import services.ServiceQuestion;
import services.ServiceReponse;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceReponseTest {

    static ServiceReponse  sr;
    static ServiceQuiz     sq;
    static ServiceQuestion sqst;

    private int idQuiz     = -1;
    private int idQuestion = -1;
    private int idReponse  = -1;

    // IDs existants en DB
    private final int idPsychologue = 6; // mohamed
    private final int idPatient     = 4; // meriem

    @BeforeAll
    public static void setup() {
        sr   = new ServiceReponse();
        sq   = new ServiceQuiz();
        sqst = new ServiceQuestion();
        System.out.println("[DEBUG_LOG] Services initialisés.");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        // Supprimer la réponse si elle existe encore
        if (idReponse != -1 && idQuiz != -1) {
            List<Reponse> reponses = sr.getReponsesParQuiz(idQuiz);
            Reponse toDelete = reponses.stream()
                    .filter(r -> r.getIdReponse() == idReponse)
                    .findFirst().orElse(null);
            if (toDelete != null) {
                sr.delete(toDelete);
                System.out.println("[DEBUG_LOG] Cleanup: Réponse supprimée ID = " + idReponse);
            }
            idReponse = -1;
        }

        // Supprimer le quiz (cascade supprime questions + réponses liées)
        if (idQuiz != -1) {
            Quiz q = sq.getQuizById(idQuiz);
            if (q != null) {
                sq.delete(q);
                System.out.println("[DEBUG_LOG] Cleanup: Quiz supprimé ID = " + idQuiz);
            }
            idQuiz     = -1;
            idQuestion = -1;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Méthode utilitaire : créer un quiz + une question de base
    // ══════════════════════════════════════════════════════════════
    private void creerQuizEtQuestion() throws SQLException {
        Quiz q = new Quiz(
                idPatient, idPsychologue,
                "Quiz test réponse", "desc",
                "psychologique", true
        );
        sq.add(q);
        idQuiz = q.getIdQuiz();

        Question question = new Question(idQuiz, "Question de test", 1, "checkbox");
        sqst.add(question);
        idQuestion = question.getIdQuestion();

        System.out.println("[DEBUG_LOG] Quiz ID=" + idQuiz + " | Question ID=" + idQuestion + " créés.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 1 : Ajouter un choix (psychologue, id_users = NULL)
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(1)
    public void testAddChoixPsychologue() throws SQLException {
        creerQuizEtQuestion();

        // Psychologue ajoute un choix possible (id_users = NULL)
        Reponse choix = new Reponse(idQuiz, idQuestion, "Jamais", 0);
        sr.add(choix);
        idReponse = choix.getIdReponse();

        System.out.println("[DEBUG_LOG] Choix créé ID = " + idReponse);

        assertTrue(idReponse > 0, "L'ID du choix doit être > 0");
        assertNull(choix.getIdUsers(), "id_users doit être NULL pour un choix");

        // Vérifier dans les choix de la question
        List<Reponse> choix2 = sr.getChoixParQuestion(idQuestion);
        assertFalse(choix2.isEmpty(), "La liste de choix ne doit pas être vide");

        boolean found = choix2.stream().anyMatch(r -> r.getIdReponse() == idReponse);
        assertTrue(found, "Le choix doit exister en DB");

        System.out.println("[DEBUG_LOG] Vérifié : Choix existe pour Question ID = " + idQuestion);
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 2 : Ajouter une réponse patient (id_users = idPatient)
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(2)
    public void testAddReponsePatient() throws SQLException {
        creerQuizEtQuestion();

        // Patient soumet une réponse
        Reponse reponse = new Reponse(idQuiz, idQuestion, idPatient, "Parfois", 1);
        sr.add(reponse);
        idReponse = reponse.getIdReponse();

        System.out.println("[DEBUG_LOG] Réponse patient créée ID = " + idReponse);

        assertTrue(idReponse > 0, "L'ID de la réponse doit être > 0");
        assertEquals(idPatient, reponse.getIdUsers(), "id_users doit être l'ID du patient");

        // Vérifier dans les réponses du quiz
        List<Reponse> reponses = sr.getReponsesParQuiz(idQuiz);
        assertFalse(reponses.isEmpty(), "La liste de réponses ne doit pas être vide");

        boolean found = reponses.stream().anyMatch(r -> r.getIdReponse() == idReponse);
        assertTrue(found, "La réponse du patient doit exister en DB");

        System.out.println("[DEBUG_LOG] Vérifié : Réponse patient existe pour Quiz ID = " + idQuiz);
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 3 : Modifier une réponse
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(3)
    public void testUpdateReponse() throws SQLException {
        creerQuizEtQuestion();

        // Créer une réponse patient
        Reponse reponse = new Reponse(idQuiz, idQuestion, idPatient, "Parfois", 1);
        sr.add(reponse);
        idReponse = reponse.getIdReponse();

        // Modifier
        reponse.setTexteReponse("Souvent");
        reponse.setValeur(2);
        sr.update(reponse);

        System.out.println("[DEBUG_LOG] Réponse modifiée ID = " + idReponse);

        // Vérifier les modifications
        Reponse updated = sr.getReponsesParQuiz(idQuiz).stream()
                .filter(r -> r.getIdReponse() == idReponse)
                .findFirst().orElse(null);

        assertNotNull(updated, "La réponse modifiée doit exister");
        assertEquals("Souvent", updated.getTexteReponse(), "Le texte doit être modifié");
        assertEquals(2,         updated.getValeur(),       "La valeur doit être 2");

        System.out.println("[DEBUG_LOG] Vérifié : Réponse modifiée avec succès.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 4 : Récupérer réponses d'un patient pour un quiz
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(4)
    public void testGetReponsesParPatientEtQuiz() throws SQLException {
        creerQuizEtQuestion();

        // Ajouter 2 réponses pour le patient
        Reponse r1 = new Reponse(idQuiz, idQuestion, idPatient, "Parfois", 1);
        sr.add(r1);
        idReponse = r1.getIdReponse(); // pour le cleanup

        List<Reponse> reponses = sr.getReponsesParPatientEtQuiz(idPatient, idQuiz);

        assertFalse(reponses.isEmpty(), "Le patient doit avoir des réponses");

        // Vérifier que toutes les réponses appartiennent bien au patient
        boolean toutesAuPatient = reponses.stream()
                .allMatch(r -> r.getIdUsers() != null && r.getIdUsers() == idPatient);
        assertTrue(toutesAuPatient, "Toutes les réponses doivent appartenir au patient");

        System.out.println("[DEBUG_LOG] Vérifié : " + reponses.size() +
                " réponse(s) trouvée(s) pour patient ID=" + idPatient +
                " | quiz ID=" + idQuiz);
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 5 : Vérifier séparation choix / réponses patients
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(5)
    public void testSeparationChoixEtReponses() throws SQLException {
        creerQuizEtQuestion();

        // Psychologue ajoute 3 choix (id_users = NULL)
        sr.add(new Reponse(idQuiz, idQuestion, "Jamais",  0));
        sr.add(new Reponse(idQuiz, idQuestion, "Parfois", 1));
        sr.add(new Reponse(idQuiz, idQuestion, "Souvent", 2));

        // Patient répond (id_users = idPatient)
        Reponse repPatient = new Reponse(idQuiz, idQuestion, idPatient, "Parfois", 1);
        sr.add(repPatient);
        idReponse = repPatient.getIdReponse(); // pour le cleanup

        // Vérifier les choix (id_users = NULL)
        List<Reponse> choix = sr.getChoixParQuestion(idQuestion);
        assertEquals(3, choix.size(), "3 choix possibles doivent exister");
        assertTrue(choix.stream().allMatch(r -> r.getIdUsers() == null),
                "Tous les choix doivent avoir id_users = NULL");

        // Vérifier les réponses patients (id_users NOT NULL)
        List<Reponse> reponses = sr.getReponsesParQuiz(idQuiz);
        assertEquals(1, reponses.size(), "1 réponse patient doit exister");
        assertTrue(reponses.stream().allMatch(r -> r.getIdUsers() != null),
                "Toutes les réponses doivent avoir un id_users");

        System.out.println("[DEBUG_LOG] Vérifié : " +
                choix.size() + " choix | " + reponses.size() + " réponse(s) patient.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 6 : Supprimer une réponse
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(6)
    public void testDeleteReponse() throws SQLException {
        creerQuizEtQuestion();

        // Créer une réponse
        Reponse reponse = new Reponse(idQuiz, idQuestion, idPatient, "Jamais", 0);
        sr.add(reponse);
        idReponse = reponse.getIdReponse();

        // Supprimer
        sr.delete(reponse);

        // Vérifier qu'elle n'existe plus
        List<Reponse> reponses = sr.getReponsesParQuiz(idQuiz);
        boolean found = reponses.stream().anyMatch(r -> r.getIdReponse() == idReponse);
        assertFalse(found, "La réponse supprimée ne doit plus exister");

        idReponse = -1; // éviter double suppression dans cleanUp
        System.out.println("[DEBUG_LOG] Vérifié : Réponse supprimée avec succès.");
    }
}