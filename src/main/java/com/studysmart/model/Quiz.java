package com.studysmart.model;

import java.util.ArrayList;
import java.util.List;

public class Quiz {
    private int id;
    private final int userId;
    private final int materialId;
    private final String createdAt;
    private int score;
    private String fileName;
    private List<Question> questions = new ArrayList<>();

    public Quiz(int id, int userId, int materialId, String createdAt, int score) {
        this.id = id;
        this.userId = userId;
        this.materialId = materialId;
        this.createdAt = createdAt;
        this.score = score;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public int getMaterialId() { return materialId; }
    public String getCreatedAt() { return createdAt; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}
