package com.lendbridge.entity;

import com.lendbridge.enums.DocumentStatus;
import com.lendbridge.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_documents",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "document_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String documentNumber;

    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    private String rejectionNote;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() { submittedAt = LocalDateTime.now(); }
}
