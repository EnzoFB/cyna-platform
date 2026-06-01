package com.cyna.integration;

import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.Role;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.notification.MailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = "app.security.rate-limit.enabled=false")
class CatalogAdminSmokeIntegrationTest {

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
    private UserRepository userRepository;

    @MockitoBean
    private MailService mailService;

    @Nested
    class PublicEndpoints {

        @Test
        void should_expose_public_catalog_endpoints() throws Exception {
            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get("/api/v1/products/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get("/api/v1/categories/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

            mockMvc.perform(get("/api/v1/offers/promotions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get("/api/v1/offers/promotions/fixed-text"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void should_accept_contact_form_and_validate_invalid_payload() throws Exception {
            mockMvc.perform(post("/api/v1/contact")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"John\",\"email\":\"john@example.com\",\"subject\":\"Help\",\"message\":\"Hello\",\"lang\":\"fr\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(post("/api/v1/contact")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }
    }

    @Nested
    class AdminAuthorization {

        @Test
        void should_require_authentication_for_admin_endpoints() throws Exception {
            mockMvc.perform(get("/api/v1/admin/promotions"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

            mockMvc.perform(get("/api/v1/admin/dashboard"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        void should_forbid_customer_on_admin_endpoints() throws Exception {
            String customerToken = registerCustomerAndGetAccessToken("catalog-admin-customer-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(get("/api/v1/admin/promotions")
                            .header("Authorization", bearer(customerToken)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

            mockMvc.perform(get("/api/v1/admin/dashboard")
                            .header("Authorization", bearer(customerToken)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }

        @Test
        void should_allow_admin_on_admin_read_endpoints() throws Exception {
            String adminToken = createAdminAndGetAccessToken("catalog-admin-admin-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(get("/api/v1/admin/promotions")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get("/api/v1/admin/dashboard")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    class CategoryAdminScenarios {

        @Test
        void should_create_update_upload_image_and_delete_category_as_admin() throws Exception {
            String adminToken = createAdminAndGetAccessToken("catalog-category-admin-" + UUID.randomUUID() + "@example.com");
            String categoryName = "CAT-" + UUID.randomUUID().toString().substring(0, 8);

            UUID categoryId = createCategoryAsAdmin(adminToken, categoryName);

            mockMvc.perform(get("/api/v1/categories/" + categoryId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(categoryId.toString()))
                    .andExpect(jsonPath("$.data.name").value(categoryName));

            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].id", hasItem(categoryId.toString())));

            Map<String, Object> updatePayload = Map.of(
                    "name", categoryName + "-UPDATED",
                    "translations", Map.of(
                            "fr", Map.of("fullName", "Nom FR MAJ", "description", "Description FR MAJ"),
                            "en", Map.of("fullName", "Name EN Updated", "description", "Description EN Updated")
                    ),
                    "active", false
            );

            mockMvc.perform(put("/api/v1/categories/" + categoryId)
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatePayload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(categoryId.toString()));

            mockMvc.perform(get("/api/v1/categories/" + categoryId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.active").value(false))
                    .andExpect(jsonPath("$.data.name").value(categoryName + "-UPDATED"));

            MockMultipartFile image = new MockMultipartFile("image", "category.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
            mockMvc.perform(multipart("/api/v1/categories/" + categoryId + "/image")
                            .file(image)
                            .with(r -> {
                                r.setMethod("PATCH");
                                return r;
                            })
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/categories/" + categoryId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.imageBase64").isNotEmpty());

            mockMvc.perform(delete("/api/v1/categories/" + categoryId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/categories/" + categoryId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        void should_return_conflict_when_deleting_category_with_products() throws Exception {
            String adminToken = createAdminAndGetAccessToken("catalog-category-conflict-admin-" + UUID.randomUUID() + "@example.com");
            UUID categoryId = createCategoryAsAdmin(adminToken, "CAT-LINKED-" + UUID.randomUUID().toString().substring(0, 8));

            createProductAsAdmin(
                    adminToken,
                    categoryId,
                    "Prod linked " + UUID.randomUUID().toString().substring(0, 6),
                    19.99,
                    199.99
            );

            mockMvc.perform(delete("/api/v1/categories/" + categoryId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("HAS_PRODUCTS"));
        }

        @Test
        void should_validate_category_payload_and_not_found() throws Exception {
            String adminToken = createAdminAndGetAccessToken("catalog-category-validation-admin-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/categories")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

            Map<String, Object> validUpdatePayload = Map.of(
                    "name", "VALID_NAME",
                    "translations", Map.of(
                            "fr", Map.of("fullName", "Nom FR", "description", "Description FR")
                    ),
                    "active", true
            );

            mockMvc.perform(put("/api/v1/categories/" + UUID.randomUUID())
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdatePayload)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }

    @Nested
    class ProductAdminScenarios {

        @Test
        void should_create_update_list_and_delete_product_as_admin() throws Exception {
            String adminToken = createAdminAndGetAccessToken("catalog-product-admin-" + UUID.randomUUID() + "@example.com");
            UUID categoryId = createCategoryAsAdmin(adminToken, "CAT-PROD-" + UUID.randomUUID().toString().substring(0, 8));

            UUID productId = createProductAsAdmin(
                    adminToken,
                    categoryId,
                    "Product " + UUID.randomUUID().toString().substring(0, 8),
                    29.99,
                    299.99
            );

            mockMvc.perform(get("/api/v1/products/" + productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(productId.toString()))
                    .andExpect(jsonPath("$.data.isPublished").value(false))
                    .andExpect(jsonPath("$.data.isAvailable").value(true));

            mockMvc.perform(get("/api/v1/products")
                            .param("categoryId", categoryId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items[*].id", hasItem(productId.toString())));

            Map<String, Object> updatePayload = Map.of(
                    "translations", Map.of(
                            "fr", Map.of(
                                    "name", "Produit MAJ",
                                    "serviceDescription", "Service MAJ",
                                    "technicalDescription", "Tech MAJ",
                                    "highlightPoints", List.of("Point A", "Point B")
                            )
                    ),
                    "categoryId", categoryId,
                    "priorityLevel", 7,
                    "monthlyPrice", 39.99,
                    "annualPrice", 399.99,
                    "currency", "EUR",
                    "freeTrialDays", 10,
                    "isPublished", true,
                    "isAvailable", false
            );

            mockMvc.perform(put("/api/v1/products/" + productId)
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatePayload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(productId.toString()));

            mockMvc.perform(get("/api/v1/products/" + productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isPublished").value(true))
                    .andExpect(jsonPath("$.data.isAvailable").value(false))
                    .andExpect(jsonPath("$.data.monthlyPrice").value(39.99));

            mockMvc.perform(delete("/api/v1/products/" + productId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/products/" + productId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        void should_upload_reorder_and_delete_product_images_as_admin() throws Exception {
            String adminToken = createAdminAndGetAccessToken("catalog-product-image-admin-" + UUID.randomUUID() + "@example.com");
            UUID categoryId = createCategoryAsAdmin(adminToken, "CAT-IMG-" + UUID.randomUUID().toString().substring(0, 8));
            UUID productId = createProductAsAdmin(
                    adminToken,
                    categoryId,
                    "Product image " + UUID.randomUUID().toString().substring(0, 6),
                    49.99,
                    499.99
            );

            MockMultipartFile firstImage = new MockMultipartFile("image", "p1.jpg", "image/jpeg", new byte[]{10, 20, 30});
            MvcResult firstUploadResult = mockMvc.perform(multipart("/api/v1/products/" + productId + "/images")
                            .file(firstImage)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isCreated())
                    .andReturn();
            UUID firstImageId = extractUuidData(firstUploadResult);

            MockMultipartFile secondImage = new MockMultipartFile("image", "p2.jpg", "image/jpeg", new byte[]{40, 50, 60});
            MvcResult secondUploadResult = mockMvc.perform(multipart("/api/v1/products/" + productId + "/images")
                            .file(secondImage)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isCreated())
                    .andReturn();
            UUID secondImageId = extractUuidData(secondUploadResult);

            mockMvc.perform(get("/api/v1/products/" + productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.images.length()").value(2));

            mockMvc.perform(put("/api/v1/products/" + productId + "/images/order")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(secondImageId, firstImageId))))
                    .andExpect(status().isNoContent());

            MvcResult reorderedResult = mockMvc.perform(get("/api/v1/products/" + productId))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode imagesAfterReorder = objectMapper.readTree(reorderedResult.getResponse().getContentAsString())
                    .at("/data/images");
            assertTrue(containsNodeWithFieldValue(imagesAfterReorder, "id", firstImageId.toString()));
            assertTrue(containsNodeWithFieldValue(imagesAfterReorder, "id", secondImageId.toString()));

            mockMvc.perform(delete("/api/v1/products/" + productId + "/images/" + firstImageId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNoContent());

            waitUntilProductHasSingleExpectedImage(productId, secondImageId);
        }

        @Test
        void should_validate_product_payload_and_handle_business_errors() throws Exception {
            String adminToken = createAdminAndGetAccessToken("catalog-product-validation-admin-" + UUID.randomUUID() + "@example.com");
            UUID categoryId = createCategoryAsAdmin(adminToken, "CAT-VALID-" + UUID.randomUUID().toString().substring(0, 8));

            mockMvc.perform(post("/api/v1/products")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

            Map<String, Object> invalidCurrencyPayload = Map.of(
                    "translations", Map.of(
                            "fr", Map.of(
                                    "name", "Produit devise",
                                    "serviceDescription", "Service devise",
                                    "technicalDescription", "Tech devise",
                                    "highlightPoints", List.of("Point A")
                            )
                    ),
                    "categoryId", categoryId,
                    "priorityLevel", 1,
                    "monthlyPrice", 19.99,
                    "annualPrice", 199.99,
                    "currency", "eur",
                    "freeTrialDays", 0
            );

            mockMvc.perform(post("/api/v1/products")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidCurrencyPayload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

            Map<String, Object> missingCategoryPayload = Map.of(
                    "translations", Map.of(
                            "fr", Map.of(
                                    "name", "Produit missing cat",
                                    "serviceDescription", "Service missing cat",
                                    "technicalDescription", "Tech missing cat",
                                    "highlightPoints", List.of("Point A")
                            )
                    ),
                    "categoryId", UUID.randomUUID(),
                    "priorityLevel", 1,
                    "monthlyPrice", 19.99,
                    "annualPrice", 199.99,
                    "currency", "EUR",
                    "freeTrialDays", 0
            );

            mockMvc.perform(post("/api/v1/products")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(missingCategoryPayload)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATION"));

            Map<String, Object> validUpdatePayload = Map.of(
                    "translations", Map.of(
                            "fr", Map.of(
                                    "name", "Produit update",
                                    "serviceDescription", "Service update",
                                    "technicalDescription", "Tech update",
                                    "highlightPoints", List.of("Point A")
                            )
                    ),
                    "categoryId", categoryId,
                    "priorityLevel", 1,
                    "monthlyPrice", 25.99,
                    "annualPrice", 259.99,
                    "currency", "EUR",
                    "freeTrialDays", 0,
                    "isPublished", true,
                    "isAvailable", true
            );

            mockMvc.perform(put("/api/v1/products/" + UUID.randomUUID())
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdatePayload)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }

    @Nested
    class PromotionAdminScenarios {

        @Test
        void should_create_update_delete_promotion_and_expose_it_publicly() throws Exception {
            String adminToken = createAdminAndGetAccessToken("catalog-promo-admin-" + UUID.randomUUID() + "@example.com");
            UUID categoryId = createCategoryAsAdmin(adminToken, "CAT-PROMO-" + UUID.randomUUID().toString().substring(0, 8));
            String productName = "Product promo " + UUID.randomUUID().toString().substring(0, 8);
            double monthlyPrice = 59.99;
            double annualPrice = 599.99;
            UUID productId = createProductAsAdmin(
                    adminToken,
                    categoryId,
                    productName,
                    monthlyPrice,
                    annualPrice
            );
            publishProductAsAdmin(adminToken, productId, categoryId, productName, monthlyPrice, annualPrice);

            Instant startAt = Instant.now().minusSeconds(3600);
            Instant endAt = Instant.now().plusSeconds(7 * 24 * 3600L);

            UUID promotionId = createPromotionAsAdmin(
                    adminToken,
                    productId,
                    20,
                    startAt,
                    endAt,
                    true,
                    true,
                    1,
                    "Promo FR initiale",
                    "Initial EN promo"
            );

            mockMvc.perform(get("/api/v1/admin/promotions/" + promotionId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(promotionId.toString()))
                    .andExpect(jsonPath("$.data.discountPercent").value(20))
                    .andExpect(jsonPath("$.data.showInCarousel").value(true));

            mockMvc.perform(get("/api/v1/admin/promotions")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].id", hasItem(promotionId.toString())));

            MvcResult publicOffersResult = mockMvc.perform(get("/api/v1/offers/promotions")
                            .param("lang", "en"))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode offersData = objectMapper.readTree(publicOffersResult.getResponse().getContentAsString()).path("data");
            assertTrue(containsNodeWithFieldValue(offersData, "promotionId", promotionId.toString()),
                    "Public promotions should include the promotion just created");

            Map<String, Object> settingsPayload = Map.of(
                    "translations", Map.of(
                            "fr", Map.of("fixedText", "Texte FR test integration"),
                            "en", Map.of("fixedText", "EN integration test text")
                    )
            );
            mockMvc.perform(put("/api/v1/admin/promotions/carousel-settings")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(settingsPayload)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/offers/promotions/fixed-text")
                            .param("lang", "en"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("EN integration test text"));

            Map<String, Object> updatePayload = Map.of(
                    "discountPercent", 25,
                    "translations", Map.of(
                            "fr", Map.of("marketingText", "Promo FR MAJ"),
                            "en", Map.of("marketingText", "Updated EN promo")
                    ),
                    "startAt", startAt,
                    "endAt", endAt.plusSeconds(24 * 3600L),
                    "enabled", true,
                    "showInCarousel", true,
                    "carouselOrder", 1
            );

            mockMvc.perform(put("/api/v1/admin/promotions/" + promotionId)
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatePayload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(promotionId.toString()));

            mockMvc.perform(get("/api/v1/admin/promotions/" + promotionId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.discountPercent").value(25));

            mockMvc.perform(delete("/api/v1/admin/promotions/" + promotionId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/admin/promotions/" + promotionId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        @Test
        void should_handle_promotion_overlap_conflict_validation_and_not_found() throws Exception {
            String adminToken = createAdminAndGetAccessToken("catalog-promo-business-admin-" + UUID.randomUUID() + "@example.com");
            UUID categoryId = createCategoryAsAdmin(adminToken, "CAT-PROMO-BUS-" + UUID.randomUUID().toString().substring(0, 8));
            UUID firstProduct = createProductAsAdmin(
                    adminToken,
                    categoryId,
                    "Product promo A " + UUID.randomUUID().toString().substring(0, 6),
                    39.99,
                    399.99
            );
            UUID secondProduct = createProductAsAdmin(
                    adminToken,
                    categoryId,
                    "Product promo B " + UUID.randomUUID().toString().substring(0, 6),
                    49.99,
                    499.99
            );

            Instant startAt = Instant.now().minusSeconds(3600);
            Instant endAt = Instant.now().plusSeconds(5 * 24 * 3600L);

            createPromotionAsAdmin(
                    adminToken,
                    firstProduct,
                    15,
                    startAt,
                    endAt,
                    true,
                    true,
                    1,
                    "Promo FR A",
                    "Promo EN A"
            );

            Map<String, Object> overlapPayload = Map.of(
                    "productId", firstProduct,
                    "discountPercent", 10,
                    "translations", Map.of(
                            "fr", Map.of("marketingText", "Promo overlap FR"),
                            "en", Map.of("marketingText", "Promo overlap EN")
                    ),
                    "startAt", startAt.plusSeconds(300),
                    "endAt", endAt.plusSeconds(300),
                    "enabled", true,
                    "showInCarousel", true,
                    "carouselOrder", 2
            );

            mockMvc.perform(post("/api/v1/admin/promotions")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(overlapPayload)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("PROMOTION_OVERLAP"));

            Map<String, Object> carouselConflictPayload = Map.of(
                    "productId", secondProduct,
                    "discountPercent", 30,
                    "translations", Map.of(
                            "fr", Map.of("marketingText", "Promo conflict FR"),
                            "en", Map.of("marketingText", "Promo conflict EN")
                    ),
                    "startAt", startAt,
                    "endAt", endAt,
                    "enabled", true,
                    "showInCarousel", true,
                    "carouselOrder", 1
            );

            mockMvc.perform(post("/api/v1/admin/promotions")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(carouselConflictPayload)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("CAROUSEL_ORDER_CONFLICT"));

            Map<String, Object> unknownProductPayload = Map.of(
                    "productId", UUID.randomUUID(),
                    "discountPercent", 10,
                    "translations", Map.of(
                            "fr", Map.of("marketingText", "Promo missing product FR"),
                            "en", Map.of("marketingText", "Promo missing product EN")
                    ),
                    "startAt", startAt,
                    "endAt", endAt,
                    "enabled", true,
                    "showInCarousel", false,
                    "carouselOrder", 1
            );

            mockMvc.perform(post("/api/v1/admin/promotions")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(unknownProductPayload)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

            Map<String, Object> invalidWindowPayload = Map.of(
                    "productId", secondProduct,
                    "discountPercent", 20,
                    "translations", Map.of(
                            "fr", Map.of("marketingText", "Promo invalid dates FR"),
                            "en", Map.of("marketingText", "Promo invalid dates EN")
                    ),
                    "startAt", endAt,
                    "endAt", startAt,
                    "enabled", true,
                    "showInCarousel", false,
                    "carouselOrder", 1
            );

            mockMvc.perform(post("/api/v1/admin/promotions")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidWindowPayload)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

            Map<String, Object> updatePayload = Map.of(
                    "discountPercent", 20,
                    "translations", Map.of(
                            "fr", Map.of("marketingText", "Update unknown FR"),
                            "en", Map.of("marketingText", "Update unknown EN")
                    ),
                    "startAt", startAt,
                    "endAt", endAt,
                    "enabled", true,
                    "showInCarousel", false,
                    "carouselOrder", 1
            );

            mockMvc.perform(put("/api/v1/admin/promotions/" + UUID.randomUUID())
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatePayload)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

            mockMvc.perform(delete("/api/v1/admin/promotions/" + UUID.randomUUID())
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DashboardAdminScenarios {

        @Test
        void should_get_and_update_dashboard_goals() throws Exception {
            String adminToken = createAdminAndGetAccessToken("catalog-dashboard-admin-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(get("/api/v1/admin/dashboard")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currentYear").isNumber());

            Map<String, Object> updateTargetPayload = Map.of(
                    "goalKey", "revenue",
                    "targetValue", 12345
            );

            mockMvc.perform(put("/api/v1/admin/dashboard/2026/goals/target")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateTargetPayload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.year").value(2026))
                    .andExpect(jsonPath("$.data.revenueGoal.targetValue").value(12345));

            Map<String, Object> updateMonthlyPayload = Map.of(
                    "monthlyRevenueGoal", List.of(100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L, 1000L, 1100L, 1200L)
            );

            mockMvc.perform(put("/api/v1/admin/dashboard/2026/goals/monthly-revenue")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateMonthlyPayload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.year").value(2026))
                    .andExpect(jsonPath("$.data.monthlyRevenueGoal[11]").value(1200));

            mockMvc.perform(get("/api/v1/admin/dashboard")
                            .param("year", "2026")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.years[0].year").value(2026))
                    .andExpect(jsonPath("$.data.years[0].revenueGoal.targetValue").value(7800))
                    .andExpect(jsonPath("$.data.years[0].monthlyRevenueGoal[11]").value(1200));
        }

        @Test
        void should_validate_dashboard_payloads() throws Exception {
            String adminToken = createAdminAndGetAccessToken("catalog-dashboard-validation-admin-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(put("/api/v1/admin/dashboard/2026/goals/target")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"goalKey\":\"invalid\",\"targetValue\":10}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

            mockMvc.perform(put("/api/v1/admin/dashboard/2026/goals/target")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"goalKey\":\"revenue\",\"targetValue\":-1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

            mockMvc.perform(put("/api/v1/admin/dashboard/2026/goals/monthly-revenue")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"monthlyRevenueGoal\":[]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

            Map<String, Object> tooManyMonthsPayload = Map.of(
                    "monthlyRevenueGoal", List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
            );

            mockMvc.perform(put("/api/v1/admin/dashboard/2026/goals/monthly-revenue")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tooManyMonthsPayload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }
    }

    @Nested
    class CustomerForbiddenOnAdminCrud {

        @Test
        void should_forbid_customer_on_product_admin_crud() throws Exception {
            String customerToken = registerCustomerAndGetAccessToken("catalog-product-customer-" + UUID.randomUUID() + "@example.com");

            String productPayload = """
                    {
                      "translations": {"fr": {"name":"X","serviceDescription":"S","technicalDescription":"T","highlightPoints":["h1"]}},
                      "categoryId": "00000000-0000-0000-0000-000000000001",
                      "priorityLevel": 1,
                      "monthlyPrice": 10,
                      "annualPrice": 100,
                      "currency": "EUR",
                      "freeTrialDays": 0
                    }
                    """;

            mockMvc.perform(post("/api/v1/products")
                            .header("Authorization", bearer(customerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productPayload))
                    .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/v1/products/" + UUID.randomUUID())
                            .header("Authorization", bearer(customerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productPayload))
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/api/v1/products/" + UUID.randomUUID())
                            .header("Authorization", bearer(customerToken)))
                    .andExpect(status().isForbidden());

            MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});
            mockMvc.perform(multipart("/api/v1/products/" + UUID.randomUUID() + "/images")
                            .file(image)
                            .with(r -> {
                                r.setMethod("POST");
                                return r;
                            })
                            .header("Authorization", bearer(customerToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void should_forbid_customer_on_category_admin_crud() throws Exception {
            String customerToken = registerCustomerAndGetAccessToken("catalog-category-customer-" + UUID.randomUUID() + "@example.com");

            String createPayload = "{\"name\":\"edr\",\"translations\":{\"fr\":{\"fullName\":\"EDR\",\"description\":\"Desc\"}}}";
            String updatePayload = """
                    {
                      "name": "edr",
                      "translations": {"fr": {"fullName":"EDR","description":"Desc"}},
                      "active": true
                    }
                    """;

            mockMvc.perform(post("/api/v1/categories")
                            .header("Authorization", bearer(customerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createPayload))
                    .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/v1/categories/" + UUID.randomUUID())
                            .header("Authorization", bearer(customerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePayload))
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/api/v1/categories/" + UUID.randomUUID())
                            .header("Authorization", bearer(customerToken)))
                    .andExpect(status().isForbidden());

            MockMultipartFile image = new MockMultipartFile("image", "cat.jpg", "image/jpeg", new byte[]{4, 5, 6});
            mockMvc.perform(multipart("/api/v1/categories/" + UUID.randomUUID() + "/image")
                            .file(image)
                            .with(r -> {
                                r.setMethod("PATCH");
                                return r;
                            })
                            .header("Authorization", bearer(customerToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void should_forbid_customer_on_admin_promotions_and_dashboard_writes() throws Exception {
            String customerToken = registerCustomerAndGetAccessToken("catalog-promo-customer-" + UUID.randomUUID() + "@example.com");

            mockMvc.perform(post("/api/v1/admin/promotions")
                            .header("Authorization", bearer(customerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/v1/admin/promotions/" + UUID.randomUUID())
                            .header("Authorization", bearer(customerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/api/v1/admin/promotions/" + UUID.randomUUID())
                            .header("Authorization", bearer(customerToken)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/v1/admin/promotions/carousel-settings")
                            .header("Authorization", bearer(customerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/v1/admin/dashboard/2026/goals/target")
                            .header("Authorization", bearer(customerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"goalKey\":\"revenue\",\"targetValue\":10}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/v1/admin/dashboard/2026/goals/monthly-revenue")
                            .header("Authorization", bearer(customerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"monthlyRevenueGoal\":[1,1,1,1,1,1,1,1,1,1,1,1]}"))
                    .andExpect(status().isForbidden());
        }
    }

    private UUID createCategoryAsAdmin(String adminToken, String categoryName) throws Exception {
        Map<String, Object> payload = Map.of(
                "name", categoryName,
                "translations", Map.of(
                        "fr", Map.of("fullName", categoryName + " FR", "description", "Description FR"),
                        "en", Map.of("fullName", categoryName + " EN", "description", "Description EN")
                )
        );

        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractUuidData(result);
    }

    private UUID createProductAsAdmin(String adminToken,
                                      UUID categoryId,
                                      String productName,
                                      double monthlyPrice,
                                      double annualPrice) throws Exception {
        Map<String, Object> payload = Map.of(
                "translations", Map.of(
                        "fr", Map.of(
                                "name", productName,
                                "serviceDescription", "Service for " + productName,
                                "technicalDescription", "Technical for " + productName,
                                "highlightPoints", List.of("Point A", "Point B")
                        )
                ),
                "categoryId", categoryId,
                "priorityLevel", 1,
                "monthlyPrice", monthlyPrice,
                "annualPrice", annualPrice,
                "currency", "EUR",
                "freeTrialDays", 7
        );

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractUuidData(result);
    }

    private UUID createPromotionAsAdmin(String adminToken,
                                        UUID productId,
                                        int discountPercent,
                                        Instant startAt,
                                        Instant endAt,
                                        boolean enabled,
                                        boolean showInCarousel,
                                        int carouselOrder,
                                        String frText,
                                        String enText) throws Exception {
        Map<String, Object> payload = Map.of(
                "productId", productId,
                "discountPercent", discountPercent,
                "translations", Map.of(
                        "fr", Map.of("marketingText", frText),
                        "en", Map.of("marketingText", enText)
                ),
                "startAt", startAt,
                "endAt", endAt,
                "enabled", enabled,
                "showInCarousel", showInCarousel,
                "carouselOrder", carouselOrder
        );

        MvcResult result = mockMvc.perform(post("/api/v1/admin/promotions")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractUuidData(result);
    }

    private void publishProductAsAdmin(String adminToken,
                                       UUID productId,
                                       UUID categoryId,
                                       String productName,
                                       double monthlyPrice,
                                       double annualPrice) throws Exception {
        Map<String, Object> payload = Map.of(
                "translations", Map.of(
                        "fr", Map.of(
                                "name", productName,
                                "serviceDescription", "Service for " + productName,
                                "technicalDescription", "Technical for " + productName,
                                "highlightPoints", List.of("Point A", "Point B")
                        )
                ),
                "categoryId", categoryId,
                "priorityLevel", 1,
                "monthlyPrice", monthlyPrice,
                "annualPrice", annualPrice,
                "currency", "EUR",
                "freeTrialDays", 7,
                "isPublished", true,
                "isAvailable", true
        );

        mockMvc.perform(put("/api/v1/products/" + productId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    private UUID extractUuidData(MvcResult result) throws Exception {
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return UUID.fromString(data.asText());
    }

    private boolean containsNodeWithFieldValue(JsonNode arrayNode, String fieldName, String expectedValue) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return false;
        }
        for (JsonNode node : arrayNode) {
            if (expectedValue.equals(node.path(fieldName).asText())) {
                return true;
            }
        }
        return false;
    }

    private String createAdminAndGetAccessToken(String email) throws Exception {
        User admin = User.createByAdmin(
                Email.of(email),
                HashedPassword.of("integration-test-hash"),
                "Catalog",
                "Admin",
                Role.ADMIN
        );
        userRepository.save(admin);
        return jwtProvider.generateAccessToken(admin);
    }

    private String registerCustomerAndGetAccessToken(String email) throws Exception {
        User customer = User.register(
                Email.of(email),
                HashedPassword.of("integration-test-hash"),
                "Catalog",
                "Customer",
                "fr"
        );
        userRepository.save(customer);
        return jwtProvider.generateAccessToken(customer);
    }

    private void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for register retry", e);
        }
    }

    private void waitUntilProductHasSingleExpectedImage(UUID productId, UUID expectedImageId) throws Exception {
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            MvcResult result = mockMvc.perform(get("/api/v1/products/" + productId))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode images = objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/images");
            if (images.isArray()
                    && images.size() == 1
                    && expectedImageId.toString().equals(images.get(0).path("id").asText())) {
                return;
            }

            if (attempt < maxAttempts) {
                sleepSafely(150L * attempt);
            }
        }

        throw new AssertionError("Product images did not converge to a single expected image after deletion");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
