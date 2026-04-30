package controller;

import java.util.List;
import java.util.Random;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import model.Process;

final class ProcessFormController {

	private static final int MAX_PROCESS_COUNT = 15;
	private static final int RANDOM_PROCESS_COUNT = 5;
	private static final int RANDOM_MAX_ARRIVAL_TIME = 30;
	private static final int RANDOM_MAX_BURST_TIME = 20;

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

	void addProcess() {
		if (processList.size() >= MAX_PROCESS_COUNT) {
			dialogController.showWarning("경고", "프로세스는 최대 15개까지 추가할 수 있습니다.");
			return;
		}

		Integer arrivalTime = parseNonNegativeInt(addAtInput, "Arrival Time");
		Integer burstTime = parsePositiveInt(addBtInput, "Burst Time");
		if (arrivalTime == null || burstTime == null) {
			return;
		}

		addProcess(arrivalTime, burstTime);
		addAtInput.clear();
		addBtInput.clear();
		refreshCallback.run();
	}

	void addRandomProcesses() {
		if (processList.size() >= MAX_PROCESS_COUNT) {
			dialogController.showWarning("경고", "프로세스는 최대 15개까지 추가할 수 있습니다.");
			return;
		}

		int countToAdd = Math.min(RANDOM_PROCESS_COUNT, MAX_PROCESS_COUNT - processList.size());

		for (int i = 0; i < countToAdd; i++) {
			int arrivalTime = random.nextInt(RANDOM_MAX_ARRIVAL_TIME + 1);
			int burstTime = random.nextInt(RANDOM_MAX_BURST_TIME) + 1;
			addProcess(arrivalTime, burstTime);
		}

		refreshCallback.run();
	}

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

	int getProcessSequence() {
		return processSequence;
	}

	void setProcessSequence(int processSequence) {
		this.processSequence = Math.max(1, processSequence);
	}

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

	private void addProcess(int arrivalTime, int burstTime) {
		String pid = "P" + processSequence++;
		Process process = new Process(pid, arrivalTime, burstTime);
		processList.add(process);
		updateProcessCombo.getItems().add(pid);
		deleteProcessCombo.getItems().add(pid);
	}

	private Process findProcess(String pid) {
		return processList.stream()
				.filter(process -> process.getPid().equals(pid))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("프로세스를 찾을 수 없습니다."));
	}

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
