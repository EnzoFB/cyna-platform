package com.cyna.modules.product.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class ProductCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                buildCache(ProductCacheNames.PRODUCT_LIST, Duration.ofMinutes(5), 200),
                buildCache(ProductCacheNames.PRODUCT_BY_ID, Duration.ofMinutes(5), 1_000),
                buildCache(ProductCacheNames.CATEGORY_LIST, Duration.ofHours(1), 100),
                buildCache(ProductCacheNames.CATEGORY_BY_ID, Duration.ofHours(1), 500)
        ));
        return cacheManager;
    }

    private CaffeineCache buildCache(String cacheName, Duration ttl, long maximumSize) {
        return new CaffeineCache(
                cacheName,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttl)
                        .maximumSize(maximumSize)
                        .build()
        );
    }
}
