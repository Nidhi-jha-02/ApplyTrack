package com.applytrack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test verifying resource availability and classpath integrity.
 *
 * Demonstrates:
 * - Unit Testing (JUnit 5)
 * - Java Reflection and Resource loading (Unit 2 / Unit 3)
 */
public class MainAppTest {

    @Test
    @DisplayName("Verify Login.fxml exists in classpath")
    public void testLoginFxmlExists() {
        URL resource = getClass().getResource("/com/applytrack/view/Login.fxml");
        assertNotNull(resource, "Login.fxml must exist on classpath");
    }

    @Test
    @DisplayName("Verify Register.fxml exists in classpath")
    public void testRegisterFxmlExists() {
        URL resource = getClass().getResource("/com/applytrack/view/Register.fxml");
        assertNotNull(resource, "Register.fxml must exist on classpath");
    }

    @Test
    @DisplayName("Verify MainLayout.fxml exists in classpath")
    public void testMainLayoutFxmlExists() {
        URL resource = getClass().getResource("/com/applytrack/view/MainLayout.fxml");
        assertNotNull(resource, "MainLayout.fxml must exist on classpath");
    }

    @Test
    @DisplayName("Verify Dashboard.fxml exists in classpath")
    public void testDashboardFxmlExists() {
        URL resource = getClass().getResource("/com/applytrack/view/Dashboard.fxml");
        assertNotNull(resource, "Dashboard.fxml must exist on classpath");
    }

    @Test
    @DisplayName("Verify Applications.fxml exists in classpath")
    public void testApplicationsFxmlExists() {
        URL resource = getClass().getResource("/com/applytrack/view/Applications.fxml");
        assertNotNull(resource, "Applications.fxml must exist on classpath");
    }

    @Test
    @DisplayName("Verify ApplicationDialog.fxml exists in classpath")
    public void testApplicationDialogFxmlExists() {
        URL resource = getClass().getResource("/com/applytrack/view/ApplicationDialog.fxml");
        assertNotNull(resource, "ApplicationDialog.fxml must exist on classpath");
    }

    @Test
    @DisplayName("Verify CSS stylesheet exists in classpath")
    public void testStylesheetExists() {
        URL resource = getClass().getResource("/com/applytrack/css/styles.css");
        assertNotNull(resource, "styles.css must exist on classpath");
    }

    @Test
    @DisplayName("Verify MainApp class inherits from Application")
    public void testMainAppInheritance() {
        assertTrue(javafx.application.Application.class.isAssignableFrom(MainApp.class),
                "MainApp must extend javafx.application.Application (Unit 3 Inheritance)");
    }
}
