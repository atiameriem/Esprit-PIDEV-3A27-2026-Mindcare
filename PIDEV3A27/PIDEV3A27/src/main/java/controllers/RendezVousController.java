package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RendezVousController {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private TableView rendezVousTable;
    @FXML private Label totalLabel;
    @FXML private Label upcomingLabel;
    @FXML private Label monthLabel;
    @FXML private Button addButton;
    
    @FXML
    public void initialize() {
        // Initialisation de la vue Rendez-vous
        System.out.println("Vue Rendez-vous chargée");
        
        // Initialiser le combo box si nécessaire
        if (filterCombo != null) {
            filterCombo.getSelectionModel().selectFirst();
        }
        
        // Vous pouvez ajouter ici la logique pour charger les rendez-vous depuis la base de données
        // Exemple: loadRendezVous();
    }
    
    @FXML
    private void handleAddRendezVous() {
        System.out.println("Ajout d'un nouveau rendez-vous");
        // Logique pour ajouter un rendez-vous
    }
}
