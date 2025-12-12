import java.util.ArrayList;

public class Post {
    private int pid; // 게시물 ID
    private String userId; // 아이디 (로직용)
    private String userName; // 이름 (표시용)
    private String imagePath;
    private String content;
    private int likeCount;
    private boolean likedByMe;
    private ArrayList<String> comments; // "작성자Id:내용" 형태의 문자열 리스트

    private String userProfileImage;

    public Post(int pid, String userId, String userName, String userProfileImage, String imagePath, String content,
            int likeCount,
            boolean likedByMe, ArrayList<String> comments) {
        this.pid = pid;
        this.userId = userId;
        this.userName = userName;
        this.userProfileImage = userProfileImage;
        this.imagePath = imagePath;
        this.content = content;
        this.likeCount = likeCount;
        this.likedByMe = likedByMe;
        this.comments = comments;
    }

    public int getPid() {
        return pid;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserProfileImage() {
        return userProfileImage;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getContent() {
        return content;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public boolean isLikedByMe() {
        return likedByMe;
    }

    public ArrayList<String> getComments() {
        return comments;
    }
}