import java.sql.*;
import java.util.ArrayList;

public class DBConnection {
    // MySQL 
    private static final String URL = "jdbc:mysql://localhost:3306/hongstagram?serverTimezone=UTC";
    private static final String USER = "root";  
    private static final String PASSWORD = "ko@1477885";

    // DB 연결 객체 가져오기
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.out.println("DB 연결 실패: 라이브러리가 없거나 비번이 틀림");
            e.printStackTrace();
            return null;
        }
    }

    // 로그인 확인 기능
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

    // 게시물 저장하기 (UPLOAD)
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

    // 모든 게시물 가져오기 (REFRESH)
    public static String getAllPosts() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder sb = new StringBuilder();

        try {
            conn = getConnection();
            // sers 테이블과 조인하여 이름(name)도 조회
            String sql = "SELECT p.writer_id, u.name, p.content, p.image_path " +
                         "FROM posts p " +
                         "JOIN users u ON p.writer_id = u.uid " +
                         "ORDER BY p.pid DESC";
            
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String uid = rs.getString("writer_id");
                String name = rs.getString("name"); // 이름 가져옴
                String content = rs.getString("content");
                String img = rs.getString("image_path");
                if(img == null) img = "null"; 

                // 순서: 아이디@@이름@@내용@@이미지
                sb.append(uid).append("@@")
                  .append(name).append("@@")
                  .append(content).append("@@")
                  .append(img).append("///");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if(rs!=null) rs.close(); if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){}
        }
        
        return sb.toString();
    }

    // 회원가입 (JOIN)
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
    // 채팅 메시지 저장 (DB에 기록)
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

    // 특정인과의 대화 기록 가져오기 (채팅방 열 때 사용)
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

    // 대화 목록 가져오기 (이름 및 방 제목 포함)
    public static String getChatList(String myId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder sb = new StringBuilder();
        
        java.util.HashSet<String> userSet = new java.util.HashSet<>();
        
        try {
            conn = getConnection();
            
            // 1:1 대화 상대
            String sql1 = "SELECT DISTINCT sender FROM messages WHERE receiver = ? UNION SELECT DISTINCT receiver FROM messages WHERE sender = ?";
            pstmt = conn.prepareStatement(sql1);
            pstmt.setString(1, myId);
            pstmt.setString(2, myId);
            rs = pstmt.executeQuery();
            while(rs.next()) {
                String uid = rs.getString(1);
                if (uid != null && !uid.equals(myId)) userSet.add(uid);
            }
            rs.close(); pstmt.close();

            // 1:1 대화 상대 이름 가져오기
            for (String uid : userSet) {
                String sql2 = "SELECT name FROM users WHERE uid = ?";
                pstmt = conn.prepareStatement(sql2);
                pstmt.setString(1, uid);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    String name = rs.getString("name");
                    sb.append(uid).append("(").append(name).append(")").append("///");
                } else {
                    sb.append(uid).append("(").append(uid).append(")").append("///");
                }
                rs.close(); pstmt.close();
            }

            String sql3 = "SELECT rm.room_id, cr.room_name " +
                          "FROM room_members rm " +
                          "JOIN chat_rooms cr ON rm.room_id = cr.room_id " +
                          "WHERE rm.user_id = ?";
            pstmt = conn.prepareStatement(sql3);
            pstmt.setString(1, myId);
            rs = pstmt.executeQuery();
            while(rs.next()) {
                int rId = rs.getInt("room_id");
                String rName = rs.getString("room_name");
                
                // 형식: GROUP_방번호(방이름)
                // 예: GROUP_1(캡스톤회의방)
                sb.append("GROUP_").append(rId).append("(").append(rName).append(")").append("///");
            }

        } catch (Exception e) { e.printStackTrace(); }
        finally { try { if(rs!=null) rs.close(); if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){} }
        
        return sb.toString();
    }

    // 계정 탈퇴
    public static boolean deleteUser(String uid) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // [중요] 트랜잭션 시작 (하나라도 실패하면 취소)

            // 내가 보낸/받은 메시지 삭제
            String sql1 = "DELETE FROM messages WHERE sender = ? OR receiver = ?";
            pstmt = conn.prepareStatement(sql1);
            pstmt.setString(1, uid);
            pstmt.setString(2, uid);
            pstmt.executeUpdate();
            pstmt.close();

            // 내가 쓴 게시물 삭제
            String sql2 = "DELETE FROM posts WHERE writer_id = ?";
            pstmt = conn.prepareStatement(sql2);
            pstmt.setString(1, uid);
            pstmt.executeUpdate();
            pstmt.close();

            // 채팅방 멤버 목록이 있다면 멤버 목록에서 삭제

            // 최종적으로 회원 정보 삭제
            String sql4 = "DELETE FROM users WHERE uid = ?";
            pstmt = conn.prepareStatement(sql4);
            pstmt.setString(1, uid);
            int result = pstmt.executeUpdate();

            if (result > 0) {
                conn.commit(); // 성공 시 반영
                return true;
            } else {
                conn.rollback(); // 실패 시 되돌리기
                return false;
            }

        } catch (Exception e) {
            try { if(conn != null) conn.rollback(); } catch(SQLException ex){}
            e.printStackTrace();
            return false;
        } finally {
            try { if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){}
        }
    }
    // 모든 유저 목록 가져오기 (자신 제외)
    public static String getAllUsers(String myId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder sb = new StringBuilder();
        try {
            conn = getConnection();
            String sql = "SELECT uid FROM users WHERE uid != ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, myId);
            rs = pstmt.executeQuery();
            while(rs.next()) {
                sb.append(rs.getString("uid")).append("///");
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { try { if(rs!=null) rs.close(); if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){} }
        return sb.toString();
    }

    // 그룹 방 만들기
    public static int createGroupRoom(String roomName, String creator, String membersStr) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int roomId = -1;
        
        try {
            conn = getConnection();
            conn.setAutoCommit(false); 

            // 방 목록에 추가
            String sql1 = "INSERT INTO chat_rooms (room_name, created_by) VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql1, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, roomName);
            pstmt.setString(2, creator);
            pstmt.executeUpdate();
            
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) roomId = rs.getInt(1); 

            // 멤버들 추가
            String sql2 = "INSERT INTO room_members (room_id, user_id) VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql2);
            
            // 방장 먼저 추가
            pstmt.setInt(1, roomId);
            pstmt.setString(2, creator);
            pstmt.addBatch();
            
            // 나머지 멤버 추가 (방장은 제외!)
            String[] memberArr = membersStr.split(",");
            for(String m : memberArr) {
                String memberId = m.trim();
                // [핵심 수정] 방장이거나 빈 문자열이면 건너뜀
                if(!memberId.isEmpty() && !memberId.equals(creator)) {
                    pstmt.setInt(1, roomId);
                    pstmt.setString(2, memberId);
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch(); 
            
            conn.commit(); // 저장 확정
        } catch (Exception e) {
            try { if(conn!=null) conn.rollback(); } catch(Exception ex){}
            e.printStackTrace();
            roomId = -1;
        } finally {
            try { if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){}
        }
        return roomId;
    }

    // 메시지 저장
    public static void saveGroupMessage(String sender, int roomId, String msg) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            // receiver는 null, room_id에 값 저장
            String sql = "INSERT INTO messages (sender, room_id, msg) VALUES (?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, sender);
            pstmt.setInt(2, roomId);
            pstmt.setString(3, msg);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
        finally { try { if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){} }
    }

    // 방 멤버 ID 리스트 가져오기 (전송용)
    public static ArrayList<String> getRoomMembers(int roomId) {
        ArrayList<String> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            String sql = "SELECT user_id FROM room_members WHERE room_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, roomId);
            rs = pstmt.executeQuery();
            while(rs.next()) list.add(rs.getString(1));
        } catch(Exception e) { e.printStackTrace(); }
        finally { try { if(rs!=null) rs.close(); if(pstmt!=null) pstmt.close(); if(conn!=null) conn.close(); } catch(Exception e){} }
        return list;
    }
    // 그룹 채팅 기록 가져오기
    public static String getGroupChatHistory(int roomId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder sb = new StringBuilder();
        try {
            conn = getConnection();
            // 해당 방(room_id)의 모든 메시지 시간순 정렬
            String sql = "SELECT sender, msg FROM messages WHERE room_id = ? ORDER BY mid ASC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, roomId);
            
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
}