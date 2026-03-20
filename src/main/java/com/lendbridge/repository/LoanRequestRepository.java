package com.lendbridge.repository;
import com.lendbridge.entity.LoanRequest;
import com.lendbridge.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {
    List<LoanRequest> findByBorrowerId(Long borrowerId);
    List<LoanRequest> findByStatus(LoanStatus status);
    List<LoanRequest> findByStatusIn(List<LoanStatus> statuses);
    @Query("SELECT lr FROM LoanRequest lr WHERE lr.status = 'PENDING' ORDER BY lr.createdAt DESC")
    List<LoanRequest> findAllPending();
    @Query("SELECT lr FROM LoanRequest lr WHERE lr.acceptedByLender.id = :lenderId AND lr.status IN ('MATCHED','ACCEPTED','DISBURSED')")
    List<LoanRequest> findMatchedByLenderId(@Param("lenderId") Long lenderId);
}
