import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class ChatFrame {

    private String targetId;
    private MainFrame mainFrame;
    private Stage stage;
    private TextArea textArea;
    private TextField inputField;

    public ChatFrame(String myId, String targetId, MainFrame mainFrame) {
        // this.myId = myId; // unused
        this.targetId = targetId;
        this.mainFrame = mainFrame;
        createWindow();
    }

    private void createWindow() {
        stage = new Stage();
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT); // 둥근 테두리 처리

        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("chat-root");
        layout.getStyleClass().add("rounded-window"); // 둥근 테두리 처리

        // 커스텀 타이틀바
        layout.setTop(App.createTitleBar(stage, targetId + "님"));

        textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        layout.setCenter(textArea);

        HBox bottom = new HBox(10);
        inputField = new TextField();
        inputField.getStyleClass().add("chat-input");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnAction(e -> sendMessage());

        Button sendBtn = new Button("전송");
        sendBtn.getStyleClass().add("btn-primary");
        sendBtn.setOnAction(e -> sendMessage());

        bottom.getChildren().addAll(inputField, sendBtn);
        layout.setBottom(bottom);

        // 그림자 효과를 위한 래퍼
        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(layout);
        wrapper.setStyle("-fx-background-color: transparent; -fx-padding: 10;");

        Scene scene = new Scene(wrapper, 370, 520);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT); // 투명하게

        // 스타일 적용
        ThemeManager.getInstance().applyTheme(scene);

        // CSS 클래스 적용
        textArea.getStyleClass().add("chat-area");

        stage.setScene(scene);
    }

    public void show() {
        if (!stage.isShowing()) {
            stage.show();
        }
        stage.toFront();
    }

    public void close() {
        stage.close();
    }

    private void sendMessage() {
        String msg = inputField.getText();
        if (msg.isEmpty())
            return;

        textArea.appendText("[나] " + msg + "\n");
        mainFrame.sendChatMessage(targetId, msg);
        mainFrame.requestChatList();
        inputField.clear();
    }

    public void receiveMessage(String msg) {
        Platform.runLater(() -> {
            textArea.appendText("[" + targetId + "] " + msg + "\n");
        });
    }

    public void clearArea() {
        Platform.runLater(() -> textArea.clear());
    }

    public void appendMsg(String msg) {
        Platform.runLater(() -> textArea.appendText(msg));
    }

    public void setVisible(boolean b) {
        if (b)
            show();
        else
            close();
    }
}