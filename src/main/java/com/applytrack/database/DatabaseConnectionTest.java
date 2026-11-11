package com.applytrack.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Diagnostic runner that verifies end-to-end JDBC connectivity
 * against the local MariaDB / MySQL database.
 *
 * Demonstrates:
 * - Core Java entry point (public static void main) - Unit 1
 * - JDBC DriverManager, Connection, PreparedStatement, ResultSet - Unit 3
 * - Try-with-resources automated resource management - Unit 3
 * - Clean terminal output formatting (StringBuilder / String) - Unit 2
 * - Safe error handling avoiding password leakage - Unit 3
 */
public class DatabaseConnectionTest {

    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("       APPLYTRACK DATABASE TEST     ");
        System.out.println("====================================\n");

        DatabaseConfig config = DatabaseConfig.getInstance();
        boolean driverAvailable = false;
        boolean connectionSuccess = false;
        boolean validationPassed = false;
        String databaseName = "Unknown";
        String databaseServer = "Unknown";
        int totalTablesFound = 0;

        // 1. Verify Driver Availability
        try {
            Class.forName(config.getDriverClassName());
            driverAvailable = true;
            System.out.println("JDBC Driver: AVAILABLE (" + config.getDriverClassName() + ")");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver: FAILED - " + e.getMessage());
        }

        // 2 & 3. Establish & Validate Connection
        if (driverAvailable) {
            try (Connection conn = DatabaseConnection.getConnection(config)) {
                if (conn != null && !conn.isClosed()) {
                    connectionSuccess = true;
                    System.out.println("Database Connection: SUCCESS");

                    validationPassed = conn.isValid(3);
                    System.out.println("Connection Validation: " + (validationPassed ? "PASSED" : "FAILED"));

                    DatabaseMetaData meta = conn.getMetaData();
                    databaseName = conn.getCatalog();
                    databaseServer = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();

                    System.out.println("Database Name: " + databaseName);
                    System.out.println("Database Server: " + databaseServer);

                    // 4. Test simple SELECT query
                    try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 AS heartbeat");
                         ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt("heartbeat") == 1) {
                            System.out.println("Query Execution Test (SELECT 1): PASSED");
                        }
                    }

                    // 5. Inspect required tables
                    DatabaseHelper.TableValidationResult tableResult = DatabaseHelper.verifyRequiredTables();
                    totalTablesFound = tableResult.getExistingTables().size();
                    System.out.println("Tables Verified in '" + databaseName + "': " + totalTablesFound + " found (" +
                            String.join(", ", tableResult.getExistingTables()) + ")");

                    if (tableResult.isAllPresent()) {
                        System.out.println("Schema Integrity: ALL 5 REQUIRED TABLES PRESENT");
                    } else {
                        System.err.println("Schema Integrity: MISSING TABLES -> " + tableResult.getMissingTables());
                    }
                }
            } catch (SQLException e) {
                System.err.println("Database Connection: FAILED");
                System.err.println("Diagnostic Error: " + e.getMessage());
            }
        }

        System.out.println("\n====================================");
        System.out.println("       DATABASE TEST COMPLETED      ");
        System.out.println("====================================");

        // Exit status code for CI/testing automation
        if (!driverAvailable || !connectionSuccess || !validationPassed) {
            System.exit(1);
        }
    }
}

