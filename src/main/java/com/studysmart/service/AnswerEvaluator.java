package com.studysmart.service;

import com.studysmart.db.QuizDAO;
import com.studysmart.db.ResponseDAO;
import com.studysmart.model.Question;
import com.studysmart.model.Quiz;
import com.studysmart.model.Response;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AnswerEvaluator {
    private final QuizDAO quizDAO;
    private final ResponseDAO responseDAO;

    public AnswerEvaluator(QuizDAO quizDAO, ResponseDAO responseDAO) {
        this.quizDAO = quizDAO;
        this.responseDAO = responseDAO;
    }

    public List<Response> evaluate(Quiz quiz, List<String> answers) throws SQLException {
        List<Question> questions = quiz.getQuestions();
        if (answers.size() != questions.size()) {
            throw new IllegalArgumentException("Please answer every question.");
        }

        int score = 0;
        ArrayList<Response> responses = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            if (!AnswerValidator.isValid(answers.get(i))) {
                throw new IllegalArgumentException(AnswerValidator.WARNING);
            }
            Question question = questions.get(i);
            String answer = AnswerValidator.normalise(answers.get(i));
            boolean correct = answer.equals(question.getCorrectAnswer());
            if (correct) {
                score = score + 1;
            }
            responses.add(new Response(question, answer, correct));
        }

        responseDAO.saveAll(quiz.getId(), responses);
        quizDAO.updateScore(quiz.getId(), score);
        quiz.setScore(score);
        return responses;
    }
}
