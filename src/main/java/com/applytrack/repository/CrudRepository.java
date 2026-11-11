package com.applytrack.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD (Create, Read, Update, Delete) repository contract.
 *
 * Demonstrates:
 * - Generics: Type parameters <T, ID> (Unit 3)
 * - Interfaces and Polymorphic Data Access (Unit 3)
 * - Optional<T> return types to eliminate NullPointerException (Unit 3)
 * - Standardized checked exception declaration: throws SQLException (Unit 3)
 *
 * @param <T> The entity type
 * @param <ID> The primary key identifier type (e.g., Integer, Long)
 */
public interface CrudRepository<T, ID> {

    /**
     * Persists a new entity into the database and returns the entity with its generated ID.
     */
    T save(T entity) throws SQLException;

    /**
     * Finds an entity by its primary key ID.
     */
    Optional<T> findById(ID id) throws SQLException;

    /**
     * Returns all entities from the corresponding table.
     */
    List<T> findAll() throws SQLException;

    /**
     * Updates an existing entity in the database.
     *
     * @return true if updated, false if no row was modified
     */
    boolean update(T entity) throws SQLException;

    /**
     * Deletes an entity by its primary key ID.
     *
     * @return true if deleted, false if not found
     */
    boolean deleteById(ID id) throws SQLException;
}

