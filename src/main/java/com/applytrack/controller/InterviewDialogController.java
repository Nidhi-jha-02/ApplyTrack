package com.applytrack.controller;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.Interview;
import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.service.ApplicationService;
import com.applytrack.service.InterviewService;
import com.applytrack.session.UserSession;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for the Schedule & Edit Interview modal dialog.
 *
 * Demonstrates:
 * - OOP: Encapsulation & MVC Layered Separation (Unit 3)
 * - Modern Java Date & Time API (LocalDate, LocalTime, LocalDateTime) - Unit 1 & 2
 * - Form validation and multi-format time parser (Unit 1 & 2)
 * - Custom ComboBox StringConverter and CellFactory (Unit 3)
 * - User-Specific Data Security: strictly uses UserSession.getUserId()
 */
public class InterviewDialogController implements Initializable {

    private static final DateTimeFormatter TIME_12H = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
    private static final DateTimeFormatter TIME_12H_NO_SPACE = DateTimeFormatter.ofPattern("hh:mma", Locale.US);
    private static final DateTimeFormatter TIME_24H = DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    @FXML private Label dialogTitleLabel;
    @FXML private Label errorLabel;

    @FXML private ComboBox<Application> applicationBox;
    @FXML private TextField roundField;
    @FXML private ComboBox<String> modeBox;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeBox;
    @FXML private ComboBox<String> outcomeBox;
    @FXML private TextField interviewerField;
    @FXML private TextField meetingLinkField;
    @FXML private TextField locationField;
    @FXML private TextArea feedbackArea;

    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    private Stage dialogStage;
    private Interview interview;
    private boolean saved = false;

    private InterviewService interviewService;
    private ApplicationService applicationService;

    public InterviewDialogController() {
        this.interviewService = new InterviewService();
        this.applicationService = new ApplicationService();
    }

    public void setInterviewService(InterviewService interviewService) {
        if (interviewService != null) this.interviewService = interviewService;
    }

