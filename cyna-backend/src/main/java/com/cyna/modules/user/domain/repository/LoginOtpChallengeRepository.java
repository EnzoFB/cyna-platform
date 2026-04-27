package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.LoginOtpChallenge;

import java.util.Optional;
import java.util.UUID;

public interface LoginOtpChallengeRepository {

    void save(LoginOtpChallenge challenge);

    Optional<LoginOtpChallenge> findById(UUID id);
}
