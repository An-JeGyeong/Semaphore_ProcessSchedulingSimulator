package model;

public class Process {

	private final String pid;

	private int arrivalTime;
	private int burstTime;
	private int remainingWork;
	private int executedTime;
	private int remainingTime;
	private int waitingTime;
	private int turnaroundTime;
	private double normalizedTT;

	public Process(String pid, int arrivalTime, int burstTime) {
		this.pid = pid;
		this.arrivalTime = arrivalTime;
		this.burstTime = burstTime;
		reset();
	}

	public String getPid() {
		return pid;
	}

	public int getArrivalTime() {
		return arrivalTime;
	}

	public int getBurstTime() {
		return burstTime;
	}

	public int getRemainingWork() {
		return remainingWork;
	}

	public int getExecutedTime() {
		return executedTime;
	}

	public int getRemainingTime() {
		return remainingTime;
	}

	public void setRemainingTime(int remainingTime) {
		this.remainingTime = remainingTime;
	}

	public int getWaitingTime() {
		return waitingTime;
	}

	public void setWaitingTime(int waitingTime) {
		this.waitingTime = waitingTime;
	}

	public int getTurnaroundTime() {
		return turnaroundTime;
	}

	public void setTurnaroundTime(int turnaroundTime) {
		this.turnaroundTime = turnaroundTime;
	}

	public double getNormalizedTT() {
		return normalizedTT;
	}

	public void setNormalizedTT(double normalizedTT) {
		this.normalizedTT = normalizedTT;
	}

	public void updateInput(int arrivalTime, int burstTime) {
		this.arrivalTime = arrivalTime;
		this.burstTime = burstTime;
		reset();
	}

	public void reset() {
		remainingWork = burstTime;
		executedTime = 0;
		remainingTime = burstTime;
		clearMetrics();
	}

	public void clearMetrics() {
		waitingTime = 0;
		turnaroundTime = 0;
		normalizedTT = 0.0;
	}

	public void executeOneSecond(CoreConfig core) {
		int performance = core.getCoreType().getPerformancePerSecond();
		int workDone = Math.min(performance, remainingWork);

		remainingWork -= workDone;
		executedTime++;
	}

	public boolean isFinished() {
		return remainingWork <= 0;
	}
}
