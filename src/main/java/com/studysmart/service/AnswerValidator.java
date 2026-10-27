package com.studysmart.service;

public class AnswerValidator {
    public static final String WARNING = "You may be entering an invalid answer. Please type true or false only.";

    public static String normalise(String input) {
        return input == null ? "" : input.trim().toLowerCase();
    }

    public static boolean isValid(String input) {
        String answer = normalise(input);
        return answer.equals("true") || answer.equals("false");
    }
}
