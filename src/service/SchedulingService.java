package service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.AlgorithmType;
import model.CoreConfig;
import model.CoreType;
import model.GanttBlock;
import model.Process;
import model.SchedulingResult;

public class SchedulingService {

    public SchedulingResult run(
            AlgorithmType algorithm,
            List<Process> processes,
            List<CoreConfig> cores,
            int timeQuantum) {
        if (algorithm == null) {
            throw new IllegalArgumentException("알고리즘을 선택하세요.");
        }
        if (processes == null || processes.isEmpty()) {
            throw new IllegalArgumentException("프로세스를 먼저 추가하세요.");
        }
        if (cores == null || cores.isEmpty()) {
            throw new IllegalArgumentException("하나 이상의 Core를 선택하세요.");
        }
        if (algorithm == AlgorithmType.RR) {
            validateTimeQuantum(timeQuantum);
        }

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

        applyMetrics(states);
        return new SchedulingResult(ganttBlocks);
    }

    public SchedulingResult run(AlgorithmType algorithm, List<Process> processes, int timeQuantum) {
        return run(algorithm, processes, List.of(new CoreConfig(1, CoreType.E_CORE)), timeQuantum);
    }

    private List<ProcessState> createStates(List<Process> processes) {
        List<ProcessState> states = new ArrayList<>();

        for (int i = 0; i < processes.size(); i++) {
            Process process = processes.get(i);
            process.reset();
            states.add(new ProcessState(process, i));
        }

        return states;
    }

    private List<CoreRuntime> createCoreRuntimes(List<CoreConfig> cores) {
        List<CoreRuntime> runtimes = new ArrayList<>();

        for (int i = 0; i < cores.size(); i++) {
            runtimes.add(new CoreRuntime(i, cores.get(i)));
        }

        return runtimes;
    }

    private void validateTimeQuantum(int timeQuantum) {
        if (timeQuantum <= 0) {
            throw new IllegalArgumentException("Time Quantum은 1 이상이어야 합니다.");
        }
    }

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

    private List<GanttBlock> runCustom(List<ProcessState> states, List<CoreRuntime> cores) {
        List<GanttBlock> gantt = new ArrayList<>();
        List<CoreRuntime> energyOrder = new ArrayList<>(cores);
        energyOrder.sort(Comparator
                .comparingDouble((CoreRuntime core) -> core.config.getCoreType().getPowerPerSecond())
                .thenComparingInt(core -> core.index));
        int time = 0;

        while (!allFinished(states)) {
            assignIdleCores(states, energyOrder, time, this::selectHrrn);

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

    private ProcessState selectFcfs(List<ProcessState> states, int time, Set<ProcessState> assigned) {
        return states.stream()
                .filter(state -> state.isReady(time) && !assigned.contains(state))
                .min(Comparator
                        .comparingInt(ProcessState::arrivalTime)
                        .thenComparingInt(ProcessState::inputOrder))
                .orElse(null);
    }

    private ProcessState selectSpn(List<ProcessState> states, int time, Set<ProcessState> assigned) {
        return states.stream()
                .filter(state -> state.isReady(time) && !assigned.contains(state))
                .min(Comparator
                        .comparingInt(ProcessState::burstTime)
                        .thenComparingInt(ProcessState::arrivalTime)
                        .thenComparingInt(ProcessState::inputOrder))
                .orElse(null);
    }

    private ProcessState selectHrrn(List<ProcessState> states, int time, Set<ProcessState> assigned) {
        return states.stream()
                .filter(state -> state.isReady(time) && !assigned.contains(state))
                .max(Comparator
                        .comparingDouble((ProcessState state) -> responseRatio(state, time))
                        .thenComparingInt(state -> -state.arrivalTime())
                        .thenComparingInt(state -> -state.inputOrder()))
                .orElse(null);
    }

    private double responseRatio(ProcessState state, int time) {
        int waitingTime = Math.max(0, time - state.arrivalTime() - state.executedSeconds);
        return (waitingTime + state.burstTime()) / (double) state.burstTime();
    }

    private List<ProcessState> readyStates(List<ProcessState> states, int time) {
        return states.stream()
                .filter(state -> state.isReady(time))
                .toList();
    }

    private List<ProcessState> sortedByArrival(List<ProcessState> states) {
        List<ProcessState> sorted = new ArrayList<>(states);
        sorted.sort(Comparator
                .comparingInt(ProcessState::arrivalTime)
                .thenComparingInt(ProcessState::inputOrder));
        return sorted;
    }

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

    private int nextArrivalTime(List<ProcessState> states, int time) {
        return states.stream()
                .filter(state -> !state.isFinished() && state.arrivalTime() > time)
                .mapToInt(ProcessState::arrivalTime)
                .min()
                .orElse(time + 1);
    }

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

    private void applyMetrics(List<ProcessState> states) {
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

    @FunctionalInterface
    private interface Selector {
        ProcessState select(List<ProcessState> states, int time, Set<ProcessState> assigned);
    }

    private static class CoreRuntime {
        private final int index;
        private final CoreConfig config;
        private ProcessState current;
        private int quantumUsed;

        private CoreRuntime(int index, CoreConfig config) {
            this.index = index;
            this.config = config;
        }

        private void assign(ProcessState state) {
            current = state;
            quantumUsed = 0;
        }

        private void clear() {
            current = null;
            quantumUsed = 0;
        }
    }

    private static class ProcessState {
        private final Process process;
        private final int inputOrder;
        private int remainingWork;
        private int executedSeconds;
        private int finishTime;

        private ProcessState(Process process, int inputOrder) {
            this.process = process;
            this.inputOrder = inputOrder;
            this.remainingWork = process.getBurstTime();
        }

        private String pid() {
            return process.getPid();
        }

        private int arrivalTime() {
            return process.getArrivalTime();
        }

        private int burstTime() {
            return process.getBurstTime();
        }

        private int remainingWork() {
            return remainingWork;
        }

        private int inputOrder() {
            return inputOrder;
        }

        private boolean isReady(int time) {
            return !isFinished() && arrivalTime() <= time;
        }

        private boolean isFinished() {
            return remainingWork <= 0;
        }
    }
}
