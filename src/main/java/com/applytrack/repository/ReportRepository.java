package com.applytrack.repository;

import com.applytrack.model.dto.ApplicationReportItem;
import com.applytrack.model.dto.DateWiseActivityItem;
import com.applytrack.model.dto.InterviewReportItem;
import com.applytrack.model.dto.RecruiterReportItem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface contract for retrieving structured data sets for reports and exports.
 *
 * Demonstrates:
 * - OOP: Interface Abstraction and Decoupled Data Access (Unit 3)
 * - User-Specific Data Security: Every query strictly binds user_id (Unit 3)
 * - Checked Exception handling: throws SQLException (Unit 3)
 */
public interface ReportRepository {

    /**
     * Retrieves application records for the specified user and optional date range.
     */
    List<ApplicationReportItem> getApplicationReportData(int userId, LocalDate start, LocalDate end) throws SQLException;

    /**
     * Retrieves interview records for the specified user and optional date range.
     */
    List<InterviewReportItem> getInterviewReportData(int userId, LocalDate start, LocalDate end) throws SQLException;

    /**
     * Retrieves recruiter contacts accessible to the specified user.
     */
    List<RecruiterReportItem> getRecruiterReportData(int userId) throws SQLException;

    /**
     * Retrieves combined chronological audit activity records for applications and interviews.
     */
    List<DateWiseActivityItem> getDateWiseActivityData(int userId, LocalDate start, LocalDate end) throws SQLException;
}

