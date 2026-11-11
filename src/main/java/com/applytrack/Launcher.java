package com.applytrack;

/**
 * Launcher class to bypass JavaFX runtime component checks
 * when running from standard classpath or IDE execution.
 *
 * Demonstrates:
 * - Java Main Method and Entry Point (Unit 1)
 * - Static method delegation (Unit 2)
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}

