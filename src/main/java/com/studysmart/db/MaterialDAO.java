package com.studysmart.db;

import com.studysmart.model.StudyMaterial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MaterialDAO {
    private final Database db;

    public MaterialDAO(Database db) {
        this.db = db;
    }

    public StudyMaterial insert(int userId, String fileName) throws SQLException {
        String sql = "INSERT INTO StudyMaterials (user_id, file_name, upload_date) VALUES (?, ?, ?)";
        try (Connection conn = db.connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, fileName);
            ps.setString(3, Database.now());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return new StudyMaterial(keys.getInt(1), userId, fileName);
        }
    }
}
