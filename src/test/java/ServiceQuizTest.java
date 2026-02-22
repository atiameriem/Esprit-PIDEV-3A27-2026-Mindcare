package tests;

import models.Quiz;
import models.Question;
import models.Reponse;
import org.junit.jupiter.api.*;
import services.ServiceQuiz;
import services.ServiceQuestion;
import services.ServiceReponse;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceQuizTest {

    static ServiceQuiz sq;
    static ServiceQuestion sQuestion;
    static ServiceReponse sReponse;

    private int idQuiz = -1;

    private final int idPatient = 4;
    private final int idPsychologue = 6;

    @BeforeAll
    static void setup() {
        sq = new ServiceQuiz();
        sQuestion = new ServiceQuestion();
        sReponse = new ServiceReponse();
        System.out.println("=== ServiceQuizTest START ===");
    }

    @AfterEach
    void cleanup() throws SQLException {
        if (idQuiz != -1) {
            Quiz q = sq.getQuizById(idQuiz);
            if (q != null) {
                sq.delete(q);
                System.out.println("Cleanup Quiz ID = " + idQuiz);
            }
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

    // ================== ADD ==================
    @Test
    @Order(1)
    void testAddQuiz() throws SQLException {
        creerQuiz();
        assertTrue(idQuiz > 0);

        Quiz q = sq.getQuizById(idQuiz);
        assertNotNull(q);
        assertEquals("Quiz Test", q.getTitre());
    }

    // ================== UPDATE ==================
    @Test
    @Order(2)
    void testUpdateQuiz() throws SQLException {
        creerQuiz();

        Quiz q = sq.getQuizById(idQuiz);
        q.setTitre("Quiz Modifié");
        q.setDescription("Nouvelle description");
        q.setTypeTest("Anxiété");
        q.setActif(false);

        sq.update(q);

        Quiz updated = sq.getQuizById(idQuiz);
        assertEquals("Quiz Modifié", updated.getTitre());
        assertEquals("Anxiété", updated.getTypeTest());
        assertFalse(updated.isActif());
    }

    // ================== GET BY PSY ==================
    @Test
    @Order(3)
    void testGetQuizParPsychologue() throws SQLException {
        creerQuiz();

        List<Quiz> list = sq.getQuizParPsychologue(idPsychologue);
        assertTrue(list.stream().anyMatch(q -> q.getIdQuiz() == idQuiz));
    }

    // ================== GET BY PATIENT ==================
    @Test
    @Order(4)
    void testGetQuizParPatient() throws SQLException {
        creerQuiz();

        List<Quiz> list = sq.getQuizParPatient(idPatient);
        assertTrue(list.stream().anyMatch(q -> q.getIdQuiz() == idQuiz));
    }

    // ================== GET ALL ==================
    @Test
    @Order(5)
    void testGetAll() throws SQLException {
        creerQuiz();

        List<Quiz> list = sq.getAll();
        assertTrue(list.stream().anyMatch(q -> q.getIdQuiz() == idQuiz));
    }

    // ================== GET QUIZ WITH QUESTIONS ==================
    @Test
    @Order(6)
    void testGetQuizWithQuestions() throws SQLException {
        creerQuiz();

        Question q1 = new Question(idQuiz, "Q1 ?", 1, "checkbox");
        sQuestion.add(q1);

        Quiz quiz = sq.getQuizById(idQuiz);
        assertNotNull(quiz.getQuestions());
        assertEquals(1, quiz.getQuestions().size());
    }

    // ================== CALCUL SCORE ==================
    @Test
    @Order(7)
    void testCalculerEtSauvegarderScore() throws SQLException {
        creerQuiz();

        Question q1 = new Question(idQuiz, "Q1 ?", 1, "checkbox");
        sQuestion.add(q1);

        sReponse.add(new Reponse(idQuiz, q1.getIdQuestion(), idPatient, "A", 3));
        sReponse.add(new Reponse(idQuiz, q1.getIdQuestion(), idPatient, "B", 4));

        String result = sq.calculerEtSauvegarderScore(idQuiz, idPatient);

        assertTrue(result.contains("Score"));
        assertTrue(result.contains("Niveau"));
    }

    // ================== HISTORIQUE ==================
    @Test
    @Order(8)
    void testGetHistoriquePatient() throws SQLException {
        creerQuiz();

        Question q1 = new Question(idQuiz, "Q1 ?", 1, "checkbox");
        sQuestion.add(q1);

        sReponse.add(new Reponse(idQuiz, q1.getIdQuestion(), idPatient, "A", 5));

        sq.calculerEtSauvegarderScore(idQuiz, idPatient);

        List<String> historique = sq.getHistoriquePatient(idPatient);

        assertFalse(historique.isEmpty());
    }

    // ================== GET PATIENTS ==================
    @Test
    @Order(9)
    void testGetTousLesPatients() throws SQLException {
        Map<Integer, String> patients = sq.getTousLesPatients();
        assertTrue(patients.containsKey(idPatient));
    }

    // ================== DELETE ==================
    @Test
    @Order(10)
    void testDeleteQuiz() throws SQLException {
        creerQuiz();

        Quiz q = sq.getQuizById(idQuiz);
        sq.delete(q);

        Quiz deleted = sq.getQuizById(idQuiz);
        assertNull(deleted);
        idQuiz = -1;
    }
}
