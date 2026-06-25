package com.canopy.service;

import com.canopy.model.*;
import com.canopy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final GeminiService geminiService;
    private final DocumentChunkRepository chunkRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentRepository documentRepository;

    private static final int TOP_K = 4;

    @Transactional
    public Map<String, Object> askQuestion(Document document, User user, String question) {
        // 1. Embed the question
        float[] queryEmbedding = geminiService.generateEmbedding(question);
        String embeddingStr = floatArrayToPostgresArray(queryEmbedding);

        // 2. Retrieve top-K similar chunks
        List<DocumentChunk> relevantChunks = chunkRepository
                .findTopKByCosineSimilarity(document.getId(), embeddingStr, TOP_K);

        if (relevantChunks.isEmpty()) {
            return Map.of("answer", "I couldn't find relevant information in this document for your question.",
                          "sources", List.of());
        }

        // 3. Build context from chunks
        String context = relevantChunks.stream()
                .map(c -> "[Page " + c.getPageNumber() + "]: " + c.getContent())
                .collect(Collectors.joining("\n\n"));

        // 4. Build prompt
        String systemPrompt = """
            You are Canopy, an intelligent document assistant.
            Your job is to answer questions based ONLY on the provided document context.
            Always be accurate, concise, and cite the page numbers when possible.
            If the answer is not in the context, clearly state that.
            Format your response in clear paragraphs.
            """;

        String userPrompt = """
            Document: %s
            
            Context from document:
            %s
            
            Question: %s
            
            Please provide a clear, accurate answer based on the document context above.
            """.formatted(document.getOriginalName(), context, question);

        // 5. Generate answer
        String answer = geminiService.chat(systemPrompt, userPrompt);

        // 6. Build source citations
        List<Map<String, Object>> sources = relevantChunks.stream()
                .map(c -> Map.<String, Object>of(
                        "page", c.getPageNumber(),
                        "preview", c.getContent().substring(0, Math.min(c.getContent().length(), 120)) + "..."
                ))
                .toList();

        // 7. Save chat message
        ChatMessage msg = ChatMessage.builder()
                .question(question)
                .answer(answer)
                .sources(sources.toString())
                .document(document)
                .user(user)
                .build();
        chatMessageRepository.save(msg);

        // 8. Update question count
        document.setQuestionCount(document.getQuestionCount() + 1);
        documentRepository.save(document);

        return Map.of("answer", answer, "sources", sources);
    }

    public List<ChatMessage> getChatHistory(Document document, User user) {
        return chatMessageRepository.findByDocumentAndUserOrderByCreatedAtAsc(document, user);
    }

    private String floatArrayToPostgresArray(float[] arr) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }
}
