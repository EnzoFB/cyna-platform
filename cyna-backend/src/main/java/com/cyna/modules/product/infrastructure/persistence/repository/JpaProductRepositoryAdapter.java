package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.infrastructure.persistence.mapper.ProductJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    public boolean existsById(UUID id) {
        return springRepo.existsById(id);
    }
}
