package com.applytrack.repository;

import com.applytrack.database.DatabaseHelper;
import com.applytrack.model.User;
import com.applytrack.repository.impl.JdbcUserRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live database integration test for JdbcUserRepository against local MariaDB.
 *
 * Demonstrates:
 * - Live JDBC PreparedStatement CRUD verification - Unit 3
 * - BCrypt hash persistence verification
 */
public class JdbcUserRepositoryLiveTest {

    private final JdbcUserRepository userRepository = new JdbcUserRepository();

    @BeforeAll
    public static void verifyDatabaseAvailable() {
        Assumptions.assumeTrue(DatabaseHelper.isDatabaseAvailable(),
                "Skipping live DB repository test: Database is not reachable.");
    }

    @Test
    @DisplayName("Verify findByEmail retrieves seeded user (Alex Mercer)")
    public void testFindByEmail() throws SQLException {
        Optional<User> userOpt = userRepository.findByEmail("alex.mercer@applytrack.io");
        assertTrue(userOpt.isPresent(), "Demo user 'alex.mercer@applytrack.io' should be in the database");

        User user = userOpt.get();
        assertEquals("Alex Mercer", user.getFullName());
        assertNotNull(user.getPasswordHash());
        // Verify demo password 'password123' works with BCrypt
        assertTrue(BCrypt.checkpw("password123", user.getPasswordHash()));
    }

    @Test
    @DisplayName("Verify existsByEmail with existing and non-existing emails")
    public void testExistsByEmail() throws SQLException {
        assertTrue(userRepository.existsByEmail("alex.mercer@applytrack.io"));
        assertFalse(userRepository.existsByEmail("non_existent_random_user_99@example.com"));
    }

    @Test
    @DisplayName("Verify full CRUD lifecycle: save, findById, update, and delete")
    public void testUserCrudLifecycle() throws SQLException {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        String testEmail = "testuser." + randomSuffix + "@applytrack.io";
        String testHash = BCrypt.hashpw("testPassword123", BCrypt.gensalt(10));

        // 1. Create / Save
        User user = new User("Integration Test User", testEmail, testHash);
        User saved = userRepository.save(user);

        assertNotNull(saved);
        assertTrue(saved.getId() > 0, "Generated ID must be assigned");

        // 2. Read / FindById
        Optional<User> fetchedOpt = userRepository.findById(saved.getId());
        assertTrue(fetchedOpt.isPresent());
        User fetched = fetchedOpt.get();
        assertEquals("Integration Test User", fetched.getFullName());
        assertEquals(testEmail, fetched.getEmail());

        // 3. Update
        fetched.setFullName("Updated Integration User");
        boolean updated = userRepository.update(fetched);
        assertTrue(updated);

        Optional<User> refetchedOpt = userRepository.findById(saved.getId());
        assertTrue(refetchedOpt.isPresent());
        assertEquals("Updated Integration User", refetchedOpt.get().getFullName());

        // 4. Delete
        boolean deleted = userRepository.deleteById(saved.getId());
        assertTrue(deleted);

        Optional<User> postDeleteOpt = userRepository.findById(saved.getId());
        assertTrue(postDeleteOpt.isEmpty(), "User should no longer exist after deletion");
    }
}

