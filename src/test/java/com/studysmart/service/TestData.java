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
        return quizJson(1, 10);
    }

    // `count` questions numbered from `first`, cycling through a mix of difficulties
    static String quizJson(int first, int count) {
        String[] difficulties = {"easy", "hard", "medium", "easy", "hard", "medium", "medium", "easy", "hard", "medium"};
        StringBuilder sb = new StringBuilder("{\"questions\": [");
        for (int n = 0; n < count; n++) {
            int i = first - 1 + n;
            if (n > 0) {
                sb.append(",");
            }
            sb.append("{\"question\": \"Statement ").append(i + 1).append("\", ")
              .append("\"answer\": \"").append(i % 2 == 0 ? "True" : "false").append("\", ")
              .append("\"topic\": \"Topic ").append(i % 3).append("\", ")
              .append("\"difficulty\": \"").append(difficulties[i % difficulties.length]).append("\"}");
        }
        return sb.append("]}").toString();
    }

    // Just enough of a PDF to pass the header check; the fake AI never reads it
    static byte[] pdfBytes() {
        return "%PDF-1.4\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    }

    // Gives each reply in turn, then keeps repeating the last one
    static OpenAIClient fakeAi(String... replies) {
        return new OpenAIClient() {
            private int calls = 0;

            private String next() {
                return replies[Math.min(calls++, replies.length - 1)];
            }

            @Override
            public String ask(String instructions, String content, boolean jsonOutput) {
                return next();
            }

            @Override
            public String askAboutPdf(String instructions, String fileName, byte[] pdf, boolean jsonOutput) {
                return next();
            }
        };
    }
}
