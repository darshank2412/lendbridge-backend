package com.lendbridge.repository;
import com.lendbridge.entity.OtpRecord;
import com.lendbridge.enums.OtpPurpose;
import com.lendbridge.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface OtpRecordRepository extends JpaRepository<OtpRecord, Long> {
    Optional<OtpRecord> findTopByIdentifierAndOtpTypeAndPurposeAndUsedFalseOrderByCreatedAtDesc(
        String identifier, OtpType otpType, OtpPurpose purpose);
}
