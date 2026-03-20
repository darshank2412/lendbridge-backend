package com.lendbridge.service;

import com.lendbridge.dto.request.OtpRequest;
import com.lendbridge.dto.request.OtpVerifyRequest;
import com.lendbridge.entity.OtpRecord;
import com.lendbridge.entity.User;
import com.lendbridge.enums.OtpPurpose;
import com.lendbridge.enums.OtpType;
import com.lendbridge.enums.Role;
import com.lendbridge.enums.UserStatus;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.OtpRecordRepository;
import com.lendbridge.repository.UserRepository;
import com.lendbridge.security.JwtUtil;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRecordRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String twilioPhone;

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;

    @Value("${otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    public void initTwilio() {
        if (twilioEnabled) {
            try {
                Twilio.init(accountSid, authToken);
                log.info("Twilio initialized");
            } catch (Exception e) {
                log.warn("Twilio init failed - using console OTP mode: {}", e.getMessage());
            }
        } else {
            log.info("Twilio disabled - OTP will be printed to console (dev mode)");
        }
    }

    @Transactional
    public String sendOtp(OtpRequest request) {
        String otp = generateOtp();
        String identifier = request.getIdentifier();

        // Store BCrypt hash of OTP - BCrypt output is always 60 chars
        OtpRecord record = OtpRecord.builder()
                .identifier(identifier)
                .otpCode(passwordEncoder.encode(otp))
                .otpType(request.getOtpType())
                .purpose(request.getPurpose())
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)
                .build();
        otpRepository.save(record);

        if (request.getOtpType() == OtpType.PHONE) {
            sendSms(identifier, request.getCountryCode(), otp);
        } else {
            log.info("EMAIL OTP for {} -> {}", identifier, otp);
        }

        // DEV MODE: return OTP in response so frontend can display it
        // In production (twilio.enabled=true) this line is never reached
        if (!twilioEnabled) {
            return "DEV MODE - Your OTP is: " + otp
                    + " (This will not appear when Twilio is enabled)";
        }

        return "OTP sent successfully to " + maskIdentifier(identifier);
    }

    @Transactional
    public Map<String, Object> verifyOtp(OtpVerifyRequest request) {
        OtpRecord record = otpRepository
                .findTopByIdentifierAndOtpTypeAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        request.getIdentifier(), request.getOtpType(), request.getPurpose())
                .orElseThrow(() -> LendbridgeException.badRequest(
                        "No OTP found. Please request a new one."));

        if (record.isExpired()) {
            throw LendbridgeException.badRequest("OTP has expired. Please request a new one.");
        }

        if (!passwordEncoder.matches(request.getOtpCode(), record.getOtpCode())) {
            throw LendbridgeException.badRequest("Invalid OTP. Please try again.");
        }

        record.setUsed(true);
        otpRepository.save(record);

        // Create user shell on first OTP verification, or fetch existing user
        Optional<User> existing = userRepository.findByPhoneNumber(request.getIdentifier());
        User user;
        if (existing.isPresent()) {
            user = existing.get();
        } else {
            user = User.builder()
                    .phoneNumber(request.getIdentifier())
                    .countryCode(request.getCountryCode())
                    .role(request.getRole())
                    .status(UserStatus.MOBILE_VERIFIED)
                    .phoneVerified(true)
                    .password(passwordEncoder.encode("TEMP_" + request.getOtpCode()))
                    .build();
            user = userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user, user.getId(), user.getRole().name());

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("token", token);
        result.put("role", user.getRole());
        result.put("status", user.getStatus());
        return result;
    }

    private void sendSms(String phone, String countryCode, String otp) {
        String fullNumber = (countryCode != null ? countryCode : "+91") + phone;
        if (twilioEnabled) {
            try {
                Message.creator(
                        new PhoneNumber(fullNumber),
                        new PhoneNumber(twilioPhone),
                        "Your LendBridge OTP is: " + otp
                                + ". Valid for " + otpExpiryMinutes
                                + " minutes. Do not share."
                ).create();
                log.info("SMS sent to {}", fullNumber);
            } catch (Exception e) {
                log.error("SMS send failed for {}: {}", fullNumber, e.getMessage());
                log.info("FALLBACK OTP for {} -> {}", fullNumber, otp);
            }
        } else {
            log.info("==================================");
            log.info("  DEV OTP for {} -> {}", fullNumber, otp);
            log.info("==================================");
        }
    }

    private String generateOtp() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String maskIdentifier(String id) {
        if (id.length() <= 4) return "****";
        return id.substring(0, 2) + "****" + id.substring(id.length() - 2);
    }
}