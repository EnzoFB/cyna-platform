package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.domain.model.UserConsentAction;
import com.cyna.modules.user.domain.model.UserConsentLog;
import com.cyna.modules.user.domain.repository.UserConsentLogRepository;
import com.cyna.modules.user.infrastructure.persistence.entity.UserConsentLogJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
class JpaUserConsentLogRepositoryAdapter implements UserConsentLogRepository {

    private final SpringDataUserConsentLogRepository springRepo;

    JpaUserConsentLogRepositoryAdapter(SpringDataUserConsentLogRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public UserConsentLog save(UserConsentLog log) {
        UserConsentLogJpaEntity entity = new UserConsentLogJpaEntity();
        entity.setId(log.getId());
        entity.setUserId(log.getUserId());
        entity.setAction(log.getAction().name());
        entity.setLabelVersion(log.getLabelVersion());
        entity.setIpAddress(log.getIpAddress());
        entity.setUserAgent(log.getUserAgent());
        entity.setGivenAt(log.getGivenAt());
        springRepo.save(entity);
        return log;
    }

    @Override
    public List<UserConsentLog> findAllByUserId(UUID userId) {
        return springRepo.findByUserIdOrderByGivenAtDesc(userId).stream()
                .map(e -> UserConsentLog.reconstitute(
                        e.getId(), e.getUserId(), UserConsentAction.valueOf(e.getAction()),
                        e.getLabelVersion(), e.getIpAddress(), e.getUserAgent(), e.getGivenAt()))
                .toList();
    }
}
