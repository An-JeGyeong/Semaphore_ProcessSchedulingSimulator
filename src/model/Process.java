package model;

public class Process {

	private final String pid;

	private int arrivalTime;
	private int burstTime;
	private int remainingWork;
	private int executedTime;

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

	public void updateInput(int arrivalTime, int burstTime) {
		this.arrivalTime = arrivalTime;
		this.burstTime = burstTime;
		reset();
	}

	public void reset() {
		this.remainingWork = burstTime;
		this.executedTime = 0;
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
