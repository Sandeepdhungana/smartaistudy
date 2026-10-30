package com.studysmart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studysmart.db.Database;
import com.studysmart.db.QuizDAO;
import com.studysmart.model.Question;
import com.studysmart.model.Quiz;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class QuizGenerator {
    public static final int QUESTION_COUNT = 10;
    private static final int MAX_ATTEMPTS = 3;
    // How many of each difficulty the final quiz aims for (hard, medium, easy)
    private static final int[] DIFFICULTY_MIX = {3, 4, 3};

    // Asks for 12 so a reply that is a question or two short (or has a bad one) still leaves 10
    private static final String QUIZ_PROMPT =
            "You are a teacher writing a revision quiz. Using ONLY the study material in the PDF given by the user, "
            + "write exactly 12 different true or false questions: 4 hard, 4 medium and 4 easy. "
            + "Each question must be a single statement that is clearly either true or false. "
            + "Count the questions before replying and make sure there are 12. "
            + "Return JSON in exactly this format: "
            + "{\"questions\": [{\"question\": \"statement to judge\", \"answer\": \"true or false\", "
            + "\"topic\": \"short topic name\", \"difficulty\": \"hard, medium or easy\"}]}";

    private final OpenAIClient ai;
    private final QuizDAO quizDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    public QuizGenerator(OpenAIClient ai, QuizDAO quizDAO) {
        this.ai = ai;
        this.quizDAO = quizDAO;
    }

    public Quiz generate(int userId, int materialId, String fileName, byte[] pdf) throws AIServiceException, SQLException {
        // Keeps asking (up to MAX_ATTEMPTS times) until there are at least 10 different valid questions
        ArrayList<Question> collected = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        AIServiceException lastError = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS && collected.size() < QUESTION_COUNT; attempt++) {
            try {
                for (Question q : parseQuestions(ai.askAboutPdf(QUIZ_PROMPT, fileName, pdf, true))) {
                    if (seen.add(q.getText().toLowerCase())) {
                        collected.add(q);
                    }
                }
            } catch (AIServiceException e) {
                lastError = e;
            }
        }
        if (collected.size() < QUESTION_COUNT) {
            if (collected.isEmpty() && lastError != null) {
                throw lastError;
            }
            throw new AIServiceException("The AI could not write 10 questions from this PDF. "
                    + "Check it contains readable study notes and try again.");
        }

        ArrayList<Question> questions = pickTen(collected);
        sortByDifficulty(questions);

        Quiz quiz = new Quiz(0, userId, materialId, Database.now(), -1);
        quiz.setQuestions(questions);
        quizDAO.save(quiz);
        return quiz;
    }

    // Returns every valid question in the reply; invalid ones are skipped rather than failing the whole quiz
    public ArrayList<Question> parseQuestions(String json) throws AIServiceException {
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new AIServiceException("The AI reply was not in the expected format. Please try again.");
        }

        ArrayList<Question> questions = new ArrayList<>();
        for (JsonNode item : findQuestionArray(root)) {
            String text = item.path("question").asText("").trim();
            String answer = item.path("answer").asText("").trim().toLowerCase();
            String topic = item.path("topic").asText("").trim();
            String difficulty = item.path("difficulty").asText("").trim().toLowerCase();

            if (text.isEmpty() || !(answer.equals("true") || answer.equals("false"))) {
                continue;
            }
            if (topic.isEmpty()) {
                topic = "General";
            }
            if (difficultyRank(difficulty) == -1) {
                difficulty = "medium";
            }
            questions.add(new Question(0, text, answer, topic, difficulty, 0));
        }
        return questions;
    }

    // Normally {"questions": [...]}, but also accepts a bare array or the list under a different key
    private JsonNode findQuestionArray(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        if (root.path("questions").isArray()) {
            return root.path("questions");
        }
        for (JsonNode child : root) {
            if (child.isArray()) {
                return child;
            }
        }
        return mapper.createArrayNode();
    }

    // Chooses 10 questions, aiming for 3 hard, 4 medium and 3 easy, topping up from whatever is left
    static ArrayList<Question> pickTen(List<Question> candidates) {
        ArrayList<Question> remaining = new ArrayList<>(candidates);
        ArrayList<Question> picked = new ArrayList<>();
        for (int rank = 0; rank < DIFFICULTY_MIX.length; rank++) {
            int needed = DIFFICULTY_MIX[rank];
            Iterator<Question> it = remaining.iterator();
            while (needed > 0 && it.hasNext()) {
                Question q = it.next();
                if (difficultyRank(q.getDifficulty()) == rank) {
                    picked.add(q);
                    it.remove();
                    needed--;
                }
            }
        }
        for (int i = 0; picked.size() < QUESTION_COUNT && i < remaining.size(); i++) {
            picked.add(remaining.get(i));
        }
        return picked;
    }

    // Orders hard -> medium -> easy, then numbers the questions 1 to 10
    public static void sortByDifficulty(List<Question> questions) {
        questions.sort(Comparator.comparingInt(q -> difficultyRank(q.getDifficulty())));
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).setOrder(i + 1);
        }
    }

    private static int difficultyRank(String difficulty) {
        switch (difficulty) {
            case "hard": return 0;
            case "medium": return 1;
            case "easy": return 2;
            default: return -1;
        }
    }
}
