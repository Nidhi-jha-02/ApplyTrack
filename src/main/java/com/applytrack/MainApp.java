package com.applytrack;

import com.applytrack.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main application class extending JavaFX Application.
 * Entry point for the JavaFX runtime lifecycle.
 *
 * Demonstrates:
 * - Inheritance (extends Application) - Unit 3
 * - Method Overriding (start, stop) - Unit 3
 * - Secure launch flow: forces authentication before dashboard access - Unit 3
 */
public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        // Launch directly into the modern Login screen
        SceneNavigator.showLoginScreen(stage, null, null);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
