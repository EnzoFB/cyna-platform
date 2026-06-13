package com.cyna.modules.payment.integration;

import com.cyna.modules.order.interfaces.rest.dto.request.CreateOrderRequest;
import com.cyna.modules.order.interfaces.rest.dto.request.CreateOrderRequest.CreateOrderLineRequest;
import com.cyna.modules.payment.domain.model.Payment;
import com.cyna.modules.payment.domain.model.PaymentStatus;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort.SetupIntentResult;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort.SubscriptionForLineResult;
import com.cyna.modules.payment.domain.repository.PaymentRepository;
import com.cyna.modules.payment.interfaces.rest.dto.request.FinalizePaymentRequest;
import com.cyna.modules.payment.interfaces.rest.dto.request.InitiatePaymentRequest;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.cyna.modules.product.infrastructure.persistence.repository.SpringDataProductRepository;
import com.cyna.modules.subscription.domain.model.BillingCycle;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.model.SubscriptionStatus;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.infrastructure.persistence.entity.UserJpaEntity;
import com.cyna.modules.user.infrastructure.persistence.mapper.UserJpaMapper;
import com.cyna.modules.user.infrastructure.persistence.repository.SpringDataUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the V14 multi-product mixed-cycle checkout flow:
 * <ol>
 *   <li>{@code POST /orders}    — Order with 2 lines: one MONTHLY, one ANNUAL.</li>
 *   <li>{@code POST /payments/initiate} — Backend creates a SetupIntent (mocked).</li>
 *   <li>{@code POST /payments/finalize} — Backend creates one Stripe Subscription
 *       <strong>per OrderLine</strong> with the PaymentMethod the frontend collected.</li>
 * </ol>
 *
 * <p>Real Stripe is mocked out at the {@link PaymentGatewayPort} boundary so this
 * test runs offline in CI. Every assertion targets observable state we'd query in
 * production (DB rows, HTTP response), not implementation internals.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@SuppressWarnings("resource") // Testcontainers manages the container lifecycle.
class MultiCycleCheckoutIntegrationTest {

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
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserJpaMapper userMapper;
    @Autowired private SpringDataUserRepository userRepository;
    @Autowired private SpringDataCategoryRepository categoryRepository;
    @Autowired private SpringDataProductRepository productRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private PaymentRepository paymentRepository;

    @MockitoBean
    private PaymentGatewayPort paymentGateway;

    private String accessToken;

    @BeforeEach
    void setUp() {
        var user = seedUser("checkout-" + UUID.randomUUID() + "@example.com");
        accessToken = jwtProvider.generateAccessToken(user);

        // Stripe SetupIntent: any call returns a deterministic, distinct fake.
        when(paymentGateway.createSetupIntent(any()))
                .thenReturn(new SetupIntentResult("seti_test", "seti_test_secret"));
        when(paymentGateway.createCustomerForUser(any(), any()))
                .thenReturn("cus_test_" + UUID.randomUUID());
        // Per-line Stripe subscription creation: return a NEW stripe sub id every
        // call so the test can assert subs are distinct.
        when(paymentGateway.createSubscriptionForLine(
                any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any()
        )).thenAnswer(inv -> new SubscriptionForLineResult(
                "sub_" + UUID.randomUUID(), "active"
        ));
    }