    public void setApplicationService(ApplicationService applicationService) {
        if (applicationService != null) this.applicationService = applicationService;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaved() {
        return saved;
    }

    public Interview getInterview() {
        return interview;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize Mode ComboBox
        modeBox.setItems(FXCollections.observableArrayList(
                InterviewMode.ONLINE.getDisplayName(),
                InterviewMode.OFFLINE.getDisplayName()
        ));
        modeBox.setValue(InterviewMode.ONLINE.getDisplayName());

        // Initialize Outcome ComboBox
        outcomeBox.setItems(FXCollections.observableArrayList(
                InterviewOutcome.SCHEDULED.getDisplayName(),
                InterviewOutcome.PENDING.getDisplayName(),
                InterviewOutcome.PASSED.getDisplayName(),
                InterviewOutcome.FAILED.getDisplayName(),
                InterviewOutcome.ON_HOLD.getDisplayName(),
                InterviewOutcome.CANCELLED.getDisplayName()
        ));
        outcomeBox.setValue(InterviewOutcome.SCHEDULED.getDisplayName());

        // Initialize standard time suggestions
        timeBox.setItems(FXCollections.observableArrayList(
                "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM",
                "12:00 PM", "01:00 PM", "02:00 PM", "02:30 PM", "03:00 PM", "03:30 PM",
                "04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM", "06:00 PM"
        ));
        timeBox.setValue("02:30 PM");

        // Default date is today or tomorrow
        datePicker.setValue(LocalDate.now());

        // Configure Application ComboBox display
        configureApplicationBox();

        // Load active user's applications
        loadUserApplications();
    }

    private void configureApplicationBox() {
        StringConverter<Application> converter = new StringConverter<>() {
            @Override
            public String toString(Application app) {
                if (app == null) return "";
                return app.getCompanyName() + " — " + app.getJobRole() + " (#" + app.getId() + ")";
            }

            @Override
            public Application fromString(String string) {
                return null;
            }
        };

        applicationBox.setConverter(converter);
        applicationBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Application item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCompanyName() + " — " + item.getJobRole() + " (#" + item.getId() + ")");
                }
            }
        });
        applicationBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Application item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCompanyName() + " — " + item.getJobRole() + " (#" + item.getId() + ")");
                }
            }
        });
    }

    private void loadUserApplications() {
        int userId = UserSession.getInstance().getUserId();
        if (userId <= 0) return;

        try {
            List<Application> apps = applicationService.getApplicationsForUser(userId);
            applicationBox.setItems(FXCollections.observableArrayList(apps));
            if (!apps.isEmpty()) {
                applicationBox.setValue(apps.get(0));
            } else {
                showError("No active job applications found. Please create a job application in the Applications module first before scheduling interviews.");
            }
        } catch (SQLException e) {
            System.err.println("Error loading user applications for interview dialog: " + e.getMessage());
            showError("Could not retrieve applications: " + e.getMessage());
        }
    }

    /**
     * Pre-selects a specific application when scheduling from the Applications module.
     */
    public void setPreselectedApplication(Application app) {
        if (app == null) return;
        // Look up in list
        for (Application existing : applicationBox.getItems()) {
            if (existing.getId() == app.getId()) {
                applicationBox.setValue(existing);
                return;
            }
        }
        applicationBox.getItems().add(app);
        applicationBox.setValue(app);
    }

    /**
     * Populates form fields for editing an existing interview record.
     */
    public void setInterview(Interview interview) {
        this.interview = interview;
        if (interview == null) return;

        dialogTitleLabel.setText("Edit Interview Round #" + interview.getId());
        saveBtn.setText("Update Interview");

        // Match application
        if (interview.getApplication() != null) {
            setPreselectedApplication(interview.getApplication());
        } else {
            for (Application app : applicationBox.getItems()) {
                if (app.getId() == interview.getApplicationId()) {
                    applicationBox.setValue(app);
                    break;
                }
            }
        }
        // Disable changing the parent application during edit to maintain relationship integrity
        applicationBox.setDisable(true);

        roundField.setText(interview.getRoundName());

        if (interview.getInterviewMode() != null) {
            modeBox.setValue(interview.getInterviewMode().getDisplayName());
        }

        if (interview.getInterviewDate() != null) {
            datePicker.setValue(interview.getInterviewDate().toLocalDate());
            timeBox.setValue(interview.getInterviewDate().format(TIME_12H));
        }

        if (interview.getOutcome() != null) {
            outcomeBox.setValue(interview.getOutcome().getDisplayName());
        }

        if (interview.getInterviewerName() != null) {
            interviewerField.setText(interview.getInterviewerName());
        }

        if (interview.getMeetingLink() != null) {
            meetingLinkField.setText(interview.getMeetingLink());
        }

        if (interview.getInterviewLocation() != null) {
            locationField.setText(interview.getInterviewLocation());
        }

        if (interview.getFeedback() != null) {
            feedbackArea.setText(interview.getFeedback());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        hideError();

        Application selectedApp = applicationBox.getValue();
        if (selectedApp == null) {
            showError("Please select a job application for this interview.");
            return;
        }

        String roundName = roundField.getText() != null ? roundField.getText().trim() : "";
        if (roundName.isEmpty()) {
            showError("Interview round name is required (e.g. 'Technical Assessment', 'HR Round').");
            roundField.requestFocus();
            return;
        }

        LocalDate date = datePicker.getValue();
        if (date == null) {
            showError("Please select an interview date.");
            datePicker.requestFocus();
            return;
        }

        String timeText = timeBox.getValue() != null ? timeBox.getValue().trim() : "";
        LocalTime time = parseTime(timeText);
        if (time == null) {
            showError("Invalid time format: '" + timeText + "'. Please enter time in 12h (e.g. 02:30 PM) or 24h (e.g. 14:30) format.");
            timeBox.requestFocus();
            return;
        }

        LocalDateTime interviewDateTime = LocalDateTime.of(date, time);

        InterviewMode mode = InterviewMode.fromString(modeBox.getValue());
        InterviewOutcome outcome = InterviewOutcome.fromString(outcomeBox.getValue());

        String interviewer = interviewerField.getText() != null ? interviewerField.getText().trim() : null;
        String meetingLink = meetingLinkField.getText() != null ? meetingLinkField.getText().trim() : null;
        String location = locationField.getText() != null ? locationField.getText().trim() : null;
        String feedback = feedbackArea.getText() != null ? feedbackArea.getText().trim() : null;

        int userId = UserSession.getInstance().getUserId();

        try {
            if (interview == null) {
                // Schedule new interview
                this.interview = interviewService.scheduleInterview(
                        selectedApp.getId(),
                        userId,
                        roundName,
                        interviewDateTime,
                        mode,
                        location,
                        meetingLink,
                        interviewer,
                        feedback,
                        outcome
                );
            } else {
                // Update existing interview
                this.interview = interviewService.updateInterview(
                        interview.getId(),
                        userId,
                        roundName,
                        interviewDateTime,
                        mode,
                        location,
                        meetingLink,
                        interviewer,
                        feedback,
                        outcome
                );
            }

            saved = true;
            if (dialogStage != null) {
                dialogStage.close();
            }

        } catch (ValidationException ve) {
            showError(ve.getMessage());
        } catch (SQLException sqle) {
            showError("Database error while saving interview: " + sqle.getMessage());
        }
    }

    /**
     * Flexible parser supporting 12-hour AM/PM, 24-hour HH:mm, and single digit hour formats.
     */
    private LocalTime parseTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String clean = text.trim();

        // Try standard 12-hour formats
        try {
            return LocalTime.parse(clean.toUpperCase(Locale.US), TIME_12H);
        } catch (DateTimeParseException ignored) {}

        try {
            return LocalTime.parse(clean.toUpperCase(Locale.US), TIME_12H_NO_SPACE);
        } catch (DateTimeParseException ignored) {}

        // Try standard 24-hour format (e.g., "14:30")
        try {
            return LocalTime.parse(clean, TIME_24H);
        } catch (DateTimeParseException ignored) {}

        // Try single-digit hour 24-hour format (e.g., "9:30")
        try {
            if (clean.matches("^\\d:\\d{2}$")) {
                return LocalTime.parse("0" + clean, TIME_24H);
            }
        } catch (DateTimeParseException ignored) {}

        // Try ISO local time
        try {
            return LocalTime.parse(clean);
        } catch (DateTimeParseException ignored) {}

        return null;
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}
