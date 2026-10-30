package com.studysmart.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DatabaseTest {
    @TempDir
    Path tempDir;

    @Test
    void oldExtractedTextColumnIsDropped() throws Exception {
        String path = tempDir.resolve("old.db").toString();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE Users (user_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL, "
                    + "email TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL)");
            st.execute("CREATE TABLE StudyMaterials (material_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL REFERENCES Users(user_id), file_name TEXT NOT NULL, "
                    + "extracted_text TEXT NOT NULL, upload_date TEXT NOT NULL)");
            st.execute("INSERT INTO Users (username, email, password_hash) VALUES ('sam', 'sam@example.com', 'hash')");
            st.execute("INSERT INTO StudyMaterials (user_id, file_name, extracted_text, upload_date) "
                    + "VALUES (1, 'old.pdf', 'old text', '2026-01-01 10:00:00')");
        }

        Database db = new Database(path);

        try (Connection conn = db.connect(); Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("PRAGMA table_info(StudyMaterials)")) {
                while (rs.next()) {
                    assertFalse("extracted_text".equals(rs.getString("name")));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT file_name FROM StudyMaterials")) {
                rs.next();
                assertEquals("old.pdf", rs.getString(1));
            }
        }
        assertEquals(2, new MaterialDAO(db).insert(1, "new.pdf").getId());
    }
}
