package com.studysmart.service;

import com.studysmart.db.Database;
import com.studysmart.db.MaterialDAO;
import com.studysmart.db.QuizDAO;
import com.studysmart.db.ResponseDAO;
import com.studysmart.db.StudyPlanDAO;
import com.studysmart.db.UserDAO;
import com.studysmart.model.Question;
import com.studysmart.model.Quiz;
import com.studysmart.model.Response;
import com.studysmart.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizFlowTest {
    private Database db;
    private QuizDAO quizDAO;
    private int userId;
    private int materialId;

    @BeforeEach
    void setUp() throws Exception {
        db = TestData.tempDatabase();
        quizDAO = new QuizDAO(db);
        User user = new UserDAO(db).insert("sam", "sam@example.com", "hash");
        userId = user.getId();
        materialId = new MaterialDAO(db).insert(userId, "notes.pdf").getId();
    }

    @Test
    void quizIsSortedHardToEasyAndSaved() throws Exception {
        QuizGenerator generator = new QuizGenerator(TestData.fakeAi(TestData.quizJson()), quizDAO);
        Quiz quiz = generator.generate(userId, materialId, "notes.pdf", TestData.pdfBytes());

        List<Question> saved = quizDAO.findQuestions(quiz.getId());
        assertEquals(10, saved.size());
        assertEquals("hard", saved.get(0).getDifficulty());
        assertEquals("easy", saved.get(9).getDifficulty());
        for (int i = 1; i < saved.size(); i++) {
            assertTrue(rank(saved.get(i - 1)) <= rank(saved.get(i)));
            assertEquals(i + 1, saved.get(i).getOrder());
        }
        assertTrue(saved.get(0).getCorrectAnswer().equals("true") || saved.get(0).getCorrectAnswer().equals("false"));
    }

    @Test
    void wrongNumberOfQuestionsIsRejected() {
        String nine = TestData.quizJson().replaceFirst("\\{\"question\": \"Statement 1\"[^}]*},", "");
        QuizGenerator generator = new QuizGenerator(TestData.fakeAi(nine), quizDAO);
        assertThrows(AIServiceException.class, () -> generator.parseQuestions(nine));
    }

    @Test
    void invalidAnswerFromAiIsRejected() {
        String bad = TestData.quizJson().replaceFirst("\"True\"", "\"yes\"");
        QuizGenerator generator = new QuizGenerator(TestData.fakeAi(bad), quizDAO);
        assertThrows(AIServiceException.class, () -> generator.parseQuestions(bad));
    }

    @Test
    void notJsonIsRejected() {
        QuizGenerator generator = new QuizGenerator(TestData.fakeAi("sorry"), quizDAO);
        assertThrows(AIServiceException.class, () -> generator.parseQuestions("sorry"));
    }

    @Test
    void answersAreMarkedAndScoreSaved() throws Exception {
        Quiz quiz = new QuizGenerator(TestData.fakeAi(TestData.quizJson()), quizDAO)
                .generate(userId, materialId, "notes.pdf", TestData.pdfBytes());

        // Answer the first 7 correctly and the last 3 wrongly
        List<String> answers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String correct = quiz.getQuestions().get(i).getCorrectAnswer();
            answers.add(i < 7 ? correct.toUpperCase() : (correct.equals("true") ? "false" : "true"));
        }
        ResponseDAO responseDAO = new ResponseDAO(db);
        List<Response> responses = new AnswerEvaluator(quizDAO, responseDAO).evaluate(quiz, answers);

        assertEquals(7, quiz.getScore());
        assertTrue(responses.get(0).isCorrect());
        assertFalse(responses.get(9).isCorrect());
        assertEquals(7, quizDAO.findByUser(userId).get(0).getScore());
        assertEquals(10, responseDAO.findByQuiz(quiz.getId(), quiz.getQuestions()).size());
    }

    @Test
    void invalidAnswerIsNotEvaluated() throws Exception {
        Quiz quiz = new QuizGenerator(TestData.fakeAi(TestData.quizJson()), quizDAO)
                .generate(userId, materialId, "notes.pdf", TestData.pdfBytes());
        List<String> answers = new ArrayList<>(List.of("true", "false", "true", "false", "true",
                "false", "true", "false", "true", "yes"));
        assertThrows(IllegalArgumentException.class,
                () -> new AnswerEvaluator(quizDAO, new ResponseDAO(db)).evaluate(quiz, answers));
    }

    @Test
    void studyPlanUsesWeakTopicsAndIsSaved() throws Exception {
        Quiz quiz = new QuizGenerator(TestData.fakeAi(TestData.quizJson()), quizDAO)
                .generate(userId, materialId, "notes.pdf", TestData.pdfBytes());
        List<String> answers = new ArrayList<>();
        for (Question q : quiz.getQuestions()) {
            answers.add(q.getCorrectAnswer().equals("true") ? "false" : "true");
        }
        List<Response> responses = new AnswerEvaluator(quizDAO, new ResponseDAO(db)).evaluate(quiz, answers);

        Map<String, List<Question>> groups = StudyPlanGenerator.groupMistakesByTopic(responses);
        assertEquals(3, groups.size());
        String summary = StudyPlanGenerator.buildSummary(quiz.getScore(), 10, groups);
        assertTrue(summary.contains("Score: 0/10"));
        assertTrue(summary.contains("Topic 0 (4 mistakes)"));

        StudyPlanDAO planDAO = new StudyPlanDAO(db);
        String plan = new StudyPlanGenerator(TestData.fakeAi("1. Revise topic 0"), planDAO).generate(quiz, responses);
        assertEquals("1. Revise topic 0", plan);
        assertEquals(plan, planDAO.findByQuiz(quiz.getId()).getPlanText());
    }

    private int rank(Question q) {
        return List.of("hard", "medium", "easy").indexOf(q.getDifficulty());
    }
}
