package com.canopy.repository;

import com.canopy.model.ChatMessage;
import com.canopy.model.Document;
import com.canopy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByDocumentAndUserOrderByCreatedAtAsc(Document document, User user);
    List<ChatMessage> findByUserOrderByCreatedAtDesc(User user);
    long countByUser(User user);
}
