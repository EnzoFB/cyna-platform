package com.cyna.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/swagger-ui", "/v3/api-docs", "/actuator/health"
    );

    private final AccessTokenAuthenticator accessTokenAuthenticator;

    public JwtAuthenticationFilter(AccessTokenAuthenticator accessTokenAuthenticator) {
        this.accessTokenAuthenticator = accessTokenAuthenticator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        accessTokenAuthenticator.authenticate(token).ifPresentOrElse(principal -> {
            var authorities = principal.roles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal.userId(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }, SecurityContextHolder::clearContext);

        filterChain.doFilter(request, response);
    }
}
