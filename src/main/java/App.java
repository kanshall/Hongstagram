
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import java.io.IOException;

public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);

        // 테마 매니저가 설정 알아서 로딩함

        Login loginView = new Login();
        scene = new Scene(loginView.getView(), 450, 800);

        // 처음에 테마 적용
        ThemeManager.getInstance().applyTheme(scene);

        stage.setTitle("Hongstagram");
        stage.setScene(scene);
        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    // 화면 전환을 위한 헬퍼
    public static void setRoot(Parent root) {
        scene.setRoot(root);
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen();
        // 혹시 몰라서 테마 다시 적용 (원래 유지되긴 함)
        ThemeManager.getInstance().applyTheme(scene);
    }

    // 메인 진입점
    public static void main(String[] args) {
        launch();
    }

    // --- 창 드래그 로직 ---
    private static double xOffset = 0;
    private static double yOffset = 0;

    public static void makeDraggable(javafx.stage.Stage stage, javafx.scene.Node node) {
        node.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        node.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    // --- 커스텀 타이틀바 만드는 공장 ---
    public static javafx.scene.layout.HBox createTitleBar(javafx.stage.Stage stage, String title) {
        javafx.scene.layout.HBox titleBar = new javafx.scene.layout.HBox();
        titleBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        titleBar.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
        titleBar.getStyleClass().add("title-bar");

        // 아이콘/텍스트 (왼쪽)
        javafx.scene.control.Label titleLbl = new javafx.scene.control.Label(title);
        titleLbl.setStyle("-fx-font-weight:bold; -fx-text-fill: -fx-text-base-color;");
        javafx.scene.layout.HBox.setHgrow(titleLbl, javafx.scene.layout.Priority.ALWAYS);
        titleBar.getChildren().add(titleLbl);

        // 최소화
        javafx.scene.control.Button minBtn = new javafx.scene.control.Button("—");
        minBtn.getStyleClass().add("caption-btn");
        minBtn.setOnAction(e -> stage.setIconified(true));

        // 닫기
        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("✕");
        closeBtn.getStyleClass().add("caption-btn");
        closeBtn.getStyleClass().add("close-btn");
        closeBtn.setOnAction(e -> {
            stage.close();
            // 메인 창이면 앱 종료
            if (stage == primaryStage) {
                System.exit(0);
            }
        });

        titleBar.getChildren().addAll(minBtn, closeBtn);

        // 드래그 가능하게 설정
        makeDraggable(stage, titleBar);

        return titleBar;
    }
}
