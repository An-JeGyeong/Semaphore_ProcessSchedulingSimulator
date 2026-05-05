package controller;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import model.CoreConfig;
import model.CoreType;

final class CoreSelectionController {

	private final ToggleGroup[] groups;

	// FXML에서 주입된 Core 라디오 그룹들을 한곳에서 다루기 위해 보관한다.
	CoreSelectionController(ToggleGroup... groups) {
		this.groups = groups;
	}

	// Core 선택이 바뀔 때 외부 화면을 갱신할 콜백을 등록한다.
	void addSelectionListener(Runnable listener) {
		for (ToggleGroup group : groups) {
			group.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> listener.run());
		}
	}

	// Gantt 차트에 표시할 선택된 Core 라벨 목록을 만든다.
	List<String> getSelectedCoreLabels() {
		List<String> coreLabels = new ArrayList<>();

		for (int i = 0; i < groups.length; i++) {
			CoreType coreType = getSelectedModelCoreType(groups[i]);
			if (coreType.isEnabled()) {
				coreLabels.add(new CoreConfig(i + 1, coreType).getLabel());
			}
		}

		return coreLabels;
	}

	// 탭 상태 저장을 위해 각 Core 라디오 그룹의 선택 텍스트를 수집한다.

	List<String> getSelectedCoreTypes() {
		List<String> coreTypes = new ArrayList<>();

		for (ToggleGroup group : groups) {
			coreTypes.add(getSelectedCoreType(group));
		}

		return coreTypes;
	}

	// 탭 복원 시 저장된 Core 선택 상태를 라디오 버튼에 다시 적용한다.
	void selectCoreTypes(List<String> coreTypes) {
		for (int i = 0; i < groups.length; i++) {
			String type = i < coreTypes.size() ? coreTypes.get(i) : CoreType.OFF.getDisplayName();
			selectToggleByText(groups[i], type);
		}
	}

	// 스케줄링 서비스에 넘길 활성 Core 설정 목록을 만든다.
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

	// Overview 표시를 위해 P-Core 또는 E-Core 선택 개수를 센다.
	int countSelectedCoreType(CoreType coreType) {
		int count = 0;

		for (ToggleGroup group : groups) {
			if (coreType == getSelectedModelCoreType(group)) {
				count++;
			}
		}

		return count;
	}

	// 탭 상태 저장에 사용할 수 있도록 선택된 라디오 버튼의 표시 문자열을 읽는다.
	private String getSelectedCoreType(ToggleGroup group) {
		if (group == null || !(group.getSelectedToggle() instanceof RadioButton selectedButton)) {
			return CoreType.OFF.getDisplayName();
		}

		return selectedButton.getText();
	}

	// 화면 표시 문자열을 스케줄링 로직에서 사용하는 CoreType 모델 값으로 변환한다.
	private CoreType getSelectedModelCoreType(ToggleGroup group) {
		if (group == null || !(group.getSelectedToggle() instanceof RadioButton selectedButton)) {
			return CoreType.OFF;
		}

		return CoreType.fromDisplayName(selectedButton.getText());
	}

	// 저장된 탭 상태를 라디오 버튼 선택 상태로 복원하고, 값이 없으면 off로 되돌린다.
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
			if (toggle instanceof RadioButton radioButton
					&& CoreType.OFF.getDisplayName().equalsIgnoreCase(radioButton.getText())) {
				group.selectToggle(toggle);
				return;
			}
		}
	}
}
