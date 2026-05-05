package controller;

import java.util.List;
import java.util.Random;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import model.Process;
import model.ProcessConstraints;

final class ProcessFormController {

	private final List<Process> processList;
	private final TextField addAtInput;
	private final TextField addBtInput;
	private final TextField updateAtInput;
	private final TextField updateBtInput;
	private final ComboBox<String> updateProcessCombo;
	private final ComboBox<String> deleteProcessCombo;
	private final DialogController dialogController;
	private final Runnable refreshCallback;
	private final Random random = new Random();

	private int processSequence = 1;

	// Options 영역의 프로세스 입력 필드와 콤보박스, 갱신 콜백을 묶어 관리한다.
	ProcessFormController(
			List<Process> processList,
			TextField addAtInput,
			TextField addBtInput,
			TextField updateAtInput,
			TextField updateBtInput,
			ComboBox<String> updateProcessCombo,
			ComboBox<String> deleteProcessCombo,
			DialogController dialogController,
			Runnable refreshCallback) {
		this.processList = processList;
		this.addAtInput = addAtInput;
		this.addBtInput = addBtInput;
		this.updateAtInput = updateAtInput;
		this.updateBtInput = updateBtInput;
		this.updateProcessCombo = updateProcessCombo;
		this.deleteProcessCombo = deleteProcessCombo;
		this.dialogController = dialogController;
		this.refreshCallback = refreshCallback;
	}

	// 입력된 Arrival Time과 Burst Time으로 단일 프로세스를 추가한다.
	void addProcess() {
		if (!canAddProcess()) {
			showMaxProcessWarning();
			return;
		}

		Integer arrivalTime = parseNonNegativeInt(addAtInput, "Arrival Time");
		Integer burstTime = parsePositiveInt(addBtInput, "Burst Time");
		if (arrivalTime == null || burstTime == null) {
			return;
		}

		if (addProcess(arrivalTime, burstTime)) {
			addAtInput.clear();
			addBtInput.clear();
			refreshCallback.run();
		}
	}

	// 최대 15개 제한을 넘지 않는 선에서 랜덤 프로세스를 추가한다.
	void addRandomProcesses() {
		if (!canAddProcess()) {
			showMaxProcessWarning();
			return;
		}

		int countToAdd = Math.min(
				ProcessConstraints.RANDOM_PROCESS_COUNT,
				ProcessConstraints.MAX_PROCESS_COUNT - processList.size());
		for (int i = 0; i < countToAdd && canAddProcess(); i++) {
			int arrivalTime = random.nextInt(ProcessConstraints.RANDOM_MAX_ARRIVAL_TIME + 1);
			int burstTime = random.nextInt(ProcessConstraints.RANDOM_MAX_BURST_TIME) + 1;
			addProcess(arrivalTime, burstTime);
		}

		refreshCallback.run();
	}

	// 선택된 프로세스의 Arrival Time과 Burst Time을 수정한다.
	void updateProcess() {
		String selectedPid = updateProcessCombo.getValue();

		if (selectedPid == null) {
			dialogController.showWarning("경고", "수정할 프로세스를 선택하세요.");
			return;
		}

		Integer arrivalTime = parseNonNegativeInt(updateAtInput, "Arrival Time");
		Integer burstTime = parsePositiveInt(updateBtInput, "Burst Time");
		if (arrivalTime == null || burstTime == null) {
			return;
		}

		findProcess(selectedPid).updateInput(arrivalTime, burstTime);
		updateAtInput.clear();
		updateBtInput.clear();
		refreshCallback.run();
	}

	// 선택된 프로세스를 목록과 ComboBox 선택지에서 제거한다.
	void deleteProcess() {
		String selectedPid = deleteProcessCombo.getValue();

		if (selectedPid == null) {
			dialogController.showWarning("경고", "삭제할 프로세스를 선택하세요.");
			return;
		}

		processList.removeIf(process -> process.getPid().equals(selectedPid));
		updateProcessCombo.getItems().remove(selectedPid);
		deleteProcessCombo.getItems().remove(selectedPid);
		updateProcessCombo.setValue(null);
		deleteProcessCombo.setValue(null);
		refreshCallback.run();
	}

	// 양수 입력이 필요한 필드를 검증하고 숫자로 변환한다.
	Integer parsePositiveInt(TextField field, String fieldName) {
		Integer value = parseInteger(field, fieldName);
		if (value == null) {
			return null;
		}
		if (value <= 0) {
			dialogController.showWarning("경고", fieldName + "는 1 이상이어야 합니다.");
			return null;
		}
		return value;
	}

	// 탭을 저장할 때 다음에 생성될 프로세스 번호를 함께 보관하기 위해 반환한다.
	int getProcessSequence() {
		return processSequence;
	}

	// 탭을 복원할 때 프로세스 번호가 이전 상태에서 이어지도록 되돌린다.
	void setProcessSequence(int processSequence) {
		this.processSequence = Math.max(1, processSequence);
	}

	// 탭 전환 등으로 프로세스 목록이 바뀌었을 때 수정/삭제 ComboBox를 다시 채운다.
	void reloadProcessCombos() {
		updateProcessCombo.getItems().clear();
		deleteProcessCombo.getItems().clear();

		for (Process process : processList) {
			updateProcessCombo.getItems().add(process.getPid());
			deleteProcessCombo.getItems().add(process.getPid());
		}

		updateProcessCombo.setValue(null);
		deleteProcessCombo.setValue(null);
	}

	// 실제 Process 객체를 생성하고 내부 목록과 선택지에 등록한다.
	private boolean addProcess(int arrivalTime, int burstTime) {
		if (!canAddProcess()) {
			return false;
		}

		String pid = "P" + processSequence++;
		Process process = new Process(pid, arrivalTime, burstTime);
		processList.add(process);
		updateProcessCombo.getItems().add(pid);
		deleteProcessCombo.getItems().add(pid);
		return true;
	}

	// 프로세스가 최대 개수 제한에 도달했는지 확인한다.
	private boolean canAddProcess() {
		return processList.size() < ProcessConstraints.MAX_PROCESS_COUNT;
	}

	// 프로세스가 15개를 넘을 때 사용자에게 안내한다.
	private void showMaxProcessWarning() {
		dialogController.showWarning("경고", "프로세스는 최대 15개까지 추가할 수 있습니다.");
	}
	
	// PID로 프로세스를 찾아 수정 작업에 사용한다.
	private Process findProcess(String pid) {
		return processList.stream()
				.filter(process -> process.getPid().equals(pid))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("프로세스를 찾을 수 없습니다."));
	}

	// 0 이상 입력이 필요한 필드를 검증하고 숫자로 변환한다.
	private Integer parseNonNegativeInt(TextField field, String fieldName) {
		Integer value = parseInteger(field, fieldName);
		if (value == null) {
			return null;
		}
		if (value < 0) {
			dialogController.showWarning("경고", fieldName + "는 0 이상이어야 합니다.");
			return null;
		}
		return value;
	}

	// 공통 숫자 입력 검증을 수행하고 실패 시 경고창을 띄운다.
	private Integer parseInteger(TextField field, String fieldName) {
		String text = field.getText();
		if (text == null || text.isBlank()) {
			dialogController.showWarning("경고", fieldName + "를 입력하세요.");
			return null;
		}

		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			dialogController.showWarning("경고", fieldName + "는 숫자만 입력할 수 있습니다.");
			return null;
		}
	}
}
