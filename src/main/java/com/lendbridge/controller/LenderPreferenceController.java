package com.lendbridge.controller;

import com.lendbridge.dto.request.LenderPreferenceDto;
import com.lendbridge.dto.response.ApiResponse;
import com.lendbridge.dto.response.LenderPreferenceResponse;
import com.lendbridge.service.LenderPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lender-preferences")
@RequiredArgsConstructor
@Tag(name = "Lender Preference APIs", description = "Lender sets preferences per loan product for matchmaking")
public class LenderPreferenceController {

    private final LenderPreferenceService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ADMIN — Get all active lender preferences")
    public ResponseEntity<ApiResponse<List<LenderPreferenceResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success(service.getAllActive()));
    }

    @GetMapping("/my")
    @Operation(summary = "LENDER — Get all my preferences (one per loan product)")
    public ResponseEntity<ApiResponse<List<LenderPreferenceResponse>>> getMyPreferences(
            @RequestParam Long lenderId) {
        return ResponseEntity.ok(ApiResponse.success(service.getMyPreferences(lenderId)));
    }

    @PostMapping
    @Operation(summary = "LENDER — Save or update preference for a loan product")
    public ResponseEntity<ApiResponse<LenderPreferenceResponse>> savePreference(
            @RequestParam Long lenderId,
            @Valid @RequestBody LenderPreferenceDto dto) {
        return ResponseEntity.ok(ApiResponse.success(
                service.savePreference(lenderId, dto), "Preference saved"));
    }

    @PatchMapping("/deactivate")
    @Operation(summary = "LENDER — Deactivate preference for a specific loan product")
    public ResponseEntity<ApiResponse<LenderPreferenceResponse>> deactivate(
            @RequestParam Long lenderId,
            @RequestParam Long loanProductId) {
        return ResponseEntity.ok(ApiResponse.success(
                service.deactivate(lenderId, loanProductId), "Preference deactivated"));
    }
}
