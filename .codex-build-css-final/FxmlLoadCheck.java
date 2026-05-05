import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

public class FxmlLoadCheck {
    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.startup(() -> {
            try {
                URL url = FxmlLoadCheck.class.getResource("/view/main.fxml");
                if (url == null) {
                    throw new IllegalStateException("main.fxml not found");
                }
                Object root = FXMLLoader.load(url);
                System.out.println("FXML loaded: " + root.getClass().getSimpleName());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
                Platform.exit();
            }
        });
        latch.await();
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
    }
}
