package com.lendbridge.repository;
import com.lendbridge.entity.LenderPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface LenderPreferenceRepository extends JpaRepository<LenderPreference, Long> {
    List<LenderPreference> findByLenderId(Long lenderId);
    List<LenderPreference> findByIsActiveTrue();
    Optional<LenderPreference> findByLenderIdAndLoanProductId(Long lenderId, Long loanProductId);
    @Query("SELECT lp FROM LenderPreference lp WHERE lp.lender.id = :lenderId AND lp.isActive = true")
    List<LenderPreference> findActiveByLenderId(@Param("lenderId") Long lenderId);
}
