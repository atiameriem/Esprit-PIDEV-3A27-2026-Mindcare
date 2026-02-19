package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import models.Reclamation;
import models.TypeReclamation;

public class ReclamationDialogController {

    @FXML private ComboBox<TypeReclamation> typeComboBox;
    @FXML private TextArea descriptionArea;

    private Reclamation reclamation;
    private boolean saveClicked = false;

    @FXML
    public void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList(TypeReclamation.values()));
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;

        if (reclamation != null) {
            typeComboBox.setValue(reclamation.getType());
            descriptionArea.setText(reclamation.getDescription());
        }
    }

    @FXML
    private void handleSave() {

        reclamation.setType(typeComboBox.getValue());
        reclamation.setDescription(descriptionArea.getText());

        saveClicked = true;
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) typeComboBox.getScene().getWindow();
        stage.close();
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public Reclamation getReclamation() {
        return reclamation;
    }
}
