package com.applytrack.util;

import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.enums.ApplicationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying the 4 core Java collections implementations.
 *
 * Demonstrates:
 * - ArrayList, HashMap, HashSet, PriorityQueue - Unit 3
 */
public class ApplicationCollectionUtilTest {

    @Test
    @DisplayName("Verify ArrayList creation and dynamic resizing")
    public void testArrayListCreation() {
        Application app1 = new Application(1, 1, "SDE 1", LocalDate.now());
        Application app2 = new Application(1, 2, "Backend Engineer", LocalDate.now());

        List<Application> list = ApplicationCollectionUtil.createApplicationList(app1, app2);
        assertEquals(2, list.size());
        assertEquals("SDE 1", list.get(0).getJobRole());
    }

    @Test
    @DisplayName("Verify HashMap grouping by ApplicationStatus")
    public void testHashMapGrouping() {
        Application app1 = new Application(1, 1, "Role 1", LocalDate.now());
        app1.setStatus(ApplicationStatus.APPLIED);

        Application app2 = new Application(1, 2, "Role 2", LocalDate.now());
        app2.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);

        Application app3 = new Application(1, 3, "Role 3", LocalDate.now());
        app3.setStatus(ApplicationStatus.APPLIED);

        List<Application> apps = Arrays.asList(app1, app2, app3);
        Map<ApplicationStatus, List<Application>> grouped = ApplicationCollectionUtil.groupApplicationsByStatus(apps);

        assertNotNull(grouped);
        assertEquals(2, grouped.get(ApplicationStatus.APPLIED).size());
        assertEquals(1, grouped.get(ApplicationStatus.INTERVIEW_SCHEDULED).size());
        assertEquals(0, grouped.get(ApplicationStatus.REJECTED).size());
    }

    @Test
    @DisplayName("Verify HashSet deduplication of company names")
    public void testHashSetDeduplication() {
        Company google = new Company("Google");
        Company msft = new Company("Microsoft");

        Application app1 = new Application(1, 1, "Role 1", LocalDate.now());
        app1.setCompany(google);

        Application app2 = new Application(1, 1, "Role 2", LocalDate.now());
        app2.setCompany(google); // Duplicate company

        Application app3 = new Application(1, 2, "Role 3", LocalDate.now());
        app3.setCompany(msft);

        List<Application> apps = Arrays.asList(app1, app2, app3);
        Set<String> uniqueCompanies = ApplicationCollectionUtil.getUniqueCompanyNames(apps);

        assertEquals(2, uniqueCompanies.size(), "HashSet must eliminate duplicate 'Google' entries");
        assertTrue(uniqueCompanies.contains("Google"));
        assertTrue(uniqueCompanies.contains("Microsoft"));
        assertFalse(uniqueCompanies.contains("Amazon"));
    }

    @Test
    @DisplayName("Verify PriorityQueue deadline ordering (Min-Heap)")
    public void testPriorityQueueDeadlines() {
        LocalDate today = LocalDate.now();

        Application urgentApp = new Application(1, 1, "Urgent App", today);
        urgentApp.setDeadline(today.plusDays(2)); // Earliest deadline

        Application laterApp = new Application(1, 2, "Later App", today);
        laterApp.setDeadline(today.plusDays(10));

        Application midApp = new Application(1, 3, "Mid App", today);
        midApp.setDeadline(today.plusDays(5));

        // Insert in scrambled order: later, urgent, mid
        List<Application> apps = Arrays.asList(laterApp, urgentApp, midApp);
        PriorityQueue<Application> queue = ApplicationCollectionUtil.createUpcomingDeadlineQueue(apps);

        assertEquals(3, queue.size());

        // Min-heap peek() must return the most urgent deadline (plusDays(2))
        Application firstOut = queue.poll();
        assertEquals("Urgent App", firstOut.getJobRole());

        Application secondOut = queue.poll();
        assertEquals("Mid App", secondOut.getJobRole());

        Application thirdOut = queue.poll();
        assertEquals("Later App", thirdOut.getJobRole());
    }
}

