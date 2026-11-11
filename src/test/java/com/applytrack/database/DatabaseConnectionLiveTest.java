package com.applytrack.database;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live Integration Test verifying end-to-end connectivity
 * against the local MariaDB / MySQL instance.
 *
 * Demonstrates:
 * - JUnit 5 Assumptions (Assumptions.assumeTrue) - Unit 3
 * - JDBC Connection and PreparedStatement testing - Unit 3
 * - Metadata validation - Unit 3
 */
public class DatabaseConnectionLiveTest {

    @BeforeAll
    public static void checkDatabaseReachable() {
        boolean available = DatabaseHelper.isDatabaseAvailable();
        Assumptions.assumeTrue(available,
                "Skipping live database integration tests because local MariaDB/MySQL is unreachable or credentials are not yet configured.");
    }

    @Test
    @DisplayName("Verify live connection establishment and validity")
    public void testLiveConnection() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assertNotNull(conn, "Connection object should not be null");
            assertTrue(conn.isValid(3), "Connection should validate within 3 seconds");
            assertEquals("applytrack_db", conn.getCatalog(), "Active database catalog should be 'applytrack_db'");
        }
    }

    @Test
    @DisplayName("Verify heartbeat query execution (SELECT 1)")
    public void testHeartbeatQuery() {
        boolean result = DatabaseHelper.executeHeartbeat();
        assertTrue(result, "Database heartbeat query should return true");
    }

    @Test
    @DisplayName("Verify that all 5 required tables exist in applytrack_db")
    public void testAllRequiredTablesExist() {
        DatabaseHelper.TableValidationResult result = DatabaseHelper.verifyRequiredTables();
        assertTrue(result.isAllPresent(),
                "All 5 required tables must be present in applytrack_db. Missing: " + result.getMissingTables());
        assertTrue(result.getExistingTables().contains("users"));
        assertTrue(result.getExistingTables().contains("companies"));
        assertTrue(result.getExistingTables().contains("recruiters"));
        assertTrue(result.getExistingTables().contains("applications"));
        assertTrue(result.getExistingTables().contains("interviews"));
    }

    @Test
    @DisplayName("Verify sample data records can be queried via PreparedStatement")
    public void testSampleDataQuery() throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM companies";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            assertTrue(rs.next());
            int total = rs.getInt("total");
            assertTrue(total >= 5, "Sample data should have at least 5 seeded companies");
        }
    }
}

