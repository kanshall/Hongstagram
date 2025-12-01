import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Login extends JFrame {
    private JTextField idText;
    private JPasswordField pwText;

    public Login() {
        setTitle("로그인");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container c = getContentPane();
        c.setLayout(null); 

        // 1. ID 
        JLabel idLabel = new JLabel("ID");
        idLabel.setBounds(50, 50, 80, 30);
        c.add(idLabel);

        idText = new JTextField();
        idText.setBounds(140, 50, 100, 30);
        c.add(idText);

        // 2. Password 
        JLabel pwLabel = new JLabel("Password");
        pwLabel.setBounds(50, 100, 80, 30);
        c.add(pwLabel);

        pwText = new JPasswordField(); 
        pwText.setBounds(140, 100, 100, 30);
        pwText.addActionListener(e -> tryLogin()); // 엔터키 처리
        c.add(pwText);

        // 3. 버튼들
        JButton loginBtn = new JButton("로그인");
        loginBtn.setBounds(50, 150, 90, 30);
        loginBtn.addActionListener(e -> tryLogin());
        c.add(loginBtn);
        
        // [추가] 회원가입 버튼
        JButton joinBtn = new JButton("회원가입");
        joinBtn.setBounds(150, 150, 90, 30);
        joinBtn.addActionListener(e -> openJoinDialog());
        c.add(joinBtn);

        setSize(300, 250);
        setLocationRelativeTo(null); 
        setVisible(true);
    }

    // [추가] 회원가입 대화상자 열기
    private void openJoinDialog() {
        JDialog d = new JDialog(this, "회원가입", true);
        d.setLayout(new FlowLayout());
        d.setSize(250, 200);
        d.setLocationRelativeTo(this);

        JTextField newId = new JTextField(15);
        JTextField newPw = new JTextField(15);
        JTextField newName = new JTextField(15);
        
        d.add(new JLabel("아이디")); d.add(newId);
        d.add(new JLabel("비밀번호")); d.add(newPw);
        d.add(new JLabel("닉네임")); d.add(newName);
        
        JButton okBtn = new JButton("가입하기");
        okBtn.addActionListener(e -> {
            // 서버에 회원가입 요청 보내기 (JOIN@@아이디@@비번@@이름)
            sendJoinRequest(newId.getText(), newPw.getText(), newName.getText(), d);
        });
        d.add(okBtn);
        d.setVisible(true);
    }

    private void sendJoinRequest(String uid, String upw, String name, JDialog dialog) {
        if(uid.isEmpty() || upw.isEmpty()) return;
        try {
            Socket s = new Socket("127.0.0.1", 9999);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());
            
            out.writeUTF("JOIN@@" + uid + "@@" + upw + "@@" + name);
            String res = in.readUTF();
            
            if(res.equals("JOIN_SUCCESS")) {
                JOptionPane.showMessageDialog(null, "가입 성공! 로그인 해주세요.");
                dialog.dispose(); // 가입창 닫기
            } else {
                JOptionPane.showMessageDialog(null, "가입 실패 (아이디 중복 등)");
            }
            s.close();
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void tryLogin() {
        String uid = idText.getText();
        String upw = new String(pwText.getPassword());
        if (uid.isEmpty() || upw.isEmpty()) return;

        try {
            Socket socket = new Socket("127.0.0.1", 9999); // 연결!
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF("LOGIN@@" + uid + "@@" + upw);
            String response = in.readUTF(); 

            if (response.startsWith("LOGIN_SUCCESS")) {
                String[] parts = response.split("@@");
                String nickname = parts[1];

                JOptionPane.showMessageDialog(null, "환영합니다, " + nickname + "님!");
                
                new MainFrame(uid, socket); 
                
                dispose(); 
            } else {
                JOptionPane.showMessageDialog(null, "로그인 실패!");
                socket.close(); // 실패했을 때만 닫게
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "서버 연결 실패!");
        }
    }

    public static void main(String[] args) {
        new Login();
    }
}