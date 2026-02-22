
import models.Question;
import models.Reponse;
import models.Quiz;
import org.junit.jupiter.api.*;
import services.ServiceQuestion;
import services.ServiceQuiz;
import services.ServiceReponse;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceQuestionTest {

    static ServiceQuestion sqst;
    static ServiceQuiz sq;
    static ServiceReponse sr;

    private int idQuiz = -1;
    private int idQuestion = -1;

    private final int idPsychologue = 6;
    private final int idPatient = 4;

    @BeforeAll
    public static void setup() {
        sqst = new ServiceQuestion();
        sq = new ServiceQuiz();
        sr = new ServiceReponse();
    }

    @AfterEach
    void cleanup() throws SQLException {
        if (idQuiz != -1) {
            Quiz q = sq.getQuizById(idQuiz);
            if (q != null) sq.delete(q);
            idQuiz = -1;
            idQuestion = -1;
        }
    }

    private void creerQuiz() throws SQLException {
        Quiz q = new Quiz(idPatient, idPsychologue, "Quiz Test", "desc", "psychologique", true);
        sq.add(q);
        idQuiz = q.getIdQuiz();
    }

    @Test
    @Order(1)
    public void testAddAndGetQuestion() throws SQLException {
        creerQuiz();

        Question q = new Question(idQuiz, "Test question add/get", 1, "checkbox");
        sqst.add(q);
        idQuestion = q.getIdQuestion();
        assertTrue(idQuestion > 0);

        Question fromDb = sqst.getQuestionById(idQuestion);
        assertNotNull(fromDb);
        assertEquals("Test question add/get", fromDb.getTexteQuestion());
        assertEquals(1, fromDb.getOrdre());
        assertEquals("checkbox", fromDb.getTypeQuestion());
    }

    @Test
    @Order(2)
    public void testAddAvecChoix() throws SQLException {
        creerQuiz();

        Question q = new Question(idQuiz, "Question avec choix", 1, "checkbox");
        sqst.addAvecChoix(q, List.of(
                new Reponse(idQuiz, 0, "A", 0),
                new Reponse(idQuiz, 0, "B", 1),
                new Reponse(idQuiz, 0, "C", 2)
        ));
        idQuestion = q.getIdQuestion();

        Question qDb = sqst.getQuestionById(idQuestion);
        assertNotNull(qDb);
        assertEquals(3, qDb.getReponses().size());
        assertTrue(qDb.getReponses().stream().allMatch(r -> r.getIdUsers() == null));
    }

    @Test
    @Order(3)
    public void testUpdateQuestion() throws SQLException {
        creerQuiz();
        Question q = new Question(idQuiz, "Avant update", 1, "checkbox");
        sqst.add(q);
        idQuestion = q.getIdQuestion();

        q.setTexteQuestion("Apres update");
        q.setOrdre(2);
        q.setTypeQuestion("radio");
        sqst.update(q);

        Question qDb = sqst.getQuestionById(idQuestion);
        assertEquals("Apres update", qDb.getTexteQuestion());
        assertEquals(2, qDb.getOrdre());
        assertEquals("radio", qDb.getTypeQuestion());
    }

    @Test
    @Order(4)
    public void testGetQuestionsByQuiz() throws SQLException {
        creerQuiz();
        sqst.add(new Question(idQuiz, "Q1", 1, "checkbox"));
        sqst.add(new Question(idQuiz, "Q2", 2, "checkbox"));
        sqst.add(new Question(idQuiz, "Q3", 3, "checkbox"));

        List<Question> list = sqst.getQuestionsByQuiz(idQuiz);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0).getOrdre());
        assertEquals(2, list.get(1).getOrdre());
        assertEquals(3, list.get(2).getOrdre());
    }

    @Test
    @Order(5)
    public void testGetQuestionsByQuizAvecChoix() throws SQLException {
        creerQuiz();
        Question q1 = new Question(idQuiz, "Q1 avec choix", 1, "checkbox");
        sqst.addAvecChoix(q1, List.of(
                new Reponse(idQuiz, 0, "Oui", 1),
                new Reponse(idQuiz, 0, "Non", 0)
        ));
        Question q2 = new Question(idQuiz, "Q2 avec choix", 2, "checkbox");
        sqst.addAvecChoix(q2, List.of(
                new Reponse(idQuiz, 0, "A", 1),
                new Reponse(idQuiz, 0, "B", 2)
        ));

        List<Question> list = sqst.getQuestionsByQuizAvecChoix(idQuiz);
        assertEquals(2, list.size());
        for (Question q : list) {
            assertNotNull(q.getReponses());
            assertFalse(q.getReponses().isEmpty());
        }
    }

    @Test
    @Order(6)
    public void testGetQuestionAvecReponsesPatient() throws SQLException {
        creerQuiz();
        Question q = new Question(idQuiz, "Q patient", 1, "checkbox");
        sqst.add(q);
        idQuestion = q.getIdQuestion();

        sr.add(new Reponse(idQuiz, idQuestion, idPatient, "Réponse patient", 2));

        Question qDb = sqst.getQuestionAvecReponsesPatient(idQuestion, idPatient);
        assertEquals(1, qDb.getReponses().size());
        assertEquals("Réponse patient", qDb.getReponses().get(0).getTexteReponse());
        assertEquals(idPatient, qDb.getReponses().get(0).getIdUsers());
    }

    @Test
    @Order(7)
    public void testCountQuestionsByQuiz() throws SQLException {
        creerQuiz();
        sqst.add(new Question(idQuiz, "Q1", 1, "checkbox"));
        sqst.add(new Question(idQuiz, "Q2", 2, "checkbox"));

        int count = sqst.countQuestionsByQuiz(idQuiz);
        assertEquals(2, count);
    }

    @Test
    @Order(8)
    public void testDeleteQuestion() throws SQLException {
        creerQuiz();
        Question q = new Question(idQuiz, "A supprimer", 1, "checkbox");
        sqst.addAvecChoix(q, List.of(
                new Reponse(idQuiz, 0, "A", 1),
                new Reponse(idQuiz, 0, "B", 2)
        ));
        idQuestion = q.getIdQuestion();

        sqst.delete(q);

        Question qDb = sqst.getQuestionById(idQuestion);
        assertNull(qDb);
        assertTrue(sr.getChoixParQuestion(idQuestion).isEmpty());
    }
}
