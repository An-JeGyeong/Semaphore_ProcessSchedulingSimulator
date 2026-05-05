package model;

public class Process {

	// 하나의 프로세스 입력값과 실행 결과 메트릭을 함께 들고 있는 모델이다.
	// arrivalTime/burstTime은 사용자 입력값이고, WT/TT/NTT는 실행 후 서비스가 채운다.
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

	// 사용자가 AT/BT를 수정하면 이전 실행 결과를 지우고 입력 상태로 되돌린다.
	public void updateInput(int arrivalTime, int burstTime) {
		this.arrivalTime = arrivalTime;
		this.burstTime = burstTime;
		reset();
	}

	// 새 스케줄링 실행 전에 남은 작업량과 결과 메트릭을 초기화한다.
	public void reset() {
		remainingWork = burstTime;
		executedTime = 0;
		remainingTime = burstTime;
		clearMetrics();
	}

	// WT, TT, NTT만 지워서 아직 실행되지 않은 상태로 표시한다.
	public void clearMetrics() {
		waitingTime = 0;
		turnaroundTime = 0;
		normalizedTT = 0.0;
	}

	// Core 성능만큼 1초 동안 작업량을 줄인다.
	// P-Core가 2의 일을 처리해도 시간은 항상 1초 증가한다는 과제 조건을 따른다.
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
