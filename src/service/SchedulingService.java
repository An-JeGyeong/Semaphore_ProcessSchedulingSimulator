package service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import model.AlgorithmType;
import model.CoreConfig;
import model.CoreType;
import model.GanttBlock;
import model.Process;
import model.SchedulingResult;

public class SchedulingService {

	// 스케줄링 서비스의 진입점이다. 입력 검증 후 알고리즘별 실행 메서드로 분기하고,
	// 마지막에 Process 모델의 WT/TT/NTT를 한 번에 반영한다.
	public SchedulingResult run(
			AlgorithmType algorithm,
			List<Process> processes,
			List<CoreConfig> cores,
			int timeQuantum) {
		SchedulingValidator.validate(algorithm, processes, cores, timeQuantum);

		List<ProcessState> states = createStates(processes);
		List<CoreRuntime> runtimes = createCoreRuntimes(cores);
		List<GanttBlock> ganttBlocks = switch (algorithm) {
			case FCFS -> runNonPreemptive(states, runtimes, this::selectFcfs);
			case RR -> runRoundRobin(states, runtimes, timeQuantum);
			case SPN -> runNonPreemptive(states, runtimes, this::selectSpn);
			case SRTN -> runSrtn(states, runtimes);
			case HRRN -> runNonPreemptive(states, runtimes, this::selectHrrn);
			case CUSTOM -> runCustom(states, runtimes);
		};

		SchedulingMetrics.apply(states);
		return new SchedulingResult(ganttBlocks);
	}

	// Core 구성이 없던 기존 호출을 위해 E-Core 1개 기준으로 실행한다.
	public SchedulingResult run(AlgorithmType algorithm, List<Process> processes, int timeQuantum) {
		return run(algorithm, processes, List.of(new CoreConfig(1, CoreType.E_CORE)), timeQuantum);
	}

	// Process 모델은 화면에 표시되는 입력/결과 값을 담고 있으므로,
	// 실행 중 remainingWork, finishTime 같은 임시 값은 ProcessState에 따로 둔다.
	private List<ProcessState> createStates(List<Process> processes) {
		List<ProcessState> states = new ArrayList<>();

		for (int i = 0; i < processes.size(); i++) {
			Process process = processes.get(i);
			process.reset();
			states.add(new ProcessState(process, i));
		}

		return states;
	}

	// 선택된 Core 설정을 실제 실행 슬롯으로 변환한다.
	private List<CoreRuntime> createCoreRuntimes(List<CoreConfig> cores) {
		List<CoreRuntime> runtimes = new ArrayList<>();

		for (int i = 0; i < cores.size(); i++) {
			runtimes.add(new CoreRuntime(i, cores.get(i)));
		}

		return runtimes;
	}

	// FCFS, SPN, HRRN 공통 루프다. selector만 바꿔서 "어떤 프로세스를 고르는지"를 분리하고,
	// 선택된 프로세스는 완료될 때까지 같은 Core에서 계속 실행한다.
	private List<GanttBlock> runNonPreemptive(
			List<ProcessState> states,
			List<CoreRuntime> cores,
			Selector selector) {
		List<GanttBlock> gantt = new ArrayList<>();
		int time = 0;

		while (!allFinished(states)) {
			assignIdleCores(states, cores, time, selector);

			if (allCoresIdle(cores)) {
				time = nextArrivalTime(states, time);
				continue;
			}

			executeOneTick(cores, gantt, time);
			releaseFinishedCores(cores, time + 1, false, null);
			time++;
		}

		return gantt;
	}

	// RR은 도착한 프로세스를 readyQueue에 넣고, Core가 비면 큐 앞에서 꺼낸다.
	// quantumUsed가 Time Quantum에 도달하면 끝나지 않은 프로세스를 큐 뒤로 보낸다.
	private List<GanttBlock> runRoundRobin(List<ProcessState> states, List<CoreRuntime> cores, int timeQuantum) {
		List<GanttBlock> gantt = new ArrayList<>();
		List<ProcessState> arrivalOrder = sortedByArrival(states);
		ArrayDeque<ProcessState> readyQueue = new ArrayDeque<>();
		int nextArrivalIndex = 0;
		int time = 0;

		while (!allFinished(states)) {
			nextArrivalIndex = addArrivals(arrivalOrder, readyQueue, nextArrivalIndex, time);
			assignRoundRobin(cores, readyQueue);

			if (allCoresIdle(cores) && readyQueue.isEmpty()) {
				time = nextArrivalTime(states, time);
				continue;
			}

			executeOneTick(cores, gantt, time);
			releaseFinishedCores(cores, time + 1, true, readyQueue);

			for (CoreRuntime core : cores) {
				if (core.current != null && core.quantumUsed >= timeQuantum) {
					readyQueue.addLast(core.current);
					core.clear();
				}
			}

			time++;
		}

		return gantt;
	}

