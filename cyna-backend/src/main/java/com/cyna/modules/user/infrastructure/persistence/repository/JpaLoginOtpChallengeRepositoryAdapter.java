package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.domain.model.LoginOtpChallenge;
import com.cyna.modules.user.domain.repository.LoginOtpChallengeRepository;
import com.cyna.modules.user.infrastructure.persistence.mapper.LoginOtpChallengeJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaLoginOtpChallengeRepositoryAdapter implements LoginOtpChallengeRepository {

    private final SpringDataLoginOtpChallengeRepository springRepo;
    private final LoginOtpChallengeJpaMapper mapper;

    public JpaLoginOtpChallengeRepositoryAdapter(SpringDataLoginOtpChallengeRepository springRepo,
                                                 LoginOtpChallengeJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(LoginOtpChallenge challenge) {
        springRepo.save(mapper.toJpa(challenge));
    }

    @Override
    public Optional<LoginOtpChallenge> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }
}
