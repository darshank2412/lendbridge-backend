package com.lendbridge.config;

import com.lendbridge.entity.User;
import com.lendbridge.enums.KycStatus;
import com.lendbridge.enums.Role;
import com.lendbridge.enums.UserStatus;
import com.lendbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedDefaultAdmin() {
        return args -> {
            String adminPhone = "9999999999";

            if (userRepository.findByPhoneNumber(adminPhone).isEmpty()) {
                User admin = User.builder()
                        .phoneNumber(adminPhone)
                        .countryCode("+91")
                        .fullName("Admin User")
                        .email("admin@lendbridge.com")
                        .password(passwordEncoder.encode("Admin@123"))
                        .role(Role.ADMIN)
                        .status(UserStatus.REGISTRATION_COMPLETE)
                        .kycStatus(KycStatus.VERIFIED)
                        .phoneVerified(true)
                        .emailVerified(true)
                        .platformAccountNumber("LBADMIN001")
                        .build();

                userRepository.save(admin);
                log.info("================================================");
                log.info("  Default Admin created successfully");
                log.info("  Phone    : 9999999999");
                log.info("  Password : Admin@123");
                log.info("================================================");
            } else {
                log.info("Default admin already exists - skipping seed");
            }
        };
    }
}