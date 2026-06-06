package com.cyna.modules.product.infrastructure.cache;

import com.cyna.modules.product.application.command.create.CreateProductCommand;
import com.cyna.modules.product.application.command.create.CreateProductCommandHandler;
import com.cyna.modules.product.application.command.updateoffercarouselsettings.UpdateOfferCarouselSettingsCommand;
import com.cyna.modules.product.application.command.updateoffercarouselsettings.UpdateOfferCarouselSettingsCommandHandler;
import com.cyna.modules.product.application.command.updatecategory.UpdateCategoryCommand;
import com.cyna.modules.product.application.command.updatecategory.UpdateCategoryCommandHandler;
import com.cyna.modules.product.application.query.getcategorybyid.GetCategoryByIdQueryHandler;
import com.cyna.modules.product.application.query.getcategorybyid.CategoryReadModel;
import com.cyna.modules.product.application.query.getcategorybyid.GetCategoryByIdQuery;
import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.application.query.getoffercarouselsettings.GetOfferCarouselSettingsQuery;
import com.cyna.modules.product.application.query.getoffercarouselsettings.GetOfferCarouselSettingsQueryHandler;
import com.cyna.modules.product.application.query.list.ListProductsQuery;
import com.cyna.modules.product.application.query.list.ListProductsQueryHandler;
import com.cyna.modules.product.application.query.list.ProductSort;
import com.cyna.modules.product.application.query.listofferpromotions.ListOfferPromotionsQuery;
import com.cyna.modules.product.application.query.listofferpromotions.ListOfferPromotionsQueryHandler;
import com.cyna.modules.product.application.promotion.PromotionPricingResolver;
import com.cyna.modules.product.domain.model.CarouselSettingsTranslation;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.CategoryTranslation;
import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.ProductImage;
import com.cyna.modules.product.domain.model.ProductTranslation;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.model.PromotionTranslation;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Page;
import com.cyna.shared.infrastructure.mediator.SpringMediator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(ProductApplicationCacheIntegrationTest.TestConfig.class)
class ProductApplicationCacheIntegrationTest {

    private static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ProductSort sort = ProductSort.defaultSort();
    private final ListProductsQuery listProductsQuery =
            new ListProductsQuery(0, 20, true, null, CATEGORY_ID, null, "xdr",
                    null, null, null, null, null, sort);
    private final ListOfferPromotionsQuery listOfferPromotionsQuery = new ListOfferPromotionsQuery("fr");
    private final GetOfferCarouselSettingsQuery offerCarouselSettingsQuery = new GetOfferCarouselSettingsQuery();

    @Autowired
    private Mediator mediator;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private OfferCarouselSettingsRepository offerCarouselSettingsRepository;

    @Autowired
    private com.cyna.modules.product.domain.repository.CarouselSlotRepository carouselSlotRepository;

    @BeforeEach
    void setUp() {
        reset(productRepository, categoryRepository, productImageRepository, promotionRepository,
              offerCarouselSettingsRepository, carouselSlotRepository);
        when(carouselSlotRepository.findAll()).thenReturn(List.of());
        when(promotionRepository.findActiveByProductIds(anyCollection(), any(Instant.class))).thenReturn(List.of());
        clearAllCaches();
    }

