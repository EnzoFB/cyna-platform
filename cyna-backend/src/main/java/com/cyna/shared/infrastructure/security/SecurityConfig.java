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
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Locale;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({SecurityProperties.class, com.cyna.shared.infrastructure.logging.LoggingProperties.class})
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:4201}")
    private List<String> allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final SecurityProperties securityProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitingFilter rateLimitingFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtAccessDeniedHandler jwtAccessDeniedHandler,
                          SecurityProperties securityProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
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
        config.setExposedHeaders(List.of("Authorization", "X-Correlation-Id"));
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
                        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                        repository.setCookiePath("/");
                        csrf.csrfTokenRepository(repository);
                        csrf.requireCsrfProtectionMatcher(authCookieEndpointsCsrfMatcher());
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
                        .requestMatchers(HttpMethod.POST, "/api/v1/logs/client").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/contact").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/offers/promotions/**").permitAll()
                        .requestMatchers("/api/v1/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/subscriptions/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/orders/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()
                        // VAT preview is computed solely from the request body (productIds +
                        // billing country); it carries no user data, so the cart can show an
                        // estimated TTC to guests too. The legally-binding VAT is still
                        // recomputed from the real billing address at checkout.
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/tax-preview").permitAll()
                        .requestMatchers("/api/v1/payments/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/account/payment-methods/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/account/check-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/account/email/confirm").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class);

        if (securityProperties.requireHttps()) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        return http.build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static RequestMatcher authCookieEndpointsCsrfMatcher() {
        return new OrRequestMatcher(
                new AntPathRequestMatcher("/api/v1/auth/refresh", HttpMethod.POST.name()),
                new AntPathRequestMatcher("/api/v1/auth/logout", HttpMethod.POST.name())
        );
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
