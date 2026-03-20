package com.lendbridge.repository;
import com.lendbridge.entity.KycDocument;
import com.lendbridge.enums.DocumentType;
import com.lendbridge.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByUserId(Long userId);
    Optional<KycDocument> findByUserIdAndDocumentType(Long userId, DocumentType type);
    boolean existsByUserIdAndDocumentType(Long userId, DocumentType type);
    long countByStatus(DocumentStatus status);
}
