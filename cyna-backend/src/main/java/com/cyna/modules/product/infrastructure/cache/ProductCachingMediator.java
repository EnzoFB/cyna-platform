package com.cyna.modules.product.infrastructure.cache;

import com.cyna.modules.product.application.command.addimage.AddProductImageCommand;
import com.cyna.modules.product.application.command.addtocaousel.AddToCarouselCommand;
import com.cyna.modules.product.application.command.create.CreateProductCommand;
import com.cyna.modules.product.application.command.createcategory.CreateCategoryCommand;
import com.cyna.modules.product.application.command.createpromotion.CreatePromotionCommand;
import com.cyna.modules.product.application.command.delete.DeleteProductCommand;
import com.cyna.modules.product.application.command.deletecategory.DeleteCategoryCommand;
import com.cyna.modules.product.application.command.deleteimage.DeleteProductImageCommand;
import com.cyna.modules.product.application.command.deletepromotion.DeletePromotionCommand;
import com.cyna.modules.product.application.command.reordercarouselpromotions.ReorderCarouselPromotionsCommand;
import com.cyna.modules.product.application.command.reorderimages.ReorderProductImagesCommand;
import com.cyna.modules.product.application.command.removefromcarousel.RemoveFromCarouselCommand;
import com.cyna.modules.product.application.command.update.UpdateProductCommand;
import com.cyna.modules.product.application.command.updatecategory.UpdateCategoryCommand;
import com.cyna.modules.product.application.command.updatecategoryimage.UpdateCategoryImageCommand;
import com.cyna.modules.product.application.command.updateoffercarouselsettings.UpdateOfferCarouselSettingsCommand;
import com.cyna.modules.product.application.command.updatepromotion.UpdatePromotionCommand;
import com.cyna.modules.product.application.query.getbyid.GetProductByIdQuery;
import com.cyna.modules.product.application.query.getcategorybyid.GetCategoryByIdQuery;
import com.cyna.modules.product.application.query.getoffercarouselsettings.GetOfferCarouselSettingsQuery;
import com.cyna.modules.product.application.query.list.ListProductsQuery;
import com.cyna.modules.product.application.query.listcategories.ListCategoriesQuery;
import com.cyna.modules.product.application.query.listofferpromotions.ListOfferPromotionsQuery;
import com.cyna.shared.application.Command;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.application.Query;
import com.cyna.shared.domain.Result;
import com.cyna.shared.infrastructure.mediator.SpringMediator;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Primary
public class ProductCachingMediator implements Mediator {

    private final SpringMediator delegate;
    private final CacheManager cacheManager;

    public ProductCachingMediator(SpringMediator delegate, CacheManager cacheManager) {
        this.delegate = delegate;
        this.cacheManager = cacheManager;
    }

