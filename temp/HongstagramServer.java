import java.io.*;
import java.net.*;
import java.util.*; 

public class HongstagramServer {
    public static Vector<ClientHandler> allUsers = new Vector<>();
    public static HashMap<String, ClientHandler> onlineUsers = new HashMap<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(9999);
            System.out.println("--- [홍스타그램 서버] 가동 시작 (Port: 9999) ---");

            while (true) { // 스레드
                Socket socket = serverSocket.accept();
                System.out.println("👋 클라이언트 접속: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket);
                allUsers.add(handler);
                handler.start(); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String myId; 

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void sendMessage(String msg) {
        try { out.writeUTF(msg); } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String msg = in.readUTF(); 
                System.out.println("📩 받은 메시지: [" + msg + "]");

                String[] parts = msg.split("@@"); 
                if (parts.length < 1) continue;
                String command = parts[0];

                // 1. 로그인
                if (command.equals("LOGIN")) {
                    if (parts.length < 3) { out.writeUTF("LOGIN_FAIL"); continue; }
                    String uid = parts[1];
                    String upw = parts[2];

                    String userName = DBConnection.loginCheck(uid, upw);
                    if (userName != null) {
                        out.writeUTF("LOGIN_SUCCESS@@" + userName);
                        this.myId = uid;
                        HongstagramServer.onlineUsers.put(this.myId, this);
                        System.out.println("✅ 접속자 등록: " + this.myId);
                    } else {
                        out.writeUTF("LOGIN_FAIL");
                    }
                }
                
                // 2. 업로드
                else if (command.equals("UPLOAD")) {
                    if (parts.length < 4) { out.writeUTF("UPLOAD_FAIL"); continue; }
                    String uid = parts[1];
                    String content = parts[2];
                    String imgPath = parts[3];
                    
                    boolean isSuccess = DBConnection.uploadPost(uid, content, imgPath);
                    if (isSuccess) {
                        out.writeUTF("UPLOAD_SUCCESS");
                        System.out.println("✅ 업로드 성공: " + uid);
                    } else {
                        out.writeUTF("UPLOAD_FAIL");
                    }
                }

                // 3. 새로고침
                else if (command.equals("REFRESH")) {
                    String allPosts = DBConnection.getAllPosts();
                    out.writeUTF("REFRESH_DATA@@" + allPosts);
                }

                // 4. 채팅 (저장 기능 포함!)
                else if (command.equals("CHAT")) {
                    String targetId = parts[1];
                    String chatMsg = parts[2];
                    
                    // [중요] DB에 먼저 저장!
                    DBConnection.saveMessage(this.myId, targetId, chatMsg);
                    System.out.println("💾 채팅 저장완료: " + this.myId + " -> " + targetId);

                    // 상대가 온라인이면 실시간 전송
                    ClientHandler target = HongstagramServer.onlineUsers.get(targetId);
                    if (target != null) {
                        target.sendMessage("CHAT_MSG@@" + this.myId + "@@" + chatMsg);
                    }
                }
                
                // 5. 회원가입
                else if (command.equals("JOIN")) {
                     String uid = parts[1];
                     String upw = parts[2];
                     String name = parts[3];
                     boolean ok = DBConnection.joinUser(uid, upw, name);
                     out.writeUTF(ok ? "JOIN_SUCCESS" : "JOIN_FAIL");
                }

                // 6. [추가] 대화 목록 요청 (채팅 탭)
                else if (command.equals("GET_CHAT_LIST")) {
                    String userList = DBConnection.getChatList(this.myId);
                    out.writeUTF("CHAT_LIST_DATA@@" + userList);
                }

                // 7. [추가] 지난 대화 기록 요청
                else if (command.equals("GET_HISTORY")) {
                    String targetId = parts[1];
                    String history = DBConnection.getChatHistory(this.myId, targetId);
                    out.writeUTF("HISTORY_DATA@@" + targetId + "@@" + history);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ 퇴장: " + myId);
            if(myId != null) HongstagramServer.onlineUsers.remove(myId);
        }
    }
}