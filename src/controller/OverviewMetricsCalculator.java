package controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.CoreType;
import model.GanttBlock;
import model.Process;
import model.SchedulingResult;

final class OverviewMetricsCalculator {

	private final List<Process> processList;
	private final CoreSelectionController coreSelectionController;

	OverviewMetricsCalculator(List<Process> processList, CoreSelectionController coreSelectionController) {
		this.processList = processList;
		this.coreSelectionController = coreSelectionController;
	}

	OverviewMetrics calculate(SchedulingResult result, int totalTime) {
		List<String> coreLabels = coreSelectionController.getSelectedCoreLabels();

		return new OverviewMetrics(
				formatCoreSummary(result, coreLabels, "P-Core", totalTime),
				formatCoreSummary(result, coreLabels, "E-Core", totalTime),
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

	private String formatCoreSummary(SchedulingResult result, List<String> coreLabels, String coreType, int totalTime) {
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
			String coreType,
			int totalTime) {
		int coreCount = coreSelectionController.countSelectedCoreType(coreType);
		if (result == null || totalTime <= 0 || coreLabels.isEmpty() || coreCount == 0) {
			return 0.0;
		}

		int busyTime = calculateBusyTimeForCoreType(result, coreLabels, coreType);
		return (double) busyTime / (totalTime * coreCount);
	}

	private double calculateCorePower(SchedulingResult result, List<String> coreLabels, String coreType) {
		if (coreSelectionController.countSelectedCoreType(coreType) == 0) {
			return 0.0;
		}

		CoreType type = toCoreType(coreType);
		int busyTime = calculateBusyTimeForCoreType(result, coreLabels, coreType);
		int startupEvents = countStartupEventsForType(result, coreLabels, coreType);
		return startupEvents * type.getStartupPower() + busyTime * type.getPowerPerSecond();
	}

	private int calculateBusyTimeForCoreType(SchedulingResult result, List<String> coreLabels, String coreType) {
		if (result == null || coreLabels.isEmpty()) {
			return 0;
		}

		int busyTime = 0;
		for (GanttBlock block : result.getGanttBlocks()) {
			int laneIndex = block.getCoreIndex();
			if (laneIndex >= 0 && laneIndex < coreLabels.size() && coreLabels.get(laneIndex).startsWith(coreType)) {
				busyTime += block.getEnd() - block.getStart();
			}
		}
		return busyTime;
	}

	private int countStartupEventsForType(SchedulingResult result, List<String> coreLabels, String coreType) {
		if (result == null || coreLabels.isEmpty()) {
			return 0;
		}

		Map<Integer, Integer> lastEndByCore = new HashMap<>();
		int startupEvents = 0;

		for (GanttBlock block : result.getGanttBlocks()) {
			int laneIndex = block.getCoreIndex();
			if (laneIndex >= 0 && laneIndex < coreLabels.size() && coreLabels.get(laneIndex).startsWith(coreType)) {
				Integer lastEnd = lastEndByCore.get(laneIndex);
				if (lastEnd == null || lastEnd < block.getStart()) {
					startupEvents++;
				}
				lastEndByCore.put(laneIndex, block.getEnd());
			}
		}

		return startupEvents;
	}

	private CoreType toCoreType(String coreType) {
		return "P-Core".equals(coreType) ? CoreType.P_CORE : CoreType.E_CORE;
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
