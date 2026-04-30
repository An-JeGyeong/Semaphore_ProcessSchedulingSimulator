package controller;

import java.util.List;

import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import model.GanttBlock;
import model.SchedulingResult;

public class GanttChartController {

    private static final String FONT_FAMILY = "Noto Sans";
    private static final int DEFAULT_VISIBLE_TIME = 15;
    private static final double LEFT_AXIS_WIDTH = 130;
    private static final double TOP_AXIS_HEIGHT = 72;
    private static final double RIGHT_PADDING = 24;
    private static final double BOTTOM_PADDING = 64;
    private static final double TIME_SCALE = 52;
    private static final int MAX_CORE_COUNT = 4;
    private static final double ROW_HEIGHT = 56;
    private static final double BLOCK_HEIGHT = 24;
    private static final double DEFAULT_CHART_WIDTH =
            LEFT_AXIS_WIDTH + DEFAULT_VISIBLE_TIME * TIME_SCALE + RIGHT_PADDING;
    private static final double MIN_CHART_HEIGHT = 360;

    public void drawEmpty(AnchorPane ganttPane, List<String> coreLabels) {
        drawChart(null, ganttPane, coreLabels, MIN_CHART_HEIGHT);
    }

    public void drawEmpty(AnchorPane ganttPane, List<String> coreLabels, double viewportHeight) {
        drawChart(null, ganttPane, coreLabels, viewportHeight);
    }

    public void draw(SchedulingResult result, AnchorPane ganttPane, List<String> coreLabels) {
        drawChart(result, ganttPane, coreLabels, MIN_CHART_HEIGHT);
    }

    public void draw(SchedulingResult result, AnchorPane ganttPane, List<String> coreLabels, double viewportHeight) {
        drawChart(result, ganttPane, coreLabels, viewportHeight);
    }

    private void drawChart(SchedulingResult result, AnchorPane ganttPane, List<String> coreLabels, double viewportHeight) {
        ganttPane.getChildren().clear();

        int laneCount = Math.max(1, coreLabels.size());
        boolean hasResult = result != null && !result.getGanttBlocks().isEmpty();
        int maxTime = hasResult ? Math.max(DEFAULT_VISIBLE_TIME, findMaxTime(result)) : DEFAULT_VISIBLE_TIME;
        double chartWidth = hasResult
                ? LEFT_AXIS_WIDTH + maxTime * TIME_SCALE + RIGHT_PADDING
                : DEFAULT_CHART_WIDTH;
        double plotHeight = MAX_CORE_COUNT * ROW_HEIGHT;
        double chartHeight = Math.max(1, viewportHeight);
        double axisEndX = chartWidth - RIGHT_PADDING;
        double axisEndY = Math.min(chartHeight - 46, TOP_AXIS_HEIGHT + plotHeight + 28);

        ganttPane.setMinWidth(chartWidth);
        ganttPane.setPrefWidth(chartWidth);
        ganttPane.setMinHeight(0);
        ganttPane.setPrefHeight(chartHeight);
        ganttPane.setMaxHeight(chartHeight);

        drawAxes(ganttPane, maxTime, axisEndX, axisEndY);
        drawCoreLabels(ganttPane, coreLabels);

        if (result != null && !coreLabels.isEmpty()) {
            drawBlocks(ganttPane, result, coreLabels.size());
        }
    }

    private int findMaxTime(SchedulingResult result) {
        if (result == null) {
            return DEFAULT_VISIBLE_TIME;
        }

        return result.getGanttBlocks().stream()
                .mapToInt(GanttBlock::getEnd)
                .max()
                .orElse(DEFAULT_VISIBLE_TIME);
    }

    private void drawAxes(AnchorPane ganttPane, int maxTime, double axisEndX, double axisEndY) {
        for (int time = 0; time <= maxTime; time++) {
            double x = LEFT_AXIS_WIDTH + time * TIME_SCALE;

            Line gridLine = new Line(x, TOP_AXIS_HEIGHT + 34, x, axisEndY);
            gridLine.setStroke(Color.web("#303030"));
            gridLine.setStrokeWidth(1);

            Text tickText = new Text(String.valueOf(time));
            tickText.setFill(Color.web("#A6A6A6"));
            tickText.setFont(Font.font(FONT_FAMILY, 10));
            tickText.setX(x - tickText.getLayoutBounds().getWidth() / 2);
            tickText.setY(TOP_AXIS_HEIGHT + 22);

            ganttPane.getChildren().addAll(gridLine, tickText);
        }
    }

    private void drawCoreLabels(AnchorPane ganttPane, List<String> coreLabels) {
        for (int coreIndex = 0; coreIndex < coreLabels.size(); coreIndex++) {
            double y = getCoreCenterY(coreIndex, coreLabels.size());
            Text label = new Text(LEFT_AXIS_WIDTH - 86, y + 6, coreLabels.get(coreIndex));
            label.setFill(Color.web("#BDBDBD"));
            label.setFont(Font.font(FONT_FAMILY, 12));
            ganttPane.getChildren().add(label);
        }
    }

    private void drawBlocks(AnchorPane ganttPane, SchedulingResult result, int laneCount) {
        for (GanttBlock block : result.getGanttBlocks()) {
            int coreIndex = Math.min(block.getCoreIndex(), laneCount - 1);
            double x = LEFT_AXIS_WIDTH + block.getStart() * TIME_SCALE;
            double y = getCoreCenterY(coreIndex, laneCount) - BLOCK_HEIGHT / 2;
            double width = Math.max(1, (block.getEnd() - block.getStart()) * TIME_SCALE);

            Rectangle rect = new Rectangle(x, y, width, BLOCK_HEIGHT);
            rect.setFill(getColor(block.getPid()));

            Text label = new Text(x + 8, y + 20, block.getPid());
            label.setFill(Color.WHITE);
            label.setFont(Font.font(FONT_FAMILY, 11));
            label.setMouseTransparent(true);

            ganttPane.getChildren().addAll(rect, label);
        }
    }

    private double getCoreCenterY(int coreIndex, int laneCount) {
        double topLaneY = TOP_AXIS_HEIGHT + 48;
        double maxGroupCenterY = topLaneY + ((MAX_CORE_COUNT - 1) * ROW_HEIGHT) / 2;

        if (laneCount <= 1) {
            return maxGroupCenterY;
        }

        double groupHeight = (laneCount - 1) * ROW_HEIGHT;
        double firstLaneY = maxGroupCenterY - groupHeight / 2;
        return firstLaneY + coreIndex * ROW_HEIGHT;
    }

    private Color getColor(String pid) {
        return Color.web(ProcessColorPalette.getColor(pid));
    }
}
