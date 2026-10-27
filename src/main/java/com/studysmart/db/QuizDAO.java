package com.studysmart.db;

import com.studysmart.model.Question;
import com.studysmart.model.Quiz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QuizDAO {
    private final Database db;

    public QuizDAO(Database db) {
        this.db = db;
    }

    // Saves the quiz and all its questions together, so a half-saved quiz is never left behind
    public void save(Quiz quiz) throws SQLException {
        String quizSql = "INSERT INTO Quizzes (user_id, material_id, created_at, score) VALUES (?, ?, ?, NULL)";
        String questionSql = "INSERT INTO Questions (quiz_id, question_text, correct_answer, topic, difficulty, question_order) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.connect()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement ps = conn.prepareStatement(quizSql, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, quiz.getUserId());
                ps.setInt(2, quiz.getMaterialId());
                ps.setString(3, quiz.getCreatedAt());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                keys.next();
                quiz.setId(keys.getInt(1));

                PreparedStatement qs = conn.prepareStatement(questionSql, Statement.RETURN_GENERATED_KEYS);
                for (Question q : quiz.getQuestions()) {
                    qs.setInt(1, quiz.getId());
                    qs.setString(2, q.getText());
                    qs.setString(3, q.getCorrectAnswer());
                    qs.setString(4, q.getTopic());
                    qs.setString(5, q.getDifficulty());
                    qs.setInt(6, q.getOrder());
                    qs.executeUpdate();
                    ResultSet qKeys = qs.getGeneratedKeys();
                    qKeys.next();
                    q.setId(qKeys.getInt(1));
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void updateScore(int quizId, int score) throws SQLException {
        try (Connection conn = db.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE Quizzes SET score = ? WHERE quiz_id = ?")) {
            ps.setInt(1, score);
            ps.setInt(2, quizId);
            ps.executeUpdate();
        }
    }

    public List<Quiz> findByUser(int userId) throws SQLException {
        String sql = "SELECT q.quiz_id, q.material_id, q.created_at, q.score, m.file_name "
                + "FROM Quizzes q JOIN StudyMaterials m ON q.material_id = m.material_id "
                + "WHERE q.user_id = ? ORDER BY q.quiz_id DESC";
        List<Quiz> quizzes = new ArrayList<>();
        try (Connection conn = db.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int score = rs.getInt("score");
                if (rs.wasNull()) {
                    score = -1;
                }
                Quiz quiz = new Quiz(rs.getInt("quiz_id"), userId, rs.getInt("material_id"),
                        rs.getString("created_at"), score);
                quiz.setFileName(rs.getString("file_name"));
                quizzes.add(quiz);
            }
        }
        return quizzes;
    }

    public List<Question> findQuestions(int quizId) throws SQLException {
        String sql = "SELECT * FROM Questions WHERE quiz_id = ? ORDER BY question_order";
        List<Question> questions = new ArrayList<>();
        try (Connection conn = db.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quizId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                questions.add(new Question(rs.getInt("question_id"), rs.getString("question_text"),
                        rs.getString("correct_answer"), rs.getString("topic"),
                        rs.getString("difficulty"), rs.getInt("question_order")));
            }
        }
        return questions;
    }
}
