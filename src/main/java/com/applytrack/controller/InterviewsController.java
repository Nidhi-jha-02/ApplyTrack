package com.applytrack.controller;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Interview;
import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.service.ApplicationService;
import com.applytrack.service.InterviewService;
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
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Interview Pipeline Management view (Module 3).
 *
 * Demonstrates:
 * - OOP: Encapsulation & MVC Layered Separation (Unit 3)
 * - JavaFX TableView with custom TableCell factories and property value mapping (Unit 3)
 * - Modern Java Date/Time formatting (LocalDateTime, DateTimeFormatter) - Unit 1 & 2
 * - Multithreading via javafx.concurrent.Task for background database queries (Unit 3)
 * - User-Specific Data Security: strictly uses UserSession.getUserId()
 * - Interactive Search, Timeline & Outcome Filters, Dynamic Sorting & Modal Dialogs (Unit 3)
 */
public class InterviewsController implements Initializable {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);

    @FXML private Label upcomingCountBadge;
    @FXML private Label recordCountBadge;
    @FXML private Button scheduleInterviewBtn;
    @FXML private Button refreshBtn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> timelineFilterBox;
    @FXML private ComboBox<String> outcomeFilterBox;
    @FXML private ComboBox<String> sortByBox;
    @FXML private Button resetFiltersBtn;

    @FXML private Label statusMessageLabel;

    @FXML private TableView<Interview> interviewsTable;
    @FXML private TableColumn<Interview, String> companyCol;
    @FXML private TableColumn<Interview, String> roleCol;
    @FXML private TableColumn<Interview, String> roundCol;
    @FXML private TableColumn<Interview, String> dateTimeCol;
    @FXML private TableColumn<Interview, String> modeCol;
    @FXML private TableColumn<Interview, String> outcomeCol;
    @FXML private TableColumn<Interview, String> interviewerCol;

    @FXML private VBox emptyStateContainer;

    @FXML private Label selectedInterviewLabel;
    @FXML private Button viewDetailsBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private final ObservableList<Interview> masterData = FXCollections.observableArrayList();
    private InterviewService interviewService;
    private ApplicationService applicationService;

    public InterviewsController() {
        this(new InterviewService(), new ApplicationService());
    }

    public InterviewsController(InterviewService interviewService, ApplicationService applicationService) {
        this.interviewService = interviewService != null ? interviewService : new InterviewService();
        this.applicationService = applicationService != null ? applicationService : new ApplicationService();
    }

    public void setInterviewService(InterviewService interviewService) {
        if (interviewService != null) this.interviewService = interviewService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilterControls();
        setupSelectionListener();
        setupDoubleClickHandler();

        // Load data on startup
        loadInterviewsAsync();
    }

    private void setupTableColumns() {
        // 1. Company Name
        companyCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCompanyName()));

        // 2. Job Role
        roleCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getJobRole()));

        // 3. Round Name
        roundCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoundName()));

        // 4. Date & Time
        dateTimeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedDateTime()));

        // 5. Mode Pill Badge (Online vs Offline)
        modeCol.setCellValueFactory(cellData -> {
            InterviewMode mode = cellData.getValue().getInterviewMode();
            return new SimpleStringProperty(mode != null ? mode.getDisplayName() : "Online");
        });
        modeCol.setCellFactory(col -> new TableCell<>() {
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
                    boolean isOnline = "Online".equalsIgnoreCase(item);
                    String bg = isOnline ? "#e0f2fe" : "#f1f5f9";
                    String text = isOnline ? "#0369a1" : "#475569";
                    badge.setStyle(
                            "-fx-background-color: " + bg + "; " +
                            "-fx-text-fill: " + text + "; " +
                            "-fx-font-weight: bold; -fx-font-size: 11px; " +
                            "-fx-padding: 3 8 3 8; -fx-background-radius: 12px;"
                    );
                    setGraphic(badge);
                }
            }
        });

        // 6. Outcome Pill Badge
        outcomeCol.setCellValueFactory(cellData -> {
            InterviewOutcome outcome = cellData.getValue().getOutcome();
            return new SimpleStringProperty(outcome != null ? outcome.getDisplayName() : "Scheduled");
        });
        outcomeCol.setCellFactory(col -> new TableCell<>() {
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
                    Interview interview = getTableRow().getItem();
                    String badgeColor = (interview != null && interview.getOutcome() != null)
                            ? interview.getOutcome().getBadgeColor()
                            : "#f59e0b";

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

        // 7. Interviewer / Meeting Info
        interviewerCol.setCellValueFactory(cellData -> {
            Interview interview = cellData.getValue();
            String interviewer = interview.getInterviewerName();
            String link = interview.getMeetingLink();
            String loc = interview.getInterviewLocation();

            if (interviewer != null && !interviewer.isBlank()) {
                return new SimpleStringProperty(interviewer);
            } else if (link != null && !link.isBlank()) {
                return new SimpleStringProperty("Link attached");
            } else if (loc != null && !loc.isBlank()) {
                return new SimpleStringProperty(loc);
            }
            return new SimpleStringProperty("—");
        });

        interviewsTable.setItems(masterData);
    }

    private void setupFilterControls() {
        // Timeline Filter
        timelineFilterBox.getItems().addAll("All Interviews", "Upcoming Interviews", "Completed / Past");
        timelineFilterBox.setValue("All Interviews");

        // Outcome Filter
        outcomeFilterBox.getItems().add("All Outcomes");
        for (InterviewOutcome outcome : InterviewOutcome.values()) {
            outcomeFilterBox.getItems().add(outcome.getDisplayName());
        }
        outcomeFilterBox.setValue("All Outcomes");

        // Sort Options
        sortByBox.getItems().addAll(
                "Interview Date (Nearest First)",
                "Interview Date (Farthest First)",
                "Company Name (A-Z)",
                "Round Name (A-Z)",
                "Outcome Status"
        );
        sortByBox.setValue("Interview Date (Nearest First)");

        // Search text change listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadInterviewsAsync());

        // ComboBox change listeners
        timelineFilterBox.valueProperty().addListener((obs, oldVal, newVal) -> loadInterviewsAsync());
        outcomeFilterBox.valueProperty().addListener((obs, oldVal, newVal) -> loadInterviewsAsync());
        sortByBox.valueProperty().addListener((obs, oldVal, newVal) -> loadInterviewsAsync());
    }

    private void setupSelectionListener() {
        interviewsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, selected) -> {
            boolean hasSelection = selected != null;
            viewDetailsBtn.setDisable(!hasSelection);
            editBtn.setDisable(!hasSelection);
            deleteBtn.setDisable(!hasSelection);

            if (hasSelection) {
                selectedInterviewLabel.setText(
                        "#" + selected.getId() + " • " + selected.getCompanyName() +
                        " — " + selected.getRoundName() + " (" + selected.getFormattedDateTime() + ")"
                );
            } else {
                selectedInterviewLabel.setText("None selected (click a row above)");
            }
        });
    }

    private void setupDoubleClickHandler() {
        interviewsTable.setRowFactory(tv -> {
            TableRow<Interview> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    handleViewDetails(null);
                }
            });
            return row;
        });
    }

    /**
     * Loads interviews from MariaDB asynchronously via JavaFX Task.
     */
    public void loadInterviewsAsync() {
        UserSession session = UserSession.getInstance();
        final int userId = session.getUserId();

        if (userId <= 0) {
            statusMessageLabel.setText("Not authenticated. Please log in.");
            masterData.clear();
            updateEmptyState();
            return;
        }

        final String keyword = searchField.getText() != null ? searchField.getText().trim() : null;
        final String timelineVal = timelineFilterBox.getValue();
        final String outcomeVal = outcomeFilterBox.getValue();
        final String sortVal = sortByBox.getValue();

        // Map UI sort value to database column and direction
        final String sortBy;
        final boolean ascending;

        if ("Interview Date (Nearest First)".equals(sortVal)) {
            sortBy = "date";
            ascending = true;
        } else if ("Interview Date (Farthest First)".equals(sortVal)) {
            sortBy = "date";
            ascending = false;
        } else if ("Company Name (A-Z)".equals(sortVal)) {
            sortBy = "company";
            ascending = true;
        } else if ("Round Name (A-Z)".equals(sortVal)) {
            sortBy = "round";
            ascending = true;
        } else if ("Outcome Status".equals(sortVal)) {
            sortBy = "outcome";
            ascending = true;
        } else {
            sortBy = "date";
            ascending = true;
        }

        statusMessageLabel.setText("Loading interview pipeline from MariaDB...");

        Task<List<Interview>> task = new Task<>() {
            @Override
            protected List<Interview> call() {
                return interviewService.getFilteredInterviews(
                        userId,
                        keyword,
                        outcomeVal,
                        timelineVal,
                        sortBy,
                        ascending
                );
            }
        };

        task.setOnSucceeded(event -> {
            List<Interview> results = task.getValue();
            masterData.setAll(results);
            updateEmptyState();

            int total = interviewService.getInterviewCount(userId);
            int upcoming = interviewService.getUpcomingInterviewCount(userId);

            if (recordCountBadge != null) {
                recordCountBadge.setText(total + (total == 1 ? " Interview" : " Interviews"));
            }
            if (upcomingCountBadge != null) {
                upcomingCountBadge.setText(upcoming + (upcoming == 1 ? " Upcoming" : " Upcoming"));
            }

            statusMessageLabel.setText(
                    "Showing " + results.size() + " of " + total + " interviews | Last refreshed: " +
                    LocalTime.now().format(TIME_FORMATTER)
            );
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            System.err.println("Error loading interviews: " + ex.getMessage());
            statusMessageLabel.setText("Error querying interviews: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    private void updateEmptyState() {
        boolean isEmpty = masterData.isEmpty();
        emptyStateContainer.setVisible(isEmpty);
        interviewsTable.setVisible(!isEmpty);
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadInterviewsAsync();
    }

    @FXML
    private void handleResetFilters(ActionEvent event) {
        searchField.clear();
        timelineFilterBox.setValue("All Interviews");
        outcomeFilterBox.setValue("All Outcomes");
        sortByBox.setValue("Interview Date (Nearest First)");
        loadInterviewsAsync();
    }

    @FXML
    public void handleScheduleInterview(ActionEvent event) {
        openInterviewDialog(null);
    }

    @FXML
    private void handleEditInterview(ActionEvent event) {
        Interview selected = interviewsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select an interview row to edit.");
            return;
        }
        openInterviewDialog(selected);
    }

    @FXML
    private void handleDeleteInterview(ActionEvent event) {
        Interview selected = interviewsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select an interview to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Interview Deletion");
        confirm.setHeaderText("Delete interview round: " + selected.getRoundName() + "?");
        confirm.setContentText(
                "Are you sure you want to delete this interview for " + selected.getCompanyName() +
                " (" + selected.getJobRole() + ")?\n\nThis action cannot be undone."
        );

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int userId = UserSession.getInstance().getUserId();
            try {
                boolean deleted = interviewService.deleteInterview(selected.getId(), userId);
                if (deleted) {
                    loadInterviewsAsync();
                    showAlert(Alert.AlertType.INFORMATION, "Deleted", "Interview round #" + selected.getId() + " was deleted successfully.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Could not delete interview round.");
                }
            } catch (ValidationException | SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Deletion Error", e.getMessage());
            }
        }
    }

    @FXML
    private void handleViewDetails(ActionEvent event) {
        Interview selected = interviewsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert detailsAlert = new Alert(Alert.AlertType.INFORMATION);
        detailsAlert.setTitle("Interview Round Details — " + selected.getCompanyName());
        detailsAlert.setHeaderText(selected.getRoundName() + " @ " + selected.getCompanyName());

        StringBuilder sb = new StringBuilder();
        sb.append("• Interview ID: #").append(selected.getId()).append("\n");
        sb.append("• Company: ").append(selected.getCompanyName()).append("\n");
        sb.append("• Job Role: ").append(selected.getJobRole()).append("\n");
        sb.append("• Round: ").append(selected.getRoundName()).append("\n");
        sb.append("• Date & Time: ").append(selected.getFormattedDateTime()).append("\n");
        sb.append("• Mode: ").append(selected.getInterviewMode().getDisplayName()).append("\n");
        sb.append("• Outcome: ").append(selected.getOutcome().getDisplayName()).append("\n");

        if (selected.getInterviewerName() != null && !selected.getInterviewerName().isBlank()) {
            sb.append("• Interviewer / Panel: ").append(selected.getInterviewerName()).append("\n");
        }
        if (selected.getMeetingLink() != null && !selected.getMeetingLink().isBlank()) {
            sb.append("• Video Meeting Link: ").append(selected.getMeetingLink()).append("\n");
        }
        if (selected.getInterviewLocation() != null && !selected.getInterviewLocation().isBlank()) {
            sb.append("• Physical Location: ").append(selected.getInterviewLocation()).append("\n");
        }
        if (selected.getFeedback() != null && !selected.getFeedback().isBlank()) {
            sb.append("\n[Feedback & Preparation Notes]\n").append(selected.getFeedback()).append("\n");
        }

        detailsAlert.setContentText(sb.toString());
        detailsAlert.showAndWait();
    }

    private void openInterviewDialog(Interview interviewToEdit) {
        try {
            URL fxmlUrl = getClass().getResource("/com/applytrack/view/InterviewDialog.fxml");
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "UI Error", "InterviewDialog.fxml not found on classpath.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            InterviewDialogController controller = loader.getController();
            controller.setInterviewService(interviewService);
            controller.setApplicationService(applicationService);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle(interviewToEdit == null ? "Schedule Interview Round" : "Edit Interview Round");
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            if (interviewToEdit != null) {
                controller.setInterview(interviewToEdit);
            }

            dialogStage.showAndWait();

            if (controller.isSaved()) {
                loadInterviewsAsync();
                statusMessageLabel.setText(interviewToEdit == null
                        ? "Successfully scheduled new interview for " + controller.getInterview().getCompanyName()
                        : "Updated interview #" + controller.getInterview().getId());
            }

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Dialog Error", "Could not load interview dialog: " + e.getMessage());
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

