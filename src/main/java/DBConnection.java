import java.sql.*;
import java.util.ArrayList;

public class DBConnection {
    // MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/hongstagram?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
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

    // 테이블 초기화 (최초 실행 시)
    static {
        init();
    }

    public static void init() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();

            // likes 테이블
            String sql1 = "CREATE TABLE IF NOT EXISTS likes (" +
                    "lid INT AUTO_INCREMENT PRIMARY KEY, " +
                    "pid INT NOT NULL, " +
                    "uid VARCHAR(50) NOT NULL, " +
                    "UNIQUE(pid, uid), " +
                    "FOREIGN KEY (pid) REFERENCES posts(pid) ON DELETE CASCADE, " +
                    "FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE) DEFAULT CHARSET=utf8mb4";
            stmt.executeUpdate(sql1);

            // comments 테이블
            String sql2 = "CREATE TABLE IF NOT EXISTS comments (" +
                    "cid INT AUTO_INCREMENT PRIMARY KEY, " +
                    "pid INT NOT NULL, " +
                    "writer_id VARCHAR(50) NOT NULL, " +
                    "content TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (pid) REFERENCES posts(pid) ON DELETE CASCADE, " +
                    "FOREIGN KEY (writer_id) REFERENCES users(uid) ON DELETE CASCADE) DEFAULT CHARSET=utf8mb4";
            stmt.executeUpdate(sql2);

            // [자동 수정] 기존 테이블의 문자셋을 utf8mb4로 변환 (한글 깨짐 방지)
            // 외래키 제약조건 때문에 잠시 체크 해제 후 수행
            try {
                stmt.executeUpdate("SET FOREIGN_KEY_CHECKS=0");
                stmt.executeUpdate("ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4");
                stmt.executeUpdate("ALTER TABLE posts CONVERT TO CHARACTER SET utf8mb4");
                stmt.executeUpdate("ALTER TABLE comments CONVERT TO CHARACTER SET utf8mb4");
                stmt.executeUpdate("ALTER TABLE messages CONVERT TO CHARACTER SET utf8mb4");
                stmt.executeUpdate("ALTER TABLE chat_rooms CONVERT TO CHARACTER SET utf8mb4");
                stmt.executeUpdate("SET FOREIGN_KEY_CHECKS=1");

                // [프로필 이미지 업데이트]
                try {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN profile_image VARCHAR(500)");
                } catch (Exception e) {
                    // 이미 컬럼이 존재하면 에러 발생하므로 무시
                }
            } catch (Exception e) {
                // 테이블이 없거나 권한 부족 시 넘어감
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
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
            try {
                if (rs != null)
                    rs.close();
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
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
            try {
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
    }

    // 모든 게시물 가져오기 (REFRESH) -> 좋아요/댓글 포함
    public static String getAllPosts(String myId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder sb = new StringBuilder();

        try {
            conn = getConnection();
            // 좋아요 수와 내가 좋아요 했는지 여부도 함께 조회
            String sql = "SELECT p.pid, p.writer_id, u.name, u.profile_image, p.content, p.image_path, " +
                    "(SELECT COUNT(*) FROM likes WHERE pid = p.pid) as like_cnt, " +
                    "(SELECT COUNT(*) FROM likes WHERE pid = p.pid AND uid = ?) as liked_by_me " +
                    "FROM posts p " +
                    "JOIN users u ON p.writer_id = u.uid " +
                    "ORDER BY p.pid DESC";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, myId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                appendPostDataToStringBuilder(rs, sb);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return sb.toString();
    }

    // 특정 유저의 게시물만 가져오기
    public static String getPostsByUserId(String myId, String targetId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder sb = new StringBuilder();

        try {
            conn = getConnection();
            String sql = "SELECT p.pid, p.writer_id, u.name, u.profile_image, p.content, p.image_path, " +
                    "(SELECT COUNT(*) FROM likes WHERE pid = p.pid) as like_cnt, " +
                    "(SELECT COUNT(*) FROM likes WHERE pid = p.pid AND uid = ?) as liked_by_me " +
                    "FROM posts p " +
                    "JOIN users u ON p.writer_id = u.uid " +
                    "WHERE p.writer_id = ? " +
                    "ORDER BY p.pid DESC";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, myId);
            pstmt.setString(2, targetId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                appendPostDataToStringBuilder(rs, sb);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, rs);
        }

        return sb.toString();
    }

    // 전체 유저 수 조회
    public static int getUserCount() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int count = 0;
        try {
            conn = getConnection();
            String sql = "SELECT COUNT(*) FROM users";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return count;
    }

    private static void appendPostDataToStringBuilder(ResultSet rs, StringBuilder sb) throws SQLException {
        int pid = rs.getInt("pid");
        String uid = rs.getString("writer_id");
        String name = rs.getString("name");
        String uImg = rs.getString("profile_image"); // User Profile Image
        if (uImg == null)
            uImg = "null";
        String content = rs.getString("content");
        String img = rs.getString("image_path");
        int likeCnt = rs.getInt("like_cnt");
        int likedByMeVal = rs.getInt("liked_by_me"); // 1이면 true
        boolean isLiked = (likedByMeVal > 0);

        if (img == null)
            img = "null";

        // 댓글 가져오기 (간단히 여기서 서브 쿼리 수행)
        ArrayList<String> comments = getCommentsList(pid);

        // 프로토콜 조립
        // pid@@uid@@name@@content@@img@@likeCnt@@isLiked@@[댓글들]///

        // 댓글 문자열: user1:내용1##user2:내용2...
        StringBuilder cmSb = new StringBuilder();
        if (comments.isEmpty()) {
            cmSb.append("EMPTY");
        } else {
            for (int i = 0; i < comments.size(); i++) {
                cmSb.append(comments.get(i));
                if (i < comments.size() - 1)
                    cmSb.append("##");
            }
        }

        sb.append(pid).append("@@")
                .append(uid).append("@@")
                .append(name).append("@@")
                .append(content).append("@@")
                .append(img).append("@@")
                .append(likeCnt).append("@@")
                .append(isLiked).append("@@")
                .append(cmSb.toString()).append("@@")
                .append(uImg).append("///"); // uImg appended at the end
    }

    private static void closeResources(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null)
                rs.close();
            if (pstmt != null)
                pstmt.close();
            if (conn != null)
                conn.close();
        } catch (Exception e) {
        }
    }

