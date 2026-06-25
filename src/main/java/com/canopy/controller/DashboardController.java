package com.canopy.controller;

import com.canopy.model.User;
import com.canopy.repository.ChatMessageRepository;
import com.canopy.repository.DocumentRepository;
import com.canopy.service.AuthService;
import com.canopy.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AuthService authService;
    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final ChatMessageRepository chatMessageRepository;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = authService.getUserByEmail(userDetails.getUsername());
        var documents = documentService.getUserDocuments(user);
        long totalDocs = documentRepository.countByUser(user);
        long totalQuestions = chatMessageRepository.countByUser(user);
        Long totalDocQuestions = documentRepository.sumQuestionCountByUser(user);

        model.addAttribute("user", user);
        model.addAttribute("documents", documents);
        model.addAttribute("totalDocs", totalDocs);
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("totalDocQuestions", totalDocQuestions != null ? totalDocQuestions : 0);
        return "dashboard/index";
    }
}