	// SRTN은 선점형 알고리즘이므로 매 tick마다 모든 Core 배정을 비우고
	// 현재 준비된 프로세스 중 remainingWork가 가장 짧은 것부터 다시 배정한다.
	private List<GanttBlock> runSrtn(List<ProcessState> states, List<CoreRuntime> cores) {
		List<GanttBlock> gantt = new ArrayList<>();
		int time = 0;

		while (!allFinished(states)) {
			List<ProcessState> ready = readyStates(states, time);
			if (ready.isEmpty()) {
				time = nextArrivalTime(states, time);
				continue;
			}

			ready.sort(Comparator
					.comparingInt(ProcessState::remainingWork)
					.thenComparingInt(ProcessState::arrivalTime)
					.thenComparingInt(ProcessState::inputOrder));
			clearCores(cores);

			int assignCount = Math.min(cores.size(), ready.size());
			for (int i = 0; i < assignCount; i++) {
				cores.get(i).assign(ready.get(i));
			}

			executeOneTick(cores, gantt, time);
			releaseFinishedCores(cores, time + 1, false, null);
			time++;
		}

		return gantt;
	}

	// CUSTOM은 Tom & Jerry 창을 띄우지 않는 계산용 fallback이다.
	// 실제 시각화 알고리즘은 TomJerrySimulationController와 TomJerrySchedulingService가 담당한다.
	private List<GanttBlock> runCustom(List<ProcessState> states, List<CoreRuntime> cores) {
		List<GanttBlock> gantt = new ArrayList<>();
		Random random = new Random();
		int time = 0;

		while (!allFinished(states)) {
			List<ProcessState> ready = readyStates(states, time);
			Set<ProcessState> assigned = assignedStates(cores);

			for (CoreRuntime core : cores) {
				if (core.current != null && random.nextDouble() < 0.08) {
					core.clear();
				}

				if (core.current != null || ready.isEmpty() || random.nextDouble() < 0.12) {
					continue;
				}

				List<ProcessState> candidates = ready.stream()
						.filter(state -> !assigned.contains(state))
						.toList();
				if (candidates.isEmpty()) {
					continue;
				}

				ProcessState selected = candidates.get(random.nextInt(candidates.size()));
				core.assign(selected);
				assigned.add(selected);
			}

			if (allCoresIdle(cores)) {
				time = ready.isEmpty() ? nextArrivalTime(states, time) : time + 1;
				continue;
			}

			executeOneTick(cores, gantt, time);
			releaseFinishedCores(cores, time + 1, false, null);
			time++;
		}

		return gantt;
	}

	// 비어 있는 Core에 현재 알고리즘의 선택 기준으로 준비된 프로세스를 배정한다.
	private void assignIdleCores(List<ProcessState> states, List<CoreRuntime> cores, int time, Selector selector) {
		Set<ProcessState> assigned = assignedStates(cores);

		for (CoreRuntime core : cores) {
			if (core.current != null) {
				continue;
			}

			ProcessState selected = selector.select(states, time, assigned);
			if (selected != null) {
				core.assign(selected);
				assigned.add(selected);
			}
		}
	}

	// Round Robin 준비 큐에서 아직 다른 Core에 배정되지 않은 프로세스를 꺼내 배정한다.
	private void assignRoundRobin(List<CoreRuntime> cores, ArrayDeque<ProcessState> readyQueue) {
		Set<ProcessState> assigned = assignedStates(cores);

		for (CoreRuntime core : cores) {
			if (core.current != null) {
				continue;
			}

			ProcessState selected = pollUnassigned(readyQueue, assigned);
			if (selected != null) {
				core.assign(selected);
				assigned.add(selected);
			}
		}
	}

	private ProcessState pollUnassigned(ArrayDeque<ProcessState> readyQueue, Set<ProcessState> assigned) {
		int size = readyQueue.size();

		for (int i = 0; i < size; i++) {
			ProcessState state = readyQueue.removeFirst();
			if (!state.isFinished() && !assigned.contains(state)) {
				return state;
			}
			if (!state.isFinished()) {
				readyQueue.addLast(state);
			}
		}

		return null;
	}

	// 과제 조건상 스케줄링은 1초 단위로만 진행된다.
	// P-Core가 남은 작업량보다 성능이 커도 1초를 소비하므로 Gantt 블록은 항상 time~time+1로 기록한다.
	private void executeOneTick(List<CoreRuntime> cores, List<GanttBlock> gantt, int time) {
		for (CoreRuntime core : cores) {
			if (core.current == null) {
				continue;
			}

			ProcessState state = core.current;
			int workDone = Math.min(core.config.getCoreType().getPerformancePerSecond(), state.remainingWork);
			state.remainingWork -= workDone;
			state.executedSeconds++;
			core.quantumUsed++;
			addBlock(gantt, state.pid(), core.index, time, time + 1);
		}
	}

	// 1초 실행 후 완료된 프로세스는 finishTime을 기록하고 Core를 비운다.
	// RR에서만 끝나지 않은 프로세스를 다시 큐로 돌려보낼 수 있다.
	private void releaseFinishedCores(
			List<CoreRuntime> cores,
			int finishTime,
			boolean requeueUnfinished,
			ArrayDeque<ProcessState> readyQueue) {
		for (CoreRuntime core : cores) {
			if (core.current == null) {
				continue;
			}

			if (core.current.isFinished()) {
				core.current.finishTime = finishTime;
				core.clear();
			} else if (requeueUnfinished && core.quantumUsed <= 0 && readyQueue != null) {
				readyQueue.addLast(core.current);
				core.clear();
			}
		}
	}

