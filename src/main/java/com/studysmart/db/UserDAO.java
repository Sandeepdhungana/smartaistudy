package com.studysmart.db;

import com.studysmart.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDAO {
    private final Database db;

    public UserDAO(Database db) {
        this.db = db;
    }

    public User insert(String username, String email, String passwordHash) throws SQLException {
        String sql = "INSERT INTO Users (username, email, password_hash) VALUES (?, ?, ?)";
        try (Connection conn = db.connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return new User(keys.getInt(1), username, email);
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, username, email FROM Users WHERE email = ?";
        try (Connection conn = db.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            return new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("email"));
        }
    }

    public String getPasswordHash(String email) throws SQLException {
        String sql = "SELECT password_hash FROM Users WHERE email = ?";
        try (Connection conn = db.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }
}
