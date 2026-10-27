package com.studysmart.ui;

import com.studysmart.model.User;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

public class LoginScreen {
    private final App app;
    private final VBox view = new VBox(12);
    private final TextField usernameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label message = new Label();
    private final Button mainButton = new Button("Log In");
    private final Hyperlink switchLink = new Hyperlink("No account? Create one");
    private boolean signUpMode = false;

    public LoginScreen(App app) {
        this.app = app;

        Label title = new Label("Study Smart AI");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Turn your notes into a quiz and a study plan.");

        usernameField.setPromptText("Username");
        emailField.setPromptText("Email");
        passwordField.setPromptText("Password");
        mainButton.getStyleClass().add("primary");
        mainButton.setDefaultButton(true);
        mainButton.setMaxWidth(Double.MAX_VALUE);
        message.setWrapText(true);

        mainButton.setOnAction(e -> submit());
        switchLink.setOnAction(e -> setSignUpMode(!signUpMode));

        VBox card = new VBox(10, title, subtitle, usernameField, emailField, passwordField,
                mainButton, switchLink, message);
        card.getStyleClass().add("card");
        card.setMaxWidth(360);

        view.getChildren().add(card);
        view.setAlignment(Pos.CENTER);
        setSignUpMode(false);
    }

    private void setSignUpMode(boolean signUp) {
        signUpMode = signUp;
        usernameField.setVisible(signUp);
        usernameField.setManaged(signUp);
        mainButton.setText(signUp ? "Create Account" : "Log In");
        switchLink.setText(signUp ? "Already have an account? Log in" : "No account? Create one");
        message.setText("");
    }

    private void submit() {
        try {
            if (signUpMode) {
                app.getAuthService().signUp(usernameField.getText(), emailField.getText(), passwordField.getText());
                setSignUpMode(false);
                passwordField.clear();
                showMessage("Account created successfully. Please log in.", "success");
            } else {
                User user = app.getAuthService().logIn(emailField.getText(), passwordField.getText());
                app.setCurrentUser(user);
                app.showDashboard();
            }
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), "error");
        } catch (SQLException e) {
            showMessage(App.errorMessage(e), "error");
        }
    }

    private void showMessage(String text, String styleClass) {
        message.getStyleClass().removeAll("error", "success");
        message.getStyleClass().add(styleClass);
        message.setText(text);
    }

    public Parent getView() {
        return view;
    }
}
