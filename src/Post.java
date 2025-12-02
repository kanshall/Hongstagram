public class Post {
    private String userId;    // 아이디 (로직용, 예: test1)
    private String userName;  // 이름 (표시용, 예: 철수)
    private String imagePath;
    private String content;
    
    public Post(String userId, String userName, String imagePath, String content) {
        this.userId = userId;
        this.userName = userName;
        this.imagePath = imagePath;
        this.content = content;
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getImagePath() { return imagePath; }
    public String getContent() { return content; }
}