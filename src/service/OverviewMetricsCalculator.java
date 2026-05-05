package service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import model.CoreType;
import model.GanttBlock;
import model.OverviewMetrics;
import model.Process;
import model.SchedulingResult;

public final class OverviewMetricsCalculator {

	private final List<Process> processList;
	private final Supplier<List<String>> coreLabelsSupplier;
	private final Function<CoreType, Integer> coreCountProvider;

	public OverviewMetricsCalculator(
			List<Process> processList,
			Supplier<List<String>> coreLabelsSupplier,
			Function<CoreType, Integer> coreCountProvider) {
		this.processList = processList;
		this.coreLabelsSupplier = coreLabelsSupplier;
		this.coreCountProvider = coreCountProvider;
	}

	// Overview에 필요한 표시 문자열을 한 번에 계산한다.
	// result가 null이면 아직 실행 전 상태이므로 모든 성능 지표를 0 또는 '-'에 가까운 값으로 만든다.
	public OverviewMetrics calculate(SchedulingResult result, int totalTime) {
		List<String> coreLabels = coreLabelsSupplier.get();

		return new OverviewMetrics(
				formatCoreSummary(result, coreLabels, CoreType.P_CORE, totalTime),
				formatCoreSummary(result, coreLabels, CoreType.E_CORE, totalTime),
				String.valueOf(processList.size()),
				String.valueOf(totalTime),
				formatDecimal(calculateThroughput(totalTime)),
				formatDecimal(averageWaitingTime(result)),
				formatDecimal(averageTurnaroundTime(result)),
				formatDecimal(averageResponseTime(result)),
				formatPercent(calculateCpuUtilization(result, coreLabels, totalTime)),
				String.valueOf(countContextSwitches(result)),
				String.valueOf(calculateIdleTime(result, coreLabels, totalTime)));
	}

	// Core 종류별 사용률과 소비 전력을 "57.5% / 140.0W" 형식으로 만든다.
	// 개수는 CoreSelectionController가 제공하고, 사용 시간은 Gantt 블록에서 계산한다.
	private String formatCoreSummary(SchedulingResult result, List<String> coreLabels, CoreType coreType, int totalTime) {
		double utilization = calculateCoreUtilization(result, coreLabels, coreType, totalTime);
		double power = calculateCorePower(result, coreLabels, coreType);
		return formatPercent(utilization) + " / " + formatPower(power);
	}

	private double calculateThroughput(int totalTime) {
		if (totalTime <= 0) {
			return 0.0;
		}
		return (double) processList.size() / totalTime;
	}

	private double averageWaitingTime(SchedulingResult result) {
		if (result == null || processList.isEmpty()) {
			return 0.0;
		}
		return processList.stream()
				.mapToInt(Process::getWaitingTime)
				.average()
				.orElse(0.0);
	}

	private double averageTurnaroundTime(SchedulingResult result) {
		if (result == null || processList.isEmpty()) {
			return 0.0;
		}
		return processList.stream()
				.mapToInt(Process::getTurnaroundTime)
				.average()
				.orElse(0.0);
	}

	// 각 프로세스가 처음 실행되기까지 걸린 시간을 평균낸다.
	private double averageResponseTime(SchedulingResult result) {
		if (result == null || processList.isEmpty()) {
			return 0.0;
		}

		Map<String, Integer> firstStartByPid = new HashMap<>();
		for (GanttBlock block : result.getGanttBlocks()) {
			firstStartByPid.merge(block.getPid(), block.getStart(), Math::min);
		}

		return processList.stream()
				.mapToInt(process -> Math.max(0, firstStartByPid.getOrDefault(process.getPid(), process.getArrivalTime())
						- process.getArrivalTime()))
				.average()
				.orElse(0.0);
	}

	// 전체 Core 시간 대비 실제 작업이 수행된 시간의 비율을 계산한다.
	private double calculateCpuUtilization(SchedulingResult result, List<String> coreLabels, int totalTime) {
		if (result == null || totalTime <= 0 || coreLabels.isEmpty()) {
			return 0.0;
		}

		int busyTime = calculateBusyTime(result);
		return (double) busyTime / (totalTime * coreLabels.size());
	}

