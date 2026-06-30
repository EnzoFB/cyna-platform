package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.domain.model.EmailChangeToken;
import com.cyna.modules.user.domain.repository.EmailChangeTokenRepository;
import com.cyna.modules.user.infrastructure.persistence.entity.EmailChangeTokenJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaEmailChangeTokenRepositoryAdapter implements EmailChangeTokenRepository {

    private final SpringDataEmailChangeTokenRepository springRepo;

    public JpaEmailChangeTokenRepositoryAdapter(SpringDataEmailChangeTokenRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void save(EmailChangeToken token) {
        var entity = new EmailChangeTokenJpaEntity();
        entity.setId(token.id());
        entity.setUserId(token.userId());
        entity.setNewEmail(token.newEmail());
        entity.setToken(token.token());
        entity.setExpiresAt(token.expiresAt());
        entity.setCreatedAt(token.createdAt());
        springRepo.save(entity);
    }

    @Override
    public Optional<EmailChangeToken> findByToken(String token) {
        return springRepo.findByToken(token).map(e -> new EmailChangeToken(
                e.getId(), e.getUserId(), e.getNewEmail(),
                e.getToken(), e.getExpiresAt(), e.getCreatedAt()
        ));
    }

    @Override
    public void deleteByUserId(UUID userId) {
        springRepo.deleteByUserId(userId);
    }

    @Override
    public void deleteById(UUID id) {
        springRepo.deleteById(id);
    }
}
