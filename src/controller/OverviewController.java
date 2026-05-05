package controller;

import java.util.List;
import java.util.EnumMap;
import java.util.function.Supplier;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.AlgorithmType;
import model.CoreType;
import model.OverviewMetrics;
import model.Process;
import model.SchedulingResult;
import service.OverviewMetricsCalculator;

final class OverviewController {

	private final ComboBox<AlgorithmType> algorithmCombo;
	private final CoreSelectionController coreSelectionController;
	private final TextField timeQuantumField;
	private final OverviewLabels labels;
	private final OverviewMetricsCalculator metricsCalculator;

	// Overview가 필요한 입력 컨트롤과 계산기를 연결해 화면 갱신 준비를 마친다.
	OverviewController(
			ComboBox<AlgorithmType> algorithmCombo,
			List<Process> processList,
			CoreSelectionController coreSelectionController,
			TextField timeQuantumField,
			OverviewLabels labels) {
		this.algorithmCombo = algorithmCombo;
		this.coreSelectionController = coreSelectionController;
		this.timeQuantumField = timeQuantumField;
		this.labels = labels;
		this.metricsCalculator = new OverviewMetricsCalculator(
				processList,
				coreSelectionController::getSelectedCoreLabels,
				coreSelectionController::countSelectedCoreType);
	}

	// 현재 알고리즘, Core 선택, 스케줄링 결과를 Overview 라벨에 반영한다.
	void update(SchedulingResult result, Supplier<Integer> totalTimeSupplier) {
		AlgorithmType algorithm = algorithmCombo.getValue();
		int totalTime = result == null ? 0 : totalTimeSupplier.get();
		OverviewMetrics metrics = metricsCalculator.calculate(result, totalTime);

		labels.updateAlgorithm(algorithm == null ? "-" : algorithm.name(), formatTimeQuantum(algorithm));
		labels.updateCoreInfo(
				formatCoreLabel(CoreType.P_CORE),
				metrics.pCoreSummary(),
				formatCoreLabel(CoreType.E_CORE),
				metrics.eCoreSummary());
		labels.updateProcessInfo(metrics);
		labels.updatePerformance(metrics);
		labels.updateSystem(metrics);
	}

	// RR일 때만 Time Quantum 값을 보여주고 다른 알고리즘은 '-'로 표시한다.
	private String formatTimeQuantum(AlgorithmType algorithm) {
		if (algorithm != AlgorithmType.RR) {
			return "-";
		}

		String text = timeQuantumField.getText();
		return text == null || text.isBlank() ? "-" : text.trim();
	}

	// Core 종류 이름 뒤에 현재 선택된 개수를 붙여 표시용 라벨을 만든다.
	private String formatCoreLabel(CoreType coreType) {
		return coreType.getDisplayName() + " (" + coreSelectionController.countSelectedCoreType(coreType) + ")";
	}

	static final class OverviewLabels {
		private final EnumMap<OverviewField, Label> values = new EnumMap<>(OverviewField.class);

		// FXML Label들을 의미별 enum 키에 매핑해 이후 갱신 코드를 짧게 유지한다.
		OverviewLabels(
				Label algorithmValue,
				Label timeQuantumValue,
				Label pCoreLabel,
				Label pCoreValue,
				Label eCoreLabel,
				Label eCoreValue,
				Label processCountValue,
				Label totalTimeValue,
				Label throughputValue,
				Label avgWaitingValue,
				Label avgTurnaroundValue,
				Label avgResponseValue,
				Label cpuUtilizationValue,
				Label contextSwitchValue,
				Label idleTimeValue) {
			values.put(OverviewField.ALGORITHM, algorithmValue);
			values.put(OverviewField.TIME_QUANTUM, timeQuantumValue);
			values.put(OverviewField.P_CORE_LABEL, pCoreLabel);
			values.put(OverviewField.P_CORE_VALUE, pCoreValue);
			values.put(OverviewField.E_CORE_LABEL, eCoreLabel);
			values.put(OverviewField.E_CORE_VALUE, eCoreValue);
			values.put(OverviewField.PROCESS_COUNT, processCountValue);
			values.put(OverviewField.TOTAL_TIME, totalTimeValue);
			values.put(OverviewField.THROUGHPUT, throughputValue);
			values.put(OverviewField.AVG_WAITING, avgWaitingValue);
			values.put(OverviewField.AVG_TURNAROUND, avgTurnaroundValue);
			values.put(OverviewField.AVG_RESPONSE, avgResponseValue);
			values.put(OverviewField.CPU_UTILIZATION, cpuUtilizationValue);
			values.put(OverviewField.CONTEXT_SWITCH, contextSwitchValue);
			values.put(OverviewField.IDLE_TIME, idleTimeValue);
		}

		// 알고리즘 정보 영역의 값을 갱신한다.
		private void updateAlgorithm(String algorithm, String timeQuantum) {
			set(OverviewField.ALGORITHM, algorithm);
			set(OverviewField.TIME_QUANTUM, timeQuantum);
		}

		// Core 개수, 사용률, 전력량 정보를 갱신한다.
		private void updateCoreInfo(String pCoreLabelText, String pCoreSummary, String eCoreLabelText, String eCoreSummary) {
			set(OverviewField.P_CORE_LABEL, pCoreLabelText);
			set(OverviewField.P_CORE_VALUE, pCoreSummary);
			set(OverviewField.E_CORE_LABEL, eCoreLabelText);
			set(OverviewField.E_CORE_VALUE, eCoreSummary);
		}

		// 프로세스 수, 총 실행 시간, 처리량 정보를 갱신한다.
		private void updateProcessInfo(OverviewMetrics metrics) {
			set(OverviewField.PROCESS_COUNT, metrics.processCount());
			set(OverviewField.TOTAL_TIME, metrics.totalTime());
			set(OverviewField.THROUGHPUT, metrics.throughput());
		}

		// 평균 대기/반환/응답 시간과 CPU 사용률 정보를 갱신한다.
		private void updatePerformance(OverviewMetrics metrics) {
			set(OverviewField.AVG_WAITING, metrics.avgWaitingTime());
			set(OverviewField.AVG_TURNAROUND, metrics.avgTurnaroundTime());
			set(OverviewField.AVG_RESPONSE, metrics.avgResponseTime());
			set(OverviewField.CPU_UTILIZATION, metrics.cpuUtilization());
		}

		// Context Switch와 Idle Time 정보를 갱신한다.
		private void updateSystem(OverviewMetrics metrics) {
			set(OverviewField.CONTEXT_SWITCH, metrics.contextSwitches());
			set(OverviewField.IDLE_TIME, metrics.idleTime());
		}

		// 지정한 Overview 항목의 실제 Label 텍스트를 교체한다.
		private void set(OverviewField field, String text) {
			values.get(field).setText(text);
		}
	}

	private enum OverviewField {
		ALGORITHM,
		TIME_QUANTUM,
		P_CORE_LABEL,
		P_CORE_VALUE,
		E_CORE_LABEL,
		E_CORE_VALUE,
		PROCESS_COUNT,
		TOTAL_TIME,
		THROUGHPUT,
		AVG_WAITING,
		AVG_TURNAROUND,
		AVG_RESPONSE,
		CPU_UTILIZATION,
		CONTEXT_SWITCH,
		IDLE_TIME
	}
}
