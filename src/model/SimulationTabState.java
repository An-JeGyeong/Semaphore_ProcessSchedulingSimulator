package model;

import java.util.ArrayList;
import java.util.List;

public final class SimulationTabState {

	private final int id;
	private final String name;
	private List<Process> processes = new ArrayList<>();
	private int processSequence = 1;
	private AlgorithmType algorithm = AlgorithmType.FCFS;
	private String timeQuantum = "";
	private List<String> coreTypes = List.of("off", "off", "off", "off");
	private SchedulingResult result;
	private List<String> coreLabels = List.of();

	public SimulationTabState(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<Process> getProcesses() {
		return processes;
	}

	public void setProcesses(List<Process> processes) {
		this.processes = new ArrayList<>(processes);
	}

	public int getProcessSequence() {
		return processSequence;
	}

	public void setProcessSequence(int processSequence) {
		this.processSequence = processSequence;
	}

	public AlgorithmType getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(AlgorithmType algorithm) {
		this.algorithm = algorithm == null ? AlgorithmType.FCFS : algorithm;
	}

	public String getTimeQuantum() {
		return timeQuantum;
	}

	public void setTimeQuantum(String timeQuantum) {
		this.timeQuantum = timeQuantum == null ? "" : timeQuantum;
	}

	public List<String> getCoreTypes() {
		return coreTypes;
	}

	public void setCoreTypes(List<String> coreTypes) {
		this.coreTypes = new ArrayList<>(coreTypes);
	}

	public SchedulingResult getResult() {
		return result;
	}

	public void setResult(SchedulingResult result) {
		this.result = result;
	}

	public List<String> getCoreLabels() {
		return coreLabels;
	}

	public void setCoreLabels(List<String> coreLabels) {
		this.coreLabels = new ArrayList<>(coreLabels);
	}
}
