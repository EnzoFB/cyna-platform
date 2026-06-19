package com.cyna.modules.user.infrastructure.security;

import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.shared.infrastructure.security.AccessTokenAuthenticator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * User-module adapter wiring the shared authentication plumbing to this
 * module's JWT validation. The {@code user} module owns token issuance and
 * validation ({@link JwtProvider}); the shared filter chain only knows the
 * {@link AccessTokenAuthenticator} SPI, so the dependency points
 * {@code user → shared} — never the reverse.
 */
@Component
public class JwtAccessTokenAuthenticator implements AccessTokenAuthenticator {

    private final JwtProvider jwtProvider;

    public JwtAccessTokenAuthenticator(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Optional<AuthenticatedPrincipal> authenticate(String token) {
        try {
            var claims = jwtProvider.validateAccessToken(token);
            String userId = (String) claims.get("userId");

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");

            return Optional.of(new AuthenticatedPrincipal(userId, roles));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
