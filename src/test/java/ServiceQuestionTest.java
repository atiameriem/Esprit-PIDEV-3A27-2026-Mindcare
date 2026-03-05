import models.Question;
import models.Quiz;
import org.junit.jupiter.api.*;
import services.ServiceQuestion;
import services.ServiceQuiz;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceQuestionTest {

    static ServiceQuestion sqst;
    static ServiceQuiz     sq;

    private int idQuiz     = -1;
    private int idQuestion = -1;

    private final int idPatient     = 4;
    private final int idPsychologue = 6;

    @BeforeAll
    static void setup() {
        sqst = new ServiceQuestion();
        sq   = new ServiceQuiz();
    }

    @AfterEach
    void cleanup() throws SQLException {
        if (idQuestion != -1) {
            // ✅ delete(int id) au lieu de delete(Question q)
            sqst.delete(idQuestion);
            idQuestion = -1;
        }
        if (idQuiz != -1) {
            Quiz q = sq.getQuizById(idQuiz);
            // ✅ delete(int id) au lieu de delete(Quiz q)
            if (q != null) sq.delete(idQuiz);
            idQuiz = -1;
        }
    }

    private void creerQuiz() throws SQLException {
        Quiz q = new Quiz();
        q.setIdUsers(idPatient);
        q.setCreePar(idPsychologue);
        q.setTitre("Quiz Test Question");
        q.setDescription("Description test");
        q.setTypeTest("Test");
        q.setActif(true);
        q.setDateCreation(LocalDateTime.now());
        // ✅ create() au lieu de add()
        sq.create(q);
        idQuiz = q.getIdQuiz();
    }

    @Test
    @Order(1)
    void testCreateAndGetQuestion() throws SQLException {
        creerQuiz();
        Question q = new Question(idQuiz, "Question test add/get", 1, "checkbox");
        // ✅ create() au lieu de add()
        sqst.create(q);
        idQuestion = q.getIdQuestion();

        assertTrue(idQuestion > 0);

        Question fromDb = sqst.getQuestionById(idQuestion);
        assertNotNull(fromDb);
        assertEquals("Question test add/get", fromDb.getTexteQuestion());
        assertEquals(1, fromDb.getOrdre());
        assertEquals("checkbox", fromDb.getTypeQuestion());
    }

    @Test
    @Order(2)
    void testUpdateQuestion() throws SQLException {
        creerQuiz();
        Question q = new Question(idQuiz, "Avant update", 1, "checkbox");
        // ✅ create() au lieu de add()
        sqst.create(q);
        idQuestion = q.getIdQuestion();

        q.setTexteQuestion("Après update");
        q.setOrdre(2);
        q.setTypeQuestion("radio");
        // ✅ update() inchangé
        sqst.update(q);

        Question qDb = sqst.getQuestionById(idQuestion);
        assertEquals("Après update", qDb.getTexteQuestion());
        assertEquals(2, qDb.getOrdre());
        assertEquals("radio", qDb.getTypeQuestion());
    }

    @Test
    @Order(3)
    void testDeleteQuestion() throws SQLException {
        creerQuiz();
        Question q = new Question(idQuiz, "À supprimer", 1, "checkbox");
        // ✅ create() au lieu de add()
        sqst.create(q);
        idQuestion = q.getIdQuestion();

        // ✅ delete(int id) au lieu de delete(Question q)
        sqst.delete(idQuestion);

        Question qDb = sqst.getQuestionById(idQuestion);
        assertNull(qDb);
        idQuestion = -1; // déjà supprimé, évite double delete dans cleanup
    }
}