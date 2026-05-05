package application;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	// JavaFX 애플리케이션을 시작하고 main.fxml 기반의 메인 창을 띄운다.
	@Override
	public void start(Stage primaryStage) {
		try {
			loadFonts();

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main.fxml"));
			Parent root = loader.load();
			applyWindowClip(root);

			Scene scene = new Scene(root);
			scene.setFill(Color.TRANSPARENT);

			primaryStage.initStyle(StageStyle.TRANSPARENT);
			primaryStage.setScene(scene);
			primaryStage.show();

			fitStageToScreen(primaryStage);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to start application.", e);
		}
	}


	// 사용 가능한 화면 크기에 맞춰 초기 창 크기를 제한한다.
	private void fitStageToScreen(Stage stage) {
		var bounds = Screen.getPrimary().getVisualBounds();

		if (stage.getWidth() > bounds.getWidth()) {
			stage.setWidth(bounds.getWidth());
		}
		if (stage.getHeight() > bounds.getHeight()) {
			stage.setHeight(bounds.getHeight());
		}
		if (stage.getX() < bounds.getMinX() || stage.getX() + stage.getWidth() > bounds.getMaxX()) {
			stage.setX(bounds.getMinX() + Math.max(0, (bounds.getWidth() - stage.getWidth()) / 2));
		}
		if (stage.getY() < bounds.getMinY() || stage.getY() + stage.getHeight() > bounds.getMaxY()) {
			stage.setY(bounds.getMinY() + Math.max(0, (bounds.getHeight() - stage.getHeight()) / 2));
		}
	}

	// 커스텀 undecorated 창의 모서리에 라운드 클립을 적용한다.
	private void applyWindowClip(Parent root) {
		Rectangle clip = new Rectangle();
		clip.setArcWidth(10);
		clip.setArcHeight(10);
		clip.widthProperty().bind(root.layoutBoundsProperty().map(bounds -> bounds.getWidth()));
		clip.heightProperty().bind(root.layoutBoundsProperty().map(bounds -> bounds.getHeight()));
		root.setClip(clip);
	}


	// 화면 전체에서 사용하는 Noto Sans 폰트를 미리 로드한다.
	private void loadFonts() {
		loadFont("/view/fonts/Noto_Sans/NotoSans-Regular.ttf");
		loadFont("/view/fonts/Noto_Sans/NotoSans-Bold.ttf");
		loadFont("/view/fonts/Noto_Sans_KR/NotoSansKR-Regular.ttf");
		loadFont("/view/fonts/Noto_Sans_KR/NotoSansKR-Bold.ttf");
	}

	private void loadFont(String resourcePath) {
		try (var stream = getClass().getResourceAsStream(resourcePath)) {
			if (stream != null) {
				Font.loadFont(stream, 12);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to load font: " + resourcePath, e);
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
