package controller;

import java.util.ArrayList;
import java.util.List;

import model.AlgorithmType;
import model.Process;
import model.SchedulingResult;

final class SimulationTabState {

	private final int id;
	private final String name;
	private List<Process> processes = new ArrayList<>();
	private int processSequence = 1;
	private AlgorithmType algorithm = AlgorithmType.FCFS;
	private String timeQuantum = "";
	private List<String> coreTypes = List.of("off", "off", "off", "off");
	private SchedulingResult result;
	private List<String> coreLabels = List.of();

	SimulationTabState(int id, String name) {
		this.id = id;
		this.name = name;
	}

	int getId() {
		return id;
	}

	String getName() {
		return name;
	}

	List<Process> getProcesses() {
		return processes;
	}

	void setProcesses(List<Process> processes) {
		this.processes = new ArrayList<>(processes);
	}

	int getProcessSequence() {
		return processSequence;
	}

	void setProcessSequence(int processSequence) {
		this.processSequence = processSequence;
	}

	AlgorithmType getAlgorithm() {
		return algorithm;
	}

	void setAlgorithm(AlgorithmType algorithm) {
		this.algorithm = algorithm == null ? AlgorithmType.FCFS : algorithm;
	}

	String getTimeQuantum() {
		return timeQuantum;
	}

	void setTimeQuantum(String timeQuantum) {
		this.timeQuantum = timeQuantum == null ? "" : timeQuantum;
	}

	List<String> getCoreTypes() {
		return coreTypes;
	}

	void setCoreTypes(List<String> coreTypes) {
		this.coreTypes = new ArrayList<>(coreTypes);
	}

	SchedulingResult getResult() {
		return result;
	}

	void setResult(SchedulingResult result) {
		this.result = result;
	}

	List<String> getCoreLabels() {
		return coreLabels;
	}

	void setCoreLabels(List<String> coreLabels) {
		this.coreLabels = new ArrayList<>(coreLabels);
	}
}
