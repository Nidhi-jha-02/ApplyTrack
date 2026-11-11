package com.applytrack.repository;

import com.applytrack.model.User;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Repository interface specifically for User persistence and authentication queries.
 *
 * Demonstrates:
 * - Interface Inheritance (extends CrudRepository<User, Integer>) - Unit 3
 * - Polymorphism and Segregated Interfaces - Unit 3
 */
public interface UserRepository extends CrudRepository<User, Integer> {

    /**
     * Finds a candidate user by their unique login email address.
     *
     * @param email The applicant's email address
     * @return Optional containing User if present, or empty
     * @throws SQLException On database communication error
     */
    Optional<User> findByEmail(String email) throws SQLException;

    /**
     * Checks if an email is already registered in the system.
     *
     * @param email The applicant's email address
     * @return true if an account exists with this email, false otherwise
     * @throws SQLException On database communication error
     */
    boolean existsByEmail(String email) throws SQLException;
}

