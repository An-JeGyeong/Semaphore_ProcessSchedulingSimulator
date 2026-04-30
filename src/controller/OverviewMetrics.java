package controller;

record OverviewMetrics(
		String pCoreSummary,
		String eCoreSummary,
		String processCount,
		String totalTime,
		String throughput,
		String avgWaitingTime,
		String avgTurnaroundTime,
		String avgResponseTime,
		String cpuUtilization,
		String contextSwitches,
		String idleTime) {
}
