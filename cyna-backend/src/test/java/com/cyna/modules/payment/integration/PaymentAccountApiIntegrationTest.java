package com.cyna.modules.payment.integration;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PaymentAccountApiIntegrationTest {

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

    @Nested
    class PaymentMethods {

        @Test
        void should_list_empty_payment_methods_when_user_has_no_stripe_customer() throws Exception {
            Session session = registerAndExtract("pm-list-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(get("/api/v1/account/payment-methods")
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        void should_return_404_when_saving_payment_method_without_stripe_customer() throws Exception {
            Session session = registerAndExtract("pm-save-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/account/payment-methods")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"stripePaymentMethodId\":\"pm_card_visa\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("NO_STRIPE_CUSTOMER"));
        }

        @Test
        void should_return_400_when_saving_payment_method_with_invalid_payload() throws Exception {
            Session session = registerAndExtract("pm-save-invalid-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/account/payment-methods")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"stripePaymentMethodId\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }

        @Test
        void should_return_401_for_payment_methods_without_token() throws Exception {
            mockMvc.perform(get("/api/v1/account/payment-methods"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    @Nested
    class Invoices {

        @Test
        void should_list_empty_invoices_when_user_has_no_stripe_customer() throws Exception {
            Session session = registerAndExtract("invoices-list-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(get("/api/v1/account/invoices")
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        void should_return_401_for_invoices_without_token() throws Exception {
            mockMvc.perform(get("/api/v1/account/invoices"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    @Nested
    class ConsentLog {

        @Test
        void should_log_payment_method_consent() throws Exception {
            Session session = registerAndExtract("consent-log-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/account/consent-log/payment-method")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"stripePaymentMethodId\":\"pm_card_visa\",\"labelVersion\":\"v1\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void should_return_400_for_invalid_consent_payload() throws Exception {
            Session session = registerAndExtract("consent-log-invalid-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/account/consent-log/payment-method")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"stripePaymentMethodId\":\"pm_card_visa\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }
    }

    @Nested
    class Payments {

        @Test
        void should_return_404_when_getting_payment_for_unknown_order() throws Exception {
            Session session = registerAndExtract("payment-get-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(get("/api/v1/payments/order/" + UUID.randomUUID())
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("PAYMENT_NOT_FOUND"));
        }

        @Test
        void should_return_404_when_opening_billing_portal_without_stripe_customer() throws Exception {
            Session session = registerAndExtract("payment-portal-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/payments/billing-portal")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"returnUrl\":\"https://app.cyna.test/account\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("NO_STRIPE_CUSTOMER"));
        }

        @Test
        void should_return_400_when_billing_portal_payload_is_invalid() throws Exception {
            Session session = registerAndExtract("payment-portal-invalid-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/payments/billing-portal")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"returnUrl\":\"not-a-url\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }
    }

    private Session registerAndExtract(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, "password123", "Test", "User", "Acme", "fr", true))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return new Session(data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Session(String accessToken, String refreshToken) {
    }
}
