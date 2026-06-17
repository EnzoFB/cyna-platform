package com.cyna.shared.infrastructure.config;

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

/**
 * Central OpenAPI / Swagger definition for the Cyna platform.
 *
 * <p>The generated specification is served at {@code /v3/api-docs} and rendered by
 * Swagger UI at {@code /swagger-ui.html}. Endpoint-level documentation lives on the
 * controllers themselves (via {@code @Tag} / {@code @Operation} / {@code @ApiResponse}).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Value("${cyna.api.server-url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI cynaOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url(serverUrl).description("Current environment"),
                        new Server().url("http://localhost:8080").description("Local development")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, bearerScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Cyna API")
                .version("v1")
                .description("""
                        REST API for the **Cyna** cybersecurity marketplace.

                        ## Conventions
                        - All responses are wrapped in a standard envelope: `{ success, data, error, timestamp }`.
                        - List endpoints return a paginated payload: `{ items, page, size, totalElements, totalPages, first, last }`.
                        - Errors carry a machine-readable `code` and a human-readable `message`
                          (e.g. `NOT_FOUND`, `VALIDATION_ERROR`, `BUSINESS_RULE_VIOLATION`).

                        ## Authentication
                        Most endpoints require a Bearer JWT access token. Obtain one via `POST /api/v1/auth/login`,
                        then click **Authorize** and paste the token. Public catalog reads (products, categories)
                        and authentication endpoints do not require a token.
                        """)
                .contact(new Contact()
                        .name("Cyna Platform Team")
                        .email("support@cyna.io"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://cyna.io"));
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste your JWT access token here (without the \"Bearer \" prefix)");
    }
}
