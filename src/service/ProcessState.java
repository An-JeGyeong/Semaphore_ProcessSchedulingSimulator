package service;

import model.Process;

final class ProcessState {

	final Process process;
	final int inputOrder;
	int remainingWork;
	int executedSeconds;
	int finishTime;

	ProcessState(Process process, int inputOrder) {
		this.process = process;
		this.inputOrder = inputOrder;
		this.remainingWork = process.getBurstTime();
	}

	String pid() {
		return process.getPid();
	}

	int arrivalTime() {
		return process.getArrivalTime();
	}

	int burstTime() {
		return process.getBurstTime();
	}

	int remainingWork() {
		return remainingWork;
	}

	int inputOrder() {
		return inputOrder;
	}

	boolean isReady(int time) {
		return !isFinished() && arrivalTime() <= time;
	}

	boolean isFinished() {
		return remainingWork <= 0;
	}
}
