package com.lendbridge.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "LendBridge — Micro Lending Platform API",
        description = "Backend APIs for LendBridge P2P Micro Lending Platform — Weeks 1–8",
        version = "v1",
        contact = @Contact(name = "LendBridge Team", email = "admin@lendbridge.in")
    ),
    servers = @Server(url = "http://localhost:8081", description = "Local Dev")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT token — obtain via POST /auth/login or POST /auth/otp/verify"
)
public class OpenApiConfig {}
