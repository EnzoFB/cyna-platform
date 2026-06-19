package com.cyna.modules.subscription.integration;

import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.model.SubscriptionStatus;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.infrastructure.persistence.mapper.UserJpaMapper;
import com.cyna.modules.user.infrastructure.persistence.repository.SpringDataUserRepository;
import com.cyna.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validates the V14 single-write cancellation semantics:
 * <ul>
 *   <li>The cancel endpoint must call Stripe with {@code cancel_at_period_end=true}
 *       (so the customer keeps access through the paid period).</li>
 *   <li>It must return a projected state ({@code status=ACTIVE, autoRenew=false})
 *       for instant UX feedback.</li>
 *   <li>It must NOT mutate the local DB — the webhook handler is the only path
 *       that does. The DB stays unchanged until {@code customer.subscription.updated}
 *       arrives.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@SuppressWarnings("resource") // Testcontainers manages the container lifecycle.
class CancelSubscriptionIntegrationTest {

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

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserJpaMapper userMapper;
    @Autowired private SpringDataUserRepository userRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean
    private PaymentGatewayPort paymentGateway;

    private User user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = seedUser("cancel-" + UUID.randomUUID() + "@example.com");
        accessToken = jwtProvider.generateAccessToken(user);
    }

    @Test
    void cancel_request_calls_stripe_and_write_through_mirrors_db() throws Exception {
        Subscription sub = seedActiveSubscription("sub_stripe_to_cancel");
        // Stripe confirms cancel_at_period_end=true; status stays active until period end.
        when(paymentGateway.setSubscriptionCancelAtPeriodEnd("sub_stripe_to_cancel", true))
                .thenReturn(new PaymentGatewayPort.StripeSubscriptionState("active", true, null, null));

        mockMvc.perform(post("/api/v1/subscriptions/" + sub.getId() + "/cancel")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                // Projected state: status still ACTIVE (user keeps access until endAt),
                // autoRenew flipped to false (cancellation scheduled).
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.autoRenew").value(false));

        // Stripe was called with cancelAtPeriodEnd=true.
        verify(paymentGateway).setSubscriptionCancelAtPeriodEnd("sub_stripe_to_cancel", true);

        // Write-through: the DB is mirrored from Stripe's authoritative response within
        // the request — autoRenew=false is durable on refresh, with Stripe still the
        // source of truth. The subscription stays ACTIVE (access kept until period end);
        // the terminal CANCELLED state is still driven by the
        // customer.subscription.deleted webhook.
        Subscription latest = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(latest.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(latest.isAutoRenew()).isFalse();
        assertThat(latest.getCancelledAt()).isNotNull();
    }

    @Test
    void cancel_on_already_terminal_subscription_is_idempotent_and_skips_stripe() throws Exception {
        Subscription sub = seedActiveSubscription("sub_already_cancelled");
        Subscription fullyCancelled = sub.markFullyCancelled().getValue();
        subscriptionRepository.save(fullyCancelled);

        mockMvc.perform(post("/api/v1/subscriptions/" + sub.getId() + "/cancel")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Already terminal → Stripe is NOT called again.
        verify(paymentGateway, org.mockito.Mockito.never())
                .setSubscriptionCancelAtPeriodEnd(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void cancel_returns_403_when_subscription_belongs_to_another_user() throws Exception {
        User otherUser = seedUser("intruder-" + UUID.randomUUID() + "@example.com");
        Subscription sub = seedActiveSubscriptionFor(otherUser.getId(), "sub_other_user");

        mockMvc.perform(post("/api/v1/subscriptions/" + sub.getId() + "/cancel")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(paymentGateway, org.mockito.Mockito.never())
                .setSubscriptionCancelAtPeriodEnd(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyBoolean());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User seedUser(String email) {
        var u = User.register(
                Email.of(email),
                HashedPassword.of("$2a$10$dummyForTests" + UUID.randomUUID()),
                "Test", "User", "fr"
        );
        userRepository.saveAndFlush(userMapper.toJpa(u));
        return u;
    }

    private Subscription seedActiveSubscription(String stripeSubscriptionId) {
        return seedActiveSubscriptionFor(user.getId(), stripeSubscriptionId);
    }

    private Subscription seedActiveSubscriptionFor(UUID ownerId, String stripeSubscriptionId) {
        Ids ids = seedProductAndOrder(ownerId);
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        Subscription sub = Subscription.createActive(
                ownerId, ids.orderId(), UUID.randomUUID(), ids.productId(),
                "EDR Test", "EDR", BillingCycle.MONTHLY, 1,
                Money.of(BigDecimal.valueOf(99), "EUR"),
                start, end, end,
                stripeSubscriptionId, null
        );
        subscriptionRepository.save(sub);
        return sub;
    }

    private record Ids(UUID orderId, UUID productId) {}

    /**
     * Seeds a real product + order so the subscription FKs added in migration
     * V10 (subscription → order, subscription → product) are satisfied. The
     * SOC category {@code 0000…0001} is seeded by V1.
     */
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
                  (id, user_id, status, subtotal_amount, currency)
                VALUES (?, ?, 'PAID', 82.5000, 'EUR')
                """, orderId, ownerId);
        return new Ids(orderId, productId);
    }
}
