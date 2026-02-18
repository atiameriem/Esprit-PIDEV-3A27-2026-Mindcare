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
public class ServiceQuestionTest {

    static ServiceQuestion sqst;
    static ServiceQuiz     sq;
    static ServiceReponse  sr;

    private int idQuiz     = -1;
    private int idQuestion = -1;

    // IDs existants en DB
    private final int idPsychologue = 1; // mohamed
    private final int idPatient     = 2; // meriem

    @BeforeAll
    public static void setup() {
        sqst = new ServiceQuestion();
        sq   = new ServiceQuiz();
        sr   = new ServiceReponse();
        System.out.println("[DEBUG_LOG] Services initialisés.");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        // Le quiz supprime en CASCADE questions + réponses liées
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
    // Méthode utilitaire : créer un quiz de base
    // ══════════════════════════════════════════════════════════════
    private void creerQuiz() throws SQLException {
        Quiz q = new Quiz(
                idPatient, idPsychologue,
                "Quiz test question", "desc",
                "psychologique", true
        );
        sq.add(q);
        idQuiz = q.getIdQuiz();
        System.out.println("[DEBUG_LOG] Quiz créé ID = " + idQuiz);
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 1 : Ajouter une question simple
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(1)
    public void testAddQuestion() throws SQLException {
        creerQuiz();

        Question q = new Question(idQuiz, "Je me sens tendu(e)", 1, "checkbox");
        sqst.add(q);
        idQuestion = q.getIdQuestion();

        System.out.println("[DEBUG_LOG] Question créée ID = " + idQuestion);

        assertTrue(idQuestion > 0, "L'ID de la question doit être > 0");

        // Vérifier en DB
        Question found = sqst.getQuestionById(idQuestion);
        assertNotNull(found, "La question doit exister en DB");
        assertEquals("Je me sens tendu(e)", found.getTexteQuestion());
        assertEquals(1,          found.getOrdre());
        assertEquals("checkbox", found.getTypeQuestion());
        assertEquals(idQuiz,     found.getIdQuiz());

        System.out.println("[DEBUG_LOG] Vérifié : Question existe en DB.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 2 : Ajouter une question avec ses choix
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(2)
    public void testAddQuestionAvecChoix() throws SQLException {
        creerQuiz();

        Question q = new Question(idQuiz, "Je dors mal la nuit", 1, "checkbox");

        sqst.addAvecChoix(q, List.of(
                new Reponse(idQuiz, 0, "Jamais",  0),
                new Reponse(idQuiz, 0, "Parfois", 1),
                new Reponse(idQuiz, 0, "Souvent", 2)
        ));
        idQuestion = q.getIdQuestion();

        System.out.println("[DEBUG_LOG] Question + choix créés ID = " + idQuestion);

        // Vérifier les choix (id_users = NULL)
        List<Reponse> choix = sr.getChoixParQuestion(idQuestion);
        assertEquals(3, choix.size(), "3 choix doivent exister");
        assertTrue(choix.stream().allMatch(r -> r.getIdUsers() == null),
                "Tous les choix doivent avoir id_users = NULL");

        // Vérifier les textes et valeurs
        assertEquals("Jamais",  choix.get(0).getTexteReponse());
        assertEquals("Parfois", choix.get(1).getTexteReponse());
        assertEquals("Souvent", choix.get(2).getTexteReponse());
        assertEquals(0, choix.get(0).getValeur());
        assertEquals(1, choix.get(1).getValeur());
        assertEquals(2, choix.get(2).getValeur());

        System.out.println("[DEBUG_LOG] Vérifié : 3 choix corrects pour Question ID = " + idQuestion);
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 3 : Modifier une question
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(3)
    public void testUpdateQuestion() throws SQLException {
        creerQuiz();

        // Créer
        Question q = new Question(idQuiz, "Texte initial", 1, "checkbox");
        sqst.add(q);
        idQuestion = q.getIdQuestion();

        // Modifier
        q.setTexteQuestion("Texte modifié");
        q.setOrdre(2);
        q.setTypeQuestion("radio");
        sqst.update(q);

        System.out.println("[DEBUG_LOG] Question modifiée ID = " + idQuestion);

        // Vérifier
        Question updated = sqst.getQuestionById(idQuestion);
        assertNotNull(updated);
        assertEquals("Texte modifié", updated.getTexteQuestion());
        assertEquals(2,       updated.getOrdre());
        assertEquals("radio", updated.getTypeQuestion());

        System.out.println("[DEBUG_LOG] Vérifié : Question modifiée avec succès.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 4 : Récupérer les questions d'un quiz
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(4)
    public void testGetQuestionsByQuiz() throws SQLException {
        creerQuiz();

        // Ajouter 3 questions
        sqst.add(new Question(idQuiz, "Question 1", 1, "checkbox"));
        sqst.add(new Question(idQuiz, "Question 2", 2, "checkbox"));
        sqst.add(new Question(idQuiz, "Question 3", 3, "checkbox"));

        List<Question> questions = sqst.getQuestionsByQuiz(idQuiz);

        assertEquals(3, questions.size(), "3 questions doivent exister pour ce quiz");

        // Vérifier l'ordre croissant
        assertEquals(1, questions.get(0).getOrdre());
        assertEquals(2, questions.get(1).getOrdre());
        assertEquals(3, questions.get(2).getOrdre());

        System.out.println("[DEBUG_LOG] Vérifié : " + questions.size() +
                " questions récupérées dans l'ordre pour Quiz ID = " + idQuiz);
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 5 : Récupérer les questions avec leurs choix
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(5)
    public void testGetQuestionsByQuizAvecChoix() throws SQLException {
        creerQuiz();

        // Ajouter 2 questions avec choix
        Question q1 = new Question(idQuiz, "Question 1", 1, "checkbox");
        sqst.addAvecChoix(q1, List.of(
                new Reponse(idQuiz, 0, "Jamais",  0),
                new Reponse(idQuiz, 0, "Parfois", 1),
                new Reponse(idQuiz, 0, "Souvent", 2)
        ));

        Question q2 = new Question(idQuiz, "Question 2", 2, "checkbox");
        sqst.addAvecChoix(q2, List.of(
                new Reponse(idQuiz, 0, "Non",     0),
                new Reponse(idQuiz, 0, "Parfois", 1),
                new Reponse(idQuiz, 0, "Oui",     2)
        ));

        List<Question> questions = sqst.getQuestionsByQuizAvecChoix(idQuiz);

        assertEquals(2, questions.size(), "2 questions doivent exister");

        for (Question question : questions) {
            assertNotNull(question.getReponses(), "Les choix ne doivent pas être null");
            assertEquals(3, question.getReponses().size(),
                    "Chaque question doit avoir 3 choix");
            assertTrue(question.getReponses().stream()
                            .allMatch(r -> r.getIdUsers() == null),
                    "Tous les choix doivent avoir id_users = NULL");
        }

        System.out.println("[DEBUG_LOG] Vérifié : 2 questions avec 3 choix chacune.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 6 : Compter les questions d'un quiz
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(6)
    public void testCountQuestionsByQuiz() throws SQLException {
        creerQuiz();

        // Ajouter 4 questions
        sqst.add(new Question(idQuiz, "Q1", 1, "checkbox"));
        sqst.add(new Question(idQuiz, "Q2", 2, "checkbox"));
        sqst.add(new Question(idQuiz, "Q3", 3, "checkbox"));
        sqst.add(new Question(idQuiz, "Q4", 4, "checkbox"));

        int count = sqst.countQuestionsByQuiz(idQuiz);

        assertEquals(4, count, "4 questions doivent être comptées");

        System.out.println("[DEBUG_LOG] Vérifié : " + count + " questions comptées pour Quiz ID = " + idQuiz);
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 7 : Supprimer une question
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(7)
    public void testDeleteQuestion() throws SQLException {
        creerQuiz();

        // Créer une question avec choix
        Question q = new Question(idQuiz, "Question à supprimer", 1, "checkbox");
        sqst.addAvecChoix(q, List.of(
                new Reponse(idQuiz, 0, "Jamais",  0),
                new Reponse(idQuiz, 0, "Parfois", 1),
                new Reponse(idQuiz, 0, "Souvent", 2)
        ));
        idQuestion = q.getIdQuestion();

        // Supprimer
        sqst.delete(q);

        // Vérifier que la question n'existe plus
        Question deleted = sqst.getQuestionById(idQuestion);
        assertNull(deleted, "La question supprimée ne doit plus exister");

        // Vérifier que les choix ont aussi disparu (CASCADE)
        List<Reponse> choix = sr.getChoixParQuestion(idQuestion);
        assertTrue(choix.isEmpty(), "Les choix doivent être supprimés avec la question");

        idQuestion = -1;
        System.out.println("[DEBUG_LOG] Vérifié : Question + choix supprimés avec succès.");
    }

    // ══════════════════════════════════════════════════════════════
    // ✔ TEST 8 : Quiz complet (questions + choix + réponse patient)
    // ══════════════════════════════════════════════════════════════
    @Test
    @Order(8)
    public void testQuizComplet() throws SQLException {
        creerQuiz();

        // Psychologue crée 2 questions avec choix
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

        // Patient récupère les questions avec choix
        List<Question> questionsAvecChoix = sqst.getQuestionsByQuizAvecChoix(idQuiz);
        assertEquals(2, questionsAvecChoix.size());

        // Patient répond : Parfois(1) + Souvent(2)
        sr.add(new Reponse(idQuiz, q1.getIdQuestion(), idPatient, "Parfois", 1));
        sr.add(new Reponse(idQuiz, q2.getIdQuestion(), idPatient, "Souvent", 2));

        // Calcul du score
        String resultat = sq.calculerEtSauvegarderScore(idQuiz, idPatient);

        assertTrue(resultat.contains("Score: 3"));
        assertTrue(resultat.contains("Niveau: faible"));

        System.out.println("[DEBUG_LOG] Quiz complet validé : " + resultat);
    }
}