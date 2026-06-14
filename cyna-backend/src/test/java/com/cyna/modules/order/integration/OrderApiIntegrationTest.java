package com.cyna.modules.order.integration;

import com.cyna.modules.order.interfaces.rest.dto.request.CancelOrderRequest;
import com.cyna.modules.order.interfaces.rest.dto.request.CreateOrderRequest;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryTranslationJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductTranslationJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.cyna.modules.product.infrastructure.persistence.repository.SpringDataProductRepository;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = "app.security.rate-limit.enabled=false")
class OrderApiIntegrationTest {

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

    @Nested
    class CreateOrder {

        @Test
        void should_create_order_when_request_is_valid() throws Exception {
            String token = registerCustomerAndGetAccessToken("order-create-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();

            var request = new CreateOrderRequest(List.of(
                    new CreateOrderRequest.CreateOrderLineRequest(productId, BillingCycle.MONTHLY, 2)
            ));

            mockMvc.perform(post("/api/v1/orders")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isNotEmpty());
        }

        @Test
        void should_return_401_when_unauthenticated() throws Exception {
            UUID productId = createPublishedProduct();
            var request = new CreateOrderRequest(List.of(
                    new CreateOrderRequest.CreateOrderLineRequest(productId, BillingCycle.MONTHLY, 1)
            ));

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        void should_return_400_when_request_validation_fails() throws Exception {
            String token = registerCustomerAndGetAccessToken("order-validation-" + UUID.randomUUID() + "@example.com");

            var request = new CreateOrderRequest(List.of());

            mockMvc.perform(post("/api/v1/orders")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }

        @Test
        void should_return_422_when_product_does_not_exist() throws Exception {
            String token = registerCustomerAndGetAccessToken("order-missing-product-" + UUID.randomUUID() + "@example.com");

            var request = new CreateOrderRequest(List.of(
                    new CreateOrderRequest.CreateOrderLineRequest(UUID.randomUUID(), BillingCycle.MONTHLY, 1)
            ));

            mockMvc.perform(post("/api/v1/orders")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATION"));
        }
    }

    @Nested
    class GetOrderById {

        @Test
        void should_return_order_when_user_is_owner() throws Exception {
            String token = registerCustomerAndGetAccessToken("order-owner-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();
            UUID orderId = createOrder(token, productId);

            mockMvc.perform(get("/api/v1/orders/" + orderId)
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        void should_return_404_when_order_does_not_exist() throws Exception {
            String token = registerCustomerAndGetAccessToken("order-404-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID())
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        void should_return_403_when_order_belongs_to_another_user() throws Exception {
            String ownerToken = registerCustomerAndGetAccessToken("order-owner2-" + UUID.randomUUID() + "@example.com");
            String otherToken = registerCustomerAndGetAccessToken("order-other-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();
            UUID orderId = createOrder(ownerToken, productId);

            mockMvc.perform(get("/api/v1/orders/" + orderId)
                            .header("Authorization", bearer(otherToken)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }

        @Test
        void should_return_401_when_unauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    @Nested
    class ListOrders {

        @Test
        void should_list_orders_for_authenticated_user() throws Exception {
            String token = registerCustomerAndGetAccessToken("order-list-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();
            UUID orderId = createOrder(token, productId);

            mockMvc.perform(get("/api/v1/orders")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdAt,desc")
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.items[0].id").value(orderId.toString()))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        void should_fall_back_to_default_sort_when_sort_parameter_is_invalid() throws Exception {
            String token = registerCustomerAndGetAccessToken("order-sort-" + UUID.randomUUID() + "@example.com");

            // Tolerant parsing: an unknown sort field falls back to the default
            // order (created_at,desc) — never a 400.
            mockMvc.perform(get("/api/v1/orders")
                            .param("sort", "dropTable,desc")
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void should_return_401_when_unauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    @Nested
    class CancelOrder {

        @Test
        void should_cancel_order_when_owner_requests_it() throws Exception {
            String token = registerCustomerAndGetAccessToken("order-cancel-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();
            UUID orderId = createOrder(token, productId);

            mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CancelOrderRequest("Customer request"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(orderId.toString()));

            mockMvc.perform(get("/api/v1/orders/" + orderId)
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        }

        @Test
        void should_return_400_when_cancellation_reason_contains_html() throws Exception {
            String token = registerCustomerAndGetAccessToken("order-cancel-html-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();
            UUID orderId = createOrder(token, productId);

            mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CancelOrderRequest("<b>not allowed</b>"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }

        @Test
        void should_return_404_when_cancelling_unknown_order() throws Exception {
            String token = registerCustomerAndGetAccessToken("order-cancel-404-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/cancel")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CancelOrderRequest("No longer needed"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        void should_return_403_when_cancelling_order_of_another_user() throws Exception {
            String ownerToken = registerCustomerAndGetAccessToken("order-cancel-owner-" + UUID.randomUUID() + "@example.com");
            String otherToken = registerCustomerAndGetAccessToken("order-cancel-other-" + UUID.randomUUID() + "@example.com");
            UUID productId = createPublishedProduct();
            UUID orderId = createOrder(ownerToken, productId);

            mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                            .header("Authorization", bearer(otherToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CancelOrderRequest("Not my order"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }

        @Test
        void should_return_401_when_unauthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CancelOrderRequest("Attempt"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
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

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(root.at("/data").asText());
    }

    private String registerCustomerAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, "password123", "Order", "Tester", "Acme", "fr", true))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/accessToken")
                .asText();
    }

    private UUID createPublishedProduct() {
        CategoryJpaEntity category = categoryRepository.findByName("ORDER_TEST")
                .orElseGet(() -> {
                    CategoryJpaEntity cat = new CategoryJpaEntity();
                    cat.setId(UUID.randomUUID());
                    cat.setName("ORDER_TEST");
                    cat.setActive(true);
                    cat.setCreatedAt(Instant.now());
                    cat.setUpdatedAt(Instant.now());
                    cat.setTranslations(Set.of(
                            CategoryTranslationJpaEntity.of(cat, "fr", "Order Test", "Order test category")
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
                        "Order test product " + product.getId(),
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
