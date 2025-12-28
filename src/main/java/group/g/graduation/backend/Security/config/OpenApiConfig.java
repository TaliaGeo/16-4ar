package group.g.graduation.backend.Security.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port}")
    private String serverPort;

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gharsih API")
                        .description("Gharsih - Medicinal Plants & Garden Management Backend API\n\n" +
                                "## Authentication\n" +
                                "Most endpoints require JWT authentication. To authenticate:\n" +
                                "1. Login via `/api/auth/login` with email and password\n" +
                                "2. Copy the `accessToken` from the response\n" +
                                "3. Click the 'Authorize' button and paste the token\n\n" +
                                "## Test Credentials\n" +
                                "- **Email:** admin@example.com\n" +
                                "- **Password:** admin123")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Gharsih Team")
                                .email("gharsih@example.com"))
                        .license(new License()
                                .name("MIT License")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort)
                                .description("Local Development Server")
                ))
                // Add JWT Security
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token (without 'Bearer ' prefix)")
                        )
                );
    }
}