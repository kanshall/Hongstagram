import java.net.*;
import java.util.*; 

public class HongstagramServer {
    public static Vector<ClientHandler> allUsers = new Vector<>();
    public static HashMap<String, ClientHandler> onlineUsers = new HashMap<>();

    public static void main(String[] args) {
        // try-with-resources 문법 사용 (자동으로 close 해줌)
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            System.out.println("--- [홍스타그램 서버] 가동 시작 (Port: 9999) ---");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("👋 클라이언트 접속: " + socket.getInetAddress());
                
                // ClientHandler 클래스는 이제 별도 파일에 있으니 바로 호출 가능
                ClientHandler handler = new ClientHandler(socket);
                allUsers.add(handler);
                handler.start(); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    } 
} 