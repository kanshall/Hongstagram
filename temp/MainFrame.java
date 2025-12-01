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
    private JPanel chatListPanel; // [추가] 채팅 목록 패널
    
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

        // 0. 서버 연결 유지 & 리스너 시작
        initConnection(); 

        // 테마 설정
        themeList.add(new Theme("기본", Color.WHITE, Color.WHITE, Color.BLACK, new Color(0, 50, 200))); 
        themeList.add(new Theme("다크", new Color(30, 30, 30), new Color(50, 50, 50), Color.WHITE, new Color(255, 204, 0))); 
        themeList.add(new Theme("홍익", new Color(0, 30, 80), new Color(0, 50, 120), Color.WHITE, Color.YELLOW)); 

        setTitle("Hongstagram - " + myId);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container c = getContentPane();
        c.setLayout(new BorderLayout());

        // [Top] 상단 헤더
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel titleLabel = new JLabel("Hongstagram");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(titleLabel);

        JButton themeBtn = new JButton("🎨");
        themeBtn.setBorderPainted(false); themeBtn.setContentAreaFilled(false); themeBtn.setFocusPainted(false);
        themeBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        themeBtn.addActionListener(e -> {
            currentThemeIndex++; 
            if (currentThemeIndex >= themeList.size()) currentThemeIndex = 0;
            applyTheme(); 
        });
        topPanel.add(themeBtn);
        c.add(topPanel, BorderLayout.NORTH);

        // [Center] 카드 레이아웃 (화면 교체용)
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        
        mainContentPanel.add(createHomePanel(), "HOME");
        // 다른 패널들은 버튼 누를 때 생성
        c.add(mainContentPanel, BorderLayout.CENTER);

        // [Bottom] 하단 메뉴바
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 15));
        bottomPanel.setBorder(new MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        JButton btnHome = createMenuButton("🏠");
        JButton btnAdd = createMenuButton("➕");
        JButton btnChat = createMenuButton("💬"); // [추가] 채팅 목록 버튼
        JButton btnUser = createMenuButton("👤");

        // 버튼 이벤트
        btnHome.addActionListener(e -> {
            requestRefresh(); 
            cardLayout.show(mainContentPanel, "HOME");
        });
        btnAdd.addActionListener(e -> {
            mainContentPanel.add(createUploadPanel(), "UPLOAD"); 
            cardLayout.show(mainContentPanel, "UPLOAD");
        });
        // [추가] 채팅 탭 클릭 시 목록 요청
        btnChat.addActionListener(e -> {
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
    // [Listener] 서버 메시지 수신
    // ===============================================================
    // MainFrame.java -> startListener 메소드

    private void startListener() {
        try {
            while (true) {
                String msg = in.readUTF();
                
                // 일단 명령어(헤더)만 확인하기 위해 앞부분만 살짝 자름
                String[] basicParts = msg.split("@@");
                String command = basicParts[0];

                // 1. 피드 데이터 (통째로 가져오기)
                if (command.equals("REFRESH_DATA")) {
                    String rawData = "";
                    int splitIndex = msg.indexOf("@@");
                    if (splitIndex != -1) {
                        rawData = msg.substring(splitIndex + 2); 
                    }
                    updateFeedUI(rawData);
                }
                
                // 2. 업로드 성공
                else if (command.equals("UPLOAD_SUCCESS")) {
                    JOptionPane.showMessageDialog(null, "업로드 완료!");
                    requestRefresh();
                    cardLayout.show(mainContentPanel, "HOME");
                }
                
                // 3. 실시간 채팅 수신
                else if (command.equals("CHAT_MSG")) {
                    // CHAT_MSG@@보낸사람@@내용
                    // 이건 내용이 짧으니까 split 써도 되지만, 내용에 @@가 있을 수 있으니 안전하게 substring 추천
                    // 하지만 기존 로직 유지 (간단한 대화)
                    if (basicParts.length >= 3) {
                        String senderId = basicParts[1];
                        String text = basicParts[2];
                        
                        ChatFrame chatRoom = chatRooms.get(senderId);
                        if (chatRoom == null) {
                            chatRoom = new ChatFrame(myId, senderId, this);
                            chatRooms.put(senderId, chatRoom);
                        }
                        chatRoom.setVisible(true);
                        chatRoom.receiveMessage(text);
                    }
                }
                
                // 4. 채팅 목록 수신 (통째로 가져오기)
                else if (command.equals("CHAT_LIST_DATA")) {
                    String rawData = "";
                    int splitIndex = msg.indexOf("@@");
                    if (splitIndex != -1) {
                        rawData = msg.substring(splitIndex + 2);
                    }
                    updateChatListUI(rawData);
                }
                
                // 5. [수정됨] 과거 대화 기록 수신 (여기가 문제였음!)
                else if (command.equals("HISTORY_DATA")) {
                    // 구조: HISTORY_DATA@@상대방ID@@기록내용...
                    // 상대방ID는 basicParts[1]에 있음.
                    if (basicParts.length >= 2) {
                        String targetUser = basicParts[1];
                        
                        // "HISTORY_DATA@@상대방ID@@" 그 뒤에 있는 진짜 데이터를 꺼내야 함
                        // 헤더 만들기
                        String prefix = "HISTORY_DATA@@" + targetUser + "@@";
                        
                        String history = "";
                        if (msg.startsWith(prefix)) {
                            history = msg.substring(prefix.length()); // 헤더 길이만큼 자르고 뒷부분 다 가져옴
                        }
                        
                        openChatWithHistory(targetUser, history);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("서버 연결 끊김");
        }
    }

    // 서버 요청 메소드들
    public void requestRefresh() { try { out.writeUTF("REFRESH"); } catch (Exception e) {} }
    public void sendChatMessage(String targetId, String msg) { try { out.writeUTF("CHAT@@" + targetId + "@@" + msg); } catch (Exception e) {} }

    // ===============================================================
    // [UI Update] 데이터로 화면 그리기
    // ===============================================================
    
    private void updateFeedUI(String rawData) {
        feedListPanel.removeAll();
        if (rawData.length() > 0) {
            String[] posts = rawData.split("///");
            for (String postStr : posts) {
                String[] parts = postStr.split("@@");
                if (parts.length >= 3) {
                    String uid = parts[0];
                    String content = parts[1];
                    String imgPath = parts[2];
                    if(imgPath.equals("null")) imgPath = null;
                    
                    Post p = new Post(uid, imgPath, content);
                    feedListPanel.add(createPostItem(p));
                    feedListPanel.add(Box.createVerticalStrut(20));
                }
            }
        }
        feedListPanel.revalidate();
        feedListPanel.repaint();
    }

    // [추가] 채팅 목록 그리기
    private void updateChatListUI(String rawData) {
        if(chatListPanel == null) return;
        chatListPanel.removeAll();
        Theme t = themeList.get(currentThemeIndex); 
        chatListPanel.setBackground(t.bgMain);

        if (rawData.length() > 0) {
            String[] users = rawData.split("///");
            for (String user : users) {
                JButton userBtn = new JButton("💬 " + user + "님과의 대화");
                userBtn.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
                userBtn.setBorderPainted(false);
                userBtn.setContentAreaFilled(false);
                userBtn.setForeground(t.textMain);
                userBtn.setHorizontalAlignment(SwingConstants.LEFT);
                
                // 버튼 누르면 대화 기록 요청
                userBtn.addActionListener(e -> {
                    try { out.writeUTF("GET_HISTORY@@" + user); } catch(Exception ex){}
                });
                chatListPanel.add(userBtn);
                chatListPanel.add(Box.createVerticalStrut(10));
            }
        } else {
            JLabel l = new JLabel("대화 내역이 없습니다.");
            l.setForeground(t.textMain);
            chatListPanel.add(l);
        }
        chatListPanel.revalidate();
        chatListPanel.repaint();
    }

    // [추가] 기록 받아서 채팅창 열기
    private void openChatWithHistory(String targetUser, String historyData) {
        ChatFrame chatRoom = chatRooms.get(targetUser);
        if (chatRoom == null) {
            chatRoom = new ChatFrame(myId, targetUser, this);
            chatRooms.put(targetUser, chatRoom);
        }
        
        // 기존 내용 지우고 DB 내용으로 채우기
        chatRoom.clearArea();
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

    // ===============================================================
    // UI Panels
    // ===============================================================
    
    private JPanel createHomePanel() {
        feedListPanel = new JPanel();
        feedListPanel.setLayout(new BoxLayout(feedListPanel, BoxLayout.Y_AXIS));
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
        JPanel panel = new JPanel(null);
        panel.setBackground(t.bgMain); 

        JLabel label = new JLabel("새 게시물 작성");
        label.setBounds(130, 20, 200, 30);
        label.setForeground(t.textMain);
        panel.add(label);

        JButton imgBtn = new JButton("사진 선택");
        imgBtn.setBounds(50, 70, 300, 200);
        imgBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("이미지", "jpg", "png");
            fc.setFileFilter(filter);
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                selectedImagePath = fc.getSelectedFile().getAbsolutePath();
                ImageIcon icon = new ImageIcon(selectedImagePath);
                imgBtn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(300, 200, Image.SCALE_SMOOTH)));
            }
        });
        panel.add(imgBtn);

        JTextArea contentArea = new JTextArea();
        contentArea.setBounds(50, 290, 300, 100);
        contentArea.setBorder(new LineBorder(Color.GRAY));
        panel.add(contentArea);

        JButton uploadBtn = new JButton("공유하기");
        uploadBtn.setBounds(50, 410, 300, 40);
        uploadBtn.addActionListener(e -> {
            String safeImg = (selectedImagePath == null) ? "null" : selectedImagePath;
            try { out.writeUTF("UPLOAD@@" + myId + "@@" + contentArea.getText() + "@@" + safeImg); } catch(Exception ex) {}
        });
        panel.add(uploadBtn);
        return panel;
    }

    // [추가] 채팅 목록 탭 화면
    private JPanel createChatListPanel() {
        Theme t = themeList.get(currentThemeIndex);
        chatListPanel = new JPanel();
        chatListPanel.setLayout(new BoxLayout(chatListPanel, BoxLayout.Y_AXIS));
        chatListPanel.setBackground(t.bgMain);
        
        // 서버에 목록 요청
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

    // [수정됨] 마이페이지 (프로필 디자인 + 채팅 버튼 포함)
    // [수정 완료] 마이페이지 생성 메소드
    private JPanel createMyPagePanel() {
        Theme t = themeList.get(currentThemeIndex);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(t.bgMain); 

        // 1. 상단 컨테이너 (프로필 + 통계)
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS)); // [수정됨]
        topContainer.setBackground(t.bgPanel);
        topContainer.setBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        // A. 프로필 정보 (사진 + 이름 + DM버튼)
        JPanel profileInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        profileInfo.setBackground(t.bgPanel);

        JLabel profileImg = new JLabel("프사", SwingConstants.CENTER);
        profileImg.setPreferredSize(new Dimension(80, 80));
        profileImg.setOpaque(true);
        profileImg.setBackground(Color.LIGHT_GRAY); 
        profileImg.setBorder(new LineBorder(Color.GRAY));
        
        // 이름과 버튼을 담을 패널
        JPanel namePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        namePanel.setBackground(t.bgPanel);
        
        JLabel nameLabel = new JLabel(myId);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        nameLabel.setForeground(t.textMain);
        
        // [친구 찾기 버튼]
        JButton chatBtn = new JButton("🔍 친구 찾기 / DM");
        chatBtn.setBackground(new Color(240, 240, 240));
        chatBtn.setFocusPainted(false);
        chatBtn.addActionListener(e -> {
             String target = JOptionPane.showInputDialog("대화할 상대방 ID를 입력하세요:");
             if(target != null && !target.isEmpty()) {
                 try { out.writeUTF("GET_HISTORY@@" + target); } catch(Exception ex){}
             }
        });
        
        namePanel.add(nameLabel);
        namePanel.add(chatBtn);
        
        profileInfo.add(profileImg);
        profileInfo.add(namePanel);
        
        // B. 통계 정보 (게시물/팔로워/팔로잉)
        JPanel statsPanel = new JPanel(new GridLayout(1, 3));
        statsPanel.setBackground(t.bgPanel);
        statsPanel.setBorder(new EmptyBorder(0, 20, 15, 20)); 
        
        statsPanel.add(createStatItem("게시물", "0", t));
        statsPanel.add(createStatItem("팔로워", "1.2K", t));
        statsPanel.add(createStatItem("팔로잉", "55", t));

        topContainer.add(profileInfo);
        topContainer.add(statsPanel);
        
        panel.add(topContainer, BorderLayout.NORTH);
        
        // 2. 하단 (내 게시물 그리드)
        JLabel gridPlaceholder = new JLabel("<html><center>📸<br>여기에 내 사진들이 표시됩니다.</center></html>", SwingConstants.CENTER);
        gridPlaceholder.setForeground(Color.GRAY);
        panel.add(gridPlaceholder, BorderLayout.CENTER);

        return panel;
    }

    // [추가] 통계 숫자 예쁘게 만드는 도우미 함수
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
        
        JButton userBtn = new JButton("👤 " + post.getUsername());
        userBtn.setBorderPainted(false); userBtn.setContentAreaFilled(false);
        userBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        userBtn.setForeground(t.textMain);
        userBtn.addActionListener(e -> {
            if(!post.getUsername().equals(myId)) {
                // 아이디 클릭 시 대화 기록 요청
                try { out.writeUTF("GET_HISTORY@@" + post.getUsername()); } catch(Exception ex){}
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
        JPanel top = (JPanel)getContentPane().getComponent(0);
        top.setBackground(t.bgPanel);
        JPanel bot = (JPanel)getContentPane().getComponent(2);
        bot.setBackground(t.bgPanel);
        
        // 하단 탭 버튼 글자색은 기본적으로 검정이니 패널 색만 변경
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