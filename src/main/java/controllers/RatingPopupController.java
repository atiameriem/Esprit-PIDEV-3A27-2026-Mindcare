package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import services.ParticipationService;
import utils.UserSession;

import java.sql.SQLException;

public class RatingPopupController {

    @FXML
    private Button star1, star2, star3, star4, star5;
    @FXML
    private javafx.scene.shape.SVGPath path1, path2, path3, path4, path5;

    private int selectedRating = 0;
    private int formationId;
    private final ParticipationService participationService = new ParticipationService();

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    @FXML
    private void handleStar1() {
        updateStars(1);
    }

    @FXML
    private void handleStar2() {
        updateStars(2);
    }

    @FXML
    private void handleStar3() {
        updateStars(3);
    }

    @FXML
    private void handleStar4() {
        updateStars(4);
    }

    @FXML
    private void handleStar5() {
        updateStars(5);
    }

    private void updateStars(int rating) {
        selectedRating = rating;
        javafx.scene.shape.SVGPath[] paths = { path1, path2, path3, path4, path5 };
        for (int i = 0; i < paths.length; i++) {
            if (i < rating) {
                paths[i].setFill(javafx.scene.paint.Color.web("#FFC107"));
            } else {
                paths[i].setFill(javafx.scene.paint.Color.web("#E0E0E0"));
            }
        }
    }

    @FXML
    private void handleCancel() {
        closePopup();
    }

    @FXML
    private void handleSave() {
        if (selectedRating > 0) {
            try {
                int userId = UserSession.getInstance().getUser().getId_users();
                participationService.updateRating(userId, formationId, selectedRating);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        closePopup();
    }

    private void closePopup() {
        Stage stage = (Stage) star1.getScene().getWindow();
        stage.close();
    }
}
