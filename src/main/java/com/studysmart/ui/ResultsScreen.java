package com.studysmart.ui;

import com.studysmart.model.Quiz;
import com.studysmart.model.Response;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class ResultsScreen {
    private final App app;
    private final Quiz quiz;
    private final List<Response> responses;
    private final BorderPane view = new BorderPane();
    private final TextArea planArea = new TextArea();
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Button retryButton = new Button("Try Again");

    public ResultsScreen(App app, Quiz quiz, List<Response> responses, String studyPlan) {
        this.app = app;
        this.quiz = quiz;
        this.responses = responses;

        Label title = new Label("Score: " + quiz.getScore() + "/" + responses.size());
        title.getStyleClass().add("title");

        VBox content = new VBox(10);
        for (Response r : responses) {
            content.getChildren().add(buildResultRow(r));
        }

        Label planTitle = new Label("Your Study Plan");
        planTitle.getStyleClass().add("question-heading");
        planArea.setEditable(false);
        planArea.setWrapText(true);
        planArea.setPrefRowCount(10);
        spinner.setPrefSize(28, 28);
        spinner.setVisible(false);
        retryButton.setVisible(false);
        retryButton.setOnAction(e -> generatePlan());
        VBox planCard = new VBox(8, new HBox(10, planTitle, spinner), planArea, retryButton);
        planCard.getStyleClass().add("card");
        content.getChildren().add(planCard);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("primary");
        back.setOnAction(e -> app.showDashboard());

        view.setTop(title);
        view.setCenter(scroll);
        view.setBottom(back);
        BorderPane.setMargin(scroll, new Insets(12, 0, 12, 0));
        view.setPadding(new Insets(20));

        if (studyPlan == null) {
            generatePlan();
        } else {
            planArea.setText(studyPlan);
        }
    }

    private VBox buildResultRow(Response r) {
        Label heading = new Label("Question " + r.getQuestion().getOrder() + ": "
                + (r.isCorrect() ? "Correct" : "Incorrect"));
        heading.getStyleClass().add("question-heading");
        Label text = new Label(r.getQuestion().getText());
        text.setWrapText(true);
        Label answers = new Label("Your answer: " + r.getUserAnswer()
                + "    Correct answer: " + r.getQuestion().getCorrectAnswer()
                + "    Topic: " + r.getQuestion().getTopic());
        answers.setWrapText(true);

        VBox row = new VBox(4, heading, text, answers);
        row.getStyleClass().addAll("card", r.isCorrect() ? "result-correct" : "result-incorrect");
        return row;
    }

    private void generatePlan() {
        spinner.setVisible(true);
        retryButton.setVisible(false);
        planArea.setText("Generating your personalised study plan...");
        app.runTask(() -> app.getStudyPlanGenerator().generate(quiz, responses),
                plan -> {
                    spinner.setVisible(false);
                    planArea.setText(plan);
                },
                error -> {
                    spinner.setVisible(false);
                    retryButton.setVisible(true);
                    planArea.setText("The study plan could not be created.\n" + error);
                });
    }

    public Parent getView() {
        return view;
    }
}
