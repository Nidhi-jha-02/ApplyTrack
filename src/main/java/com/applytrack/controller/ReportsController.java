package com.applytrack.controller;

import com.applytrack.session.UserSession;
import com.applytrack.exception.ValidationException;
import com.applytrack.model.dto.ReportFilter;
import com.applytrack.model.dto.ReportPayload;
import com.applytrack.model.enums.DateRangeFilter;
import com.applytrack.model.enums.ReportType;
import com.applytrack.service.ReportService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the Reports & Data Export Center.
 *
 * Demonstrates:
 * - JavaFX Multithreading with javafx.concurrent.Task & Worker state (Unit 3)
 * - Safe UI updates dispatched on JavaFX Application Thread (Unit 3)
 * - Java File I/O dialog integration via FileChooser (Unit 3)
 * - Defensive validation and custom exception handling (Unit 3)
 * - Generics and Collections (List, Map) (Unit 3)
 * - Separation of Concerns: View -> Controller -> Service -> Repositories/Exporters (Unit 3)
 */
public class ReportsController implements Initializable {

    @FXML private ComboBox<ReportType> reportTypeCombo;
    @FXML private Label reportDescriptionLabel;
    @FXML private ComboBox<String> dateRangeCombo;
    @FXML private HBox customDateBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button exportPdfBtn;
    @FXML private Button exportExcelBtn;

    @FXML private HBox feedbackBanner;
    @FXML private Label feedbackIcon;
    @FXML private Label feedbackTitle;
    @FXML private Label feedbackDetails;
    @FXML private Button dismissFeedbackBtn;

    @FXML private Label previewScopeBadge;
    @FXML private Label previewRecordCountBadge;

    @FXML private GridPane kpiGrid;
    @FXML private VBox kpiCard1;
    @FXML private VBox kpiCard2;
    @FXML private VBox kpiCard3;
    @FXML private VBox kpiCard4;
    @FXML private Label kpiLabel1;
    @FXML private Label kpiLabel2;
    @FXML private Label kpiLabel3;
    @FXML private Label kpiLabel4;
    @FXML private Label kpiValue1;
    @FXML private Label kpiValue2;
    @FXML private Label kpiValue3;
    @FXML private Label kpiValue4;

    @FXML private VBox loadingOverlay;
    @FXML private Label loadingText;

    private final ReportService reportService;
    private ReportFilter activeFilter;

    public ReportsController() {
        this(new ReportService());
    }

    public ReportsController(ReportService reportService) {
        this.reportService = reportService != null ? reportService : new ReportService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind visibility to managed property for clean UI collapse
        if (feedbackBanner != null) {
            feedbackBanner.managedProperty().bind(feedbackBanner.visibleProperty());
            feedbackBanner.setVisible(false);
        }
        if (customDateBox != null) {
            customDateBox.managedProperty().bind(customDateBox.visibleProperty());
            customDateBox.setVisible(false);
        }
        if (loadingOverlay != null) {
            loadingOverlay.managedProperty().bind(loadingOverlay.visibleProperty());
            loadingOverlay.setVisible(false);
        }

        // Initialize Report Type ComboBox
        setupReportTypeCombo();

        // Initialize Date Range ComboBox
        setupDateRangeCombo();

        // Initialize default filter
        activeFilter = ReportFilter.predefined(DateRangeFilter.ALL_TIME);

        // Populate initial preview
        refreshPreviewAsync();
    }

    private void setupReportTypeCombo() {
        if (reportTypeCombo == null) return;

        reportTypeCombo.getItems().setAll(ReportType.values());
        reportTypeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ReportType type) {
                return type != null ? type.getDisplayName() : "";
            }

