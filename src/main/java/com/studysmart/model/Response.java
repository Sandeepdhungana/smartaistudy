package com.studysmart.model;

public class Response {
    private final Question question;
    private final String userAnswer;
    private final boolean correct;

    public Response(Question question, String userAnswer, boolean correct) {
        this.question = question;
        this.userAnswer = userAnswer;
        this.correct = correct;
    }

    public Question getQuestion() { return question; }
    public String getUserAnswer() { return userAnswer; }
    public boolean isCorrect() { return correct; }
}
