package com.cyna.shared.interfaces.rest;

import org.springframework.http.CacheControl;

import java.util.concurrent.TimeUnit;

public final class ApiCachePolicies {

    public static final CacheControl PRODUCT_CATALOG =
            CacheControl.maxAge(0, TimeUnit.SECONDS).mustRevalidate().cachePublic();

    public static final CacheControl STATIC_CONFIGURATION =
            CacheControl.maxAge(3600, TimeUnit.SECONDS).cachePublic();

    private ApiCachePolicies() {
    }
}
