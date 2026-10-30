package com.studysmart.model;

public class StudyMaterial {
    private final int id;
    private final int userId;
    private final String fileName;

    public StudyMaterial(int id, int userId, String fileName) {
        this.id = id;
        this.userId = userId;
        this.fileName = fileName;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getFileName() { return fileName; }
}
