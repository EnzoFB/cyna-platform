package com.cyna.shared.infrastructure.security;

import com.cyna.modules.user.infrastructure.security.JwtAccessDeniedHandler;
import com.cyna.modules.user.infrastructure.security.JwtAuthenticationEntryPoint;
import com.cyna.modules.user.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Locale;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:4201}")
    private List<String> allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final SecurityProperties securityProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtAccessDeniedHandler jwtAccessDeniedHandler,
                          SecurityProperties securityProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
        this.securityProperties = securityProperties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> {
                    SecurityProperties.Csrf csrfConfig = securityProperties.csrf();
                    if (csrfConfig != null && csrfConfig.enabled()) {
                        csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
                        if (csrfConfig.ignoredPaths() != null && !csrfConfig.ignoredPaths().isEmpty()) {
                            var matchers = csrfConfig.ignoredPaths().stream()
                                    .map(AntPathRequestMatcher::new)
                                    .toArray(AntPathRequestMatcher[]::new);
                            csrf.ignoringRequestMatchers(matchers);
                        }
                    } else {
                        csrf.disable();
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> {
                    headers.contentTypeOptions(options -> {});
                    headers.frameOptions(frame -> frame.deny());
                    headers.referrerPolicy(referrer -> referrer
                            .policy(resolveReferrerPolicy(securityProperties.referrerPolicy())));
                    if (hasText(securityProperties.contentSecurityPolicy())) {
                        headers.contentSecurityPolicy(csp -> csp
                                .policyDirectives(securityProperties.contentSecurityPolicy()));
                    }
                    if (hasText(securityProperties.permissionsPolicy())) {
                        headers.permissionsPolicy(policy -> policy
                                .policy(securityProperties.permissionsPolicy()));
                    }
                    if (securityProperties.hsts() != null && securityProperties.hsts().enabled()) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(securityProperties.hsts().includeSubdomains())
                                .preload(securityProperties.hsts().preload())
                                .maxAgeInSeconds(securityProperties.hsts().maxAgeSeconds()));
                    }
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/subscriptions/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/orders/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/account/check-email").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (securityProperties.requireHttps()) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        return http.build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static ReferrerPolicyHeaderWriter.ReferrerPolicy resolveReferrerPolicy(String policy) {
        if (policy == null || policy.isBlank()) {
            return ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER;
        }
        String normalized = policy.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "NO_REFERRER" -> ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER;
            case "NO_REFERRER_WHEN_DOWNGRADE" -> ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER_WHEN_DOWNGRADE;
            case "SAME_ORIGIN" -> ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN;
            case "ORIGIN" -> ReferrerPolicyHeaderWriter.ReferrerPolicy.ORIGIN;
            case "STRICT_ORIGIN" -> ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN;
            case "ORIGIN_WHEN_CROSS_ORIGIN" -> ReferrerPolicyHeaderWriter.ReferrerPolicy.ORIGIN_WHEN_CROSS_ORIGIN;
            case "STRICT_ORIGIN_WHEN_CROSS_ORIGIN" ->
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN;
            case "UNSAFE_URL" -> ReferrerPolicyHeaderWriter.ReferrerPolicy.UNSAFE_URL;
            default -> ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER;
        };
    }
}
