package com.studysmart.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerValidatorTest {
    @Test
    void acceptsTrueAndFalse() {
        assertTrue(AnswerValidator.isValid("true"));
        assertTrue(AnswerValidator.isValid("false"));
        assertTrue(AnswerValidator.isValid("  FALSE "));
    }

    @Test
    void rejectsOtherInput() {
        assertFalse(AnswerValidator.isValid("yes"));
        assertFalse(AnswerValidator.isValid("tru"));
        assertFalse(AnswerValidator.isValid(""));
        assertFalse(AnswerValidator.isValid("   "));
        assertFalse(AnswerValidator.isValid(null));
    }
}
