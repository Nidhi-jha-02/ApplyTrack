package com.applytrack.controller;

import com.applytrack.model.dto.AnalyticsSummary;
import com.applytrack.model.dto.CompanyApplicationData;
import com.applytrack.model.dto.InterviewAnalytics;
import com.applytrack.model.dto.KeyInsights;
import com.applytrack.model.dto.MonthlyApplicationData;
import com.applytrack.model.dto.StatusDistribution;
import com.applytrack.model.enums.DateRangeFilter;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.service.AnalyticsService;
import com.applytrack.session.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the Advanced Analytics & Insights module.
 *
 * Demonstrates:
 * - JavaFX Initializable lifecycle and UI bindings (Unit 3)
 * - Multithreading via javafx.concurrent.Task preventing GUI freeze (Unit 3)
 * - JavaFX Chart manipulation (PieChart, LineChart, BarChart) (Unit 3)
 * - User-Specific Data Security using UserSession singleton (Unit 3)
 * - Event handling with dynamic filtering (Unit 1 & Unit 2)
 */
public class AnalyticsController implements Initializable {

    // Filter controls
    @FXML private ComboBox<DateRangeFilter> dateRangeCombo;
    @FXML private Button refreshBtn;

    // KPI Summary Labels
    @FXML private Label totalAppsLabel;
    @FXML private Label totalAppsCaption;
    @FXML private Label activeAppsLabel;
    @FXML private Label totalInterviewsLabel;
    @FXML private Label offersLabel;
    @FXML private Label offerRateLabel;
    @FXML private Label rejectedLabel;
    @FXML private Label conversionRateLabel;
    @FXML private Label conversionCaptionLabel;

    // Key Insights Labels
    @FXML private Label insightsDateScopeLabel;
    @FXML private Label insightsNarrativeLabel;
    @FXML private Label insightTopCompanyLabel;
    @FXML private Label insightPeakMonthLabel;
    @FXML private Label insightCompletedInterviewsLabel;
    @FXML private Label insightPendingDecisionsLabel;

    // Application Charts
    @FXML private PieChart statusPieChart;
    @FXML private Label statusPieEmptyLabel;
    @FXML private LineChart<String, Number> monthlyAppLineChart;
    @FXML private Label monthlyAppEmptyLabel;

    // Interview Charts
    @FXML private PieChart interviewOutcomePieChart;
    @FXML private Label interviewOutcomeEmptyLabel;
    @FXML private LineChart<String, Number> monthlyInterviewLineChart;
    @FXML private Label monthlyInterviewEmptyLabel;

    // Company Chart
    @FXML private BarChart<String, Number> companyBarChart;
    @FXML private Label companyBarEmptyLabel;

    // Loading overlay
    @FXML private VBox loadingOverlay;

    private final AnalyticsService analyticsService;

