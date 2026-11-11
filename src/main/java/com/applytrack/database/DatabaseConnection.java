package com.applytrack.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Reusable JDBC Connection Factory for ApplyTrack.
 *
 * Design Pattern:
 * Implements a connection provider factory rather than storing a single permanent
 * shared Connection object. This avoids stale connections, connection drops,
 * and threading conflicts.
 *
 * Demonstrates:
 * - Static Methods and Factory Pattern (Unit 2)
 * - Varargs for clean resource cleanup (Unit 2)
 * - JDBC Driver Loading (Class.forName) & DriverManager (Unit 3)
 * - Checked Exception Handling with descriptive diagnostics (Unit 3)
 */
public class DatabaseConnection {

    private static boolean driverLoaded = false;

    // Private constructor to prevent instantiation (Utility/Factory class)
    private DatabaseConnection() {}

    /**
     * Initializes and registers the JDBC driver once per JVM lifecycle.
     */
    private static synchronized void ensureDriverLoaded(String driverClassName) throws SQLException {
        if (!driverLoaded) {
            try {
                Class.forName(driverClassName);
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                throw new SQLException("JDBC Driver class not found: " + driverClassName +
                        ". Please verify mysql-connector-j dependency in pom.xml.", e);
            }
        }
    }

    /**
     * Obtains a fresh, valid Connection using the global configuration.
     * The caller is responsible for closing the connection (preferably using try-with-resources).
     *
     * @return A newly established java.sql.Connection
     * @throws SQLException If a connection error occurs
     */
    public static Connection getConnection() throws SQLException {
        return getConnection(DatabaseConfig.getInstance());
    }

    /**
     * Obtains a Connection using an explicitly provided DatabaseConfig (Overloading - Unit 2).
     *
     * @param config The database configuration to connect with
     * @return A newly established java.sql.Connection
     * @throws SQLException If authentication or network connection fails
     */
    public static Connection getConnection(DatabaseConfig config) throws SQLException {
        if (config == null || !config.isValid()) {
            throw new SQLException("Invalid database configuration provided.");
        }

        ensureDriverLoaded(config.getDriverClassName());

        try {
            return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
        } catch (SQLException e) {
            // Provide actionable beginner-friendly diagnostic message
            String errorMsg = buildDiagnosticMessage(e, config);
            throw new SQLException(errorMsg, e.getSQLState(), e.getErrorCode(), e);
        }
    }

    /**
     * Tests if a connection can be successfully established and validated.
     *
     * @return true if connection succeeds and is valid within 3 seconds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(3);
        } catch (SQLException e) {
            System.err.println("Database connectivity test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gracefully closes one or more AutoCloseable resources (Varargs - Unit 2).
     *
     * @param closeables One or more closeable resources (Connection, Statement, ResultSet)
     */
    public static void closeQuietly(AutoCloseable... closeables) {
        if (closeables == null) return;
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                    // Suppressed to prevent masking primary exceptions
                }
            }
        }
    }

    /**
     * Constructs friendly troubleshooting advice based on SQL error codes (Unit 1 conditionals).
     */
    private static String buildDiagnosticMessage(SQLException e, DatabaseConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("Failed to connect to database at: ").append(config.getUrl()).append("\n");
        sb.append("User: ").append(config.getUsername()).append("\n");
        sb.append("Error Code: ").append(e.getErrorCode()).append(", SQL State: ").append(e.getSQLState()).append("\n");
        sb.append("Message: ").append(e.getMessage()).append("\n");

        if (e.getErrorCode() == 1045) {
            sb.append("-> Hint: Access denied. Please check your database password in 'db.properties'.");
        } else if (e.getErrorCode() == 1049) {
            sb.append("-> Hint: Database does not exist. Please run 'database/schema.sql' to initialize applytrack_db.");
        } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("communications link failure")) {
            sb.append("-> Hint: MySQL / MariaDB server appears to be offline. Verify that the service is running on port 3306.");
        }

        return sb.toString();
    }
}

