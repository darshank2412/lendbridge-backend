package com.lendbridge.util;

import com.lendbridge.dto.request.AddressDto;
import com.lendbridge.dto.response.*;
import com.lendbridge.entity.*;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    public UserResponse toUserResponse(User user) {
        AddressDto addressDto = null;
        if (user.getAddress() != null) {
            Address a = user.getAddress();
            addressDto = new AddressDto(a.getLine1(), a.getCity(), a.getState(), a.getPincode());
        }
        return UserResponse.builder()
                .id(user.getId())
                .countryCode(user.getCountryCode())
                .mobileNumber(user.getPhoneNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .role(user.getRole())
                .status(user.getStatus())
                .kycStatus(user.getKycStatus())
                .incomeBracket(user.getIncomeBracket())
                .p2pExperience(user.getP2pExperience())
                .address(addressDto)
                .platformAccountNumber(user.getPlatformAccountNumber())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public SavingsProductResponse toSavingsProductResponse(SavingsProduct p) {
        return SavingsProductResponse.builder()
                .id(p.getId()).name(p.getName())
                .minBalance(p.getMinBalance()).maxBalance(p.getMaxBalance())
                .interestRate(p.getInterestRate()).status(p.getStatus())
                .createdAt(p.getCreatedAt()).build();
    }

    public LoanProductResponse toLoanProductResponse(LoanProduct p) {
        return LoanProductResponse.builder()
                .id(p.getId()).name(p.getName())
                .minAmount(p.getMinAmount()).maxAmount(p.getMaxAmount())
                .minInterest(p.getMinInterest()).maxInterest(p.getMaxInterest())
                .minTenure(p.getMinTenure()).maxTenure(p.getMaxTenure())
                .status(p.getStatus()).createdAt(p.getCreatedAt()).build();
    }

    public BankAccountResponse toBankAccountResponse(BankAccount a) {
        return BankAccountResponse.builder()
                .id(a.getId()).accountNumber(a.getAccountNumber())
                .accountType(a.getAccountType()).balance(a.getBalance())
                .status(a.getStatus()).userId(a.getUser().getId())
                .savingsProductId(a.getSavingsProduct() != null ? a.getSavingsProduct().getId() : null)
                .savingsProductName(a.getSavingsProduct() != null ? a.getSavingsProduct().getName() : null)
                .loanProductId(a.getLoanProduct() != null ? a.getLoanProduct().getId() : null)
                .loanProductName(a.getLoanProduct() != null ? a.getLoanProduct().getName() : null)
                .createdAt(a.getCreatedAt()).build();
    }

    public LenderPreferenceResponse toLenderPreferenceResponse(LenderPreference p) {
        return LenderPreferenceResponse.builder()
                .id(p.getId())
                .lenderId(p.getLender().getId())
                .lenderName(p.getLender().getFullName())
                .loanProductId(p.getLoanProduct().getId())
                .loanProductName(p.getLoanProduct().getName())
                .minInterestRate(p.getMinInterestRate()).maxInterestRate(p.getMaxInterestRate())
                .minTenureMonths(p.getMinTenureMonths()).maxTenureMonths(p.getMaxTenureMonths())
                .minLoanAmount(p.getMinLoanAmount()).maxLoanAmount(p.getMaxLoanAmount())
                .riskAppetite(p.getRiskAppetite()).isActive(p.getIsActive())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }

    public LoanRequestResponse toLoanRequestResponse(LoanRequest r) {
        return LoanRequestResponse.builder()
                .id(r.getId())
                .borrowerId(r.getBorrower().getId())
                .borrowerName(r.getBorrower().getFullName())
                .loanProductId(r.getLoanProduct().getId())
                .loanProductName(r.getLoanProduct().getName())
                .amount(r.getAmount()).tenureMonths(r.getTenureMonths())
                .purpose(r.getPurpose()).purposeDescription(r.getPurposeDescription())
                .status(r.getStatus()).rejectionReason(r.getRejectionReason())
                .acceptedByLenderId(r.getAcceptedByLender() != null ? r.getAcceptedByLender().getId() : null)
                .acceptedByLenderName(r.getAcceptedByLender() != null ? r.getAcceptedByLender().getFullName() : null)
                .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt()).build();
    }

    public EmiScheduleResponse toEmiScheduleResponse(EmiSchedule e) {
        return EmiScheduleResponse.builder()
                .id(e.getId()).emiNumber(e.getEmiNumber())
                .dueDate(e.getDueDate()).openingBalance(e.getOpeningBalance())
                .principalComponent(e.getPrincipalComponent())
                .interestComponent(e.getInterestComponent())
                .emiAmount(e.getEmiAmount()).closingBalance(e.getClosingBalance())
                .status(e.getStatus()).paidDate(e.getPaidDate()).amountPaid(e.getAmountPaid())
                .build();
    }
}
