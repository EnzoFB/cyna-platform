package com.cyna.modules.product.application.query.list;

import com.cyna.modules.product.application.promotion.PromotionPriceView;
import com.cyna.modules.product.application.promotion.PromotionPricingResolver;
import com.cyna.modules.product.application.query.getbyid.ProductImageReadModel;
import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.QueryHandler;
import com.cyna.shared.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ListProductsQueryHandler implements QueryHandler<ListProductsQuery, Page<ProductReadModel>> {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionPricingResolver promotionPricingResolver;

    public ListProductsQueryHandler(ProductRepository productRepository,
                                    CategoryRepository categoryRepository,
                                    ProductImageRepository productImageRepository,
                                    PromotionRepository promotionRepository,
                                    PromotionPricingResolver promotionPricingResolver) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.promotionRepository = promotionRepository;
        this.promotionPricingResolver = promotionPricingResolver;
    }

    @Override
    public Page<ProductReadModel> handle(ListProductsQuery query) {
        int safePage = Math.max(0, query.page());
        int safeSize = Math.min(Math.max(1, query.size()), 100);

        var page = productRepository.findAll(
                safePage,
                safeSize,
                query.published(),
                query.available(),
                query.categoryId(),
                query.categoryIds(),
                query.search(),
                query.monthlyPriceMin(),
                query.monthlyPriceMax(),
                query.annualPriceMin(),
                query.annualPriceMax(),
                query.minFreeTrialDays(),
                query.sort()
        );

        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        Map<UUID, List<ProductImageReadModel>> productImages = productImageRepository.findByProductIds(
                        page.items().stream().map(p -> p.getId()).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        img -> img.getProductId(),
                        Collectors.mapping(
                                img -> new ProductImageReadModel(
                                        img.getId(),
                                        Base64.getEncoder().encodeToString(img.getImageData())
                                ),
                                Collectors.toList()
                        )
                ));
        Instant now = Instant.now();
        Map<UUID, Promotion> activePromotionsByProductId = promotionPricingResolver.selectActiveByProduct(
                promotionRepository.findActiveByProductIds(
                        page.items().stream().map(p -> p.getId()).distinct().toList(),
                        now
                ),
                now
        );

        var items = page.items().stream().map(product -> {
            PromotionPriceView pricing = promotionPricingResolver.resolve(
                    product,
                    activePromotionsByProductId.get(product.getId())
            );
            return new ProductReadModel(
                    product.getId(),
                    product.getName(),
                    product.getNameEn(),
                    product.getCategoryId(),
                    categoryNames.getOrDefault(product.getCategoryId(), "Unknown"),
                    product.getPriorityLevel(),
                    product.getServiceDescription(),
                    product.getServiceDescriptionEn(),
                    product.getTechnicalDescription(),
                    product.getTechnicalDescriptionEn(),
                    pricing.baseMonthlyPrice(),
                    pricing.baseAnnualPrice(),
                    pricing.discountedMonthlyPrice(),
                    pricing.discountedAnnualPrice(),
                    pricing.discountPercent(),
                    pricing.promotionStartAt(),
                    pricing.promotionEndAt(),
                    product.getCurrency(),
                    product.isPublished(),
                    product.isAvailable(),
                    product.getFreeTrialDays(),
                    product.getHighlightPoints(),
                    product.getHighlightPointsEn(),
                    productImages.getOrDefault(product.getId(), List.of()),
                    product.getCreatedAt(),
                    product.getUpdatedAt()
            );
        }).toList();

        return new Page<>(items, page.pageNumber(), page.pageSize(), page.totalElements(), page.totalPages());
    }
}