    // 댓글 리스트 가져오기 헬퍼
    public static ArrayList<String> getCommentsList(int pid) {
        ArrayList<String> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            // users 테이블과 조인하여 이름(name)을 가져옴
            String sql = "SELECT u.name, c.content " +
                    "FROM comments c " +
                    "JOIN users u ON c.writer_id = u.uid " +
                    "WHERE c.pid = ? " +
                    "ORDER BY c.created_at ASC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, pid);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                String content = rs.getString("content");
                // 작성자ID 대신 작성자이름을 보냄
                list.add(name + ":" + content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
        return list;
    }

    // 좋아요 토글
    public static boolean toggleLike(int pid, String uid) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean result = false;
        try {
            conn = getConnection();
            // 이미 좋아요 했는지 확인
            String checkSql = "SELECT lid FROM likes WHERE pid=? AND uid=?";
            pstmt = conn.prepareStatement(checkSql);
            pstmt.setInt(1, pid);
            pstmt.setString(2, uid);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                // 이미 있음 -> 삭제 (취소)
                pstmt.close();
                String delSql = "DELETE FROM likes WHERE pid=? AND uid=?";
                pstmt = conn.prepareStatement(delSql);
                pstmt.setInt(1, pid);
                pstmt.setString(2, uid);
                pstmt.executeUpdate();
                result = false; // 좋아요 취소됨
            } else {
                // 없음 -> 추가
                pstmt.close();
                String insSql = "INSERT INTO likes (pid, uid) VALUES (?, ?)";
                pstmt = conn.prepareStatement(insSql);
                pstmt.setInt(1, pid);
                pstmt.setString(2, uid);
                pstmt.executeUpdate();
                result = true; // 좋아요 됨
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
        return result;
    }

    // 댓글 달기
    public static boolean addComment(int pid, String uid, String content) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            String sql = "INSERT INTO comments (pid, writer_id, content) VALUES (?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, pid);
            pstmt.setString(2, uid);
            pstmt.setString(3, content);
            int cnt = pstmt.executeUpdate();
            return cnt > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
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
            try {
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
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
            pstmt.setString(1, me);
            pstmt.setString(2, other);
            pstmt.setString(3, other);
            pstmt.setString(4, me);

            rs = pstmt.executeQuery();
            while (rs.next()) {
                // 보낸사람@@내용///
                sb.append(rs.getString("sender")).append("@@")
                        .append(rs.getString("msg")).append("///");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
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
            while (rs.next()) {
                String uid = rs.getString(1);
                if (uid != null && !uid.equals(myId))
                    userSet.add(uid);
            }
            rs.close();
            pstmt.close();

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
                rs.close();
                pstmt.close();
            }

            String sql3 = "SELECT rm.room_id, cr.room_name " +
                    "FROM room_members rm " +
                    "JOIN chat_rooms cr ON rm.room_id = cr.room_id " +
                    "WHERE rm.user_id = ?";
            pstmt = conn.prepareStatement(sql3);
            pstmt.setString(1, myId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                int rId = rs.getInt("room_id");
                String rName = rs.getString("room_name");

                // 형식: GROUP_방번호(방이름)
                // 예: GROUP_1(캡스톤회의방)
                sb.append("GROUP_").append(rId).append("(").append(rName).append(")").append("///");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }

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
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException ex) {
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
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
            while (rs.next()) {
                sb.append(rs.getString("uid")).append("///");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
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
            if (rs.next())
                roomId = rs.getInt(1);

            // 멤버들 추가
            String sql2 = "INSERT INTO room_members (room_id, user_id) VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql2);

            // 방장 먼저 추가
            pstmt.setInt(1, roomId);
            pstmt.setString(2, creator);
            pstmt.addBatch();

            // 나머지 멤버 추가 (방장은 제외!)
            String[] memberArr = membersStr.split(",");
            for (String m : memberArr) {
                String memberId = m.trim();
                // [핵심 수정] 방장이거나 빈 문자열이면 건너뜀
                if (!memberId.isEmpty() && !memberId.equals(creator)) {
                    pstmt.setInt(1, roomId);
                    pstmt.setString(2, memberId);
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();

            conn.commit(); // 저장 확정
        } catch (Exception e) {
            try {
                if (conn != null)
                    conn.rollback();
            } catch (Exception ex) {
            }
            e.printStackTrace();
            roomId = -1;
        } finally {
            try {
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
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
            while (rs.next())
                list.add(rs.getString(1));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
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
            while (rs.next()) {
                // 보낸사람@@내용///
                sb.append(rs.getString("sender")).append("@@")
                        .append(rs.getString("msg")).append("///");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
        return sb.toString();
    }

    // 비밀번호 변경
    public static boolean updatePassword(String uid, String newPw) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            String sql = "UPDATE users SET upw = ? WHERE uid = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newPw);
            pstmt.setString(2, uid);

            int count = pstmt.executeUpdate();
            return (count > 0);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
    }

    // 프로필 사진 변경
    public static boolean updateProfileImage(String uid, String path) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            String sql = "UPDATE users SET profile_image = ? WHERE uid = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, path);
            pstmt.setString(2, uid);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
    }

    // 내 정보 가져오기 (프로필 이미지 등)
    public static String getUserInfo(String uid) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String result = "";
        try {
            conn = getConnection();
            String sql = "SELECT name, profile_image FROM users WHERE uid = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, uid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                String img = rs.getString("profile_image");
                if (img == null)
                    img = "null";
                result = name + "@@" + img;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
                if (pstmt != null)
                    pstmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
        return result;
    }
}