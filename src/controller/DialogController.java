package controller;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.StageStyle;

final class DialogController {

	// 경고 메시지를 프로젝트 스타일이 적용된 커스텀 Dialog로 표시한다.
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

	// Dialog 전용 CSS를 연결하고 기본 크기를 맞춰 기본 JavaFX Alert 느낌을 덜어낸다.
	private void applyDialogStyles(DialogPane dialogPane) {
		dialogPane.getStylesheets().add(getClass().getResource("/view/css/dialog.css").toExternalForm());
		dialogPane.getStyleClass().add("dialog-pane");
		dialogPane.setPrefWidth(360);
		dialogPane.setMinHeight(160);
	}
}
