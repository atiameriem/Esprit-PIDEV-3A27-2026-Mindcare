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
public class ServiceQuizTest {

    static ServiceQuiz     sq;
    static ServiceQuestion sqst;
    static ServiceReponse  sr;

    private int idQuiz = -1;

    // IDs existants en DB (mohamed=1, meriem=2)
    private final int idPsychologue = 1;
    private final int idPatient     = 2;

    @BeforeAll
    public static void setup() {
        sq   = new ServiceQuiz();
        sqst = new ServiceQuestion();
        sr   = new ServiceReponse();
        System.out.println("[DEBUG_LOG] Services initialisés.");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (idQuiz != -1) {
            Quiz toDelete = sq.getQuizById(idQuiz);
            if (toDelete != null) {
                sq.delete(toDelete);
                System.out.println("[DEBUG_LOG] Cleanup: Quiz supprimé ID = " + idQuiz);
            }
            idQuiz = -1;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 1 : Créer un quiz
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(1)
    public void testAddQuiz() throws SQLException {
        Quiz q = new Quiz(
                idPatient,       // id_users  : patient assigné
                idPsychologue,   // cree_par  : psychologue créateur
                "Test de stress",
                "Évalue le stress",
                "psychologique",
                true
        );
        sq.add(q);
        idQuiz = q.getIdQuiz();

        System.out.println("[DEBUG_LOG] Quiz créé ID = " + idQuiz);

        // Vérifier que l'ID a bien été généré
        assertTrue(idQuiz > 0, "L'ID du quiz doit être > 0");

        // Vérifier que le quiz existe en DB
        Quiz found = sq.getQuizById(idQuiz);
        assertNotNull(found, "Le quiz doit exister en DB");
        assertEquals("Test de stress", found.getTitre());
        assertEquals("psychologique",  found.getTypeTest());
        assertTrue(found.isActif());

        System.out.println("[DEBUG_LOG] Vérifié : Quiz existe en DB.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 2 : Modifier un quiz
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(2)
    public void testUpdateQuiz() throws SQLException {
        // Créer
        Quiz q = new Quiz(
                idPatient, idPsychologue,
                "Test initial", "Description initiale",
                "psychologique", true
        );
        sq.add(q);
        idQuiz = q.getIdQuiz();

        // Modifier
        q.setTitre      ("Test modifié");
        q.setDescription("Nouvelle description");
        q.setTypeTest   ("cognitif");
        q.setActif      (false);
        sq.update(q);

        System.out.println("[DEBUG_LOG] Quiz modifié ID = " + idQuiz);

        // Vérifier les modifications
        Quiz updated = sq.getQuizById(idQuiz);
        assertNotNull(updated);
        assertEquals("Test modifié",       updated.getTitre());
        assertEquals("Nouvelle description", updated.getDescription());
        assertEquals("cognitif",           updated.getTypeTest());
        assertFalse(updated.isActif());

        System.out.println("[DEBUG_LOG] Vérifié : Quiz modifié avec succès.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 3 : Récupérer les quiz d'un psychologue
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(3)
    public void testGetQuizParPsychologue() throws SQLException {
        // Créer un quiz
        Quiz q = new Quiz(
                idPatient, idPsychologue,
                "Quiz psychologue test", "desc",
                "psychologique", true
        );
        sq.add(q);
        idQuiz = q.getIdQuiz();

        // Récupérer tous les quiz du psychologue
        List<Quiz> quizzes = sq.getQuizParPsychologue(idPsychologue);

        assertFalse(quizzes.isEmpty(), "La liste ne doit pas être vide");

        boolean found = quizzes.stream()
                .anyMatch(quiz -> quiz.getIdQuiz() == idQuiz);
        assertTrue(found, "Le quiz créé doit apparaître dans la liste");

        System.out.println("[DEBUG_LOG] Vérifié : " + quizzes.size() +
                " quiz(zes) trouvé(s) pour psychologue ID = " + idPsychologue);
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 4 : Ajouter questions + choix
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(4)
    public void testAddQuestionsAvecChoix() throws SQLException {
        // Créer quiz
        Quiz q = new Quiz(
                idPatient, idPsychologue,
                "Quiz avec questions", "desc",
                "psychologique", true
        );
        sq.add(q);
        idQuiz = q.getIdQuiz();

        // Ajouter 2 questions avec choix
        Question q1 = new Question(idQuiz, "Je me sens tendu(e)", 1, "checkbox");
        sqst.addAvecChoix(q1, List.of(
                new Reponse(idQuiz, 0, "Jamais",  0),
                new Reponse(idQuiz, 0, "Parfois", 1),
                new Reponse(idQuiz, 0, "Souvent", 2)
        ));

        Question q2 = new Question(idQuiz, "Je dors mal la nuit", 2, "checkbox");
        sqst.addAvecChoix(q2, List.of(
                new Reponse(idQuiz, 0, "Jamais",  0),
                new Reponse(idQuiz, 0, "Parfois", 1),
                new Reponse(idQuiz, 0, "Souvent", 2)
        ));

        // Vérifier les questions chargées avec choix
        List<Question> questions = sqst.getQuestionsByQuizAvecChoix(idQuiz);

        assertEquals(2, questions.size(), "2 questions doivent exister");

        for (Question question : questions) {
            assertEquals(3, question.getReponses().size(),
                    "Chaque question doit avoir 3 choix");
        }

        System.out.println("[DEBUG_LOG] Vérifié : 2 questions + 6 choix ajoutés.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 5 : Patient passe le test + calcul score
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(5)
    public void testPasserTestEtCalculerScore() throws SQLException {
        // Créer quiz avec questions
        Quiz q = new Quiz(
                idPatient, idPsychologue,
                "Test score", "desc",
                "psychologique", true
        );
        sq.add(q);
        idQuiz = q.getIdQuiz();

        Question q1 = new Question(idQuiz, "Question 1", 1, "checkbox");
        sqst.addAvecChoix(q1, List.of(
                new Reponse(idQuiz, 0, "Jamais",  0),
                new Reponse(idQuiz, 0, "Parfois", 1),
                new Reponse(idQuiz, 0, "Souvent", 2)
        ));

        Question q2 = new Question(idQuiz, "Question 2", 2, "checkbox");
        sqst.addAvecChoix(q2, List.of(
                new Reponse(idQuiz, 0, "Jamais",  0),
                new Reponse(idQuiz, 0, "Parfois", 1),
                new Reponse(idQuiz, 0, "Souvent", 2)
        ));

        // Patient répond : Parfois(1) + Souvent(2) = score 3
        sr.add(new Reponse(idQuiz, q1.getIdQuestion(), idPatient, "Parfois", 1));
        sr.add(new Reponse(idQuiz, q2.getIdQuestion(), idPatient, "Souvent", 2));

        // Calculer score
        String resultat = sq.calculerEtSauvegarderScore(idQuiz, idPatient);
        System.out.println("[DEBUG_LOG] Résultat : " + resultat);

        assertTrue(resultat.contains("Score: 3"), "Le score doit être 3");
        assertTrue(resultat.contains("Niveau: faible"), "Le niveau doit être faible (score < 5)");

        System.out.println("[DEBUG_LOG] Vérifié : Score et niveau corrects.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 6 : Historique + suivi évolution
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(6)
    public void testHistoriqueEtEvolution() throws SQLException {
        // Récupérer l'historique du patient
        List<String> historique = sq.getHistoriquePatient(idPatient);

        assertNotNull(historique, "L'historique ne doit pas être null");
        assertFalse(historique.isEmpty(), "L'historique doit contenir au moins un passage");

        System.out.println("[DEBUG_LOG] Historique de meriem (ID=" + idPatient + ") :");
        historique.forEach(ligne -> System.out.println("  → " + ligne));

        System.out.println("[DEBUG_LOG] Vérifié : Historique récupéré avec succès.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 7 : Supprimer un quiz
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(7)
    public void testDeleteQuiz() throws SQLException {
        // Créer
        Quiz q = new Quiz(
                idPatient, idPsychologue,
                "Quiz à supprimer", "desc",
                "psychologique", true
        );
        sq.add(q);
        idQuiz = q.getIdQuiz();

        // Supprimer
        sq.delete(q);

        // Vérifier que le quiz n'existe plus
        Quiz deleted = sq.getQuizById(idQuiz);
        assertNull(deleted, "Le quiz supprimé ne doit plus exister en DB");

        idQuiz = -1; // éviter double suppression dans cleanUp
        System.out.println("[DEBUG_LOG] Vérifié : Quiz supprimé avec succès.");
    }
}