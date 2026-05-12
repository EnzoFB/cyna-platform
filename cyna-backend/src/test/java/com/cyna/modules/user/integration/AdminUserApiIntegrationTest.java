package com.cyna.modules.user.integration;

import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.modules.user.interfaces.dto.request.CreateAdminUserRequest;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import com.cyna.modules.user.interfaces.dto.request.UpdateUserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AdminUserApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cyna_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    // ------------------------------------------------------------------ helpers

    /**
     * Registers a user, promotes them to ADMIN in the DB, then mints a fresh
     * JWT carrying the ADMIN role. We bypass {@code POST /auth/login} (which
     * now requires an OTP round-trip) and use {@link JwtProvider} directly —
     * this test exercises the admin endpoints, not the login flow.
     */
    private String createAdminAndGetToken(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, "password123", "Admin", "User", "fr"))))
                .andExpect(status().isCreated());

        jdbcTemplate.update(
                "UPDATE user_schema.users SET role = 'ADMIN' WHERE email = ?", email);

        var promoted = userRepository.findByEmail(Email.of(email))
                .orElseThrow(() -> new IllegalStateException("Admin user not found after promotion"));
        return jwtProvider.generateAccessToken(promoted);
    }

    private String registerCustomerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, "password123", "Customer", "User", "fr"))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
    }

    private UUID getUserIdByEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM user_schema.users WHERE email = ?",
                UUID.class, email);
    }

    // ==================================================================
    // GET /api/v1/admin/users
    // ==================================================================

    @Nested
    class ListUsers {

        @Test
        void should_return_paginated_user_list_for_admin() throws Exception {
            String adminToken = createAdminAndGetToken("admin.list@example.com");

            mockMvc.perform(get("/api/v1/admin/users")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.totalElements").isNumber())
                    .andExpect(jsonPath("$.data.page").isNumber())
                    .andExpect(jsonPath("$.data.size").isNumber());
        }

        @Test
        void should_support_pagination_parameters() throws Exception {
            String adminToken = createAdminAndGetToken("admin.listpage@example.com");

            mockMvc.perform(get("/api/v1/admin/users")
                            .param("page", "0")
                            .param("size", "5")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.size").value(5));
        }

        @Test
        void should_deny_access_for_customer() throws Exception {
            String customerToken = registerCustomerAndGetToken("customer.list@example.com");

            mockMvc.perform(get("/api/v1/admin/users")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }

        @Test
        void should_reject_unauthenticated_request() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    // ==================================================================
    // POST /api/v1/admin/users
    // ==================================================================

    @Nested
    class CreateUser {

        @Test
        void should_create_customer_user() throws Exception {
            String adminToken = createAdminAndGetToken("admin.createcust@example.com");

            var request = new CreateAdminUserRequest(
                    "new.customer@example.com", "password123", "New", "Customer", "CUSTOMER");

            mockMvc.perform(post("/api/v1/admin/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(notNullValue()));
        }

        @Test
        void should_create_support_user() throws Exception {
            String adminToken = createAdminAndGetToken("admin.createsup@example.com");

            var request = new CreateAdminUserRequest(
                    "new.support@example.com", "password123", "Support", "Agent", "SUPPORT");

            mockMvc.perform(post("/api/v1/admin/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(notNullValue()));
        }

        @Test
        void should_reject_duplicate_email() throws Exception {
            String adminToken = createAdminAndGetToken("admin.createdup@example.com");

            // Seed an existing user
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest("existing@example.com", "password123", "Existing", "User", "fr"))))
                    .andExpect(status().isCreated());

            var request = new CreateAdminUserRequest(
                    "existing@example.com", "password123", "Another", "User", "CUSTOMER");

            mockMvc.perform(post("/api/v1/admin/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void should_deny_access_for_customer() throws Exception {
            String customerToken = registerCustomerAndGetToken("customer.create@example.com");

            var request = new CreateAdminUserRequest(
                    "sneaky@example.com", "password123", "Sneaky", "User", "ADMIN");

            mockMvc.perform(post("/api/v1/admin/users")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }

    // ==================================================================
    // PUT /api/v1/admin/users/{id}
    // ==================================================================

    @Nested
    class UpdateUser {

        @Test
        void should_update_user_name_and_role() throws Exception {
            String adminToken = createAdminAndGetToken("admin.update@example.com");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest("update.target@example.com", "password123", "Old", "Name", "fr"))))
                    .andExpect(status().isCreated());

            UUID userId = getUserIdByEmail("update.target@example.com");

            var request = new UpdateUserRequest("Updated", "Name", "SUPPORT", "ACTIVE");

            mockMvc.perform(put("/api/v1/admin/users/" + userId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void should_deactivate_user() throws Exception {
            String adminToken = createAdminAndGetToken("admin.deactivate@example.com");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest("deactivate.target@example.com", "password123", "Active", "User", "fr"))))
                    .andExpect(status().isCreated());

            UUID userId = getUserIdByEmail("deactivate.target@example.com");

            var request = new UpdateUserRequest("Active", "User", "CUSTOMER", "INACTIVE");

            mockMvc.perform(put("/api/v1/admin/users/" + userId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void should_deny_access_for_customer() throws Exception {
            String customerToken = registerCustomerAndGetToken("customer.update@example.com");
            UUID anyId = UUID.randomUUID();

            var request = new UpdateUserRequest("Hacked", "Name", "ADMIN", "ACTIVE");

            mockMvc.perform(put("/api/v1/admin/users/" + anyId)
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }

    // ==================================================================
    // DELETE /api/v1/admin/users/{id}
    // ==================================================================

    @Nested
    class DeleteUser {

        @Test
        void should_delete_existing_user() throws Exception {
            String adminToken = createAdminAndGetToken("admin.delete@example.com");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest("delete.target@example.com", "password123", "To", "Delete", "fr"))))
                    .andExpect(status().isCreated());

            UUID userId = getUserIdByEmail("delete.target@example.com");

            mockMvc.perform(delete("/api/v1/admin/users/" + userId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void should_deny_access_for_customer() throws Exception {
            String customerToken = registerCustomerAndGetToken("customer.delete@example.com");
            UUID anyId = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/admin/users/" + anyId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }
}
