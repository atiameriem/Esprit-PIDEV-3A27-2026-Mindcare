package controllers;

import services.ServiceEmail;
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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NouveauTestController {

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

    // ✅ idPsychologue depuis la session connectée
    private int idPsychologue = Session.getUserId();

    // ✅ idPatient variable — peut être changé via setIdPatientCible()
    private int idPatient = 3;

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/pi-dev";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private Quiz    quizEnEdition = null;
    private boolean modeEdition   = false;

    private final List<QuestionBloc> questionBlocs = new ArrayList<>();

    // ════════════════════════════════════════════════════════
    // ✅ Appelé depuis EspacePraticienController
    //    Pré-sélectionne le patient pour lequel on crée le test
    // ════════════════════════════════════════════════════════
    public void setIdPatientCible(int idPatient) {
        this.idPatient = idPatient;

        // Afficher le nom du patient dans le sous-titre
        String sql = "SELECT nom FROM users WHERE id_users = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPatient);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("nom");
                if (labelSousTitrePage != null)
                    labelSousTitrePage.setText("Création d'un test pour : " + nom);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement patient : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // ✅ Ajouter seulement dans initialize() — tout le reste IDENTIQUE
    @FXML
    public void initialize() {
        // ✅ Vérification rôle — réservé aux psychologues et admins
        var role = Session.getRoleConnecte();
        if (role != null
                && role != utils.Session.Role.PSYCHOLOGUE
                && role != utils.Session.Role.ADMIN) {
            if (labelTitrePage != null)
                labelTitrePage.setText("⛔ Accès réservé aux psychologues.");
            if (btnSauvegarder != null) btnSauvegarder.setDisable(true);
            return;
        }

        idPsychologue = Session.getUserId() > 0 ? Session.getUserId() : 6;
        comboTypeTest.setItems(FXCollections.observableArrayList(
                "psychologique", "cognitif", "comportemental",
                "émotionnel", "STRESS", "BIEN_ETRE", "HUMEUR"
        ));
        comboTypeTest.getSelectionModel().selectFirst();
    }

    // ════════════════════════════════════════════════════════
    // MODE ÉDITION
    // ════════════════════════════════════════════════════════
    public void setQuizPourEdition(Quiz quiz) {
        this.quizEnEdition = quiz;
        this.modeEdition   = true;

        labelTitrePage.setText("Modifier le Test");
        labelSousTitrePage.setText("Modifiez les informations, questions et choix");
        btnSauvegarder.setText("💾  Mettre à jour le test");

        fieldTitre.setText(quiz.getTitre());
        fieldDescription.setText(quiz.getDescription() != null ? quiz.getDescription() : "");
        if (quiz.getTypeTest() != null)
            comboTypeTest.getSelectionModel().select(quiz.getTypeTest());

        chargerQuestionsExistantes(quiz.getIdQuiz());
    }

    private void chargerQuestionsExistantes(int idQuiz) {
        try {
            List<Question> questions = serviceQuestion.getQuestionsByQuizAvecChoix(idQuiz);
            listeQuestionsForm.getChildren().clear();
            questionBlocs.clear();

            if (questions.isEmpty()) {
                labelAucuneQuestion.setVisible(true);
                return;
            }
            labelAucuneQuestion.setVisible(false);

            for (int i = 0; i < questions.size(); i++) {
                QuestionBloc bloc = new QuestionBloc(i + 1, questions.get(i));
                questionBlocs.add(bloc);
                listeQuestionsForm.getChildren().add(bloc.getNode());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement questions : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // Ajouter un bloc question vide
    // ════════════════════════════════════════════════════════
    @FXML
    private void ajouterQuestion() {
        labelAucuneQuestion.setVisible(false);
        QuestionBloc bloc = new QuestionBloc(questionBlocs.size() + 1, null);
        questionBlocs.add(bloc);
        listeQuestionsForm.getChildren().add(bloc.getNode());
    }

    // ════════════════════════════════════════════════════════
    // Sauvegarder
    // ════════════════════════════════════════════════════════
    @FXML
    private void sauvegarderTest() {
        if (fieldTitre.getText().trim().isEmpty()) {
            afficherAlerte("Erreur", "Le titre est obligatoire."); return;
        }
        if (questionBlocs.isEmpty()) {
            afficherAlerte("Erreur", "Ajoutez au moins une question."); return;
        }
        for (QuestionBloc b : questionBlocs) {
            if (b.getTexteQuestion().trim().isEmpty()) {
                afficherAlerte("Erreur", "Remplissez le texte de toutes les questions.");
                return;
            }
            if (b.getChoix().size() < 2) {
                afficherAlerte("Erreur", "Chaque question doit avoir au moins 2 choix.");
                return;
            }
        }

        try {
            if (modeEdition) mettreAJourQuiz();
            else             creerNouveauQuiz();
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Impossible de sauvegarder : " + e.getMessage());
        }
    }

    // ── Création ──────────────────────────────────────────────────
    private void creerNouveauQuiz() throws SQLException {
        Quiz quiz = new Quiz(
                idPatient, idPsychologue,
                fieldTitre.getText().trim(),
                fieldDescription.getText().trim(),
                comboTypeTest.getValue(), true
        );
        serviceQuiz.add(quiz);
        sauvegarderQuestions(quiz.getIdQuiz());

        // ✅ Thread email avec sa PROPRE connexion
        final int patientId = this.idPatient;
        Thread emailThread = new Thread(() -> {
            String sql = "SELECT nom, email FROM users WHERE id_users = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, patientId);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String nom   = rs.getString("nom");
                    String email = rs.getString("email");

                    if (email != null && !email.isEmpty()) {
                        ServiceEmail se = new ServiceEmail();
                        String html = se.templateNouveauQuiz(
                                nom,
                                quiz.getTitre(),
                                quiz.getDescription() != null ? quiz.getDescription() : "Test psychologique",
                                quiz.getTypeTest() != null ? quiz.getTypeTest() : "psychologique"
                        );
                        se.envoyerEmail(
                                email,
                                "🧠 MindCare — Nouveau test : " + quiz.getTitre(),
                                html
                        );
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur email : " + e.getMessage());
            }
        });
        emailThread.setDaemon(true);
        emailThread.start();

        afficherInfo("Succès", "Test \"" + quiz.getTitre() + "\" créé !\n📧 Email envoyé au patient.");
        retourListe();
    }

    // ── Mise à jour ───────────────────────────────────────────────
    private void mettreAJourQuiz() throws SQLException {
        quizEnEdition.setTitre      (fieldTitre.getText().trim());
        quizEnEdition.setDescription(fieldDescription.getText().trim());
        quizEnEdition.setTypeTest   (comboTypeTest.getValue());
        serviceQuiz.update(quizEnEdition);

        List<Question> anciennes = serviceQuestion.getQuestionsByQuiz(quizEnEdition.getIdQuiz());
        for (Question q : anciennes) {
            List<Reponse> choix = serviceReponse.getChoixParQuestion(q.getIdQuestion());
            for (Reponse r : choix) serviceReponse.delete(r);
            serviceQuestion.delete(q);
        }

        sauvegarderQuestions(quizEnEdition.getIdQuiz());
        afficherInfo("Succès", "Test \"" + quizEnEdition.getTitre() + "\" mis à jour !");
        retourListe();
    }

    // ── Sauvegarder questions + choix ─────────────────────────────
    private void sauvegarderQuestions(int idQuiz) throws SQLException {
        for (int i = 0; i < questionBlocs.size(); i++) {
            QuestionBloc bloc = questionBlocs.get(i);
            Question q = new Question(idQuiz, bloc.getTexteQuestion(), i + 1, "radio");
            List<Reponse> choix = new ArrayList<>();
            for (QuestionBloc.ChoixEntry e : bloc.getChoix()) {
                choix.add(new Reponse(idQuiz, 0, e.texte, e.valeur));
            }
            serviceQuestion.addAvecChoix(q, choix);
        }
    }

    // ════════════════════════════════════════════════════════
    // Navigation
    // ════════════════════════════════════════════════════════
    @FXML
    private void retourListe() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/PasserTests.fxml")
            );
            Node vue = loader.load();
            VBox parent = (VBox) fieldTitre.getScene().lookup("#contentArea");
            if (parent != null) parent.getChildren().setAll(vue);
        } catch (IOException e) {
            System.err.println("❌ Erreur retour : " + e.getMessage());
        }
    }

    private void afficherAlerte(String t, String m) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    private void afficherInfo(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
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
                    "-fx-background-color: #f8f9fa;" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: #e0e0e0; -fx-border-radius: 10;"
            );

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            Label num = new Label("Question " + numero);
            num.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#2c4a6e;");
            HBox.setHgrow(num, Priority.ALWAYS);
            Button del = new Button("✕");
            del.setStyle("-fx-background-color:transparent;-fx-text-fill:#e74c3c;" +
                    "-fx-font-size:13px;-fx-cursor:hand;");
            del.setOnAction(e -> supprimerBloc());
            header.getChildren().addAll(num, del);

            fieldTexte = new TextField(question != null ? question.getTexteQuestion() : "");
            fieldTexte.setPromptText("Texte de la question...");
            fieldTexte.setStyle(
                    "-fx-background-color:white;-fx-border-color:#e0e0e0;" +
                            "-fx-border-radius:8;-fx-background-radius:8;" +
                            "-fx-padding:10;-fx-font-size:13px;"
            );

            Label choixLabel = new Label("Choix de réponses");
            choixLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#7f8c8d;");

            choixContainer = new VBox(8);

            Button addChoix = new Button("+ Ajouter un choix");
            addChoix.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#2c4a6e;" +
                            "-fx-font-size:12px;-fx-font-weight:bold;-fx-cursor:hand;"
            );
            addChoix.setOnAction(e -> ajouterChoix("", choixEntries.size()));

            if (question != null && question.getReponses() != null
                    && !question.getReponses().isEmpty()) {
                for (Reponse r : question.getReponses())
                    ajouterChoix(r.getTexteReponse(), r.getValeur());
            } else {
                ajouterChoix("", 0);
                ajouterChoix("", 1);
            }

            node.getChildren().addAll(header, fieldTexte, choixLabel, choixContainer, addChoix);
        }

        private void ajouterChoix(String texte, int valeur) {
            HBox ligne = new HBox(8);
            ligne.setAlignment(Pos.CENTER_LEFT);

            Label dot = new Label("○");
            dot.setStyle("-fx-text-fill:#95a5a6;-fx-font-size:14px;");

            TextField ft = new TextField(texte);
            ft.setPromptText("Texte du choix");
            ft.setPrefWidth(300);
            ft.setStyle(
                    "-fx-background-color:white;-fx-border-color:#e0e0e0;" +
                            "-fx-border-radius:6;-fx-background-radius:6;" +
                            "-fx-padding:7;-fx-font-size:12px;"
            );

            Label vl = new Label("Valeur:");
            vl.setStyle("-fx-font-size:11px;-fx-text-fill:#95a5a6;");

            Spinner<Integer> sv = new Spinner<>(0, 10, valeur);
            sv.setPrefWidth(70);

            Button rm = new Button("✕");
            rm.setStyle("-fx-background-color:transparent;-fx-text-fill:#e74c3c;-fx-cursor:hand;");

            ChoixEntry entry = new ChoixEntry(ft, sv);
            choixEntries.add(entry);
            rm.setOnAction(e -> {
                choixContainer.getChildren().remove(ligne);
                choixEntries.remove(entry);
            });

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
            String texte;
            int    valeur;
            ChoixEntry(TextField ft, Spinner<Integer> sv) {
                this.texte  = ft.getText().trim();
                this.valeur = sv.getValue();
                ft.textProperty().addListener((o, x, n) -> this.texte  = n.trim());
                sv.valueProperty().addListener((o, x, n) -> this.valeur = n);
            }
        }
    }
}