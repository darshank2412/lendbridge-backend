package com.lendbridge.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lendbridge.enums.LoanPurpose;
import com.lendbridge.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnoreProperties({"loanRequests", "kycDocuments", "bankAccounts", "password", "authorities"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    @JsonIgnoreProperties({"loanRequests", "lender", "borrower"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanPurpose purpose;

    @Column(length = 500)
    private String purposeDescription;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LoanStatus status = LoanStatus.PENDING;

    private String rejectionReason;

    @JsonIgnoreProperties({"loanRequests", "kycDocuments", "bankAccounts", "password", "authorities"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "accepted_by_lender_id")
    private User acceptedByLender;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}