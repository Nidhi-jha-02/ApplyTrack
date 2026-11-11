package com.applytrack.controller;

import com.applytrack.model.User;
import com.applytrack.session.UserSession;
import com.applytrack.util.SceneNavigator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the Main Application Layout (Sidebar + Top Bar + Dynamic Content Area).
 *
 * Demonstrates:
 * - OOP: Encapsulation & Separation of Concerns (Unit 3)
 * - User Session Integration: Displays authenticated applicant identity (Unit 3)
 * - Interfaces: Initializable interface implementation (Unit 3)
 * - Collections: HashMap for view caching and button tracking (Unit 3)
 * - Exception Handling: try-catch with graceful fallback (Unit 3)
 * - Enhanced switch & Polymorphism (Unit 1 & Unit 3)
 */
public class MainLayoutController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label userProfileLabel;
    @FXML private Label userEmailLabel;
    @FXML private Button logoutButton;

    @FXML private Button navDashboardBtn;
    @FXML private Button navApplicationsBtn;
    @FXML private Button navInterviewsBtn;
    @FXML private Button navRecruitersBtn;
    @FXML private Button navAnalyticsBtn;
    @FXML private Button navReportsBtn;
    @FXML private Button navSettingsBtn;

    // View cache and controller cache using HashMap (Unit 3 Collections)
    private static MainLayoutController instance;
    private final Map<String, Node> viewCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private Button activeNavButton;

    public static MainLayoutController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        // Enforce session check and update profile UI
        UserSession session = UserSession.getInstance();
        if (session.isAuthenticated()) {
            if (userProfileLabel != null) userProfileLabel.setText(session.getUserFullName());
            if (userEmailLabel != null) userEmailLabel.setText(session.getUserEmail());
            if (statusLabel != null) statusLabel.setText("Active Session: " + session.getUserFullName());
        } else {
            // If someone opens MainLayout directly without authentication, show guest indicator
            if (userProfileLabel != null) userProfileLabel.setText("Guest User");
            if (userEmailLabel != null) userEmailLabel.setText("Unauthenticated");
            if (statusLabel != null) statusLabel.setText("System: Guest Mode");
        }

        // Default active view based on user preferences (or Dashboard fallback)
        String landing = "dashboard";
        if (session.isAuthenticated()) {
            try {
                com.applytrack.service.SettingsService ss = new com.applytrack.service.SettingsService();
                com.applytrack.model.UserPreferences up = ss.getPreferences(session.getUserId());
                if (up != null && up.getDefaultLandingPage() != null && !up.getDefaultLandingPage().isBlank()) {
                    landing = up.getDefaultLandingPage().trim().toLowerCase();
                }
            } catch (Exception ex) {
                // Fall back to dashboard
            }
        }

        switch (landing) {
            case "applications" -> {
                setActiveNav(navApplicationsBtn, "Job Applications", "Track and manage all your submitted applications");
                loadView("Applications", "/com/applytrack/view/Applications.fxml");
            }
            case "interviews" -> {
                setActiveNav(navInterviewsBtn, "Interview Pipeline", "Multi-round scheduling, preparation, and outcome tracking");
                loadView("Interviews", "/com/applytrack/view/Interviews.fxml");
            }
            case "recruiters" -> {
                setActiveNav(navRecruitersBtn, "Recruiter Directory", "Recruiter contacts, company connections, and communication notes");
                loadView("Recruiters", "/com/applytrack/view/Recruiters.fxml");
            }
            case "analytics" -> {
                setActiveNav(navAnalyticsBtn, "Career Analytics", "Application funnel, conversion rates, and monthly trends");
                loadView("Analytics", "/com/applytrack/view/Analytics.fxml");
            }
            case "reports" -> {
                setActiveNav(navReportsBtn, "Reports & Data Export", "Generate executive summaries and export to PDF/Excel");
                loadView("Reports", "/com/applytrack/view/Reports.fxml");
            }
            case "settings" -> {
                setActiveNav(navSettingsBtn, "Settings & Preferences", "User Profile, Security Credentials & Visual Customization");
                loadView("Settings", "/com/applytrack/view/Settings.fxml");
            }
            default -> {
                setActiveNav(navDashboardBtn, "Dashboard", "Overview & Career Insights");
                loadView("Dashboard", "/com/applytrack/view/Dashboard.fxml");
            }
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Clear authenticated session state
        UserSession.getInstance().clearSession();

        // Redirect back to Login view
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SceneNavigator.showLoginScreen(stage, "You have been logged out successfully.", null);
    }

    @FXML
    private void handleNavigation(ActionEvent event) {
        if (!(event.getSource() instanceof Button clickedBtn)) {
            return;
        }

        String target = (String) clickedBtn.getUserData();
        if (target == null) {
            target = clickedBtn.getText().trim();
        }

        switch (target.toLowerCase()) {
            case "dashboard" -> {
                setActiveNav(clickedBtn, "Dashboard", "Overview & Career Insights");
                loadView("Dashboard", "/com/applytrack/view/Dashboard.fxml");
            }
            case "applications" -> {
                setActiveNav(clickedBtn, "Job Applications", "Track and manage all your submitted applications");
                loadView("Applications", "/com/applytrack/view/Applications.fxml");
            }
            case "interviews" -> {
                setActiveNav(clickedBtn, "Interview Pipeline", "Multi-round scheduling, preparation, and outcome tracking");
                loadView("Interviews", "/com/applytrack/view/Interviews.fxml");
            }
            case "recruiters" -> {
                setActiveNav(clickedBtn, "Recruiter Directory", "Recruiter contacts, company connections, and communication notes");
                loadView("Recruiters", "/com/applytrack/view/Recruiters.fxml");
            }
            case "analytics" -> {
                setActiveNav(clickedBtn, "Career Analytics", "Application funnel, conversion rates, and monthly trends");
                loadView("Analytics", "/com/applytrack/view/Analytics.fxml");
            }
            case "reports" -> {
                setActiveNav(clickedBtn, "Reports & Data Export", "Generate executive summaries and export to PDF/Excel");
                loadView("Reports", "/com/applytrack/view/Reports.fxml");
            }
            case "settings" -> {
                setActiveNav(clickedBtn, "Settings & Preferences", "User Profile, Security Credentials & Visual Customization");
                loadView("Settings", "/com/applytrack/view/Settings.fxml");
            }
            default -> System.out.println("Unhandled navigation target: " + target);
        }
    }

    private void setActiveNav(Button btn, String title, String subtitle) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-button-active");
        }
        activeNavButton = btn;
        if (activeNavButton != null && !activeNavButton.getStyleClass().contains("nav-button-active")) {
            activeNavButton.getStyleClass().add("nav-button-active");
        }
        if (pageTitleLabel != null) pageTitleLabel.setText(title);
        if (pageSubtitleLabel != null) pageSubtitleLabel.setText(subtitle);
    }

    private void loadView(String viewKey, String fxmlPath) {
        try {
            if (viewCache.containsKey(viewKey)) {
                contentArea.getChildren().setAll(viewCache.get(viewKey));
                Object cachedController = controllerCache.get(viewKey);
                if (cachedController instanceof DashboardController dc) {
                    dc.refreshDashboardData();
                } else if (cachedController instanceof ApplicationsController ac) {
                    ac.loadApplicationsAsync();
                } else if (cachedController instanceof InterviewsController ic) {
                    ic.loadInterviewsAsync();
                } else if (cachedController instanceof RecruitersController rc) {
                    rc.loadRecruitersAsync();
                } else if (cachedController instanceof AnalyticsController anc) {
                    anc.loadAnalyticsAsync(com.applytrack.model.enums.DateRangeFilter.ALL_TIME);
                } else if (cachedController instanceof ReportsController repc) {
                    repc.refreshPreviewAsync();
                } else if (cachedController instanceof SettingsController sc) {
                    sc.loadSettingsAsync();
                }
                return;
            }

            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                loadPlaceholderView(viewKey, "FXML file not found: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Node node = loader.load();
            Object controller = loader.getController();

            viewCache.put(viewKey, node);
            if (controller != null) {
                controllerCache.put(viewKey, controller);
            }

            contentArea.getChildren().setAll(node);

        } catch (IOException e) {
            System.err.println("Error loading view " + viewKey + ": " + e.getMessage());
            e.printStackTrace();
            loadPlaceholderView(viewKey, "Error loading view: " + e.getMessage());
        }
    }

    private void loadPlaceholderView(String moduleName, String message) {
        StackPane placeholderPane = new StackPane();
        placeholderPane.setStyle("-fx-background-color: #f8fafc; -fx-padding: 40px; -fx-alignment: center;");

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(15);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);
        vbox.setStyle("-fx-background-color: white; -fx-padding: 40px; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 12, 0, 0, 4); -fx-max-width: 650px;");

        Label title = new Label(moduleName);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label desc = new Label(message);
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-text-alignment: center;");

        Label badge = new Label("STAGED FOR IMPLEMENTATION");
        badge.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-font-weight: bold; -fx-padding: 5 12 5 12; -fx-background-radius: 20px; -fx-font-size: 11px;");

        vbox.getChildren().addAll(badge, title, desc);
        placeholderPane.getChildren().add(vbox);

        contentArea.getChildren().setAll(placeholderPane);
    }

    public void navigateToInterviews() {
        if (navInterviewsBtn != null) {
            setActiveNav(navInterviewsBtn, "Interview Pipeline", "Multi-round scheduling, preparation, and outcome tracking");
        }
        loadView("Interviews", "/com/applytrack/view/Interviews.fxml");
    }

    public void navigateToRecruiters() {
        if (navRecruitersBtn != null) {
            setActiveNav(navRecruitersBtn, "Recruiter Directory", "Recruiter contacts, company connections, and communication notes");
        }
        loadView("Recruiters", "/com/applytrack/view/Recruiters.fxml");
    }

    public void navigateToAnalytics() {
        if (navAnalyticsBtn != null) {
            setActiveNav(navAnalyticsBtn, "Career Analytics", "Application funnel, conversion rates, and monthly trends");
        }
        loadView("Analytics", "/com/applytrack/view/Analytics.fxml");
    }

    public void navigateToApplications() {
        if (navApplicationsBtn != null) {
            setActiveNav(navApplicationsBtn, "Job Applications", "Track and manage all your submitted applications");
        }
        loadView("Applications", "/com/applytrack/view/Applications.fxml");
    }

    public void navigateToReports() {
        if (navReportsBtn != null) {
            setActiveNav(navReportsBtn, "Reports & Data Export", "Generate executive summaries and export to PDF/Excel");
        }
        loadView("Reports", "/com/applytrack/view/Reports.fxml");
    }

    public void updateUserProfileUI(User user) {
        if (user != null) {
            if (userProfileLabel != null) userProfileLabel.setText(user.getFullName());
            if (userEmailLabel != null) userEmailLabel.setText(user.getEmail());
            if (statusLabel != null) statusLabel.setText("Active Session: " + user.getFullName());
        }
    }

    public void navigateToSettings() {
        if (navSettingsBtn != null) {
            setActiveNav(navSettingsBtn, "Settings & Preferences", "User Profile, Security Credentials & Visual Customization");
        }
        loadView("Settings", "/com/applytrack/view/Settings.fxml");
    }
}
