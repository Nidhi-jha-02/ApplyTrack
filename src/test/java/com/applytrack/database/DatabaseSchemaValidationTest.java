package com.applytrack.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test verifying the integrity and consistency of SQL schema scripts.
 *
 * Demonstrates:
 * - Java File I/O (java.nio.file.Files, Path) - Unit 3
 * - String searching and pattern matching - Unit 2
 * - Testing schema consistency before database migration
 */
public class DatabaseSchemaValidationTest {

    @Test
    @DisplayName("Verify database/schema.sql exists and contains all 5 required tables")
    public void testSchemaFileIntegrity() throws IOException {
        Path schemaPath = Paths.get("database", "schema.sql");
        assertTrue(Files.exists(schemaPath), "database/schema.sql must exist on disk");

        String content = Files.readString(schemaPath).toLowerCase();

        assertTrue(content.contains("create table if not exists users"), "Schema must define 'users' table");
        assertTrue(content.contains("create table if not exists companies"), "Schema must define 'companies' table");
        assertTrue(content.contains("create table if not exists recruiters"), "Schema must define 'recruiters' table");
        assertTrue(content.contains("create table if not exists applications"), "Schema must define 'applications' table");
        assertTrue(content.contains("create table if not exists interviews"), "Schema must define 'interviews' table");
    }

    @Test
    @DisplayName("Verify database/schema.sql defines correct relational integrity and delete constraints")
    public void testForeignKeysAndConstraints() throws IOException {
        Path schemaPath = Paths.get("database", "schema.sql");
        String content = Files.readString(schemaPath).toLowerCase();

        // Check foreign key constraints
        assertTrue(content.contains("fk_recruiter_company"), "Schema must define fk_recruiter_company");
        assertTrue(content.contains("fk_app_user"), "Schema must define fk_app_user");
        assertTrue(content.contains("fk_app_company"), "Schema must define fk_app_company");
        assertTrue(content.contains("fk_app_recruiter"), "Schema must define fk_app_recruiter");
        assertTrue(content.contains("fk_interview_app"), "Schema must define fk_interview_app");

        // Check cascade behaviors
        assertTrue(content.contains("on delete cascade"), "Schema must use ON DELETE CASCADE appropriately");
        assertTrue(content.contains("on delete set null"), "Schema must use ON DELETE SET NULL for recruiters");
        assertTrue(content.contains("on delete restrict"), "Schema must use ON DELETE RESTRICT for company deletions");
    }

    @Test
    @DisplayName("Verify database/sample_data.sql exists and provides seed data")
    public void testSampleDataFileIntegrity() throws IOException {
        Path sampleDataPath = Paths.get("database", "sample_data.sql");
        assertTrue(Files.exists(sampleDataPath), "database/sample_data.sql must exist on disk");

        String content = Files.readString(sampleDataPath);
        assertTrue(content.contains("INSERT INTO users"), "Sample data must populate users");
        assertTrue(content.contains("INSERT INTO companies"), "Sample data must populate companies");
        assertTrue(content.contains("INSERT INTO applications"), "Sample data must populate applications");
        assertTrue(content.contains("INSERT INTO interviews"), "Sample data must populate interviews");
    }
}

