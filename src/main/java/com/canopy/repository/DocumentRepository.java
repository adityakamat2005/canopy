package com.canopy.repository;

import com.canopy.model.Document;
import com.canopy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserOrderByUploadedAtDesc(User user);
    Optional<Document> findByIdAndUser(Long id, User user);
    long countByUser(User user);

    @Query("SELECT SUM(d.questionCount) FROM Document d WHERE d.user = :user")
    Long sumQuestionCountByUser(User user);
}
