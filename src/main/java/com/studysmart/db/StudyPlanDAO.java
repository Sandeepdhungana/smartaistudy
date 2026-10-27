package com.studysmart.db;

import com.studysmart.model.StudyPlan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudyPlanDAO {
    private final Database db;

    public StudyPlanDAO(Database db) {
        this.db = db;
    }

    public void save(int quizId, String planText) throws SQLException {
        String sql = "INSERT INTO StudyPlans (quiz_id, plan_text, created_at) VALUES (?, ?, ?)";
        try (Connection conn = db.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            ps.setString(2, planText);
            ps.setString(3, Database.now());
            ps.executeUpdate();
        }
    }

    public StudyPlan findByQuiz(int quizId) throws SQLException {
        String sql = "SELECT * FROM StudyPlans WHERE quiz_id = ?";
        try (Connection conn = db.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            return new StudyPlan(rs.getInt("plan_id"), quizId, rs.getString("plan_text"), rs.getString("created_at"));
        }
    }
}
