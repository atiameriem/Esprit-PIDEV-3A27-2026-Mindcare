package utils.forum;

import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

/**
 * Composants UI réutilisables (icônes, menu 3 points, style des dialogs).
 * Le controller ne doit pas porter ces détails.
 */
public final class UiFactory {

    private UiFactory() {}

    public static void styleDialog(DialogPane pane) {
        try {
            pane.getStylesheets().add(UiFactory.class.getResource("/css/forum.css").toExternalForm());
            pane.getStyleClass().add("forumDialog");
        } catch (Exception ignored) {
        }
    }

    public static Button dotsButton(ContextMenu menu) {
        Button b = new Button();
        b.getStyleClass().add("dotsBtn");
        b.setGraphic(kebabIcon());
        b.setOnAction(e -> {
            if (menu == null) return;
            if (menu.isShowing()) menu.hide();
            else menu.show(b, Side.BOTTOM, 0, 0);
        });
        return b;
    }

    public static ContextMenu buildIconMenu(
            boolean showEdit, boolean enableEdit, Runnable onEdit,
            boolean showDelete, boolean enableDelete, Runnable onDelete,
            boolean showReport, Runnable onReport
    ) {
        ContextMenu menu = new ContextMenu();

        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("iconMenuBox");

        if (showEdit) {
            Button edit = iconBtn("edit", () -> { menu.hide(); if (onEdit != null) onEdit.run(); });
            edit.setGraphic(pencilIcon());
            edit.setDisable(!enableEdit);
            box.getChildren().add(edit);
        }

        if (showDelete) {
            Button del = iconBtn("delete", () -> { menu.hide(); if (onDelete != null) onDelete.run(); });
            del.setGraphic(trashIcon());
            del.setDisable(!enableDelete);
            box.getChildren().add(del);
        }

        if (showReport) {
            Button rep = iconBtn("report", () -> { menu.hide(); if (onReport != null) onReport.run(); });
            rep.setGraphic(flagIcon());
            box.getChildren().add(rep);
        }

        CustomMenuItem item = new CustomMenuItem(box, false);
        item.setHideOnClick(false);
        menu.getItems().add(item);

        if (box.getChildren().isEmpty()) {
            menu.getItems().clear();
        }

        return menu;
    }

    private static Button iconBtn(String variant, Runnable action) {
        Button b = new Button();
        b.getStyleClass().addAll("iconBtn", variant);
        b.setOnAction(e -> action.run());
        return b;
    }

    private static SVGPath pencilIcon() {
        SVGPath s = new SVGPath();
        s.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z M20.71 7.04a1.003 1.003 0 0 0 0-1.42l-2.34-2.34a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 1.84-1.82z");
        s.getStyleClass().add("iconSvg");
        return s;
    }

    private static SVGPath trashIcon() {
        SVGPath s = new SVGPath();
        s.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12z M19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        s.getStyleClass().add("iconSvg");
        return s;
    }

    private static SVGPath flagIcon() {
        SVGPath s = new SVGPath();
        s.setContent("M6 2v20h2v-7h10l-2-5 2-5H8V2H6z");
        s.getStyleClass().add("iconSvg");
        return s;
    }

    private static SVGPath kebabIcon() {
        SVGPath s = new SVGPath();
        s.setContent("M12 2a2 2 0 1 0 0 4a2 2 0 0 0 0-4z M12 10a2 2 0 1 0 0 4a2 2 0 0 0 0-4z M12 18a2 2 0 1 0 0 4a2 2 0 0 0 0-4z");
        s.getStyleClass().add("iconSvg");
        return s;
    }
}
