package service.tomjerry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import model.CoreConfig;
import model.GanttBlock;
import model.Process;
import model.SchedulingResult;

public class TomJerrySchedulingService {

	private final List<GanttBlock> ganttBlocks = new ArrayList<>();

	// 새 Tom & Jerry 실행을 시작하기 전에 누적된 Gantt 결과를 비운다.
	public void reset() {
		ganttBlocks.clear();
	}

	// 현재까지 생쥐가 잡혀 실행된 기록을 SchedulingResult로 반환한다.
	public SchedulingResult getResult() {
		return new SchedulingResult(ganttBlocks);
	}

	// 모든 생쥐 프로세스가 완료됐는지 확인한다.
	public boolean allCompleted(List<? extends MouseStatus> mice) {
		return !mice.isEmpty() && mice.stream().allMatch(MouseStatus::isCompleted);
	}

	// 기본적으로 먼저 도착한 생쥐를 우선하지만, 일부 확률로 다른 생쥐를 고른다.
	// 이 랜덤성이 Tom & Jerry Scheduling의 실행 순서를 FCFS와 다르게 만든다.
	public <T extends MouseStatus> T chooseTarget(T currentTarget, List<T> mice, Random random) {
		List<T> ready = mice.stream()
				.filter(mouse -> mouse.isActive() && !mouse.isCompleted() && !mouse.isCaught())
				.sorted(Comparator.comparingInt(MouseStatus::getArrivalTime))
				.toList();

		if (ready.isEmpty()) {
			return null;
		}

		if (random.nextDouble() < 0.20) {
			return ready.get(random.nextInt(ready.size()));
		}
		if (currentTarget != null && ready.contains(currentTarget) && random.nextDouble() < 0.62) {
			return currentTarget;
		}
		return ready.get(0);
	}

	// 고양이가 생쥐를 잡은 순간을 프로세스 시작 시점으로 기록한다.
	// 실행 시간은 burstTime / Core 성능을 올림 처리해서 1초 단위 작업 조건을 맞춘다.
	public boolean startExecution(CoreExecution execution, Process process, CoreConfig core, int coreIndex, int currentTick) {
		int startTick = Math.max(currentTick, process.getArrivalTime());
		int duration = (int) Math.ceil(process.getBurstTime()
				/ (double) core.getCoreType().getPerformancePerSecond());
		int endTick = Math.max(startTick + 1, startTick + duration);

		execution.process = process;
		execution.coreIndex = coreIndex;
		execution.startTick = startTick;
		execution.endTick = endTick;
		execution.lastPublishedEndTick = startTick;

		return publishExecutionProgress(execution, startTick + 1);
	}

	// 실행 중인 프로세스를 현재 tick까지 진행시키고 완료 여부를 반환한다.
	public UpdateStatus updateExecution(CoreExecution execution, int tick) {
		if (!execution.isRunning()) {
			return UpdateStatus.unchanged();
		}

		boolean changed = publishExecutionProgress(execution, tick + 1);
		boolean completed = tick + 1 >= execution.endTick;
		if (completed) {
			completeExecution(execution);
			changed = true;
		}
		return new UpdateStatus(changed, completed);
	}

	// 별도 창에서 실행되는 동안 메인 Gantt가 실시간으로 늘어나야 하므로,
	// 완료 시점까지 한 번에 넣지 않고 현재 tick까지의 구간만 공개한다.
	private boolean publishExecutionProgress(CoreExecution execution, int visibleEndTick) {
		int endTick = Math.min(execution.endTick, Math.max(execution.startTick + 1, visibleEndTick));
		if (endTick <= execution.lastPublishedEndTick) {
			return false;
		}

		addOrExtendBlock(execution.process.getPid(), execution.coreIndex, execution.lastPublishedEndTick, endTick);
		execution.lastPublishedEndTick = endTick;
		return true;
	}

	// Tom & Jerry는 고양이가 잡은 뒤에는 한 Core에서 끝까지 실행되므로,
	// waitingTime은 arrivalTime부터 capture/startTick까지의 시간으로 계산한다.
	private void completeExecution(CoreExecution execution) {
		int turnaroundTime = execution.endTick - execution.process.getArrivalTime();
		int waitingTime = Math.max(0, execution.startTick - execution.process.getArrivalTime());
		int executedSeconds = execution.endTick - execution.startTick;

		execution.process.setWaitingTime(waitingTime);
		execution.process.setTurnaroundTime(turnaroundTime);
		execution.process.setNormalizedTT(executedSeconds == 0 ? 0.0 : turnaroundTime / (double) executedSeconds);
		execution.process.setRemainingTime(0);
		execution.clear();
	}

	// 같은 프로세스가 같은 Core에서 이어지면 Gantt 블록을 새로 만들지 않고 확장한다.
	private void addOrExtendBlock(String pid, int coreIndex, int start, int end) {
		if (end <= start) {
			return;
		}
		if (!ganttBlocks.isEmpty()) {
			GanttBlock last = ganttBlocks.get(ganttBlocks.size() - 1);
			if (last.getPid().equals(pid) && last.getCoreIndex() == coreIndex && last.getEnd() == start) {
				ganttBlocks.set(ganttBlocks.size() - 1, new GanttBlock(pid, coreIndex, last.getStart(), end));
				return;
			}
		}
		ganttBlocks.add(new GanttBlock(pid, coreIndex, start, end));
	}

	public interface MouseStatus {
		int getArrivalTime();

		boolean isActive();

		boolean isCaught();

		boolean isCompleted();
	}

	public static final class CoreExecution {
		private Process process;
		private int coreIndex;
		private int startTick;
		private int endTick;
		private int lastPublishedEndTick;

		public boolean isRunning() {
			return process != null;
		}

		private void clear() {
			process = null;
			coreIndex = 0;
			startTick = 0;
			endTick = 0;
			lastPublishedEndTick = 0;
		}
	}

	public static final class UpdateStatus {
		private final boolean changed;
		private final boolean completed;

		private UpdateStatus(boolean changed, boolean completed) {
			this.changed = changed;
			this.completed = completed;
		}

		public static UpdateStatus unchanged() {
			return new UpdateStatus(false, false);
		}

		public boolean isChanged() {
			return changed;
		}

		public boolean isCompleted() {
			return completed;
		}
	}
}
