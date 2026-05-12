package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.domain.model.PasswordResetToken;
import com.cyna.modules.user.domain.repository.PasswordResetTokenRepository;
import com.cyna.modules.user.infrastructure.persistence.mapper.PasswordResetTokenJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaPasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final SpringDataPasswordResetTokenRepository springRepo;
    private final PasswordResetTokenJpaMapper mapper;

    public JpaPasswordResetTokenRepositoryAdapter(SpringDataPasswordResetTokenRepository springRepo,
                                                  PasswordResetTokenJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(PasswordResetToken token) {
        springRepo.save(mapper.toJpa(token));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return springRepo.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public void deleteUnconsumedByUserId(UUID userId) {
        springRepo.deleteUnconsumedByUserId(userId);
    }
}
