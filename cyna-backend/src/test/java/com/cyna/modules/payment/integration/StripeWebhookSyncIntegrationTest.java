package com.cyna.modules.payment.integration;

import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort.StripeWebhookEvent;
import com.cyna.modules.subscription.domain.model.BillingCycle;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.model.SubscriptionStatus;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.infrastructure.persistence.mapper.UserJpaMapper;
import com.cyna.modules.user.infrastructure.persistence.repository.SpringDataUserRepository;
import com.cyna.shared.domain.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validates the anti-drift webhook path. Whether the change originated from our
 * own backend (cancel endpoint, auto-renew toggle), from the Stripe dashboard,
 * or from the Customer Portal — Stripe always emits
 * {@code customer.subscription.updated}, and the local DB must reconcile to
 * Stripe's authoritative state through that one path.
 *
 * <p>The webhook signature check is the only authentication on this endpoint
 * (anonymous from Spring's POV). We bypass the HMAC by mocking the gateway's
 * parser; signature verification itself is covered by the unit test on
 * {@code StripePaymentAdapter.parseWebhookEvent}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@SuppressWarnings("resource") // Testcontainers manages the container lifecycle.
class StripeWebhookSyncIntegrationTest {

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
    @Autowired private UserJpaMapper userMapper;
    @Autowired private SpringDataUserRepository userRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private PaymentGatewayPort paymentGateway;

    @Test
    void customer_subscription_updated_with_cancel_at_period_end_disables_auto_renew_locally() throws Exception {
        Subscription sub = seedActiveSubscription("sub_cancel_via_dashboard");
        assertThat(sub.isAutoRenew()).isTrue();
        assertThat(sub.getCancelledAt()).isNull();

        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(new StripeWebhookEvent(
                        "customer.subscription.updated",
                        null, "sub_cancel_via_dashboard", "cus_x",
                        null, null, null,
                        "active", Boolean.TRUE, sub.getEndAt(), null
                ));

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("Stripe-Signature", "t=any,v1=fake_we_mock_parsing")
                        .content("{}"))
                .andExpect(status().isOk());

        Subscription synced = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(synced.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE); // user keeps access
        assertThat(synced.isAutoRenew()).isFalse();                          // but won't renew
        assertThat(synced.getCancelledAt()).isNotNull();
    }

    @Test
    void customer_subscription_deleted_transitions_local_state_to_cancelled() throws Exception {
        Subscription sub = seedActiveSubscription("sub_to_terminate");

        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(new StripeWebhookEvent(
                        "customer.subscription.deleted",
                        null, "sub_to_terminate", "cus_x",
                        null, null, null,
                        "canceled", null, null, Instant.now()
                ));

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("Stripe-Signature", "t=any,v1=fake_we_mock_parsing")
                        .content("{}"))
                .andExpect(status().isOk());

        Subscription cancelled = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    void customer_subscription_updated_with_past_due_status_marks_local_past_due() throws Exception {
        Subscription sub = seedActiveSubscription("sub_to_past_due");

        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(new StripeWebhookEvent(
                        "customer.subscription.updated",
                        null, "sub_to_past_due", "cus_x",
                        null, null, null,
                        "past_due", Boolean.FALSE, sub.getEndAt(), null
                ));

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("Stripe-Signature", "t=any,v1=fake_we_mock_parsing")
                        .content("{}"))
                .andExpect(status().isOk());

        Subscription updated = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void unknown_event_types_are_silently_acked_so_stripe_does_not_retry() throws Exception {
        Subscription sub = seedActiveSubscription("sub_irrelevant_event");

        when(paymentGateway.parseWebhookEvent(anyString(), anyString()))
                .thenReturn(new StripeWebhookEvent(
                        "customer.tax_id.updated",
                        null, null, "cus_x",
                        null, null, null,
                        null, null, null, null
                ));

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("Stripe-Signature", "t=any,v1=fake_we_mock_parsing")
                        .content("{}"))
                .andExpect(status().isOk());

        // Local state untouched.
        Subscription untouched = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(untouched.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(untouched.isAutoRenew()).isTrue();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Subscription seedActiveSubscription(String stripeSubscriptionId) {
        UUID ownerId = seedUser().getId();
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        Subscription sub = Subscription.createActive(
                ownerId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "EDR Test", "EDR", BillingCycle.MONTHLY, 1,
                Money.of(BigDecimal.valueOf(99), "EUR"),
                start, end, end,
                stripeSubscriptionId, null
        );
        subscriptionRepository.save(sub);
        return sub;
    }

    private User seedUser() {
        var u = User.register(
                Email.of("webhook-" + UUID.randomUUID() + "@example.com"),
                HashedPassword.of("$2a$10$dummyForTests" + UUID.randomUUID()),
                "Test", "User", "fr"
        );
        userRepository.saveAndFlush(userMapper.toJpa(u));
        return u;
    }

    @SuppressWarnings("unused") // surface helper for future tests
    private List<Subscription> subsForStripe(String stripeSubscriptionId) {
        return subscriptionRepository.findAllByStripeSubscriptionId(stripeSubscriptionId);
    }
}
