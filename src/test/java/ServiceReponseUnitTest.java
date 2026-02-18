
import models.Quiz;
import models.Question;
import models.Reponse;
import org.junit.jupiter.api.*;
import services.ServiceQuiz;
import services.ServiceQuestion;
import services.ServiceReponse;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceReponseUnitTest {

    static ServiceQuiz sq;
    static ServiceQuestion sQuestion;
    static ServiceReponse sReponse;

    private int idQuiz = -1;
    private int idQuestion = -1;

    private final int idPatient = 4;
    private final int idPsychologue = 6;

    @BeforeAll
    static void setup() {
        sq = new ServiceQuiz();
        sQuestion = new ServiceQuestion();
        sReponse = new ServiceReponse();
        System.out.println("=== ServiceReponseTest START ===");
    }

    @AfterEach
    void cleanup() throws SQLException {

        if (idQuestion != -1) {
            Question q = new Question();
            q.setIdQuestion(idQuestion);
            sQuestion.delete(q);
            idQuestion = -1;
        }

        if (idQuiz != -1) {
            Quiz quiz = sq.getQuizById(idQuiz);
            if (quiz != null) sq.delete(quiz);
            idQuiz = -1;
        }
    }

    private void creerQuizEtQuestion() throws SQLException {

        Quiz quiz = new Quiz();
        quiz.setIdUsers(idPatient);
        quiz.setCreePar(idPsychologue);
        quiz.setTitre("Quiz Reponse Test");
        quiz.setDescription("desc");
        quiz.setTypeTest("Stress");
        quiz.setActif(true);
        quiz.setDateCreation(LocalDateTime.now());

        sq.add(quiz);
        idQuiz = quiz.getIdQuiz();

        Question question = new Question(idQuiz, "Question test ?", 1, "checkbox");
        sQuestion.add(question);
        idQuestion = question.getIdQuestion();
    }

    // ================= ADD PATIENT =================
    @Test
    @Order(1)
    void testAddReponsePatient() throws SQLException {

        creerQuizEtQuestion();

        Reponse r = new Reponse(idQuiz, idQuestion, idPatient, "Oui", 4);
        sReponse.add(r);

        assertTrue(r.getIdReponse() > 0);
    }

    // ================= ADD CHOIX PSY =================
    @Test
    @Order(2)
    void testAddChoixPsychologue() throws SQLException {

        creerQuizEtQuestion();

        Reponse choix = new Reponse(idQuiz, idQuestion, "Choix Psy", 5);
        sReponse.add(choix);

        assertNull(choix.getIdUsers());
        assertTrue(choix.getIdReponse() > 0);
    }

    // ================= UPDATE =================
    @Test
    @Order(3)
    void testUpdateReponse() throws SQLException {

        creerQuizEtQuestion();

        Reponse r = new Reponse(idQuiz, idQuestion, idPatient, "Non", 2);
        sReponse.add(r);

        r.setTexteReponse("Peut-être");
        r.setValeur(5);
        sReponse.update(r);

        List<Reponse> list =
                sReponse.getReponsesParPatientEtQuiz(idPatient, idQuiz);

        assertEquals("Peut-être", list.get(0).getTexteReponse());
    }

    // ================= DELETE =================
    @Test
    @Order(4)
    void testDeleteReponse() throws SQLException {

        creerQuizEtQuestion();

        Reponse r = new Reponse(idQuiz, idQuestion, idPatient, "Supprimer", 1);
        sReponse.add(r);

        sReponse.delete(r);

        List<Reponse> list =
                sReponse.getReponsesParPatientEtQuiz(idPatient, idQuiz);

        assertTrue(list.isEmpty());
    }

    // ================= GET ALL =================
    @Test
    @Order(5)
    void testGetAll() throws SQLException {

        creerQuizEtQuestion();

        Reponse r = new Reponse(idQuiz, idQuestion, idPatient, "TestAll", 3);
        sReponse.add(r);

        List<Reponse> list = sReponse.getAll();

        assertTrue(list.stream()
                .anyMatch(rep -> rep.getIdQuiz() == idQuiz));
    }

    // ================= GET CHOIX PAR QUESTION =================
    @Test
    @Order(6)
    void testGetChoixParQuestion() throws SQLException {

        creerQuizEtQuestion();

        Reponse choix1 = new Reponse(idQuiz, idQuestion, "Choix 1", 3);
        Reponse choix2 = new Reponse(idQuiz, idQuestion, "Choix 2", 5);

        sReponse.add(choix1);
        sReponse.add(choix2);

        List<Reponse> choix =
                sReponse.getChoixParQuestion(idQuestion);

        assertEquals(2, choix.size());
        assertTrue(choix.get(0).isChoixPsychologue());
    }

    // ================= SCORE =================
    @Test
    @Order(7)
    void testCalculerScorePourcent() throws SQLException {

        creerQuizEtQuestion();

        sReponse.add(new Reponse(idQuiz, idQuestion, idPatient, "A", 5));
        sReponse.add(new Reponse(idQuiz, idQuestion, idPatient, "B", 5));

        int score =
                sReponse.calculerScorePourcent(idPatient, idQuiz);

        assertEquals(100, score);
    }

    // ================= CONSEIL =================
    @Test
    @Order(8)
    void testGetConseil() {

        assertEquals("Excellent, continuez comme ça !",
                sReponse.getConseil(90));

        assertEquals("Bien, mais quelques améliorations sont possibles.",
                sReponse.getConseil(65));

        assertEquals("Travaillez davantage sur ce sujet.",
                sReponse.getConseil(30));
    }

    // ================= FILTRE PAR TYPE =================
    @Test
    @Order(9)
    void testGetReponsesParTypes() throws SQLException {

        creerQuizEtQuestion();

        sReponse.add(new Reponse(idQuiz, idQuestion,
                idPatient, "TypeTest", 4));

        List<Reponse> list =
                sReponse.getReponsesParTypes(idPatient,
                        Arrays.asList("Stress"));

        assertFalse(list.isEmpty());
    }
}
