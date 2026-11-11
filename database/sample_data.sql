-- ===================================================================
-- ApplyTrack: Realistic Demo Seed Data
-- Safe to re-run multiple times (Idempotent using ON DUPLICATE KEY UPDATE)
-- ===================================================================

USE applytrack_db;

-- -------------------------------------------------------------
-- 1. Insert Demo Candidate User
-- Password: "password123" (BCrypt hash)
-- -------------------------------------------------------------
INSERT INTO users (user_id, full_name, email, password_hash)
VALUES (
    1,
    'Alex Mercer',
    'alex.mercer@applytrack.io',
    '$2a$10$n3HO5YCqxZO.xV/6h6bOW.fWUBql4Qdn0bJv9nR2z5b0LtyrMEtbK'
)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    email = VALUES(email),
    password_hash = VALUES(password_hash);

-- -------------------------------------------------------------
-- 2. Insert Companies
-- -------------------------------------------------------------
INSERT INTO companies (company_id, company_name, website, industry, location) VALUES
(1, 'Google', 'https://careers.google.com', 'Technology / Cloud', 'Mountain View, CA / Hyderabad'),
(2, 'Microsoft', 'https://careers.microsoft.com', 'Enterprise Software', 'Redmond, WA / Bengaluru'),
(3, 'Amazon', 'https://amazon.jobs', 'E-Commerce / Cloud AWS', 'Seattle, WA / Hyderabad'),
(4, 'Spotify', 'https://www.lifeatspotify.com', 'Audio Streaming / Media', 'Stockholm / Remote'),
(5, 'Atlassian', 'https://www.atlassian.com/company/careers', 'Productivity Software', 'Bengaluru / Remote'),
(6, 'Adobe', 'https://careers.adobe.com', 'Digital Media & Creative Cloud', 'Noida / Bengaluru')
ON DUPLICATE KEY UPDATE
    website = VALUES(website),
    industry = VALUES(industry),
    location = VALUES(location);

-- -------------------------------------------------------------
-- 3. Insert Recruiters (Fictional campus talent partners)
-- -------------------------------------------------------------
INSERT INTO recruiters (recruiter_id, company_id, recruiter_name, email, phone, linkedin_url, notes) VALUES
(1, 1, 'Sarah Jenkins', 'sarah.jenkins@example.com', '+1-650-555-0199', 'https://linkedin.com/in/sarahjenkins-talent', 'University Talent Acquisition Partner for EMEA and APAC.'),
(2, 2, 'Rohan Verma', 'rohan.verma@example.com', '+91-98765-43210', 'https://linkedin.com/in/rohanverma-tech', 'Senior Campus Recruiter. Prefers communication via email.'),
(3, 3, 'Emily Zhang', 'emily.zhang@example.com', '+1-206-555-0144', 'https://linkedin.com/in/emilyzhang-aws', 'AWS Core Services recruiter for Graduate hiring.'),
(4, 5, 'Kavita Iyer', 'kavita.iyer@example.com', '+91-91234-56789', 'https://linkedin.com/in/kavitaiyer-hr', 'Talent scout for Atlassian Cloud Platform teams.')
ON DUPLICATE KEY UPDATE
    recruiter_name = VALUES(recruiter_name),
    email = VALUES(email),
    phone = VALUES(phone),
    linkedin_url = VALUES(linkedin_url),
    notes = VALUES(notes);

