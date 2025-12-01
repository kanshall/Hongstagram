public class Post {
    private String username;
    private String imagePath;
    private String content;
    
    public Post(String username, String imagePath, String content) {
        this.username = username;
        this.imagePath = imagePath;
        this.content = content;
    }

    public String getUsername() { return username; }
    public String getImagePath() { return imagePath; }
    public String getContent() { return content; }
}