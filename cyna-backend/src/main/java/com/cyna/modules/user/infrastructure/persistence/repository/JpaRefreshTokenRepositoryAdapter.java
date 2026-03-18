package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.infrastructure.persistence.mapper.RefreshTokenJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository springRepo;
    private final RefreshTokenJpaMapper mapper;

    public JpaRefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository springRepo,
                                            RefreshTokenJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(RefreshToken token) {
        springRepo.save(mapper.toJpa(token));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return springRepo.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public void revokeAllByUserId(UUID userId) {
        springRepo.revokeAllByUserId(userId);
    }
}
