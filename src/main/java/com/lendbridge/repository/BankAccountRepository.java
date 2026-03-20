package com.lendbridge.repository;
import com.lendbridge.entity.BankAccount;
import com.lendbridge.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByUserId(Long userId);
    Optional<BankAccount> findByUserIdAndAccountType(Long userId, AccountType type);
    boolean existsByUserIdAndAccountType(Long userId, AccountType type);
}
