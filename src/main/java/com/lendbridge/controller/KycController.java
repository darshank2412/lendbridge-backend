package com.lendbridge.controller;

import com.lendbridge.dto.request.KycSubmitRequest;
import com.lendbridge.dto.response.ApiResponse;
import com.lendbridge.entity.KycDocument;
import com.lendbridge.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC APIs", description = "KYC document submission and verification")
@SecurityRequirement(name = "bearerAuth")
public class KycController {

    private final KycService kycService;

    @PostMapping("/submit")
    @Operation(summary = "Submit a KYC document",
               description = "Submit AADHAAR, PAN, PASSPORT, DRIVING_LICENSE, or VOTER_ID. Both AADHAAR and PAN required for full KYC approval.")
    public ResponseEntity<ApiResponse<KycDocument>> submitDocument(
            @RequestParam Long userId,
            @Valid @RequestBody KycSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                kycService.submitDocument(userId, request),
                "Document submitted successfully"));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get KYC documents for a user")
    public ResponseEntity<ApiResponse<List<KycDocument>>> getDocuments(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(kycService.getDocuments(userId)));
    }

    @PatchMapping("/approve/{docId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Approve a KYC document")
    public ResponseEntity<ApiResponse<KycDocument>> approveDocument(@PathVariable Long docId) {
        return ResponseEntity.ok(ApiResponse.success(
                kycService.approveDocument(docId), "Document approved"));
    }

    @PatchMapping("/reject/{docId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Reject a KYC document")
    public ResponseEntity<ApiResponse<KycDocument>> rejectDocument(
            @PathVariable Long docId,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(
                kycService.rejectDocument(docId, reason), "Document rejected"));
    }
}
