package com.canopy.service;

import com.canopy.model.*;
import com.canopy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final GeminiService geminiService;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    @Transactional
    public Document processDocument(MultipartFile file, User user) throws IOException {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String fileType = getFileType(originalName);
        String rawText = extractText(file, fileType);

        Document document = Document.builder()
                .filename(UUID.randomUUID() + "_" + originalName)
                .originalName(originalName)
                .fileType(fileType)
                .fileSize(file.getSize())
                .user(user)
                .build();
        document = documentRepository.save(document);

        // Generate summary
        log.info("Generating summary for document: {}", originalName);
        String summary = geminiService.generateSummary(rawText);
        document.setSummary(summary);

        // Chunk the text
        List<String> chunks = chunkText(rawText);
        document.setChunkCount(chunks.size());
        document = documentRepository.save(document);

        // Embed each chunk
        log.info("Embedding {} chunks...", chunks.size());
        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            float[] embedding = geminiService.generateEmbedding(chunks.get(i));
            DocumentChunk chunk = DocumentChunk.builder()
                    .content(chunks.get(i))
                    .chunkIndex(i)
                    .pageNumber(i / 3 + 1)
                    .embedding(embedding)
                    .document(document)
                    .build();
            chunkEntities.add(chunk);
        }
        chunkRepository.saveAll(chunkEntities);
        log.info("Document processed successfully: {}", originalName);

        return document;
    }

    private String extractText(MultipartFile file, String fileType) throws IOException {
        return switch (fileType) {
            case "PDF" -> extractFromPdf(file);
            case "DOCX" -> extractFromDocx(file);
            case "TXT" -> new String(file.getBytes());
            default -> throw new IllegalArgumentException("Unsupported file type");
        };
    }

    private String extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String extractFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            return sb.toString();
        }
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        int i = 0;
        while (i < words.length) {
            int end = Math.min(i + CHUNK_SIZE, words.length);
            String chunk = String.join(" ", Arrays.copyOfRange(words, i, end));
            if (!chunk.isBlank()) chunks.add(chunk);
            i += CHUNK_SIZE - CHUNK_OVERLAP;
        }
        return chunks;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        String name = file.getOriginalFilename();
        if (name == null) throw new IllegalArgumentException("Invalid file name");
        String ext = name.substring(name.lastIndexOf('.') + 1).toUpperCase();
        if (!Set.of("PDF", "DOCX", "TXT").contains(ext))
            throw new IllegalArgumentException("Only PDF, DOCX, and TXT files are supported");
        if (file.getSize() > 20 * 1024 * 1024)
            throw new IllegalArgumentException("File size must be under 20MB");
    }

    private String getFileType(String filename) {
        if (filename == null) return "UNKNOWN";
        return filename.substring(filename.lastIndexOf('.') + 1).toUpperCase();
    }

    public List<Document> getUserDocuments(User user) {
        return documentRepository.findByUserOrderByUploadedAtDesc(user);
    }

    public Optional<Document> getDocumentByIdAndUser(Long id, User user) {
        return documentRepository.findByIdAndUser(id, user);
    }

    @Transactional
    public void deleteDocument(Long id, User user) {
        documentRepository.findByIdAndUser(id, user).ifPresent(doc -> {
            chunkRepository.deleteByDocument(doc);
            documentRepository.delete(doc);
        });
    }
}
