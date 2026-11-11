package com.applytrack.repository;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.Company;
import com.applytrack.repository.impl.JdbcCompanyRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 6: JdbcCompanyRepository Live MariaDB Integration Tests")
class JdbcCompanyRepositoryLiveTest {

    private static JdbcCompanyRepository repository;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Assumptions.assumeTrue(conn != null && !conn.isClosed(), "MariaDB connection must be active");
            repository = new JdbcCompanyRepository();
        } catch (SQLException e) {
            Assumptions.abort("MariaDB not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("findByName returns existing seed company")
    void testFindByName() throws SQLException {
        Optional<Company> google = repository.findByName("Google");
        assertTrue(google.isPresent());
        assertEquals("Google", google.get().getCompanyName());
    }

    @Test
    @DisplayName("findOrCreateByName creates new company and avoids duplicates on second invocation")
    void testFindOrCreateByName() throws SQLException {
        String testCompanyName = "Oracle Systems";
        Company c1 = repository.findOrCreateByName(testCompanyName);
        assertNotNull(c1);
        assertTrue(c1.getId() > 0);

        Company c2 = repository.findOrCreateByName(testCompanyName);
        assertNotNull(c2);
        assertEquals(c1.getId(), c2.getId(), "Subsequent call must return the same company ID without creating duplicates");
    }

    @Test
    @DisplayName("findAll returns companies sorted alphabetically")
    void testFindAll() throws SQLException {
        List<Company> companies = repository.findAll();
        assertNotNull(companies);
        assertTrue(companies.size() >= 5);

        for (int i = 0; i < companies.size() - 1; i++) {
            String name1 = companies.get(i).getCompanyName();
            String name2 = companies.get(i + 1).getCompanyName();
            assertTrue(name1.compareToIgnoreCase(name2) <= 0, "Companies must be sorted alphabetically");
        }
    }
}

