package controller;

import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
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
import model.SimulationTabState;
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
	private final TomJerrySimulationController tomJerrySimulationController = new TomJerrySimulationController();

	private CoreSelectionController coreSelectionController;
	private ProcessFormController processFormController;
	private ResultTableController resultTableController;
	private OverviewController overviewController;
	private SchedulingResult currentResult;
	private List<String> currentCoreLabels = List.of();
	private SimulationTabState activeTab;
	private boolean loadingTab;
	private SimulationTabController simulationTabController;
	private static final double BASE_GANTT_VIEWPORT_HEIGHT = 360;

	// FXML 로딩 직후 한 번 호출된다. 메인 컨트롤러는 직접 로직을 처리하기보다
	// 창, 옵션, 테이블, Overview, Gantt, 탭 담당 컨트롤러를 연결하는 조립 역할을 한다.
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

	// 커스텀 타이틀바의 이동, 최소화, 최대화, 닫기 동작을 설정한다.
	private void setupWindowControls() {
		new WindowController(titleBar.getParent(), minBtn, maxBtn, closeBtn).initialize();
	}

	// 알고리즘과 Time Quantum은 실행 전에도 Overview에 보여야 하므로 값이 바뀔 때마다 패널을 갱신한다.
	private void setupAlgorithmCombo() {
		algorithmCombo.getItems().setAll(AlgorithmType.values());
		algorithmCombo.setValue(AlgorithmType.FCFS);
		
		algorithmCombo.valueProperty().addListener((observable, oldValue, newValue) -> updateOverview(null, 0));
		
		timeQuantumField.textProperty().addListener((observable, oldValue, newValue) -> updateOverview(null, 0));
	}

	// Core 선택은 실행 결과의 전제 조건이므로 변경되면 이전 결과를 버리고 빈 Gantt 프레임만 다시 그린다.
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

	// 프로세스 결과 테이블의 컬럼 렌더링과 데이터 바인딩을 준비한다.
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

	// 우측 Overview 패널에 필요한 라벨과 계산기를 연결한다.
	private void setupOverview() {
		overviewController = new OverviewController(
				algorithmCombo,
				processList,
				coreSelectionController,
				timeQuantumField,
				new OverviewController.OverviewLabels(
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
						overviewIdleTimeValue));
	}

	// 프로세스 추가, 랜덤 생성, 수정, 삭제 입력 폼을 담당 컨트롤러에 연결한다.
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

	// Gantt 영역 크기가 바뀔 때 차트와 하단 스크롤바 위치를 다시 맞춘다.
	private void setupGanttViewport() {
		ganttScrollPane.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) -> {
			redrawGanttChart();
			alignGanttHorizontalScrollBar();
		});
	}

	// 탭마다 프로세스 목록, Core 선택, 알고리즘, 실행 결과가 독립적으로 유지되도록 저장/복원을 연결한다.
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
			}
	);
		simulationTabController.initialize();
	}
	
	// 다른 탭으로 이동하기 직전 현재 화면 상태를 모델에 복사한다.
	// 이 과정을 거치지 않으면 탭을 바꿀 때 입력값과 Gantt 결과가 사라진다.
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

	// 탭 모델에 저장된 값을 화면 컨트롤에 다시 넣는다.
	// loadingTab 플래그는 복원 중 발생하는 라디오 버튼 이벤트가 결과를 초기화하지 않도록 막는다.
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

	// ADD 버튼 클릭 시 입력된 AT/BT 값으로 프로세스를 추가한다.
	@FXML
	private void handleAddProcess() {
		processFormController.addProcess();
	}

	// RANDOM 버튼 클릭 시 남은 슬롯만큼 랜덤 프로세스를 생성한다.
	@FXML
	private void handleRandomProcess() {
		processFormController.addRandomProcesses();
	}

	// UPDATE 버튼 클릭 시 선택된 프로세스의 AT/BT 값을 수정한다.
	@FXML
	private void handleUpdateProcess() {
		processFormController.updateProcess();
	}

	// DELETE 버튼 클릭 시 선택된 프로세스를 목록에서 제거한다.
	@FXML
	private void handleDeleteProcess() {
		processFormController.deleteProcess();
	}

	// 실행 버튼의 전체 흐름을 담당한다. 입력 검증, RR Time Quantum 처리,
	// 일반 알고리즘 실행과 Tom & Jerry 별도 창 실행을 여기서 분기한다.
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
			
			if (algorithm == AlgorithmType.CUSTOM) {
				processList.forEach(Process::reset);
				
				currentResult = new SchedulingResult(List.of());
				currentCoreLabels = selectedCoreLabels;
				
				drawGanttChartFrame();
				updateResultTable();
				updateOverview(currentResult, 0);
				
				tomJerrySimulationController.show(
					processList,
					selectedCoreConfigs,
					this::updateCustomSimulationResult
				);
				
				return;
			}
			
			SchedulingResult result = schedulingService.run(
				algorithm,
				processList,
				selectedCoreConfigs,
				timeQuantum
			);
			
			currentResult = result;
			currentCoreLabels = selectedCoreLabels;
			
			drawGanttChart(result, selectedCoreLabels);
			updateResultTable();
			updateOverview(result, getTotalTime(result));
			
		} catch (IllegalArgumentException e) {
			dialogController.showWarning("실행 오류", e.getMessage());
		}
	}

	// Tom & Jerry는 별도 창에서 실행되지만 Gantt/Table/Overview는 메인 화면에 계속 갱신되어야 한다.
	// 시뮬레이션 창이 새 SchedulingResult를 전달할 때마다 현재 결과로 교체한다.
	private void updateCustomSimulationResult(SchedulingResult result) {
		currentResult = result;
		drawGanttChart(result, currentCoreLabels);
		updateResultTable();
		updateOverview(result, getTotalTime(result));
	}

	// 스케줄링 결과와 Core 라벨을 기준으로 Gantt 차트를 그린다.
	private void drawGanttChart(SchedulingResult result, List<String> coreLabels) {
		if (ganttPane != null) {
			ganttController.draw(result, ganttPane, coreLabels, getGanttViewportHeight());
			alignGanttHorizontalScrollBar();
		}
	}

	// 실행 결과가 없을 때도 선택된 Core 구조를 보여주는 빈 Gantt 프레임을 그린다.
	private void drawGanttChartFrame() {
		if (ganttPane != null) {
			ganttController.drawEmpty(ganttPane, coreSelectionController.getSelectedCoreLabels(), getGanttViewportHeight());
			alignGanttHorizontalScrollBar();
		}
	}

	// 현재 결과 유무에 따라 빈 프레임 또는 실제 Gantt 차트를 다시 렌더링한다.
	private void redrawGanttChart() {
		if (currentResult == null) {
			drawGanttChartFrame();
		} else {
			drawGanttChart(currentResult, currentCoreLabels);
		}
	}

	// JavaFX 레이아웃이 끝나기 전에는 viewport 높이가 0일 수 있다.
	// 그 경우 기본 높이를 사용해 초기 Gantt 배치가 깨지지 않게 한다.
	private double getGanttViewportHeight() {
		if (ganttScrollPane == null || ganttScrollPane.getViewportBounds().getHeight() <= 0) {
			return BASE_GANTT_VIEWPORT_HEIGHT;
		}
		return ganttScrollPane.getViewportBounds().getHeight();
	}

	// ScrollPane 내부 스크롤바는 레이아웃 이후에 생성되므로 Platform.runLater로 찾는다.
	// 최대화처럼 Gantt 영역이 커질 때 스크롤바를 차트 쪽으로 끌어올려 시각적 간격을 줄인다.
	private void alignGanttHorizontalScrollBar() {
		if (ganttScrollPane == null) {
			return;
		}

		Platform.runLater(() -> {
			Node horizontalBar = ganttScrollPane.lookupAll(".scroll-bar").stream()
					.filter(node -> node instanceof ScrollBar)
					.map(node -> (ScrollBar) node)
					.filter(scrollBar -> scrollBar.getOrientation() == Orientation.HORIZONTAL)
					.findFirst()
					.orElse(null);

			if (horizontalBar != null) {
				horizontalBar.setTranslateY(-getGanttVerticalOffset());
			}
		});
	}

	// Gantt 영역이 기본 높이보다 커졌을 때 스크롤바를 위로 올릴 보정값을 계산한다.
	private double getGanttVerticalOffset() {
		double extraHeight = Math.max(0, getGanttViewportHeight() - BASE_GANTT_VIEWPORT_HEIGHT);
		return Math.min(90, extraHeight * 0.28);
	}

	// 프로세스 목록이 변경되면 결과를 초기화하고 테이블, Gantt, Overview를 갱신한다.
	private void refreshProcessTable() {
		currentResult = null;
		currentCoreLabels = coreSelectionController.getSelectedCoreLabels();
		resultTableController.setProcesses(processList);
		drawGanttChartFrame();
		updateOverview(null, 0);
	}

	// 현재 프로세스 목록과 계산된 메트릭을 테이블에 다시 반영한다.
	private void updateResultTable() {
		resultTableController.setProcesses(processList);
		resultTableController.refresh();
	}

	// 현재 결과와 총 실행 시간을 기준으로 Overview 값을 갱신한다.
	private void updateOverview(SchedulingResult result, int totalTime) {
		if (overviewController != null) {
			overviewController.update(result, () -> totalTime);
		}
	}

	// Gantt 블록 중 가장 늦게 끝난 시간을 전체 실행 시간으로 계산한다.
	private int getTotalTime(SchedulingResult result) {
		return result.getGanttBlocks().stream()
				.mapToInt(block -> block.getEnd())
				.max()
				.orElse(0);
	}

}

