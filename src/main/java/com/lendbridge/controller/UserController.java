package com.lendbridge.controller;

import com.lendbridge.dto.request.CreateAdminRequest;
import com.lendbridge.dto.request.UserProfileUpdateRequest;
import com.lendbridge.dto.request.UserRegistrationRequest;
import com.lendbridge.dto.response.ApiResponse;
import com.lendbridge.dto.response.UserResponse;
import com.lendbridge.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "User APIs", description = "User onboarding and profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get my profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getMyProfile(userDetails.getUsername())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateProfile(userDetails.getUsername(), request),
                "Profile updated successfully"));
    }

    @PostMapping("/register")
    @Operation(summary = "Complete registration")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @RequestParam Long userId,
            @Valid @RequestBody UserRegistrationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.register(userId, request),
                "Registration completed successfully"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all admins")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllAdmins() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllAdmins()));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new admin")
    public ResponseEntity<ApiResponse<UserResponse>> createAdmin(
            @Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.createAdmin(request), "Admin created successfully"));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an admin")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(@PathVariable Long id) {
        userService.deleteAdmin(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Admin deleted successfully"));
    }
}
