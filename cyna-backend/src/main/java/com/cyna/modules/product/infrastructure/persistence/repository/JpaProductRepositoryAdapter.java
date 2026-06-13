package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.application.query.list.ProductSort;
import com.cyna.modules.product.application.query.list.ProductSortField;
import com.cyna.modules.product.application.query.list.SortDirection;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryTranslationJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductTranslationJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.mapper.ProductJpaMapper;
import com.cyna.shared.domain.Page;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProductRepositoryAdapter implements ProductRepository {

    private final SpringDataProductRepository springRepo;
    private final ProductJpaMapper mapper;

    public JpaProductRepositoryAdapter(SpringDataProductRepository springRepo, ProductJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Product product) {
        springRepo.save(mapper.toJpa(product));
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Product> findAllByIds(List<UUID> ids) {
        return springRepo.findAllByIdIn(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Page<Product> findAll(int page,
                                 int size,
                                 Boolean published,
                                 Boolean available,
                                 UUID categoryId,
                                 List<UUID> categoryIds,
                                 String search,
                                 BigDecimal monthlyPriceMin,
                                 BigDecimal monthlyPriceMax,
                                 BigDecimal annualPriceMin,
                                 BigDecimal annualPriceMax,
                                 Integer minFreeTrialDays,
                                 ProductSort sort) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        List<UUID> mergedCategoryIds = mergeCategoryIds(categoryId, categoryIds);
        List<String> searchTerms = splitSearchTerms(search);

        Specification<ProductJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (published != null) {
                predicates.add(cb.equal(root.get("isPublished"), published));
            }

            if (available != null) {
                predicates.add(cb.equal(root.get("isAvailable"), available));
            }

            if (!mergedCategoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(mergedCategoryIds));
            }

            if (monthlyPriceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("monthlyPrice"), monthlyPriceMin));
            }

            if (monthlyPriceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("monthlyPrice"), monthlyPriceMax));
            }

            if (annualPriceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("annualPrice"), annualPriceMin));
            }

            if (annualPriceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("annualPrice"), annualPriceMax));
            }

            if (minFreeTrialDays != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("freeTrialDays"), minFreeTrialDays));
            }

            for (String searchTerm : searchTerms) {
                String likeValue = "%" + searchTerm + "%";

                // Search in product FR translations (name, serviceDescription, technicalDescription)
                Subquery<Long> ptSub = query.subquery(Long.class);
                var ptRoot = ptSub.from(ProductTranslationJpaEntity.class);
                ptSub.select(cb.literal(1L))
                    .where(cb.and(
                        cb.equal(ptRoot.get("id").get("productId"), root.get("id")),
                        cb.equal(ptRoot.get("id").get("locale"), "fr"),
                        cb.or(
                            cb.like(cb.lower(ptRoot.get("name")), likeValue),
                            cb.like(cb.lower(ptRoot.get("serviceDescription")), likeValue),
                            cb.like(cb.lower(ptRoot.get("technicalDescription")), likeValue)
                        )
                    ));

                // Search in category slug (non-translatable) + category FR translations
                Subquery<Long> ctSub = query.subquery(Long.class);
                var ctRoot = ctSub.from(CategoryTranslationJpaEntity.class);
                ctSub.select(cb.literal(1L))
                    .where(cb.and(
                        cb.equal(ctRoot.get("id").get("categoryId"), root.get("category").get("id")),
                        cb.equal(ctRoot.get("id").get("locale"), "fr"),
                        cb.or(
                            cb.like(cb.lower(ctRoot.get("fullName")), likeValue),
                            cb.like(cb.lower(ctRoot.get("description")), likeValue)
                        )
                    ));

                predicates.add(cb.or(
                    cb.exists(ptSub),
                    cb.like(cb.lower(root.get("category").get("name")), likeValue),
                    cb.exists(ctSub)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        org.springframework.data.domain.Page<ProductJpaEntity> result = springRepo.findAll(spec, pageable);
        List<Product> items = result.getContent().stream().map(mapper::toDomain).toList();

        return new Page<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public boolean existsById(UUID id) {
        return springRepo.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        springRepo.deleteById(id);
    }

    @Override
    public long countByCategoryId(UUID categoryId) {
        return springRepo.countByCategory_Id(categoryId);
    }

    private Sort buildSort(ProductSort sort) {
        Sort.Direction direction = sort.direction() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort availability = Sort.by(Sort.Direction.DESC, "isAvailable");
        Sort primary = Sort.by(direction, sort.field().jpaProperty());

        if (sort.field() == ProductSortField.PRIORITY) {
            return availability.and(primary).and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return availability.and(primary);
    }

    private List<UUID> mergeCategoryIds(UUID categoryId, List<UUID> categoryIds) {
        LinkedHashSet<UUID> merged = new LinkedHashSet<>();
        if (categoryId != null) {
            merged.add(categoryId);
        }
        if (categoryIds != null) {
            categoryIds.stream()
                    .filter(java.util.Objects::nonNull)
                    .forEach(merged::add);
        }
        return List.copyOf(merged);
    }

    private List<String> splitSearchTerms(String search) {
        if (search == null || search.isBlank()) {
            return List.of();
        }

        return Arrays.stream(search.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(term -> !term.isBlank())
                .toList();
    }
}
