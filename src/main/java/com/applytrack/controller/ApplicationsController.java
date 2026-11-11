package com.applytrack.controller;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.JobType;
import com.applytrack.service.ApplicationService;
import com.applytrack.session.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Job Application Management view (Module 2).
 *
 * Demonstrates:
 * - OOP: Encapsulation & MVC Layered Separation (Unit 3)
 * - JavaFX TableView with custom TableCell factories and property value mapping (Unit 3)
 * - Modern Java Date/Time formatting (java.time.LocalDate, DateTimeFormatter) - Unit 1 & 2
 * - Multithreading via javafx.concurrent.Task for background database queries (Unit 3)
 * - User-Specific Data Security: strictly uses UserSession.getUserId()
 * - Interactive Search, Filter, Sort and Confirmation Dialogs (Unit 3)
 */
public class ApplicationsController implements Initializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML private Label recordCountBadge;
    @FXML private Button addApplicationBtn;
    @FXML private Button refreshBtn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterBox;
    @FXML private ComboBox<String> jobTypeFilterBox;
    @FXML private ComboBox<String> sortByBox;
    @FXML private Button resetFiltersBtn;

    @FXML private Label statusMessageLabel;

    @FXML private TableView<Application> applicationsTable;
    @FXML private TableColumn<Application, String> companyCol;
    @FXML private TableColumn<Application, String> roleCol;
    @FXML private TableColumn<Application, String> typeCol;
    @FXML private TableColumn<Application, String> dateCol;
    @FXML private TableColumn<Application, String> deadlineCol;
    @FXML private TableColumn<Application, String> salaryCol;
    @FXML private TableColumn<Application, String> statusCol;

    @FXML private VBox emptyStateContainer;

    @FXML private Label selectedAppLabel;
    @FXML private Button scheduleInterviewBtn;
    @FXML private Button viewDetailsBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private final ObservableList<Application> masterData = FXCollections.observableArrayList();
    private ApplicationService applicationService;

    public ApplicationsController() {
        this(new ApplicationService());
    }

    public ApplicationsController(ApplicationService applicationService) {
        this.applicationService = applicationService != null ? applicationService : new ApplicationService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilterControls();
        setupSelectionListener();
        setupRowDoubleClickListener();

        loadApplicationsAsync();
    }

    private void setupTableColumns() {
        // 1. Company Name
        companyCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCompanyName()));
        companyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
                }
            }
        });

        // 2. Job Role
        roleCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getJobRole()));

        // 3. Job Type
        typeCol.setCellValueFactory(cellData -> {
            JobType type = cellData.getValue().getJobType();
            return new SimpleStringProperty(type != null ? type.getDisplayName() : "Full-time");
        });

        // 4. Application Date
        dateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getApplicationDate();
            return new SimpleStringProperty(date != null ? date.format(DATE_FORMATTER) : "—");
        });

        // 5. Deadline (with remaining days badge)
        deadlineCol.setCellValueFactory(cellData -> {
            LocalDate deadline = cellData.getValue().getDeadline();
            return new SimpleStringProperty(deadline != null ? deadline.format(DATE_FORMATTER) : "—");
        });
        deadlineCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "—".equals(item)) {
                    setText(empty ? null : "—");
                    setGraphic(null);
                    setStyle("-fx-text-fill: #94a3b8;");
                } else {
                    Application app = getTableRow().getItem();
                    if (app != null && app.getDeadline() != null) {
                        long days = ChronoUnit.DAYS.between(LocalDate.now(), app.getDeadline());
                        setText(item);
                        if (days < 0) {
                            setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;"); // Overdue
                        } else if (days <= 3) {
                            setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;"); // Urgent
                        } else {
                            setStyle("-fx-text-fill: #2563eb;"); // Upcoming
                        }
                    } else {
                        setText(item);
                        setStyle("");
                    }
                }
            }
        });

        // 6. Expected Salary / Stipend
        salaryCol.setCellValueFactory(cellData -> {
            BigDecimal salary = cellData.getValue().getExpectedSalary();
            if (salary != null && salary.compareTo(BigDecimal.ZERO) > 0) {
                return new SimpleStringProperty(CURRENCY_FORMAT.format(salary));
            }
            return new SimpleStringProperty("—");
        });

        // 7. Status Pill Badge
        statusCol.setCellValueFactory(cellData -> {
            ApplicationStatus status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status != null ? status.getDisplayName() : "Applied");
        });
        statusCol.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.setAlignment(Pos.CENTER);
                badge.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    badge.setText(item);
                    Application app = getTableRow().getItem();
                    String badgeColor = (app != null && app.getStatus() != null)
                            ? app.getStatus().getBadgeColor()
                            : "#6366f1";

                    badge.setStyle(
                            "-fx-background-color: " + badgeColor + "20; " +
                            "-fx-text-fill: " + badgeColor + "; " +
                            "-fx-font-weight: bold; -fx-font-size: 11px; " +
                            "-fx-padding: 3 8 3 8; -fx-background-radius: 12px; " +
                            "-fx-border-color: " + badgeColor + "50; -fx-border-radius: 12px;"
                    );
                    setGraphic(badge);
                }
            }
        });

        applicationsTable.setItems(masterData);
    }

    private void setupFilterControls() {
        // Status Filter
        statusFilterBox.getItems().add("All Statuses");
        for (ApplicationStatus status : ApplicationStatus.values()) {
            statusFilterBox.getItems().add(status.getDisplayName());
        }
        statusFilterBox.setValue("All Statuses");

        // Job Type Filter
        jobTypeFilterBox.getItems().add("All Types");
        for (JobType type : JobType.values()) {
            jobTypeFilterBox.getItems().add(type.getDisplayName());
        }
        jobTypeFilterBox.setValue("All Types");

        // Sort Options
        sortByBox.getItems().addAll(
                "Application Date (Newest)",
                "Application Date (Oldest)",
                "Deadline (Nearest)",
                "Company Name (A-Z)",
                "Company Name (Z-A)",
                "Job Role (A-Z)"
        );
        sortByBox.setValue("Application Date (Newest)");

        // Dynamic Filtering Listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilterBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        jobTypeFilterBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        sortByBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupSelectionListener() {
        applicationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = (newSelection != null);
            if (scheduleInterviewBtn != null) scheduleInterviewBtn.setDisable(!hasSelection);
            viewDetailsBtn.setDisable(!hasSelection);
            editBtn.setDisable(!hasSelection);
            deleteBtn.setDisable(!hasSelection);

            if (hasSelection) {
                selectedAppLabel.setText(newSelection.getJobRole() + " at " + newSelection.getCompanyName());
            } else {
                selectedAppLabel.setText("None selected (click a row above)");
            }
        });
    }

    private void setupRowDoubleClickListener() {
        applicationsTable.setRowFactory(tv -> {
            TableRow<Application> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    handleViewDetails(null);
                }
            });
            return row;
        });
    }

    /**
     * Loads applications asynchronously from MariaDB for the authenticated user.
     */
    public void loadApplicationsAsync() {
        int userId = UserSession.getInstance().getUserId();
        if (userId <= 0) {
            statusMessageLabel.setText("Guest Mode: Sign in to view your applications.");
            masterData.clear();
            updateEmptyState();
            return;
        }

        refreshBtn.setDisable(true);
        statusMessageLabel.setText("Syncing applications with MariaDB...");

        Task<List<Application>> task = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                return applicationService.getApplicationsForUser(userId);
            }
        };

        task.setOnSucceeded(e -> {
            masterData.setAll(task.getValue());
            applyFilters();
            refreshBtn.setDisable(false);
            statusMessageLabel.setText("Loaded " + masterData.size() + " applications from database.");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("Failed to load applications: " + ex.getMessage());
            ex.printStackTrace();
            refreshBtn.setDisable(false);
            statusMessageLabel.setText("Database error: " + ex.getMessage());
        });

        Thread worker = new Thread(task, "ApplyTrack-AppLoader");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Applies search, status filtering, and sorting to the table view.
     */
    private void applyFilters() {
        int userId = UserSession.getInstance().getUserId();
        if (userId <= 0) return;

        String keyword = searchField.getText();
        String selectedStatus = statusFilterBox.getValue();
        String selectedType = jobTypeFilterBox.getValue();
        String selectedSort = sortByBox.getValue();

        ApplicationStatus statusFilter = ("All Statuses".equals(selectedStatus) || selectedStatus == null)
                ? null
                : ApplicationStatus.fromString(selectedStatus);

        JobType jobTypeFilter = ("All Types".equals(selectedType) || selectedType == null)
                ? null
                : JobType.fromString(selectedType);

        String sortByCol = "application_date";
        boolean ascending = false;

        if (selectedSort != null) {
            switch (selectedSort) {
                case "Application Date (Oldest)" -> { sortByCol = "application_date"; ascending = true; }
                case "Deadline (Nearest)" -> { sortByCol = "deadline"; ascending = true; }
                case "Company Name (A-Z)" -> { sortByCol = "company_name"; ascending = true; }
                case "Company Name (Z-A)" -> { sortByCol = "company_name"; ascending = false; }
                case "Job Role (A-Z)" -> { sortByCol = "job_role"; ascending = true; }
                default -> { sortByCol = "application_date"; ascending = false; }
            }
        }

        final String finalSortBy = sortByCol;
        final boolean finalAscending = ascending;

        Task<List<Application>> filterTask = new Task<>() {
            @Override
            protected List<Application> call() throws Exception {
                return applicationService.getFilteredApplications(
                        userId, keyword, statusFilter, jobTypeFilter, finalSortBy, finalAscending
                );
            }
        };

        filterTask.setOnSucceeded(e -> {
            List<Application> filtered = filterTask.getValue();
            masterData.setAll(filtered);
            recordCountBadge.setText(filtered.size() + " Applications");
            updateEmptyState();
        });

        filterTask.setOnFailed(e -> {
            System.err.println("Filtering failed: " + filterTask.getException().getMessage());
        });

        Thread filterWorker = new Thread(filterTask, "ApplyTrack-FilterWorker");
        filterWorker.setDaemon(true);
        filterWorker.start();
    }

    private void updateEmptyState() {
        boolean isEmpty = masterData.isEmpty();
        applicationsTable.setVisible(!isEmpty);
        emptyStateContainer.setVisible(isEmpty);
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadApplicationsAsync();
    }

    @FXML
    private void handleResetFilters(ActionEvent event) {
        searchField.clear();
        statusFilterBox.setValue("All Statuses");
        jobTypeFilterBox.setValue("All Types");
        sortByBox.setValue("Application Date (Newest)");
        applyFilters();
    }

    @FXML
    private void handleAddNewApplication(ActionEvent event) {
        openApplicationDialog(null);
    }

    @FXML
    private void handleEditApplication(ActionEvent event) {
        Application selected = applicationsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openApplicationDialog(selected);
        }
    }

    @FXML
    private void handleDeleteApplication(ActionEvent event) {
        Application selected = applicationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Application Deletion");
        confirm.setHeaderText("Delete Application for " + selected.getCompanyName() + "?");
        confirm.setContentText("Are you sure you want to permanently delete your application for " +
                selected.getJobRole() + " at " + selected.getCompanyName() + "?\n\n" +
                "Note: Any associated interview rounds for this application will also be deleted.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int userId = UserSession.getInstance().getUserId();
            try {
                boolean deleted = applicationService.deleteApplication(selected.getId(), userId);
                if (deleted) {
                    masterData.remove(selected);
                    recordCountBadge.setText(masterData.size() + " Applications");
                    statusMessageLabel.setText("Deleted application #" + selected.getId() + " successfully.");
                    updateEmptyState();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Could not delete application record.");
                }
            } catch (ValidationException | SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Deletion Error", e.getMessage());
            }
        }
    }

    @FXML
    private void handleViewDetails(ActionEvent event) {
        Application selected = applicationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert detailsAlert = new Alert(Alert.AlertType.INFORMATION);
        detailsAlert.setTitle("Application Details — " + selected.getCompanyName());
        detailsAlert.setHeaderText(selected.getJobRole() + " @ " + selected.getCompanyName());

        StringBuilder sb = new StringBuilder();
        sb.append("• Application ID: #").append(selected.getId()).append("\n");
        sb.append("• Company: ").append(selected.getCompanyName()).append("\n");
        sb.append("• Position Role: ").append(selected.getJobRole()).append("\n");
        sb.append("• Employment Type: ").append(selected.getJobType().getDisplayName()).append("\n");
        sb.append("• Current Status: ").append(selected.getStatus().getDisplayName()).append("\n");
        sb.append("• Date Applied: ").append(selected.getApplicationDate().format(DATE_FORMATTER)).append("\n");
        sb.append("• Application Deadline: ").append(selected.getDeadline() != null ? selected.getDeadline().format(DATE_FORMATTER) : "None specified").append("\n");

        if (selected.getExpectedSalary() != null) {
            sb.append("• Expected Salary: ").append(CURRENCY_FORMAT.format(selected.getExpectedSalary())).append("\n");
        }
        if (selected.getJobUrl() != null && !selected.getJobUrl().isBlank()) {
            sb.append("• Posting URL: ").append(selected.getJobUrl()).append("\n");
        }
        if (selected.getJobDescription() != null && !selected.getJobDescription().isBlank()) {
            sb.append("\n[Job Description]\n").append(selected.getJobDescription()).append("\n");
        }
        if (selected.getNotes() != null && !selected.getNotes().isBlank()) {
            sb.append("\n[Personal Notes]\n").append(selected.getNotes()).append("\n");
        }

        detailsAlert.setContentText(sb.toString());
        detailsAlert.showAndWait();
    }

    @FXML
    private void handleScheduleInterview(ActionEvent event) {
        Application selected = applicationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            URL fxmlUrl = getClass().getResource("/com/applytrack/view/InterviewDialog.fxml");
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "UI Error", "InterviewDialog.fxml not found on classpath.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            InterviewDialogController controller = loader.getController();
            controller.setApplicationService(applicationService);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Schedule Interview — " + selected.getCompanyName());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            controller.setPreselectedApplication(selected);

            dialogStage.showAndWait();

            if (controller.isSaved()) {
                statusMessageLabel.setText("Interview scheduled successfully for " + selected.getCompanyName() + "!");
            }

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Dialog Error", "Could not open interview dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openApplicationDialog(Application applicationToEdit) {
        try {
            URL fxmlUrl = getClass().getResource("/com/applytrack/view/ApplicationDialog.fxml");
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "UI Error", "ApplicationDialog.fxml not found on classpath.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            ApplicationDialogController controller = loader.getController();
            controller.setApplicationService(applicationService);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle(applicationToEdit == null ? "Add New Job Application" : "Edit Job Application");
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            if (applicationToEdit != null) {
                controller.setApplication(applicationToEdit);
            }

            dialogStage.showAndWait();

            if (controller.isSaved()) {
                loadApplicationsAsync();
                statusMessageLabel.setText(applicationToEdit == null
                        ? "Successfully added new application for " + controller.getApplication().getCompanyName()
                        : "Updated application #" + controller.getApplication().getId());
            }

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Dialog Error", "Could not load application dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

