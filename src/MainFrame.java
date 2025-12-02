import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    
    private ArrayList<Theme> themeList = new ArrayList<>();
    private int currentThemeIndex = 0; 
    
    private JPanel feedListPanel; 
    private JPanel chatListPanel;
    
    private String selectedImagePath = null; 
    private String myId; 
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    // 채팅창 관리
    private HashMap<String, ChatFrame> chatRooms = new HashMap<>();

    public MainFrame(String userId, Socket passedSocket) {
        this.myId = userId; 
        this.socket = passedSocket; 

        initConnection(); 

        // 테마 설정
        themeList.add(new Theme("기본", Color.WHITE, Color.WHITE, Color.BLACK, new Color(0, 50, 200))); 
        themeList.add(new Theme("다크", new Color(30, 30, 30), new Color(50, 50, 50), Color.WHITE, new Color(255, 204, 0))); 
        themeList.add(new Theme("홍익", new Color(0, 30, 80), new Color(0, 50, 120), Color.WHITE, Color.YELLOW)); 

        setTitle("Hongstagram - " + myId);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container c = getContentPane();
        c.setLayout(new BorderLayout());

        // [Top]
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel titleLabel = new JLabel("Hongstagram");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(titleLabel);
        c.add(topPanel, BorderLayout.NORTH);

        // [Center]
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        
        mainContentPanel.add(createSettingsPanel(), "SETTINGS");
        mainContentPanel.add(createHomePanel(), "HOME");
        cardLayout.show(mainContentPanel, "HOME");
        c.add(mainContentPanel, BorderLayout.CENTER);

        // [Bottom]
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 15));
        bottomPanel.setBorder(new MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        JButton btnHome = createMenuButton("🏠");
        JButton btnAdd = createMenuButton("➕");
        JButton btnChat = createMenuButton("💬");
        JButton btnUser = createMenuButton("👤");

        btnHome.addActionListener(e -> {
            requestRefresh(); 
            cardLayout.show(mainContentPanel, "HOME");
        });
        btnAdd.addActionListener(e -> {
            mainContentPanel.add(createUploadPanel(), "UPLOAD"); 
            cardLayout.show(mainContentPanel, "UPLOAD");
        });
        btnChat.addActionListener(e -> {
            // createChatListPanel()이 이제 내부에서 데이터 요청을 함
            mainContentPanel.add(createChatListPanel(), "CHAT_LIST");
            cardLayout.show(mainContentPanel, "CHAT_LIST");
        });
        btnUser.addActionListener(e -> {
            mainContentPanel.add(createMyPagePanel(), "MYPAGE"); 
            cardLayout.show(mainContentPanel, "MYPAGE");
        });

        bottomPanel.add(btnHome);
        bottomPanel.add(btnAdd);
        bottomPanel.add(btnChat);
        bottomPanel.add(btnUser);
        c.add(bottomPanel, BorderLayout.SOUTH);

        setSize(500, 700);
        setLocationRelativeTo(null);
        applyTheme(); 
        setVisible(true);
    }

    private void initConnection() {
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            System.out.println("서버와 연결 유지됨: " + myId);
            new Thread(() -> startListener()).start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "연결 오류!");
            System.exit(0);
        }
    }

    // ===============================================================
    // [Listener] 서버 메시지 수신 (헬퍼 메소드 사용으로 정리됨)
    // ===============================================================
    private void startListener() {
        try {
            while (true) {
                String msg = in.readUTF();
                String[] basicParts = msg.split("@@");
                String command = basicParts[0];

                if (command.equals("REFRESH_DATA")) {
                    String rawData = "";
                    int splitIndex = msg.indexOf("@@");
                    if (splitIndex != -1) rawData = msg.substring(splitIndex + 2);
                    updateFeedUI(rawData);
                }
                else if (command.equals("UPLOAD_SUCCESS")) {
                    JOptionPane.showMessageDialog(null, "업로드 완료!");
                    requestRefresh();
                    cardLayout.show(mainContentPanel, "HOME");
                }
                // [1:1 채팅 수신] - openAndReceiveChat 사용!
                else if (command.equals("CHAT_MSG")) {
                    if (basicParts.length >= 3) {
                        String senderId = basicParts[1];
                        String text = basicParts[2];
                        openAndReceiveChat(senderId, text, false);
                    }
                }
                // [그룹 채팅 수신] - openAndReceiveChat 사용!
                else if (command.equals("GROUP_CHAT_MSG")) {
                    if (basicParts.length >= 4) {
                        String roomId = basicParts[1];
                        String senderId = basicParts[2];
                        String text = basicParts[3];
                        
                        if (!senderId.equals(myId)) {
                            // 그룹 메시지는 "GROUP_방번호"가 ID가 됨
                            openAndReceiveChat("GROUP_" + roomId, "[" + senderId + "] " + text, true);
                        }
                    }
                }
                else if (command.equals("CHAT_LIST_DATA")) {
                    String rawData = "";
                    int splitIndex = msg.indexOf("@@");
                    if (splitIndex != -1) rawData = msg.substring(splitIndex + 2);
                    updateChatListUI(rawData);
                }
                // [기록 수신] - openChatWithHistory 사용!
                else if (command.equals("HISTORY_DATA")) {
                    if (basicParts.length >= 2) {
                        String targetUser = basicParts[1];
                        String prefix = "HISTORY_DATA@@" + targetUser + "@@";
                        String history = "";
                        if (msg.startsWith(prefix)) history = msg.substring(prefix.length());
                        openChatWithHistory(targetUser, history); 
                    }
                }
                else if (command.equals("ALL_USERS_DATA")) {
                    String rawData = "";
                    int splitIndex = msg.indexOf("@@");
                    if (splitIndex != -1) rawData = msg.substring(splitIndex + 2);
                    showUserSelectDialog(rawData); 
                }
                else if (command.equals("GROUP_CREATED")) {
                    String roomId = basicParts[1];
                    String roomName = basicParts[2];
                    JOptionPane.showMessageDialog(null, "그룹방 생성 완료: " + roomName);
                    try { out.writeUTF("GET_CHAT_LIST"); } catch(Exception e){}
                    // 빈 방 열기 (기록 없음) -> openChatWithHistory 사용
                    openChatWithHistory("GROUP_" + roomId, ""); 
                }
                else if (command.equals("DELETE_SUCCESS")) {
                    JOptionPane.showMessageDialog(null, "탈퇴가 완료되었습니다.");
                    try { socket.close(); } catch(Exception e){}
                    dispose();
                    new Login();
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("서버 연결 끊김");
        }
    }

    public void requestRefresh() { try { out.writeUTF("REFRESH"); } catch (Exception e) {} }
    public void requestChatList() { try { out.writeUTF("GET_CHAT_LIST"); } catch (Exception e) {} }

    public void sendChatMessage(String targetId, String msg) { 
        try { 
            if (targetId.startsWith("GROUP_")) {
                String roomId = targetId.substring("GROUP_".length());
                out.writeUTF("GROUP_MSG@@" + roomId + "@@" + msg);
            } else {
                out.writeUTF("CHAT@@" + targetId + "@@" + msg); 
            }
        } catch (Exception e) {} 
    }

    // ===============================================================
    // [Helper] 채팅창 관련 헬퍼 메소드 (누락되었던 부분 복구!)
    // ===============================================================
    
    // 1. 실시간 메시지 수신 시 창 열고 내용 추가
    private void openAndReceiveChat(String keyId, String msg, boolean isGroup) {
        ChatFrame chatRoom = chatRooms.get(keyId);
        if (chatRoom == null) {
            chatRoom = new ChatFrame(myId, keyId, this);
            chatRooms.put(keyId, chatRoom);
        }
        chatRoom.setVisible(true);
        chatRoom.receiveMessage(msg);
    }

    // 2. 과거 기록으로 창 열기
    private void openChatWithHistory(String keyId, String historyData) {
        ChatFrame chatRoom = chatRooms.get(keyId);
        if (chatRoom == null) {
            chatRoom = new ChatFrame(myId, keyId, this);
            chatRooms.put(keyId, chatRoom);
        }
        chatRoom.clearArea(); // 기존 내용 지우기
        
        if (historyData.length() > 0) {
            String[] msgs = historyData.split("///");
            for (String m : msgs) {
                String[] p = m.split("@@"); // 보낸사람@@내용
                if (p.length >= 2) {
                    String prefix = p[0].equals(myId) ? "[나]" : "[" + p[0] + "]";
                    chatRoom.appendMsg(prefix + " " + p[1] + "\n");
                }
            }
        }
        chatRoom.setVisible(true);
    }

    private void showUserSelectDialog(String rawData) {
        JDialog d = new JDialog(this, "대화 상대 선택", true);
        d.setSize(300, 400);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout());

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        
        ArrayList<JCheckBox> boxes = new ArrayList<>();
        if (rawData.length() > 0) {
            String[] users = rawData.split("///");
            for (String user : users) {
                JCheckBox box = new JCheckBox(user);
                boxes.add(box);
                listPanel.add(box);
            }
        }
        d.add(new JScrollPane(listPanel), BorderLayout.CENTER);

        JButton okBtn = new JButton("확인");
        okBtn.addActionListener(e -> {
            ArrayList<String> selected = new ArrayList<>();
            for (JCheckBox box : boxes) {
                if (box.isSelected()) selected.add(box.getText());
            }

            if (selected.size() == 0) {
                JOptionPane.showMessageDialog(d, "대화 상대를 선택하세요.");
                return;
            }

            if (selected.size() == 1) {
                try { out.writeUTF("GET_HISTORY@@" + selected.get(0)); } catch(Exception ex){}
            } else {
                String roomName = JOptionPane.showInputDialog(d, "그룹 채팅방 이름을 입력하세요:");
                if (roomName != null && !roomName.trim().isEmpty()) {
                    String members = String.join(",", selected);
                    try { out.writeUTF("CREATE_GROUP@@" + roomName + "@@" + members); } catch(Exception ex){}
                }
            }
            d.dispose();
        });
        d.add(okBtn, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // ===============================================================
    // UI 생성 메소드들
    // ===============================================================
    private void updateFeedUI(String rawData) {
        feedListPanel.removeAll();
        if (rawData.length() > 0) {
            String[] posts = rawData.split("///");
            for (String postStr : posts) {
                String[] parts = postStr.split("@@");
                
                // 데이터가 4개(아이디, 이름, 내용, 이미지)인지 확인
                if (parts.length >= 4) {
                    String uid = parts[0];
                    String name = parts[1]; // 이름 추가됨
                    String content = parts[2];
                    String imgPath = parts[3];
                    
                    if(imgPath.equals("null")) imgPath = null;
                    
                    // Post 생성자에 이름도 같이 전달
                    Post p = new Post(uid, name, imgPath, content);
                    feedListPanel.add(createPostItem(p));
                    feedListPanel.add(Box.createVerticalStrut(20));
                }
            }
        }
        feedListPanel.revalidate();
        feedListPanel.repaint();
    }

    // [수정됨] 채팅 목록 그리기 (이름 표시 기능 추가)
    // [수정됨] 채팅 목록 그리기 (방 이름 파싱 기능 추가)
    private void updateChatListUI(String rawData) {
        if(chatListPanel == null) return;
        chatListPanel.removeAll();
        Theme t = themeList.get(currentThemeIndex); 
        chatListPanel.setBackground(t.bgMain);

        // 상단 "새 대화 시작" 버튼
        JButton newChatBtn = new JButton("➕ 새 대화 시작");
        newChatBtn.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        newChatBtn.setBackground(new Color(230, 240, 255));
        newChatBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        newChatBtn.addActionListener(e -> {
            try { out.writeUTF("GET_ALL_USERS"); } catch(Exception ex){}
        });
        chatListPanel.add(newChatBtn);
        chatListPanel.add(Box.createVerticalStrut(10));

        if (rawData.length() > 0) {
            String[] listItems = rawData.split("///"); // "아이디(이름)" 덩어리들
            
            for (String item : listItems) {
                // 파싱: "GROUP_1(우리방)" -> realId="GROUP_1", displayName="우리방"
                String realId = item;
                String displayName = item;
                
                int parenIndex = item.indexOf("(");
                if (parenIndex != -1 && item.endsWith(")")) {
                    realId = item.substring(0, parenIndex);
                    displayName = item.substring(parenIndex + 1, item.length() - 1);
                }

                // 버튼 텍스트 결정
                String btnText;
                if (realId.startsWith("GROUP_")) {
                    btnText = "▶ " + displayName; // 그룹
                } else {
                    btnText = "▷ " + displayName; // 1:1
                }
                
                JButton userBtn = new JButton(btnText);
                userBtn.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
                userBtn.setBorderPainted(false);
                userBtn.setContentAreaFilled(false);
                userBtn.setForeground(t.textMain);
                userBtn.setHorizontalAlignment(SwingConstants.LEFT);
                
                // 클릭 시에는 이름이 아니라 'realId(GROUP_1)'를 사용
                String finalTargetId = realId;
                userBtn.addActionListener(e -> {
                    try { out.writeUTF("GET_HISTORY@@" + finalTargetId); } catch(Exception ex){}
                });
                
                chatListPanel.add(userBtn);
                chatListPanel.add(Box.createVerticalStrut(10));
            }
        } 
        chatListPanel.revalidate();
        chatListPanel.repaint();
    }

    private JPanel createHomePanel() {
        Theme t = themeList.get(currentThemeIndex);
        feedListPanel = new JPanel();
        feedListPanel.setLayout(new BoxLayout(feedListPanel, BoxLayout.Y_AXIS));
        feedListPanel.setBackground(t.bgMain);
        requestRefresh();
        
        JScrollPane scrollPane = new JScrollPane(feedListPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        
        JPanel finalPanel = new JPanel(new BorderLayout());
        finalPanel.add(scrollPane, BorderLayout.CENTER);
        return finalPanel;
    }

    private JPanel createUploadPanel() {
        Theme t = themeList.get(currentThemeIndex);
        JPanel panel = new JPanel(new GridBagLayout()); 
        panel.setBackground(t.bgMain); 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); 
        gbc.gridx = 0; 
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        
        JLabel label = new JLabel("새 게시물 작성", SwingConstants.CENTER); 
        label.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        label.setForeground(t.textMain);
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(label, gbc);

        JButton imgBtn = new JButton("사진 선택");
        imgBtn.setPreferredSize(new Dimension(300, 200)); 
        imgBtn.setMinimumSize(new Dimension(300, 200));
        imgBtn.setMaximumSize(new Dimension(300, 200));
        
        imgBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("이미지", "jpg", "png");
            fc.setFileFilter(filter);
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                selectedImagePath = fc.getSelectedFile().getAbsolutePath();
                ImageIcon icon = new ImageIcon(selectedImagePath);
                imgBtn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(300, 200, Image.SCALE_SMOOTH)));
                imgBtn.setText("");
            }
        });
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE; 
        panel.add(imgBtn, gbc);

        JTextArea contentArea = new JTextArea(5, 30); 
        contentArea.setLineWrap(true);
        contentArea.setBorder(new LineBorder(Color.GRAY));
        JScrollPane scrollContent = new JScrollPane(contentArea); 
        scrollContent.setPreferredSize(new Dimension(300, 100));
        scrollContent.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollContent.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        panel.add(scrollContent, gbc);
        
        JButton uploadBtn = new JButton("공유하기");
        uploadBtn.setPreferredSize(new Dimension(300, 40));
        uploadBtn.addActionListener(e -> {
            String safeImg = (selectedImagePath == null) ? "null" : selectedImagePath;
            try { out.writeUTF("UPLOAD@@" + myId + "@@" + contentArea.getText() + "@@" + safeImg); } catch(Exception ex) {}
        });
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(uploadBtn, gbc);
        
        gbc.gridy = 4;
        gbc.weighty = 1.0; 
        panel.add(new JPanel(), gbc);
        return panel;
    }

    // 채팅 목록 탭 화면
    private JPanel createChatListPanel() {
        // 패널이 이미 있으면 재사용 (없으면 새로 생성)
        if (chatListPanel == null) {
            chatListPanel = new JPanel();
            chatListPanel.setLayout(new BoxLayout(chatListPanel, BoxLayout.Y_AXIS));
        }
        
        // 테마 적용
        Theme t = themeList.get(currentThemeIndex);
        chatListPanel.setBackground(t.bgMain);
        
        // 화면 열 때마다 서버에 최신 목록 요청
        try { out.writeUTF("GET_CHAT_LIST"); } catch(Exception e) {}

        JScrollPane scrollPane = new JScrollPane(chatListPanel);
        scrollPane.setBorder(null);
        
        JPanel finalPanel = new JPanel(new BorderLayout());
        finalPanel.setBackground(t.bgMain);
        
        JLabel label = new JLabel("메시지 목록", SwingConstants.CENTER);
        label.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        label.setForeground(t.textMain);
        label.setBorder(new EmptyBorder(10,0,10,0));
        
        finalPanel.add(label, BorderLayout.NORTH);
        finalPanel.add(scrollPane, BorderLayout.CENTER);
        return finalPanel;
    }

    private JPanel createMyPagePanel() {
        Theme t = themeList.get(currentThemeIndex);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(t.bgMain); 
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS)); 
        topContainer.setBackground(t.bgPanel);
        topContainer.setBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        JPanel profileInfoWrapper = new JPanel(new BorderLayout());
        profileInfoWrapper.setBackground(t.bgPanel);
        JPanel profileDetail = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        profileDetail.setBackground(t.bgPanel);

        JLabel profileImg = new JLabel("프사", SwingConstants.CENTER);
        profileImg.setPreferredSize(new Dimension(80, 80));
        profileImg.setOpaque(true);
        profileImg.setBackground(Color.LIGHT_GRAY); 
        
        JPanel namePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        namePanel.setBackground(t.bgPanel);
        JLabel nameLabel = new JLabel(myId);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        nameLabel.setForeground(t.textMain);
        
        JButton chatBtn = new JButton("🔍 친구 찾기 / DM");
        chatBtn.setBackground(new Color(240, 240, 240));
        chatBtn.addActionListener(e -> {
             String target = JOptionPane.showInputDialog("대화할 상대방 ID:");
             if(target != null && !target.isEmpty()) try { out.writeUTF("GET_HISTORY@@" + target); } catch(Exception ex){}
        });
        
        namePanel.add(nameLabel);
        namePanel.add(chatBtn);
        profileDetail.add(profileImg);
        profileDetail.add(namePanel);

        JButton settingsBtn = new JButton("⚙️"); 
        settingsBtn.setContentAreaFilled(false);
        settingsBtn.setBorderPainted(false);
        settingsBtn.addActionListener(e -> cardLayout.show(mainContentPanel, "SETTINGS"));

        profileInfoWrapper.add(profileDetail, BorderLayout.CENTER);
        profileInfoWrapper.add(settingsBtn, BorderLayout.EAST);
        topContainer.add(profileInfoWrapper);

        JPanel statsPanel = new JPanel(new GridLayout(1, 3));
        statsPanel.setBackground(t.bgPanel);
        statsPanel.setBorder(new EmptyBorder(0, 20, 15, 20)); 
        statsPanel.add(createStatItem("게시물", "0", t));
        statsPanel.add(createStatItem("팔로워", "1.2K", t));
        statsPanel.add(createStatItem("팔로잉", "55", t));
        topContainer.add(statsPanel); 
        panel.add(topContainer, BorderLayout.NORTH);
        
        JLabel gridPlaceholder = new JLabel("<html><center>📸<br>내 사진들</center></html>", SwingConstants.CENTER);
        gridPlaceholder.setForeground(Color.GRAY);
        panel.add(gridPlaceholder, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSettingsPanel() {
        Theme t = themeList.get(currentThemeIndex);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(t.bgMain);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("환경설정 (Settings)", SwingConstants.CENTER);
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        title.setForeground(t.textMain);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(30));

        // 테마 변경 버튼
        JButton themeBtn = new JButton("🎨 테마 변경: " + t.name);
        styleSettingsButton(themeBtn, t);
        themeBtn.addActionListener(e -> {
            // 다음 테마로 변경
            currentThemeIndex++; 
            if (currentThemeIndex >= themeList.size()) currentThemeIndex = 0;
            
            // 전체 프레임(헤더/푸터) 색상 적용
            applyTheme();
            
            // 모든 패널을 '새로운 테마'로 다시 만들어서 갈아끼움
            mainContentPanel.add(createSettingsPanel(), "SETTINGS"); // 설정 화면도 다시 만듦!
            mainContentPanel.add(createMyPagePanel(), "MYPAGE");
            mainContentPanel.add(createUploadPanel(), "UPLOAD");
            mainContentPanel.add(createChatListPanel(), "CHAT_LIST"); 
            mainContentPanel.add(createHomePanel(), "HOME");

            // 새로 만든 설정 화면을 바로 보여줌 (색상 바뀐 거 확인)
            cardLayout.show(mainContentPanel, "SETTINGS"); 
        });
        panel.add(themeBtn);
        panel.add(Box.createVerticalStrut(15));

        // 로그아웃 버튼
        JButton logoutBtn = new JButton("🚪 로그아웃");
        styleSettingsButton(logoutBtn, t);
        logoutBtn.addActionListener(e -> {
            try { 
                if(out != null) out.writeUTF("LOGOUT"); 
                socket.close(); 
            } catch(Exception ex) {}
            dispose();
            new Login(); 
        });
        panel.add(logoutBtn);
        panel.add(Box.createVerticalStrut(15));
        
        // 계정 탈퇴 버튼
        JButton deleteBtn = new JButton("💀 계정 탈퇴");
        styleSettingsButton(deleteBtn, t);
        deleteBtn.addActionListener(e -> {
            int answer = JOptionPane.showConfirmDialog(null, "정말 탈퇴하시겠습니까?", "경고", JOptionPane.YES_NO_OPTION);
            if(answer == JOptionPane.YES_OPTION) {
                try { out.writeUTF("DELETE_USER"); } catch(Exception ex){}
            }
        });
        panel.add(deleteBtn);
        
        // 여백
        panel.add(Box.createVerticalStrut(15));
        
        // 뒤로가기 버튼
        JButton backBtn = new JButton("⬅ 돌아가기");
        styleSettingsButton(backBtn, t);
        backBtn.addActionListener(e -> {
            // 돌아갈 때도 리프레쉬
            mainContentPanel.add(createMyPagePanel(), "MYPAGE");
            cardLayout.show(mainContentPanel, "MYPAGE");
        });
        panel.add(backBtn);

        return panel;
    }

    private void styleSettingsButton(JButton btn, Theme t) {
        btn.setMaximumSize(new Dimension(400, 50));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
        btn.setBackground(Color.LIGHT_GRAY);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JPanel createStatItem(String title, String count, Theme t) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(t.bgPanel);
        JLabel lCount = new JLabel(count, SwingConstants.CENTER);
        lCount.setFont(new Font("SansSerif", Font.BOLD, 16));
        lCount.setForeground(t.textMain);
        JLabel lTitle = new JLabel(title, SwingConstants.CENTER);
        lTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lTitle.setForeground(Color.GRAY);
        p.add(lCount, BorderLayout.CENTER);
        p.add(lTitle, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createPostItem(Post post) {
        Theme t = themeList.get(currentThemeIndex);
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(t.bgPanel);
        p.setBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(t.bgPanel);
        
        JButton userBtn = new JButton("👤 " + post.getUserName());
        
        userBtn.setBorderPainted(false); userBtn.setContentAreaFilled(false);
        userBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        userBtn.setForeground(t.textMain);
        
        userBtn.addActionListener(e -> {
            if(!post.getUserId().equals(myId)) {
                try { 
                    out.writeUTF("GET_HISTORY@@" + post.getUserId()); 
                } catch(Exception ex){}
            }
        });
        
        header.add(userBtn);
        p.add(header, BorderLayout.NORTH);

        JPanel imgP = new JPanel();
        imgP.setBackground(t.bgPanel);
        if(post.getImagePath() != null) {
            ImageIcon icon = new ImageIcon(post.getImagePath());
            imgP.add(new JLabel(new ImageIcon(icon.getImage().getScaledInstance(380, 300, Image.SCALE_SMOOTH))));
        }
        p.add(imgP, BorderLayout.CENTER);
        JPanel footer = new JPanel();
        footer.setBackground(t.bgPanel);
        JLabel content = new JLabel(post.getContent());
        content.setForeground(t.textMain);
        footer.add(content);
        p.add(footer, BorderLayout.SOUTH);
        return p;
    }

    private void applyTheme() {
        Theme t = themeList.get(currentThemeIndex);
        getContentPane().setBackground(t.bgMain);
        mainContentPanel.setBackground(t.bgMain);
        requestRefresh(); 
        mainContentPanel.repaint();
    }
    
    private JButton createMenuButton(String t) {
        JButton b = new JButton(t);
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        b.setBorderPainted(false); b.setContentAreaFilled(false);
        return b;
    }

    public static void main(String[] args) {
        System.out.println("Login.java를 실행하세요!");
    }
}