    @Test
    void should_cache_product_list_queries() {
        Product product = sampleProduct(PRODUCT_ID, CATEGORY_ID);
        Page<Product> page = new Page<>(List.of(product), 0, 20, 1, 1);

        when(productRepository.findAll(0, 20, true, null, CATEGORY_ID, null, "xdr", null, null, null, null, null, sort)).thenReturn(page);
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory(CATEGORY_ID, "xdr")));
        when(productImageRepository.findByProductIds(List.of(PRODUCT_ID))).thenReturn(List.of());

        Page<ProductReadModel> first = mediator.send(listProductsQuery);
        Page<ProductReadModel> second = mediator.send(listProductsQuery);

        assertThat(first.items()).hasSize(1);
        assertThat(second.items()).hasSize(1);
        verify(productRepository, times(1)).findAll(0, 20, true, null, CATEGORY_ID, null, "xdr", null, null, null, null, null, sort);
    }

    @Test
    void should_invalidate_product_list_cache_when_creating_product() {
        Product product = sampleProduct(PRODUCT_ID, CATEGORY_ID);
        Page<Product> page = new Page<>(List.of(product), 0, 20, 1, 1);

        when(productRepository.findAll(0, 20, true, null, CATEGORY_ID, null, "xdr", null, null, null, null, null, sort)).thenReturn(page);
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory(CATEGORY_ID, "xdr")));
        when(productImageRepository.findByProductIds(List.of(PRODUCT_ID))).thenReturn(List.of());
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(sampleCategory(CATEGORY_ID, "xdr")));

        mediator.send(listProductsQuery);

        Cache productListCache = cacheManager.getCache(ProductCacheNames.PRODUCT_LIST);
        assertThat(productListCache).isNotNull();
        assertThat(productListCache.get(listProductsQuery)).isNotNull();

        mediator.send(new CreateProductCommand(
                Map.of("fr", new ProductTranslation(
                        "XDR Premium",
                        "Managed XDR service",
                        "Technical details",
                        List.of("24/7 SOC")
                )),
                CATEGORY_ID,
                2,
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                14
        ));

        assertThat(productListCache.get(listProductsQuery)).isNull();

        mediator.send(listProductsQuery);
        verify(productRepository, times(2)).findAll(0, 20, true, null, CATEGORY_ID, null, "xdr", null, null, null, null, null, sort);
    }

    @Test
    void should_invalidate_category_detail_cache_when_updating_category() {
        Category category = sampleCategory(CATEGORY_ID, "xdr");
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        CategoryReadModel first = mediator.send(new GetCategoryByIdQuery(CATEGORY_ID));

        Cache categoryByIdCache = cacheManager.getCache(ProductCacheNames.CATEGORY_BY_ID);
        assertThat(categoryByIdCache).isNotNull();
        assertThat(first).isNotNull();
        assertThat(categoryByIdCache.get(CATEGORY_ID)).isNotNull();

        mediator.send(new UpdateCategoryCommand(
                CATEGORY_ID,
                "xdr-renamed",
                Map.of("fr", new CategoryTranslation("XDR Renamed", "Updated description")),
                true
        ));

        assertThat(categoryByIdCache.get(CATEGORY_ID)).isNull();
    }

    @Test
    void should_cache_offer_promotions_queries() {
        Product product = sampleProduct(PRODUCT_ID, CATEGORY_ID);
        Promotion promotion = samplePromotion(PRODUCT_ID);
        UUID promotionId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        when(promotionRepository.findAll()).thenReturn(List.of(promotion));
        when(carouselSlotRepository.findAll()).thenReturn(
                List.of(new com.cyna.modules.product.domain.model.CarouselSlot(promotionId, 1)));
        when(productRepository.findAllByIds(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory(CATEGORY_ID, "xdr")));
        when(productImageRepository.findByProductIds(List.of(PRODUCT_ID))).thenReturn(List.of(sampleProductImage(PRODUCT_ID)));

        var first = mediator.send(listOfferPromotionsQuery);
        var second = mediator.send(listOfferPromotionsQuery);

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        verify(promotionRepository, times(1)).findAll();
        verify(productRepository, times(1)).findAllByIds(List.of(PRODUCT_ID));
        verify(categoryRepository, times(1)).findAll();
        verify(productImageRepository, times(1)).findByProductIds(List.of(PRODUCT_ID));
    }

    @Test
    void should_invalidate_offer_promotions_cache_when_updating_category() {
        Product product = sampleProduct(PRODUCT_ID, CATEGORY_ID);
        Promotion promotion = samplePromotion(PRODUCT_ID);
        Category category = sampleCategory(CATEGORY_ID, "xdr");
        UUID promotionId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        when(promotionRepository.findAll()).thenReturn(List.of(promotion));
        when(carouselSlotRepository.findAll()).thenReturn(
                List.of(new com.cyna.modules.product.domain.model.CarouselSlot(promotionId, 1)));
        when(productRepository.findAllByIds(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(productImageRepository.findByProductIds(List.of(PRODUCT_ID))).thenReturn(List.of(sampleProductImage(PRODUCT_ID)));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        mediator.send(listOfferPromotionsQuery);

        Cache offerPromotionsCache = cacheManager.getCache(ProductCacheNames.OFFER_PROMOTIONS_LIST);
        assertThat(offerPromotionsCache).isNotNull();
        assertThat(offerPromotionsCache.get(listOfferPromotionsQuery)).isNotNull();

        mediator.send(new UpdateCategoryCommand(
                CATEGORY_ID,
                "xdr-renamed",
                Map.of("fr", new CategoryTranslation("XDR Renamed", "Updated description")),
                true
        ));

        assertThat(offerPromotionsCache.get(listOfferPromotionsQuery)).isNull();

        mediator.send(listOfferPromotionsQuery);
        verify(promotionRepository, times(2)).findAll();
    }

    @Test
    void should_invalidate_offer_carousel_settings_cache_when_updating_settings() {
        when(offerCarouselSettingsRepository.find()).thenReturn(Optional.of(sampleOfferCarouselSettings("Texte initial")));

        var first = mediator.send(offerCarouselSettingsQuery);
        var second = mediator.send(offerCarouselSettingsQuery);

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        verify(offerCarouselSettingsRepository, times(1)).find();

        Cache settingsCache = cacheManager.getCache(ProductCacheNames.OFFER_CAROUSEL_SETTINGS);
        assertThat(settingsCache).isNotNull();
        assertThat(settingsCache.get(offerCarouselSettingsQuery)).isNotNull();

        mediator.send(new UpdateOfferCarouselSettingsCommand(
                Map.of("fr", new CarouselSettingsTranslation("Texte mis a jour")),
                5
        ));

        assertThat(settingsCache.get(offerCarouselSettingsQuery)).isNull();
    }

    @Test
    void should_keep_product_list_cache_when_create_product_command_fails() {
        Product product = sampleProduct(PRODUCT_ID, CATEGORY_ID);
        Page<Product> page = new Page<>(List.of(product), 0, 20, 1, 1);
        UUID unknownCategory = UUID.fromString("33333333-3333-3333-3333-333333333333");

        when(productRepository.findAll(0, 20, true, null, CATEGORY_ID, null, "xdr", null, null, null, null, null, sort)).thenReturn(page);
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory(CATEGORY_ID, "xdr")));
        when(productImageRepository.findByProductIds(List.of(PRODUCT_ID))).thenReturn(List.of());
        when(categoryRepository.findById(unknownCategory)).thenReturn(Optional.empty());

        mediator.send(listProductsQuery);

        Cache productListCache = cacheManager.getCache(ProductCacheNames.PRODUCT_LIST);
        assertThat(productListCache).isNotNull();
        assertThat(productListCache.get(listProductsQuery)).isNotNull();

        var result = mediator.send(new CreateProductCommand(
                Map.of("fr", new ProductTranslation(
                        "XDR Premium",
                        "Managed XDR service",
                        "Technical details",
                        List.of("24/7 SOC")
                )),
                unknownCategory,
                2,
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                14
        ));

        assertThat(result.isFailure()).isTrue();
        assertThat(productListCache.get(listProductsQuery)).isNotNull();

        mediator.send(listProductsQuery);
        verify(productRepository, times(1)).findAll(0, 20, true, null, CATEGORY_ID, null, "xdr", null, null, null, null, null, sort);
    }

    @Test
    void should_keep_product_list_cache_when_update_category_command_fails() {
        Product product = sampleProduct(PRODUCT_ID, CATEGORY_ID);
        Page<Product> page = new Page<>(List.of(product), 0, 20, 1, 1);
        UUID unknownCategory = UUID.fromString("44444444-4444-4444-4444-444444444444");

        when(productRepository.findAll(0, 20, true, null, CATEGORY_ID, null, "xdr", null, null, null, null, null, sort)).thenReturn(page);
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory(CATEGORY_ID, "xdr")));
        when(productImageRepository.findByProductIds(List.of(PRODUCT_ID))).thenReturn(List.of());
        when(categoryRepository.findById(unknownCategory)).thenReturn(Optional.empty());

        mediator.send(listProductsQuery);

        Cache productListCache = cacheManager.getCache(ProductCacheNames.PRODUCT_LIST);
        assertThat(productListCache).isNotNull();
        assertThat(productListCache.get(listProductsQuery)).isNotNull();

        var result = mediator.send(new UpdateCategoryCommand(
                unknownCategory,
                "xdr-renamed",
                Map.of("fr", new CategoryTranslation("XDR Renamed", "Updated description")),
                true
        ));

        assertThat(result.isFailure()).isTrue();
        assertThat(productListCache.get(listProductsQuery)).isNotNull();

        mediator.send(listProductsQuery);
        verify(productRepository, times(1)).findAll(0, 20, true, null, CATEGORY_ID, null, "xdr", null, null, null, null, null, sort);
    }

    private void clearAllCaches() {
        clearCache(ProductCacheNames.PRODUCT_LIST);
        clearCache(ProductCacheNames.PRODUCT_BY_ID);
        clearCache(ProductCacheNames.CATEGORY_LIST);
        clearCache(ProductCacheNames.CATEGORY_BY_ID);
        clearCache(ProductCacheNames.OFFER_PROMOTIONS_LIST);
        clearCache(ProductCacheNames.OFFER_CAROUSEL_SETTINGS);
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private Product sampleProduct(UUID productId, UUID categoryId) {
        Instant createdAt = Instant.parse("2026-04-20T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-04-27T10:00:00Z");
        return Product.reconstitute(
                productId,
                Map.of("fr", new ProductTranslation(
                        "XDR Ultimate",
                        "Managed XDR service",
                        "Technical details",
                        List.of("24/7 SOC")
                )),
                categoryId,
                1,
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                true,
                true,
                14,
                createdAt,
                updatedAt
        );
    }

    private Category sampleCategory(UUID categoryId, String name) {
        Instant createdAt = Instant.parse("2026-04-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-04-27T10:00:00Z");
        return Category.reconstitute(
                categoryId,
                name,
                Map.of("fr", new CategoryTranslation(name.toUpperCase(), "Category description")),
                null,
                true,
                createdAt,
                updatedAt
        );
    }

    private Promotion samplePromotion(UUID productId) {
        Instant now = Instant.now();
        return Promotion.reconstitute(
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                productId,
                25,
                Map.of("fr", new PromotionTranslation("Offre speciale")),
                now.minusSeconds(3_600),
                now.plusSeconds(3_600),
                true,
                now.minusSeconds(7_200),
                now.minusSeconds(3_600)
        );
    }

    private ProductImage sampleProductImage(UUID productId) {
        Instant createdAt = Instant.parse("2026-04-22T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-04-22T10:00:00Z");
        return ProductImage.reconstitute(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                productId,
                new byte[]{1, 2, 3},
                "image/jpeg",
                0,
                createdAt,
                updatedAt
        );
    }

    private OfferCarouselSettings sampleOfferCarouselSettings(String fixedText) {
        Instant createdAt = Instant.parse("2026-04-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-04-27T10:00:00Z");
        return OfferCarouselSettings.reconstitute(
                Map.of("fr", new CarouselSettingsTranslation(fixedText)),
                OfferCarouselSettings.DEFAULT_MAX_SLIDES,
                createdAt,
                updatedAt
        );
    }

    @Configuration
    @Import(ProductCacheConfig.class)
    static class TestConfig {

        @Bean
        SpringMediator springMediator(ApplicationContext applicationContext) {
            return new SpringMediator(applicationContext);
        }

        @Bean
        Mediator mediator(SpringMediator springMediator, CacheManager cacheManager) {
            return new ProductCachingMediator(springMediator, cacheManager);
        }

        @Bean
        ProductRepository productRepository() {
            return mock(ProductRepository.class);
        }

        @Bean
        CategoryRepository categoryRepository() {
            return mock(CategoryRepository.class);
        }

        @Bean
        ProductImageRepository productImageRepository() {
            return mock(ProductImageRepository.class);
        }

        @Bean
        PromotionRepository promotionRepository() {
            return mock(PromotionRepository.class);
        }

        @Bean
        OfferCarouselSettingsRepository offerCarouselSettingsRepository() {
            return mock(OfferCarouselSettingsRepository.class);
        }

        @Bean
        com.cyna.modules.product.domain.repository.CarouselSlotRepository carouselSlotRepository() {
            return mock(com.cyna.modules.product.domain.repository.CarouselSlotRepository.class);
        }

        @Bean
        PromotionPricingResolver promotionPricingResolver() {
            return new PromotionPricingResolver();
        }

        @Bean
        TransactionRunner transactionRunner() {
            return new TransactionRunner() {
                @Override
                public void run(Runnable action) {
                    action.run();
                }

                @Override
                public <T> T runReturning(java.util.function.Supplier<T> action) {
                    return action.get();
                }
            };
        }

        @Bean
        ListProductsQueryHandler listProductsQueryHandler(ProductRepository productRepository,
                                                          CategoryRepository categoryRepository,
                                                          ProductImageRepository productImageRepository,
                                                          PromotionRepository promotionRepository,
                                                          PromotionPricingResolver promotionPricingResolver) {
            return new ListProductsQueryHandler(
                    productRepository,
                    categoryRepository,
                    productImageRepository,
                    promotionRepository,
                    promotionPricingResolver
            );
        }

        @Bean
        ListOfferPromotionsQueryHandler listOfferPromotionsQueryHandler(PromotionRepository promotionRepository,
                                                                        com.cyna.modules.product.domain.repository.CarouselSlotRepository carouselSlotRepository,
                                                                        ProductRepository productRepository,
                                                                        ProductImageRepository productImageRepository,
                                                                        CategoryRepository categoryRepository) {
            return new ListOfferPromotionsQueryHandler(
                    promotionRepository,
                    carouselSlotRepository,
                    productRepository,
                    productImageRepository,
                    categoryRepository
            );
        }

        @Bean
        GetOfferCarouselSettingsQueryHandler getOfferCarouselSettingsQueryHandler(
                OfferCarouselSettingsRepository offerCarouselSettingsRepository) {
            return new GetOfferCarouselSettingsQueryHandler(offerCarouselSettingsRepository);
        }

        @Bean
        GetCategoryByIdQueryHandler getCategoryByIdQueryHandler(CategoryRepository categoryRepository,
                                                                ProductRepository productRepository) {
            return new GetCategoryByIdQueryHandler(categoryRepository, productRepository);
        }

        @Bean
        CreateProductCommandHandler createProductCommandHandler(ProductRepository productRepository,
                                                                CategoryRepository categoryRepository,
                                                                TransactionRunner transactionRunner) {
            return new CreateProductCommandHandler(productRepository, categoryRepository, transactionRunner);
        }

        @Bean
        UpdateCategoryCommandHandler updateCategoryCommandHandler(CategoryRepository categoryRepository,
                                                                  TransactionRunner transactionRunner) {
            return new UpdateCategoryCommandHandler(categoryRepository, transactionRunner);
        }

        @Bean
        UpdateOfferCarouselSettingsCommandHandler updateOfferCarouselSettingsCommandHandler(
                OfferCarouselSettingsRepository offerCarouselSettingsRepository,
                TransactionRunner transactionRunner) {
            return new UpdateOfferCarouselSettingsCommandHandler(offerCarouselSettingsRepository, transactionRunner);
        }
    }
}
