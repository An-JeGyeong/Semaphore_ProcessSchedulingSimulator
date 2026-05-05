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

	// 커스텀 타이틀바와 창 제어 버튼을 받아 undecorated 창 동작을 직접 구현한다.
	WindowController(Node dragArea, Button minBtn, Button maxBtn, Button closeBtn) {
		this.dragArea = dragArea;
		this.minBtn = minBtn;
		this.maxBtn = maxBtn;
		this.closeBtn = closeBtn;
	}

	// 커스텀 창 버튼, 드래그 이동, 더블클릭 최대화 이벤트를 연결한다.
	void initialize() {
		dragArea.setOnMousePressed(this::handleMousePressed);
		dragArea.setOnMouseDragged(this::handleMouseDragged);
		dragArea.setOnMouseClicked(this::handleMouseClicked);
		dragArea.setOnMouseReleased(this::handleMouseReleased);

		minBtn.setOnAction(event -> getStage(minBtn).setIconified(true));
		maxBtn.setOnAction(event -> toggleMaximize(getStage(maxBtn)));
		closeBtn.setOnAction(event -> getStage(closeBtn).close());
	}

	// 창을 끌기 시작할 때 마우스와 창의 기준 좌표를 저장한다.
	private void handleMousePressed(MouseEvent event) {
		if (isWindowButtonEvent(event)) {
			return;
		}

		xOffset = event.getSceneX();
		yOffset = event.getSceneY();
		draggingFromMaximized = customMaximized;
	}

	// 마우스 이동량에 맞춰 undecorated 창을 이동시킨다.
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

	// 타이틀바 더블 클릭 시 일반 창처럼 최대화와 복원을 전환한다.
	private void handleMouseClicked(MouseEvent event) {
		if (isWindowButtonEvent(event)) {
			return;
		}
		if (event.getClickCount() == 2) {
			toggleMaximize(getStage(dragArea));
		}
	}

	// 창을 화면 맨 위로 끌어 놓으면 Windows 창처럼 최대화한다.
	private void handleMouseReleased(MouseEvent event) {
		if (isWindowButtonEvent(event)) {
			return;
		}
		if (event.getScreenY() <= 5) {
			maximizeToVisualBounds(getStage(dragArea));
		}
	}

	// 최소화/최대화/닫기 버튼 위에서 발생한 마우스 이벤트는 드래그 처리에서 제외한다.
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

	// 현재 창 상태에 따라 최대화와 복원을 전환한다.
	private void toggleMaximize(Stage stage) {
		if (customMaximized) {
			restoreWindow(stage);
		} else {
			maximizeToVisualBounds(stage);
		}
	}

	// 작업 표시줄 영역을 제외한 현재 모니터의 visual bounds에 맞춰 창을 키운다.
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

	// 최대화 전에 저장해 둔 위치와 크기로 창을 되돌린다.
	private void restoreWindow(Stage stage) {
		stage.setMaximized(false);
		stage.setX(restoreX);
		stage.setY(restoreY);
		stage.setWidth(restoreWidth);
		stage.setHeight(restoreHeight);
		customMaximized = false;
	}

	// 최대화 복원을 위해 현재 창의 위치와 크기를 저장한다.
	private void saveRestoreBounds(Stage stage) {
		if (customMaximized) {
			return;
		}

		restoreX = stage.getX();
		restoreY = stage.getY();
		restoreWidth = stage.getWidth();
		restoreHeight = stage.getHeight();
	}

	// 이벤트가 발생한 노드에서 현재 Stage를 찾아 창 제어에 사용한다.
	private Stage getStage(Node node) {
		return (Stage) node.getScene().getWindow();
	}
}