-- -------------------------------------------------------------
-- 4. Insert Job Applications
-- Diverse statuses: Applied, Under Review, Shortlisted, Interview Scheduled, Selected, Rejected, Withdrawn
-- -------------------------------------------------------------
INSERT INTO applications (
    application_id, user_id, company_id, recruiter_id, job_role, job_type,
    application_date, deadline, job_description, job_url, expected_salary, status, notes
) VALUES
(
    1, 1, 1, 1,
    'Software Engineer - Backend', 'Full-time',
    '2026-10-15', '2026-11-20',
    'Design and maintain distributed microservices in Java and Go.',
    'https://careers.google.com/jobs/results/12345', 135000.00,
    'Interview Scheduled',
    'Cleared Online Assessment with 100% score. Technical Round 1 scheduled.'
),
(
    2, 1, 2, 2,
    'Software Engineer Intern', 'Internship',
    '2026-10-20', '2026-11-15',
    'Azure Developer Experience and tooling team summer internship.',
    'https://careers.microsoft.com/jobs/9876', 50000.00,
    'Under Review',
    'Resume forwarded by college placement cell. Awaiting OA link.'
),
(
    3, 1, 3, 3,
    'SDE I (Java Systems)', 'Full-time',
    '2026-09-10', '2026-10-01',
    'Core catalog, high-concurrency order placement and pricing engineering.',
    'https://amazon.jobs/en/jobs/45678', 120000.00,
    'Selected',
    'Offer received! Discussing joining dates and relocation package.'
),
(
    4, 1, 4, NULL,
    'Junior Backend Developer', 'Full-time',
    '2026-10-01', '2026-10-25',
    'Audio streaming backend services using Java, Spring Boot & Kafka.',
    'https://spotify.com/careers/backend-jr', 115000.00,
    'Rejected',
    'Resume screened out at initial round. Allowed to reapply after 6 months.'
),
(
    5, 1, 5, 4,
    'Associate Software Engineer', 'Full-time',
    '2026-11-05', '2026-11-18',
    'Jira Cloud Platform backend development and high-throughput APIs.',
    'https://atlassian.com/careers/ase-2026', 110000.00,
    'Shortlisted',
    'Profile shortlisted for Online Assessment. Scheduled for next weekend.'
),
(
    6, 1, 6, NULL,
    'Software Technology Intern', 'Internship',
    '2026-11-08', '2026-11-30',
    'Creative Cloud desktop rendering engine engineering in C++ and Java.',
    'https://adobe.com/careers/intern-2026', 45000.00,
    'Applied',
    'Applied directly through college campus placement portal.'
)
ON DUPLICATE KEY UPDATE
    job_role = VALUES(job_role),
    job_type = VALUES(job_type),
    application_date = VALUES(application_date),
    deadline = VALUES(deadline),
    job_description = VALUES(job_description),
    job_url = VALUES(job_url),
    expected_salary = VALUES(expected_salary),
    status = VALUES(status),
    notes = VALUES(notes);

-- -------------------------------------------------------------
-- 5. Insert Interview Rounds
-- Demonstrating multiple rounds for an application (Google & Amazon)
-- -------------------------------------------------------------
INSERT INTO interviews (
    interview_id, application_id, round_name, interview_date, interview_mode,
    interview_location, meeting_link, interviewer_name, feedback, outcome
) VALUES
(
    1, 1, 'Online Assessment',
    '2026-10-25 15:00:00', 'Online',
    'Remote Proctor', 'https://assessment.hackerrank.com/test-id-1029',
    'Automated Evaluator', 'Solved 2 DSA questions (Graph Dijkstra and DP). All test cases passed.', 'Passed'
),
(
    2, 1, 'Technical Round 1',
    '2026-11-12 18:30:00', 'Online',
    'Google Meet', 'https://meet.google.com/xyz-applytrack',
    'David Chen (Senior SDE)', 'Prepare on Java Concurrency, OOP design patterns and System Design.', 'Scheduled'
),
(
    3, 3, 'Online Assessment',
    '2026-09-18 10:00:00', 'Online',
    'Amazon Proctor Portal', 'https://hirepro.in/amazon-test-456',
    'Proctor System', 'Full score in coding challenge and work style assessment.', 'Passed'
),
(
    4, 3, 'Technical Round 1 - DSA & OOP',
    '2026-09-28 14:00:00', 'Online',
    'Amazon Chime', 'https://chime.aws/meet-998877',
    'Kavita Nair (SDE II)', 'Strong knowledge of HashMaps, Binary Trees, and Time Complexity.', 'Passed'
),
(
    5, 3, 'Final HR & Bar Raiser',
    '2026-10-08 16:00:00', 'Online',
    'Amazon Chime', 'https://chime.aws/meet-112233',
    'Marcus Vance (Engineering Manager)', 'Demonstrated Amazon Leadership Principles and strong communication.', 'Passed'
)
ON DUPLICATE KEY UPDATE
    round_name = VALUES(round_name),
    interview_date = VALUES(interview_date),
    interview_mode = VALUES(interview_mode),
    interview_location = VALUES(interview_location),
    meeting_link = VALUES(meeting_link),
    interviewer_name = VALUES(interviewer_name),
    feedback = VALUES(feedback),
    outcome = VALUES(outcome);

-- -------------------------------------------------------------
-- 6. Insert User Preferences (Phase 11)
-- -------------------------------------------------------------
INSERT INTO user_preferences (
    preference_id, user_id, theme, default_landing_page,
    preferred_analytics_range, reminder_upcoming_interviews, reminder_application_deadlines
) VALUES
(
    1, 1, 'LIGHT', 'DASHBOARD', 'ALL_TIME', TRUE, TRUE
)
ON DUPLICATE KEY UPDATE
    theme = VALUES(theme),
    default_landing_page = VALUES(default_landing_page),
    preferred_analytics_range = VALUES(preferred_analytics_range),
    reminder_upcoming_interviews = VALUES(reminder_upcoming_interviews),
    reminder_application_deadlines = VALUES(reminder_application_deadlines);

