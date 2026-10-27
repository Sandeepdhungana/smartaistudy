package com.studysmart.service;

import com.studysmart.db.UserDAO;
import com.studysmart.model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;

public class AuthService {
    private static final int ITERATIONS = 65536;
    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User signUp(String username, String email, String password) throws SQLException {
        if (isBlank(username) || isBlank(email) || isBlank(password)) {
            throw new IllegalArgumentException("Please fill in all fields.");
        }
        email = email.trim().toLowerCase();
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (userDAO.findByEmail(email) != null) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User user = userDAO.insert(username.trim(), email, hashPassword(password));
        return user;
    }

    public User logIn(String email, String password) throws SQLException {
        if (isBlank(email) || isBlank(password)) {
            throw new IllegalArgumentException("Please fill in all fields.");
        }
        email = email.trim().toLowerCase();
        String storedHash = userDAO.getPasswordHash(email);
        if (storedHash == null) {
            throw new IllegalArgumentException("User does not exist.");
        }
        if (!checkPassword(password, storedHash)) {
            throw new IllegalArgumentException("Incorrect password.");
        }
        return userDAO.findByEmail(email);
    }

    // Stored as "salt:hash" so the same password never gives the same stored value twice
    public static String hashPassword(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        Base64.Encoder b64 = Base64.getEncoder();
        return b64.encodeToString(salt) + ":" + b64.encodeToString(pbkdf2(password, salt));
    }

    public static boolean checkPassword(String password, String stored) {
        String[] parts = stored.split(":");
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        return MessageDigest.isEqual(expected, pbkdf2(password, salt));
    }

    private static byte[] pbkdf2(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is not available", e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
