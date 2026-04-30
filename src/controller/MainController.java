package controller;

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import model.AlgorithmType;
import model.CoreConfig;
import model.Process;
import model.SchedulingResult;
import service.SchedulingService;

public class MainController {

	@FXML
	private Pane titleBar;
	@FXML
	private Button minBtn;
	@FXML
	private Button maxBtn;
	@FXML
	private Button closeBtn;
	@FXML
	private ComboBox<AlgorithmType> algorithmCombo;
	@FXML
	private ToggleGroup coreGroup1;
	@FXML
	private ToggleGroup coreGroup2;
	@FXML
	private ToggleGroup coreGroup3;
	@FXML
	private ToggleGroup coreGroup4;
	@FXML
	private HBox simulationTabBar;
	@FXML
	private Button tabAddBtn;
	@FXML
	private ScrollPane ganttScrollPane;
	@FXML
	private AnchorPane ganttPane;

	@FXML
	private TextField timeQuantumField;
	@FXML
	private TextField addAtInput;
	@FXML
	private TextField addBtInput;
	@FXML
	private TextField updateAtInput;
	@FXML
	private TextField updateBtInput;
	@FXML
	private ComboBox<String> updateProcessCombo;
	@FXML
	private ComboBox<String> deleteProcessCombo;

	@FXML
	private TableView<Process> resultTable;
	@FXML
	private TableColumn<Process, String> pidColumn;
	@FXML
	private TableColumn<Process, Integer> atColumn;
	@FXML
	private TableColumn<Process, Integer> btColumn;
	@FXML
	private TableColumn<Process, Integer> wtColumn;
	@FXML
	private TableColumn<Process, Integer> ttColumn;
	@FXML
	private TableColumn<Process, Double> nttColumn;

	@FXML
	private Label overviewAlgorithmValue;
	@FXML
	private Label overviewTimeQuantumValue;
	@FXML
	private Label overviewPCoreLabel;
	@FXML
	private Label overviewPCoreValue;
	@FXML
	private Label overviewECoreLabel;
	@FXML
	private Label overviewECoreValue;
	@FXML
	private Label overviewProcessCountValue;
	@FXML
	private Label overviewTotalTimeValue;
	@FXML
	private Label overviewThroughputValue;
	@FXML
	private Label overviewAvgWaitingValue;
	@FXML
	private Label overviewAvgTurnaroundValue;
	@FXML
	private Label overviewAvgResponseValue;
	@FXML
	private Label overviewCpuUtilizationValue;
	@FXML
	private Label overviewContextSwitchValue;
	@FXML
	private Label overviewIdleTimeValue;

	private final List<Process> processList = new java.util.ArrayList<>();
	private final GanttChartController ganttController = new GanttChartController();
	private final SchedulingService schedulingService = new SchedulingService();
	private final DialogController dialogController = new DialogController();

	private CoreSelectionController coreSelectionController;
	private ProcessFormController processFormController;
	private ResultTableController resultTableController;
	private OverviewController overviewController;
	private SchedulingResult currentResult;
	private List<String> currentCoreLabels = List.of();
	private SimulationTabState activeTab;
	private boolean loadingTab;
	private SimulationTabController simulationTabController;

	@FXML
	public void initialize() {
		setupWindowControls();
		setupAlgorithmCombo();
		setupCoreOptions();
		setupTable();
		setupOverview();
		setupProcessForm();
		setupGanttViewport();
		setupTabs();
		drawGanttChartFrame();
		updateOverview(null, 0);
	}

	private void setupWindowControls() {
		new WindowController(titleBar.getParent(), minBtn, maxBtn, closeBtn).initialize();
	}

	private void setupAlgorithmCombo() {
		algorithmCombo.getItems().setAll(AlgorithmType.values());
		algorithmCombo.setValue(AlgorithmType.FCFS);
		algorithmCombo.valueProperty().addListener((observable, oldValue, newValue) -> updateOverview(null, 0));
		timeQuantumField.textProperty().addListener((observable, oldValue, newValue) -> updateOverview(null, 0));
	}

	private void setupCoreOptions() {
		coreSelectionController = new CoreSelectionController(coreGroup1, coreGroup2, coreGroup3, coreGroup4);
		coreSelectionController.addSelectionListener(() -> {
			if (loadingTab) {
				return;
			}
			currentResult = null;
			currentCoreLabels = coreSelectionController.getSelectedCoreLabels();
			drawGanttChartFrame();
			updateOverview(null, 0);
		});
	}

	private void setupTable() {
		resultTableController = new ResultTableController(
				resultTable,
				pidColumn,
				atColumn,
				btColumn,
				wtColumn,
				ttColumn,
				nttColumn);
		resultTableController.initialize();
	}

	private void setupOverview() {
		overviewController = new OverviewController(
				algorithmCombo,
				processList,
				coreSelectionController,
				timeQuantumField,
				overviewAlgorithmValue,
				overviewTimeQuantumValue,
				overviewPCoreLabel,
				overviewPCoreValue,
				overviewECoreLabel,
				overviewECoreValue,
				overviewProcessCountValue,
				overviewTotalTimeValue,
				overviewThroughputValue,
				overviewAvgWaitingValue,
				overviewAvgTurnaroundValue,
				overviewAvgResponseValue,
				overviewCpuUtilizationValue,
				overviewContextSwitchValue,
				overviewIdleTimeValue);
	}

	private void setupProcessForm() {
		processFormController = new ProcessFormController(
				processList,
				addAtInput,
				addBtInput,
				updateAtInput,
				updateBtInput,
				updateProcessCombo,
				deleteProcessCombo,
				dialogController,
				this::refreshProcessTable);
	}

