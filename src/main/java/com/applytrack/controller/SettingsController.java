package com.applytrack.controller;

import com.applytrack.exception.AuthenticationException;
import com.applytrack.exception.ValidationException;
import com.applytrack.model.User;
import com.applytrack.model.UserPreferences;
import com.applytrack.model.enums.AppTheme;
import com.applytrack.model.enums.DateRangeFilter;
import com.applytrack.service.SettingsService;
import com.applytrack.session.UserSession;
import com.applytrack.util.SceneNavigator;
import com.applytrack.util.ThemeManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for the Settings & User Profile Management module.
 *
 * Demonstrates:
 * - Form Validation & Domain Security (Unit 3)
 * - Safe Event Handling and Data Binding (Unit 3)
 * - Dynamic Theme Switching via ThemeManager (Unit 3)
 * - Asynchronous Background Task execution (Unit 3)
 * - Session Synchronization & Defensive Access Control (Unit 3)
 */
public class SettingsController implements Initializable {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a", Locale.US);

    @FXML private Label headerUserBadge;
    @FXML private HBox feedbackBanner;
    @FXML private Label feedbackIcon;
    @FXML private Label feedbackTitle;
    @FXML private Label feedbackDetails;
    @FXML private TabPane settingsTabPane;

    // Tab 1: Profile
    @FXML private TextField profileFullNameField;
    @FXML private TextField profileEmailField;
    @FXML private Label profileUserIdLabel;
    @FXML private Label profileCreatedAtLabel;
    @FXML private Label profileUpdatedAtLabel;

    // Tab 2: Security
    @FXML private PasswordField currentPasswordField;
    @FXML private TextField currentPasswordVisibleField;
    @FXML private Button toggleCurrentPasswordBtn;

    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordVisibleField;
    @FXML private Button toggleNewPasswordBtn;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisibleField;
    @FXML private Button toggleConfirmPasswordBtn;

    // Tab 3: Preferences
    @FXML private ComboBox<AppTheme> themeComboBox;
    @FXML private ComboBox<String> landingPageComboBox;
    @FXML private ComboBox<DateRangeFilter> analyticsScopeComboBox;
    @FXML private CheckBox reminderInterviewsCheck;
    @FXML private CheckBox reminderDeadlinesCheck;

    // Tab 4: Account Information
    @FXML private Label infoAccountNameLabel;
    @FXML private Label infoAccountEmailLabel;
    @FXML private Label infoAccountIdLabel;

    private final SettingsService settingsService;
    private User cachedUser;
    private UserPreferences cachedPreferences;

    private boolean currentPasswordVisible = false;
    private boolean newPasswordVisible = false;
    private boolean confirmPasswordVisible = false;

    public SettingsController() {
        this(new SettingsService());
    }

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService != null ? settingsService : new SettingsService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (feedbackBanner != null) {
            feedbackBanner.managedProperty().bind(feedbackBanner.visibleProperty());
            feedbackBanner.setVisible(false);
        }

        setupPasswordToggles();
        setupPreferenceSelectors();

