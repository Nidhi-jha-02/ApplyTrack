package com.applytrack.model.interfaces;

/**
 * Interface defining search filtering capability for model entities.
 *
 * Demonstrates:
 * - Java Interfaces and Polymorphic Contracts (Unit 3)
 * - Decoupled search filtering across disparate entity types
 */
public interface Searchable {

    /**
     * Determines whether the entity matches a case-insensitive search keyword.
     *
     * @param query The search keyword or phrase
     * @return true if the entity matches the query, false otherwise
     */
    boolean matches(String query);
}

