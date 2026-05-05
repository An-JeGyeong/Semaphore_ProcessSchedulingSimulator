package controller;

import java.util.List;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.Process;
import util.ProcessColorPalette;

final class ResultTableController {

	private final TableView<Process> resultTable;
	private final TableColumn<Process, String> pidColumn;
	private final TableColumn<Process, Integer> atColumn;
	private final TableColumn<Process, Integer> btColumn;
	private final TableColumn<Process, Integer> wtColumn;
	private final TableColumn<Process, Integer> ttColumn;
	private final TableColumn<Process, Double> nttColumn;

	// 결과 테이블과 각 컬럼 참조를 받아 컬럼 초기화와 데이터 교체를 전담한다.
	ResultTableController(
			TableView<Process> resultTable,
			TableColumn<Process, String> pidColumn,
			TableColumn<Process, Integer> atColumn,
			TableColumn<Process, Integer> btColumn,
			TableColumn<Process, Integer> wtColumn,
			TableColumn<Process, Integer> ttColumn,
			TableColumn<Process, Double> nttColumn) {
		this.resultTable = resultTable;
		this.pidColumn = pidColumn;
		this.atColumn = atColumn;
		this.btColumn = btColumn;
		this.wtColumn = wtColumn;
		this.ttColumn = ttColumn;
		this.nttColumn = nttColumn;
	}

	// 결과 테이블 컬럼과 프로세스 색상 렌더링을 초기화한다.
	void initialize() {
		pidColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPid()));
		pidColumn.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(String pid, boolean empty) {
				super.updateItem(pid, empty);

				if (empty || pid == null) {
					setText(null);
					setStyle("");
					return;
				}

				setText(pid);
				setStyle("-fx-text-fill: " + ProcessColorPalette.getColor(pid)
						+ "; -fx-font-family: \"Noto Sans\"; -fx-font-size: 10px;");
			}
		});
		atColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getArrivalTime()).asObject());
		btColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getBurstTime()).asObject());
		wtColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getWaitingTime()).asObject());
		ttColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTurnaroundTime()).asObject());
		nttColumn.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getNormalizedTT()).asObject());
		nttColumn.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(Double value, boolean empty) {
				super.updateItem(value, empty);
				setText(empty || value == null ? null : String.format("%.2f", value));
			}
		});
	}

	// 테이블에 표시할 프로세스 목록을 교체한다.
	void setProcesses(List<Process> processes) {
		resultTable.getItems().setAll(processes);
	}

	// 기존 행 높이와 스타일을 유지하면서 테이블을 다시 그린다.
	void refresh() {
		resultTable.refresh();
	}
}
