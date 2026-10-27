package com.studysmart.model;

public class StudyMaterial {
    private final int id;
    private final int userId;
    private final String fileName;
    private final String extractedText;

    public StudyMaterial(int id, int userId, String fileName, String extractedText) {
        this.id = id;
        this.userId = userId;
        this.fileName = fileName;
        this.extractedText = extractedText;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getFileName() { return fileName; }
    public String getExtractedText() { return extractedText; }
}
