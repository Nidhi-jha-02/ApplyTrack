package com.applytrack.controller;

import com.applytrack.model.dto.DashboardSummary;
import com.applytrack.model.dto.DeadlineSummaryItem;
import com.applytrack.model.dto.InterviewSummaryItem;
import com.applytrack.service.DashboardService;
import com.applytrack.session.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the Dynamic Dashboard View.
 *
 * Demonstrates:
 * - OOP: Encapsulation & Separation of Concerns (Unit 3)
 * - Layered Architecture: Delegates analytics to DashboardService (Unit 3)
 * - Multithreading & Asynchronous Task execution via javafx.concurrent.Task (Unit 3)
 * - Modern Java Date/Time API (java.time.LocalDate, LocalTime, DateTimeFormatter) - Unit 1 & 2
 * - User-Specific Data Security: strictly uses UserSession.getUserId()
 * - Clean UI Empty States and dynamic component population (Unit 3)
 */
public class DashboardController implements Initializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);

    @FXML private Label welcomeTitleLabel;
    @FXML private Label currentDateLabel;
    @FXML private Button refreshBtn;
    @FXML private Label statusMessageLabel;

    @FXML private Label totalAppsLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label interviewsLabel;
    @FXML private Label offersLabel;
    @FXML private Label rejectedLabel;

    @FXML private PieChart statusPieChart;
    @FXML private Label statusPieEmptyLabel;

    @FXML private LineChart<String, Number> monthlyTrendChart;
    @FXML private CategoryAxis trendXAxis;
    @FXML private NumberAxis trendYAxis;
    @FXML private Label trendEmptyLabel;

    @FXML private VBox deadlinesContainer;
    @FXML private Label deadlinesEmptyLabel;

    @FXML private VBox interviewsContainer;
    @FXML private Label interviewsEmptyLabel;

    private final DashboardService dashboardService;

    public DashboardController() {
        this(new DashboardService());
    }

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService != null ? dashboardService : new DashboardService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Display formatted current date
        LocalDate today = LocalDate.now();
        if (currentDateLabel != null) {
            currentDateLabel.setText(today.format(DATE_FORMATTER));
        }

        // Personalize welcome banner with authenticated applicant name
        UserSession session = UserSession.getInstance();
        if (welcomeTitleLabel != null) {
            if (session.isAuthenticated()) {
                welcomeTitleLabel.setText("Welcome back, " + session.getUserFullName());
            } else {
                welcomeTitleLabel.setText("Welcome to ApplyTrack Intelligence");
            }
        }

        // Kick off asynchronous live data fetch from database
        refreshDashboardData();
    }

    /**
     * Refreshes dashboard data asynchronously using JavaFX Task to prevent UI freezing.
     */
    @FXML
    public void handleRefresh(ActionEvent event) {
        refreshDashboardData();
    }

    public void refreshDashboardData() {
        UserSession session = UserSession.getInstance();
        final int userId = session.getUserId();

        if (userId <= 0) {
            if (statusMessageLabel != null) {
                statusMessageLabel.setText("Guest mode: No authenticated user session found.");
            }
            populateDashboard(DashboardSummary.empty());
            return;
        }

        if (refreshBtn != null) {
            refreshBtn.setDisable(true);
        }
        if (statusMessageLabel != null) {
            statusMessageLabel.setText("Synchronizing metrics with MariaDB database...");
        }

        // Background worker thread via JavaFX Task (Unit 3 Multithreading)
        Task<DashboardSummary> loadTask = new Task<>() {
            @Override
            protected DashboardSummary call() throws Exception {
                return dashboardService.getDashboardSummary(userId);
            }
        };

        loadTask.setOnSucceeded(workerStateEvent -> {
            DashboardSummary summary = loadTask.getValue();
            populateDashboard(summary);
            if (refreshBtn != null) {
                refreshBtn.setDisable(false);
            }
            if (statusMessageLabel != null) {
                statusMessageLabel.setText("Last synchronized: " + LocalTime.now().format(TIME_FORMATTER) + " | User ID: " + userId);
            }
        });

        loadTask.setOnFailed(workerStateEvent -> {
            Throwable ex = loadTask.getException();
            System.err.println("Dashboard refresh failed: " + ex.getMessage());
            ex.printStackTrace();
            if (refreshBtn != null) {
                refreshBtn.setDisable(false);
            }
            if (statusMessageLabel != null) {
                statusMessageLabel.setText("Database sync error: " + ex.getMessage());
            }
        });

        Thread worker = new Thread(loadTask, "ApplyTrack-DashboardWorker");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Updates all UI controls with data from the DashboardSummary DTO.
     * Can also be called synchronously in test suites.
     *
     * @param summary Aggregated dashboard metrics DTO
     */
    public void populateDashboard(DashboardSummary summary) {
        if (summary == null) {
            summary = DashboardSummary.empty();
        }

        // 1. Update 5 Metric KPI Cards
        if (totalAppsLabel != null) totalAppsLabel.setText(String.valueOf(summary.getTotalApplications()));
        if (inProgressLabel != null) inProgressLabel.setText(String.valueOf(summary.getInProgressApplications()));
        if (interviewsLabel != null) interviewsLabel.setText(String.valueOf(summary.getScheduledInterviews()));
        if (offersLabel != null) offersLabel.setText(String.valueOf(summary.getOffersCount()));
        if (rejectedLabel != null) rejectedLabel.setText(String.valueOf(summary.getRejectedCount()));

        // 2. Populate Status Distribution PieChart
        populateStatusChart(summary.getStatusDistribution());

        // 3. Populate Monthly Trends LineChart
        populateMonthlyTrendChart(summary.getMonthlyTrends());

        // 4. Populate Upcoming Deadlines Panel
        populateDeadlinesPanel(summary.getUpcomingDeadlines());

        // 5. Populate Scheduled Interviews Panel
        populateInterviewsPanel(summary.getUpcomingInterviews());
    }

    private void populateStatusChart(Map<String, Integer> statusMap) {
        if (statusPieChart == null) return;

        statusPieChart.getData().clear();

        if (statusMap == null || statusMap.isEmpty()) {
            statusPieChart.setVisible(false);
            if (statusPieEmptyLabel != null) statusPieEmptyLabel.setVisible(true);
            return;
        }

        statusPieChart.setVisible(true);
        if (statusPieEmptyLabel != null) statusPieEmptyLabel.setVisible(false);

        for (Map.Entry<String, Integer> entry : statusMap.entrySet()) {
            String label = entry.getKey() + " (" + entry.getValue() + ")";
            statusPieChart.getData().add(new PieChart.Data(label, entry.getValue()));
        }
    }

    private void populateMonthlyTrendChart(Map<String, Integer> trendsMap) {
        if (monthlyTrendChart == null) return;

        monthlyTrendChart.getData().clear();

        if (trendsMap == null || trendsMap.isEmpty()) {
            monthlyTrendChart.setVisible(false);
            if (trendEmptyLabel != null) trendEmptyLabel.setVisible(true);
            return;
        }

        monthlyTrendChart.setVisible(true);
        if (trendEmptyLabel != null) trendEmptyLabel.setVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Applications Submitted");

        for (Map.Entry<String, Integer> entry : trendsMap.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        monthlyTrendChart.getData().add(series);
    }

    private void populateDeadlinesPanel(java.util.List<DeadlineSummaryItem> deadlines) {
        if (deadlinesContainer == null) return;

        deadlinesContainer.getChildren().clear();

        if (deadlines == null || deadlines.isEmpty()) {
            deadlinesContainer.setVisible(false);
            if (deadlinesEmptyLabel != null) deadlinesEmptyLabel.setVisible(true);
            return;
        }

        deadlinesContainer.setVisible(true);
        if (deadlinesEmptyLabel != null) deadlinesEmptyLabel.setVisible(false);

        for (DeadlineSummaryItem item : deadlines) {
            HBox row = new HBox(12.0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("activity-row");

            VBox infoBox = new VBox(2.0);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Label companyLabel = new Label(item.getCompanyName());
            companyLabel.getStyleClass().add("item-primary-text");

            Label roleLabel = new Label(item.getJobRole() + " • " + item.getStatus());
            roleLabel.getStyleClass().add("item-secondary-text");
            infoBox.getChildren().addAll(companyLabel, roleLabel);

            VBox badgeBox = new VBox(3.0);
            badgeBox.setAlignment(Pos.CENTER_RIGHT);

            String formattedDate = item.getDeadline() != null ? item.getDeadline().format(DEADLINE_FORMATTER) : "No date";
            Label dateLabel = new Label("Due: " + formattedDate);
            dateLabel.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #475569; -fx-font-weight: 600;");

            Label urgencyBadge = new Label(item.getFormattedRemaining());
            urgencyBadge.setStyle(item.getUrgency().getBadgeStyle() + " -fx-font-size: 10.5px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 6px;");

            badgeBox.getChildren().addAll(dateLabel, urgencyBadge);
            row.getChildren().addAll(infoBox, badgeBox);

            deadlinesContainer.getChildren().add(row);
        }
    }

    private void populateInterviewsPanel(java.util.List<InterviewSummaryItem> interviews) {
        if (interviewsContainer == null) return;

        interviewsContainer.getChildren().clear();

        if (interviews == null || interviews.isEmpty()) {
            interviewsContainer.setVisible(false);
            if (interviewsEmptyLabel != null) interviewsEmptyLabel.setVisible(true);
            return;
        }

        interviewsContainer.setVisible(true);
        if (interviewsEmptyLabel != null) interviewsEmptyLabel.setVisible(false);

        for (InterviewSummaryItem item : interviews) {
            HBox row = new HBox(12.0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("activity-row");

            VBox infoBox = new VBox(2.0);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Label titleLabel = new Label(item.getCompanyName() + " — " + item.getRoundName());
            titleLabel.getStyleClass().add("item-primary-text");

            String metaText = item.getJobRole() + " (" + item.getInterviewMode() + ")";
            if (item.getMeetingLink() != null && !item.getMeetingLink().isBlank()) {
                metaText += " • Meeting link attached";
            }
            Label metaLabel = new Label(metaText);
            metaLabel.getStyleClass().add("item-secondary-text");
            infoBox.getChildren().addAll(titleLabel, metaLabel);

            VBox badgeBox = new VBox(3.0);
            badgeBox.setAlignment(Pos.CENTER_RIGHT);

            Label timeLabel = new Label(item.getFormattedDateTime());
            timeLabel.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #0f172a; -fx-font-weight: bold;");

            Label outcomeBadge = new Label(item.getOutcome());
            outcomeBadge.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-size: 10.5px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 6px;");

            badgeBox.getChildren().addAll(timeLabel, outcomeBadge);
            row.getChildren().addAll(infoBox, badgeBox);

            interviewsContainer.getChildren().add(row);
        }
    }

    @FXML
    private void handleNewApplication(ActionEvent event) {
        try {
            URL fxmlUrl = getClass().getResource("/com/applytrack/view/ApplicationDialog.fxml");
            if (fxmlUrl == null) {
                showModuleNotice("Add Application", "ApplicationDialog.fxml not found on classpath.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            javafx.scene.Parent root = loader.load();

            ApplicationDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Add New Job Application");
            dialogStage.setScene(new javafx.scene.Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isSaved()) {
                refreshDashboardData();
            }

        } catch (IOException e) {
            System.err.println("Could not open application dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewInterviews(ActionEvent event) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToInterviews();
        } else {
            showModuleNotice("Interview Pipeline", "Interview module is ready. Please access it from the navigation sidebar.");
        }
    }

    @FXML
    private void handleViewRecruiters(ActionEvent event) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToRecruiters();
        } else {
            showModuleNotice("Recruiter Directory", "Recruiter module is ready. Please access it from the navigation sidebar.");
        }
    }

    private void showModuleNotice(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("ApplyTrack Intelligence Roadmap");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
