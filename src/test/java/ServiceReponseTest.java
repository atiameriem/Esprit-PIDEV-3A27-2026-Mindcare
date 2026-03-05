import models.Question;
import models.Reponse;
import models.Quiz;
import org.junit.jupiter.api.*;
import services.ServiceQuestion;
import services.ServiceQuiz;
import services.ServiceReponse;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceReponseTest {

    static ServiceQuiz sq;
    static ServiceQuestion sqst;
    static ServiceReponse sr;

    private int idQuiz     = -1;
    private int idQuestion = -1;

    private final int idPatient     = 4;
    private final int idPsychologue = 6;

    @BeforeAll
    static void setup() {
        sq   = new ServiceQuiz();
        sqst = new ServiceQuestion();
        sr   = new ServiceReponse();
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

    private void creerQuizEtQuestion() throws SQLException {
        Quiz quiz = new Quiz();
        quiz.setIdUsers(idPatient);
        quiz.setCreePar(idPsychologue);
        quiz.setTitre("Quiz Test Reponse");
        quiz.setDescription("Description test");
        quiz.setTypeTest("Test");
        quiz.setActif(true);
        quiz.setDateCreation(LocalDateTime.now());
        // ✅ create() au lieu de add()
        sq.create(quiz);
        idQuiz = quiz.getIdQuiz();

        Question q = new Question(idQuiz, "Question test réponse", 1, "checkbox");
        // ✅ create() au lieu de add()
        sqst.create(q);
        idQuestion = q.getIdQuestion();
    }

    @Test
    @Order(1)
    void testAddAndGetReponse() throws SQLException {
        creerQuizEtQuestion();

        Reponse r = new Reponse(idQuiz, idQuestion, idPatient, "Réponse test", 3);
        // ✅ create() au lieu de add()
        sr.create(r);

        List<Reponse> list = sr.getReponsesParQuestionEtUser(idQuestion, idPatient);
        assertFalse(list.isEmpty());
        assertEquals("Réponse test", list.get(0).getTexteReponse());
        assertEquals(idPatient, list.get(0).getIdUsers());
    }

    @Test
    @Order(2)
    void testDeleteReponse() throws SQLException {
        creerQuizEtQuestion();

        Reponse r = new Reponse(idQuiz, idQuestion, idPatient, "Réponse à supprimer", 2);
        // ✅ create() au lieu de add()
        sr.create(r);

        // ✅ delete(int id) au lieu de delete(Reponse r)
        sr.delete(r.getIdReponse());

        List<Reponse> list = sr.getReponsesParQuestionEtUser(idQuestion, idPatient);
        assertTrue(list.isEmpty());
    }
}