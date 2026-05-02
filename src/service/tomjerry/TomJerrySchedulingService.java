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

	public void reset() {
		ganttBlocks.clear();
	}

	public SchedulingResult getResult() {
		return new SchedulingResult(ganttBlocks);
	}

	public boolean allCompleted(List<? extends MouseStatus> mice) {
		return !mice.isEmpty() && mice.stream().allMatch(MouseStatus::isCompleted);
	}

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

	private boolean publishExecutionProgress(CoreExecution execution, int visibleEndTick) {
		int endTick = Math.min(execution.endTick, Math.max(execution.startTick + 1, visibleEndTick));
		if (endTick <= execution.lastPublishedEndTick) {
			return false;
		}

		addOrExtendBlock(execution.process.getPid(), execution.coreIndex, execution.lastPublishedEndTick, endTick);
		execution.lastPublishedEndTick = endTick;
		return true;
	}

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
