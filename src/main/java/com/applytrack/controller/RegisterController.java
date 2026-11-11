package com.applytrack.controller;

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
 * Controller for the ApplyTrack Candidate Registration screen.
 *
 * Demonstrates:
 * - Form input capture and real-time user feedback - Unit 1/3
 * - Exception Handling (catch ValidationException, SQLException) - Unit 3
 * - Clean UI redirect flow returning to Login - Unit 2
 */
public class RegisterController implements Initializable {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button registerButton;
    @FXML private Button backToLoginButton;

    private final AuthService authService = new AuthService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearMessage();
        Platform.runLater(() -> fullNameField.requestFocus());
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        clearMessage();

        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        registerButton.setDisable(true);
        registerButton.setText("Creating Account...");

        try {
            User newUser = authService.register(fullName, email, password, confirmPassword);
            if (newUser != null) {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                // Redirect back to login screen with success message and pre-filled email
                SceneNavigator.showLoginScreen(stage,
                        "Registration successful! Welcome to ApplyTrack, " + newUser.getFullName() + ". Please sign in.",
                        newUser.getEmail());
            }
        } catch (ValidationException e) {
            showErrorMessage(e.getMessage());
        } catch (SQLException e) {
            showErrorMessage("Database error during registration: " + e.getMessage());
        } finally {
            registerButton.setDisable(false);
            registerButton.setText("Create Candidate Account");
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SceneNavigator.showLoginScreen(stage, null, null);
    }

    private void showErrorMessage(String error) {
        if (messageLabel != null) {
            messageLabel.setText(error);
            messageLabel.setStyle("-fx-text-fill: #dc2626; -fx-background-color: #fef2f2; -fx-border-color: #fca5a5; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px 12px;");
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

