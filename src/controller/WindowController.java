package controller;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

final class WindowController {

	private final Node dragArea;
	private final Button minBtn;
	private final Button maxBtn;
	private final Button closeBtn;

	private double xOffset;
	private double yOffset;
	private boolean draggingFromMaximized;
	private boolean customMaximized;
	private double restoreX;
	private double restoreY;
	private double restoreWidth;
	private double restoreHeight;

	WindowController(Node dragArea, Button minBtn, Button maxBtn, Button closeBtn) {
		this.dragArea = dragArea;
		this.minBtn = minBtn;
		this.maxBtn = maxBtn;
		this.closeBtn = closeBtn;
	}

	void initialize() {
		dragArea.setOnMousePressed(this::handleMousePressed);
		dragArea.setOnMouseDragged(this::handleMouseDragged);
		dragArea.setOnMouseClicked(this::handleMouseClicked);
		dragArea.setOnMouseReleased(this::handleMouseReleased);

		minBtn.setOnAction(event -> getStage(minBtn).setIconified(true));
		maxBtn.setOnAction(event -> toggleMaximize(getStage(maxBtn)));
		closeBtn.setOnAction(event -> getStage(closeBtn).close());
	}

	private void handleMousePressed(MouseEvent event) {
		if (isWindowButtonEvent(event)) {
			return;
		}

		xOffset = event.getSceneX();
		yOffset = event.getSceneY();
		draggingFromMaximized = customMaximized;
	}

	private void handleMouseDragged(MouseEvent event) {
		if (isWindowButtonEvent(event)) {
			return;
		}

		Stage stage = getStage(dragArea);

		if (draggingFromMaximized) {
			restoreWindow(stage);
			xOffset = stage.getWidth() / 2;
			yOffset = event.getSceneY();
			draggingFromMaximized = false;
		}

		stage.setX(event.getScreenX() - xOffset);
		stage.setY(event.getScreenY() - yOffset);
	}

	private void handleMouseClicked(MouseEvent event) {
		if (isWindowButtonEvent(event)) {
			return;
		}
		if (event.getClickCount() == 2) {
			toggleMaximize(getStage(dragArea));
		}
	}

	private void handleMouseReleased(MouseEvent event) {
		if (isWindowButtonEvent(event)) {
			return;
		}
		if (event.getScreenY() <= 5) {
			maximizeToVisualBounds(getStage(dragArea));
		}
	}

	private boolean isWindowButtonEvent(MouseEvent event) {
		Node node = event.getPickResult().getIntersectedNode();

		while (node != null) {
			if (node instanceof Button) {
				return true;
			}
			node = node.getParent();
		}

		return false;
	}

	private void toggleMaximize(Stage stage) {
		if (customMaximized) {
			restoreWindow(stage);
		} else {
			maximizeToVisualBounds(stage);
		}
	}

	private void maximizeToVisualBounds(Stage stage) {
		saveRestoreBounds(stage);

		var bounds = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
				.stream()
				.findFirst()
				.orElse(Screen.getPrimary())
				.getVisualBounds();

		stage.setMaximized(false);
		stage.setX(bounds.getMinX());
		stage.setY(bounds.getMinY());
		stage.setWidth(bounds.getWidth());
		stage.setHeight(bounds.getHeight());
		customMaximized = true;
	}

	private void restoreWindow(Stage stage) {
		stage.setMaximized(false);
		stage.setX(restoreX);
		stage.setY(restoreY);
		stage.setWidth(restoreWidth);
		stage.setHeight(restoreHeight);
		customMaximized = false;
	}

	private void saveRestoreBounds(Stage stage) {
		if (customMaximized) {
			return;
		}

		restoreX = stage.getX();
		restoreY = stage.getY();
		restoreWidth = stage.getWidth();
		restoreHeight = stage.getHeight();
	}

	private Stage getStage(Node node) {
		return (Stage) node.getScene().getWindow();
	}
}
