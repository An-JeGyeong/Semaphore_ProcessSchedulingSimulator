package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			loadFonts();

			// FXML 파일을 로드해서 UI 생성
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/view/main.fxml")
			);
			
			Parent root = loader.load();
			
			// UI를 담을 Scene 생성
			Scene scene = new Scene(root);
			scene.setFill(Color.TRANSPARENT);
			
			// 기본 타이틀바 제거 (커스텀 타이틀바 사용을 위해)
			primaryStage.initStyle(StageStyle.TRANSPARENT);
			primaryStage.setScene(scene); // Scene을 창(Stage)에 연결
			primaryStage.show(); // 창을 화면에 표시
			
		} catch(Exception e) { 
			e.printStackTrace();
		}
	}

	// 해당 경로에 있는 폰트 파일 불러오기
	private void loadFonts() {
		loadFont("/view/fonts/Noto_Sans/NotoSans-Regular.ttf");
		loadFont("/view/fonts/Noto_Sans/NotoSans-Bold.ttf");
		loadFont("/view/fonts/Noto_Sans_KR/NotoSansKR-Regular.ttf");
		loadFont("/view/fonts/Noto_Sans_KR/NotoSansKR-Bold.ttf");
	}
	
	// 전달받은 경로의 .ttf 폰트 파일을 JavaFX에 등록
	private void loadFont(String resourcePath) {
		try (var stream = getClass().getResourceAsStream(resourcePath)) {
			if (stream != null) {
				// 폰트를 JavaFX에 등록
				Font.loadFont(stream, 12);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args); // 프로그램 실행 시작
	}
}
