import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ClientHandler extends Thread {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String myId;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String msg = in.readUTF();
                System.out.println("받은 메시지: [" + msg + "]");

                String[] parts = msg.split("@@");
                if (parts.length < 1)
                    continue;
                String command = parts[0];

                // 로그인
                if (command.equals("LOGIN")) {
                    if (parts.length < 3) {
                        out.writeUTF("LOGIN_FAIL");
                        continue;
                    }
                    String uid = parts[1];
                    String upw = parts[2];

                    String userName = DBConnection.loginCheck(uid, upw);
                    if (userName != null) {
                        out.writeUTF("LOGIN_SUCCESS@@" + userName);
                        this.myId = uid;
                        HongstagramServer.onlineUsers.put(this.myId, this);
                        System.out.println("접속자 등록: " + this.myId);
                    } else {
                        out.writeUTF("LOGIN_FAIL");
                    }
                }

                // 업로드
                else if (command.equals("UPLOAD")) {
                    if (parts.length < 4) {
                        out.writeUTF("UPLOAD_FAIL");
                        continue;
                    }
                    String uid = parts[1];
                    String content = parts[2];
                    String imgPath = parts[3];

                    boolean isSuccess = DBConnection.uploadPost(uid, content, imgPath);
                    if (isSuccess)
                        out.writeUTF("UPLOAD_SUCCESS");
                    else
                        out.writeUTF("UPLOAD_FAIL");
                }

                // 새로고침
                else if (command.equals("REFRESH")) {
                    String allPosts = DBConnection.getAllPosts(this.myId);
                    out.writeUTF("REFRESH_DATA@@" + allPosts);
                }

                // 1:1 채팅
                else if (command.equals("CHAT")) {
                    String targetId = parts[1];
                    String chatMsg = parts[2];
                    DBConnection.saveMessage(this.myId, targetId, chatMsg);

                    ClientHandler target = HongstagramServer.onlineUsers.get(targetId);
                    if (target != null) {
                        target.sendMessage("CHAT_MSG@@" + this.myId + "@@" + chatMsg);
                    }
                }

                // 회원가입
                else if (command.equals("JOIN")) {
                    String uid = parts[1];
                    String upw = parts[2];
                    String name = parts[3];
                    boolean ok = DBConnection.joinUser(uid, upw, name);
                    out.writeUTF(ok ? "JOIN_SUCCESS" : "JOIN_FAIL");
                }

                // 채팅 목록 요청
                else if (command.equals("GET_CHAT_LIST")) {
                    String userList = DBConnection.getChatList(this.myId);
                    out.writeUTF("CHAT_LIST_DATA@@" + userList);
                }

                // 대화 기록 요청 (개인/그룹 분기)
                else if (command.equals("GET_HISTORY")) {
                    String targetId = parts[1];
                    String history = "";
                    if (targetId.startsWith("GROUP_")) {
                        try {
                            int rId = Integer.parseInt(targetId.substring(6));
                            history = DBConnection.getGroupChatHistory(rId);
                        } catch (Exception e) {
                        }
                    } else {
                        history = DBConnection.getChatHistory(this.myId, targetId);
                    }
                    out.writeUTF("HISTORY_DATA@@" + targetId + "@@" + history);
                }

                // 비밀번호 변경
                else if (command.equals("CHANGE_PW")) {
                    String newPw = parts[1];
                    boolean isSuccess = DBConnection.updatePassword(this.myId, newPw);
                    if (isSuccess)
                        out.writeUTF("CHANGE_PW_SUCCESS");
                    else
                        out.writeUTF("CHANGE_PW_FAIL");
                }

                // 계정 탈퇴
                else if (command.equals("DELETE_USER")) {
                    boolean isDeleted = DBConnection.deleteUser(this.myId);
                    if (isDeleted) {
                        out.writeUTF("DELETE_SUCCESS");
                        HongstagramServer.onlineUsers.remove(this.myId);
                        break;
                    } else {
                        out.writeUTF("DELETE_FAIL");
                    }
                }

                // 로그아웃
                else if (command.equals("LOGOUT")) {
                    System.out.println("로그아웃: " + myId);
                    break;
                }

                // 전체 유저 목록 요청
                else if (command.equals("GET_ALL_USERS")) {
                    String users = DBConnection.getAllUsers(this.myId);
                    out.writeUTF("ALL_USERS_DATA@@" + users);
                }

                // 방 만들기 요청
                else if (command.equals("CREATE_GROUP")) {
                    String roomName = parts[1];
                    String membersStr = parts[2]; // "철수,영희,민수"

                    int roomId = DBConnection.createGroupRoom(roomName, this.myId, membersStr);

                    if (roomId != -1) {
                        out.writeUTF("GROUP_CREATED@@" + roomId + "@@" + roomName);

                        // 초대된 멤버들에게 실시간 갱신
                        ArrayList<String> allMembers = DBConnection.getRoomMembers(roomId);

                        for (String memberId : allMembers) {
                            if (memberId.equals(this.myId))
                                continue;

                            // 접속해 있는 멤버 찾기
                            ClientHandler target = HongstagramServer.onlineUsers.get(memberId);
                            if (target != null) {
                                // 상대방 목록 새로고침
                                String newList = DBConnection.getChatList(memberId);
                                target.sendMessage("CHAT_LIST_DATA@@" + newList);
                                System.out.println("🔔 초대 알림 전송: " + memberId);
                            }
                        }
                    } else {
                        out.writeUTF("GROUP_FAIL");
                    }
                }

                // 그룹 메시지 전송
                else if (command.equals("GROUP_MSG")) {
                    int roomId = Integer.parseInt(parts[1]);
                    String groupMsg = parts[2]; // 변수명 충돌 방지

                    DBConnection.saveGroupMessage(this.myId, roomId, groupMsg);

                    ArrayList<String> members = DBConnection.getRoomMembers(roomId);
                    for (String memberId : members) {
                        ClientHandler target = HongstagramServer.onlineUsers.get(memberId);
                        if (target != null) {
                            target.sendMessage("GROUP_CHAT_MSG@@" + roomId + "@@" + this.myId + "@@" + groupMsg);
                        }
                    }
                }

                // 좋아요
                else if (command.equals("LIKE")) {
                    int pid = Integer.parseInt(parts[1]);
                    boolean result = DBConnection.toggleLike(pid, this.myId);
                    // 갱신된 데이터를 다시 보내주는 게 좋음 (단순 성공 여부보다)
                    // 여기서는 간단히 성공 메시지만 혹은 전체 리프레시 유도
                    if (result)
                        out.writeUTF("LIKE_SUCCESS@@Liket");
                    else
                        out.writeUTF("LIKE_SUCCESS@@Unliked");
                }

                // 댓글
                else if (command.equals("COMMENT")) {
                    int pid = Integer.parseInt(parts[1]);
                    String content = parts[2];
                    boolean ok = DBConnection.addComment(pid, this.myId, content);
                    if (ok)
                        out.writeUTF("COMMENT_SUCCESS");
                    else
                        out.writeUTF("COMMENT_FAIL");
                }

                // 프로필 사진 변경
                else if (command.equals("UPDATE_PROFILE_IMAGE")) {
                    String path = parts[1];
                    boolean ok = DBConnection.updateProfileImage(this.myId, path);
                    if (ok)
                        out.writeUTF("UPDATE_PROFILE_SUCCESS");
                    else
                        out.writeUTF("UPDATE_PROFILE_FAIL");
                }

                // 내 프로필 정보 요청
                else if (command.equals("GET_MY_PROFILE")) {
                    String info = DBConnection.getUserInfo(this.myId);
                    // 통계용 유저 수 (전체 유저 - 1)
                    int totalUsers = DBConnection.getUserCount();
                    int followerCount = Math.max(0, totalUsers - 1);

                    // 정보: 이름@@경로
                    // 새 프로토콜: 이름@@경로@@팔로워수
                    out.writeUTF("MY_PROFILE_DATA@@" + info + "@@" + followerCount);
                }

                // 내 게시물 요청 (마이페이지용)
                else if (command.equals("GET_MY_POSTS")) {
                    // 내 아이디로 조회
                    String posts = DBConnection.getPostsByUserId(this.myId, this.myId);
                    out.writeUTF("MY_POSTS_DATA@@" + posts);
                }
            }
        } catch (Exception e) {
            System.out.println("비정상 종료: " + myId);
        } finally {
            if (myId != null)
                HongstagramServer.onlineUsers.remove(myId);
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
            }
        }
    }
}