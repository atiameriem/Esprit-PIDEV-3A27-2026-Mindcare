package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ForumController {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private VBox postsContainer;
    @FXML private Button newPostButton;
    
    @FXML
    public void initialize() {
        // Initialisation de la vue Forum
        System.out.println("Vue Forum chargée");
        
        // Initialiser les combo boxes
        if (sortCombo != null) {
            sortCombo.getSelectionModel().selectFirst();
        }
        
        if (categoryCombo != null) {
            categoryCombo.getSelectionModel().selectFirst();
        }
        
        // Vous pouvez ajouter ici la logique pour charger les posts depuis la base de données
        // Exemple: loadForumPosts();
    }
    
    @FXML
    private void handleNewPost() {
        System.out.println("Création d'une nouvelle publication");
        // Logique pour créer un nouveau post
    }
}
