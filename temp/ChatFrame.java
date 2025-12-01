import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatFrame extends JFrame {
    private JTextArea textArea;
    private JTextField inputField;
    private String myId;
    private String targetId; 
    private MainFrame mainFrame; 

    public ChatFrame(String myId, String targetId, MainFrame mainFrame) {
        this.myId = myId;
        this.targetId = targetId;
        this.mainFrame = mainFrame;

        setTitle(targetId + "님과의 대화");
        setLayout(new BorderLayout());

        // 1. 대화 내용 (수정 불가)
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        // 2. 입력창
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        
        JButton sendBtn = new JButton("전송");
        
        ActionListener sendAction = e -> sendMessage();
        inputField.addActionListener(sendAction);
        sendBtn.addActionListener(sendAction);

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(350, 500);
        setLocationRelativeTo(null);
        // setVisible(true); // 생성하자마자 띄우지 않고 데이터 로딩 후 띄움
    }

    private void sendMessage() {
        String msg = inputField.getText();
        if (msg.isEmpty()) return;

        textArea.append("[나] " + msg + "\n");
        mainFrame.sendChatMessage(targetId, msg);
        inputField.setText(""); 
    }

    public void receiveMessage(String msg) {
        textArea.append("[" + targetId + "] " + msg + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
    
    // [추가] 화면 싹 비우기 (과거 기록 불러올 때 사용)
    public void clearArea() {
        textArea.setText("");
    }
    
    // [추가] 메시지 한 줄 추가 (DB 기록 넣을 때 사용)
    public void appendMsg(String msg) {
        textArea.append(msg);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
}