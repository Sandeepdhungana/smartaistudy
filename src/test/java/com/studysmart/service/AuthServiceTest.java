package com.studysmart.service;

import com.studysmart.db.UserDAO;
import com.studysmart.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {
    private AuthService auth;

    @BeforeEach
    void setUp() throws Exception {
        auth = new AuthService(new UserDAO(TestData.tempDatabase()));
    }

    @Test
    void signUpThenLogIn() throws Exception {
        auth.signUp("sam", "Sam@Example.com", "secret123");
        User user = auth.logIn("sam@example.com", "secret123");
        assertEquals("sam", user.getUsername());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        auth.signUp("sam", "sam@example.com", "secret123");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> auth.logIn("sam@example.com", "wrong"));
        assertEquals("Incorrect password.", e.getMessage());
    }

    @Test
    void unknownEmailIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> auth.logIn("nobody@example.com", "secret123"));
        assertEquals("User does not exist.", e.getMessage());
    }

    @Test
    void emptyFieldsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> auth.signUp("", "a@b.com", "pw"));
        assertThrows(IllegalArgumentException.class, () -> auth.signUp("sam", "a@b.com", " "));
        assertThrows(IllegalArgumentException.class, () -> auth.logIn("", "pw"));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        auth.signUp("sam", "sam@example.com", "secret123");
        assertThrows(IllegalArgumentException.class, () -> auth.signUp("sam2", "sam@example.com", "other"));
    }

    @Test
    void passwordIsNotStoredInPlainText() {
        String hash = AuthService.hashPassword("secret123");
        assertNotEquals("secret123", hash);
        assertNotEquals(hash, AuthService.hashPassword("secret123"));
    }
}
