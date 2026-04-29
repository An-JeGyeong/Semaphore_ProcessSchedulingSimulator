package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.beans.property.*;

import model.AlgorithmType;
import model.Process;

public class MainController {

	@FXML private Pane titleBar;
	@FXML private Button minBtn;
	@FXML private Button maxBtn;
	@FXML private Button closeBtn;
	@FXML private ComboBox<AlgorithmType> algorithmCombo;
	@FXML private ScrollPane ganttScrollPane;
	@FXML private AnchorPane ganttPane;
	
	@FXML private TextField addAtInput;
	@FXML private TextField addBtInput;
	@FXML private TextField updateAtInput;
	@FXML private TextField updateBtInput;
	@FXML private ComboBox<String> updateProcessCombo;
	@FXML private ComboBox<String> deleteProcessCombo;
	
	@FXML private TableView<Process> resultTable;

	@FXML private TableColumn<Process, String> pidColumn;
	@FXML private TableColumn<Process, Integer> atColumn;
	@FXML private TableColumn<Process, Integer> btColumn;
	@FXML private TableColumn<Process, Integer> wtColumn;
	@FXML private TableColumn<Process, Integer> ttColumn;
	@FXML private TableColumn<Process, Double> nttColumn;

	private final List<Process> processList = new ArrayList<>();
	private final Random random = new Random();

	private int processSequence = 1;

    private double xOffset;
    private double yOffset;
    private boolean draggingFromMaximized = false;
    
    private final GanttChartController ganttController = new GanttChartController();
	
    @FXML
    public void initialize() {
    	
    	// 창 클릭 시, 클릭 위치 저장
    	titleBar.setOnMousePressed(event -> {
    	    Stage stage = (Stage) titleBar.getScene().getWindow();

    	    xOffset = event.getSceneX();
    	    yOffset = event.getSceneY();

    	    draggingFromMaximized = stage.isMaximized();
    	});

     // 창 드래그 이동
        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();

            // 최대화 상태에서 아래로 드래그하면 복원
            if (draggingFromMaximized) {
                stage.setMaximized(false);

                // 복원된 창이 마우스 아래 자연스럽게 오도록 위치 보정
                xOffset = stage.getWidth() / 2;
                yOffset = event.getSceneY();

                draggingFromMaximized = false;
            }

            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        
        // 최소화
        minBtn.setOnAction(event -> {
            Stage stage = (Stage) minBtn.getScene().getWindow();
            stage.setIconified(true);
        });
        
        // 최대화, 복원 버튼
        maxBtn.setOnAction(event -> {
            Stage stage = (Stage) maxBtn.getScene().getWindow();
            stage.setMaximized(!stage.isMaximized());
        });
        
        // 닫기
        closeBtn.setOnAction(event -> {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        });
        
        // 타이틀바 더블클릭 시 최대화, 복원
        titleBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Stage stage = (Stage) titleBar.getScene().getWindow();
                stage.setMaximized(!stage.isMaximized());
            }
        });
        
     // 화면 맨 위 근처로 드래그해서 놓으면 최대화
        titleBar.setOnMouseReleased(event -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();

            if (event.getScreenY() <= 5) {
                stage.setMaximized(true);
            }
        });
        
        setupAlgorithmCombo();
        
        setupTable();
    }
    
    private void setupAlgorithmCombo() {
    	algorithmCombo.getItems().addAll(AlgorithmType.values());
    	algorithmCombo.setValue(AlgorithmType.FCFS);
    }
    
    @FXML
    private void handleAddProcess() {
    	if (processList.size() >= 15) {
            showWarning("프로세스 추가 제한", "프로세스는 최대 15개까지 추가할 수 있습니다.");
            return;
        }

        if (addAtInput.getText().isBlank() || addBtInput.getText().isBlank()) {
            showWarning("입력 오류", "Arrival Time과 Burst Time을 모두 입력하세요.");
            return;
        }

        int at;
        int bt;

        try {
            at = Integer.parseInt(addAtInput.getText());
            bt = Integer.parseInt(addBtInput.getText());
        } catch (NumberFormatException e) {
            showWarning("입력 오류", "Arrival Time과 Burst Time은 숫자만 입력할 수 있습니다.");
            return;
        }

        String pid = "P" + processSequence++;

        Process process = new Process(pid, at, bt);
        processList.add(process);

        updateProcessCombo.getItems().add(pid);
        deleteProcessCombo.getItems().add(pid);

        addAtInput.clear();
        addBtInput.clear();

        refreshProcessTable();
    }
    
    @FXML
    private void handleRandomProcess() {
    	if (processList.size() >= 15) {
            showWarning("프로세스 추가 제한", "프로세스는 최대 15개까지 추가할 수 있습니다.");
            return;
        }
    	
        int countToAdd = Math.min(5, 15 - processList.size());

        for (int i = 0; i < countToAdd; i++) {
            int at = random.nextInt(31);      // 0 ~ 10
            int bt = random.nextInt(20) + 1;  // 1 ~ 10

            String pid = "P" + processSequence++;

            Process process = new Process(pid, at, bt);
            processList.add(process);

            updateProcessCombo.getItems().add(pid);
            deleteProcessCombo.getItems().add(pid);
        }

        refreshProcessTable();
    }
    
    @FXML
    private void handleUpdateProcess() {
        String selectedPid = updateProcessCombo.getValue();

        if (selectedPid == null) {
            return;
        }

        int at = Integer.parseInt(updateAtInput.getText());
        int bt = Integer.parseInt(updateBtInput.getText());

        for (Process process : processList) {
            if (process.getPid().equals(selectedPid)) {
                process.updateInput(at, bt);
                break;
            }
        }

        updateAtInput.clear();
        updateBtInput.clear();

        refreshProcessTable();
    }
    
    @FXML
    private void handleDeleteProcess() {
        String selectedPid = deleteProcessCombo.getValue();

        if (selectedPid == null) {
            return;
        }

        processList.removeIf(process -> process.getPid().equals(selectedPid));

        updateProcessCombo.getItems().remove(selectedPid);
        deleteProcessCombo.getItems().remove(selectedPid);

        updateProcessCombo.setValue(null);
        deleteProcessCombo.setValue(null);

        refreshProcessTable();
    }
    
    @FXML
    private void handleRun() {
        ganttController.drawTest(ganttPane);
    }
    
    private void setupTable() {

        pidColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPid()));

        atColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getArrivalTime()).asObject());

        btColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getBurstTime()).asObject());

        wtColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(0).asObject());

        ttColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(0).asObject());

        nttColumn.setCellValueFactory(data ->
                new SimpleDoubleProperty(0.0).asObject());
    }
    
    private void refreshProcessTable() {
        resultTable.getItems().setAll(processList);
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}