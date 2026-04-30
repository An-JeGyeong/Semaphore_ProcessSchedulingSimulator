package controller;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.StageStyle;

final class DialogController {

	void showWarning(String title, String message) {
		Dialog<Void> dialog = new Dialog<>();
		dialog.setTitle(title);
		dialog.initStyle(StageStyle.TRANSPARENT);
		dialog.getDialogPane().setHeaderText(title);
		dialog.getDialogPane().setContentText(message);
		dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
		applyDialogStyles(dialog.getDialogPane());
		dialog.showAndWait();
	}

	private void applyDialogStyles(DialogPane dialogPane) {
		dialogPane.getStylesheets().add(getClass().getResource("/view/dialog.css").toExternalForm());
		dialogPane.getStyleClass().add("dialog-pane");
		dialogPane.setPrefWidth(360);
		dialogPane.setMinHeight(160);
	}
}