        loadSettingsAsync();
    }

    private void setupPasswordToggles() {
        if (currentPasswordField != null && currentPasswordVisibleField != null) {
            currentPasswordField.textProperty().bindBidirectional(currentPasswordVisibleField.textProperty());
        }
        if (newPasswordField != null && newPasswordVisibleField != null) {
            newPasswordField.textProperty().bindBidirectional(newPasswordVisibleField.textProperty());
        }
        if (confirmPasswordField != null && confirmPasswordVisibleField != null) {
            confirmPasswordField.textProperty().bindBidirectional(confirmPasswordVisibleField.textProperty());
        }
    }

    private void setupPreferenceSelectors() {
        // Theme ComboBox
        if (themeComboBox != null) {
            themeComboBox.getItems().setAll(AppTheme.values());
            themeComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(AppTheme theme) {
                    return theme != null ? theme.getDisplayName() : "";
                }

                @Override
                public AppTheme fromString(String string) {
                    return AppTheme.fromString(string);
                }
            });
            themeComboBox.setValue(AppTheme.LIGHT);
        }

        // Landing Page ComboBox
        if (landingPageComboBox != null) {
            landingPageComboBox.getItems().setAll(
                    "Dashboard",
                    "Job Applications",
                    "Interview Pipeline",
                    "Career Analytics",
                    "Reports & Export"
            );
            landingPageComboBox.setValue("Dashboard");
        }

        // Analytics Scope ComboBox
        if (analyticsScopeComboBox != null) {
            analyticsScopeComboBox.getItems().setAll(DateRangeFilter.values());
            analyticsScopeComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(DateRangeFilter filter) {
                    return filter != null ? filter.getDisplayName() : "";
                }

                @Override
                public DateRangeFilter fromString(String string) {
                    return DateRangeFilter.fromString(string);
                }
            });
            analyticsScopeComboBox.setValue(DateRangeFilter.ALL_TIME);
        }
    }

    /**
     * Asynchronously loads the user's profile and preferences from MariaDB.
     */
    public void loadSettingsAsync() {
        UserSession session = UserSession.getInstance();
        if (!session.isAuthenticated()) {
            return;
        }

        final int userId = session.getUserId();

        Task<SettingsDataBundle> loadTask = new Task<>() {
            @Override
            protected SettingsDataBundle call() throws Exception {
                User user = settingsService.getUserProfile(userId);
                UserPreferences prefs = settingsService.getPreferences(userId);
                return new SettingsDataBundle(user, prefs);
            }
        };

        loadTask.setOnSucceeded(e -> {
            SettingsDataBundle bundle = loadTask.getValue();
            this.cachedUser = bundle.user;
            this.cachedPreferences = bundle.preferences;
            populateUi(bundle.user, bundle.preferences);
        });

        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            showErrorBanner("Failed to load settings", ex != null ? ex.getMessage() : "Unknown database error.");
        });

        new Thread(loadTask, "ApplyTrack-SettingsLoadThread").start();
    }

    private void populateUi(User user, UserPreferences prefs) {
        if (user != null) {
            if (headerUserBadge != null) headerUserBadge.setText(user.getFullName() + " (#" + user.getId() + ")");
            if (profileFullNameField != null) profileFullNameField.setText(user.getFullName());
            if (profileEmailField != null) profileEmailField.setText(user.getEmail());
            if (profileUserIdLabel != null) profileUserIdLabel.setText(String.valueOf(user.getId()));

            if (profileCreatedAtLabel != null) {
                profileCreatedAtLabel.setText(user.getCreatedAt() != null
                        ? user.getCreatedAt().format(DATE_TIME_FORMATTER)
                        : "—");
            }
            if (profileUpdatedAtLabel != null) {
                profileUpdatedAtLabel.setText(user.getUpdatedAt() != null
                        ? user.getUpdatedAt().format(DATE_TIME_FORMATTER)
                        : "—");
            }

            if (infoAccountNameLabel != null) infoAccountNameLabel.setText(user.getFullName());
            if (infoAccountEmailLabel != null) infoAccountEmailLabel.setText(user.getEmail());
            if (infoAccountIdLabel != null) infoAccountIdLabel.setText("UID-" + user.getId());
        }

        if (prefs != null) {
            if (themeComboBox != null) themeComboBox.setValue(prefs.getTheme());
            if (landingPageComboBox != null) {
                String landing = mapLandingPageToDisplay(prefs.getDefaultLandingPage());
                landingPageComboBox.setValue(landing);
            }
            if (analyticsScopeComboBox != null) analyticsScopeComboBox.setValue(prefs.getPreferredAnalyticsRange());
            if (reminderInterviewsCheck != null) reminderInterviewsCheck.setSelected(prefs.isReminderUpcomingInterviews());
            if (reminderDeadlinesCheck != null) reminderDeadlinesCheck.setSelected(prefs.isReminderApplicationDeadlines());
        }
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        UserSession session = UserSession.getInstance();
        if (!session.isAuthenticated()) {
            showErrorBanner("Session Expired", "Please sign in again to update your profile.");
            return;
        }

        String name = profileFullNameField != null ? profileFullNameField.getText() : "";
        String email = profileEmailField != null ? profileEmailField.getText() : "";

        try {
            User updated = settingsService.updateProfile(session.getUserId(), name, email);
            this.cachedUser = updated;
            populateUi(updated, cachedPreferences);

            // Update main layout header
            MainLayoutController ml = MainLayoutController.getInstance();
            if (ml != null) {
                ml.updateUserProfileUI(updated);
            }

            showSuccessBanner("Profile Updated Successfully", "Your changes have been saved to MariaDB.");
        } catch (ValidationException e) {
            showErrorBanner("Profile Validation Error", e.getMessage());
        } catch (SQLException e) {
            showErrorBanner("Database Error", "Failed to update profile: " + e.getMessage());
        }
    }

    @FXML
    private void handleResetProfile(ActionEvent event) {
        if (cachedUser != null) {
            profileFullNameField.setText(cachedUser.getFullName());
            profileEmailField.setText(cachedUser.getEmail());
            hideFeedbackBanner();
        }
    }

    @FXML
    private void handleToggleCurrentPassword(ActionEvent event) {
        currentPasswordVisible = !currentPasswordVisible;
        toggleField(currentPasswordField, currentPasswordVisibleField, toggleCurrentPasswordBtn, currentPasswordVisible);
    }

    @FXML
    private void handleToggleNewPassword(ActionEvent event) {
        newPasswordVisible = !newPasswordVisible;
        toggleField(newPasswordField, newPasswordVisibleField, toggleNewPasswordBtn, newPasswordVisible);
    }

    @FXML
    private void handleToggleConfirmPassword(ActionEvent event) {
        confirmPasswordVisible = !confirmPasswordVisible;
        toggleField(confirmPasswordField, confirmPasswordVisibleField, toggleConfirmPasswordBtn, confirmPasswordVisible);
    }

    private void toggleField(PasswordField pf, TextField tf, Button btn, boolean visible) {
        if (pf == null || tf == null || btn == null) return;
        pf.setVisible(!visible);
        pf.setManaged(!visible);
        tf.setVisible(visible);
        tf.setManaged(visible);
        btn.setText(visible ? "Hide" : "Show");
    }

    @FXML
    private void handleUpdatePassword(ActionEvent event) {
        UserSession session = UserSession.getInstance();
        if (!session.isAuthenticated()) {
            showErrorBanner("Session Expired", "Please sign in again to update your password.");
            return;
        }

        String currentPwd = currentPasswordField != null ? currentPasswordField.getText() : "";
        String newPwd = newPasswordField != null ? newPasswordField.getText() : "";
        String confirmPwd = confirmPasswordField != null ? confirmPasswordField.getText() : "";

        try {
            settingsService.changePassword(session.getUserId(), currentPwd, newPwd, confirmPwd);
            handleClearPasswordFields(null);
            showSuccessBanner("Password Changed Successfully",
                    "Your new password is now active and protected with BCrypt (10 salt rounds).");
        } catch (ValidationException | AuthenticationException e) {
            showErrorBanner("Password Change Failed", e.getMessage());
        } catch (SQLException e) {
            showErrorBanner("Database Error", "Failed to update password: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearPasswordFields(ActionEvent event) {
        if (currentPasswordField != null) currentPasswordField.clear();
        if (newPasswordField != null) newPasswordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
    }

    @FXML
    private void handleSavePreferences(ActionEvent event) {
        UserSession session = UserSession.getInstance();
        if (!session.isAuthenticated()) {
            showErrorBanner("Session Expired", "Please sign in again to customize preferences.");
            return;
        }

        AppTheme theme = themeComboBox != null && themeComboBox.getValue() != null
                ? themeComboBox.getValue()
                : AppTheme.LIGHT;

        String landing = landingPageComboBox != null && landingPageComboBox.getValue() != null
                ? mapLandingDisplayToCode(landingPageComboBox.getValue())
                : "DASHBOARD";

        DateRangeFilter range = analyticsScopeComboBox != null && analyticsScopeComboBox.getValue() != null
                ? analyticsScopeComboBox.getValue()
                : DateRangeFilter.ALL_TIME;

        boolean reminderInterviews = reminderInterviewsCheck != null && reminderInterviewsCheck.isSelected();
        boolean reminderDeadlines = reminderDeadlinesCheck != null && reminderDeadlinesCheck.isSelected();

        UserPreferences prefs = new UserPreferences();
        prefs.setUserId(session.getUserId());
        prefs.setTheme(theme);
        prefs.setDefaultLandingPage(landing);
        prefs.setPreferredAnalyticsRange(range);
        prefs.setReminderUpcomingInterviews(reminderInterviews);
        prefs.setReminderApplicationDeadlines(reminderDeadlines);

        try {
            UserPreferences saved = settingsService.savePreferences(prefs);
            this.cachedPreferences = saved;

            // Apply theme dynamically to active scene
            Scene currentScene = ((Node) event.getSource()).getScene();
            if (currentScene != null) {
                ThemeManager.applyTheme(currentScene, theme);
            }

            showSuccessBanner("Preferences Saved",
                    "Application theme (" + theme.getDisplayName() + ") and preferences successfully updated in MariaDB.");
        } catch (ValidationException | SQLException e) {
            showErrorBanner("Preferences Error", e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        settingsService.logout();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SceneNavigator.showLoginScreen(stage, "You have been logged out securely.", null);
    }

    @FXML
    private void handleDismissFeedback(ActionEvent event) {
        hideFeedbackBanner();
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

    private String mapLandingPageToDisplay(String code) {
        if (code == null) return "Dashboard";
        return switch (code.toUpperCase().trim()) {
            case "APPLICATIONS" -> "Job Applications";
            case "INTERVIEWS" -> "Interview Pipeline";
            case "ANALYTICS" -> "Career Analytics";
            case "REPORTS" -> "Reports & Export";
            default -> "Dashboard";
        };
    }

    private String mapLandingDisplayToCode(String display) {
        if (display == null) return "DASHBOARD";
        return switch (display.trim()) {
            case "Job Applications" -> "APPLICATIONS";
            case "Interview Pipeline" -> "INTERVIEWS";
            case "Career Analytics" -> "ANALYTICS";
            case "Reports & Export" -> "REPORTS";
            default -> "DASHBOARD";
        };
    }

    private record SettingsDataBundle(User user, UserPreferences preferences) {}
}
