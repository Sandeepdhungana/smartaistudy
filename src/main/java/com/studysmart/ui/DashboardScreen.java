package com.studysmart.ui;

import com.studysmart.model.Quiz;
import com.studysmart.model.Response;
import com.studysmart.model.StudyMaterial;
import com.studysmart.model.StudyPlan;
import com.studysmart.model.User;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class DashboardScreen {
    private final App app;
    private final BorderPane view = new BorderPane();
    private final Label fileLabel = new Label("No file selected");
    private final Button uploadButton = new Button("Upload PDF");
    private final Button generateButton = new Button("Generate Quiz");
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Label status = new Label();
    private final ListView<Quiz> history = new ListView<>();
    private File selectedFile;

    public DashboardScreen(App app) {
        this.app = app;
        User user = app.getCurrentUser();

        Label welcome = new Label("Welcome, " + user.getUsername());
        welcome.getStyleClass().add("title");
        Button logOut = new Button("Log Out");
        logOut.setOnAction(e -> app.showLogin());
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox top = new HBox(welcome, gap, logOut);

        uploadButton.getStyleClass().add("primary");
        generateButton.getStyleClass().add("primary");
        uploadButton.setOnAction(e -> chooseFile());
        generateButton.setOnAction(e -> generateQuiz());
        spinner.setVisible(false);
        spinner.setPrefSize(28, 28);
        status.setWrapText(true);

        Label hint = new Label("Only PDF files are accepted.");
        hint.getStyleClass().add("hint");
        VBox uploadCard = new VBox(10, new Label("Study material"), uploadButton, fileLabel, hint,
                new HBox(10, generateButton, spinner), status);
        uploadCard.getStyleClass().add("card");

        history.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Quiz quiz, boolean empty) {
                super.updateItem(quiz, empty);
                if (empty || quiz == null) {
                    setText(null);
                } else {
                    String score = quiz.getScore() < 0 ? "not finished" : quiz.getScore() + "/10";
                    setText(quiz.getCreatedAt() + "   " + quiz.getFileName() + "   " + score);
                }
            }
        });
        history.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                openQuiz(history.getSelectionModel().getSelectedItem());
            }
        });
        Button openButton = new Button("Open Selected Quiz");
        openButton.setOnAction(e -> openQuiz(history.getSelectionModel().getSelectedItem()));
        VBox historyCard = new VBox(10, new Label("Past quizzes (double-click to open)"), history, openButton);
        historyCard.getStyleClass().add("card");
        VBox.setVgrow(history, Priority.ALWAYS);
        VBox.setVgrow(historyCard, Priority.ALWAYS);

        view.setTop(top);
        view.setCenter(new VBox(16, uploadCard, historyCard));
        BorderPane.setMargin(top, new Insets(0, 0, 16, 0));
        view.setPadding(new Insets(20));
        loadHistory();
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose study material");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF files", "*.pdf"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File file = chooser.showOpenDialog(view.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            fileLabel.setText("Selected file: " + file.getName());
            status.setText("");
        }
    }

    private void generateQuiz() {
        if (selectedFile == null) {
            App.showError("Please upload a PDF file.");
            return;
        }
        File file = selectedFile;
        int userId = app.getCurrentUser().getId();
        setBusy(true, "Reading your PDF and generating questions...");
        app.runTask(() -> {
            String text = app.getPdfService().extractText(file);
            StudyMaterial material = app.getMaterialDAO().insert(userId, file.getName(), text);
            Quiz quiz = app.getQuizGenerator().generate(userId, material.getId(), text);
            quiz.setFileName(file.getName());
            return quiz;
        }, quiz -> {
            setBusy(false, "");
            app.showQuiz(quiz);
        }, error -> {
            setBusy(false, "");
            App.showError(error);
        });
    }

    private void openQuiz(Quiz quiz) {
        if (quiz == null) {
            return;
        }
        try {
            quiz.setQuestions(app.getQuizDAO().findQuestions(quiz.getId()));
            if (quiz.getScore() < 0) {
                app.showQuiz(quiz);
                return;
            }
            List<Response> responses = app.getResponseDAO().findByQuiz(quiz.getId(), quiz.getQuestions());
            StudyPlan plan = app.getStudyPlanDAO().findByQuiz(quiz.getId());
            app.showResults(quiz, responses, plan == null ? null : plan.getPlanText());
        } catch (SQLException e) {
            App.showError(App.errorMessage(e));
        }
    }

    private void loadHistory() {
        try {
            history.getItems().setAll(app.getQuizDAO().findByUser(app.getCurrentUser().getId()));
        } catch (SQLException e) {
            App.showError(App.errorMessage(e));
        }
    }

    private void setBusy(boolean busy, String text) {
        spinner.setVisible(busy);
        uploadButton.setDisable(busy);
        generateButton.setDisable(busy);
        status.setText(text);
    }

    public Parent getView() {
        return view;
    }
}
