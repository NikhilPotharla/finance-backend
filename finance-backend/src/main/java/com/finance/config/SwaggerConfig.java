package com.finance.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Finance Data Processing API",
                version = "1.0",
                description = "REST API for Finance Data Processing with JWT Authentication and Role-Based Access Control",
                contact = @Contact(name = "Finance Backend", email = "admin@finance.com")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development Server")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Bearer Token. Enter: Bearer <your-token>",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {
}
