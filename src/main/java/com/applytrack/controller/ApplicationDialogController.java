package com.applytrack.controller;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.JobType;
import com.applytrack.service.ApplicationService;
import com.applytrack.session.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Add / Edit Application modal dialog.
 *
 * Demonstrates:
 * - OOP: Encapsulation & Separation of Concerns (Unit 3)
 * - JavaFX Form Controls and Event Handling (Unit 3)
 * - Java Enums in ComboBox controls (Unit 2)
 * - Modern Java Date API: LocalDate & DatePicker (Unit 1 & 2)
 * - BigDecimal parsing and error handling (Unit 1 & 3)
 */
public class ApplicationDialogController implements Initializable {

    @FXML private Label dialogTitleLabel;
    @FXML private Label errorLabel;
    @FXML private TextField companyField;
    @FXML private TextField roleField;
    @FXML private ComboBox<JobType> jobTypeBox;
    @FXML private ComboBox<ApplicationStatus> statusBox;
    @FXML private DatePicker appDatePicker;
    @FXML private DatePicker deadlinePicker;
    @FXML private TextField salaryField;
    @FXML private TextField urlField;
    @FXML private ComboBox<com.applytrack.model.Recruiter> recruiterBox;
    @FXML private TextArea descArea;
    @FXML private TextArea notesArea;
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    private Stage dialogStage;
    private Application application;
    private boolean saved = false;
    private ApplicationService applicationService = new ApplicationService();
    private com.applytrack.service.RecruiterService recruiterService = new com.applytrack.service.RecruiterService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jobTypeBox.getItems().setAll(JobType.values());
        jobTypeBox.setValue(JobType.FULL_TIME);

        statusBox.getItems().setAll(ApplicationStatus.values());
        statusBox.setValue(ApplicationStatus.APPLIED);

        appDatePicker.setValue(LocalDate.now());
        configureRecruiterBox();
        loadUserRecruiters();
        hideError();
    }

    private void configureRecruiterBox() {
        if (recruiterBox == null) return;
        recruiterBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(com.applytrack.model.Recruiter r) {
                if (r == null || r.getId() <= 0) return "None (Unassigned)";
                return r.getRecruiterName() + " (" + r.getCompanyName() + ")";
            }

            @Override
            public com.applytrack.model.Recruiter fromString(String string) {
                return null;
            }
        });

        recruiterBox.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(com.applytrack.model.Recruiter item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getId() <= 0) {
                    setText("None (Unassigned)");
                } else {
                    setText(item.getRecruiterName() + " (" + item.getCompanyName() + ")");
                }
            }
        });

        recruiterBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(com.applytrack.model.Recruiter item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getId() <= 0) {
                    setText("None (Unassigned)");
                } else {
                    setText(item.getRecruiterName() + " (" + item.getCompanyName() + ")");
                }
            }
        });
    }

    private void loadUserRecruiters() {
        if (recruiterBox == null) return;
        int userId = UserSession.getInstance().getUserId();
        if (userId <= 0) return;

        List<com.applytrack.model.Recruiter> recruiters = recruiterService.getRecruitersForUser(userId);
        recruiterBox.getItems().clear();
        // Add null / unassigned sentinel
        com.applytrack.model.Recruiter unassigned = new com.applytrack.model.Recruiter();
        unassigned.setId(0);
        unassigned.setRecruiterName("None (Unassigned)");
        recruiterBox.getItems().add(unassigned);
        recruiterBox.getItems().addAll(recruiters);
        recruiterBox.setValue(unassigned);
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setApplicationService(ApplicationService applicationService) {
        if (applicationService != null) {
            this.applicationService = applicationService;
        }
    }

    /**
     * Pre-populates the dialog fields when editing an existing application.
     */
    public void setApplication(Application application) {
        this.application = application;
        if (application != null) {
            dialogTitleLabel.setText("Edit Job Application");
            saveBtn.setText("Update Application");

            if (application.getCompany() != null) {
                companyField.setText(application.getCompany().getCompanyName());
            }
            roleField.setText(application.getJobRole());
            if (application.getJobType() != null) {
                jobTypeBox.setValue(application.getJobType());
            }
            if (application.getStatus() != null) {
                statusBox.setValue(application.getStatus());
            }
            if (application.getApplicationDate() != null) {
                appDatePicker.setValue(application.getApplicationDate());
            }
            if (application.getDeadline() != null) {
                deadlinePicker.setValue(application.getDeadline());
            }
            if (application.getExpectedSalary() != null) {
                salaryField.setText(application.getExpectedSalary().toPlainString());
            }
            if (application.getJobUrl() != null) {
                urlField.setText(application.getJobUrl());
            }
            if (application.getJobDescription() != null) {
                descArea.setText(application.getJobDescription());
            }
            if (application.getNotes() != null) {
                notesArea.setText(application.getNotes());
            }

            if (recruiterBox != null && application.getRecruiterId() != null) {
                for (com.applytrack.model.Recruiter r : recruiterBox.getItems()) {
                    if (r != null && r.getId() == application.getRecruiterId()) {
                        recruiterBox.setValue(r);
                        break;
                    }
                }
            }
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public Application getApplication() {
        return application;
    }

    @FXML
    private void handleSave(ActionEvent event) {
        hideError();

        String companyName = companyField.getText();
        String jobRole = roleField.getText();
        JobType jobType = jobTypeBox.getValue();
        ApplicationStatus status = statusBox.getValue();
        LocalDate appDate = appDatePicker.getValue();
        LocalDate deadline = deadlinePicker.getValue();
        String salaryText = salaryField.getText();
        String jobUrl = urlField.getText();
        String jobDescription = descArea.getText();
        String notes = notesArea.getText();

        BigDecimal salary = null;
        if (salaryText != null && !salaryText.trim().isEmpty()) {
            try {
                salary = new BigDecimal(salaryText.trim());
            } catch (NumberFormatException e) {
                showError("Expected salary must be a valid number (e.g. 120000 or 50000.50).");
                return;
            }
        }

        int userId = UserSession.getInstance().getUserId();
        if (userId <= 0) {
            showError("Authentication error: No active user session detected.");
            return;
        }

        Integer recruiterId = null;
        if (recruiterBox != null && recruiterBox.getValue() != null && recruiterBox.getValue().getId() > 0) {
            recruiterId = recruiterBox.getValue().getId();
        }

        try {
            if (application == null || application.getId() <= 0) {
                // Create
                application = applicationService.createApplication(
                        userId, companyName, jobRole, jobType, appDate, deadline,
                        salary, jobUrl, jobDescription, status, notes, recruiterId
                );
            } else {
                // Update
                application = applicationService.updateApplication(
                        application.getId(), userId, companyName, jobRole, jobType, appDate, deadline,
                        salary, jobUrl, jobDescription, status, notes, recruiterId
                );
            }
            saved = true;
            if (dialogStage != null) {
                dialogStage.close();
            }
        } catch (ValidationException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Database error while saving application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
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
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}

