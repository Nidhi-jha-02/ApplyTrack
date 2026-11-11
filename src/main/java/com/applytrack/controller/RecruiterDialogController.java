package com.applytrack.controller;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.Recruiter;
import com.applytrack.repository.CompanyRepository;
import com.applytrack.repository.impl.JdbcCompanyRepository;
import com.applytrack.service.ApplicationService;
import com.applytrack.service.RecruiterService;
import com.applytrack.session.UserSession;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the Add and Edit Recruiter modal dialog.
 *
 * Demonstrates:
 * - OOP: Encapsulation & MVC Layered Separation (Unit 3)
 * - JavaFX ComboBox with Custom CellFactory and StringConverter (Unit 3)
 * - Validation error feedback and user-friendly exception handling (Unit 3)
 * - Scoped Company loading based on candidate's application pipeline (Unit 3)
 */
public class RecruiterDialogController implements Initializable {

    @FXML private Label dialogTitleLabel;
    @FXML private Label errorLabel;

    @FXML private TextField nameField;
    @FXML private ComboBox<Company> companyBox;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField linkedinField;
    @FXML private TextArea notesArea;

    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    private Stage dialogStage;
    private Recruiter recruiter;
    private boolean saved = false;

    private RecruiterService recruiterService;
    private ApplicationService applicationService;
    private CompanyRepository companyRepository;

    public RecruiterDialogController() {
        this(new RecruiterService(), new ApplicationService(), new JdbcCompanyRepository());
    }

    public RecruiterDialogController(RecruiterService recruiterService, ApplicationService applicationService, CompanyRepository companyRepository) {
        this.recruiterService = recruiterService != null ? recruiterService : new RecruiterService();
        this.applicationService = applicationService != null ? applicationService : new ApplicationService();
        this.companyRepository = companyRepository != null ? companyRepository : new JdbcCompanyRepository();
    }

    public void setRecruiterService(RecruiterService recruiterService) {
        if (recruiterService != null) this.recruiterService = recruiterService;
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

    public Recruiter getRecruiter() {
        return recruiter;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureCompanyBox();
        loadAccessibleCompanies();
    }

    private void configureCompanyBox() {
        companyBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Company comp) {
                return (comp != null && comp.getCompanyName() != null) ? comp.getCompanyName() : "";
            }

            @Override
            public Company fromString(String string) {
                return null;
            }
        });

        companyBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Company item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getCompanyName());
            }
        });

        companyBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Company item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getCompanyName());
            }
        });
    }

    private void loadAccessibleCompanies() {
        int userId = UserSession.getInstance().getUserId();
        if (userId <= 0) return;

        try {
            // Find distinct companies where candidate has submitted applications
            List<Application> apps = applicationService.getApplicationsForUser(userId);
            Map<Integer, Company> companyMap = new LinkedHashMap<>();
            for (Application app : apps) {
                if (app.getCompany() != null && app.getCompany().getId() > 0) {
                    companyMap.put(app.getCompany().getId(), app.getCompany());
                }
            }

            List<Company> companies = new ArrayList<>(companyMap.values());
            companyBox.setItems(FXCollections.observableArrayList(companies));
            if (!companies.isEmpty()) {
                companyBox.setValue(companies.get(0));
            }
        } catch (SQLException e) {
            System.err.println("Error loading accessible companies: " + e.getMessage());
            showError("Could not load companies: " + e.getMessage());
        }
    }

    /**
     * Populates form fields for editing an existing recruiter.
     */
    public void setRecruiter(Recruiter recruiter) {
        this.recruiter = recruiter;
        if (recruiter == null) return;

        dialogTitleLabel.setText("Edit Recruiter — " + recruiter.getRecruiterName());
        saveBtn.setText("Update Recruiter");

        nameField.setText(recruiter.getRecruiterName());

        if (recruiter.getCompany() != null) {
            boolean found = false;
            for (Company comp : companyBox.getItems()) {
                if (comp.getId() == recruiter.getCompany().getId()) {
                    companyBox.setValue(comp);
                    found = true;
                    break;
                }
            }
            if (!found) {
                companyBox.getItems().add(recruiter.getCompany());
                companyBox.setValue(recruiter.getCompany());
            }
        }

        if (recruiter.getEmail() != null) emailField.setText(recruiter.getEmail());
        if (recruiter.getPhone() != null) phoneField.setText(recruiter.getPhone());
        if (recruiter.getLinkedinUrl() != null) linkedinField.setText(recruiter.getLinkedinUrl());
        if (recruiter.getNotes() != null) notesArea.setText(recruiter.getNotes());
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

        String name = nameField.getText() != null ? nameField.getText().trim() : "";
        if (name.isEmpty()) {
            showError("Recruiter name is required.");
            nameField.requestFocus();
            return;
        }

        Company selectedCompany = companyBox.getValue();
        if (selectedCompany == null) {
            showError("Please select a company in your application pipeline for this recruiter.");
            companyBox.requestFocus();
            return;
        }

        String email = emailField.getText() != null ? emailField.getText().trim() : null;
        String phone = phoneField.getText() != null ? phoneField.getText().trim() : null;
        String linkedin = linkedinField.getText() != null ? linkedinField.getText().trim() : null;
        String notes = notesArea.getText() != null ? notesArea.getText().trim() : null;

        int userId = UserSession.getInstance().getUserId();

        try {
            if (recruiter == null) {
                // Create new recruiter
                this.recruiter = recruiterService.createRecruiter(
                        userId,
                        name,
                        selectedCompany.getId(),
                        email,
                        phone,
                        linkedin,
                        notes
                );
            } else {
                // Update existing recruiter
                this.recruiter = recruiterService.updateRecruiter(
                        recruiter.getId(),
                        userId,
                        name,
                        selectedCompany.getId(),
                        email,
                        phone,
                        linkedin,
                        notes
                );
            }

            saved = true;
            if (dialogStage != null) {
                dialogStage.close();
            }

        } catch (ValidationException ve) {
            showError(ve.getMessage());
        } catch (SQLException sqle) {
            showError("Database error while saving recruiter: " + sqle.getMessage());
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
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}

