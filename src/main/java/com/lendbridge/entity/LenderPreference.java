package com.lendbridge.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lendbridge.enums.RiskAppetite;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lender_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"lender_id", "loan_product_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LenderPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnoreProperties({"lenderPreferences", "kycDocuments", "bankAccounts", "loanRequests", "password", "authorities"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lender_id", nullable = false)
    private User lender;

    @JsonIgnoreProperties({"lenderPreferences", "loanRequests", "bankAccounts"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal minInterestRate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal maxInterestRate;

    @Column(nullable = false)
    private Integer minTenureMonths;

    @Column(nullable = false)
    private Integer maxTenureMonths;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minLoanAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maxLoanAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskAppetite riskAppetite;

    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}