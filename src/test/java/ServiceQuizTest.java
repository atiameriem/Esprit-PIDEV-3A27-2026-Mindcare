
import models.Quiz;
import org.junit.jupiter.api.*;
import services.ServiceQuiz;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceQuizTest {

    static ServiceQuiz sq;

    private int idQuiz = -1;
    private final int idPatient = 4;
    private final int idPsychologue = 6;

    @BeforeAll
    static void setup() {
        sq = new ServiceQuiz();
    }

    @AfterEach
    void cleanup() throws SQLException {
        if (idQuiz != -1) {
            Quiz q = sq.getQuizById(idQuiz);
            if (q != null) sq.delete(q);
            idQuiz = -1;
        }
    }

    private void creerQuiz() throws SQLException {
        Quiz quiz = new Quiz();
        quiz.setIdUsers(idPatient);
        quiz.setCreePar(idPsychologue);
        quiz.setTitre("Quiz Test");
        quiz.setDescription("Description test");
        quiz.setTypeTest("Stress");
        quiz.setActif(true);
        quiz.setDateCreation(LocalDateTime.now());
        sq.add(quiz);
        idQuiz = quiz.getIdQuiz();
    }

    @Test
    @Order(1)
    void testAddQuiz() throws SQLException {
        creerQuiz();
        assertTrue(idQuiz > 0);

        Quiz q = sq.getQuizById(idQuiz);
        assertNotNull(q);
        assertEquals("Quiz Test", q.getTitre());
    }

    @Test
    @Order(2)
    void testUpdateQuiz() throws SQLException {
        creerQuiz();
        Quiz q = sq.getQuizById(idQuiz);
        q.setTitre("Quiz Modifié");
        q.setTypeTest("Anxiété");
        q.setActif(false);
        sq.update(q);

        Quiz updated = sq.getQuizById(idQuiz);
        assertEquals("Quiz Modifié", updated.getTitre());
        assertEquals("Anxiété", updated.getTypeTest());
        assertFalse(updated.isActif());
    }

    @Test
    @Order(3)
    void testGetAll() throws SQLException {
        creerQuiz();
        List<Quiz> list = sq.getAll();
        assertTrue(list.stream().anyMatch(q -> q.getIdQuiz() == idQuiz));
    }

    @Test
    @Order(4)
    void testDeleteQuiz() throws SQLException {
        creerQuiz();
        Quiz q = sq.getQuizById(idQuiz);
        sq.delete(q);

        Quiz deleted = sq.getQuizById(idQuiz);
        assertNull(deleted);
        idQuiz = -1;
    }
}