	private void setupGanttViewport() {
		ganttScrollPane.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) -> redrawGanttChart());
	}

	private void setupTabs() {
		simulationTabController = new SimulationTabController(
				simulationTabBar,
				tabAddBtn,
				new SimulationTabController.Listener() {
					@Override
					public void beforeTabChange() {
						saveActiveTabState();
					}

					@Override
					public void afterTabSelected(SimulationTabState tab) {
						activeTab = tab;
						loadActiveTabState();
					}

					@Override
					public void onLastTabCloseRequested() {
						dialogController.showWarning("경고", "최소 하나의 Simulation은 유지해야 합니다.");
					}
				});
		simulationTabController.initialize();
	}
	private void saveActiveTabState() {
		if (activeTab == null || processFormController == null || coreSelectionController == null) {
			return;
		}

		activeTab.setProcesses(processList);
		activeTab.setProcessSequence(processFormController.getProcessSequence());
		activeTab.setAlgorithm(algorithmCombo.getValue());
		activeTab.setTimeQuantum(timeQuantumField.getText());
		activeTab.setCoreTypes(coreSelectionController.getSelectedCoreTypes());
		activeTab.setResult(currentResult);
		activeTab.setCoreLabels(currentCoreLabels);
	}

	private void loadActiveTabState() {
		if (activeTab == null) {
			return;
		}

		loadingTab = true;
		processList.clear();
		processList.addAll(activeTab.getProcesses());
		processFormController.setProcessSequence(activeTab.getProcessSequence());
		processFormController.reloadProcessCombos();
		algorithmCombo.setValue(activeTab.getAlgorithm());
		timeQuantumField.setText(activeTab.getTimeQuantum());
		coreSelectionController.selectCoreTypes(activeTab.getCoreTypes());
		currentResult = activeTab.getResult();
		currentCoreLabels = new java.util.ArrayList<>(activeTab.getCoreLabels());
		loadingTab = false;

		updateResultTable();
		redrawGanttChart();
		updateOverview(currentResult, currentResult == null ? 0 : getTotalTime(currentResult));
	}

	@FXML
	private void handleAddProcess() {
		processFormController.addProcess();
	}

	@FXML
	private void handleRandomProcess() {
		processFormController.addRandomProcesses();
	}

	@FXML
	private void handleUpdateProcess() {
		processFormController.updateProcess();
	}

	@FXML
	private void handleDeleteProcess() {
		processFormController.deleteProcess();
	}

	@FXML
	private void handleRun() {
		AlgorithmType algorithm = algorithmCombo.getValue();
		List<String> selectedCoreLabels = coreSelectionController.getSelectedCoreLabels();

		if (algorithm == null) {
			dialogController.showWarning("경고", "알고리즘을 선택하세요.");
			return;
		}
		if (selectedCoreLabels.isEmpty()) {
			dialogController.showWarning("경고", "하나 이상의 Core를 선택하세요.");
			return;
		}
		if (processList.isEmpty()) {
			dialogController.showWarning("경고", "프로세스를 먼저 추가하세요.");
			return;
		}

		int timeQuantum = 0;
		if (algorithm == AlgorithmType.RR) {
			Integer parsedTimeQuantum = processFormController.parsePositiveInt(timeQuantumField, "Time Quantum");
			if (parsedTimeQuantum == null) {
				return;
			}
			timeQuantum = parsedTimeQuantum;
		}

		try {
			List<CoreConfig> selectedCoreConfigs = coreSelectionController.getSelectedCoreConfigs();
			SchedulingResult result = schedulingService.run(algorithm, processList, selectedCoreConfigs, timeQuantum);
			currentResult = result;
			currentCoreLabels = selectedCoreLabels;
			drawGanttChart(result, selectedCoreLabels);
			updateResultTable();
			updateOverview(result, getTotalTime(result));
		} catch (IllegalArgumentException e) {
			dialogController.showWarning("실행 오류", e.getMessage());
		}
	}

	private void drawGanttChart(SchedulingResult result, List<String> coreLabels) {
		if (ganttPane != null) {
			ganttController.draw(result, ganttPane, coreLabels, getGanttViewportHeight());
		}
	}

	private void drawGanttChartFrame() {
		if (ganttPane != null) {
			ganttController.drawEmpty(ganttPane, coreSelectionController.getSelectedCoreLabels(), getGanttViewportHeight());
		}
	}

	private void redrawGanttChart() {
		if (currentResult == null) {
			drawGanttChartFrame();
		} else {
			drawGanttChart(currentResult, currentCoreLabels);
		}
	}

	private double getGanttViewportHeight() {
		if (ganttScrollPane == null || ganttScrollPane.getViewportBounds().getHeight() <= 0) {
			return 360;
		}
		return ganttScrollPane.getViewportBounds().getHeight();
	}

	private void refreshProcessTable() {
		currentResult = null;
		currentCoreLabels = coreSelectionController.getSelectedCoreLabels();
		resultTableController.setProcesses(processList);
		drawGanttChartFrame();
		updateOverview(null, 0);
	}

	private void updateResultTable() {
		resultTableController.setProcesses(processList);
		resultTableController.refresh();
	}

	private void updateOverview(SchedulingResult result, int totalTime) {
		if (overviewController != null) {
			overviewController.update(result, () -> totalTime);
		}
	}

	private int getTotalTime(SchedulingResult result) {
		return result.getGanttBlocks().stream()
				.mapToInt(block -> block.getEnd())
				.max()
				.orElse(0);
	}

}

