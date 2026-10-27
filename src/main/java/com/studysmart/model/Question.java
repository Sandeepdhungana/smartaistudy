package com.studysmart.model;

public class Question {
    private int id;
    private final String text;
    private final String correctAnswer;
    private final String topic;
    private final String difficulty;
    private int order;

    public Question(int id, String text, String correctAnswer, String topic, String difficulty, int order) {
        this.id = id;
        this.text = text;
        this.correctAnswer = correctAnswer;
        this.topic = topic;
        this.difficulty = difficulty;
        this.order = order;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getText() { return text; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getTopic() { return topic; }
    public String getDifficulty() { return difficulty; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}
