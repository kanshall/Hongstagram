import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HongstagramServer {
    public static Vector<ClientHandler> allUsers = new Vector<>();
    public static ConcurrentHashMap<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            System.out.println("--- [홍스타그램 서버] 가동 시작 (Port: 9999) ---");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("클라이언트 접속: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);
                allUsers.add(handler);
                handler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}