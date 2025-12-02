import java.awt.Color;

public class Theme {
    String name;        // 테마 이름
    Color bgMain;       // 전체 배경색 (바닥)
    Color bgPanel;      // 게시물, 버튼 등 패널 배경색
    Color textMain;     // 기본 글자색
    Color point;        // 강조 포인트 색상 (제목 등)

    public Theme(String name, Color bgMain, Color bgPanel, Color textMain, Color point) {
        this.name = name;
        this.bgMain = bgMain;
        this.bgPanel = bgPanel;
        this.textMain = textMain;
        this.point = point;
    }
}