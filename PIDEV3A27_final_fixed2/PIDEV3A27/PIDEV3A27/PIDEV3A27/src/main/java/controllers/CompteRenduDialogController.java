package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import models.CompteRenduSeance;
import javafx.scene.control.Label;
public class CompteRenduDialogController {

    @FXML private ComboBox<CompteRenduSeance.ProgresCR> progresCombo;
    @FXML private TextArea resumeArea;
    @FXML private TextArea actionsArea;
    @FXML private TextArea aiSummaryArea;
    @FXML private Label aiStatusLabel;
    //Méthode appelée quand tu ouvres le popup en mode modification.
    public void initForEdit(CompteRenduSeance toEdit) {
        //si on reçoit null, on sort
        if (toEdit == null) return;
        //remplissage
        progresCombo.getItems().setAll(CompteRenduSeance.ProgresCR.values());
        progresCombo.getSelectionModel().select(toEdit.getProgresCr());

        resumeArea.setText(toEdit.getResumeSeanceCr() == null ? "" : toEdit.getResumeSeanceCr());
        actionsArea.setText(toEdit.getProchainesActionCr() == null ? "" : toEdit.getProchainesActionCr());
        aiSummaryArea.setText(toEdit.getAiResumeCr() == null ? "" : toEdit.getAiResumeCr());
    }
//Méthode appelée quand tu ouvres le popup en mode création
    public void initForCreate() {
        progresCombo.getItems().setAll(CompteRenduSeance.ProgresCR.values());
        progresCombo.getSelectionModel().selectFirst();
        resumeArea.setText("");
        actionsArea.setText("");
        aiSummaryArea.setText("");
    }

    public CompteRenduSeance.ProgresCR getProgres() { return progresCombo.getValue(); }
    public String getResume() { return resumeArea.getText(); }
    public String getActions() { return actionsArea.getText(); }
    public String getAiSummary() { return aiSummaryArea.getText(); }
}