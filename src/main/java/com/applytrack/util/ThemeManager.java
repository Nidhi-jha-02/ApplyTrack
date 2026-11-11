package com.applytrack.util;

import com.applytrack.model.enums.AppTheme;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Global Theme Manager utility dynamically applying Light and Dark stylesheets across JavaFX scenes.
 *
 * Demonstrates:
 * - Static Helper Methods (Unit 2)
 * - JavaFX Stylesheet manipulation & Resource resolution (Unit 3)
 * - Safe null checks & fallback logic (Unit 1 & Unit 3)
 */
public class ThemeManager {

    private static final String BASE_CSS = "/com/applytrack/css/styles.css";
    private static final String DARK_CSS = "/com/applytrack/css/dark-theme.css";

    private static AppTheme currentTheme = AppTheme.LIGHT;

    private ThemeManager() {}

    public static AppTheme getCurrentTheme() {
        return currentTheme;
    }

    public static void setCurrentTheme(AppTheme theme) {
        currentTheme = (theme != null) ? theme : AppTheme.LIGHT;
    }

    /**
     * Applies the specified theme to the given Scene.
     */
    public static void applyTheme(Scene scene, AppTheme theme) {
        if (scene == null) return;

        currentTheme = (theme != null) ? theme : AppTheme.LIGHT;

        URL baseCssUrl = ThemeManager.class.getResource(BASE_CSS);
        if (baseCssUrl != null) {
            String baseCss = baseCssUrl.toExternalForm();
            if (!scene.getStylesheets().contains(baseCss)) {
                scene.getStylesheets().add(baseCss);
            }
        }

        URL darkCssUrl = ThemeManager.class.getResource(DARK_CSS);
        if (darkCssUrl != null) {
            String darkCss = darkCssUrl.toExternalForm();
            if (currentTheme == AppTheme.DARK) {
                if (!scene.getStylesheets().contains(darkCss)) {
                    scene.getStylesheets().add(darkCss);
                }
            } else {
                scene.getStylesheets().remove(darkCss);
            }
        }
    }

    /**
     * Re-applies the currently configured theme to the scene.
     */
    public static void applyCurrentTheme(Scene scene) {
        applyTheme(scene, currentTheme);
    }

    /**
     * Applies the current theme to the stage's active scene.
     */
    public static void applyCurrentTheme(Stage stage) {
        if (stage != null && stage.getScene() != null) {
            applyCurrentTheme(stage.getScene());
        }
    }
}

