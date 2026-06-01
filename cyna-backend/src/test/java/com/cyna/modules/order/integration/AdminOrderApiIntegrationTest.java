package com.cyna.modules.order.integration;

import com.cyna.modules.order.interfaces.rest.dto.request.CancelOrderRequest;
import com.cyna.modules.order.interfaces.rest.dto.request.CreateOrderRequest;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryTranslationJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductTranslationJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.cyna.modules.product.infrastructure.persistence.repository.SpringDataProductRepository;
import com.cyna.modules.subscription.domain.model.BillingCycle;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = "app.security.rate-limit.enabled=false")
class AdminOrderApiIntegrationTest {

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
    private SpringDataProductRepository productRepository;

    @Autowired
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Nested
    class ListAllOrders {

        @Test
        void should_return_paginated_orders_for_admin() throws Exception {
            String customerToken = registerCustomerAndGetAccessToken("admin-orders-customer-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();
            UUID createdOrderId = createOrder(customerToken, productId);
            String adminToken = createAdminAndGetAccessToken("admin-orders-admin-" + UUID.randomUUID() + "@example.com");

            MvcResult result = mockMvc.perform(get("/api/v1/admin/orders")
                            .param("page", "0")
                            .param("size", "10")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andReturn();

            JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/items");
            boolean containsCreatedOrder = false;
            for (JsonNode item : items) {
                if (createdOrderId.toString().equals(item.path("id").asText())) {
                    containsCreatedOrder = true;
                    break;
                }
            }
            assertThat(containsCreatedOrder).isTrue();
        }

        @Test
        void should_filter_orders_by_status_for_admin() throws Exception {
            String customerToken = registerCustomerAndGetAccessToken("admin-orders-filter-customer-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();
            UUID createdOrderId = createOrder(customerToken, productId);
            String adminToken = createAdminAndGetAccessToken("admin-orders-filter-admin-" + UUID.randomUUID() + "@example.com");

            MvcResult result = mockMvc.perform(get("/api/v1/admin/orders")
                            .param("status", "PENDING")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/items");
            boolean containsCreatedOrder = false;
            for (JsonNode item : items) {
                if (createdOrderId.toString().equals(item.path("id").asText())) {
                    containsCreatedOrder = true;
                    assertThat(item.path("status").asText()).isEqualTo("PENDING");
                    break;
                }
            }
            assertThat(containsCreatedOrder).isTrue();
        }

        @Test
        void should_return_403_for_customer_token() throws Exception {
            String customerToken = registerCustomerAndGetAccessToken("admin-orders-forbidden-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(get("/api/v1/admin/orders")
                            .header("Authorization", bearer(customerToken)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }

        @Test
        void should_return_401_without_authentication() throws Exception {
            mockMvc.perform(get("/api/v1/admin/orders"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    @Nested
    class GetOrderById {

        @Test
        void should_return_order_for_admin() throws Exception {
            String customerToken = registerCustomerAndGetAccessToken("admin-order-get-customer-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();
            UUID orderId = createOrder(customerToken, productId);
            String adminToken = createAdminAndGetAccessToken("admin-order-get-admin-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(orderId.toString()));
        }

        @Test
        void should_return_404_when_order_does_not_exist() throws Exception {
            String adminToken = createAdminAndGetAccessToken("admin-order-404-admin-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(get("/api/v1/admin/orders/" + UUID.randomUUID())
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }

    @Nested
    class CancelOrder {

        @Test
        void should_cancel_any_customer_order_for_admin() throws Exception {
            String customerToken = registerCustomerAndGetAccessToken("admin-order-cancel-customer-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();
            UUID orderId = createOrder(customerToken, productId);
            String adminToken = createAdminAndGetAccessToken("admin-order-cancel-admin-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/cancel")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CancelOrderRequest("Cancelled by admin"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(orderId.toString()));

            mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        }

        @Test
        void should_return_400_when_cancellation_reason_is_invalid() throws Exception {
            String customerToken = registerCustomerAndGetAccessToken("admin-order-cancel-validation-customer-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();
            UUID orderId = createOrder(customerToken, productId);
            String adminToken = createAdminAndGetAccessToken("admin-order-cancel-validation-admin-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/cancel")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CancelOrderRequest("<script>alert(1)</script>"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }

        @Test
        void should_return_404_when_cancelling_unknown_order() throws Exception {
            String adminToken = createAdminAndGetAccessToken("admin-order-cancel-404-admin-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/admin/orders/" + UUID.randomUUID() + "/cancel")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CancelOrderRequest("Not found"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }

    private UUID createOrder(String token, UUID productId) throws Exception {
        var request = new CreateOrderRequest(List.of(
                new CreateOrderRequest.CreateOrderLineRequest(productId, BillingCycle.MONTHLY, 1)
        ));

        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).at("/data").asText());
    }

    private String createAdminAndGetAccessToken(String email) throws Exception {
        registerCustomerAndGetAccessToken(email);
        promoteUserToAdminWithRetry(email);
        var adminUser = loadAdminUserByEmailWithRetry(email);
        return jwtProvider.generateAccessToken(adminUser);
    }

    private String registerCustomerAndGetAccessToken(String email) throws Exception {
        int maxAttempts = 8;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest(email, "password123", "Admin", "Order", "Acme", "fr", true))))
                    .andReturn();

            int statusCode = result.getResponse().getStatus();
            if (statusCode == CREATED.value()) {
                return objectMapper.readTree(result.getResponse().getContentAsString())
                        .at("/data/accessToken")
                        .asText();
            }

            boolean shouldRetry = statusCode == TOO_MANY_REQUESTS.value() || statusCode >= 500;
            if (shouldRetry && attempt < maxAttempts) {
                long retryAfterMillis = parseRetryAfterSeconds(result.getResponse().getHeader("Retry-After")) * 1000L;
                long cappedBackoffMillis = Math.min(4000L, 250L * attempt);
                sleepSafely(Math.max(retryAfterMillis, cappedBackoffMillis));
                continue;
            }

            throw new AssertionError(
                    "Expected 201 Created from /api/v1/auth/register, got " + statusCode
                            + " body=" + result.getResponse().getContentAsString()
            );
        }

        throw new AssertionError("Unable to register user after retries");
    }

    private long parseRetryAfterSeconds(String retryAfterHeader) {
        if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
            return 1L;
        }
        try {
            return Math.max(1L, Long.parseLong(retryAfterHeader.trim()));
        } catch (NumberFormatException ignored) {
            return 1L;
        }
    }

    private void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for rate-limit retry", e);
        }
    }

    private void promoteUserToAdminWithRetry(String email) {
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            int updated = jdbcTemplate.update("UPDATE user_schema.users SET role = 'ADMIN' WHERE email = ?", email);
            if (updated > 0) {
                return;
            }
            if (attempt < maxAttempts) {
                sleepSafely(100L * attempt);
            }
        }
        throw new IllegalStateException("Unable to promote user to ADMIN after retries");
    }

    private User loadAdminUserByEmailWithRetry(String email) {
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            var user = userRepository.findByEmail(Email.of(email));
            if (user.isPresent() && "ADMIN".equals(user.get().getRole().name())) {
                return user.get();
            }
            if (attempt < maxAttempts) {
                sleepSafely(100L * attempt);
            }
        }
        throw new IllegalStateException("Admin user not found with ADMIN role after promotion");
    }

    private UUID createPublishedProduct() {
        CategoryJpaEntity category = categoryRepository.findByName("ADMIN_ORDER_TEST")
                .orElseGet(() -> {
                    CategoryJpaEntity cat = new CategoryJpaEntity();
                    cat.setId(UUID.randomUUID());
                    cat.setName("ADMIN_ORDER_TEST");
                    cat.setActive(true);
                    cat.setCreatedAt(Instant.now());
                    cat.setUpdatedAt(Instant.now());
                    cat.setTranslations(Set.of(
                            CategoryTranslationJpaEntity.of(cat, "fr", "Admin Order Test", "Admin order test category")
                    ));
                    return categoryRepository.saveAndFlush(cat);
                });

        ProductJpaEntity product = new ProductJpaEntity();
        product.setId(UUID.randomUUID());
        product.setCategory(category);
        product.setPriorityLevel(1);
        product.setMonthlyPrice(BigDecimal.valueOf(100));
        product.setAnnualPrice(BigDecimal.valueOf(1000));
        product.setCurrency("EUR");
        product.setPublished(true);
        product.setAvailable(true);
        product.setFreeTrialDays(0);
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        product.setTranslations(Set.of(
                ProductTranslationJpaEntity.of(
                        product,
                        "fr",
                        "Admin order product " + product.getId(),
                        "Service description",
                        "Technical description",
                        List.of("24/7")
                )
        ));
        return productRepository.saveAndFlush(product).getId();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
