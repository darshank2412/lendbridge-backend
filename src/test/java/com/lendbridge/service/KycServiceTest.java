package com.lendbridge.service;

import com.lendbridge.dto.request.KycSubmitRequest;
import com.lendbridge.entity.*;
import com.lendbridge.enums.*;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KYC Service Unit Tests")
class KycServiceTest {

    @Mock KycDocumentRepository kycRepo;
    @Mock UserRepository userRepo;
    @Mock UserService userService;
    @InjectMocks KycService kycService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).role(Role.BORROWER)
                .kycStatus(KycStatus.PENDING).build();
    }

    @Test
    @DisplayName("Submitting AADHAAR creates PENDING document")
    void submitAadhaarCreatesPendingDoc() {
        when(userService.findById(1L)).thenReturn(user);
        when(kycRepo.existsByUserIdAndDocumentType(1L, DocumentType.AADHAAR)).thenReturn(false);
        when(kycRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new KycSubmitRequest(DocumentType.AADHAAR, "1234 5678 9012", null);
        var doc = kycService.submitDocument(1L, req);

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PENDING);
        assertThat(doc.getDocumentType()).isEqualTo(DocumentType.AADHAAR);
    }

    @Test
    @DisplayName("Submitting duplicate non-rejected document throws conflict")
    void duplicateDocumentThrowsConflict() {
        KycDocument existing = KycDocument.builder()
                .documentType(DocumentType.AADHAAR)
                .status(DocumentStatus.PENDING).build();
        when(userService.findById(1L)).thenReturn(user);
        when(kycRepo.existsByUserIdAndDocumentType(1L, DocumentType.AADHAAR)).thenReturn(true);
        when(kycRepo.findByUserIdAndDocumentType(1L, DocumentType.AADHAAR))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                kycService.submitDocument(1L, new KycSubmitRequest(DocumentType.AADHAAR, "1234", null)))
                .isInstanceOf(LendbridgeException.class)
                .hasMessageContaining("already submitted");
    }

    @Test
    @DisplayName("Approving AADHAAR + PAN sets user KYC status to VERIFIED")
    void approvingBothDocsVerifiesUser() {
        KycDocument aadhaar = KycDocument.builder().id(1L).user(user)
                .documentType(DocumentType.AADHAAR).status(DocumentStatus.PENDING).build();
        KycDocument pan = KycDocument.builder().id(2L).user(user)
                .documentType(DocumentType.PAN).status(DocumentStatus.VERIFIED).build();

        when(kycRepo.findById(1L)).thenReturn(Optional.of(aadhaar));
        when(kycRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(kycRepo.findByUserId(1L)).thenReturn(List.of(aadhaar, pan));
        when(userRepo.save(any())).thenReturn(user);

        kycService.approveDocument(1L);

        assertThat(aadhaar.getStatus()).isEqualTo(DocumentStatus.VERIFIED);
        assertThat(user.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
    }

    @Test
    @DisplayName("Rejecting document sets rejection note")
    void rejectDocumentSetsNote() {
        KycDocument doc = KycDocument.builder().id(1L).user(user)
                .documentType(DocumentType.PAN).status(DocumentStatus.PENDING).build();

        when(kycRepo.findById(1L)).thenReturn(Optional.of(doc));
        when(kycRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(kycRepo.findByUserId(1L)).thenReturn(List.of(doc));
        when(userRepo.save(any())).thenReturn(user);

        kycService.rejectDocument(1L, "Document unclear");

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.REJECTED);
        assertThat(doc.getRejectionNote()).isEqualTo("Document unclear");
    }
}
