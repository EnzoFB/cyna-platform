package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.model.UserId;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.modules.user.infrastructure.persistence.mapper.UserJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
    public Optional<User> findById(UserId id) {
        return springRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return springRepo.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return springRepo.existsByEmail(email.value());
    }
}
