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
import util.ProcessColorPalette;

public class GanttChartController {

	private static final String FONT_FAMILY = "Noto Sans";
	private static final int DEFAULT_VISIBLE_TIME = 15;
	private static final double LEFT_AXIS_WIDTH = 130;
	private static final double MIN_TOP_AXIS_HEIGHT = 72;
	private static final double RIGHT_PADDING = 24;
	private static final double TIME_SCALE = 52;
	private static final int MAX_CORE_COUNT = 4;
	private static final double ROW_HEIGHT = 56;
	private static final double BLOCK_HEIGHT = 24;
	private static final double DEFAULT_CHART_WIDTH =
			LEFT_AXIS_WIDTH + DEFAULT_VISIBLE_TIME * TIME_SCALE + RIGHT_PADDING;
	private static final double MIN_CHART_HEIGHT = 360;

	// 결과가 없을 때 선택된 Core 라벨과 기본 시간 축만 그린다.
	public void drawEmpty(AnchorPane ganttPane, List<String> coreLabels) {
		drawChart(null, ganttPane, coreLabels, MIN_CHART_HEIGHT);
	}

	// ScrollPane viewport 높이를 반영해 빈 Gantt 프레임을 다시 그린다.
	public void drawEmpty(AnchorPane ganttPane, List<String> coreLabels, double viewportHeight) {
		drawChart(null, ganttPane, coreLabels, viewportHeight);
	}

	// 스케줄링 결과를 받아 시간 축, Core 라벨, 실행 블록을 함께 그린다.
	public void draw(SchedulingResult result, AnchorPane ganttPane, List<String> coreLabels) {
		drawChart(result, ganttPane, coreLabels, MIN_CHART_HEIGHT);
	}

	// 실제 결과가 있을 때 viewport 높이에 맞춰 축과 실행 블록을 함께 그린다.
	public void draw(SchedulingResult result, AnchorPane ganttPane, List<String> coreLabels, double viewportHeight) {
		drawChart(result, ganttPane, coreLabels, viewportHeight);
	}

	// Gantt Pane 크기를 계산한 뒤 실제 차트 구성 요소를 순서대로 렌더링한다.
	private void drawChart(SchedulingResult result, AnchorPane ganttPane, List<String> coreLabels, double viewportHeight) {
		ganttPane.getChildren().clear();

		boolean hasResult = result != null && !result.getGanttBlocks().isEmpty();
		int maxTime = hasResult ? Math.max(DEFAULT_VISIBLE_TIME, findMaxTime(result)) : DEFAULT_VISIBLE_TIME;
		double chartWidth = hasResult
				? LEFT_AXIS_WIDTH + maxTime * TIME_SCALE + RIGHT_PADDING
				: DEFAULT_CHART_WIDTH;
		double chartHeight = Math.max(1, viewportHeight);
		double topAxisHeight = getTopAxisHeight(chartHeight);
		double axisEndY = Math.min(chartHeight - 46, topAxisHeight + MAX_CORE_COUNT * ROW_HEIGHT + 28);

		ganttPane.setMinWidth(chartWidth);
		ganttPane.setPrefWidth(chartWidth);
		ganttPane.setMinHeight(0);
		ganttPane.setPrefHeight(chartHeight);
		ganttPane.setMaxHeight(chartHeight);

		drawAxes(ganttPane, maxTime, axisEndY, topAxisHeight);
		drawCoreLabels(ganttPane, coreLabels, topAxisHeight);

		if (result != null && !coreLabels.isEmpty()) {
			drawBlocks(ganttPane, result, coreLabels.size(), topAxisHeight);
		}
	}

	// 최대화처럼 세로 공간이 늘어날 때 축이 너무 위에 붙지 않도록 상단 여백을 계산한다.
	private double getTopAxisHeight(double chartHeight) {
		double extraHeight = Math.max(0, chartHeight - MIN_CHART_HEIGHT);
		return MIN_TOP_AXIS_HEIGHT + Math.min(90, extraHeight * 0.28);
	}

	// 모든 GanttBlock의 종료 시간 중 가장 큰 값을 찾아 시간 축의 끝을 정한다.
	private int findMaxTime(SchedulingResult result) {
		return result.getGanttBlocks().stream()
				.mapToInt(GanttBlock::getEnd)
				.max()
				.orElse(DEFAULT_VISIBLE_TIME);
	}


	// 시간 숫자와 세로 경계선을 그려 실행 위치를 읽을 수 있게 한다.
	private void drawAxes(AnchorPane ganttPane, int maxTime, double axisEndY, double topAxisHeight) {
		for (int time = 0; time <= maxTime; time++) {
			double x = LEFT_AXIS_WIDTH + time * TIME_SCALE;

			Line gridLine = new Line(x, topAxisHeight + 34, x, axisEndY);
			gridLine.setStroke(Color.web("#303030"));
			gridLine.setStrokeWidth(1);

			Text tickText = new Text(String.valueOf(time));
			tickText.setFill(Color.web("#A6A6A6"));
			tickText.setFont(Font.font(FONT_FAMILY, 10));
			tickText.setX(x - tickText.getLayoutBounds().getWidth() / 2);
			tickText.setY(topAxisHeight + 22);

			ganttPane.getChildren().addAll(gridLine, tickText);
		}
	}

	// 선택된 Core 개수에 맞춰 Y축 라벨 간격을 배치한다.
	private void drawCoreLabels(AnchorPane ganttPane, List<String> coreLabels, double topAxisHeight) {
		for (int coreIndex = 0; coreIndex < coreLabels.size(); coreIndex++) {
			double y = getCoreCenterY(coreIndex, coreLabels.size(), topAxisHeight);
			Text label = new Text(LEFT_AXIS_WIDTH - 86, y + 6, coreLabels.get(coreIndex));
			label.setFill(Color.web("#BDBDBD"));
			label.setFont(Font.font(FONT_FAMILY, 12));
			ganttPane.getChildren().add(label);
		}
	}

	// 각 GanttBlock을 프로세스 색상과 시간 길이에 맞는 사각형으로 그린다.
	private void drawBlocks(AnchorPane ganttPane, SchedulingResult result, int laneCount, double topAxisHeight) {
		for (GanttBlock block : result.getGanttBlocks()) {
			int coreIndex = Math.min(block.getCoreIndex(), laneCount - 1);
			double x = LEFT_AXIS_WIDTH + block.getStart() * TIME_SCALE;
			double y = getCoreCenterY(coreIndex, laneCount, topAxisHeight) - BLOCK_HEIGHT / 2;
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

	// 선택된 Core 개수와 관계없이 전체 4개 Core 기준 영역 안에서 행을 균형 있게 배치한다.
	private double getCoreCenterY(int coreIndex, int laneCount, double topAxisHeight) {
		double topLaneY = topAxisHeight + 48;
		double maxGroupCenterY = topLaneY + ((MAX_CORE_COUNT - 1) * ROW_HEIGHT) / 2;

		if (laneCount <= 1) {
			return maxGroupCenterY;
		}

		double groupHeight = (laneCount - 1) * ROW_HEIGHT;
		double firstLaneY = maxGroupCenterY - groupHeight / 2;
		return firstLaneY + coreIndex * ROW_HEIGHT;
	}

	// 프로세스 번호에 고정 색상 팔레트를 적용해 표와 Gantt 색상을 일치시킨다.
	private Color getColor(String pid) {
		return Color.web(ProcessColorPalette.getColor(pid));
	}
}
