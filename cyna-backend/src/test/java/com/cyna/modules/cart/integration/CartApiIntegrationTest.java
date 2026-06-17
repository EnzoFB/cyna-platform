package com.cyna.modules.cart.integration;

import com.cyna.shared.domain.BillingCycle;
import com.cyna.modules.cart.interfaces.dto.request.AddCartLineRequest;
import com.cyna.modules.cart.interfaces.dto.request.UpdateCartLineBillingCycleRequest;
import com.cyna.modules.cart.interfaces.dto.request.UpdateCartLineQuantityRequest;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryTranslationJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductTranslationJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.cyna.modules.product.infrastructure.persistence.repository.SpringDataProductRepository;
import com.cyna.modules.user.domain.repository.EmailVerificationTokenRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.modules.user.interfaces.dto.request.ConfirmEmailRequest;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import com.cyna.testsupport.EmailVerificationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CartApiIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository verificationTokenRepository;

    @Nested
    class GetCart {

        @Test
        void should_get_empty_cart_for_new_user() throws Exception {
            String token = registerAndGetAccessToken("cart-empty-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(get("/api/v1/cart")
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.lines.length()").value(0));
        }

        @Test
        void should_return_401_for_cart_without_token() throws Exception {
            mockMvc.perform(get("/api/v1/cart"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    @Nested
    class CartLines {

        @Test
        void should_add_update_and_delete_cart_line() throws Exception {
            String token = registerAndGetAccessToken("cart-flow-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();

            UUID lineId = addLineAndExtractLineId(token, productId, BillingCycle.MONTHLY, 1);

            mockMvc.perform(patch("/api/v1/cart/lines/" + lineId + "/quantity")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateCartLineQuantityRequest(3))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.lines[0].quantity").value(3));

            mockMvc.perform(patch("/api/v1/cart/lines/" + lineId + "/billing-cycle")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateCartLineBillingCycleRequest(BillingCycle.ANNUAL))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.lines[0].billingCycle").value("ANNUAL"));

            mockMvc.perform(delete("/api/v1/cart/lines/" + lineId)
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.lines.length()").value(0));
        }

        @Test
        void should_return_404_when_updating_unknown_cart_line() throws Exception {
            String token = registerAndGetAccessToken("cart-line-404-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(patch("/api/v1/cart/lines/" + UUID.randomUUID() + "/quantity")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateCartLineQuantityRequest(2))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        void should_return_400_when_adding_line_with_invalid_quantity() throws Exception {
            String token = registerAndGetAccessToken("cart-line-invalid-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();

            mockMvc.perform(post("/api/v1/cart/lines")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AddCartLineRequest(productId, BillingCycle.MONTHLY, 0))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }

        @Test
        void should_return_401_when_adding_line_without_token() throws Exception {
            mockMvc.perform(post("/api/v1/cart/lines")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AddCartLineRequest(UUID.randomUUID(), BillingCycle.MONTHLY, 1))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    private UUID addLineAndExtractLineId(String token, UUID productId, BillingCycle cycle, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/cart/lines")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddCartLineRequest(productId, cycle, quantity))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.path("lines").isArray()).isTrue();
        return UUID.fromString(data.path("lines").get(0).path("lineId").asText());
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        // Registration now creates a PENDING account without tokens; activate it
        // via confirm-email (seeding a known verification token) to get a session.
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, "password123", "Cart", "Tester", "Acme", "fr", true))))
                .andExpect(status().isCreated());

        String raw = EmailVerificationTestSupport.seedVerificationToken(
                userRepository, verificationTokenRepository, email);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmEmailRequest(raw))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/accessToken")
                .asText();
    }

    private UUID createPublishedProduct() {
        CategoryJpaEntity category = categoryRepository.findByName("CART_TEST")
                .orElseGet(() -> {
                    CategoryJpaEntity cat = new CategoryJpaEntity();
                    cat.setId(UUID.randomUUID());
                    cat.setName("CART_TEST");
                    cat.setActive(true);
                    cat.setCreatedAt(Instant.now());
                    cat.setUpdatedAt(Instant.now());
                    cat.setTranslations(Set.of(
                            CategoryTranslationJpaEntity.of(cat, "fr", "Cart Test", "Cart test category")
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
                        "Cart test product " + product.getId(),
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