    @Override
    public <R> Result<R> send(Command<R> command) {
        Result<R> result = delegate.send(command);
        if (result.isSuccess()) {
            evictCachesAfterSuccess(command);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Query<R> query) {
        if (query instanceof ListProductsQuery listProductsQuery) {
            return (R) getOrLoad(
                    ProductCacheNames.PRODUCT_LIST,
                    listProductsQuery,
                    () -> delegate.send(listProductsQuery),
                    true
            );
        }

        if (query instanceof GetProductByIdQuery getProductByIdQuery) {
            return (R) getOrLoad(
                    ProductCacheNames.PRODUCT_BY_ID,
                    getProductByIdQuery.id(),
                    () -> delegate.send(getProductByIdQuery),
                    false
            );
        }

        if (query instanceof ListCategoriesQuery listCategoriesQuery) {
            return (R) getOrLoad(
                    ProductCacheNames.CATEGORY_LIST,
                    listCategoriesQuery,
                    () -> delegate.send(listCategoriesQuery),
                    true
            );
        }

        if (query instanceof GetCategoryByIdQuery getCategoryByIdQuery) {
            return (R) getOrLoad(
                    ProductCacheNames.CATEGORY_BY_ID,
                    getCategoryByIdQuery.id(),
                    () -> delegate.send(getCategoryByIdQuery),
                    false
            );
        }

        if (query instanceof ListOfferPromotionsQuery listOfferPromotionsQuery) {
            return (R) getOrLoad(
                    ProductCacheNames.OFFER_PROMOTIONS_LIST,
                    listOfferPromotionsQuery,
                    () -> delegate.send(listOfferPromotionsQuery),
                    true
            );
        }

        if (query instanceof GetOfferCarouselSettingsQuery getOfferCarouselSettingsQuery) {
            return (R) getOrLoad(
                    ProductCacheNames.OFFER_CAROUSEL_SETTINGS,
                    getOfferCarouselSettingsQuery,
                    () -> delegate.send(getOfferCarouselSettingsQuery),
                    false
            );
        }

        return delegate.send(query);
    }

    private void evictCachesAfterSuccess(Command<?> command) {
        if (command instanceof CreateProductCommand) {
            evictAll(ProductCacheNames.PRODUCT_LIST);
            return;
        }

        if (command instanceof UpdateProductCommand updateProductCommand) {
            evictAll(ProductCacheNames.PRODUCT_LIST);
            evictByKey(ProductCacheNames.PRODUCT_BY_ID, updateProductCommand.id());
            evictAll(ProductCacheNames.OFFER_PROMOTIONS_LIST);
            return;
        }

        if (command instanceof DeleteProductCommand deleteProductCommand) {
            evictAll(ProductCacheNames.PRODUCT_LIST);
            evictByKey(ProductCacheNames.PRODUCT_BY_ID, deleteProductCommand.id());
            evictAll(ProductCacheNames.OFFER_PROMOTIONS_LIST);
            return;
        }

        if (command instanceof CreatePromotionCommand
                || command instanceof UpdatePromotionCommand
                || command instanceof DeletePromotionCommand) {
            evictAll(ProductCacheNames.PRODUCT_LIST);
            evictAll(ProductCacheNames.PRODUCT_BY_ID);
            evictAll(ProductCacheNames.OFFER_PROMOTIONS_LIST);
            return;
        }

        if (command instanceof AddProductImageCommand
                || command instanceof DeleteProductImageCommand
                || command instanceof ReorderProductImagesCommand) {
            evictAll(ProductCacheNames.OFFER_PROMOTIONS_LIST);
            return;
        }

        if (command instanceof AddToCarouselCommand
                || command instanceof RemoveFromCarouselCommand
                || command instanceof ReorderCarouselPromotionsCommand) {
            evictAll(ProductCacheNames.OFFER_PROMOTIONS_LIST);
            return;
        }

        if (command instanceof UpdateOfferCarouselSettingsCommand) {
            evictAll(ProductCacheNames.OFFER_CAROUSEL_SETTINGS);
            return;
        }

        if (command instanceof CreateCategoryCommand) {
            evictAll(ProductCacheNames.CATEGORY_LIST);
            return;
        }

        if (command instanceof UpdateCategoryImageCommand updateCategoryImageCommand) {
            evictAll(ProductCacheNames.CATEGORY_LIST);
            evictByKey(ProductCacheNames.CATEGORY_BY_ID, updateCategoryImageCommand.id());
            return;
        }

        if (command instanceof UpdateCategoryCommand updateCategoryCommand) {
            evictAll(ProductCacheNames.CATEGORY_LIST);
            evictByKey(ProductCacheNames.CATEGORY_BY_ID, updateCategoryCommand.id());
            evictAll(ProductCacheNames.PRODUCT_LIST);
            evictAll(ProductCacheNames.PRODUCT_BY_ID);
            evictAll(ProductCacheNames.OFFER_PROMOTIONS_LIST);
            return;
        }

        if (command instanceof DeleteCategoryCommand deleteCategoryCommand) {
            evictAll(ProductCacheNames.CATEGORY_LIST);
            evictByKey(ProductCacheNames.CATEGORY_BY_ID, deleteCategoryCommand.id());
            evictAll(ProductCacheNames.PRODUCT_LIST);
            evictAll(ProductCacheNames.PRODUCT_BY_ID);
            evictAll(ProductCacheNames.OFFER_PROMOTIONS_LIST);
        }
    }

    @SuppressWarnings("unchecked")
    private <R> R getOrLoad(String cacheName, Object key, Supplier<R> loader, boolean cacheNullValue) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return loader.get();
        }

        Cache.ValueWrapper cached = cache.get(key);
        if (cached != null) {
            return (R) cached.get();
        }

        R value = loader.get();
        if (cacheNullValue || value != null) {
            cache.put(key, value);
        }
        return value;
    }

    private void evictAll(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private void evictByKey(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