	// 도착 시간이 가장 빠른 준비 프로세스를 선택한다.
	private ProcessState selectFcfs(List<ProcessState> states, int time, Set<ProcessState> assigned) {
		return states.stream()
				.filter(state -> state.isReady(time) && !assigned.contains(state))
				.min(Comparator
						.comparingInt(ProcessState::arrivalTime)
						.thenComparingInt(ProcessState::inputOrder))
				.orElse(null);
	}

	// Burst Time이 가장 짧은 준비 프로세스를 선택한다.
	private ProcessState selectSpn(List<ProcessState> states, int time, Set<ProcessState> assigned) {
		return states.stream()
				.filter(state -> state.isReady(time) && !assigned.contains(state))
				.min(Comparator
						.comparingInt(ProcessState::burstTime)
						.thenComparingInt(ProcessState::arrivalTime)
						.thenComparingInt(ProcessState::inputOrder))
				.orElse(null);
	}

	// 응답률이 가장 높은 준비 프로세스를 선택한다.
	private ProcessState selectHrrn(List<ProcessState> states, int time, Set<ProcessState> assigned) {
		return states.stream()
				.filter(state -> state.isReady(time) && !assigned.contains(state))
				.max(Comparator
						.comparingDouble((ProcessState state) -> responseRatio(state, time))
						.thenComparingInt(state -> -state.arrivalTime())
						.thenComparingInt(state -> -state.inputOrder()))
				.orElse(null);
	}

	// HRRN에서 사용하는 응답률을 계산한다.
	private double responseRatio(ProcessState state, int time) {
		int waitingTime = Math.max(0, time - state.arrivalTime() - state.executedSeconds);
		return (waitingTime + state.burstTime()) / (double) state.burstTime();
	}

	// 현재 시간에 도착했고 아직 끝나지 않은 프로세스 목록을 만든다.
	private List<ProcessState> readyStates(List<ProcessState> states, int time) {
		return new ArrayList<>(states.stream()
				.filter(state -> state.isReady(time))
				.toList());
	}

	private List<ProcessState> sortedByArrival(List<ProcessState> states) {
		List<ProcessState> sorted = new ArrayList<>(states);
		sorted.sort(Comparator
				.comparingInt(ProcessState::arrivalTime)
				.thenComparingInt(ProcessState::inputOrder));
		return sorted;
	}

	// 현재 시간까지 도착한 프로세스를 RR 준비 큐에 추가한다.
	private int addArrivals(
			List<ProcessState> arrivalOrder,
			ArrayDeque<ProcessState> readyQueue,
			int nextArrivalIndex,
			int time) {
		int index = nextArrivalIndex;

		while (index < arrivalOrder.size() && arrivalOrder.get(index).arrivalTime() <= time) {
			ProcessState state = arrivalOrder.get(index);
			if (!state.isFinished()) {
				readyQueue.addLast(state);
			}
			index++;
		}

		return index;
	}

	private Set<ProcessState> assignedStates(List<CoreRuntime> cores) {
		Set<ProcessState> assigned = new HashSet<>();

		for (CoreRuntime core : cores) {
			if (core.current != null) {
				assigned.add(core.current);
			}
		}

		return assigned;
	}

	private boolean allFinished(List<ProcessState> states) {
		return states.stream().allMatch(ProcessState::isFinished);
	}

	private boolean allCoresIdle(List<CoreRuntime> cores) {
		return cores.stream().allMatch(core -> core.current == null);
	}

	private void clearCores(List<CoreRuntime> cores) {
		for (CoreRuntime core : cores) {
			core.clear();
		}
	}

	// 실행할 프로세스가 없을 때 다음 도착 시간으로 시뮬레이션 시간을 점프한다.
	private int nextArrivalTime(List<ProcessState> states, int time) {
		return states.stream()
				.filter(state -> !state.isFinished() && state.arrivalTime() > time)
				.mapToInt(ProcessState::arrivalTime)
				.min()
				.orElse(time + 1);
	}

	// 같은 Core에서 같은 프로세스가 연속 실행되면 Gantt 블록을 합친다.
	// 화면에는 불필요하게 1초짜리 조각이 여러 개 보이지 않게 하기 위한 처리다.
	private void addBlock(List<GanttBlock> gantt, String pid, int coreIndex, int start, int end) {
		if (start == end) {
			return;
		}

		if (!gantt.isEmpty()) {
			GanttBlock last = gantt.get(gantt.size() - 1);
			if (last.getPid().equals(pid) && last.getCoreIndex() == coreIndex && last.getEnd() == start) {
				gantt.set(gantt.size() - 1, new GanttBlock(pid, coreIndex, last.getStart(), end));
				return;
			}
		}

		gantt.add(new GanttBlock(pid, coreIndex, start, end));
	}

	@FunctionalInterface
	private interface Selector {
		ProcessState select(List<ProcessState> states, int time, Set<ProcessState> assigned);
	}
}
