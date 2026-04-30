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

final class SimulationTabController {

	interface Listener {
		void beforeTabChange();

		void afterTabSelected(SimulationTabState tab);

		void onLastTabCloseRequested();
	}

	private final HBox tabBar;
	private final Button addButton;
	private final Listener listener;
	private final List<SimulationTabState> tabs = new ArrayList<>();
	private int nextTabId = 1;
	private SimulationTabState activeTab;

	SimulationTabController(HBox tabBar, Button addButton, Listener listener) {
		this.tabBar = tabBar;
		this.addButton = addButton;
		this.listener = listener;
	}

	void initialize() {
		tabBar.getChildren().clear();
		addButton.setOnAction(event -> addTab());
		addTab();
	}

	SimulationTabState getActiveTab() {
		return activeTab;
	}

	void addTab() {
		listener.beforeTabChange();

		int tabId = nextTabId++;
		SimulationTabState tab = new SimulationTabState(tabId, "Simulation " + tabId);
		tabs.add(tab);
		selectTab(tab);
	}

	void selectTab(SimulationTabState tab) {
		if (tab == null || tab == activeTab) {
			return;
		}

		listener.beforeTabChange();
		activeTab = tab;
		listener.afterTabSelected(tab);
		render();
	}

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

	private void render() {
		tabBar.getChildren().clear();

		for (SimulationTabState tab : tabs) {
			tabBar.getChildren().add(createTabNode(tab));
		}

		tabBar.getChildren().add(addButton);
	}

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

	private ImageView createCloseIcon() {
		ImageView imageView = new ImageView(new Image(getClass().getResource("/view/image/close.png").toExternalForm()));
		imageView.setFitWidth(12);
		imageView.setFitHeight(12);
		imageView.setPreserveRatio(true);
		imageView.setMouseTransparent(true);
		return imageView;
	}
}