	private double calculateCoreUtilization(
			SchedulingResult result,
			List<String> coreLabels,
			CoreType coreType,
			int totalTime) {
		int coreCount = countCoreType(coreType);
		if (result == null || totalTime <= 0 || coreLabels.isEmpty() || coreCount == 0) {
			return 0.0;
		}

		int busyTime = calculateBusyTimeForCoreType(result, coreLabels, coreType);
		return (double) busyTime / (totalTime * coreCount);
	}

	// 소비 전력은 과제 명세에 맞춰 실행 시간 전력 + 시동 전력으로 계산한다.
	// 쉬던 Core가 새 블록을 시작할 때마다 startup event로 보고 시동 전력을 더한다.
	private double calculateCorePower(SchedulingResult result, List<String> coreLabels, CoreType coreType) {
		if (countCoreType(coreType) == 0) {
			return 0.0;
		}

		int busyTime = calculateBusyTimeForCoreType(result, coreLabels, coreType);
		int startupEvents = countStartupEventsForType(result, coreLabels, coreType);
		return startupEvents * coreType.getStartupPower() + busyTime * coreType.getPowerPerSecond();
	}

	private int calculateBusyTimeForCoreType(SchedulingResult result, List<String> coreLabels, CoreType coreType) {
		if (result == null || coreLabels.isEmpty()) {
			return 0;
		}

		int busyTime = 0;
		for (GanttBlock block : result.getGanttBlocks()) {
			int laneIndex = block.getCoreIndex();
			if (laneIndex >= 0
					&& laneIndex < coreLabels.size()
					&& coreLabels.get(laneIndex).startsWith(coreType.getDisplayName())) {
				busyTime += block.getEnd() - block.getStart();
			}
		}
		return busyTime;
	}

	// 같은 Core에서 이전 블록과 바로 이어지면 계속 사용 중으로 보고 시동 전력을 더하지 않는다.
	// 블록 사이에 빈 시간이 있으면 미사용 상태에서 다시 켜진 것으로 계산한다.
	private int countStartupEventsForType(SchedulingResult result, List<String> coreLabels, CoreType coreType) {
		if (result == null || coreLabels.isEmpty()) {
			return 0;
		}

		Map<Integer, Integer> lastEndByCore = new HashMap<>();
		int startupEvents = 0;

		for (GanttBlock block : result.getGanttBlocks()) {
			int laneIndex = block.getCoreIndex();
			if (laneIndex >= 0
					&& laneIndex < coreLabels.size()
					&& coreLabels.get(laneIndex).startsWith(coreType.getDisplayName())) {
				Integer lastEnd = lastEndByCore.get(laneIndex);
				if (lastEnd == null || lastEnd < block.getStart()) {
					startupEvents++;
				}
				lastEndByCore.put(laneIndex, block.getEnd());
			}
		}

		return startupEvents;
	}

	private int calculateIdleTime(SchedulingResult result, List<String> coreLabels, int totalTime) {
		if (result == null || totalTime <= 0 || coreLabels.isEmpty()) {
			return 0;
		}

		int capacity = totalTime * coreLabels.size();
		return Math.max(0, capacity - calculateBusyTime(result));
	}

	private int calculateBusyTime(SchedulingResult result) {
		return result.getGanttBlocks().stream()
				.mapToInt(block -> block.getEnd() - block.getStart())
				.sum();
	}

	// 같은 Core에서 이전 프로세스와 다른 프로세스로 바뀐 횟수를 계산한다.
	private int countContextSwitches(SchedulingResult result) {
		if (result == null || result.getGanttBlocks().size() < 2) {
			return 0;
		}

		int switchCount = 0;
		Map<Integer, String> previousPidByCore = new HashMap<>();

		for (GanttBlock block : result.getGanttBlocks()) {
			String previousPid = previousPidByCore.get(block.getCoreIndex());
			if (previousPid != null && !previousPid.equals(block.getPid())) {
				switchCount++;
			}
			previousPidByCore.put(block.getCoreIndex(), block.getPid());
		}

		return switchCount;
	}

	private int countCoreType(CoreType coreType) {
		Integer count = coreCountProvider.apply(coreType);
		return count == null ? 0 : count;
	}

	private String formatDecimal(double value) {
		return String.format("%.2f", value);
	}

	private String formatPercent(double value) {
		return String.format("%.1f%%", value * 100);
	}

	private String formatPower(double value) {
		return String.format("%.1fW", value);
	}
}
