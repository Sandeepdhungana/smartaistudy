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
import java.util.List;

public class QuizGenerator {
    public static final int QUESTION_COUNT = 10;

    private static final String QUIZ_PROMPT =
            "You are a teacher writing a revision quiz. Using ONLY the study material given by the user, "
            + "write exactly 10 true or false questions: 3 hard, 4 medium and 3 easy. "
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

    public Quiz generate(int userId, int materialId, String studyText) throws AIServiceException, SQLException {
        String reply = ai.ask(QUIZ_PROMPT, studyText, true);
        ArrayList<Question> questions = parseQuestions(reply);
        sortByDifficulty(questions);

        Quiz quiz = new Quiz(0, userId, materialId, Database.now(), -1);
        quiz.setQuestions(questions);
        quizDAO.save(quiz);
        return quiz;
    }

    public ArrayList<Question> parseQuestions(String json) throws AIServiceException {
        JsonNode items;
        try {
            items = mapper.readTree(json).path("questions");
        } catch (JsonProcessingException e) {
            throw new AIServiceException("The AI reply was not in the expected format. Please try again.");
        }
        if (!items.isArray() || items.size() != QUESTION_COUNT) {
            throw new AIServiceException("The AI did not return exactly 10 questions. Please try again.");
        }

        ArrayList<Question> questions = new ArrayList<>();
        for (JsonNode item : items) {
            String text = item.path("question").asText("").trim();
            String answer = item.path("answer").asText("").trim().toLowerCase();
            String topic = item.path("topic").asText("").trim();
            String difficulty = item.path("difficulty").asText("").trim().toLowerCase();

            if (text.isEmpty() || !(answer.equals("true") || answer.equals("false"))) {
                throw new AIServiceException("The AI returned an invalid question. Please try again.");
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
