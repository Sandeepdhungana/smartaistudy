package com.studysmart.db;

import com.studysmart.model.Question;
import com.studysmart.model.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResponseDAO {
    private final Database db;

    public ResponseDAO(Database db) {
        this.db = db;
    }

    public void saveAll(int quizId, List<Response> responses) throws SQLException {
        String sql = "INSERT INTO Responses (quiz_id, question_id, user_answer, is_correct) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Response r : responses) {
                ps.setInt(1, quizId);
                ps.setInt(2, r.getQuestion().getId());
                ps.setString(3, r.getUserAnswer());
                ps.setBoolean(4, r.isCorrect());
                ps.executeUpdate();
            }
            conn.commit();
        }
    }

    // Matches each stored response back to its question object
    public List<Response> findByQuiz(int quizId, List<Question> questions) throws SQLException {
        String sql = "SELECT question_id, user_answer, is_correct FROM Responses WHERE quiz_id = ?";
        List<Response> responses = new ArrayList<>();
        try (Connection conn = db.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                for (Question q : questions) {
                    if (q.getId() == rs.getInt("question_id")) {
                        responses.add(new Response(q, rs.getString("user_answer"), rs.getBoolean("is_correct")));
                    }
                }
            }
        }
        return responses;
    }
}
