import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.*;
import java.net.Socket;

public class SignUp {

    private TextField idField;
    private PasswordField pwField;
    private TextField nameField;

    public Parent getView() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(App.createTitleBar(App.getPrimaryStage(), ""));

        VBox root = new VBox(20);
        root.getStyleClass().add("login-container"); // 로그인 스타일 재사용
        root.setAlignment(Pos.CENTER);

        // 로고 / 제목
        Label title = new Label("Hongstagram");
        title.getStyleClass().add("logo-text");

        Label subTitle = new Label("친구들과 일상을 공유해 보세요!");
        subTitle.getStyleClass().add("login-subtitle");
        // subTitle.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 14px;
        // -fx-font-weight: bold;"); // CSS로 옮김
        subTitle.setWrapText(true);
        subTitle.setMaxWidth(250);
        subTitle.setAlignment(Pos.CENTER);

        // 입력 필드들
        VBox inputContainer = new VBox(10);
        inputContainer.setMaxWidth(250);
        inputContainer.setAlignment(Pos.CENTER);

        idField = new TextField();
        idField.setPromptText("아이디");

        pwField = new PasswordField();
        pwField.setPromptText("비밀번호");

        nameField = new TextField();
        nameField.setPromptText("성명 (닉네임)");

        inputContainer.getChildren().addAll(idField, pwField, nameField);

        // 가입 버튼
        Button joinBtn = new Button("가입");
        joinBtn.getStyleClass().add("btn-primary"); // 파란색 버튼
        joinBtn.setMaxWidth(250);
        joinBtn.setOnAction(e -> tryJoin());

        // 로그인 화면으로 돌아가기
        Button backBtn = new Button("계정이 있으신가요? 로그인");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> App.setRoot(new Login().getView()));

        root.getChildren().addAll(title, subTitle, inputContainer, joinBtn, backBtn);

        mainLayout.setCenter(root);
        return mainLayout;
    }

    private void tryJoin() {
        String uid = idField.getText().trim();
        String upw = pwField.getText().trim();
        String name = nameField.getText().trim();

        if (uid.isEmpty() || upw.isEmpty() || name.isEmpty()) {
            showAlert("입력 오류", "모든 정보를 입력해주세요.");
            return;
        }

        try {
            Socket socket = new Socket("127.0.0.1", 9999);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF("JOIN@@" + uid + "@@" + upw + "@@" + name);
            String response = in.readUTF();

            if (response.equals("JOIN_SUCCESS")) {
                showAlert("성공", "회원가입이 완료되었습니다!\n로그인 화면으로 이동합니다.");
                App.setRoot(new Login().getView());
            } else {
                showAlert("실패", "이미 존재하는 아이디거나 오류가 발생했습니다.");
            }
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("오류", "서버 연결에 실패했습니다.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
