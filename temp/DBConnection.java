import java.sql.*;

public class DBConnection {
    // MySQL 
    private static final String URL = "jdbc:mysql://localhost:3306/hongstagram?serverTimezone=UTC";
    private static final String USER = "root";  
    private static final String PASSWORD = "ko@1477885";

    // 1. DB 연결 객체 가져오기
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.out.println("🚨 DB 연결 실패: 라이브러리가 없거나 비번이 틀림");
            e.printStackTrace();
            return null;
        }
    }

    // 2. 로그인 확인 기능
    public static String loginCheck(String uid, String upw) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String resultName = null;

        try {
            conn = getConnection();
            String sql = "SELECT name FROM users WHERE uid = ? AND upw = ?";
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, uid);
            pstmt.setString(2, upw);
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                resultName = rs.getString("name");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if(rs!=null) rs.close(); if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){}
        }
        
        return resultName;
    }

    // 3. 게시물 저장하기 (UPLOAD)
    public static boolean uploadPost(String uid, String content, String imagePath) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            String sql = "INSERT INTO posts (writer_id, content, image_path) VALUES (?, ?, ?)";
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, uid);
            pstmt.setString(2, content);
            pstmt.setString(3, imagePath); 
            
            int count = pstmt.executeUpdate(); 
            return (count > 0);
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){}
        }
    }

    // 4. 모든 게시물 가져오기 (REFRESH)
    public static String getAllPosts() {
        System.out.println("📢 [DB] 최신 getAllPosts 코드 실행됨! 구분자 @@ 사용");
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder sb = new StringBuilder();

        try {
            conn = getConnection();
            String sql = "SELECT writer_id, content, image_path FROM posts ORDER BY pid DESC";
            
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String uid = rs.getString("writer_id");
                String content = rs.getString("content");
                String img = rs.getString("image_path");
                if(img == null) img = "null"; 

                // [중요] @@ 구분자 적용 완료
                sb.append(uid).append("@@").append(content).append("@@").append(img).append("///");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if(rs!=null) rs.close(); if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){}
        }
        
        return sb.toString();
    }

    // 5. 회원가입 (JOIN)
    public static boolean joinUser(String uid, String upw, String name) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            String sql = "INSERT INTO users (uid, upw, name) VALUES (?, ?, ?)";
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, uid);
            pstmt.setString(2, upw);
            pstmt.setString(3, name);
            
            int count = pstmt.executeUpdate();
            return (count > 0);
            
        } catch (Exception e) {
            System.out.println("회원가입 실패 (아이디 중복 등)");
            return false;
        } finally {
            try { if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){}
        }
    }
    // ... 기존 코드 아래에 추가 ...

    // 6. 채팅 메시지 저장 (DB에 기록)
    public static void saveMessage(String sender, String receiver, String msg) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            String sql = "INSERT INTO messages (sender, receiver, msg) VALUES (?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, msg);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
        finally { try { if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){} }
    }

    // 7. 특정인과의 대화 기록 가져오기 (채팅방 열 때 사용)
    public static String getChatHistory(String me, String other) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder sb = new StringBuilder();
        try {
            conn = getConnection();
            // 나와 상대방이 주고받은 모든 메시지 (시간순)
            String sql = "SELECT sender, msg FROM messages " +
                         "WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?) " +
                         "ORDER BY mid ASC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, me); pstmt.setString(2, other);
            pstmt.setString(3, other); pstmt.setString(4, me);
            
            rs = pstmt.executeQuery();
            while(rs.next()) {
                // 보낸사람@@내용///
                sb.append(rs.getString("sender")).append("@@")
                  .append(rs.getString("msg")).append("///");
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { try { if(rs!=null) rs.close(); if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){} }
        return sb.toString();
    }

    // 8. 대화 목록 가져오기 (채팅 탭용)
    public static String getChatList(String myId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder sb = new StringBuilder();
        try {
            conn = getConnection();
            // 나랑 대화한 적 있는 사람들 (중복 제거)
            String sql = "SELECT DISTINCT sender FROM messages WHERE receiver = ? " +
                         "UNION " +
                         "SELECT DISTINCT receiver FROM messages WHERE sender = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, myId);
            pstmt.setString(2, myId);
            
            rs = pstmt.executeQuery();
            while(rs.next()) {
                String user = rs.getString(1);
                if (!user.equals(myId)) { // 내 이름은 제외
                    sb.append(user).append("///");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { try { if(rs!=null) rs.close(); if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){} }
        return sb.toString();
    }
}