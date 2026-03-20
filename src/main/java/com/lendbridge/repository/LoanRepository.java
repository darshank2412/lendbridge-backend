package com.lendbridge.repository;
import com.lendbridge.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByLoanRequestId(Long loanRequestId);
    List<Loan> findByBorrowerId(Long borrowerId);
    List<Loan> findByLenderId(Long lenderId);
    List<Loan> findByBorrowerIdAndClosedFalse(Long borrowerId);
}
