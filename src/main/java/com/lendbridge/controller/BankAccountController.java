package com.lendbridge.controller;

import com.lendbridge.dto.request.DepositWithdrawRequest;
import com.lendbridge.dto.request.OpenAccountRequest;
import com.lendbridge.dto.response.ApiResponse;
import com.lendbridge.dto.response.BankAccountResponse;
import com.lendbridge.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Bank Account APIs", description = "Open savings/loan accounts linked to products, deposit and withdraw")
public class BankAccountController {

    private final BankAccountService service;

    @PostMapping("/savings")
    @Operation(summary = "Open savings account",
               description = "Links account to a SavingsProduct by productId. One savings account per user.")
    public ResponseEntity<ApiResponse<BankAccountResponse>> openSavings(
            @Valid @RequestBody OpenAccountRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.openSavings(req), "Savings account opened"));
    }

    @PostMapping("/loan")
    @Operation(summary = "Open loan account",
               description = "Links account to a LoanProduct by productId. One loan account per user.")
    public ResponseEntity<ApiResponse<BankAccountResponse>> openLoan(
            @Valid @RequestBody OpenAccountRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.openLoan(req), "Loan account opened"));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<ApiResponse<BankAccountResponse>> getById(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(accountId)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all accounts for a user")
    public ResponseEntity<ApiResponse<List<BankAccountResponse>>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(service.getByUser(userId)));
    }

    @PostMapping("/{accountId}/deposit")
    @Operation(summary = "Deposit into savings account")
    public ResponseEntity<ApiResponse<BankAccountResponse>> deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody DepositWithdrawRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.deposit(accountId, req), "Deposit successful"));
    }

    @PostMapping("/{accountId}/withdraw")
    @Operation(summary = "Withdraw from savings account")
    public ResponseEntity<ApiResponse<BankAccountResponse>> withdraw(
            @PathVariable Long accountId,
            @Valid @RequestBody DepositWithdrawRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.withdraw(accountId, req), "Withdrawal successful"));
    }
}
