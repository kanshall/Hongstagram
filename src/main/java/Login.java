import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.*;
import java.net.Socket;

public class Login {

    private TextField idField;
    private PasswordField pwField;

    public Parent getView() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(App.createTitleBar(App.getPrimaryStage(), ""));

        VBox root = new VBox(20);
        root.getStyleClass().add("login-container"); // 로그인 스타일 재사용
        root.setAlignment(Pos.CENTER);

        // 로고
        Label logo = new Label("Hongstagram");
        logo.getStyleClass().add("logo-text");

        // 입력 필드들
        VBox inputContainer = new VBox(10);
        inputContainer.setMaxWidth(250);
        inputContainer.setAlignment(Pos.CENTER);

        idField = new TextField();
        idField.setPromptText("사용자 이름 또는 이메일");

        pwField = new PasswordField();
        pwField.setPromptText("비밀번호");
        pwField.setOnAction(e -> tryLogin()); // 엔터키 처리

        inputContainer.getChildren().addAll(idField, pwField);

        root.getChildren().addAll(idField, pwField);

        // 버튼들
        Button loginBtn = new Button("로그인");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(250);
        loginBtn.setOnAction(e -> tryLogin());

        // 가입 / 비밀번호 찾기 링크
        HBox linkBox = new HBox(10);
        linkBox.setAlignment(Pos.CENTER);

        Button joinBtn = new Button("가입하기");
        joinBtn.getStyleClass().add("btn-secondary");
        joinBtn.setOnAction(e -> App.setRoot(new SignUp().getView()));

        linkBox.getChildren().add(joinBtn);

        root.getChildren().addAll(logo, inputContainer, loginBtn, linkBox);

        mainLayout.setCenter(root);
        return mainLayout;
    }

    private void tryLogin() {
        String uid = idField.getText().trim();
        String upw = pwField.getText().trim();
        if (uid.isEmpty() || upw.isEmpty())
            return;

        try {
            Socket socket = new Socket("127.0.0.1", 9999);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF("LOGIN@@" + uid + "@@" + upw);
            String response = in.readUTF();

            if (response.startsWith("LOGIN_SUCCESS")) {
                // parts[1]은 이름 (안 써도 됨)
                App.setRoot(new MainFrame(uid, socket).getView());

            } else {
                showAlert("로그인 실패", "아이디 또는 비밀번호가 잘못되었습니다.");
                socket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("오류", "서버 연결에 실패했습니다.");
        }
    }
    // 다이얼로그 기능은 SignUp.java로 옮겨져서 삭제함

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}