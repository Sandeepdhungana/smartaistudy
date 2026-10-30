package com.studysmart.service;

import com.studysmart.db.Database;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

class TestData {
    static Database tempDatabase() throws IOException, SQLException {
        File file = File.createTempFile("studysmart-test", ".db");
        file.deleteOnExit();
        return new Database(file.getAbsolutePath());
    }

    // Ten questions in mixed difficulty order, as the AI might return them
    static String quizJson() {
        String[] difficulties = {"easy", "hard", "medium", "easy", "hard", "medium", "medium", "easy", "hard", "medium"};
        StringBuilder sb = new StringBuilder("{\"questions\": [");
        for (int i = 0; i < 10; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"question\": \"Statement ").append(i + 1).append("\", ")
              .append("\"answer\": \"").append(i % 2 == 0 ? "True" : "false").append("\", ")
              .append("\"topic\": \"Topic ").append(i % 3).append("\", ")
              .append("\"difficulty\": \"").append(difficulties[i]).append("\"}");
        }
        return sb.append("]}").toString();
    }

    // Just enough of a PDF to pass the header check; the fake AI never reads it
    static byte[] pdfBytes() {
        return "%PDF-1.4\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    }

    static OpenAIClient fakeAi(String reply) {
        return new OpenAIClient() {
            @Override
            public String ask(String instructions, String content, boolean jsonOutput) {
                return reply;
            }

            @Override
            public String askAboutPdf(String instructions, String fileName, byte[] pdf, boolean jsonOutput) {
                return reply;
            }
        };
    }
}
