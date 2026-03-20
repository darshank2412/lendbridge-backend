package com.lendbridge.controller;

import com.lendbridge.dto.request.LoanProductRequest;
import com.lendbridge.dto.response.ApiResponse;
import com.lendbridge.dto.response.LoanProductResponse;
import com.lendbridge.service.LoanProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/loan-products")
@RequiredArgsConstructor
@Tag(name = "Loan Product APIs", description = "Loan product CRUD")
public class LoanProductController {
    private final LoanProductService service;

    @GetMapping
    @Operation(summary = "List all active loan products")
    public ResponseEntity<ApiResponse<List<LoanProductResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan product by ID")
    public ResponseEntity<ApiResponse<LoanProductResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create loan product (ADMIN only)")
    public ResponseEntity<ApiResponse<LoanProductResponse>> create(@Valid @RequestBody LoanProductRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.create(req), "Loan product created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update loan product (ADMIN only)")
    public ResponseEntity<ApiResponse<LoanProductResponse>> update(@PathVariable Long id, @Valid @RequestBody LoanProductRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req), "Loan product updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate loan product (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Loan product deactivated"));
    }
}
