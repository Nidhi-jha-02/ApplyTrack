package com.applytrack.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Diagnostic and validation utility for the ApplyTrack database.
 *
 * Demonstrates:
 * - Collections Framework (List, Set, HashSet, ArrayList) - Unit 3
 * - JDBC DatabaseMetaData inspection - Unit 3
 * - PreparedStatement execution with try-with-resources - Unit 3
 * - Static utility methods - Unit 2
 */
public class DatabaseHelper {

    public static final List<String> REQUIRED_TABLES = Arrays.asList(
            "users",
            "companies",
            "recruiters",
            "applications",
            "interviews"
    );

    private DatabaseHelper() {}

    /**
     * Checks if the database server is responsive.
     */
    public static boolean isDatabaseAvailable() {
        return DatabaseConnection.testConnection();
    }

    /**
     * Executes a heartbeat query (SELECT 1) using PreparedStatement.
     */
    public static boolean executeHeartbeat() {
        String sql = "SELECT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() && rs.getInt(1) == 1;
        } catch (SQLException e) {
            System.err.println("Database heartbeat query failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifies that all 5 required tables exist in applytrack_db.
     *
     * @return TableValidationResult containing status and list of missing tables
     */
    public static TableValidationResult verifyRequiredTables() {
        List<String> missingTables = new ArrayList<>();
        Set<String> existingTables = new HashSet<>();

        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();

            try (ResultSet rs = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (tableName != null) {
                        existingTables.add(tableName.toLowerCase());
                    }
                }
            }

            for (String required : REQUIRED_TABLES) {
                if (!existingTables.contains(required.toLowerCase())) {
                    missingTables.add(required);
                }
            }

            return new TableValidationResult(missingTables.isEmpty(), missingTables, new ArrayList<>(existingTables));

        } catch (SQLException e) {
            System.err.println("Error inspecting database tables: " + e.getMessage());
            return new TableValidationResult(false, REQUIRED_TABLES, new ArrayList<>());
        }
    }

    /**
     * Obtains human-readable metadata about the connected database.
     */
    public static DatabaseMetadataSummary getMetadataSummary() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            return new DatabaseMetadataSummary(
                    meta.getDatabaseProductName(),
                    meta.getDatabaseProductVersion(),
                    meta.getDriverName(),
                    meta.getDriverVersion(),
                    conn.getCatalog(),
                    true
            );
        } catch (SQLException e) {
            return new DatabaseMetadataSummary("Unknown", "Unavailable", "MySQL Connector/J", "Unknown", "applytrack_db", false);
        }
    }

    /**
     * Immutable result class for table validation (Unit 3 OOP).
     */
    public static class TableValidationResult {
        private final boolean allPresent;
        private final List<String> missingTables;
        private final List<String> existingTables;

        public TableValidationResult(boolean allPresent, List<String> missingTables, List<String> existingTables) {
            this.allPresent = allPresent;
            this.missingTables = missingTables;
            this.existingTables = existingTables;
        }

        public boolean isAllPresent() {
            return allPresent;
        }

        public List<String> getMissingTables() {
            return missingTables;
        }

        public List<String> getExistingTables() {
            return existingTables;
        }
    }

    /**
     * Immutable container for database metadata summary.
     */
    public static class DatabaseMetadataSummary {
        private final String productName;
        private final String productVersion;
        private final String driverName;
        private final String driverVersion;
        private final String catalog;
        private final boolean connected;

        public DatabaseMetadataSummary(String productName, String productVersion, String driverName,
                                       String driverVersion, String catalog, boolean connected) {
            this.productName = productName;
            this.productVersion = productVersion;
            this.driverName = driverName;
            this.driverVersion = driverVersion;
            this.catalog = catalog;
            this.connected = connected;
        }

        public String getProductName() { return productName; }
        public String getProductVersion() { return productVersion; }
        public String getDriverName() { return driverName; }
        public String getDriverVersion() { return driverVersion; }
        public String getCatalog() { return catalog; }
        public boolean isConnected() { return connected; }
    }
}

