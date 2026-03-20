package com.lendbridge.service;

import com.lendbridge.dto.response.CreditScoreResponse;
import com.lendbridge.entity.User;
import com.lendbridge.enums.EmiStatus;
import com.lendbridge.enums.KycStatus;
import com.lendbridge.enums.LoanStatus;
import com.lendbridge.repository.EmiScheduleRepository;
import com.lendbridge.repository.LoanRequestRepository;
import com.lendbridge.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditScoreService {

    private final UserService userService;
    private final LoanRepository loanRepo;
    private final LoanRequestRepository loanRequestRepo;
    private final EmiScheduleRepository emiRepo;

    public CreditScoreResponse getScore(Long userId) {
        User user = userService.findById(userId);
        List<CreditScoreResponse.ScoreFactor> factors = new ArrayList<>();
        int totalScore = 300; // base score

        // ── Factor 1: KYC Status (max 150 points) ──────────
        int kycScore = 0;
        if (user.getKycStatus() == KycStatus.VERIFIED)       kycScore = 150;
        else if (user.getKycStatus() == KycStatus.PENDING)   kycScore = 30;
        totalScore += kycScore;
        factors.add(CreditScoreResponse.ScoreFactor.builder()
                .label("KYC Verification")
                .value(kycScore).maxValue(150).impact("HIGH")
                .description(user.getKycStatus() == KycStatus.VERIFIED
                        ? "Identity verified via AADHAAR + PAN"
                        : "Complete KYC to gain 150 points")
                .build());

        // ── Factor 2: Income Bracket (max 120 points) ───────
        int incomeScore = switch (user.getIncomeBracket() != null ? user.getIncomeBracket() : "") {
            case "ABOVE_50_LPA"  -> 120;
            case "20_50_LPA"     -> 100;
            case "10_20_LPA"     ->  80;
            case "5_10_LPA"      ->  60;
            case "2_5_LPA"       ->  40;
            case "BELOW_2_LPA"   ->  20;
            default              ->  50;
        };
        totalScore += incomeScore;
        factors.add(CreditScoreResponse.ScoreFactor.builder()
                .label("Income Bracket")
                .value(incomeScore).maxValue(120).impact("HIGH")
                .description("Annual income: " + (user.getIncomeBracket() != null
                        ? user.getIncomeBracket().replace("_", " ") : "Not provided"))
                .build());

        // ── Factor 3: Repayment History (max 200 points) ────
        var loans = loanRepo.findByBorrowerId(userId);
        int repaymentScore = 0;
        if (!loans.isEmpty()) {
            long totalEmis  = loans.stream().flatMap(l ->
                    emiRepo.findByLoanIdOrderByEmiNumber(l.getId()).stream()).count();
            long paidOnTime = loans.stream().flatMap(l ->
                    emiRepo.findByLoanIdOrderByEmiNumber(l.getId()).stream()
                            .filter(e -> e.getStatus() == EmiStatus.PAID)).count();
            long overdue    = loans.stream().flatMap(l ->
                    emiRepo.findByLoanIdOrderByEmiNumber(l.getId()).stream()
                            .filter(e -> e.getStatus() == EmiStatus.OVERDUE)).count();
            if (totalEmis > 0) {
                double ratio = (double) paidOnTime / totalEmis;
                repaymentScore = (int) Math.round(ratio * 200) - (int)(overdue * 10);
                repaymentScore = Math.max(0, Math.min(200, repaymentScore));
            }
        }
        totalScore += repaymentScore;
        factors.add(CreditScoreResponse.ScoreFactor.builder()
                .label("Repayment History")
                .value(repaymentScore).maxValue(200).impact("HIGH")
                .description(loans.isEmpty() ? "No loan history yet" : "Based on EMI payment track record")
                .build());

        // ── Factor 4: P2P Experience (max 80 points) ────────
        int expScore = switch (user.getP2pExperience() != null ? user.getP2pExperience().name() : "") {
            case "ADVANCED"     -> 80;
            case "INTERMEDIATE" -> 50;
            case "BEGINNER"     -> 20;
            default             -> 10;
        };
        totalScore += expScore;
        factors.add(CreditScoreResponse.ScoreFactor.builder()
                .label("P2P Experience")
                .value(expScore).maxValue(80).impact("MEDIUM")
                .description("Platform lending experience level")
                .build());

        // ── Factor 5: Active Loan Count (max 50 points) ─────
        long activeLoans = loanRequestRepo.findByBorrowerId(userId).stream()
                .filter(lr -> lr.getStatus() == LoanStatus.DISBURSED).count();
        int activeScore = activeLoans == 0 ? 50 : activeLoans <= 2 ? 40 : activeLoans <= 4 ? 20 : 5;
        totalScore += activeScore;
        factors.add(CreditScoreResponse.ScoreFactor.builder()
                .label("Active Loans")
                .value(activeScore).maxValue(50).impact("MEDIUM")
                .description(activeLoans + " active loan(s) — fewer is better")
                .build());

        // Clamp between 300 and 900
        totalScore = Math.max(300, Math.min(900, totalScore));

        String grade = totalScore >= 750 ? "EXCELLENT"
                : totalScore >= 650 ? "GOOD"
                : totalScore >= 550 ? "FAIR" : "POOR";

        String recommendation = switch (grade) {
            case "EXCELLENT" -> "Excellent credit profile. Eligible for highest loan amounts and lowest interest rates.";
            case "GOOD"      -> "Good profile. Maintain on-time EMI payments to reach Excellent tier.";
            case "FAIR"      -> "Fair profile. Complete KYC and keep repayments on time to improve score.";
            default          -> "Low score. Focus on KYC completion and clearing any overdue EMIs first.";
        };

        return CreditScoreResponse.builder()
                .userId(userId)
                .userName(user.getFullName())
                .score(totalScore)
                .grade(grade)
                .recommendation(recommendation)
                .factors(factors)
                .lastUpdated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
