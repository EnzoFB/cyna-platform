package com.cyna.modules.user.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable proof-of-consent entry (RGPD Art. 7.1). Records that the user
 * explicitly accepted the Terms / Privacy Policy at registration, which
 * version of the wording they saw, and the technical context — so the consent
 * can be evidenced later. Never edited, never updated.
 */
public final class UserConsentLog {

    private final UUID id;
    private final UUID userId;
    private final UserConsentAction action;
    private final String labelVersion;
    private final String ipAddress;
    private final String userAgent;
    private final Instant givenAt;

    private UserConsentLog(UUID id, UUID userId, UserConsentAction action,
                           String labelVersion, String ipAddress, String userAgent,
                           Instant givenAt) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.labelVersion = labelVersion;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.givenAt = givenAt;
    }

    public static UserConsentLog record(UUID userId, UserConsentAction action,
                                        String labelVersion, String ipAddress,
                                        String userAgent) {
        return new UserConsentLog(UUID.randomUUID(), userId, action,
                labelVersion, ipAddress, userAgent, Instant.now());
    }

    public static UserConsentLog reconstitute(UUID id, UUID userId, UserConsentAction action,
                                              String labelVersion, String ipAddress,
                                              String userAgent, Instant givenAt) {
        return new UserConsentLog(id, userId, action, labelVersion,
                ipAddress, userAgent, givenAt);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UserConsentAction getAction() { return action; }
    public String getLabelVersion() { return labelVersion; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getGivenAt() { return givenAt; }
}
