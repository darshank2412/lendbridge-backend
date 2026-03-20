package com.lendbridge.service;

import com.lendbridge.dto.request.DepositWithdrawRequest;
import com.lendbridge.dto.request.OpenAccountRequest;
import com.lendbridge.dto.response.BankAccountResponse;
import com.lendbridge.entity.*;
import com.lendbridge.enums.AccountType;
import com.lendbridge.enums.UserStatus;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.BankAccountRepository;
import com.lendbridge.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository accountRepo;
    private final UserService userService;
    private final SavingsProductService savingsProductService;
    private final LoanProductService loanProductService;
    private final EntityMapper mapper;

    public BankAccountResponse getById(Long accountId) {
        return mapper.toBankAccountResponse(findById(accountId));
    }

    public List<BankAccountResponse> getByUser(Long userId) {
        return accountRepo.findByUserId(userId).stream()
                .map(mapper::toBankAccountResponse).toList();
    }

    @Transactional
    public BankAccountResponse openSavings(OpenAccountRequest req) {
        User user = userService.findById(req.getUserId());
        if (accountRepo.existsByUserIdAndAccountType(req.getUserId(), AccountType.SAVINGS)) {
            throw LendbridgeException.conflict("User already has a savings account.");
        }
        SavingsProduct product = savingsProductService.findById(req.getProductId());
        BankAccount account = BankAccount.builder()
                .accountNumber(generateAccountNumber("SAV"))
                .accountType(AccountType.SAVINGS)
                .user(user)
                .savingsProduct(product)
                .build();

        // Update user status
        if (user.getStatus() == UserStatus.REGISTRATION_COMPLETE) {
            user.setStatus(UserStatus.PLATFORM_ACCOUNT_CREATED);
        }
        return mapper.toBankAccountResponse(accountRepo.save(account));
    }

    @Transactional
    public BankAccountResponse openLoan(OpenAccountRequest req) {
        User user = userService.findById(req.getUserId());
        if (accountRepo.existsByUserIdAndAccountType(req.getUserId(), AccountType.LOAN)) {
            throw LendbridgeException.conflict("User already has a loan account.");
        }
        LoanProduct product = loanProductService.findById(req.getProductId());
        BankAccount account = BankAccount.builder()
                .accountNumber(generateAccountNumber("LON"))
                .accountType(AccountType.LOAN)
                .user(user)
                .loanProduct(product)
                .build();
        return mapper.toBankAccountResponse(accountRepo.save(account));
    }

    @Transactional
    public BankAccountResponse deposit(Long accountId, DepositWithdrawRequest req) {
        BankAccount account = findById(accountId);
        if (account.getAccountType() != AccountType.SAVINGS) {
            throw LendbridgeException.badRequest("Deposits only allowed on savings accounts");
        }
        BigDecimal newBalance = account.getBalance().add(req.getAmount());
        // Check max balance
        if (account.getSavingsProduct() != null
                && newBalance.compareTo(account.getSavingsProduct().getMaxBalance()) > 0) {
            throw LendbridgeException.badRequest("Deposit exceeds maximum balance limit of ₹"
                    + account.getSavingsProduct().getMaxBalance());
        }
        account.setBalance(newBalance);
        return mapper.toBankAccountResponse(accountRepo.save(account));
    }

    @Transactional
    public BankAccountResponse withdraw(Long accountId, DepositWithdrawRequest req) {
        BankAccount account = findById(accountId);
        if (account.getAccountType() != AccountType.SAVINGS) {
            throw LendbridgeException.badRequest("Withdrawals only allowed on savings accounts");
        }
        if (account.getBalance().compareTo(req.getAmount()) < 0) {
            throw LendbridgeException.badRequest("Insufficient balance");
        }
        BigDecimal minBalance = account.getSavingsProduct() != null
                ? account.getSavingsProduct().getMinBalance() : BigDecimal.ZERO;
        BigDecimal afterWithdraw = account.getBalance().subtract(req.getAmount());
        if (afterWithdraw.compareTo(minBalance) < 0) {
            throw LendbridgeException.badRequest(
                    "Cannot withdraw — minimum balance of ₹" + minBalance + " must be maintained");
        }
        account.setBalance(afterWithdraw);
        return mapper.toBankAccountResponse(accountRepo.save(account));
    }

    public BankAccount findById(Long id) {
        return accountRepo.findById(id)
                .orElseThrow(() -> LendbridgeException.notFound("Bank account not found"));
    }

    private String generateAccountNumber(String prefix) {
        return prefix + System.currentTimeMillis() % 100_000_000;
    }
}