    public AnalyticsController() {
        this.analyticsService = new AnalyticsService();
    }

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService != null ? analyticsService : new AnalyticsService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDateRangeSelector();
        loadAnalyticsAsync(DateRangeFilter.ALL_TIME);
    }

    private void setupDateRangeSelector() {
        if (dateRangeCombo != null) {
            dateRangeCombo.setItems(FXCollections.observableArrayList(DateRangeFilter.values()));
            dateRangeCombo.setValue(DateRangeFilter.ALL_TIME);
        }
    }

    @FXML
    private void handleDateRangeChanged(ActionEvent event) {
        DateRangeFilter selected = dateRangeCombo != null ? dateRangeCombo.getValue() : DateRangeFilter.ALL_TIME;
        loadAnalyticsAsync(selected);
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        DateRangeFilter selected = dateRangeCombo != null ? dateRangeCombo.getValue() : DateRangeFilter.ALL_TIME;
        loadAnalyticsAsync(selected);
    }

    /**
     * Executes the database aggregation on a background daemon worker thread.
     */
    public void loadAnalyticsAsync(DateRangeFilter filter) {
        if (!UserSession.getInstance().isAuthenticated()) {
            System.err.println("Cannot load analytics: No authenticated user session found.");
            return;
        }

        final int userId = UserSession.getInstance().getUserId();
        final DateRangeFilter activeFilter = filter != null ? filter : DateRangeFilter.ALL_TIME;

        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
        }

        Task<AnalyticsSummary> task = new Task<>() {
            @Override
            protected AnalyticsSummary call() throws Exception {
                return analyticsService.getAnalyticsSummary(userId, activeFilter);
            }
        };

        task.setOnSucceeded(e -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisible(false);
            }
            populateAnalytics(task.getValue());
        });

        task.setOnFailed(e -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisible(false);
            }
            Throwable ex = task.getException();
            System.err.println("Failed to load analytics: " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        Thread worker = new Thread(task);
        worker.setDaemon(true);
        worker.setName("ApplyTrack-AnalyticsWorker");
        worker.start();
    }

    /**
     * Populates all UI cards, narratives, and charts safely on the JavaFX Application Thread.
     */
    private void populateAnalytics(AnalyticsSummary summary) {
        if (summary == null) {
            return;
        }

        // 1. KPI Metric Cards
        totalAppsLabel.setText(String.valueOf(summary.getTotalApplications()));
        totalAppsCaption.setText(summary.getDateRangeFilter() == DateRangeFilter.ALL_TIME
                ? "All-time submissions"
                : "Submitted in " + summary.getDateRangeFilter().getDisplayName());

        activeAppsLabel.setText(String.valueOf(summary.getActiveApplications()));
        totalInterviewsLabel.setText(String.valueOf(summary.getTotalInterviews()));
        offersLabel.setText(String.valueOf(summary.getOffersReceived()));
        offerRateLabel.setText(summary.getFormattedOfferRate() + " offer rate");
        rejectedLabel.setText(String.valueOf(summary.getRejectedApplications()));
        conversionRateLabel.setText(summary.getFormattedConversionRate());
        conversionCaptionLabel.setText(summary.getDistinctApplicationsInterviewed() + " interviewed app" +
                (summary.getDistinctApplicationsInterviewed() == 1 ? "" : "s"));

        // 2. Key Insights Banner
        KeyInsights insights = summary.getKeyInsights();
        insightsDateScopeLabel.setText("Scope: " + summary.getDateRangeFilter().getDisplayName());
        insightsNarrativeLabel.setText(insights.getSummaryText());
        insightTopCompanyLabel.setText(insights.getTopCompany() + (insights.getTopCompanyCount() > 0 ? " (" + insights.getTopCompanyCount() + ")" : ""));
        insightPeakMonthLabel.setText(insights.getPeakMonth() + (insights.getPeakMonthCount() > 0 ? " (" + insights.getPeakMonthCount() + ")" : ""));
        insightCompletedInterviewsLabel.setText(String.valueOf(insights.getCompletedInterviews()));
        insightPendingDecisionsLabel.setText(String.valueOf(insights.getPendingDecisions()));

        // 3. Application Status Distribution PieChart
        populateStatusPieChart(summary);

        // 4. Monthly Application LineChart
        populateMonthlyAppChart(summary);

        // 5. Interview Outcome Distribution PieChart
        populateInterviewOutcomeChart(summary.getInterviewAnalytics());

        // 6. Monthly Interview LineChart
        populateMonthlyInterviewChart(summary.getInterviewAnalytics());

        // 7. Company-Wise Application BarChart
        populateCompanyBarChart(summary);
    }

    private void populateStatusPieChart(AnalyticsSummary summary) {
        statusPieChart.getData().clear();
        if (summary.getStatusDistributions().isEmpty() || summary.getTotalApplications() == 0) {
            statusPieEmptyLabel.setVisible(true);
            statusPieChart.setVisible(false);
            return;
        }

        statusPieEmptyLabel.setVisible(false);
        statusPieChart.setVisible(true);

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (StatusDistribution item : summary.getStatusDistributions()) {
            if (item.getCount() > 0) {
                pieData.add(new PieChart.Data(item.getStatusName() + " (" + item.getCount() + ")", item.getCount()));
            }
        }
        statusPieChart.setData(pieData);

        // Apply slice colors matching status badge color
        Platform.runLater(() -> {
            int i = 0;
            for (StatusDistribution item : summary.getStatusDistributions()) {
                if (item.getCount() > 0 && i < statusPieChart.getData().size()) {
                    PieChart.Data slice = statusPieChart.getData().get(i);
                    if (slice.getNode() != null) {
                        slice.getNode().setStyle("-fx-pie-color: " + item.getBadgeColor() + ";");
                    }
                    i++;
                }
            }
        });
    }

    private void populateMonthlyAppChart(AnalyticsSummary summary) {
        monthlyAppLineChart.getData().clear();
        if (summary.getMonthlyApplicationTrends().isEmpty() || summary.getTotalApplications() == 0) {
            monthlyAppEmptyLabel.setVisible(true);
            monthlyAppLineChart.setVisible(false);
            return;
        }

        monthlyAppEmptyLabel.setVisible(false);
        monthlyAppLineChart.setVisible(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Applications");
        for (MonthlyApplicationData item : summary.getMonthlyApplicationTrends()) {
            series.getData().add(new XYChart.Data<>(item.getDisplayLabel(), item.getCount()));
        }
        monthlyAppLineChart.getData().add(series);
    }

    private void populateInterviewOutcomeChart(InterviewAnalytics interviewAnalytics) {
        interviewOutcomePieChart.getData().clear();
        Map<String, Integer> outcomes = interviewAnalytics.getOutcomeDistribution();

        if (outcomes == null || outcomes.isEmpty() || interviewAnalytics.getTotalInterviews() == 0) {
            interviewOutcomeEmptyLabel.setVisible(true);
            interviewOutcomePieChart.setVisible(false);
            return;
        }

        interviewOutcomeEmptyLabel.setVisible(false);
        interviewOutcomePieChart.setVisible(true);

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : outcomes.entrySet()) {
            if (entry.getValue() > 0) {
                pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
        }
        interviewOutcomePieChart.setData(pieData);

        // Apply slice colors based on InterviewOutcome
        Platform.runLater(() -> {
            int i = 0;
            for (Map.Entry<String, Integer> entry : outcomes.entrySet()) {
                if (entry.getValue() > 0 && i < interviewOutcomePieChart.getData().size()) {
                    PieChart.Data slice = interviewOutcomePieChart.getData().get(i);
                    InterviewOutcome outcome = InterviewOutcome.fromString(entry.getKey());
                    if (slice.getNode() != null) {
                        slice.getNode().setStyle("-fx-pie-color: " + outcome.getBadgeColor() + ";");
                    }
                    i++;
                }
            }
        });
    }

    private void populateMonthlyInterviewChart(InterviewAnalytics interviewAnalytics) {
        monthlyInterviewLineChart.getData().clear();
        if (interviewAnalytics.getMonthlyInterviewTrends().isEmpty() || interviewAnalytics.getTotalInterviews() == 0) {
            monthlyInterviewEmptyLabel.setVisible(true);
            monthlyInterviewLineChart.setVisible(false);
            return;
        }

        monthlyInterviewEmptyLabel.setVisible(false);
        monthlyInterviewLineChart.setVisible(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Interview Rounds");
        for (MonthlyApplicationData item : interviewAnalytics.getMonthlyInterviewTrends()) {
            series.getData().add(new XYChart.Data<>(item.getDisplayLabel(), item.getCount()));
        }
        monthlyInterviewLineChart.getData().add(series);
    }

    private void populateCompanyBarChart(AnalyticsSummary summary) {
        companyBarChart.getData().clear();
        if (summary.getCompanyApplicationData().isEmpty() || summary.getTotalApplications() == 0) {
            companyBarEmptyLabel.setVisible(true);
            companyBarChart.setVisible(false);
            return;
        }

        companyBarEmptyLabel.setVisible(false);
        companyBarChart.setVisible(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Applications");
        for (CompanyApplicationData item : summary.getCompanyApplicationData()) {
            series.getData().add(new XYChart.Data<>(item.getCompanyName(), item.getApplicationCount()));
        }
        companyBarChart.getData().add(series);
    }
}
