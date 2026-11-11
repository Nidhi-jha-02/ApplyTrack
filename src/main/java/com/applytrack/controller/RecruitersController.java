package com.applytrack.controller;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.Interview;
import com.applytrack.model.Recruiter;
import com.applytrack.repository.CompanyRepository;
import com.applytrack.repository.impl.JdbcCompanyRepository;
import com.applytrack.service.ApplicationService;
import com.applytrack.service.RecruiterService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Recruiter Management view (Module 4).
 *
 * Demonstrates:
 * - OOP: Encapsulation & MVC Layered Separation (Unit 3)
 * - JavaFX TableView with custom TableCell factories and property value mapping (Unit 3)
 * - Multithreading via javafx.concurrent.Task for background database queries (Unit 3)
 * - User-Specific Data Security: strictly uses UserSession.getUserId()
 * - Interactive Search, Company Filter, Dynamic Sorting, and Cross-Module Inspection (Unit 3)
 */
public class RecruitersController implements Initializable {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);

    @FXML private Label recordCountBadge;
    @FXML private Button addRecruiterBtn;
    @FXML private Button refreshBtn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> companyFilterBox;
    @FXML private ComboBox<String> sortByBox;
    @FXML private Button resetFiltersBtn;

    @FXML private Label statusMessageLabel;

    @FXML private TableView<Recruiter> recruitersTable;
    @FXML private TableColumn<Recruiter, String> nameCol;
    @FXML private TableColumn<Recruiter, String> companyCol;
    @FXML private TableColumn<Recruiter, String> emailCol;
    @FXML private TableColumn<Recruiter, String> phoneCol;
    @FXML private TableColumn<Recruiter, String> appsCol;
    @FXML private TableColumn<Recruiter, String> infoCol;

    @FXML private VBox emptyStateContainer;

    @FXML private Label selectedRecruiterLabel;
    @FXML private Button viewDetailsBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private final ObservableList<Recruiter> masterData = FXCollections.observableArrayList();
    private final Map<String, Integer> companyNameToIdMap = new HashMap<>();

    private RecruiterService recruiterService;
    private ApplicationService applicationService;
    private CompanyRepository companyRepository;

    public RecruitersController() {
        this(new RecruiterService(), new ApplicationService(), new JdbcCompanyRepository());
    }

    public RecruitersController(RecruiterService recruiterService, ApplicationService applicationService, CompanyRepository companyRepository) {
        this.recruiterService = recruiterService != null ? recruiterService : new RecruiterService();
        this.applicationService = applicationService != null ? applicationService : new ApplicationService();
        this.companyRepository = companyRepository != null ? companyRepository : new JdbcCompanyRepository();
    }

    public void setRecruiterService(RecruiterService recruiterService) {
        if (recruiterService != null) this.recruiterService = recruiterService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilterControls();
        setupSelectionListener();
        setupDoubleClickHandler();

        loadRecruitersAsync();
    }

    private void setupTableColumns() {
        // 1. Recruiter Name
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRecruiterName()));

        // 2. Company Name
        companyCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCompanyName()));

        // 3. Email
        emailCol.setCellValueFactory(cellData -> {
            String email = cellData.getValue().getEmail();
            return new SimpleStringProperty(email != null && !email.isBlank() ? email : "—");
        });

        // 4. Phone
        phoneCol.setCellValueFactory(cellData -> {
            String phone = cellData.getValue().getPhone();
            return new SimpleStringProperty(phone != null && !phone.isBlank() ? phone : "—");
        });

        // 5. Associated Applications Badge
        appsCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedApplicationsCount()));
        appsCol.setCellFactory(col -> new TableCell<>() {
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
                    Recruiter r = getTableRow().getItem();
                    int count = (r != null) ? r.getAssociatedApplicationsCount() : 0;
                    if (count > 0) {
                        badge.setStyle(
                                "-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; " +
                                "-fx-font-weight: bold; -fx-font-size: 11px; " +
                                "-fx-padding: 3 8 3 8; -fx-background-radius: 12px; " +
                                "-fx-border-color: #c7d2fe; -fx-border-radius: 12px;"
                        );
                    } else {
                        badge.setStyle(
                                "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; " +
                                "-fx-font-weight: 500; -fx-font-size: 11px; " +
                                "-fx-padding: 3 8 3 8; -fx-background-radius: 12px;"
                        );
                    }
                    setGraphic(badge);
                }
            }
        });

        // 6. LinkedIn / Notes
        infoCol.setCellValueFactory(cellData -> {
            Recruiter r = cellData.getValue();
            if (r.getLinkedinUrl() != null && !r.getLinkedinUrl().isBlank()) {
                return new SimpleStringProperty(r.getLinkedinUrl());
            } else if (r.getNotes() != null && !r.getNotes().isBlank()) {
                return new SimpleStringProperty(r.getNotes());
            }
            return new SimpleStringProperty("—");
        });

        recruitersTable.setItems(masterData);
    }

    private void setupFilterControls() {
        populateCompanyFilter();

        // Sort Options
        sortByBox.getItems().addAll(
                "Recruiter Name (A-Z)",
                "Recruiter Name (Z-A)",
                "Company Name (A-Z)",
                "Email Address (A-Z)",
                "Associated Applications (Most First)"
        );
        sortByBox.setValue("Recruiter Name (A-Z)");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadRecruitersAsync());
        companyFilterBox.valueProperty().addListener((obs, oldVal, newVal) -> loadRecruitersAsync());
        sortByBox.valueProperty().addListener((obs, oldVal, newVal) -> loadRecruitersAsync());
    }

    private void populateCompanyFilter() {
        companyFilterBox.getItems().clear();
        companyNameToIdMap.clear();
        companyFilterBox.getItems().add("All Companies");

        int userId = UserSession.getInstance().getUserId();
        if (userId <= 0) return;

        try {
            List<Application> apps = applicationService.getApplicationsForUser(userId);
            Map<Integer, String> uniqueCompanies = new LinkedHashMap<>();
            for (Application app : apps) {
                if (app.getCompany() != null && app.getCompany().getId() > 0) {
                    uniqueCompanies.put(app.getCompany().getId(), app.getCompany().getCompanyName());
                }
            }

            for (Map.Entry<Integer, String> entry : uniqueCompanies.entrySet()) {
                companyFilterBox.getItems().add(entry.getValue());
                companyNameToIdMap.put(entry.getValue(), entry.getKey());
            }
        } catch (SQLException e) {
            System.err.println("Error populating company filter: " + e.getMessage());
        }

        companyFilterBox.setValue("All Companies");
    }

    private void setupSelectionListener() {
        recruitersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, selected) -> {
            boolean hasSelection = selected != null;
            viewDetailsBtn.setDisable(!hasSelection);
            editBtn.setDisable(!hasSelection);
            deleteBtn.setDisable(!hasSelection);

            if (hasSelection) {
                selectedRecruiterLabel.setText(
                        "#" + selected.getId() + " • " + selected.getRecruiterName() +
                        " (" + selected.getCompanyName() + ") — " + selected.getFormattedApplicationsCount()
                );
            } else {
                selectedRecruiterLabel.setText("None selected (click a row above)");
            }
        });
    }

    private void setupDoubleClickHandler() {
        recruitersTable.setRowFactory(tv -> {
            TableRow<Recruiter> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    handleViewDetails(null);
                }
            });
            return row;
        });
    }

    /**
     * Loads recruiters from MariaDB asynchronously via JavaFX Task.
     */
    public void loadRecruitersAsync() {
        UserSession session = UserSession.getInstance();
        final int userId = session.getUserId();

        if (userId <= 0) {
            statusMessageLabel.setText("Not authenticated. Please log in.");
            masterData.clear();
            updateEmptyState();
            return;
        }

        final String keyword = searchField.getText() != null ? searchField.getText().trim() : null;
        final String selectedCompany = companyFilterBox.getValue();
        final Integer companyId = (selectedCompany != null && companyNameToIdMap.containsKey(selectedCompany))
                ? companyNameToIdMap.get(selectedCompany)
                : null;

        final String sortVal = sortByBox.getValue();
        final String sortBy;
        final boolean ascending;

        if ("Recruiter Name (Z-A)".equals(sortVal)) {
            sortBy = "name";
            ascending = false;
        } else if ("Company Name (A-Z)".equals(sortVal)) {
            sortBy = "company";
            ascending = true;
        } else if ("Email Address (A-Z)".equals(sortVal)) {
            sortBy = "email";
            ascending = true;
        } else if ("Associated Applications (Most First)".equals(sortVal)) {
            sortBy = "apps";
            ascending = false;
        } else {
            sortBy = "name";
            ascending = true;
        }

        statusMessageLabel.setText("Loading recruiter directory from MariaDB...");

        Task<List<Recruiter>> task = new Task<>() {
            @Override
            protected List<Recruiter> call() {
                return recruiterService.getFilteredRecruiters(
                        userId,
                        keyword,
                        companyId,
                        sortBy,
                        ascending
                );
            }
        };

        task.setOnSucceeded(event -> {
            List<Recruiter> results = task.getValue();
            masterData.setAll(results);
            updateEmptyState();

            int total = recruiterService.getRecruiterCount(userId);
            if (recordCountBadge != null) {
                recordCountBadge.setText(total + (total == 1 ? " Recruiter" : " Recruiters"));
            }

            statusMessageLabel.setText(
                    "Showing " + results.size() + " of " + total + " recruiters | Last refreshed: " +
                    LocalTime.now().format(TIME_FORMATTER)
            );
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            System.err.println("Error loading recruiters: " + ex.getMessage());
            statusMessageLabel.setText("Error querying recruiters: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    private void updateEmptyState() {
        boolean isEmpty = masterData.isEmpty();
        emptyStateContainer.setVisible(isEmpty);
        recruitersTable.setVisible(!isEmpty);
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        populateCompanyFilter();
        loadRecruitersAsync();
    }

    @FXML
    private void handleResetFilters(ActionEvent event) {
        searchField.clear();
        companyFilterBox.setValue("All Companies");
        sortByBox.setValue("Recruiter Name (A-Z)");
        loadRecruitersAsync();
    }

    @FXML
    public void handleAddRecruiter(ActionEvent event) {
        openRecruiterDialog(null);
    }

    @FXML
    private void handleEditRecruiter(ActionEvent event) {
        Recruiter selected = recruitersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a recruiter to edit.");
            return;
        }
        openRecruiterDialog(selected);
    }

    @FXML
    private void handleDeleteRecruiter(ActionEvent event) {
        Recruiter selected = recruitersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a recruiter to delete.");
            return;
        }

        int userId = UserSession.getInstance().getUserId();
        int linkedAppsCount = recruiterService.getAssociatedApplicationsCount(selected.getId(), userId);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Recruiter Deletion");
        confirm.setHeaderText("Delete recruiter: " + selected.getRecruiterName() + "?");

        if (linkedAppsCount > 0) {
            confirm.setContentText(
                    "⚠️ Notice: This recruiter is currently associated with " + linkedAppsCount +
                    " of your job application(s).\n\n" +
                    "Per the database foreign-key configuration (SET NULL), deleting this recruiter will unlink them " +
                    "from those applications, but your application records, interviews, and deadlines will remain completely safe.\n\n" +
                    "Are you sure you want to permanently delete " + selected.getRecruiterName() + "?"
            );
        } else {
            confirm.setContentText(
                    "Are you sure you want to delete " + selected.getRecruiterName() + " (" + selected.getCompanyName() + ")?\n\n" +
                    "This action cannot be undone."
            );
        }

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = recruiterService.deleteRecruiter(selected.getId(), userId);
                if (deleted) {
                    loadRecruitersAsync();
                    showAlert(Alert.AlertType.INFORMATION, "Deleted", "Recruiter " + selected.getRecruiterName() + " was deleted successfully.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Could not delete recruiter record.");
                }
            } catch (ValidationException | SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Deletion Error", e.getMessage());
            }
        }
    }

    @FXML
    private void handleViewDetails(ActionEvent event) {
        Recruiter selected = recruitersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        int userId = UserSession.getInstance().getUserId();
        List<Application> linkedApps = recruiterService.getAssociatedApplications(selected.getId(), userId);
        List<Interview> linkedInterviews = recruiterService.getAssociatedInterviews(selected.getId(), userId);

        Alert detailsAlert = new Alert(Alert.AlertType.INFORMATION);
        detailsAlert.setTitle("Recruiter Profile — " + selected.getRecruiterName());
        detailsAlert.setHeaderText(selected.getRecruiterName() + " @ " + selected.getCompanyName());

        StringBuilder sb = new StringBuilder();
        sb.append("• Recruiter ID: #").append(selected.getId()).append("\n");
        sb.append("• Company: ").append(selected.getCompanyName()).append("\n");
        sb.append("• Email: ").append(selected.getEmail() != null ? selected.getEmail() : "None provided").append("\n");
        sb.append("• Phone: ").append(selected.getPhone() != null ? selected.getPhone() : "None provided").append("\n");
        if (selected.getLinkedinUrl() != null && !selected.getLinkedinUrl().isBlank()) {
            sb.append("• LinkedIn Profile: ").append(selected.getLinkedinUrl()).append("\n");
        }
        if (selected.getNotes() != null && !selected.getNotes().isBlank()) {
            sb.append("\n[Recruiter Notes & History]\n").append(selected.getNotes()).append("\n");
        }

        // Section: Associated Job Applications
        sb.append("\n========================================\n");
        sb.append("ASSOCIATED APPLICATIONS (").append(linkedApps.size()).append("):\n");
        if (linkedApps.isEmpty()) {
            sb.append("  (No active job applications currently linked to this recruiter)\n");
        } else {
            for (Application app : linkedApps) {
                sb.append("  • #").append(app.getId()).append(": ").append(app.getJobRole())
                  .append(" @ ").append(app.getCompanyName())
                  .append(" [Status: ").append(app.getStatus().getDisplayName()).append("]\n");
            }
        }

        // Section: Associated Interviews
        sb.append("\nASSOCIATED INTERVIEW ROUNDS (").append(linkedInterviews.size()).append("):\n");
        if (linkedInterviews.isEmpty()) {
            sb.append("  (No interview rounds associated with this recruiter's applications)\n");
        } else {
            for (Interview i : linkedInterviews) {
                sb.append("  • ").append(i.getRoundName()).append(" (")
                  .append(i.getFormattedDateTime()).append(") [")
                  .append(i.getOutcome().getDisplayName()).append("]\n");
            }
        }

        detailsAlert.setContentText(sb.toString());
        detailsAlert.showAndWait();
    }

    private void openRecruiterDialog(Recruiter recruiterToEdit) {
        try {
            URL fxmlUrl = getClass().getResource("/com/applytrack/view/RecruiterDialog.fxml");
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "UI Error", "RecruiterDialog.fxml not found on classpath.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            RecruiterDialogController controller = loader.getController();
            controller.setRecruiterService(recruiterService);
            controller.setApplicationService(applicationService);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle(recruiterToEdit == null ? "Add New Recruiter" : "Edit Recruiter");
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            if (recruiterToEdit != null) {
                controller.setRecruiter(recruiterToEdit);
            }

            dialogStage.showAndWait();

            if (controller.isSaved()) {
                populateCompanyFilter();
                loadRecruitersAsync();
                statusMessageLabel.setText(recruiterToEdit == null
                        ? "Successfully added recruiter " + controller.getRecruiter().getRecruiterName()
                        : "Updated recruiter #" + controller.getRecruiter().getId());
            }

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Dialog Error", "Could not load recruiter dialog: " + e.getMessage());
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

