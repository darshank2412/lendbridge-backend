package com.lendbridge.controller;

import com.lendbridge.dto.request.EarlyClosureRequest;
import com.lendbridge.dto.request.PartialRepaymentRequest;
import com.lendbridge.dto.response.*;
import com.lendbridge.service.CreditScoreService;
import com.lendbridge.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.lendbridge.service.LoanAgreementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Loan Disbursement & EMI APIs", description = "Disbursement, EMI schedule, repayments (Weeks 5+7)")
public class LoanController {

    private final LoanService loanService;
    private final CreditScoreService creditScoreService;
private final LoanAgreementService loanAgreementService;

    // ── Week 5: Disbursement ──────────────────────────────

    @PostMapping("/disbursements/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ADMIN — Disburse an accepted loan request")
    public ResponseEntity<ApiResponse<LoanRequestResponse>> disburse(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(
                loanService.disburse(requestId), "Loan disbursed successfully"));
    }

    // ── Week 5: EMI Schedule ──────────────────────────────

    @GetMapping("/emi-schedule/{loanId}")
    @Operation(summary = "Get full EMI amortization schedule for a loan")
    public ResponseEntity<ApiResponse<List<EmiScheduleResponse>>> getEmiSchedule(
            @PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getEmiSchedule(loanId)));
    }

    @GetMapping("/loans/{loanId}/summary")
    @Operation(summary = "Get loan summary — EMIs paid, outstanding, overdue")
    public ResponseEntity<ApiResponse<LoanSummaryResponse>> getLoanSummary(
            @PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getLoanSummary(loanId)));
    }

    @GetMapping("/loans/active")
    @Operation(summary = "BORROWER — Get all active (non-closed) loans")
    public ResponseEntity<ApiResponse<List<LoanSummaryResponse>>> getActiveLoans(
            @RequestParam Long borrowerId) {
        return ResponseEntity.ok(ApiResponse.success(
                loanService.getActiveLoansByBorrower(borrowerId)));
    }

    // ── Week 5: Pay EMI ───────────────────────────────────

    @PostMapping("/repayments/{loanId}/pay-emi")
    @Operation(summary = "BORROWER — Pay next EMI instalment")
    public ResponseEntity<ApiResponse<EmiScheduleResponse>> payEmi(
            @PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(
                loanService.payNextEmi(loanId), "EMI paid successfully"));
    }

    // ── Week 7: Partial Repayment ─────────────────────────

    @PostMapping("/repayments/{loanId}/partial")
    @Operation(summary = "BORROWER — Make a partial repayment")
    public ResponseEntity<ApiResponse<LoanSummaryResponse>> partialRepayment(
            @PathVariable Long loanId,
            @Valid @RequestBody PartialRepaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                loanService.partialRepayment(loanId, req), "Partial repayment successful"));
    }

    // ── Week 7: Early Closure ─────────────────────────────

    @GetMapping("/repayments/{loanId}/early-close-quote")
    @Operation(summary = "BORROWER — Get early closure penalty quote")
    public ResponseEntity<ApiResponse<EarlyClosureQuoteResponse>> getEarlyClosureQuote(
            @PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getEarlyClosureQuote(loanId)));
    }

    @PostMapping("/repayments/{loanId}/early-close")
    @Operation(summary = "BORROWER — Close loan early with penalty")
    public ResponseEntity<ApiResponse<LoanSummaryResponse>> earlyClose(
            @PathVariable Long loanId,
            @RequestBody(required = false) EarlyClosureRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                loanService.earlyClose(loanId), "Loan closed early successfully"));
    }

    // ── Week 7: Credit Score ──────────────────────────────

    @GetMapping("/credit-score/{userId}")
    @Operation(summary = "Get credit score for a user")
    public ResponseEntity<ApiResponse<CreditScoreResponse>> getCreditScore(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(creditScoreService.getScore(userId)));
    }

@GetMapping("/loans/{loanId}/agreement")
@Operation(summary = "Download loan agreement PDF")
public ResponseEntity<byte[]> downloadAgreement(@PathVariable Long loanId) throws Exception {
    byte[] pdf = loanAgreementService.generateAgreement(loanId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=LoanAgreement-" + loanId + ".pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
}
}
