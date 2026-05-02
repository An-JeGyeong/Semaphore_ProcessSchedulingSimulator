package service;

import java.util.List;

final class SchedulingMetrics {

	private SchedulingMetrics() {
	}

	static void apply(List<ProcessState> states) {
		for (ProcessState state : states) {
			int turnaroundTime = state.finishTime - state.arrivalTime();
			int waitingTime = Math.max(0, turnaroundTime - state.executedSeconds);
			double normalizedTurnaroundTime = state.executedSeconds == 0
					? 0.0
					: turnaroundTime / (double) state.executedSeconds;

			state.process.setWaitingTime(waitingTime);
			state.process.setTurnaroundTime(turnaroundTime);
			state.process.setNormalizedTT(normalizedTurnaroundTime);
			state.process.setRemainingTime(0);
		}
	}
}
