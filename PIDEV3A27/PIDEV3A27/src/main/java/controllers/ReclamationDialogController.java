package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Reclamation;

public class ReclamationDialogController {

    @FXML
    private TextField objetField;
    @FXML
    private RadioButton highRadio;
    @FXML
    private RadioButton mediumRadio;
    @FXML
    private RadioButton lowRadio;
    @FXML
    private TextArea descriptionArea;

    private ToggleGroup urgenceGroup;
    private Reclamation reclamation;
    private boolean saveClicked = false;

    @FXML
    public void initialize() {
        urgenceGroup = new ToggleGroup();
        highRadio.setToggleGroup(urgenceGroup);
        mediumRadio.setToggleGroup(urgenceGroup);
        lowRadio.setToggleGroup(urgenceGroup);
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;

        if (reclamation != null) {
            objetField.setText(reclamation.getObjet());
            descriptionArea.setText(reclamation.getDescription());

            String urgence = reclamation.getUrgence();
            if ("HIGH".equalsIgnoreCase(urgence) || "HIGHT".equalsIgnoreCase(urgence))
                highRadio.setSelected(true);
            else if ("LOW".equalsIgnoreCase(urgence))
                lowRadio.setSelected(true);
            else
                mediumRadio.setSelected(true);
        }
    }

    @FXML
    private void handleSave() {
        if (reclamation != null) {
            reclamation.setObjet(objetField.getText());
            reclamation.setDescription(descriptionArea.getText());

            RadioButton selected = (RadioButton) urgenceGroup.getSelectedToggle();
            if (selected != null) {
                reclamation.setUrgence(selected.getText());
            }
        }

        saveClicked = true;
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) objetField.getScene().getWindow();
        stage.close();
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public Reclamation getReclamation() {
        return reclamation;
    }
}
