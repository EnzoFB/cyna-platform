package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.LoginOtpChallenge;

import java.util.Optional;
import java.util.UUID;

public interface LoginOtpChallengeRepository {

    void save(LoginOtpChallenge challenge);

    Optional<LoginOtpChallenge> findById(UUID id);

    /**
     * Removes every unconsumed challenge owned by the user — called before
     * issuing a new one so the user never has more than one valid OTP at
     * a time. Prevents the "spam /login to generate N concurrent challenges
     * then brute-force them in parallel" attack.
     */
    void deleteUnconsumedByUserId(UUID userId);
}
