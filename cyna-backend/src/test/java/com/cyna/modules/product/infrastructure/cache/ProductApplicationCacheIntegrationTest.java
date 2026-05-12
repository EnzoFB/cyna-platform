package com.cyna.modules.product.infrastructure.cache;

import com.cyna.modules.product.application.command.create.CreateProductCommand;
import com.cyna.modules.product.application.command.create.CreateProductCommandHandler;
import com.cyna.modules.product.application.command.updatecategory.UpdateCategoryCommand;
import com.cyna.modules.product.application.command.updatecategory.UpdateCategoryCommandHandler;
import com.cyna.modules.product.application.query.getcategorybyid.GetCategoryByIdQuery;
import com.cyna.modules.product.application.query.getcategorybyid.GetCategoryByIdQueryHandler;
import com.cyna.modules.product.application.query.getcategorybyid.CategoryReadModel;
import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.application.query.list.ListProductsQuery;
import com.cyna.modules.product.application.query.list.ListProductsQueryHandler;
import com.cyna.modules.product.application.query.list.ProductSort;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @BeforeEach
    void setUp() {
        reset(productRepository, categoryRepository, productImageRepository);
        clearAllCaches();
    }

    @Test
    void should_cache_product_list_queries() {
        Product product = sampleProduct(PRODUCT_ID, CATEGORY_ID);
        Page<Product> page = new Page<>(List.of(product), 0, 20, 1, 1);

        when(productRepository.findAll(0, 20, true, null, CATEGORY_ID, null, "xdr",
                null, null, null, null, null, sort)).thenReturn(page);
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory(CATEGORY_ID, "xdr")));
        when(productImageRepository.findByProductIds(List.of(PRODUCT_ID))).thenReturn(List.of());

        Page<ProductReadModel> first = mediator.send(listProductsQuery);
        Page<ProductReadModel> second = mediator.send(listProductsQuery);

        assertThat(first.items()).hasSize(1);
        assertThat(second.items()).hasSize(1);
        verify(productRepository, times(1)).findAll(0, 20, true, null, CATEGORY_ID, null, "xdr",
                null, null, null, null, null, sort);
    }

    @Test
    void should_invalidate_product_list_cache_when_creating_product() {
        Product product = sampleProduct(PRODUCT_ID, CATEGORY_ID);
        Page<Product> page = new Page<>(List.of(product), 0, 20, 1, 1);

        when(productRepository.findAll(0, 20, true, null, CATEGORY_ID, null, "xdr",
                null, null, null, null, null, sort)).thenReturn(page);
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory(CATEGORY_ID, "xdr")));
        when(productImageRepository.findByProductIds(List.of(PRODUCT_ID))).thenReturn(List.of());
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(sampleCategory(CATEGORY_ID, "xdr")));

        mediator.send(listProductsQuery);

        Cache productListCache = cacheManager.getCache(ProductCacheNames.PRODUCT_LIST);
        assertThat(productListCache).isNotNull();
        assertThat(productListCache.get(listProductsQuery)).isNotNull();

        mediator.send(new CreateProductCommand(
                "XDR Premium",
                CATEGORY_ID,
                2,
                "Managed XDR service",
                "Technical details",
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                14,
                List.of("24/7 SOC")
        ));

        assertThat(productListCache.get(listProductsQuery)).isNull();

        mediator.send(listProductsQuery);
        verify(productRepository, times(2)).findAll(0, 20, true, null, CATEGORY_ID, null, "xdr",
                null, null, null, null, null, sort);
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
                "XDR Renamed",
                "Updated description",
                true
        ));

        assertThat(categoryByIdCache.get(CATEGORY_ID)).isNull();
    }

    @Test
    void should_keep_product_list_cache_when_create_product_command_fails() {
        Product product = sampleProduct(PRODUCT_ID, CATEGORY_ID);
        Page<Product> page = new Page<>(List.of(product), 0, 20, 1, 1);
        UUID unknownCategory = UUID.fromString("33333333-3333-3333-3333-333333333333");

        when(productRepository.findAll(0, 20, true, null, CATEGORY_ID, null, "xdr",
                null, null, null, null, null, sort)).thenReturn(page);
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory(CATEGORY_ID, "xdr")));
        when(productImageRepository.findByProductIds(List.of(PRODUCT_ID))).thenReturn(List.of());
        when(categoryRepository.findById(unknownCategory)).thenReturn(Optional.empty());

        mediator.send(listProductsQuery);

        Cache productListCache = cacheManager.getCache(ProductCacheNames.PRODUCT_LIST);
        assertThat(productListCache).isNotNull();
        assertThat(productListCache.get(listProductsQuery)).isNotNull();

        var result = mediator.send(new CreateProductCommand(
                "XDR Premium",
                unknownCategory,
                2,
                "Managed XDR service",
                "Technical details",
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                14,
                List.of("24/7 SOC")
        ));

        assertThat(result.isFailure()).isTrue();
        assertThat(productListCache.get(listProductsQuery)).isNotNull();

        mediator.send(listProductsQuery);
        verify(productRepository, times(1)).findAll(0, 20, true, null, CATEGORY_ID, null, "xdr",
                null, null, null, null, null, sort);
    }

    @Test
    void should_keep_product_list_cache_when_update_category_command_fails() {
        Product product = sampleProduct(PRODUCT_ID, CATEGORY_ID);
        Page<Product> page = new Page<>(List.of(product), 0, 20, 1, 1);
        UUID unknownCategory = UUID.fromString("44444444-4444-4444-4444-444444444444");

        when(productRepository.findAll(0, 20, true, null, CATEGORY_ID, null, "xdr",
                null, null, null, null, null, sort)).thenReturn(page);
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
                "XDR Renamed",
                "Updated description",
                true
        ));

        assertThat(result.isFailure()).isTrue();
        assertThat(productListCache.get(listProductsQuery)).isNotNull();

        mediator.send(listProductsQuery);
        verify(productRepository, times(1)).findAll(0, 20, true, null, CATEGORY_ID, null, "xdr",
                null, null, null, null, null, sort);
    }

    private void clearAllCaches() {
        clearCache(ProductCacheNames.PRODUCT_LIST);
        clearCache(ProductCacheNames.PRODUCT_BY_ID);
        clearCache(ProductCacheNames.CATEGORY_LIST);
        clearCache(ProductCacheNames.CATEGORY_BY_ID);
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
                "XDR Ultimate",
                categoryId,
                1,
                "Managed XDR service",
                "Technical details",
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                true,
                true,
                14,
                List.of("24/7 SOC"),
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
                name.toUpperCase(),
                "Category description",
                null,
                true,
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
                                                          ProductImageRepository productImageRepository) {
            return new ListProductsQueryHandler(productRepository, categoryRepository, productImageRepository);
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
    }
}
