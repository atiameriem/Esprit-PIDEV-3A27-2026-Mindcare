package test;

import models.Quiz;
import models.Question;
import models.Reponse;
import services.ServiceQuiz;
import services.ServiceQuestion;
import services.ServiceReponse;

import java.sql.SQLException;
import java.util.List;

public class Main  {

    public static void main(String[] args) throws SQLException {

        ServiceQuiz serviceQuiz = new ServiceQuiz();
        ServiceQuestion serviceQuestion = new ServiceQuestion();
        ServiceReponse serviceReponse = new ServiceReponse();

        int idPsychologue = 6;
        int idPatient     = 5;

        // Liste des types à ajouter
        String[] typesQuiz = {
                "BIEN_ETRE", "STRESS", "HUMEUR",
                "psychologique", "cognitif", "comportemental", "émotionnel"
        };

        for (String type : typesQuiz) {

            System.out.println("\n===== Création quiz : " + type + " =====");

            // Créer le quiz
            Quiz quiz = new Quiz(
                    idPatient,
                    idPsychologue,
                    "Quiz " + type,
                    "Évalue le niveau de " + type.toLowerCase() + " du patient",
                    type,
                    true
            );
            serviceQuiz.add(quiz);
            System.out.println("Quiz créé : " + quiz);

            // Ajouter 3 questions simulées
            for (int q = 1; q <= 3; q++) {
                Question question = new Question(
                        quiz.getIdQuiz(),
                        "Question " + q + " sur " + type,
                        q,
                        "checkbox"
                );

                // Ajouter 3 choix de réponses
                serviceQuestion.addAvecChoix(question, List.of(
                        new Reponse(quiz.getIdQuiz(), 0, "Jamais", 0),
                        new Reponse(quiz.getIdQuiz(), 0, "Parfois", 1),
                        new Reponse(quiz.getIdQuiz(), 0, "Souvent", 2)
                ));
                System.out.println("Question " + q + " + choix ajoutés.");
            }

            // Simuler que le patient répond aux questions
            List<Question> questionsAvecChoix =
                    serviceQuestion.getQuestionsByQuizAvecChoix(quiz.getIdQuiz());

            int[] reponsesPatient = {1, 2, 1}; // valeurs simulées
            for (int i = 0; i < questionsAvecChoix.size(); i++) {
                Question qObj = questionsAvecChoix.get(i);
                Reponse choixSel = qObj.getReponses().get(reponsesPatient[i]);

                Reponse reponsePatient = new Reponse(
                        quiz.getIdQuiz(),
                        qObj.getIdQuestion(),
                        idPatient,
                        choixSel.getTexteReponse(),
                        choixSel.getValeur()
                );
                serviceReponse.add(reponsePatient);
            }

            // Calcul du score
            String resultat = serviceQuiz.calculerEtSauvegarderScore(
                    quiz.getIdQuiz(), idPatient
            );
            System.out.println("Résultat du quiz " + type + " : " + resultat);
        }

        System.out.println("\n✅ Tous les quiz simulés ont été ajoutés pour le patient " + idPatient);
    }
}
