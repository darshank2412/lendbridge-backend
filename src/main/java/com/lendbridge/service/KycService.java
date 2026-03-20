package com.lendbridge.service;

import com.lendbridge.dto.request.KycSubmitRequest;
import com.lendbridge.entity.KycDocument;
import com.lendbridge.entity.User;
import com.lendbridge.enums.DocumentStatus;
import com.lendbridge.enums.DocumentType;
import com.lendbridge.enums.KycStatus;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.KycDocumentRepository;
import com.lendbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KycService {

    private final KycDocumentRepository kycRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public KycDocument submitDocument(Long userId, KycSubmitRequest req) {
        User user = userService.findById(userId);

        if (kycRepository.existsByUserIdAndDocumentType(userId, req.getDocumentType())) {
            // Allow resubmission only if previously rejected
            KycDocument existing = kycRepository
                    .findByUserIdAndDocumentType(userId, req.getDocumentType())
                    .orElseThrow();
            if (existing.getStatus() != DocumentStatus.REJECTED) {
                throw LendbridgeException.conflict(
                        req.getDocumentType() + " already submitted. Status: " + existing.getStatus());
            }
            kycRepository.delete(existing);
        }

        KycDocument doc = KycDocument.builder()
                .user(user)
                .documentType(req.getDocumentType())
                .documentNumber(req.getDocumentNumber())
                .documentUrl(req.getDocumentUrl())
                .status(DocumentStatus.PENDING)
                .build();

        return kycRepository.save(doc);
    }

    public List<KycDocument> getDocuments(Long userId) {
        userService.findById(userId); // validate user exists
        return kycRepository.findByUserId(userId);
    }

    @Transactional
    public KycDocument approveDocument(Long docId) {
        KycDocument doc = findDocById(docId);
        doc.setStatus(DocumentStatus.VERIFIED);
        doc.setReviewedAt(LocalDateTime.now());
        kycRepository.save(doc);
        updateUserKycStatus(doc.getUser());
        return doc;
    }

    @Transactional
    public KycDocument rejectDocument(Long docId, String reason) {
        KycDocument doc = findDocById(docId);
        doc.setStatus(DocumentStatus.REJECTED);
        doc.setRejectionNote(reason);
        doc.setReviewedAt(LocalDateTime.now());
        kycRepository.save(doc);
        checkAndSetRejectedStatus(doc.getUser());
        return doc;
    }

    private void updateUserKycStatus(User user) {
        // Both AADHAAR and PAN must be VERIFIED for full KYC approval
        List<KycDocument> docs = kycRepository.findByUserId(user.getId());
        boolean hasVerifiedAadhaar = docs.stream()
                .anyMatch(d -> d.getDocumentType() == DocumentType.AADHAAR
                        && d.getStatus() == DocumentStatus.VERIFIED);
        boolean hasVerifiedPan = docs.stream()
                .anyMatch(d -> d.getDocumentType() == DocumentType.PAN
                        && d.getStatus() == DocumentStatus.VERIFIED);

        if (hasVerifiedAadhaar && hasVerifiedPan) {
            user.setKycStatus(KycStatus.VERIFIED);
            userRepository.save(user);
        }
    }

    private void checkAndSetRejectedStatus(User user) {
        List<KycDocument> docs = kycRepository.findByUserId(user.getId());
        boolean anyRejected = docs.stream().anyMatch(d -> d.getStatus() == DocumentStatus.REJECTED);
        boolean noneVerified = docs.stream().noneMatch(d -> d.getStatus() == DocumentStatus.VERIFIED);
        if (anyRejected && noneVerified) {
            user.setKycStatus(KycStatus.REJECTED);
            userRepository.save(user);
        }
    }

    private KycDocument findDocById(Long docId) {
        return kycRepository.findById(docId)
                .orElseThrow(() -> LendbridgeException.notFound("KYC document not found"));
    }
}
