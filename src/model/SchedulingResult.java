package model;

import java.util.List;

public class SchedulingResult {

    private final List<GanttBlock> ganttBlocks;

    public SchedulingResult(List<GanttBlock> ganttBlocks) {
        this.ganttBlocks = List.copyOf(ganttBlocks);
    }

    public List<GanttBlock> getGanttBlocks() {
        return ganttBlocks;
    }
}
