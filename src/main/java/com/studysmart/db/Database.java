package com.studysmart.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public class Database {
    private final String url;

    public Database(String filePath) throws SQLException {
        this.url = "jdbc:sqlite:" + filePath;
        createTables();
    }

    public Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    public static String now() {
        return LocalDateTime.now().withNano(0).toString().replace('T', ' ');
    }

    private void createTables() throws SQLException {
        String[] tables = {
            "CREATE TABLE IF NOT EXISTS Users ("
                + "user_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT NOT NULL,"
                + "email TEXT NOT NULL UNIQUE,"
                + "password_hash TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS StudyMaterials ("
                + "material_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER NOT NULL REFERENCES Users(user_id),"
                + "file_name TEXT NOT NULL,"
                + "upload_date TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS Quizzes ("
                + "quiz_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER NOT NULL REFERENCES Users(user_id),"
                + "material_id INTEGER NOT NULL REFERENCES StudyMaterials(material_id),"
                + "created_at TEXT NOT NULL,"
                + "score INTEGER)",
            "CREATE TABLE IF NOT EXISTS Questions ("
                + "question_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "quiz_id INTEGER NOT NULL REFERENCES Quizzes(quiz_id),"
                + "question_text TEXT NOT NULL,"
                + "correct_answer TEXT NOT NULL,"
                + "topic TEXT,"
                + "difficulty TEXT,"
                + "question_order INTEGER)",
            "CREATE TABLE IF NOT EXISTS Responses ("
                + "response_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "quiz_id INTEGER NOT NULL REFERENCES Quizzes(quiz_id),"
                + "question_id INTEGER NOT NULL REFERENCES Questions(question_id),"
                + "user_answer TEXT NOT NULL,"
                + "is_correct BOOLEAN NOT NULL)",
            "CREATE TABLE IF NOT EXISTS StudyPlans ("
                + "plan_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "quiz_id INTEGER NOT NULL UNIQUE REFERENCES Quizzes(quiz_id),"
                + "plan_text TEXT NOT NULL,"
                + "created_at TEXT NOT NULL)"
        };
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            for (String sql : tables) {
                st.execute(sql);
            }
            dropExtractedTextColumn(st);
        }
    }

    // Databases made before PDFs were sent straight to OpenAI still have this unused column
    private void dropExtractedTextColumn(Statement st) throws SQLException {
        boolean found = false;
        try (ResultSet rs = st.executeQuery("PRAGMA table_info(StudyMaterials)")) {
            while (rs.next()) {
                if ("extracted_text".equals(rs.getString("name"))) {
                    found = true;
                }
            }
        }
        if (found) {
            st.execute("ALTER TABLE StudyMaterials DROP COLUMN extracted_text");
        }
    }
}
