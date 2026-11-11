package com.applytrack.repository;

import com.applytrack.model.Company;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository contract for managing company profiles and lookups.
 *
 * Demonstrates:
 * - OOP: Interface Abstraction (Unit 3)
 * - Optional<T> return types (Unit 3)
 * - Checked Exception handling: throws SQLException (Unit 3)
 */
public interface CompanyRepository {

    /**
     * Finds a company by its primary key ID.
     */
    Optional<Company> findById(int companyId) throws SQLException;

    /**
     * Finds a company by exact or case-insensitive company name.
     */
    Optional<Company> findByName(String companyName) throws SQLException;

    /**
     * Looks up an existing company by name, or automatically creates a new record if absent.
     */
    Company findOrCreateByName(String companyName) throws SQLException;

    /**
     * Retrieves all companies ordered alphabetically by company name.
     */
    List<Company> findAll() throws SQLException;

    /**
     * Persists or updates a company profile.
     */
    Company save(Company company) throws SQLException;
}

