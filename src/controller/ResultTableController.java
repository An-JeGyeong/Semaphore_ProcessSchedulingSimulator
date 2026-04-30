package controller;

import java.util.List;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.Process;

final class ResultTableController {

	private final TableView<Process> resultTable;
	private final TableColumn<Process, String> pidColumn;
	private final TableColumn<Process, Integer> atColumn;
	private final TableColumn<Process, Integer> btColumn;
	private final TableColumn<Process, Integer> wtColumn;
	private final TableColumn<Process, Integer> ttColumn;
	private final TableColumn<Process, Double> nttColumn;

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

	void setProcesses(List<Process> processes) {
		resultTable.getItems().setAll(processes);
	}

	void refresh() {
		resultTable.refresh();
	}
}
