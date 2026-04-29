package controller;

import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class GanttChartController {
	
	public void drawTest(AnchorPane ganttPane) {
        ganttPane.getChildren().clear();

        Rectangle rect = new Rectangle(50, 50, 200, 40);
        rect.setStyle("-fx-fill: #E0531F;");

        Text text = new Text(60, 75, "P1");
        text.setStyle("-fx-fill: white; -fx-font-size: 12px;");

        ganttPane.getChildren().addAll(rect, text);
    }
	
}
