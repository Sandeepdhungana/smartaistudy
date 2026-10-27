package com.studysmart.model;

public class StudyPlan {
    private final int id;
    private final int quizId;
    private final String planText;
    private final String createdAt;

    public StudyPlan(int id, int quizId, String planText, String createdAt) {
        this.id = id;
        this.quizId = quizId;
        this.planText = planText;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public int getQuizId() { return quizId; }
    public String getPlanText() { return planText; }
    public String getCreatedAt() { return createdAt; }
}
