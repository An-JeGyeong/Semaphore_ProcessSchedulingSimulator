package controller;

import java.util.List;
import java.util.function.Supplier;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.AlgorithmType;
import model.Process;
import model.SchedulingResult;

final class OverviewController {

	private final ComboBox<AlgorithmType> algorithmCombo;
	private final CoreSelectionController coreSelectionController;
	private final TextField timeQuantumField;
	private final Label algorithmValue;
	private final Label timeQuantumValue;
	private final Label pCoreLabel;
	private final Label pCoreValue;
	private final Label eCoreLabel;
	private final Label eCoreValue;
	private final Label processCountValue;
	private final Label totalTimeValue;
	private final Label throughputValue;
	private final Label avgWaitingValue;
	private final Label avgTurnaroundValue;
	private final Label avgResponseValue;
	private final Label cpuUtilizationValue;
	private final Label contextSwitchValue;
	private final Label idleTimeValue;
	private final OverviewMetricsCalculator metricsCalculator;

	OverviewController(
			ComboBox<AlgorithmType> algorithmCombo,
			List<Process> processList,
			CoreSelectionController coreSelectionController,
			TextField timeQuantumField,
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
		this.algorithmCombo = algorithmCombo;
		this.coreSelectionController = coreSelectionController;
		this.timeQuantumField = timeQuantumField;
		this.algorithmValue = algorithmValue;
		this.timeQuantumValue = timeQuantumValue;
		this.pCoreLabel = pCoreLabel;
		this.pCoreValue = pCoreValue;
		this.eCoreLabel = eCoreLabel;
		this.eCoreValue = eCoreValue;
		this.processCountValue = processCountValue;
		this.totalTimeValue = totalTimeValue;
		this.throughputValue = throughputValue;
		this.avgWaitingValue = avgWaitingValue;
		this.avgTurnaroundValue = avgTurnaroundValue;
		this.avgResponseValue = avgResponseValue;
		this.cpuUtilizationValue = cpuUtilizationValue;
		this.contextSwitchValue = contextSwitchValue;
		this.idleTimeValue = idleTimeValue;
		this.metricsCalculator = new OverviewMetricsCalculator(processList, coreSelectionController);
	}

	void update(SchedulingResult result, Supplier<Integer> totalTimeSupplier) {
		AlgorithmType algorithm = algorithmCombo.getValue();
		int totalTime = result == null ? 0 : totalTimeSupplier.get();
		OverviewMetrics metrics = metricsCalculator.calculate(result, totalTime);

		algorithmValue.setText(algorithm == null ? "-" : algorithm.name());
		timeQuantumValue.setText(formatTimeQuantum(algorithm));
		pCoreLabel.setText(formatCoreLabel("P-Core"));
		pCoreValue.setText(metrics.pCoreSummary());
		eCoreLabel.setText(formatCoreLabel("E-Core"));
		eCoreValue.setText(metrics.eCoreSummary());
		processCountValue.setText(metrics.processCount());
		totalTimeValue.setText(metrics.totalTime());
		throughputValue.setText(metrics.throughput());
		avgWaitingValue.setText(metrics.avgWaitingTime());
		avgTurnaroundValue.setText(metrics.avgTurnaroundTime());
		avgResponseValue.setText(metrics.avgResponseTime());
		cpuUtilizationValue.setText(metrics.cpuUtilization());
		contextSwitchValue.setText(metrics.contextSwitches());
		idleTimeValue.setText(metrics.idleTime());
	}

	private String formatTimeQuantum(AlgorithmType algorithm) {
		if (algorithm != AlgorithmType.RR) {
			return "-";
		}

		String text = timeQuantumField.getText();
		return text == null || text.isBlank() ? "-" : text.trim();
	}

	private String formatCoreLabel(String coreType) {
		return coreType + " (" + coreSelectionController.countSelectedCoreType(coreType) + ")";
	}
}
