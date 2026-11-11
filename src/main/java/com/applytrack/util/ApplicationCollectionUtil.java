package com.applytrack.util;

import com.applytrack.model.Application;
import com.applytrack.model.enums.ApplicationStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Utility illustrating real-world application of the Java Collections Framework.
 *
 * Demonstrates:
 * - ArrayList: dynamic ordered list of application records (Unit 3)
 * - HashMap: key-value mapping grouping applications by status (Unit 3)
 * - HashSet: mathematical set collecting unique company names with O(1) deduplication (Unit 3)
 * - PriorityQueue: min-heap data structure sorting upcoming deadlines chronologically (Unit 3)
 * - Comparator: custom lambda comparison with null-safe handling (Unit 3)
 */
public class ApplicationCollectionUtil {

    private ApplicationCollectionUtil() {}

    /**
     * Demonstrates ArrayList: Collects variable number of applications into a new dynamic list.
     */
    public static List<Application> createApplicationList(Application... apps) {
        List<Application> list = new ArrayList<>();
        if (apps != null) {
            for (Application app : apps) {
                if (app != null) {
                    list.add(app);
                }
            }
        }
        return list;
    }

    /**
     * Demonstrates HashMap: Groups applications by their ApplicationStatus enum key.
     *
     * @param applications Input list of applications
     * @return Map where each status maps to a list of matching applications
     */
    public static Map<ApplicationStatus, List<Application>> groupApplicationsByStatus(List<Application> applications) {
        Map<ApplicationStatus, List<Application>> groupedMap = new HashMap<>();

        // Initialize all statuses with empty lists
        for (ApplicationStatus status : ApplicationStatus.values()) {
            groupedMap.put(status, new ArrayList<>());
        }

        if (applications != null) {
            for (Application app : applications) {
                if (app != null && app.getStatus() != null) {
                    groupedMap.get(app.getStatus()).add(app);
                }
            }
        }

        return groupedMap;
    }

    /**
     * Demonstrates HashSet: Extracts unique company names across all applications.
     *
     * @param applications Input list of applications
     * @return Set containing deduplicated company names
     */
    public static Set<String> getUniqueCompanyNames(List<Application> applications) {
        Set<String> uniqueCompanies = new HashSet<>();

        if (applications != null) {
            for (Application app : applications) {
                if (app != null && app.getCompany() != null && app.getCompany().getCompanyName() != null) {
                    uniqueCompanies.add(app.getCompany().getCompanyName());
                } else if (app != null && app.getCompanyId() > 0) {
                    uniqueCompanies.add("Company #" + app.getCompanyId());
                }
            }
        }

        return uniqueCompanies;
    }

    /**
     * Demonstrates PriorityQueue: Min-heap prioritizing applications with the earliest upcoming deadline.
     * Applications with null deadlines are placed at the end or filtered out.
     *
     * @param applications Input list of applications
     * @return PriorityQueue where peek() returns the most urgent impending deadline
     */
    public static PriorityQueue<Application> createUpcomingDeadlineQueue(List<Application> applications) {
        // Custom Comparator: Earlier deadline has higher priority (min-heap)
        Comparator<Application> deadlineComparator = (a1, a2) -> {
            LocalDate d1 = a1.getDeadline();
            LocalDate d2 = a2.getDeadline();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1; // Null deadlines placed at the end
            if (d2 == null) return -1;
            return d1.compareTo(d2);
        };

        PriorityQueue<Application> queue = new PriorityQueue<>(deadlineComparator);

        if (applications != null) {
            for (Application app : applications) {
                if (app != null && app.getDeadline() != null) {
                    queue.offer(app);
                }
            }
        }

        return queue;
    }
}

