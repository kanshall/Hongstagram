import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import javafx.stage.FileChooser;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class MainFrame {

    private String myId;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    // 레이아웃 정의
    private BorderPane mainLayout;
    private StackPane contentArea;

    // 화면 패널들 (뷰)
    private VBox homePanel;
    private VBox feedListContainer;
    private VBox chatListPanel;

    private String selectedImagePath = null;
    private HashMap<String, ChatFrame> chatRooms = new HashMap<>();

    // 프로필 화면 구성요소들
    private javafx.scene.shape.Circle myProfileCircle;
    private Label myNameLabel;
    private TilePane myGalleryPane;
    private Label postCountLabel; // 게시물 수
    private Label followerCountLabel; // 팔로워 수
    private Label followingCountLabel; // 팔로잉 수

    public MainFrame(String userId, Socket passedSocket) {
        System.out.println("디버그: 메인 로직 로딩됨");
        this.myId = userId;
        this.socket = passedSocket;
        initConnection();
    }

    // ... (skipping getView, switchView etc)

    private VBox createMyPagePanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.TOP_CENTER);
        // ... (프로필 헤더 생성)
        HBox profileHeader = new HBox(20);
        profileHeader.setAlignment(Pos.CENTER_LEFT);

        myProfileCircle = new javafx.scene.shape.Circle(40);
        myProfileCircle.setFill(javafx.scene.paint.Color.LIGHTGRAY);
        myProfileCircle.setCursor(javafx.scene.Cursor.HAND);
        myProfileCircle.setOnMouseClicked(e -> handleProfileImageUpload());

        myNameLabel = new Label("사용자");
        myNameLabel.getStyleClass().add("profile-name");

        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        infoBox.getChildren().addAll(myNameLabel);
        profileHeader.getChildren().addAll(myProfileCircle, infoBox);

        // 통계 보여주는 부분
        HBox stats = new HBox(30);
        stats.setAlignment(Pos.CENTER);

        // 숫자 라벨들
        postCountLabel = new Label("0");
        postCountLabel.getStyleClass().add("stat-count");

        followerCountLabel = new Label("0");
        followerCountLabel.getStyleClass().add("stat-count");

        followingCountLabel = new Label("0");
        followingCountLabel.getStyleClass().add("stat-count");

        stats.getChildren().addAll(
                createStatItem("게시물", postCountLabel),
                createStatItem("팔로워", followerCountLabel),
                createStatItem("팔로잉", followingCountLabel));

        // 갤러리 영역 (격자 모양)
        myGalleryPane = new TilePane();
        myGalleryPane.setPadding(new Insets(5));
        myGalleryPane.setHgap(5);
        myGalleryPane.setVgap(5);
        myGalleryPane.setPrefColumns(3);
        myGalleryPane.setPrefTileWidth(120);
        myGalleryPane.setPrefTileHeight(120);
        myGalleryPane.setAlignment(Pos.TOP_LEFT);
        myGalleryPane.setStyle("-fx-background-color: transparent;");

        // 레이아웃 깨짐 방지를 위해 너비 제한
        myGalleryPane.setMaxWidth(400);

        ScrollPane galleryScroll = new ScrollPane(myGalleryPane);
        galleryScroll.setFitToWidth(true);
        galleryScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        galleryScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox.setVgrow(galleryScroll, Priority.ALWAYS);

        panel.getChildren().addAll(profileHeader, stats, new Separator(), galleryScroll);
        return panel;
    }

    // 라벨 입력을 위한 오버로딩
    private VBox createStatItem(String title, Label countLabel) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        // countLabel style is already set via class
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("stat-label");
        box.getChildren().addAll(countLabel, titleLbl);
        return box;
    }

    public Parent getView() {
        mainLayout = new BorderPane();
        mainLayout.setPrefSize(450, 800);
        mainLayout.getStyleClass().add("main-container");

        // [상단] 커스텀 타이틀바랑 헤더
        VBox topContainer = new VBox();
        topContainer.getChildren().add(App.createTitleBar(App.getPrimaryStage(), "Hongstagram"));

        HBox topPanel = new HBox();
        topPanel.getStyleClass().add("top-panel");
        topPanel.setAlignment(Pos.CENTER);
        topPanel.setPadding(new Insets(10));

        Label titleLabel = new Label("Hongstagram");
        titleLabel.getStyleClass().add("logo-text");
        titleLabel.setStyle("-fx-font-size: 24px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button settingsBtn = new Button("\uE713"); // Segoe MDL2 Settings Icon
        settingsBtn.getStyleClass().add("btn-icon");
        settingsBtn.setOnAction(e -> switchView("SETTINGS"));

        topPanel.getChildren().addAll(titleLabel, spacer, settingsBtn);
        topContainer.getChildren().add(topPanel);

        mainLayout.setTop(topContainer);

        // [중앙] 콘텐츠 영역
        contentArea = new StackPane();
        // contentArea.setPadding(new Insets(0));

        // 화면들 초기화
        homePanel = createHomePanel();

        contentArea.getChildren().add(homePanel); // 기본 화면
        mainLayout.setCenter(contentArea);

        // [하단] 네비게이션 바
        HBox bottomPanel = new HBox(40);
        bottomPanel.getStyleClass().add("bottom-panel");
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPadding(new Insets(10));

        Button btnHome = createNavButton("\uE80F", e -> switchView("HOME"));
        Button btnAdd = createNavButton("\uE710", e -> switchView("UPLOAD"));
        Button btnChat = createNavButton("\uE8BD", e -> switchView("CHAT_LIST"));
        Button btnUser = createNavButton("\uE77B", e -> switchView("MYPAGE"));

        bottomPanel.getChildren().addAll(btnHome, btnAdd, btnChat, btnUser);
        mainLayout.setBottom(bottomPanel);

        return mainLayout;
    }

    private Button createNavButton(String icon, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button(icon);
        btn.getStyleClass().add("btn-icon");
        btn.setOnAction(action);
        return btn;
    }

    private void switchView(String viewName) {
        contentArea.getChildren().clear();
        if (viewName.equals("HOME")) {
            requestRefresh();
            contentArea.getChildren().add(homePanel);
        } else if (viewName.equals("UPLOAD")) {
            contentArea.getChildren().add(createUploadPanel());
        } else if (viewName.equals("CHAT_LIST")) {
            contentArea.getChildren().add(createChatListPanel());
        } else if (viewName.equals("MYPAGE")) {
            contentArea.getChildren().add(createMyPagePanel());
            sendProtocol("GET_MY_PROFILE"); // 프로필 데이터 요청
            sendProtocol("GET_MY_POSTS"); // 내 게시물 요청
        } else if (viewName.equals("SETTINGS")) {
            contentArea.getChildren().add(createSettingsPanel());
        }
    }

    // --- 패널 생성 함수들 ---

    private VBox createHomePanel() {
        VBox panel = new VBox();
        panel.setAlignment(Pos.TOP_CENTER);

        feedListContainer = new VBox(20);
        feedListContainer.setPadding(new Insets(10));
        feedListContainer.setAlignment(Pos.TOP_CENTER);
        feedListContainer.setFillWidth(true);

        ScrollPane scrollPane = new ScrollPane(feedListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        panel.getChildren().add(scrollPane);

        requestRefresh(); // 데이터 초기 로딩
        return panel;
    }

    private VBox createUploadPanel() {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(20));

        Label title = new Label("새 게시물 작성");
        title.getStyleClass().add("section-title");

        Button imgBtn = new Button("사진 선택");
        imgBtn.setPrefSize(300, 200);
        imgBtn.setStyle("-fx-background-color: #efefef; -fx-text-fill: #888;");

        imgBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png"));
            File f = fc.showOpenDialog(null);
            if (f != null) {
                selectedImagePath = f.getAbsolutePath();
                try {
                    Image img = new Image("file:" + selectedImagePath);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(300);
                    iv.setFitHeight(200);
                    iv.setPreserveRatio(true);
                    imgBtn.setGraphic(iv);
                    imgBtn.setText("");
                } catch (Exception ex) {
                }
            }
        });

        TextArea contentArea = new TextArea();
        contentArea.setPromptText("문구를 입력하세요...");
        contentArea.setPrefRowCount(3);
        contentArea.setMaxWidth(300);

        Button uploadBtn = new Button("공유하기");
        uploadBtn.getStyleClass().add("btn-primary");
        uploadBtn.setMaxWidth(300);

        uploadBtn.setOnAction(e -> {
            String safeImg = (selectedImagePath == null) ? "null" : selectedImagePath;
            String txt = contentArea.getText();
            try {
                out.writeUTF("UPLOAD@@" + myId + "@@" + txt + "@@" + safeImg);
            } catch (Exception ex) {
            }
        });

        panel.getChildren().addAll(title, imgBtn, contentArea, uploadBtn);
        return panel;
    }

    private VBox createChatListPanel() {
        chatListPanel = new VBox(10);
        chatListPanel.setPadding(new Insets(10));

        try {
            out.writeUTF("GET_CHAT_LIST");
        } catch (Exception e) {
        }

        ScrollPane scroll = new ScrollPane(chatListPanel);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox container = new VBox();
        container.getStyleClass().add("chat-list-panel"); // 메인 컨테이너

        Label title = new Label("메시지 목록");
        title.getStyleClass().add("chat-list-title");

        container.getChildren().addAll(title, scroll);
        container.setPadding(new Insets(10));

        return container;
    }

    private VBox createSettingsPanel() {
        VBox panel = new VBox(10); // 간격 조금 줄임
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20)); // 여백 조금 줄임
        panel.getStyleClass().add("settings-panel");

        Label title = new Label("설정");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight:bold;");

        // Theme Settings
        Label themeLbl = new Label("테마 선택");
        themeLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");

        ComboBox<String> themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll("LIGHT", "DARK", "HONGIK");
        themeCombo.setValue(ThemeManager.getInstance().getCurrentTheme());
        themeCombo.setPrefWidth(100); // Fixed width 100
        themeCombo.setMaxWidth(100);

        themeCombo.setOnAction(e -> {
            String selected = themeCombo.getValue();
            if (selected != null) {
                ThemeManager.getInstance().setTheme(selected);
            }
        });

        Button pwBtn = new Button("비밀번호 변경");
        pwBtn.getStyleClass().add("btn-secondary");
        pwBtn.setOnAction(e -> showPasswordChangeOverlay());

        Button logoutBtn = new Button("로그아웃");
        logoutBtn.getStyleClass().add("btn-secondary");
        logoutBtn.setStyle("-fx-text-fill: red;");
        logoutBtn.setOnAction(e -> {
            try {
                out.writeUTF("LOGOUT");
                socket.close();
            } catch (Exception ex) {
            }
            try {
                // Return to Login
                App.setRoot(new Login().getView());
            } catch (Exception ex) {
            }
        });

        panel.getChildren().addAll(title, themeLbl, themeCombo, pwBtn, logoutBtn);

        // 회원 탈퇴 버튼 (복구됨)
        Button deleteAccountBtn = new Button("회원 탈퇴");
        deleteAccountBtn.getStyleClass().add("btn-secondary");
        deleteAccountBtn.setStyle("-fx-text-fill: red; -fx-font-size: 11px;"); // 작고 빨간 글씨로
        deleteAccountBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("회원 탈퇴");
            confirm.setHeaderText("정말 탈퇴하시겠습니까?");
            confirm.setContentText("탈퇴 시 모든 데이터(게시물, 댓글, 메시지 등)가 삭제되며 복구할 수 없습니다.");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        out.writeUTF("DELETE_USER");
                        socket.close(); // 연결 바로 끊기
                    } catch (Exception ex) {
                    }
                    try {
                        App.setRoot(new Login().getView());
                        showAlert("알림", "회원 탈퇴가 완료되었습니다.");
                    } catch (Exception ex) {
                    }
                }
            });
        });
        panel.getChildren().add(deleteAccountBtn);
        return panel;
    }

    // --- 로직 & 네트워크 관련 ---

    private void handleProfileImageUpload() {
        FileChooser fc = new FileChooser();
        fc.setTitle("프로필 사진 선택");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png", "*.jpeg"));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            String path = f.getAbsolutePath();
            sendProtocol("UPDATE_PROFILE_IMAGE@@" + path);
        }
    }

    private void initConnection() {
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            new Thread(this::startListener).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startListener() {
        try {
            while (true) {
                String msg = in.readUTF();
                String[] basicParts = msg.split("@@");
                String command = basicParts[0];

                Platform.runLater(() -> handleCommand(command, msg, basicParts));
            }
        } catch (Exception e) {
            System.out.println("연결 종료");
        }
    }

    private void handleCommand(String command, String msg, String[] parts) {
        if (command.equals("REFRESH_DATA")) {
            int splitIndex = msg.indexOf("@@");
            String rawData = (splitIndex != -1) ? msg.substring(splitIndex + 2) : "";
            updateFeedUI(rawData);
        } else if (command.equals("UPLOAD_SUCCESS")) {
            showAlert("알림", "업로드 완료!");
            requestRefresh();
            switchView("HOME");
        } else if (command.equals("CHAT_LIST_DATA")) {
            int splitIndex = msg.indexOf("@@");
            String rawData = (splitIndex != -1) ? msg.substring(splitIndex + 2) : "";
            updateChatListUI(rawData);
        } else if (command.equals("LIKE_SUCCESS") || command.equals("COMMENT_SUCCESS")) {
            requestRefresh();
        } else if (command.equals("HISTORY_DATA")) {
            // HISTORY_DATA@@uid@@content
            if (parts.length >= 2) {
                String targetUser = parts[1];
                String prefix = "HISTORY_DATA@@" + targetUser + "@@";
                String history = "";
                if (msg.startsWith(prefix))
                    history = msg.substring(prefix.length());
                openChatWithHistory(targetUser, history);
            }
        } else if (command.equals("CHAT_MSG")) {
            if (parts.length >= 3) {
                String senderId = parts[1];
                String text = parts[2];
                openAndReceiveChat(senderId, text);
            }
        } else if (command.equals("ALL_USERS_DATA")) {
            int splitIndex = msg.indexOf("@@");
            String rawData = (splitIndex != -1) ? msg.substring(splitIndex + 2) : "";
            showUserSelectDialog(rawData);
        } else if (command.equals("UPDATE_PROFILE_SUCCESS")) {
            showAlert("알림", "프로필 사진이 변경되었습니다.");
            sendProtocol("GET_MY_PROFILE"); // Refresh
        } else if (command.equals("MY_PROFILE_DATA")) {
            // MY_PROFILE_DATA@@name@@path@@followerCount
            if (parts.length >= 3) {
                String name = parts[1];
                String path = parts[2];

                Platform.runLater(() -> {
                    if (myNameLabel != null)
                        myNameLabel.setText(name);

                    // Update Stats
                    if (parts.length >= 4) {
                        String fCount = parts[3];
                        if (followerCountLabel != null)
                            followerCountLabel.setText(fCount);
                        if (followingCountLabel != null)
                            followingCountLabel.setText(fCount);
                    }

                    if (myProfileCircle != null) {
                        setAvatarImage(myProfileCircle, path);
                    }
                });
            }
        } else if (command.equals("CHANGE_PW_SUCCESS")) {
            showAlert("알림", "비밀번호가 변경되었습니다. 다시 로그인해주세요.");
            // 강제 로그아웃
            try {
                socket.close();
            } catch (Exception ex) {
            }
            try {
                // 로그인 화면으로 돌아가기
                App.setRoot(new Login().getView());
            } catch (Exception ex) {
            }
        } else if (command.equals("CHANGE_PW_FAIL")) {
            showAlert("오류", "비밀번호 변경에 실패했습니다.");
        } else if (command.equals("MY_POSTS_DATA")) {
            // pid@@uid@@name@@content@@img@@likeCnt@@isLiked@@[comments]@@uImg///...
            int splitIndex = msg.indexOf("@@");
            String rawData = (splitIndex != -1) ? msg.substring(splitIndex + 2) : "";
            updateMyGallery(rawData);
        }
    }

    private void updateFeedUI(String rawData) {
        if (feedListContainer == null)
            return;
        feedListContainer.getChildren().clear();

        if (rawData.length() > 0) {
            String[] posts = rawData.split("///");
            for (String p : posts) {
                String[] pd = p.split("@@");
                if (pd.length >= 8) { // 업데이트된 프로토콜
                    int pid = Integer.parseInt(pd[0]);
                    String uid = pd[1];
                    String name = pd[2];
                    String content = pd[3];
                    String imgPath = pd[4];
                    int likeCnt = Integer.parseInt(pd[5]);
                    boolean isLiked = Boolean.parseBoolean(pd[6]);
                    String commentsRaw = pd[7];
                    String uImg = (pd.length >= 9) ? pd[8] : "null"; // 프로필 이미지

                    ArrayList<String> comments = new ArrayList<>();
                    if (!commentsRaw.equals("EMPTY")) {
                        for (String c : commentsRaw.split("##"))
                            comments.add(c);
                    }

                    Post postObj = new Post(pid, uid, name, uImg, imgPath.equals("null") ? null : imgPath, content,
                            likeCnt,
                            isLiked, comments);
                    feedListContainer.getChildren().add(createPostItem(postObj));
                }
            }
        }
    }

    private VBox createPostItem(Post post) {
        VBox card = new VBox();
        card.getStyleClass().add("post-card");

        // 헤더 부분
        HBox header = new HBox(10);
        header.getStyleClass().add("post-header");
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.shape.Circle avatar = new javafx.scene.shape.Circle(15);
        avatar.setFill(javafx.scene.paint.Color.LIGHTGRAY);
        setAvatarImage(avatar, post.getUserProfileImage());

        Label name = new Label(post.getUserName());
        name.getStyleClass().add("lbl-username");
        header.getChildren().addAll(avatar, name);

        // 이미지 부분
        if (post.getImagePath() != null) {
            try {
                ImageView iv = new ImageView(new Image("file:" + post.getImagePath()));
                iv.setFitWidth(400); // 스크롤바 공간 때문에 조금 줄임
                iv.setPreserveRatio(true);
                card.getChildren().add(iv);
            } catch (Exception e) {
            }
        } else {
            // 이미지 없으면 텍스트를 크게?
            card.getChildren().add(header);
        }

        if (post.getImagePath() != null) {
            // 헤더 없으면 추가
            if (!card.getChildren().contains(header))
                card.getChildren().add(0, header);
        }

        // 액션 버튼들 (좋아요 등)
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER_LEFT); // 하트랑 텍스트 정렬
        actions.setPadding(new Insets(10, 10, 0, 10));
        // 좋아요 버튼
        Button likeBtn = new Button();
        if (post.isLikedByMe()) {
            likeBtn.setText("\u2665"); // 채워진 하트
            likeBtn.setStyle(
                    "-fx-background-color:transparent; -fx-font-family: 'Segoe UI Symbol'; -fx-font-size:22px; -fx-cursor:hand; -fx-text-fill: red;");
        } else {
            likeBtn.setText("\u2661"); // 빈 하트
            likeBtn.setStyle(
                    "-fx-background-color:transparent; -fx-font-family: 'Segoe UI Symbol'; -fx-font-size:22px; -fx-cursor:hand; -fx-text-fill: -fx-text-primary;");
        }

        likeBtn.setOnAction(e -> sendProtocol("LIKE@@" + post.getPid()));

        Label likeCount = new Label("\uc88b\uc544\uc694 " + post.getLikeCount() + "\uac1c");
        likeCount.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-weight: bold;"); // 스타일 명시
        actions.getChildren().addAll(likeBtn, likeCount);

        // 내용 부분
        Label contentLbl = new Label(post.getUserName() + ": " + post.getContent());
        contentLbl.setWrapText(true);
        contentLbl.setPadding(new Insets(5, 10, 5, 10));
        contentLbl.setStyle("-fx-text-fill: -fx-text-primary;"); // 잘 보이게 설정

        // 구분선
        Separator sep = new Separator();
        sep.setPadding(new Insets(5, 0, 5, 0));
        // 강제로 보이게 함
        sep.setStyle(
                "-fx-background-color: -fx-main-border-color; -fx-border-style: solid; -fx-border-width: 0 0 1 0; -fx-border-color: -fx-main-border-color;");

        // 댓글 영역 - 배경색 줘서 구분
        VBox commentBox = new VBox(5);
        commentBox.setPadding(new Insets(10));
        commentBox.setStyle("-fx-background-color: rgba(128, 128, 128, 0.1); -fx-background-radius: 5px;"); // 살짝 색 입힘

        if (!post.getComments().isEmpty()) {
            Label c1 = new Label(post.getComments().get(0));
            c1.setStyle("-fx-text-fill: -fx-text-primary;");
            commentBox.getChildren().add(c1);
            if (post.getComments().size() > 1) {
                Button more = new Button(
                        "\ub313\uae00 " + (post.getComments().size() - 1) + "\uac1c \ub354\ubcf4\uae30");
                more.setStyle("-fx-background-color:transparent; -fx-text-fill: -fx-text-secondary;");
                more.setOnAction(e -> {
                    commentBox.getChildren().remove(more);
                    for (int i = 1; i < post.getComments().size(); i++) {
                        Label cNext = new Label(post.getComments().get(i));
                        cNext.setStyle("-fx-text-fill: -fx-text-primary;");
                        commentBox.getChildren().add(cNext);
                    }
                });
                commentBox.getChildren().add(more);
            }
        }

        // 댓글 입력창
        HBox inputRow = new HBox(5);
        inputRow.setPadding(new Insets(0, 10, 10, 10));
        TextField cField = new TextField();
        cField.setPromptText("댓글 달기...");
        HBox.setHgrow(cField, Priority.ALWAYS);
        Button sendBtn = new Button("게시");
        sendBtn.setOnAction(e -> {
            if (!cField.getText().isEmpty()) {
                sendProtocol("COMMENT@@" + post.getPid() + "@@" + cField.getText());
                cField.clear();
            }
        });
        inputRow.getChildren().addAll(cField, sendBtn);

        card.getChildren().addAll(actions, contentLbl, sep, commentBox, inputRow);
        return card;
    }

    private void updateChatListUI(String rawData) {
        if (chatListPanel == null)
            return;
        chatListPanel.getChildren().clear();

        // New Chat Button
        Button newChat = new Button("➕ 새 대화 시작");
        newChat.setMaxWidth(Double.MAX_VALUE);
        newChat.setAlignment(Pos.CENTER_LEFT);
        newChat.getStyleClass().add("btn-secondary");
        newChat.getStyleClass().add("new-chat-btn"); // 클래스 추가
        newChat.setOnAction(e -> sendProtocol("GET_ALL_USERS"));
        chatListPanel.getChildren().add(newChat);

        if (rawData.length() > 0) {
            for (String item : rawData.split("///")) {
                Button row = new Button(item);
                row.setMaxWidth(Double.MAX_VALUE);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("chat-list-item"); // 클래스 추가

                String realId = item;
                if (item.contains("(") && item.endsWith(")")) {
                    realId = item.substring(0, item.indexOf("("));
                }
                String finalId = realId;

                row.setOnAction(e -> {
                    try {
                        out.writeUTF("GET_HISTORY@@" + finalId);
                    } catch (Exception ex) {
                    }
                });

                chatListPanel.getChildren().add(row);
            }
        }
    }

    // 유저 선택 기능 간단하게 구현
    private void showUserSelectDialog(String rawData) {
        // ... (유저 선택 구현)
        // 간단하게 텍스트 입력으로 처리
        TextInputDialog td = new TextInputDialog();
        td.setTitle("친구 찾기");
        td.setHeaderText("ID 입력 (또는 구현 필요)");
        td.showAndWait().ifPresent(id -> {
            try {
                out.writeUTF("GET_HISTORY@@" + id);
            } catch (Exception ex) {
            }
        });
    }

    private void requestRefresh() {
        sendProtocol("REFRESH");
    }

    private void sendProtocol(String msg) {
        try {
            out.writeUTF(msg);
        } catch (Exception e) {
        }
    }

    public void sendChatMessage(String targetId, String msg) {
        try {
            if (targetId.startsWith("GROUP_")) {
                String roomId = targetId.substring("GROUP_".length());
                out.writeUTF("GROUP_MSG@@" + roomId + "@@" + msg);
            } else {
                out.writeUTF("CHAT@@" + targetId + "@@" + msg);
            }
        } catch (Exception e) {
        }
    }

    public void requestChatList() {
        sendProtocol("GET_CHAT_LIST");
    }

    private void openAndReceiveChat(String keyId, String msg) {
        ChatFrame chatRoom = chatRooms.get(keyId);
        if (chatRoom == null) {
            chatRoom = new ChatFrame(myId, keyId, this);
            chatRooms.put(keyId, chatRoom);
            chatRoom.show();
        }
        chatRoom.show();
        chatRoom.receiveMessage(msg);
    }

    private void openChatWithHistory(String keyId, String historyData) {
        ChatFrame chatRoom = chatRooms.get(keyId);
        if (chatRoom == null) {
            chatRoom = new ChatFrame(myId, keyId, this);
            chatRooms.put(keyId, chatRoom);
        }
        chatRoom.clearArea(); // Clear

        if (historyData.length() > 0) {
            String[] msgs = historyData.split("///");
            for (String m : msgs) {
                String[] p = m.split("@@");
                if (p.length >= 2) {
                    String prefix = p[0].equals(myId) ? "[나]" : "[" + p[0] + "]";
                    chatRoom.appendMsg(prefix + " " + p[1] + "\n");
                }
            }
        }
        chatRoom.show();
    }

    private void showPasswordChangeOverlay() {
        // 뒷배경 어둡게
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        overlay.setOnMouseClicked(e -> contentArea.getChildren().remove(overlay)); // 바깥 클릭하면 닫기

        // 다이얼로그 박스
        VBox dialog = new VBox(15);
        dialog.setMaxWidth(300);
        dialog.setMaxHeight(200);
        dialog.setPadding(new Insets(20));
        dialog.setAlignment(Pos.CENTER);
        dialog.setStyle(
                "-fx-background-color: -fx-bg-panel; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        // 뒷배경 클릭 방지
        dialog.setOnMouseClicked(e -> e.consume());

        Label title = new Label("비밀번호 변경");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");

        PasswordField pwField = new PasswordField();
        pwField.setPromptText("새 비밀번호");

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("취소");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> contentArea.getChildren().remove(overlay));

        Button okBtn = new Button("변경");
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setOnAction(e -> {
            String npw = pwField.getText();
            if (!npw.isEmpty()) {
                sendProtocol("CHANGE_PW@@" + npw);
                contentArea.getChildren().remove(overlay);
            }
        });

        btnBox.getChildren().addAll(cancelBtn, okBtn);
        dialog.getChildren().addAll(title, pwField, btnBox);

        overlay.getChildren().add(dialog);
        contentArea.getChildren().add(overlay);
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    private void updateMyGallery(String rawData) {
        if (myGalleryPane == null)
            return;
        myGalleryPane.getChildren().clear();

        if (rawData == null || rawData.isEmpty()) {
            Platform.runLater(() -> {
                if (postCountLabel != null)
                    postCountLabel.setText("0");
            });
            return;
        }

        // System.out.println("DEBUG: updateMyGallery called with length: " +
        // rawData.length());

        Platform.runLater(() -> {
            String[] posts = rawData.split("///");
            int validCount = 0;

            for (String post : posts) {
                try {
                    String[] parts = post.split("@@");
                    // pid[0], uid[1], name[2], content[3], img[4], ...
                    if (parts.length < 5)
                        continue;

                    String imgPath = parts[4];
                    // System.out.println("DEBUG: Post Img Path: " + imgPath);

                    if (imgPath.equals("null") || imgPath.isEmpty())
                        continue;

                    File imgFile = new File(imgPath);
                    if (!imgFile.exists()) {
                        // System.out.println("DEBUG: Image file NOT found: " + imgPath);
                        continue;
                    }

                    ImageView iv = new ImageView();
                    iv.setImage(new Image(imgFile.toURI().toString()));

                    // Grid cell size
                    iv.setFitWidth(100);
                    iv.setFitHeight(100);
                    iv.setPreserveRatio(false); // Square crop

                    myGalleryPane.getChildren().add(iv);
                    validCount++;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (postCountLabel != null)
                postCountLabel.setText(String.valueOf(validCount));
        });
    }

    // 이미지 패턴을 안전하게 만들기 위한 헬퍼 함수
    private void setAvatarImage(javafx.scene.shape.Circle circle, String path) {
        if (path == null || path.equals("null") || path.isEmpty()) {
            circle.setFill(javafx.scene.paint.Color.LIGHTGRAY);
            return;
        }
        try {
            String url;
            if (path.startsWith("http") || path.startsWith("file:")) {
                url = path;
            } else {
                File f = new File(path);
                if (!f.exists()) {
                    circle.setFill(javafx.scene.paint.Color.LIGHTGRAY);
                    return;
                }
                url = f.toURI().toString();
            }

            Image img = new Image(url, false);
            if (img.isError()) {
                circle.setFill(javafx.scene.paint.Color.LIGHTGRAY);
            } else {
                circle.setFill(new javafx.scene.paint.ImagePattern(img));
            }
        } catch (Exception e) {
            circle.setFill(javafx.scene.paint.Color.LIGHTGRAY);
        }
    }
}