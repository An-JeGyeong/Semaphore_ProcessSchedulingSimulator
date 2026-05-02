import java.util.List;
import model.AlgorithmType;
import model.CoreConfig;
import model.CoreType;
import model.Process;
import model.SchedulingResult;
import service.SchedulingService;

public class TestSchedulingRun {
    public static void main(String[] args) {
        SchedulingService service = new SchedulingService();
        List<CoreConfig> cores = List.of(new CoreConfig(1, CoreType.E_CORE), new CoreConfig(2, CoreType.P_CORE));
        for (AlgorithmType algorithm : List.of(AlgorithmType.FCFS, AlgorithmType.RR, AlgorithmType.SPN, AlgorithmType.SRTN, AlgorithmType.HRRN)) {
            List<Process> processes = List.of(
                new Process("P1", 0, 8),
                new Process("P2", 1, 4),
                new Process("P3", 2, 9),
                new Process("P4", 3, 5)
            );
            SchedulingResult result = service.run(algorithm, processes, cores, 2);
            int totalTime = result.getGanttBlocks().stream().mapToInt(block -> block.getEnd()).max().orElse(0);
            System.out.println(algorithm + " blocks=" + result.getGanttBlocks().size() + " total=" + totalTime);
        }
    }
}