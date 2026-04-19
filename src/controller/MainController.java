package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MainController {

	@FXML
    private Pane titleBar;
	
	@FXML
	private Button minBtn;
	
	@FXML
	private Button maxBtn;
	
	@FXML
	private Button closeBtn;

    private double xOffset;
    private double yOffset;
	
	public void setStage(Stage primaryStage) {
		// TODO Auto-generated method stub
		
	}
	
    @FXML
    public void initialize() {
    	
    	// 창 클릭 시, 클릭 위치 저장
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        // 창 드래그 이동
        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        
        // 최소화
        minBtn.setOnAction(event -> {
            Stage stage = (Stage) minBtn.getScene().getWindow();
            stage.setIconified(true);
        });
        
        // 최대화, 복원 버튼
        maxBtn.setOnAction(event -> {
            Stage stage = (Stage) maxBtn.getScene().getWindow();
            stage.setMaximized(!stage.isMaximized());
        });
        
        // 닫기
        closeBtn.setOnAction(event -> {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        });
        
        // 타이틀바 더블클릭 시 최대화, 복원
        titleBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Stage stage = (Stage) titleBar.getScene().getWindow();
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }
    
}