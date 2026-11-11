package com.applytrack.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Handles database configuration loading with secure credential management.
 *
 * Precedence Order:
 * 1. System Environment Variables (DB_URL, DB_USERNAME, DB_PASSWORD, DB_DRIVER)
 * 2. Local "db.properties" in the working directory
 * 3. Local ".env" file in the working directory
 * 4. Classpath resource "/config/db.properties"
 * 5. Sensible development fallbacks
 *
 * Demonstrates:
 * - Encapsulation (private fields, controlled getters) - Unit 3
 * - String operations & Sanitization - Unit 2
 * - Properties & File I/O - Unit 3
 * - Static Factory & Singleton Pattern - Unit 2/3
 * - Security: Passwords masked in logs & toString() - Unit 1/3
 */
public class DatabaseConfig {

    private static final String DEFAULT_URL =
            "jdbc:mysql://127.0.0.1:3306/applytrack_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    private final String url;
    private final String username;
    private final String password;
    private final String driverClassName;

    // Singleton configuration instance
    private static DatabaseConfig instance;

    /**
     * Parameterized Constructor (Encapsulation - Unit 3)
     */
    public DatabaseConfig(String url, String username, String password, String driverClassName) {
        this.url = (url != null && !url.trim().isEmpty()) ? url.trim() : DEFAULT_URL;
        this.username = (username != null && !username.trim().isEmpty()) ? username.trim() : DEFAULT_USERNAME;
        this.password = (password != null) ? password : DEFAULT_PASSWORD;
        this.driverClassName = (driverClassName != null && !driverClassName.trim().isEmpty())
                ? driverClassName.trim() : DEFAULT_DRIVER;
    }

    /**
     * Singleton accessor for global configuration
     */
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /**
     * Resets or reloads configuration (useful for testing)
     */
    public static synchronized void reload() {
        instance = load();
    }

    /**
     * Loads configuration following the precedence order.
     */
    public static DatabaseConfig load() {
        Properties props = new Properties();

        // 1. Try classpath resource
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/config/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {}

        // 2. Try local ".env" file in working directory
        File envFile = new File(".env");
        if (envFile.exists() && envFile.canRead()) {
            loadPropertiesFromFile(envFile, props);
        }

        // 3. Try local "db.properties" in working directory (overrides .env)
        File propsFile = new File("db.properties");
        if (propsFile.exists() && propsFile.canRead()) {
            loadPropertiesFromFile(propsFile, props);
        }

        // Extract values from properties or fallbacks
        String url = props.getProperty("db.url", props.getProperty("DB_URL", DEFAULT_URL));
        String username = props.getProperty("db.username", props.getProperty("DB_USERNAME",
                props.getProperty("DB_USER", DEFAULT_USERNAME)));
        String password = props.getProperty("db.password", props.getProperty("DB_PASSWORD", DEFAULT_PASSWORD));
        String driver = props.getProperty("db.driver", props.getProperty("DB_DRIVER", DEFAULT_DRIVER));

        // 4. System Environment variables have the highest precedence
        String envUrl = System.getenv("APPLYTRACK_DB_URL");
        if (envUrl == null) envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.trim().isEmpty()) url = envUrl;

        String envUser = System.getenv("APPLYTRACK_DB_USERNAME");
        if (envUser == null) envUser = System.getenv("DB_USERNAME");
        if (envUser == null) envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.trim().isEmpty()) username = envUser;

        String envPass = System.getenv("APPLYTRACK_DB_PASSWORD");
        if (envPass == null) envPass = System.getenv("DB_PASSWORD");
        if (envPass != null) password = envPass;

        String envDriver = System.getenv("APPLYTRACK_DB_DRIVER");
        if (envDriver == null) envDriver = System.getenv("DB_DRIVER");
        if (envDriver != null && !envDriver.trim().isEmpty()) driver = envDriver;

        return new DatabaseConfig(url, username, password, driver);
    }

    private static void loadPropertiesFromFile(File file, Properties props) {
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Warning: Unable to read config file " + file.getName() + ": " + e.getMessage());
        }
    }

    // Getters (Encapsulation)
    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    /**
     * Returns a safely masked version of the password for diagnostic logging.
     * Never prints cleartext passwords.
     */
    public String getMaskedPassword() {
        if (password == null || password.isEmpty()) {
            return "[EMPTY]";
        }
        return "****** (Length: " + password.length() + ")";
    }

    /**
     * Validates that essential configuration parameters are present.
     */
    public boolean isValid() {
        return url != null && !url.trim().isEmpty() &&
               username != null && !username.trim().isEmpty() &&
               driverClassName != null && !driverClassName.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password=" + getMaskedPassword() +
                ", driverClassName='" + driverClassName + '\'' +
                '}';
    }
}

