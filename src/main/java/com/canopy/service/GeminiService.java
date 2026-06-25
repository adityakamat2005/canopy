package com.canopy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.chat.url}")
    private String chatApiUrl;

    @Value("${gemini.embedding.url}")
    private String embeddingApiUrl;

    @Value("${gemini.model}")
    private String chatModel;

    @Value("${gemini.embedding.model}")
    private String embeddingModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiService(WebClient.Builder builder, ObjectMapper objectMapper) {
        this.webClient = builder
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
    }

    public float[] generateEmbedding(String text) {
        try {
            String url = embeddingApiUrl + "/models/" + embeddingModel + ":embedContent?key=" + apiKey;

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "models/" + embeddingModel);
            body.put("content", Map.of("parts", List.of(Map.of("text", text))));

            String response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode values = root.path("embedding").path("values");
            float[] embedding = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                embedding[i] = (float) values.get(i).asDouble();
            }
            log.debug("Generated embedding with {} dimensions", embedding.length);
            return embedding;
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage());
            return new float[768];
        }
    }

    public String chat(String systemPrompt, String userMessage) {
        try {
            String url = chatApiUrl + "/models/" + chatModel + ":generateContent?key=" + apiKey;

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
            body.put("contents", List.of(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userMessage))
            )));
            body.put("generationConfig", Map.of(
                    "temperature", 0.3,
                    "maxOutputTokens", 1024
            ));

            String response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            log.error("Error calling Gemini chat: {}", e.getMessage());
            return "I encountered an error processing your request. Please try again.";
        }
    }

    public String generateSummary(String text) {
        String truncated = text.length() > 3000 ? text.substring(0, 3000) : text;
        String prompt = """
            You are a document summarizer. Analyze the following document and provide a concise summary with exactly 5 bullet points.
            Each bullet point should capture a key insight or main topic from the document.
            Format: Start each bullet with "• "
            Keep each bullet to 1-2 sentences.
            
            Document text:
            """ + truncated;

        return chat("You are a professional document analyst.", prompt);
    }

    public List<String> generateSuggestedQuestions(String summary) {
        String prompt = """
            Based on this document summary, generate exactly 3 insightful questions a user might want to ask.
            Format: Return only the 3 questions, one per line, numbered 1. 2. 3.
            Do NOT include any other text.
            
            Summary: """ + summary;

        String response = chat("You generate smart questions about documents.", prompt);
        List<String> questions = new ArrayList<>();
        for (String line : response.split("\n")) {
            String cleaned = line.replaceAll("^[0-9]+\\.\\s*", "").trim();
            if (!cleaned.isEmpty() && cleaned.length() > 10) {
                questions.add(cleaned);
            }
        }
        return questions.stream().limit(3).toList();
    }
}