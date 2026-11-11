package com.applytrack.model;

import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.model.enums.JobType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying enum values, custom fields, and string conversion logic.
 */
public class EnumsTest {

    @Test
    @DisplayName("Verify ApplicationStatus fromString and terminal states")
    public void testApplicationStatus() {
        assertEquals(ApplicationStatus.APPLIED, ApplicationStatus.fromString("applied"));
        assertEquals(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.fromString("under review"));
        assertEquals(ApplicationStatus.SHORTLISTED, ApplicationStatus.fromString("Shortlisted"));
        assertEquals(ApplicationStatus.INTERVIEW_SCHEDULED, ApplicationStatus.fromString("interview scheduled"));
        assertEquals(ApplicationStatus.SELECTED, ApplicationStatus.fromString("selected"));
        assertEquals(ApplicationStatus.REJECTED, ApplicationStatus.fromString("rejected"));
        assertEquals(ApplicationStatus.WITHDRAWN, ApplicationStatus.fromString("withdrawn"));
        assertEquals(ApplicationStatus.NO_RESPONSE, ApplicationStatus.fromString("no response"));
        assertEquals(ApplicationStatus.APPLIED, ApplicationStatus.fromString("unknown_status_fallback"));

        assertTrue(ApplicationStatus.SELECTED.isTerminal());
        assertTrue(ApplicationStatus.REJECTED.isTerminal());
        assertFalse(ApplicationStatus.APPLIED.isTerminal());
        assertFalse(ApplicationStatus.UNDER_REVIEW.isTerminal());
    }

    @Test
    @DisplayName("Verify JobType fromString")
    public void testJobType() {
        assertEquals(JobType.FULL_TIME, JobType.fromString("full-time"));
        assertEquals(JobType.INTERNSHIP, JobType.fromString("internship"));
        assertEquals(JobType.PART_TIME, JobType.fromString("part-time"));
        assertEquals(JobType.FULL_TIME, JobType.fromString("invalid"));
    }

    @Test
    @DisplayName("Verify InterviewMode fromString")
    public void testInterviewMode() {
        assertEquals(InterviewMode.ONLINE, InterviewMode.fromString("online"));
        assertEquals(InterviewMode.OFFLINE, InterviewMode.fromString("offline"));
        assertEquals(InterviewMode.ONLINE, InterviewMode.fromString("other"));
    }

    @Test
    @DisplayName("Verify InterviewOutcome fromString")
    public void testInterviewOutcome() {
        assertEquals(InterviewOutcome.SCHEDULED, InterviewOutcome.fromString("scheduled"));
        assertEquals(InterviewOutcome.PASSED, InterviewOutcome.fromString("passed"));
        assertEquals(InterviewOutcome.FAILED, InterviewOutcome.fromString("failed"));
        assertEquals(InterviewOutcome.ON_HOLD, InterviewOutcome.fromString("on hold"));
        assertEquals(InterviewOutcome.CANCELLED, InterviewOutcome.fromString("cancelled"));
        assertEquals(InterviewOutcome.PENDING, InterviewOutcome.fromString("pending"));
        assertEquals(InterviewOutcome.SCHEDULED, InterviewOutcome.fromString(null));
    }
}

