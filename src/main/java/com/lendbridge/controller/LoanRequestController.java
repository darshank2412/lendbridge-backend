package com.lendbridge.controller;

import com.lendbridge.dto.request.LoanRequestDto;
import com.lendbridge.dto.response.ApiResponse;
import com.lendbridge.dto.response.LoanRequestResponse;
import com.lendbridge.enums.LoanStatus;
import com.lendbridge.service.LoanRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loan-requests")
@RequiredArgsConstructor
@Tag(name = "Loan Request APIs", description = "Borrower submits loan requests; Lender discovers and accepts")
public class LoanRequestController {

    private final LoanRequestService service;

    // ── BORROWER ─────────────────────────────────────────

    @PostMapping
    @Operation(summary = "BORROWER — Submit a loan request")
    public ResponseEntity<ApiResponse<LoanRequestResponse>> create(
            @RequestParam Long borrowerId,
            @Valid @RequestBody LoanRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.success(
                service.create(borrowerId, dto), "Loan request submitted"));
    }

    @GetMapping("/my")
    @Operation(summary = "BORROWER — View my loan requests")
    public ResponseEntity<ApiResponse<List<LoanRequestResponse>>> getMyRequests(
            @RequestParam Long borrowerId) {
        return ResponseEntity.ok(ApiResponse.success(service.getMyRequests(borrowerId)));
    }

    @PatchMapping("/{requestId}/cancel")
    @Operation(summary = "BORROWER — Cancel a pending loan request")
    public ResponseEntity<ApiResponse<LoanRequestResponse>> cancel(
            @PathVariable Long requestId,
            @RequestParam Long borrowerId) {
        return ResponseEntity.ok(ApiResponse.success(
                service.cancel(requestId, borrowerId), "Loan request cancelled"));
    }

    // ── LENDER ───────────────────────────────────────────

    @GetMapping("/open")
    @Operation(summary = "LENDER — Browse all open (PENDING) loan requests")
    public ResponseEntity<ApiResponse<List<LoanRequestResponse>>> getOpenRequests(
            @RequestParam Long lenderId) {
        return ResponseEntity.ok(ApiResponse.success(service.getOpenRequests(lenderId)));
    }

    @GetMapping("/open/matching")
    @Operation(summary = "LENDER — Browse requests matching my preferences")
    public ResponseEntity<ApiResponse<List<LoanRequestResponse>>> getMatchingRequests(
            @RequestParam Long lenderId) {
        return ResponseEntity.ok(ApiResponse.success(service.getMatchingRequests(lenderId)));
    }

    @GetMapping("/matched")
    @Operation(summary = "LENDER — Get all matched loan requests")
    public ResponseEntity<ApiResponse<List<LoanRequestResponse>>> getMatchedRequests() {
        return ResponseEntity.ok(ApiResponse.success(service.getMatchedRequests()));
    }

    @PatchMapping("/{requestId}/accept")
    @Operation(summary = "LENDER — Accept a matched loan request")
    public ResponseEntity<ApiResponse<LoanRequestResponse>> accept(
            @PathVariable Long requestId,
            @RequestParam Long lenderId) {
        return ResponseEntity.ok(ApiResponse.success(
                service.accept(requestId, lenderId), "Loan request accepted"));
    }

    // ── ADMIN ─────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ADMIN — Get all loan requests (filter by status)")
    public ResponseEntity<ApiResponse<List<LoanRequestResponse>>> getAll(
            @RequestParam(required = false) LoanStatus status) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll(status)));
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ADMIN — Get loan request by ID")
    public ResponseEntity<ApiResponse<LoanRequestResponse>> getById(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(requestId)));
    }

    @PatchMapping("/{requestId}/match")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ADMIN — Mark loan request as matched")
    public ResponseEntity<ApiResponse<LoanRequestResponse>> match(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(
                service.match(requestId), "Loan request marked as matched"));
    }

    @PatchMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ADMIN — Reject a loan request")
    public ResponseEntity<ApiResponse<LoanRequestResponse>> reject(
            @PathVariable Long requestId,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(
                service.reject(requestId, reason), "Loan request rejected"));
    }
}
