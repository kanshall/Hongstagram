
import javafx.scene.Scene;
import javafx.stage.Window;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class ThemeManager {
    private static ThemeManager instance;
    private String currentTheme = "LIGHT";
    private static final String CONFIG_FILE = "config.properties";

    private ThemeManager() {
        loadConfig();
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(String themeName) {
        this.currentTheme = themeName;
        saveConfig();
        applyThemeToAllOpenWindows();
    }

    // Apply to a specific Scene (used by App.java)
    public void applyTheme(Scene scene) {
        if (scene == null)
            return;
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        if (currentTheme.equalsIgnoreCase("DARK")) {
            scene.getStylesheets().add(getClass().getResource("theme_dark.css").toExternalForm());
        } else if (currentTheme.equalsIgnoreCase("HONGIK")) {
            scene.getStylesheets().add(getClass().getResource("theme_hongik.css").toExternalForm());
        }
    }

    // Apply to a specific Parent logic implementation if needed, but usually we
    // apply to Scene.
    // However, if a Parent is not yet in a Scene, we might need to wait or apply
    // when it's added.
    // For now, primary usage is on Scene.

    private void applyThemeToAllOpenWindows() {
        // This is a bit advanced, but for now we can just rely on the fact that
        // App.java might call applyTheme, or we iterate known windows.
        // In JavaFX, Window.getWindows() gives us all open stages.
        for (Window window : Window.getWindows()) {
            if (window.getScene() != null) {
                applyTheme(window.getScene());
            }
        }
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            currentTheme = props.getProperty("theme", "LIGHT");
        } catch (Exception e) {
            currentTheme = "LIGHT";
        }
    }

    private void saveConfig() {
        Properties props = new Properties();
        props.setProperty("theme", currentTheme);
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Hongstagram Config");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
