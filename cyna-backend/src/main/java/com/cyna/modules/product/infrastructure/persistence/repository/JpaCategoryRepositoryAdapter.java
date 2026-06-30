package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.infrastructure.persistence.mapper.CategoryJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

    private final SpringDataCategoryRepository springRepo;
    private final CategoryJpaMapper mapper;

    public JpaCategoryRepositoryAdapter(SpringDataCategoryRepository springRepo, CategoryJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Category category) {
        springRepo.save(mapper.toJpa(category));
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return springRepo.findByName(name).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return springRepo.existsByName(name);
    }

    @Override
    public List<Category> findAll() {
        return springRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        springRepo.deleteById(id);
    }

    @Override
    public void deleteAllByIds(List<UUID> ids) {
        springRepo.deleteAllByIdInBatch(ids);
    }
}
