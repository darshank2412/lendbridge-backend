package com.lendbridge.controller;

import com.lendbridge.dto.request.SavingsProductRequest;
import com.lendbridge.dto.response.ApiResponse;
import com.lendbridge.dto.response.SavingsProductResponse;
import com.lendbridge.service.SavingsProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/savings-products")
@RequiredArgsConstructor
@Tag(name = "Savings Product APIs", description = "Savings product CRUD")
public class SavingsProductController {
    private final SavingsProductService service;

    @GetMapping
    @Operation(summary = "List all active savings products")
    public ResponseEntity<ApiResponse<List<SavingsProductResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get savings product by ID")
    public ResponseEntity<ApiResponse<SavingsProductResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create savings product (ADMIN only)")
    public ResponseEntity<ApiResponse<SavingsProductResponse>> create(@Valid @RequestBody SavingsProductRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.create(req), "Savings product created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update savings product (ADMIN only)")
    public ResponseEntity<ApiResponse<SavingsProductResponse>> update(@PathVariable Long id, @Valid @RequestBody SavingsProductRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req), "Savings product updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate savings product (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Savings product deactivated"));
    }
}
