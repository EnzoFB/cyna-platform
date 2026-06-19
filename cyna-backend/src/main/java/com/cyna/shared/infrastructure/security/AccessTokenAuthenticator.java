package com.cyna.shared.infrastructure.security;

import java.util.List;
import java.util.Optional;

/**
 * Cross-cutting SPI that turns a bearer access token into the authenticated
 * principal used to populate the Spring {@code SecurityContext}.
 *
 * <p>Authentication plumbing (the filter chain, this contract) is a
 * <em>shared</em> concern: every module's endpoints rely on it. Token
 * <em>validation</em> however is owned by the module that issues the tokens
 * (the {@code user} module), which provides the implementation. Keeping the
 * abstraction here lets {@code shared} authenticate requests without depending
 * on any module — preserving the strict modular-monolith boundary (no
 * {@code shared → modules} edge, no {@code shared ↔ user} cycle).
 */
public interface AccessTokenAuthenticator {

    /**
     * Validates the given access token and extracts its principal.
     *
     * @param token the raw JWT (without the {@code "Bearer "} prefix)
     * @return the authenticated principal, or {@link Optional#empty()} if the
     *         token is invalid, expired or otherwise unusable
     */
    Optional<AuthenticatedPrincipal> authenticate(String token);

    /** The minimal identity a request needs to be authorized downstream. */
    record AuthenticatedPrincipal(String userId, List<String> roles) {}
}
