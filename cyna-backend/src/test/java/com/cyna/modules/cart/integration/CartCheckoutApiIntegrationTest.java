package com.cyna.modules.cart.integration;

import com.cyna.modules.cart.domain.model.BillingCycle;
import com.cyna.modules.cart.interfaces.dto.request.AddCartLineRequest;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.cyna.modules.product.infrastructure.persistence.repository.SpringDataProductRepository;
import com.cyna.modules.user.interfaces.dto.request.LoginRequest;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CartCheckoutApiIntegrationTest {

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

    @Test
    void should_checkout_cart_successfully() throws Exception {
        String token = registerAndLogin("checkout-success-" + UUID.randomUUID() + "@example.com");
        UUID productId = createProduct("PUBLISHED", BigDecimal.valueOf(300), BigDecimal.valueOf(3000));

        addLine(token, productId, 2);

        mockMvc.perform(post("/api/v1/cart/checkout")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cartId").isNotEmpty())
                .andExpect(jsonPath("$.data.subtotalHt").value(600))
                .andExpect(jsonPath("$.data.vatAmount").value(120))
                .andExpect(jsonPath("$.data.totalTtc").value(720))
                .andExpect(jsonPath("$.data.currency").value("EUR"));
    }

    @Test
    void should_return_conflict_when_checkout_is_retried() throws Exception {
        String token = registerAndLogin("checkout-retry-" + UUID.randomUUID() + "@example.com");
        UUID productId = createProduct("PUBLISHED", BigDecimal.valueOf(250), BigDecimal.valueOf(2500));

        addLine(token, productId, 1);

        mockMvc.perform(post("/api/v1/cart/checkout")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/cart/checkout")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("Cart is already checked out"));
    }

    @Test
    void should_block_checkout_when_product_became_unpublished() throws Exception {
        String token = registerAndLogin("checkout-unpublished-" + UUID.randomUUID() + "@example.com");
        UUID productId = createProduct("PUBLISHED", BigDecimal.valueOf(300), BigDecimal.valueOf(3000));

        addLine(token, productId, 1);
        unpublishProduct(productId);

        mockMvc.perform(post("/api/v1/cart/checkout")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.error.message").value("Product is no longer available: " + productId));
    }

    @Test
    void should_return_gone_for_guest_merge_endpoint() throws Exception {
        String token = registerAndLogin("checkout-merge-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/v1/cart/merge")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GONE"))
                .andExpect(jsonPath("$.error.message").value(
                        "Guest cart merge is deprecated: guest carts are no longer stored server-side. "
                                + "Since 2026-04-01, guest carts live only in the client cache (10-day TTL). "
                                + "Please sign in to persist carts on the server."
                ));
    }

    private String registerAndLogin(String email) throws Exception {
        var registerRequest = new RegisterRequest(email, "password123", "John", "Doe", "fr");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        var loginRequest = new LoginRequest(email, "password123");
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(loginResponse);
        return json.path("data").path("accessToken").asText();
    }

    private UUID createProduct(String status, BigDecimal monthlyPrice, BigDecimal annualPrice) {
        CategoryJpaEntity category = categoryRepository.findByName("EDR")
                .orElseGet(() -> {
                    var cat = new CategoryJpaEntity();
                    cat.setId(UUID.randomUUID());
                    cat.setName("EDR");
                    cat.setFullName("Endpoint Detection and Response");
                    cat.setDescription("EDR solutions");
                    cat.setActive(true);
                    cat.setCreatedAt(Instant.now());
                    cat.setUpdatedAt(Instant.now());
                    return categoryRepository.saveAndFlush(cat);
                });

        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Checkout Product " + entity.getId());
        entity.setCategory(category);
        entity.setPriorityLevel(1);
        entity.setServiceDescription("Service description");
        entity.setTechnicalDescription("Technical description");
        entity.setMonthlyPrice(monthlyPrice);
        entity.setAnnualPrice(annualPrice);
        entity.setCurrency("EUR");
        entity.setPublished("PUBLISHED".equals(status));
        entity.setAvailable(true);
        entity.setFreeTrialDays(0);
        entity.setHighlightPoints(List.of());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return productRepository.saveAndFlush(entity).getId();
    }

    private void addLine(String token, UUID productId, int quantity) throws Exception {
        var request = new AddCartLineRequest(productId, BillingCycle.MONTHLY, quantity);
        mockMvc.perform(post("/api/v1/cart/lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private void unpublishProduct(UUID productId) {
        ProductJpaEntity entity = productRepository.findById(productId).orElseThrow();
        entity.setPublished(false);
        entity.setUpdatedAt(Instant.now());
        productRepository.saveAndFlush(entity);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
