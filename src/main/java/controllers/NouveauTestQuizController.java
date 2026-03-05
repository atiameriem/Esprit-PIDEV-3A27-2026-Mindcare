package controllers;

import services.ServiceEmailQuiz;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.Question;
import models.Quiz;
import models.Reponse;
import services.ServiceQuestion;
import services.ServiceQuiz;
import services.ServiceReponse;
import utils.MyDatabase;
import utils.Session;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NouveauTestQuizController {

    @FXML private Label            labelTitrePage;
    @FXML private Label            labelSousTitrePage;
    @FXML private TextField        fieldTitre;
    @FXML private TextArea         fieldDescription;
    @FXML private ComboBox<String> comboTypeTest;
    @FXML private VBox             listeQuestionsForm;
    @FXML private Label            labelAucuneQuestion;
    @FXML private Button           btnSauvegarder;

    private final ServiceQuiz     serviceQuiz     = new ServiceQuiz();
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private final ServiceReponse  serviceReponse  = new ServiceReponse();

    private int idPsychologue  = Session.getUserId();
    private int idPatientCible = -1;
    private Quiz    quizEnEdition = null;
    private boolean modeEdition   = false;
    private boolean venantDeEspacePraticien = false;

    private final List<QuestionBloc> questionBlocs = new ArrayList<>();

    public void setIdPatientCible(int idPatientCible) {
        if (idPatientCible <= 0) return;
        this.idPatientCible = idPatientCible;
        this.venantDeEspacePraticien = true;

        String sql = "SELECT nom FROM users WHERE id_users = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPatientCible);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && labelSousTitrePage != null)
                labelSousTitrePage.setText("Création d'un test pour : " + rs.getString("nom"));
        } catch (SQLException e) {
            System.err.println("Erreur chargement patient : " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        Session.Role role = Session.getRoleConnecte();

        // ✅ Seuls psychologue et admin peuvent créer des tests
        if (role != Session.Role.PSYCHOLOGUE && role != Session.Role.ADMIN) {
            if (labelTitrePage != null)
                labelTitrePage.setText("⛔ Accès réservé aux psychologues.");
            if (labelSousTitrePage != null)
                labelSousTitrePage.setText("Vous n'avez pas les droits nécessaires.");
            if (btnSauvegarder != null)
                btnSauvegarder.setDisable(true);
            // ✅ Cacher le formulaire entier
            if (listeQuestionsForm != null) {
                listeQuestionsForm.setVisible(false);
                listeQuestionsForm.setManaged(false);
            }
            if (fieldTitre != null)       fieldTitre.setDisable(true);
            if (fieldDescription != null) fieldDescription.setDisable(true);
            if (comboTypeTest != null)    comboTypeTest.setDisable(true);
            return;
        }

        // ✅ Psychologue/Admin connecté
        idPsychologue = Session.getUserId() > 0 ? Session.getUserId() : 6;

        comboTypeTest.setItems(FXCollections.observableArrayList(
                "psychologique", "cognitif", "comportemental",
                "émotionnel", "STRESS", "BIEN_ETRE", "HUMEUR"
        ));
        comboTypeTest.getSelectionModel().selectFirst();

        if (labelSousTitrePage != null)
            labelSousTitrePage.setText("📢 Ce test sera envoyé à tous les patients");
    }
    public void setQuizPourEdition(Quiz quiz) {
        this.quizEnEdition  = quiz;
        this.modeEdition    = true;
        this.idPatientCible = quiz.getIdUsers();
        labelTitrePage.setText("Modifier le Test");
        labelSousTitrePage.setText("Modifiez les informations, questions et choix");
        btnSauvegarder.setText("💾  Mettre à jour le test");
        fieldTitre.setText(quiz.getTitre());
        fieldDescription.setText(quiz.getDescription() != null ? quiz.getDescription() : "");
        if (quiz.getTypeTest() != null) comboTypeTest.getSelectionModel().select(quiz.getTypeTest());
        chargerQuestionsExistantes(quiz.getIdQuiz());
    }

    private void chargerQuestionsExistantes(int idQuiz) {
        try {
            List<Question> questions = serviceQuestion.getQuestionsByQuizAvecChoix(idQuiz);
            listeQuestionsForm.getChildren().clear();
            questionBlocs.clear();
            if (questions.isEmpty()) { labelAucuneQuestion.setVisible(true); return; }
            labelAucuneQuestion.setVisible(false);
            for (int i = 0; i < questions.size(); i++) {
                QuestionBloc bloc = new QuestionBloc(i + 1, questions.get(i));
                questionBlocs.add(bloc);
                listeQuestionsForm.getChildren().add(bloc.getNode());
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement questions : " + e.getMessage());
        }
    }

    @FXML
    private void ajouterQuestion() {
        labelAucuneQuestion.setVisible(false);
        QuestionBloc bloc = new QuestionBloc(questionBlocs.size() + 1, null);
        questionBlocs.add(bloc);
        listeQuestionsForm.getChildren().add(bloc.getNode());
    }

    @FXML
    private void sauvegarderTest() {
        if (fieldTitre.getText().trim().isEmpty()) { afficherAlerte("Erreur", "Le titre est obligatoire."); return; }
        if (questionBlocs.isEmpty()) { afficherAlerte("Erreur", "Ajoutez au moins une question."); return; }
        for (QuestionBloc b : questionBlocs) {
            if (b.getTexteQuestion().trim().isEmpty()) { afficherAlerte("Erreur", "Remplissez le texte de toutes les questions."); return; }
            if (b.getChoix().size() < 2) { afficherAlerte("Erreur", "Chaque question doit avoir au moins 2 choix."); return; }
        }
        try {
            if (modeEdition) mettreAJourQuiz(); else creerNouveauQuiz();
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de sauvegarder : " + e.getMessage());
        }
    }

    private void creerNouveauQuiz() throws SQLException {
        int idUserPourQuiz = idPatientCible > 0 ? idPatientCible : 0;
        Quiz quiz = new Quiz(idUserPourQuiz, idPsychologue,
                fieldTitre.getText().trim(), fieldDescription.getText().trim(),
                comboTypeTest.getValue(), true);
        // ✅ create() au lieu de add()
        serviceQuiz.create(quiz);
        sauvegarderQuestions(quiz.getIdQuiz());
        envoyerEmailsPatients(quiz);
        String msg = idPatientCible > 0 ? "📧 Email envoyé au patient." : "📧 Email envoyé à tous les patients.";
        afficherInfo("Succès", "Test \"" + quiz.getTitre() + "\" créé !\n" + msg);
        retourListe();
    }

    private void envoyerEmailsPatients(Quiz quiz) {
        final int patientCible = this.idPatientCible;
        final Quiz quizFinal   = quiz;
        Thread emailThread = new Thread(() -> {
            String sql = patientCible > 0
                    ? "SELECT id_users, nom, email FROM users WHERE id_users = " + patientCible + " AND email IS NOT NULL AND email != ''"
                    : "SELECT id_users, nom, email FROM users WHERE LOWER(role) = 'patient' AND email IS NOT NULL AND email != ''";
            try (Connection conn = MyDatabase.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                ServiceEmailQuiz se = new ServiceEmailQuiz();
                int nbEnvoyes = 0;
                while (rs.next()) {
                    String nom = rs.getString("nom"), email = rs.getString("email");
                    try {
                        String html = se.templateNouveauQuiz(nom, quizFinal.getTitre(),
                                quizFinal.getDescription() != null ? quizFinal.getDescription() : "Test psychologique",
                                quizFinal.getTypeTest() != null ? quizFinal.getTypeTest() : "psychologique");
                        se.envoyerEmail(email, "🧠 MindCare — Nouveau test : " + quizFinal.getTitre(), html);
                        nbEnvoyes++;
                    } catch (Exception e) {
                        System.err.println("Erreur email pour " + email + " : " + e.getMessage());
                    }
                }
                System.out.println("✅ " + nbEnvoyes + " email(s) envoyé(s).");
            } catch (Exception e) {
                System.err.println("Erreur récupération patients : " + e.getMessage());
            }
        });
        emailThread.setDaemon(true);
        emailThread.start();
    }

    private void mettreAJourQuiz() throws SQLException {
        quizEnEdition.setTitre(fieldTitre.getText().trim());
        quizEnEdition.setDescription(fieldDescription.getText().trim());
        quizEnEdition.setTypeTest(comboTypeTest.getValue());
        serviceQuiz.update(quizEnEdition);
        List<Question> anciennes = serviceQuestion.getQuestionsByQuiz(quizEnEdition.getIdQuiz());
        for (Question q : anciennes) {
            // ✅ delete(int id) au lieu de delete(Reponse r)
            for (Reponse r : serviceReponse.getChoixParQuestion(q.getIdQuestion()))
                serviceReponse.delete(r.getIdReponse());
            // ✅ delete(int id) au lieu de delete(Question q)
            serviceQuestion.delete(q.getIdQuestion());
        }
        sauvegarderQuestions(quizEnEdition.getIdQuiz());
        afficherInfo("Succès", "Test \"" + quizEnEdition.getTitre() + "\" mis à jour !");
        retourListe();
    }

    private void sauvegarderQuestions(int idQuiz) throws SQLException {
        for (int i = 0; i < questionBlocs.size(); i++) {
            QuestionBloc bloc = questionBlocs.get(i);
            Question q = new Question(idQuiz, bloc.getTexteQuestion(), i + 1, "radio");
            List<Reponse> choix = new ArrayList<>();
            for (QuestionBloc.ChoixEntry e : bloc.getChoix())
                choix.add(new Reponse(idQuiz, 0, e.texte, e.valeur));
            // ✅ addAvecChoix reste valide (alias conservé dans ServiceQuestion)
            serviceQuestion.addAvecChoix(q, choix);
        }
    }

    @FXML
    private void retourListe() {
        String fxml = venantDeEspacePraticien
                ? "EspacepraticienQuiz.fxml"
                : "passerTests.fxml";
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxml));
            Node vue = loader.load();
            VBox parent = (VBox) fieldTitre.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (IOException e) {
            System.err.println("Erreur retour (" + fxml + ") : " + e.getMessage());
        }
    }

    private void afficherAlerte(String t, String m) {
        Alert a = new Alert(Alert.AlertType.WARNING); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
    private void afficherInfo(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    // ════════════════════════════════════════════════════════
    // Classe interne : bloc question
    // ════════════════════════════════════════════════════════
    private class QuestionBloc {

        private final VBox             node;
        private final TextField        fieldTexte;
        private final VBox             choixContainer;
        private final List<ChoixEntry> choixEntries = new ArrayList<>();

        QuestionBloc(int numero, Question question) {
            node = new VBox(10);
            node.setPadding(new Insets(16));
            node.setStyle(
                    "-fx-background-color: rgba(221,236,239,0.55);" +
                            "-fx-background-radius: 16;" +
                            "-fx-border-color: rgba(92,152,168,0.18);" +
                            "-fx-border-radius: 16;" +
                            "-fx-border-width: 1.5;"
            );

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            Label num = new Label("Question " + numero);
            num.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: #5C98A8;");
            HBox.setHgrow(num, Priority.ALWAYS);
            Button del = new Button("✕");
            del.setStyle(
                    "-fx-background-color: rgba(239,68,68,0.08);" +
                            "-fx-text-fill: #EF4444; -fx-font-size: 12px;" +
                            "-fx-cursor: hand; -fx-background-radius: 8;" +
                            "-fx-border-color: rgba(239,68,68,0.20);" +
                            "-fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 4 8;"
            );
            del.setOnAction(e -> supprimerBloc());
            header.getChildren().addAll(num, del);

            fieldTexte = new TextField(question != null ? question.getTexteQuestion() : "");
            fieldTexte.setPromptText("Texte de la question...");
            fieldTexte.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.85);" +
                            "-fx-border-color: rgba(92,152,168,0.20); -fx-border-radius: 10;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 10; -fx-font-size: 13px; -fx-text-fill: #1F2A33;"
            );

            Label choixLabel = new Label("Choix de réponses");
            choixLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6E8E9A; -fx-font-weight: 700;");

            choixContainer = new VBox(8);

            Button addChoix = new Button("+ Ajouter un choix");
            addChoix.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #5C98A8;" +
                            "-fx-font-size: 12px; -fx-font-weight: 800; -fx-cursor: hand;" +
                            "-fx-border-color: rgba(92,152,168,0.30); -fx-border-radius: 8;" +
                            "-fx-border-width: 1; -fx-padding: 5 12; -fx-background-radius: 8;"
            );
            addChoix.setOnAction(e -> ajouterChoix("", choixEntries.size()));

            if (question != null && question.getReponses() != null && !question.getReponses().isEmpty()) {
                for (Reponse r : question.getReponses()) ajouterChoix(r.getTexteReponse(), r.getValeur());
            } else {
                ajouterChoix("", 0); ajouterChoix("", 1);
            }

            node.getChildren().addAll(header, fieldTexte, choixLabel, choixContainer, addChoix);
        }

        private void ajouterChoix(String texte, int valeur) {
            HBox ligne = new HBox(8);
            ligne.setAlignment(Pos.CENTER_LEFT);

            Label dot = new Label("○");
            dot.setStyle("-fx-text-fill: #8AA8B2; -fx-font-size: 14px;");

            TextField ft = new TextField(texte);
            ft.setPromptText("Texte du choix");
            ft.setPrefWidth(300);
            ft.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.85);" +
                            "-fx-border-color: rgba(92,152,168,0.18); -fx-border-radius: 8;" +
                            "-fx-background-radius: 8; -fx-padding: 7; -fx-font-size: 12px;" +
                            "-fx-text-fill: #1F2A33;"
            );

            Label vl = new Label("Valeur:");
            vl.setStyle("-fx-font-size: 11px; -fx-text-fill: #8AA8B2; -fx-font-weight: 700;");

            Spinner<Integer> sv = new Spinner<>(0, 10, valeur);
            sv.setPrefWidth(70);

            Button rm = new Button("✕");
            rm.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #EF4444;" +
                            "-fx-cursor: hand; -fx-font-size: 12px;"
            );

            ChoixEntry entry = new ChoixEntry(ft, sv);
            choixEntries.add(entry);
            rm.setOnAction(e -> { choixContainer.getChildren().remove(ligne); choixEntries.remove(entry); });

            ligne.getChildren().addAll(dot, ft, vl, sv, rm);
            choixContainer.getChildren().add(ligne);
        }

        private void supprimerBloc() {
            listeQuestionsForm.getChildren().remove(node);
            questionBlocs.remove(this);
            if (questionBlocs.isEmpty()) labelAucuneQuestion.setVisible(true);
        }

        VBox             getNode()          { return node; }
        String           getTexteQuestion() { return fieldTexte.getText(); }
        List<ChoixEntry> getChoix() {
            List<ChoixEntry> v = new ArrayList<>();
            for (ChoixEntry e : choixEntries) if (!e.texte.isEmpty()) v.add(e);
            return v;
        }

        class ChoixEntry {
            String texte; int valeur;
            ChoixEntry(TextField ft, Spinner<Integer> sv) {
                this.texte  = ft.getText().trim();
                this.valeur = sv.getValue();
                ft.textProperty().addListener((o, x, n) -> this.texte  = n.trim());
                sv.valueProperty().addListener((o, x, n) -> this.valeur = n);
            }
        }
    }
}