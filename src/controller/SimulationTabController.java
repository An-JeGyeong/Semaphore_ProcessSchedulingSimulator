package controller;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import model.SimulationTabState;

final class SimulationTabController {

	interface Listener {
		// 탭이 바뀌기 전에 현재 화면 상태를 저장할 기회를 준다.
		void beforeTabChange();

		// 새 탭이 선택되면 해당 탭 상태를 화면에 복원하도록 알린다.
		void afterTabSelected(SimulationTabState tab);

		// 마지막 탭 닫기를 막기 위해 상위 컨트롤러에 경고 표시를 요청한다.
		void onLastTabCloseRequested();
	}

	private final HBox tabBar;
	private final Button addButton;
	private final Listener listener;
	private final List<SimulationTabState> tabs = new ArrayList<>();
	private int nextTabId = 1;
	private SimulationTabState activeTab;

	// 탭 바 UI, 추가 버튼, 상태 저장/복원 콜백을 연결한다.
	SimulationTabController(HBox tabBar, Button addButton, Listener listener) {
		this.tabBar = tabBar;
		this.addButton = addButton;
		this.listener = listener;
	}

	// 첫 Simulation 탭을 만들고 탭 추가 버튼 동작을 연결한다.
	void initialize() {
		tabBar.getChildren().clear();
		addButton.setOnAction(event -> addTab());
		addTab();
	}

	// MainController가 현재 탭 상태를 확인할 수 있도록 활성 탭을 반환한다.
	SimulationTabState getActiveTab() {
		return activeTab;
	}

	// 새 SimulationTabState를 만들고 즉시 활성 탭으로 선택한다.
	void addTab() {
		listener.beforeTabChange();

		int tabId = nextTabId++;
		SimulationTabState tab = new SimulationTabState(tabId, "Simulation " + tabId);
		tabs.add(tab);
		selectTab(tab);
	}

	// 지정한 탭을 활성화하고 화면 복원 콜백과 탭 바 렌더링을 수행한다.
	void selectTab(SimulationTabState tab) {
		if (tab == null || tab == activeTab) {
			return;
		}

		listener.beforeTabChange();
		activeTab = tab;
		listener.afterTabSelected(tab);
		render();
	}

	// 탭 닫기 요청을 처리하고 마지막 탭은 닫히지 않게 막는다.
	private void closeTab(SimulationTabState tab) {
		if (tabs.size() <= 1) {
			listener.onLastTabCloseRequested();
			return;
		}

		boolean wasActive = tab == activeTab;
		int removedIndex = tabs.indexOf(tab);
		tabs.remove(tab);

		if (wasActive) {
			activeTab = null;
			selectTab(tabs.get(Math.max(0, removedIndex - 1)));
		} else {
			render();
		}
	}

	// 현재 탭 목록을 화면의 탭 바에 다시 그린다.
	private void render() {
		tabBar.getChildren().clear();

		for (SimulationTabState tab : tabs) {
			tabBar.getChildren().add(createTabNode(tab));
		}

		tabBar.getChildren().add(addButton);
	}

	// 단일 Simulation 탭의 제목과 닫기 아이콘 UI를 만든다.
	private StackPane createTabNode(SimulationTabState tab) {
		StackPane tabNode = new StackPane();
		tabNode.getStyleClass().add("custom-tab");
		if (tab == activeTab) {
			tabNode.getStyleClass().add("selected-tab");
		}
		tabNode.setPrefWidth(168);
		tabNode.setPrefHeight(24);
		tabNode.setOnMouseClicked(event -> selectTab(tab));

		Label title = new Label(tab.getName());
		title.getStyleClass().add("custom-tab-title");

		Button closeButton = new Button();
		closeButton.getStyleClass().add("tab-close-btn");
		closeButton.setMnemonicParsing(false);
		closeButton.setPrefSize(20, 20);
		closeButton.setGraphic(createCloseIcon());
		closeButton.setOnAction(event -> {
			event.consume();
			closeTab(tab);
		});
		StackPane.setAlignment(closeButton, javafx.geometry.Pos.CENTER_RIGHT);
		StackPane.setMargin(closeButton, new Insets(0, 7, 0, 0));

		tabNode.getChildren().addAll(title, closeButton);
		return tabNode;
	}

	// 탭 닫기 버튼에 사용할 픽셀 아이콘을 ImageView로 만든다.
	private ImageView createCloseIcon() {
		ImageView imageView = new ImageView(new Image(getClass().getResource("/view/image/close.png").toExternalForm()));
		imageView.setFitWidth(12);
		imageView.setFitHeight(12);
		imageView.setPreserveRatio(true);
		imageView.setMouseTransparent(true);
		return imageView;
	}
}
