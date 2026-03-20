package com.lendbridge.service;

import com.lendbridge.dto.request.*;
import com.lendbridge.dto.response.UserResponse;
import com.lendbridge.entity.*;
import com.lendbridge.enums.*;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.UserRepository;
import com.lendbridge.security.JwtUtil;
import com.lendbridge.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityMapper mapper;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserResponse register(Long userId, UserRegistrationRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> LendbridgeException.notFound("User not found with ID: " + userId));

        if (user.getStatus() == UserStatus.REGISTRATION_COMPLETE
                || user.getStatus() == UserStatus.PLATFORM_ACCOUNT_CREATED) {
            throw LendbridgeException.conflict("User already registered.");
        }

        // Validate age >= 18
        if (req.getDateOfBirth().isAfter(LocalDate.now().minusYears(18))) {
            throw LendbridgeException.badRequest("You must be at least 18 years old.");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw LendbridgeException.conflict("Email already in use.");
        }

        Address address = Address.builder()
                .line1(req.getAddress().getLine1())
                .city(req.getAddress().getCity())
                .state(req.getAddress().getState())
                .pincode(req.getAddress().getPincode())
                .build();

        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setFullName(req.getFirstName() + " " + req.getLastName());
        user.setEmail(req.getEmail());
        user.setDateOfBirth(req.getDateOfBirth());
        user.setGender(req.getGender());
        user.setRole(req.getRole());
        user.setPan(req.getPan().toUpperCase());
        user.setIncomeBracket(req.getIncomeBracket());
        user.setP2pExperience(req.getP2pExperience());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setAddress(address);
        user.setStatus(UserStatus.REGISTRATION_COMPLETE);
        user.setPlatformAccountNumber(generateAccountNumber());

        return mapper.toUserResponse(userRepository.save(user));
    }

    public UserResponse getMyProfile(String phoneNumber) {
        User user = findByPhone(phoneNumber);
        return mapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String phoneNumber, UserProfileUpdateRequest req) {
        User user = findByPhone(phoneNumber);
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getEmail() != null) {
            if (!req.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(req.getEmail())) {
                throw LendbridgeException.conflict("Email already in use.");
            }
            user.setEmail(req.getEmail());
        }
        if (req.getGender() != null) user.setGender(req.getGender());
        return mapper.toUserResponse(userRepository.save(user));
    }

    public List<UserResponse> getAllAdmins() {
        return userRepository.findByRole(Role.ADMIN).stream()
                .map(mapper::toUserResponse).toList();
    }

    @Transactional
    public UserResponse createAdmin(CreateAdminRequest req) {
        if (userRepository.existsByPhoneNumber(req.getPhoneNumber())) {
            throw LendbridgeException.conflict("Phone number already registered.");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw LendbridgeException.conflict("Email already in use.");
        }
        User admin = User.builder()
                .phoneNumber(req.getPhoneNumber())
                .countryCode(req.getCountryCode())
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.ADMIN)
                .status(UserStatus.REGISTRATION_COMPLETE)
                .phoneVerified(true)
                .platformAccountNumber(generateAccountNumber())
                .build();
        return mapper.toUserResponse(userRepository.save(admin));
    }

    @Transactional
    public void deleteAdmin(Long id) {
        User admin = userRepository.findById(id)
                .orElseThrow(() -> LendbridgeException.notFound("Admin not found"));
        if (admin.getRole() != Role.ADMIN) {
            throw LendbridgeException.badRequest("User is not an admin");
        }
        userRepository.delete(admin);
    }

    public User findByPhone(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> LendbridgeException.notFound("User not found"));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> LendbridgeException.notFound("User not found with ID: " + id));
    }

    private String generateAccountNumber() {
        return "LB" + System.currentTimeMillis() % 10_000_000_000L;
    }
}
