package com.studysmart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

public class OpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey = System.getenv("OPENAI_API_KEY");
    private final String model;

    public OpenAIClient() {
        String envModel = System.getenv("OPENAI_MODEL");
        model = (envModel == null || envModel.isBlank()) ? DEFAULT_MODEL : envModel;
    }

    public String ask(String instructions, String content, boolean jsonOutput) throws AIServiceException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AIServiceException("No OpenAI API key found. Set the OPENAI_API_KEY environment variable and restart the app.");
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", instructions);
        messages.addObject().put("role", "user").put("content", content);
        if (jsonOutput) {
            body.putObject("response_format").put("type", "json_object");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new AIServiceException("The OpenAI API did not respond in time. Please try again.");
        } catch (IOException e) {
            throw new AIServiceException("Could not connect to the OpenAI API. Check your internet connection.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIServiceException("The request to OpenAI was cancelled.");
        }

        try {
            JsonNode json = mapper.readTree(response.body());
            if (response.statusCode() != 200) {
                String reason = json.path("error").path("message").asText("unknown error");
                throw new AIServiceException("OpenAI returned an error (" + response.statusCode() + "): " + reason);
            }
            String reply = json.path("choices").path(0).path("message").path("content").asText("");
            if (reply.isBlank()) {
                throw new AIServiceException("OpenAI returned an empty reply. Please try again.");
            }
            return reply;
        } catch (IOException e) {
            throw new AIServiceException("OpenAI returned a reply that could not be read.");
        }
    }
}