            @Override
            public ReportType fromString(String string) {
                for (ReportType rt : ReportType.values()) {
                    if (rt.getDisplayName().equalsIgnoreCase(string)) return rt;
                }
                return null;
            }
        });

        reportTypeCombo.setValue(ReportType.JOB_APPLICATIONS);
        if (reportDescriptionLabel != null) {
            reportDescriptionLabel.setText(ReportType.JOB_APPLICATIONS.getDescription());
        }
    }

    private void setupDateRangeCombo() {
        if (dateRangeCombo == null) return;

        dateRangeCombo.getItems().setAll(
                "All Time",
                "Last 30 Days",
                "Last 3 Months",
                "Last 6 Months",
                "Last 12 Months",
                "Custom Date Range"
        );
        dateRangeCombo.setValue("All Time");

        // Set default dates for custom range pickers
        if (startDatePicker != null) startDatePicker.setValue(LocalDate.now().minusMonths(1));
        if (endDatePicker != null) endDatePicker.setValue(LocalDate.now());
    }

    @FXML
    private void handleReportTypeChanged(ActionEvent event) {
        ReportType selected = reportTypeCombo.getValue();
        if (selected != null && reportDescriptionLabel != null) {
            reportDescriptionLabel.setText(selected.getDescription());
        }
        refreshPreviewAsync();
    }

    @FXML
    private void handleDateRangeChanged(ActionEvent event) {
        String selected = dateRangeCombo.getValue();
        if (selected == null) return;

        if ("Custom Date Range".equalsIgnoreCase(selected)) {
            if (customDateBox != null) {
                customDateBox.setVisible(true);
            }
        } else {
            if (customDateBox != null) {
                customDateBox.setVisible(false);
            }
            DateRangeFilter filter = mapStringToDateRangeFilter(selected);
            activeFilter = ReportFilter.predefined(filter);
            refreshPreviewAsync();
        }
    }

    @FXML
    private void handleApplyCustomRange(ActionEvent event) {
        LocalDate start = startDatePicker != null ? startDatePicker.getValue() : null;
        LocalDate end = endDatePicker != null ? endDatePicker.getValue() : null;

        if (start == null || end == null) {
            showErrorBanner("Date Selection Incomplete", "Please select both a Start Date and an End Date.");
            return;
        }

        if (start.isAfter(end)) {
            showErrorBanner("Invalid Date Range", "Start Date (" + start + ") cannot be after End Date (" + end + ").");
            return;
        }

        try {
            activeFilter = ReportFilter.custom(start, end);
            hideFeedbackBanner();
            refreshPreviewAsync();
        } catch (ValidationException e) {
            showErrorBanner("Validation Error", e.getMessage());
        }
    }

    @FXML
    private void handleRefreshPreview(ActionEvent event) {
        refreshPreviewAsync();
    }

    @FXML
    private void handleDismissFeedback(ActionEvent event) {
        hideFeedbackBanner();
    }

    @FXML
    private void handleExportPdf(ActionEvent event) {
        triggerExport(event, ".pdf", "PDF Documents (*.pdf)", "📄 Exporting PDF Report...");
    }

    @FXML
    private void handleExportExcel(ActionEvent event) {
        triggerExport(event, ".xlsx", "Excel Workbooks (*.xlsx)", "📊 Exporting Excel Workbook (.xlsx)...");
    }

    private void triggerExport(ActionEvent event, String extension, String filterDescription, String loadingMsg) {
        UserSession session = UserSession.getInstance();
        if (!session.isAuthenticated()) {
            showErrorAlert("Authentication Required", "You must be logged in to export report data.");
            return;
        }

        ReportType type = reportTypeCombo.getValue();
        if (type == null) type = ReportType.JOB_APPLICATIONS;

        String defaultFilename = type.getDefaultFilenamePrefix() + "_" + LocalDate.now() + extension;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save " + type.getDisplayName() + " - ApplyTrack");
        fileChooser.setInitialFileName(defaultFilename);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(filterDescription, "*" + extension));

        // Use home directory as fallback
        File userHome = new File(System.getProperty("user.home"));
        if (userHome.exists() && userHome.isDirectory()) {
            fileChooser.setInitialDirectory(userHome);
        }

        Window window = ((Node) event.getSource()).getScene().getWindow();
        File targetFile = fileChooser.showSaveDialog(window);

        if (targetFile == null) {
            return; // User canceled dialog
        }

        // Ensure extension is preserved
        if (!targetFile.getName().toLowerCase().endsWith(extension)) {
            targetFile = new File(targetFile.getParentFile(), targetFile.getName() + extension);
        }

        final File destination = targetFile;
        final int userId = session.getUserId();
        final String name = session.getUserFullName();
        final String email = session.getUserEmail();
        final ReportType selectedType = type;
        final ReportFilter selectedFilter = activeFilter != null ? activeFilter : ReportFilter.predefined(DateRangeFilter.ALL_TIME);

        showLoading(loadingMsg);

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ReportPayload payload = reportService.prepareReportPayload(userId, name, email, selectedType, selectedFilter);
                reportService.exportReport(payload, destination);
                return null;
            }
        };

        exportTask.setOnSucceeded(e -> {
            hideLoading();
            showSuccessBanner(
                    selectedType.getDisplayName() + " Exported Successfully",
                    "Saved to: " + destination.getAbsolutePath()
            );
        });

        exportTask.setOnFailed(e -> {
            hideLoading();
            Throwable ex = exportTask.getException();
            String errorMsg = ex != null ? ex.getMessage() : "Unknown export error occurred.";
            showErrorBanner("Export Operation Failed", errorMsg);
        });

        new Thread(exportTask, "ApplyTrack-ReportExportThread").start();
    }

    /**
     * Refreshes the preview KPI cards and record counts asynchronously without blocking the UI thread.
     */
    public void refreshPreviewAsync() {
        UserSession session = UserSession.getInstance();
        if (!session.isAuthenticated()) {
            return;
        }

        ReportType type = reportTypeCombo != null && reportTypeCombo.getValue() != null
                ? reportTypeCombo.getValue()
                : ReportType.JOB_APPLICATIONS;

        ReportFilter filter = activeFilter != null ? activeFilter : ReportFilter.predefined(DateRangeFilter.ALL_TIME);

        final int userId = session.getUserId();
        final String name = session.getUserFullName();
        final String email = session.getUserEmail();

        showLoading("Analyzing records and synthesizing report KPIs...");

        Task<ReportPayload> previewTask = new Task<>() {
            @Override
            protected ReportPayload call() throws Exception {
                return reportService.prepareReportPayload(userId, name, email, type, filter);
            }
        };

        previewTask.setOnSucceeded(e -> {
            hideLoading();
            ReportPayload payload = previewTask.getValue();
            updatePreviewUi(payload);
        });

        previewTask.setOnFailed(e -> {
            hideLoading();
            Throwable ex = previewTask.getException();
            String err = ex != null ? ex.getMessage() : "Failed to load report preview.";
            showErrorBanner("Preview Error", err);
        });

        new Thread(previewTask, "ApplyTrack-ReportPreviewThread").start();
    }

    private void updatePreviewUi(ReportPayload payload) {
        if (payload == null) return;

        if (previewScopeBadge != null) {
            previewScopeBadge.setText(payload.getFilter().getFormattedRange());
        }

        if (previewRecordCountBadge != null) {
            previewRecordCountBadge.setText(payload.getRecordCount() + " Records Found");
        }

        // Populate summary KPI cards
        Map<String, String> kpis = payload.getSummaryKpis();
        List<Map.Entry<String, String>> entryList = new ArrayList<>(kpis.entrySet());

        updateKpiCard(kpiLabel1, kpiValue1, entryList, 0, "Total Records", String.valueOf(payload.getRecordCount()));
        updateKpiCard(kpiLabel2, kpiValue2, entryList, 1, "Secondary KPI", "—");
        updateKpiCard(kpiLabel3, kpiValue3, entryList, 2, "Conversion KPI", "—");
        updateKpiCard(kpiLabel4, kpiValue4, entryList, 3, "Outcome KPI", "—");
    }

    private void updateKpiCard(Label label, Label value, List<Map.Entry<String, String>> entries, int index,
                               String defaultLabel, String defaultValue) {
        if (label == null || value == null) return;

        if (index < entries.size()) {
            Map.Entry<String, String> entry = entries.get(index);
            label.setText(entry.getKey());
            value.setText(entry.getValue());
        } else {
            label.setText(defaultLabel);
            value.setText(defaultValue);
        }
    }

    private DateRangeFilter mapStringToDateRangeFilter(String value) {
        if (value == null) return DateRangeFilter.ALL_TIME;
        return switch (value.trim()) {
            case "Last 30 Days" -> DateRangeFilter.LAST_30_DAYS;
            case "Last 3 Months" -> DateRangeFilter.LAST_3_MONTHS;
            case "Last 6 Months" -> DateRangeFilter.LAST_6_MONTHS;
            case "Last 12 Months" -> DateRangeFilter.LAST_12_MONTHS;
            default -> DateRangeFilter.ALL_TIME;
        };
    }

    private void showSuccessBanner(String title, String details) {
        if (feedbackBanner == null) return;
        feedbackBanner.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12px 18px;");
        if (feedbackIcon != null) feedbackIcon.setText("✅");
        if (feedbackTitle != null) {
            feedbackTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #166534;");
            feedbackTitle.setText(title);
        }
        if (feedbackDetails != null) {
            feedbackDetails.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #15803d;");
            feedbackDetails.setText(details);
        }
        feedbackBanner.setVisible(true);
    }

    private void showErrorBanner(String title, String details) {
        if (feedbackBanner == null) return;
        feedbackBanner.setStyle("-fx-background-color: #fef2f2; -fx-border-color: #fecaca; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12px 18px;");
        if (feedbackIcon != null) feedbackIcon.setText("❌");
        if (feedbackTitle != null) {
            feedbackTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #991b1b;");
            feedbackTitle.setText(title);
        }
        if (feedbackDetails != null) {
            feedbackDetails.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #b91c1c;");
            feedbackDetails.setText(details);
        }
        feedbackBanner.setVisible(true);
    }

    private void hideFeedbackBanner() {
        if (feedbackBanner != null) {
            feedbackBanner.setVisible(false);
        }
    }

    private void showLoading(String message) {
        if (loadingOverlay != null) {
            if (loadingText != null && message != null) {
                loadingText.setText(message);
            }
            loadingOverlay.setVisible(true);
        }
    }

    private void hideLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(false);
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

