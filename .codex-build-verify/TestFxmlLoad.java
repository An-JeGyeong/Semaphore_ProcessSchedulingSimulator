import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class TestFxmlLoad {
    public static void main(String[] args) throws Exception {
        Platform.startup(() -> {
            try {
                Parent root = FXMLLoader.load(TestFxmlLoad.class.getResource("/view/main.fxml"));
                System.out.println("FXML loaded: " + root.getClass().getSimpleName());
                Platform.exit();
            } catch (Throwable e) {
                e.printStackTrace();
                Platform.exit();
                System.exit(1);
            }
        });
    }
}