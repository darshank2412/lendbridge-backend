package com.lendbridge.repository;
import com.lendbridge.entity.EmiSchedule;
import com.lendbridge.enums.EmiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, Long> {
    List<EmiSchedule> findByLoanIdOrderByEmiNumber(Long loanId);
    Optional<EmiSchedule> findByLoanIdAndEmiNumber(Long loanId, Integer emiNumber);
    List<EmiSchedule> findByStatusAndDueDateBefore(EmiStatus status, LocalDate date);
    long countByLoanIdAndStatus(Long loanId, EmiStatus status);
}
