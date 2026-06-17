package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.domain.model.EmailVerificationToken;
import com.cyna.modules.user.domain.repository.EmailVerificationTokenRepository;
import com.cyna.modules.user.infrastructure.persistence.mapper.EmailVerificationTokenJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaEmailVerificationTokenRepositoryAdapter implements EmailVerificationTokenRepository {

    private final SpringDataEmailVerificationTokenRepository springRepo;
    private final EmailVerificationTokenJpaMapper mapper;

    public JpaEmailVerificationTokenRepositoryAdapter(SpringDataEmailVerificationTokenRepository springRepo,
                                                      EmailVerificationTokenJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(EmailVerificationToken token) {
        springRepo.save(mapper.toJpa(token));
    }

    @Override
    public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
        return springRepo.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public void deleteUnconsumedByUserId(UUID userId) {
        springRepo.deleteUnconsumedByUserId(userId);
    }
}
