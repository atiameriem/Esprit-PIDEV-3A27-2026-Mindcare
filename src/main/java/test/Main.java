package test;

import models.Quiz;
import models.Question;
import models.Reponse;
import services.ServiceQuiz;
import services.ServiceQuestion;
import services.ServiceReponse;

import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws SQLException {

        ServiceQuiz     serviceQuiz     = new ServiceQuiz();
        ServiceQuestion serviceQuestion = new ServiceQuestion();
        ServiceReponse  serviceReponse  = new ServiceReponse();

        // ══════════════════════════════════════════════════════
        // ✔ 1. PSYCHOLOGUE CRÉE UN QUIZ
        // ══════════════════════════════════════════════════════
        System.out.println("\n===== 1. Création du quiz =====");

        int idPsychologue = 6; // vnbm
        int idPatient     = 4; // meriem

        Quiz quiz = new Quiz(
                idPatient,      // patient assigné
                idPsychologue,  // psychologue créateur
                "Test de stress",
                "Évalue le niveau de stress du patient",
                "psychologique",
                true
        );
        serviceQuiz.add(quiz);
        System.out.println("Quiz créé : " + quiz);

        // ══════════════════════════════════════════════════════
        // ✔ 2 & 3. AJOUTER QUESTIONS + CHOIX
        // ══════════════════════════════════════════════════════
        System.out.println("\n===== 2 & 3. Ajout questions + choix =====");

        // Question 1
        Question q1 = new Question(quiz.getIdQuiz(),
                "Je me sens tendu(e) la plupart du temps", 1, "checkbox");

        serviceQuestion.addAvecChoix(q1, List.of(
                new Reponse(quiz.getIdQuiz(), 0, "Jamais",  0),
                new Reponse(quiz.getIdQuiz(), 0, "Parfois", 1),
                new Reponse(quiz.getIdQuiz(), 0, "Souvent", 2)
        ));

        // Question 2
        Question q2 = new Question(quiz.getIdQuiz(),
                "Je dors mal la nuit", 2, "checkbox");

        serviceQuestion.addAvecChoix(q2, List.of(
                new Reponse(quiz.getIdQuiz(), 0, "Jamais",  0),
                new Reponse(quiz.getIdQuiz(), 0, "Parfois", 1),
                new Reponse(quiz.getIdQuiz(), 0, "Souvent", 2)
        ));

        // Question 3
        Question q3 = new Question(quiz.getIdQuiz(),
                "J'ai du mal à me concentrer", 3, "checkbox");

        serviceQuestion.addAvecChoix(q3, List.of(
                new Reponse(quiz.getIdQuiz(), 0, "Jamais",  0),
                new Reponse(quiz.getIdQuiz(), 0, "Parfois", 1),
                new Reponse(quiz.getIdQuiz(), 0, "Souvent", 2)
        ));

        System.out.println("3 questions + choix ajoutés pour quiz ID = " + quiz.getIdQuiz());

        // ══════════════════════════════════════════════════════
        // ✔ 4. PATIENT PASSE LE TEST
        // ══════════════════════════════════════════════════════
        System.out.println("\n===== 4. Patient passe le test =====");

        // Charger les questions avec leurs choix pour les afficher au patient
        List<Question> questionsAvecChoix =
                serviceQuestion.getQuestionsByQuizAvecChoix(quiz.getIdQuiz());

        // Simuler les réponses du patient
        // meriem répond : "Parfois" (1), "Souvent" (2), "Parfois" (1)
        int[] reponsesPatient = {1, 2, 1};

        for (int i = 0; i < questionsAvecChoix.size(); i++) {
            Question q        = questionsAvecChoix.get(i);
            Reponse  choixSel = q.getReponses().get(reponsesPatient[i]);
            // index 0=Jamais, 1=Parfois, 2=Souvent

            Reponse reponsePatient = new Reponse(
                    quiz.getIdQuiz(),
                    q.getIdQuestion(),
                    idPatient,            // id_users = patient
                    choixSel.getTexteReponse(),
                    choixSel.getValeur()
            );
            serviceReponse.add(reponsePatient);

            System.out.println("Q" + (i+1) + " → " +
                    q.getTexteQuestion() + " : " + choixSel.getTexteReponse() +
                    " (valeur=" + choixSel.getValeur() + ")");
        }

        // ══════════════════════════════════════════════════════
        // ✔ 5. CALCUL AUTOMATIQUE DU RÉSULTAT
        // ══════════════════════════════════════════════════════
        System.out.println("\n===== 5. Calcul du résultat =====");

        String resultat = serviceQuiz.calculerEtSauvegarderScore(
                quiz.getIdQuiz(), idPatient
        );
        System.out.println("Résultat : " + resultat);

        // ══════════════════════════════════════════════════════
        // ✔ 6 & 7. HISTORIQUE + SUIVI ÉVOLUTION (vue psychologue)
        // ══════════════════════════════════════════════════════
        System.out.println("\n===== 6 & 7. Historique & évolution =====");

        List<String> historique = serviceQuiz.getHistoriquePatient(idPatient);

        System.out.println("Évolution de meriem (patient ID=" + idPatient + ") :");
        historique.forEach(ligne -> System.out.println("  → " + ligne));
    }
}
