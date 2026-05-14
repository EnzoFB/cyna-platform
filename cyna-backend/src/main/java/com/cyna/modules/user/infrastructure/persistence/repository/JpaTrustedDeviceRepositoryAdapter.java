package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.domain.model.TrustedDevice;
import com.cyna.modules.user.domain.repository.TrustedDeviceRepository;
import com.cyna.modules.user.infrastructure.persistence.mapper.TrustedDeviceJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTrustedDeviceRepositoryAdapter implements TrustedDeviceRepository {

    private final SpringDataTrustedDeviceRepository springRepo;
    private final TrustedDeviceJpaMapper mapper;

    public JpaTrustedDeviceRepositoryAdapter(SpringDataTrustedDeviceRepository springRepo,
                                             TrustedDeviceJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(TrustedDevice device) {
        springRepo.save(mapper.toJpa(device));
    }

    @Override
    public Optional<TrustedDevice> findByTokenHash(String tokenHash) {
        return springRepo.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        springRepo.deleteAllByUserId(userId);
    }
}
