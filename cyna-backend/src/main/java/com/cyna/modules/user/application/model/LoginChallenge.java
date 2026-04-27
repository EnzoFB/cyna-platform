package com.cyna.modules.user.application.model;

import java.util.UUID;

public record LoginChallenge(
        UUID challengeId,
        long expiresInSeconds
) {}
