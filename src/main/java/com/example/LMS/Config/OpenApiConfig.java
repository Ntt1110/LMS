package com.example.LMS.Config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Hệ thống Quản lý Học tập (LMS) API",
                description = "Tài liệu thiết kế RESTful API cho dự án LMS sử dụng Spring Boot và React",
                version = "1.0",
                contact = @Contact(
                        name = "Backend Team",
                        email = "admin@lms-project.com"
                )
        ),
        // Áp dụng bảo mật JWT này cho toàn bộ các API trong dự án
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Nhập Access Token (JWT) của bạn vào đây để test các API bị khóa",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
    // Không cần viết thêm logic gì vào trong class này.
    // Các annotation ở trên đã làm hết mọi việc.
}
