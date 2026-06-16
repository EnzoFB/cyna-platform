package com.cyna.modules.user.interfaces.dto.response;

import com.cyna.modules.user.application.model.LoginChallenge;

import java.util.UUID;

public record LoginChallengeResponse(
        UUID challengeId,
        long expiresInSeconds
) {
    public static LoginChallengeResponse from(LoginChallenge challenge) {
        return new LoginChallengeResponse(challenge.challengeId(), challenge.expiresInSeconds());
    }
}
