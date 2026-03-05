package utils.forum;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Custom styled popups for the whole app (no default JavaFX Alert look).
 * Lightweight, self-contained (inline styles + optional CSS classes).
 */
public final class PopupUtil {

    public enum Kind { SUCCESS, ERROR, INFO, CONFIRM }

    private PopupUtil() {}

    public static void showSuccess(String message) {
        show(Kind.SUCCESS, "Succès", message);
    }

    public static void showError(String message) {
        show(Kind.ERROR, "Erreur", message);
    }

    public static void showInfo(String message) {
        show(Kind.INFO, "Info", message);
    }
    public static void showSuccess(String title, String message) {
        show(Kind.SUCCESS, title, message);
    }

    public static void showError(String title, String message) {
        show(Kind.ERROR, title, message);
    }

    public static void showInfo(String title, String message) {
        show(Kind.INFO, title, message);
    }
    public static void show(Kind kind, String title, String message) {
        Platform.runLater(() -> {
            Stage stage = new Stage(StageStyle.TRANSPARENT);

            Window owner = getActiveWindow();
            if (owner != null) stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);

            VBox card = new VBox(10);
            card.setPadding(new Insets(16));
            card.setAlignment(Pos.CENTER_LEFT);
            card.setMinWidth(360);
            card.setMaxWidth(480);

            // Shadow + rounding
            card.setEffect(new DropShadow(18, javafx.scene.paint.Color.rgb(0,0,0,0.25)));
            card.setStyle(baseCardStyle(kind));

            Label titleLbl = new Label(title == null ? "" : title);
            titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #111827;");

            Label msgLbl = new Label(message == null ? "" : message);
            msgLbl.setWrapText(true);
            msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");

            Region spacer = new Region();
            spacer.setMinHeight(6);

            Button okBtn = new Button(kind == Kind.CONFIRM ? "OK" : "Fermer");
            okBtn.setDefaultButton(true);
            okBtn.setStyle(buttonStyle(kind));
            okBtn.setOnAction(e -> stage.close());

            HBox actions = new HBox(okBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);

            card.getChildren().addAll(titleLbl, msgLbl, spacer, actions);

            Scene scene = new Scene(card);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

            // If you have a global stylesheet, keep it
            try {
                String css = PopupUtil.class.getResource("/styles/forum.css") != null
                        ? PopupUtil.class.getResource("/styles/forum.css").toExternalForm()
                        : null;
                if (css != null) scene.getStylesheets().add(css);
            } catch (Exception ignored) {}

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        });
    }

    private static Window getActiveWindow() {
        for (Window w : Window.getWindows()) {
            if (w != null && w.isShowing() && w.isFocused()) return w;
        }
        // fallback: first showing window
        for (Window w : Window.getWindows()) {
            if (w != null && w.isShowing()) return w;
        }
        return null;
    }

    private static String baseCardStyle(Kind kind) {
        String border;
        switch (kind) {
            case SUCCESS -> border = "#22c55e";
            case ERROR -> border = "#ef4444";
            case CONFIRM -> border = "#9ca3af";
            default -> border = "#3b82f6";
        }
        return ""
            + "-fx-background-color: rgba(255,255,255,0.98);"
            + "-fx-background-radius: 16;"
            + "-fx-border-color: " + border + ";"
            + "-fx-border-width: 2;"
            + "-fx-border-radius: 16;";
    }

    private static String buttonStyle(Kind kind) {
        String bg;
        switch (kind) {
            case SUCCESS -> bg = "#22c55e";
            case ERROR -> bg = "#ef4444";
            case CONFIRM -> bg = "#6b7280";
            default -> bg = "#3b82f6";
        }
        return ""
            + "-fx-background-color: " + bg + ";"
            + "-fx-text-fill: white;"
            + "-fx-font-weight: 800;"
            + "-fx-background-radius: 12;"
            + "-fx-padding: 8 14 8 14;"
            + "-fx-cursor: hand;";
    }
}
