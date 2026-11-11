package com.applytrack.util;

import com.applytrack.MainApp;
import com.applytrack.controller.LoginController;
import com.applytrack.controller.RegisterController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Utility responsible for top-level scene switching and navigation in ApplyTrack.
 *
 * Demonstrates:
 * - Single Responsibility Principle (navigation logic decoupled from controllers) - Unit 3
 * - Static Helper Methods (Unit 2)
 * - Safe JavaFX Stage and Scene management - Unit 3
 */
public class SceneNavigator {

    private static final String CSS_PATH = "/com/applytrack/css/styles.css";

    private SceneNavigator() {}

    /**
     * Navigates the primary stage to the Login screen.
     */
    public static void showLoginScreen(Stage stage, String successMessage, String prefilledEmail) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource("/com/applytrack/view/Login.fxml"));
            Parent root = loader.load();

            LoginController controller = loader.getController();
            if (controller != null) {
                if (successMessage != null) {
                    controller.showSuccessBanner(successMessage);
                }
                if (prefilledEmail != null) {
                    controller.prefillEmail(prefilledEmail);
                }
            }

            setScene(stage, root, 1000, 650, "ApplyTrack — Candidate Sign In");

        } catch (IOException e) {
            System.err.println("Fatal: Failed to load Login.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigates the primary stage to the Registration screen.
     */
    public static void showRegisterScreen(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource("/com/applytrack/view/Register.fxml"));
            Parent root = loader.load();

            setScene(stage, root, 1000, 700, "ApplyTrack — Create Candidate Account");

        } catch (IOException e) {
            System.err.println("Fatal: Failed to load Register.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigates the primary stage to the Main Dashboard layout.
     */
    public static void showMainDashboard(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource("/com/applytrack/view/MainLayout.fxml"));
            Parent root = loader.load();

            setScene(stage, root, 1200, 780, "ApplyTrack — Job Application Intelligence System");

        } catch (IOException e) {
            System.err.println("Fatal: Failed to load MainLayout.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void setScene(Stage stage, Parent root, double width, double height, String title) {
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            Scene newScene = new Scene(root, width, height);
            attachCss(newScene);
            stage.setScene(newScene);
        } else {
            currentScene.setRoot(root);
            attachCss(currentScene);
        }

        stage.setTitle(title);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    private static void attachCss(Scene scene) {
        ThemeManager.applyCurrentTheme(scene);
    }
}

