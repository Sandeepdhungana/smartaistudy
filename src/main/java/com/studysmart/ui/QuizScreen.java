package com.studysmart.ui;

import com.studysmart.model.Question;
import com.studysmart.model.Quiz;
import com.studysmart.service.AnswerValidator;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class QuizScreen {
    private final App app;
    private final Quiz quiz;
    private final BorderPane view = new BorderPane();
    private final List<TextField> answerFields = new ArrayList<>();
    private final List<Label> warnings = new ArrayList<>();
    private final Button submitButton = new Button("Submit Answers");
    private final ProgressIndicator spinner = new ProgressIndicator();

    public QuizScreen(App app, Quiz quiz) {
        this.app = app;
        this.quiz = quiz;

        Label title = new Label("Quiz: " + (quiz.getFileName() == null ? "your study material" : quiz.getFileName()));
        title.getStyleClass().add("title");

        VBox questionList = new VBox(12);
        for (Question q : quiz.getQuestions()) {
            questionList.getChildren().add(buildQuestionCard(q));
        }
        ScrollPane scroll = new ScrollPane(questionList);
        scroll.setFitToWidth(true);

        submitButton.getStyleClass().add("primary");
        submitButton.setOnAction(e -> submit());
        Button back = new Button("Back to Dashboard");
        back.setOnAction(e -> app.showDashboard());
        spinner.setVisible(false);
        spinner.setPrefSize(28, 28);
        HBox bottom = new HBox(10, submitButton, back, spinner);

        view.setTop(title);
        view.setCenter(scroll);
        view.setBottom(bottom);
        BorderPane.setMargin(scroll, new Insets(12, 0, 12, 0));
        view.setPadding(new Insets(20));
    }

    private VBox buildQuestionCard(Question q) {
        Label heading = new Label("Question " + q.getOrder() + "  (" + q.getDifficulty() + ")");
        heading.getStyleClass().add("question-heading");
        Label text = new Label(q.getText());
        text.setWrapText(true);

        TextField field = new TextField();
        field.setPromptText("Type: true / false");
        Label warning = new Label(AnswerValidator.WARNING);
        warning.getStyleClass().add("warning");
        warning.setVisible(false);
        warning.setManaged(false);

        // Shows the yellow warning as soon as the typed answer is not true/false
        field.textProperty().addListener((obs, oldText, newText) ->
                markField(field, warning, newText.isBlank() || AnswerValidator.isValid(newText)));

        answerFields.add(field);
        warnings.add(warning);
        VBox card = new VBox(6, heading, text, field, warning);
        card.getStyleClass().add("card");
        return card;
    }

    private void markField(TextField field, Label warning, boolean valid) {
        field.getStyleClass().remove("invalid");
        if (!valid) {
            field.getStyleClass().add("invalid");
        }
        warning.setVisible(!valid);
        warning.setManaged(!valid);
    }

    private void submit() {
        boolean allValid = true;
        List<String> answers = new ArrayList<>();
        for (int i = 0; i < answerFields.size(); i++) {
            String answer = answerFields.get(i).getText();
            boolean valid = AnswerValidator.isValid(answer);
            markField(answerFields.get(i), warnings.get(i), valid);
            if (!valid) {
                allValid = false;
            }
            answers.add(answer);
        }
        if (!allValid) {
            return;
        }

        submitButton.setDisable(true);
        spinner.setVisible(true);
        app.runTask(() -> app.getAnswerEvaluator().evaluate(quiz, answers),
                responses -> app.showResults(quiz, responses, null),
                error -> {
                    submitButton.setDisable(false);
                    spinner.setVisible(false);
                    App.showError(error);
                });
    }

    public Parent getView() {
        return view;
    }
}