    @Test
    void should_create_one_stripe_subscription_per_order_line_on_mixed_cycle_checkout() throws Exception {
        UUID socId = createProduct("SOC", BigDecimal.valueOf(300), BigDecimal.valueOf(3000));
        UUID edrId = createProduct("EDR", BigDecimal.valueOf(120), BigDecimal.valueOf(1200));

        // ---- 1. Create the order with MIXED cycles --------------------
        UUID orderId = createOrder(List.of(
                new CreateOrderLineRequest(socId, BillingCycle.MONTHLY, 2),  // 2 × 300 monthly
                new CreateOrderLineRequest(edrId, BillingCycle.ANNUAL,  1)   // 1 × 1200 annual
        ));

        // ---- 2. Initiate: SetupIntent client_secret is returned --------
        mockMvc.perform(post("/api/v1/payments/initiate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InitiatePaymentRequest(orderId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.setupIntentClientSecret").value("seti_test_secret"));

        // ---- 3. Finalize with a faked PaymentMethod -------------------
        String finalizeBody = mockMvc.perform(post("/api/v1/payments/finalize")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FinalizePaymentRequest(orderId, "pm_card_visa", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lines.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        JsonNode lines = objectMapper.readTree(finalizeBody).path("data").path("lines");
        String firstSubId  = lines.get(0).get("stripeSubscriptionId").asText();
        String secondSubId = lines.get(1).get("stripeSubscriptionId").asText();
        // Each line must end up with its own Stripe Subscription id — that's the
        // whole point of the V14 refactor.
        assertThat(firstSubId).isNotEqualTo(secondSubId);

        // ---- 4. Verify DB state ---------------------------------------
        List<Subscription> firstSubMirror  = subscriptionRepository.findAllByStripeSubscriptionId(firstSubId);
        List<Subscription> secondSubMirror = subscriptionRepository.findAllByStripeSubscriptionId(secondSubId);
        assertThat(firstSubMirror).hasSize(1);
        assertThat(secondSubMirror).hasSize(1);

        List<Subscription> both = List.of(firstSubMirror.get(0), secondSubMirror.get(0));
        assertThat(both)
                .allSatisfy(s -> {
                    assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
                    assertThat(s.getOrderLineId()).isNotNull();
                });
        // The two subs span both cycles — no degenerate single-cycle Stripe sub.
        assertThat(both)
                .extracting(s -> s.getBillingCycle().name())
                .containsExactlyInAnyOrder("MONTHLY", "ANNUAL");

        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getStripeSetupIntentId()).isEqualTo("seti_test");
    }

    @Test
    void should_be_idempotent_when_finalize_is_called_twice() throws Exception {
        UUID productId = createProduct("EDR", BigDecimal.valueOf(100), BigDecimal.valueOf(1000));
        UUID orderId = createOrder(List.of(new CreateOrderLineRequest(productId, BillingCycle.MONTHLY, 1)));

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InitiatePaymentRequest(orderId))))
                .andExpect(status().isOk());

        // First finalize succeeds — capture the Stripe sub id it created.
        String firstResponse = mockMvc.perform(post("/api/v1/payments/finalize")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FinalizePaymentRequest(orderId, "pm_card_visa", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lines.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        String createdSubId = objectMapper.readTree(firstResponse)
                .path("data").path("lines").get(0).path("stripeSubscriptionId").asText();

        // Second finalize on an already-SUCCEEDED Payment must return 200 with an
        // empty per-line breakdown (idempotent echo), not 409. The user's network
        // could retry — we must not blow up.
        mockMvc.perform(post("/api/v1/payments/finalize")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FinalizePaymentRequest(orderId, "pm_card_visa", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lines.length()").value(0));

        // Exactly one local subscription bound to that Stripe sub — no duplicate
        // created by the retry.
        assertThat(subscriptionRepository.findAllByStripeSubscriptionId(createdSubId)).hasSize(1);
    }

    @Test
    void should_forward_the_b2b_vat_number_from_the_request_to_the_payment_gateway() throws Exception {
        UUID edrId = createProduct("EDR", BigDecimal.valueOf(100), BigDecimal.valueOf(1000));
        UUID orderId = createOrder(List.of(new CreateOrderLineRequest(edrId, BillingCycle.MONTHLY, 1)));

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InitiatePaymentRequest(orderId))))
                .andExpect(status().isOk());

        // Finalize WITH a B2B VAT number in the body.
        mockMvc.perform(post("/api/v1/payments/finalize")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FinalizePaymentRequest(orderId, "pm_card_visa", "DE123456789"))))
                .andExpect(status().isOk());

        // The VAT number entered at checkout must reach the gateway verbatim —
        // that is what lets Stripe Tax apply the intra-EU reverse charge. The
        // tax location is pinned with the same PaymentMethod used to charge.
        verify(paymentGateway)
                .updateCustomerTaxLocation(anyString(), eq("pm_card_visa"), eq("DE123456789"));
    }

    @Test
    void should_pass_null_vat_number_to_the_gateway_for_a_b2c_checkout() throws Exception {
        UUID edrId = createProduct("EDR", BigDecimal.valueOf(100), BigDecimal.valueOf(1000));
        UUID orderId = createOrder(List.of(new CreateOrderLineRequest(edrId, BillingCycle.MONTHLY, 1)));

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InitiatePaymentRequest(orderId))))
                .andExpect(status().isOk());

        // Finalize with NO VAT number (B2C).
        mockMvc.perform(post("/api/v1/payments/finalize")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FinalizePaymentRequest(orderId, "pm_card_visa", null))))
                .andExpect(status().isOk());

        // No VAT number → null reaches the gateway → standard destination VAT.
        verify(paymentGateway)
                .updateCustomerTaxLocation(anyString(), eq("pm_card_visa"), isNull());
    }

    // --------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------

    private String bearer() {
        return "Bearer " + accessToken;
    }

    private User seedUser(String email) {
        var user = User.register(
                Email.of(email),
                HashedPassword.of("$2a$10$dummyHashOnlyForTests" + UUID.randomUUID()),
                "Test", "User", "fr"
        );
        UserJpaEntity entity = userMapper.toJpa(user);
        userRepository.saveAndFlush(entity);
        return user;
    }

    private UUID createProduct(String categoryName, BigDecimal monthly, BigDecimal annual) {
        var category = categoryRepository.findByName(categoryName)
                .orElseGet(() -> {
                    var cat = new CategoryJpaEntity();
                    cat.setId(UUID.randomUUID());
                    cat.setName(categoryName);
                    cat.setFullName(categoryName + " full");
                    cat.setDescription(categoryName + " desc");
                    cat.setActive(true);
                    cat.setCreatedAt(Instant.now());
                    cat.setUpdatedAt(Instant.now());
                    return categoryRepository.saveAndFlush(cat);
                });

        var product = new ProductJpaEntity();
        product.setId(UUID.randomUUID());
        product.setName(categoryName + " " + UUID.randomUUID());
        product.setCategory(category);
        product.setPriorityLevel(1);
        product.setServiceDescription("svc");
        product.setTechnicalDescription("tech");
        product.setMonthlyPrice(monthly);
        product.setAnnualPrice(annual);
        product.setCurrency("EUR");
        product.setPublished(true);
        product.setAvailable(true);
        product.setFreeTrialDays(0);
        product.setHighlightPoints(List.of());
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        return productRepository.saveAndFlush(product).getId();
    }

    private UUID createOrder(List<CreateOrderLineRequest> lines) throws Exception {
        String body = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(lines))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).path("data").asText());
    }
}
