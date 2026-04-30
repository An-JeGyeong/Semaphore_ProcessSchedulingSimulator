package controller;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import model.CoreConfig;
import model.CoreType;

final class CoreSelectionController {

	private final ToggleGroup[] groups;

	CoreSelectionController(ToggleGroup... groups) {
		this.groups = groups;
	}

	void addSelectionListener(Runnable listener) {
		for (ToggleGroup group : groups) {
			group.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> listener.run());
		}
	}

	List<String> getSelectedCoreLabels() {
		List<String> coreLabels = new ArrayList<>();

		for (int i = 0; i < groups.length; i++) {
			if (isCoreEnabled(groups[i])) {
				coreLabels.add(getSelectedCoreType(groups[i]) + " " + (i + 1));
			}
		}

		return coreLabels;
	}

	List<String> getSelectedCoreTypes() {
		List<String> coreTypes = new ArrayList<>();

		for (ToggleGroup group : groups) {
			coreTypes.add(getSelectedCoreType(group));
		}

		return coreTypes;
	}

	void selectCoreTypes(List<String> coreTypes) {
		for (int i = 0; i < groups.length; i++) {
			String type = i < coreTypes.size() ? coreTypes.get(i) : "off";
			selectToggleByText(groups[i], type);
		}
	}

	List<CoreConfig> getSelectedCoreConfigs() {
		List<CoreConfig> coreConfigs = new ArrayList<>();

		for (int i = 0; i < groups.length; i++) {
			CoreType coreType = getSelectedModelCoreType(groups[i]);
			if (coreType.isEnabled()) {
				coreConfigs.add(new CoreConfig(i + 1, coreType));
			}
		}

		return coreConfigs;
	}

	int countSelectedCoreType(String coreType) {
		int count = 0;

		for (ToggleGroup group : groups) {
			if (coreType.equals(getSelectedCoreType(group))) {
				count++;
			}
		}

		return count;
	}

	private boolean isCoreEnabled(ToggleGroup group) {
		if (group == null || !(group.getSelectedToggle() instanceof RadioButton selectedButton)) {
			return false;
		}

		return !"off".equalsIgnoreCase(selectedButton.getText());
	}

	private String getSelectedCoreType(ToggleGroup group) {
		if (group == null || !(group.getSelectedToggle() instanceof RadioButton selectedButton)) {
			return "Core";
		}

		return selectedButton.getText();
	}

	private CoreType getSelectedModelCoreType(ToggleGroup group) {
		if (group == null || !(group.getSelectedToggle() instanceof RadioButton selectedButton)) {
			return CoreType.OFF;
		}

		return switch (selectedButton.getText()) {
			case "P-Core" -> CoreType.P_CORE;
			case "E-Core" -> CoreType.E_CORE;
			default -> CoreType.OFF;
		};
	}

	private void selectToggleByText(ToggleGroup group, String text) {
		if (group == null) {
			return;
		}

		for (var toggle : group.getToggles()) {
			if (toggle instanceof RadioButton radioButton && radioButton.getText().equals(text)) {
				group.selectToggle(toggle);
				return;
			}
		}

		for (var toggle : group.getToggles()) {
			if (toggle instanceof RadioButton radioButton && "off".equalsIgnoreCase(radioButton.getText())) {
				group.selectToggle(toggle);
				return;
			}
		}
	}
}
