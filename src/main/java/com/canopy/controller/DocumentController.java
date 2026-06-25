package com.canopy.controller;

import com.canopy.model.*;
import com.canopy.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;
    private final RagService ragService;
    private final AuthService authService;
    private final GeminiService geminiService;

    @GetMapping("/upload")
    public String uploadPage() {
        return "dashboard/upload";
    }

    @PostMapping("/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = authService.getUserByEmail(userDetails.getUsername());
            Document doc = documentService.processDocument(file, user);
            redirectAttributes.addFlashAttribute("success",
                "\"" + doc.getOriginalName() + "\" uploaded and indexed successfully!");
            return "redirect:/chat/" + doc.getId();
        } catch (Exception e) {
            log.error("Upload error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
            return "redirect:/upload";
        }
    }

    @GetMapping("/chat/{docId}")
    public String chatPage(@PathVariable Long docId,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model) {
        User user = authService.getUserByEmail(userDetails.getUsername());
        return documentService.getDocumentByIdAndUser(docId, user).map(doc -> {
            List<ChatMessage> history = ragService.getChatHistory(doc, user);
            List<String> suggestions = geminiService.generateSuggestedQuestions(
                doc.getSummary() != null ? doc.getSummary() : doc.getOriginalName());
            model.addAttribute("document", doc);
            model.addAttribute("history", history);
            model.addAttribute("suggestions", suggestions);
            model.addAttribute("user", user);
            return "chat/index";
        }).orElse("redirect:/dashboard");
    }

    @PostMapping("/api/ask")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> askQuestion(
            @RequestParam Long documentId,
            @RequestParam String question,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = authService.getUserByEmail(userDetails.getUsername());
            Document doc = documentService.getDocumentByIdAndUser(documentId, user)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            Map<String, Object> result = ragService.askQuestion(doc, user, question);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Ask error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "answer", "Error processing your question: " + e.getMessage(),
                "sources", List.of()
            ));
        }
    }

    @PostMapping("/delete/{docId}")
    public String deleteDocument(@PathVariable Long docId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        User user = authService.getUserByEmail(userDetails.getUsername());
        documentService.deleteDocument(docId, user);
        redirectAttributes.addFlashAttribute("success", "Document deleted successfully.");
        return "redirect:/dashboard";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }
}
