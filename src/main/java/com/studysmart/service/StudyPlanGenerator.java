package com.studysmart.service;

import com.studysmart.db.StudyPlanDAO;
import com.studysmart.model.Question;
import com.studysmart.model.Quiz;
import com.studysmart.model.Response;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StudyPlanGenerator {
    private static final String PLAN_PROMPT =
            "You are a helpful study coach. Using the student's quiz performance summary, write a short, "
            + "personalised study plan of 5 to 8 numbered steps that focuses on their weak topics. "
            + "Explain briefly what to revise for each topic. Use plain text without markdown.";

    private static final String PERFECT_SCORE_PLAN =
            "Great work - you answered every question correctly!\n"
            + "1. Briefly re-read your notes to keep the content fresh.\n"
            + "2. Upload a new section of your notes and try another quiz.\n"
            + "3. Re-test yourself on this material in a few days.";

    private final OpenAIClient ai;
    private final StudyPlanDAO planDAO;

    public StudyPlanGenerator(OpenAIClient ai, StudyPlanDAO planDAO) {
        this.ai = ai;
        this.planDAO = planDAO;
    }

    public String generate(Quiz quiz, List<Response> responses) throws AIServiceException, SQLException {
        Map<String, List<Question>> weakTopics = groupMistakesByTopic(responses);
        String plan;
        if (weakTopics.isEmpty()) {
            plan = PERFECT_SCORE_PLAN;
        } else {
            plan = ai.ask(PLAN_PROMPT, buildSummary(quiz.getScore(), responses.size(), weakTopics), false).trim();
        }
        planDAO.save(quiz.getId(), plan);
        return plan;
    }

    public static Map<String, List<Question>> groupMistakesByTopic(List<Response> responses) {
        Map<String, List<Question>> groups = new LinkedHashMap<>();
        for (Response r : responses) {
            if (!r.isCorrect()) {
                String topic = r.getQuestion().getTopic();
                groups.computeIfAbsent(topic, t -> new ArrayList<>()).add(r.getQuestion());
            }
        }
        return groups;
    }

    public static String buildSummary(int score, int total, Map<String, List<Question>> weakTopics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score: ").append(score).append("/").append(total).append("\n");
        sb.append("Weak topics:\n");
        for (Map.Entry<String, List<Question>> entry : weakTopics.entrySet()) {
            sb.append("- ").append(entry.getKey())
              .append(" (").append(entry.getValue().size()).append(" mistakes)\n");
            for (Question q : entry.getValue()) {
                sb.append("  * \"").append(q.getText()).append("\" is actually ")
                  .append(q.getCorrectAnswer()).append(" (").append(q.getDifficulty()).append(")\n");
            }
        }
        return sb.toString();
    }
}
