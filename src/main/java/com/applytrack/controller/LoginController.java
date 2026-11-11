package com.applytrack.controller;

import com.applytrack.exception.AuthenticationException;
import com.applytrack.exception.ValidationException;
import com.applytrack.model.User;
import com.applytrack.service.AuthService;
import com.applytrack.util.SceneNavigator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Controller for the ApplyTrack Login screen.
 *
 * Demonstrates:
 * - OOP: Encapsulation & Separation of Concerns (UI logic separate from business rules) - Unit 3
 * - Exception Handling (catch ValidationException, AuthenticationException, SQLException) - Unit 3
 * - State Management: Show/Hide password toggle logic - Unit 1/2
 */
public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordBtn;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;
    @FXML private Button demoLoginButton;
    @FXML private Button registerLinkButton;

    private final AuthService authService = new AuthService();
    private boolean isPasswordVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Sync password field and visible text field
        passwordVisibleField.setManaged(false);
        passwordVisibleField.setVisible(false);

        passwordField.textProperty().bindBidirectional(passwordVisibleField.textProperty());

        clearMessage();
        Platform.runLater(() -> emailField.requestFocus());
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        clearMessage();
        String email = emailField.getText();
        String password = passwordField.getText();

        loginButton.setDisable(true);
        loginButton.setText("Signing In...");

        try {
            User authenticatedUser = authService.login(email, password);
            if (authenticatedUser != null) {
                // Restore user's saved visual theme preference
                try {
                    com.applytrack.service.SettingsService ss = new com.applytrack.service.SettingsService();
                    com.applytrack.model.UserPreferences up = ss.getPreferences(authenticatedUser.getId());
                    if (up != null && up.getTheme() != null) {
                        com.applytrack.util.ThemeManager.setCurrentTheme(up.getTheme());
                    }
                } catch (Exception ex) {
                    System.err.println("Notice: Could not load user theme on login: " + ex.getMessage());
                }

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                SceneNavigator.showMainDashboard(stage);
            }
        } catch (ValidationException | AuthenticationException e) {
            showErrorMessage(e.getMessage());
        } catch (SQLException e) {
            showErrorMessage("Database connection error: " + e.getMessage());
        } finally {
            loginButton.setDisable(false);
            loginButton.setText("Sign In to ApplyTrack");
        }
    }

    @FXML
    private void handleTogglePassword(ActionEvent event) {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordField.setManaged(false);
            passwordField.setVisible(false);
            passwordVisibleField.setManaged(true);
            passwordVisibleField.setVisible(true);
            togglePasswordBtn.setText("Hide");
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
        } else {
            passwordVisibleField.setManaged(false);
            passwordVisibleField.setVisible(false);
            passwordField.setManaged(true);
            passwordField.setVisible(true);
            togglePasswordBtn.setText("Show");
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    @FXML
    private void handleUseDemoAccount(ActionEvent event) {
        emailField.setText("alex.mercer@applytrack.io");
        passwordField.setText("password123");
        clearMessage();
        showInfoMessage("Demo credentials loaded! Click 'Sign In' or press Enter.");
    }

    @FXML
    private void handleGoToRegister(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SceneNavigator.showRegisterScreen(stage);
    }

    public void showSuccessBanner(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #059669; -fx-background-color: #ecfdf5; -fx-border-color: #a7f3d0; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px 12px;");
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        }
    }

    public void prefillEmail(String email) {
        if (emailField != null && email != null) {
            emailField.setText(email);
            if (passwordField != null) {
                Platform.runLater(() -> passwordField.requestFocus());
            }
        }
    }

    private void showErrorMessage(String error) {
        if (messageLabel != null) {
            messageLabel.setText(error);
            messageLabel.setStyle("-fx-text-fill: #dc2626; -fx-background-color: #fef2f2; -fx-border-color: #fca5a5; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px 12px;");
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        }
    }

    private void showInfoMessage(String info) {
        if (messageLabel != null) {
            messageLabel.setText(info);
            messageLabel.setStyle("-fx-text-fill: #0284c7; -fx-background-color: #f0f9ff; -fx-border-color: #bae6fd; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px 12px;");
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        }
    }

    private void clearMessage() {
        if (messageLabel != null) {
            messageLabel.setText("");
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
        }
    }
}

