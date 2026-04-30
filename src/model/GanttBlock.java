package model;

public class GanttBlock {

    private final String pid;
    private final int coreIndex;
    private final int start;
    private final int end;

    public GanttBlock(String pid, int start, int end) {
        this(pid, 0, start, end);
    }

    public GanttBlock(String pid, int coreIndex, int start, int end) {
        this.pid = pid;
        this.coreIndex = coreIndex;
        this.start = start;
        this.end = end;
    }

    public String getPid() {
        return pid;
    }

    public int getCoreIndex() {
        return coreIndex;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
