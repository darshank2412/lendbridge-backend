package com.lendbridge.controller;

import com.lendbridge.dto.request.OtpRequest;
import com.lendbridge.dto.request.OtpVerifyRequest;
import com.lendbridge.dto.response.ApiResponse;
import com.lendbridge.entity.User;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.UserRepository;
import com.lendbridge.security.JwtUtil;
import com.lendbridge.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "OTP APIs", description = "OTP generation and verification")
public class AuthController {

    private final OtpService otpService;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/otp/send")
    @Operation(summary = "Send OTP")
    public ResponseEntity<ApiResponse<String>> sendOtp(@Valid @RequestBody OtpRequest request) {
        String msg = otpService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(msg));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP & Create User")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {
        Map<String, Object> result = otpService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(result, "OTP verified successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with phone + password → returns JWT")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @RequestBody Map<String, String> creds) {
        String phone    = creds.get("phoneNumber");
        String password = creds.get("password");

        if (phone == null || password == null) {
            throw LendbridgeException.badRequest("phoneNumber and password are required");
        }

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(phone, password));

        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> LendbridgeException.notFound("User not found"));

        String token = jwtUtil.generateToken(user, user.getId(), user.getRole().name());

        Map<String, Object> response = new HashMap<>();
        response.put("token",  token);
        response.put("userId", user.getId());
        response.put("role",   user.getRole());
        response.put("status", user.getStatus());
        response.put("fullName", user.getFullName());

        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }
}
