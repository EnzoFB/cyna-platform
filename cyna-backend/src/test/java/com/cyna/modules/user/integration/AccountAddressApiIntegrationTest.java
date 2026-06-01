package com.cyna.modules.user.integration;

import com.cyna.modules.user.interfaces.dto.request.AddressRequest;
import com.cyna.modules.user.interfaces.dto.request.ChangePasswordRequest;
import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import com.cyna.modules.user.interfaces.dto.request.RequestEmailChangeRequest;
import com.cyna.modules.user.interfaces.dto.request.UpdateProfileRequest;
import com.cyna.shared.application.notification.MailService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AccountAddressApiIntegrationTest {

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

    @MockitoBean
    private MailService mailService;

    @Nested
    class Account {

        @Test
        void should_get_current_user_profile() throws Exception {
            Session session = registerAndExtract("account-profile-" + UUID.randomUUID() + "@example.com", "password123");

            mockMvc.perform(get("/api/v1/account")
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value(session.email()));
        }

        @Test
        void should_return_401_when_getting_profile_without_token() throws Exception {
            mockMvc.perform(get("/api/v1/account"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        void should_check_email_availability() throws Exception {
            String usedEmail = "account-check-" + UUID.randomUUID() + "@example.com";
            registerAndExtract(usedEmail, "password123");

            mockMvc.perform(get("/api/v1/account/check-email").param("email", usedEmail))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true));

            mockMvc.perform(get("/api/v1/account/check-email").param("email", "unknown-" + UUID.randomUUID() + "@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(false));
        }

        @Test
        void should_update_profile() throws Exception {
            Session session = registerAndExtract("account-update-" + UUID.randomUUID() + "@example.com", "password123");

            mockMvc.perform(patch("/api/v1/account/profile")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateProfileRequest("Updated", "User", "NewCo"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get("/api/v1/account")
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.firstName").value("Updated"))
                    .andExpect(jsonPath("$.data.lastName").value("User"))
                    .andExpect(jsonPath("$.data.company").value("NewCo"));
        }

        @Test
        void should_request_email_change() throws Exception {
            Session session = registerAndExtract("account-email-change-" + UUID.randomUUID() + "@example.com", "password123");

            mockMvc.perform(post("/api/v1/account/email/request-change")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RequestEmailChangeRequest("new-" + UUID.randomUUID() + "@example.com", "fr"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void should_reject_invalid_email_change_token() throws Exception {
            mockMvc.perform(post("/api/v1/account/email/confirm").param("token", "invalid-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void should_export_my_data() throws Exception {
            Session session = registerAndExtract("account-export-" + UUID.randomUUID() + "@example.com", "password123");

            mockMvc.perform(get("/api/v1/account/export")
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"cyna-my-data.json\""));
        }

        @Test
        void should_change_password_with_valid_current_password() throws Exception {
            Session session = registerAndExtract("account-change-pwd-" + UUID.randomUUID() + "@example.com", "password123");

            mockMvc.perform(patch("/api/v1/account/password")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ChangePasswordRequest("password123", "newPassword456"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        void should_reject_password_change_with_wrong_current_password() throws Exception {
            Session session = registerAndExtract("account-change-pwd-fail-" + UUID.randomUUID() + "@example.com", "password123");

            mockMvc.perform(patch("/api/v1/account/password")
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ChangePasswordRequest("wrong-current", "newPassword456"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void should_delete_my_account() throws Exception {
            Session session = registerAndExtract("account-delete-" + UUID.randomUUID() + "@example.com", "password123");

            mockMvc.perform(delete("/api/v1/account")
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    class Addresses {

        @Test
        void should_crud_addresses_and_set_default() throws Exception {
            Session session = registerAndExtract("address-flow-" + UUID.randomUUID() + "@example.com", "password123");

            UUID firstId = createAddress(session.accessToken(), sampleAddress("Home"));
            UUID secondId = createAddress(session.accessToken(), sampleAddress("Office"));

            mockMvc.perform(get("/api/v1/account/addresses")
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2));

            AddressRequest updated = new AddressRequest(
                    "Alice", "Doe", "Home updated", "1 Rue A", "Apt 2",
                    "75001", "Paris", "IDF", "FR", "+33102030405", "Acme", "FR123"
            );
            mockMvc.perform(put("/api/v1/account/addresses/" + firstId)
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updated)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(patch("/api/v1/account/addresses/" + secondId + "/default")
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            MvcResult afterDefault = mockMvc.perform(get("/api/v1/account/addresses")
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode addresses = objectMapper.readTree(afterDefault.getResponse().getContentAsString()).path("data");
            boolean secondIsDefault = false;
            for (JsonNode address : addresses) {
                if (secondId.toString().equals(address.path("id").asText())) {
                    secondIsDefault = address.path("isDefault").asBoolean();
                }
            }
            assertThat(secondIsDefault).isTrue();

            mockMvc.perform(delete("/api/v1/account/addresses/" + firstId)
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get("/api/v1/account/addresses")
                            .header("Authorization", bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }

        @Test
        void should_return_400_when_updating_unknown_address() throws Exception {
            Session session = registerAndExtract("address-unknown-" + UUID.randomUUID() + "@example.com", "password123");

            mockMvc.perform(put("/api/v1/account/addresses/" + UUID.randomUUID())
                            .header("Authorization", bearer(session.accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleAddress("Unknown"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void should_return_401_when_listing_addresses_without_token() throws Exception {
            mockMvc.perform(get("/api/v1/account/addresses"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        void should_return_401_when_creating_address_without_token() throws Exception {
            mockMvc.perform(post("/api/v1/account/addresses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleAddress("Home"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        void should_return_401_when_deleting_address_without_token() throws Exception {
            mockMvc.perform(delete("/api/v1/account/addresses/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    private UUID createAddress(String accessToken, AddressRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/account/addresses")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).at("/data").asText());
    }

    private AddressRequest sampleAddress(String label) {
        return new AddressRequest(
                "John", "Doe", label, "10 Rue de la Paix", null,
                "75002", "Paris", "IDF", "FR", "+33102030405", "Acme", null
        );
    }

    private Session registerAndExtract(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, password, "Test", "User", "Acme", "fr", true))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return new Session(email, data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Session(String email, String accessToken, String refreshToken) {
    }
}
