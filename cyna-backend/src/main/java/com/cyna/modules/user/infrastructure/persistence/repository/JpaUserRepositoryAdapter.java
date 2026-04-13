package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.modules.user.infrastructure.persistence.mapper.UserJpaMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springRepo;
    private final UserJpaMapper mapper;

    public JpaUserRepositoryAdapter(SpringDataUserRepository springRepo, UserJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(User user) {
        springRepo.save(mapper.toJpa(user));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return springRepo.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return springRepo.existsByEmail(email.value());
    }

    @Override
    public List<User> findAll(int page, int size) {
        return springRepo.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .getContent()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return springRepo.count();
    }

    @Override
    public void deleteById(UserId id) {
        springRepo.deleteById(id.value());
    }
}
