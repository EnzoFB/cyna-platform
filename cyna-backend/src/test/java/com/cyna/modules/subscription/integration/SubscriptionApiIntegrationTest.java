package com.cyna.modules.subscription.integration;

import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.modules.subscription.interfaces.rest.dto.request.UpdateSubscriptionAutoRenewRequest;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.infrastructure.persistence.mapper.UserJpaMapper;
import com.cyna.modules.user.infrastructure.persistence.repository.SpringDataUserRepository;
import com.cyna.shared.domain.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@SuppressWarnings("resource")
class SubscriptionApiIntegrationTest {

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
    private JwtProvider jwtProvider;

    @Autowired
    private UserJpaMapper userMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private PaymentGatewayPort paymentGateway;

    private User owner;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        owner = seedUser("subscription-owner-" + UUID.randomUUID() + "@example.com");
        ownerToken = jwtProvider.generateAccessToken(owner);
    }

    @Test
    void should_get_subscription_by_id_for_owner() throws Exception {
        Subscription sub = seedActiveSubscriptionFor(owner.getId(), "sub-owner-get");

        mockMvc.perform(get("/api/v1/subscriptions/" + sub.getId())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(sub.getId().toString()));
    }

    @Test
    void should_return_404_when_subscription_does_not_exist() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/" + UUID.randomUUID())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void should_return_403_when_subscription_belongs_to_another_user() throws Exception {
        User other = seedUser("subscription-other-" + UUID.randomUUID() + "@example.com");
        Subscription sub = seedActiveSubscriptionFor(other.getId(), "sub-other-get");

        mockMvc.perform(get("/api/v1/subscriptions/" + sub.getId())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void should_list_subscriptions_for_owner() throws Exception {
        seedActiveSubscriptionFor(owner.getId(), "sub-owner-list");

        mockMvc.perform(get("/api/v1/subscriptions")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    void should_return_400_when_list_sort_is_invalid() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions")
                        .param("sort", "badfield,desc")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SORT"));
    }

    @Test
    void should_update_auto_renew_and_call_payment_gateway() throws Exception {
        Subscription sub = seedActiveSubscriptionFor(owner.getId(), "sub-toggle-auto-renew");

        mockMvc.perform(put("/api/v1/subscriptions/" + sub.getId() + "/auto-renew")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateSubscriptionAutoRenewRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.autoRenew").value(false));

        verify(paymentGateway).setSubscriptionCancelAtPeriodEnd("sub-toggle-auto-renew", true);
    }

    @Test
    void should_return_400_when_auto_renew_payload_is_invalid() throws Exception {
        Subscription sub = seedActiveSubscriptionFor(owner.getId(), "sub-toggle-invalid");

        mockMvc.perform(put("/api/v1/subscriptions/" + sub.getId() + "/auto-renew")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void should_return_403_when_updating_auto_renew_on_other_user_subscription() throws Exception {
        User other = seedUser("subscription-other-update-" + UUID.randomUUID() + "@example.com");
        Subscription sub = seedActiveSubscriptionFor(other.getId(), "sub-other-update");

        mockMvc.perform(put("/api/v1/subscriptions/" + sub.getId() + "/auto-renew")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateSubscriptionAutoRenewRequest(false))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(paymentGateway, never()).setSubscriptionCancelAtPeriodEnd(
                ArgumentMatchers.eq("sub-other-update"),
                ArgumentMatchers.anyBoolean()
        );
    }

    @Test
    void should_return_404_when_updating_auto_renew_on_unknown_subscription() throws Exception {
        mockMvc.perform(put("/api/v1/subscriptions/" + UUID.randomUUID() + "/auto-renew")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateSubscriptionAutoRenewRequest(false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void should_return_401_when_listing_subscriptions_without_token() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void should_return_401_when_getting_subscription_without_token() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void should_return_401_when_updating_auto_renew_without_token() throws Exception {
        mockMvc.perform(put("/api/v1/subscriptions/" + UUID.randomUUID() + "/auto-renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateSubscriptionAutoRenewRequest(false))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private User seedUser(String email) {
        var user = User.register(
                Email.of(email),
                HashedPassword.of("$2a$10$dummyHashOnlyForTests" + UUID.randomUUID()),
                "Test",
                "User",
                "fr"
        );
        userRepository.saveAndFlush(userMapper.toJpa(user));
        return user;
    }

    private Subscription seedActiveSubscriptionFor(UUID ownerId, String stripeSubscriptionId) {
        Ids ids = seedProductAndOrder(ownerId);
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        Subscription subscription = Subscription.createActive(
                ownerId,
                ids.orderId(),
                UUID.randomUUID(),
                ids.productId(),
                "EDR Test",
                "EDR",
                BillingCycle.MONTHLY,
                1,
                Money.of(BigDecimal.valueOf(99), "EUR"),
                start,
                end,
                end,
                stripeSubscriptionId,
                null
        );
        subscriptionRepository.save(subscription);
        return subscription;
    }

    private Ids seedProductAndOrder(UUID ownerId) {
        UUID productId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product_schema.products
                  (id, category_id, monthly_price, annual_price)
                VALUES (?, '00000000-0000-0000-0000-000000000001', 99.0000, 990.0000)
                """, productId);

        UUID orderId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO order_schema.orders
                  (id, user_id, status, subtotal_amount, vat_amount, total_amount, currency)
                VALUES (?, ?, 'PAID', 82.5000, 16.5000, 99.0000, 'EUR')
                """, orderId, ownerId);

        return new Ids(orderId, productId);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Ids(UUID orderId, UUID productId) {
    }
}
